package uts.sdk.modules.uniFaceAISDK

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.Keep
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.ai.face.base.addFace.AddFaceCallBack
import com.ai.face.base.addFace.CaptureFaceDispose
import com.ai.face.base.utils.DataConvertUtils
import com.ai.face.faceVerify.verify.VerifyStatus
import com.faceAI.demo.FaceSDKConfig
import com.faceAI.demo.R
import com.faceAI.demo.base.utils.BitmapUtils
import com.faceAI.demo.base.view.FaceCoverView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CaptureFaceActivity、UTS 标准模式组件和兼容模式组件共用的原生 View。
 *
 * View 自己持有 CameraX 预览、帧分析和人脸抓拍生命周期，避免组件入口依赖
 * FragmentContainerView 或宿主页面的 FragmentManager。
 * @author FaceAISDK.Service@gmail.com
 */
@Keep
class CaptureFaceNativeView(context: Context) : FrameLayout(context) {

    private val previewView = PreviewView(context)
    private val faceCoverView = FaceCoverView(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val resultExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val encodingResult = AtomicBoolean(false)
    private val waitingForRetry = AtomicBoolean(false)

    @Volatile
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraControl: CameraControl? = null
    private var boundPreview: Preview? = null
    private var boundImageAnalysis: ImageAnalysis? = null
    private var boundCameraSelector: CameraSelector? = null
    private var faceDispose: CaptureFaceDispose? = null
    private var resultCallback: ((String, Float, String) -> Unit)? = null
    private var tipsCallback: ((Int, String) -> Unit)? = null
    private var errorCallback: ((String, String) -> Unit)? = null
    private var cameraChangedCallback: ((Int) -> Unit)? = null

    private var performanceMode = CaptureFaceDispose.PERFORMANCE_MODE_FAST
    private var needLivenessCheck = true
    @Volatile
    private var cameraId = CameraSelector.LENS_FACING_FRONT
    private var linearZoom = 0.12f
    private var rotationDegrees = AUTO_ROTATION_DEGREES
    @Volatile
    private var faceCoverVisible = false
    private var started = false
    private var released = false
    private var sessionId = 0L
    private var startScheduled = false
    private var previewStreaming = false
    private var previewFallbackTried = false
    private var cameraBindingGeneration = 0L
    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private var displayListenerRegistered = false

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit

        override fun onDisplayRemoved(displayId: Int) = Unit

        override fun onDisplayChanged(displayId: Int) {
            val currentDisplay = previewView.display ?: return
            if (currentDisplay.displayId == displayId) {
                runOnMainThread { updateUseCaseTargetRotation() }
            }
        }
    }

    private val startOnLayoutListener = object : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            view: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (view.isAttachedToWindow && right > left && bottom > top) {
                view.removeOnLayoutChangeListener(this)
                startScheduled = false
                view.post { startCameraWhenReady() }
            }
        }
    }

    init {
        setBackgroundColor(Color.BLACK)

        // native-view 中 TextureView 可能因为宿主合成层级而只显示黑色。
        // PERFORMANCE 优先使用 SurfaceView，更适合 CameraX 原生预览嵌入场景。
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        // 居中裁剪相机画面以铺满组件，避免宽高比不一致时出现上下黑边。
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        faceCoverView.visibility = View.GONE
        addView(
            previewView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        addView(
            faceCoverView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    fun setResultCallback(callback: ((String, Float, String) -> Unit)?) {
        resultCallback = callback
    }

    fun setTipsCallback(callback: ((Int, String) -> Unit)?) {
        tipsCallback = callback
    }

    fun setErrorCallback(callback: ((String, String) -> Unit)?) {
        errorCallback = callback
    }

    fun setCameraChangedCallback(callback: ((Int) -> Unit)?) {
        cameraChangedCallback = callback
    }

    fun setFaceCoverVisible(visible: Boolean) {
        faceCoverVisible = visible
        runOnMainThread {
            if (!released) {
                applyFaceCoverVisibility()
            }
        }
    }

    /**
     * 开始一次抓拍会话。成功后会暂停检测，调用 retry() 才会进入下一轮。
     * 重复调用会先结束上一轮，再使用新参数重新绑定相机。
     */
    @JvmOverloads
    fun start(
        performanceMode: Int = CaptureFaceDispose.PERFORMANCE_MODE_FAST,
        needLivenessCheck: Boolean = true,
        cameraId: Int = CameraSelector.LENS_FACING_FRONT,
        linearZoom: Float = 0.12f,
        rotationDegrees: Int = AUTO_ROTATION_DEGREES
    ) {
        if (released) {
            notifyError("VIEW_RELEASED", "CaptureFaceNativeView has been released")
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifyError("CAMERA_PERMISSION_REQUIRED", "Camera permission is required")
            return
        }

        if (!isSupportedCameraId(cameraId)) {
            notifyError("INVALID_CAMERA_ID", "cameraId must be 0 (front) or 1 (back)")
            return
        }

        if (!isSupportedRotationDegrees(rotationDegrees)) {
            notifyError(
                "INVALID_ROTATION_DEGREES",
                "rotationDegrees must be -1 (auto), 0, 90, 180 or 270"
            )
            return
        }

        this.performanceMode = performanceMode.coerceIn(
            CaptureFaceDispose.PERFORMANCE_MODE_NO_LIMIT,
            CaptureFaceDispose.PERFORMANCE_MODE_ACCURATE
        )
        this.needLivenessCheck = needLivenessCheck
        this.cameraId = cameraId
        this.linearZoom = linearZoom.coerceIn(0f, 1f)
        this.rotationDegrees = rotationDegrees

        if (!isAttachedToWindow || width == 0 || height == 0) {
            scheduleStartAfterLayout()
            return
        }

        startCameraWhenReady()
    }

    private fun startCameraWhenReady() {
        if (released) return
        if (!isAttachedToWindow || width == 0 || height == 0) {
            scheduleStartAfterLayout()
            return
        }

        val lifecycleOwner = findActivity() as? LifecycleOwner
        if (lifecycleOwner == null) {
            notifyError("LIFECYCLE_OWNER_REQUIRED", "The component host must implement LifecycleOwner")
            return
        }

        stopInternal()
        this.started = true
        registerDisplayListener()
        previewStreaming = false
        previewFallbackTried = false
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        val currentSession = ++sessionId
        waitingForRetry.set(false)

        // API、标准组件、兼容组件都可以独立作为第一个插件入口使用。
        FaceSDKConfig.init(context)
        faceDispose = CaptureFaceDispose(
            context,
            this.performanceMode,
            this.needLivenessCheck,
            object : AddFaceCallBack() {
                override fun onCompleted(
                    cropped: android.graphics.Bitmap,
                    silentScore: Float,
                    origin: android.graphics.Bitmap
                ) {
                    if (!started || released || currentSession != sessionId) return
                    if (!waitingForRetry.compareAndSet(false, true)) return
                    encodeAndDispatch(currentSession, cropped, silentScore, origin)
                }

                override fun onProcessTips(actionCode: Int) {
                    post {
                        if (started && !released && currentSession == sessionId) {
                            showProcessTips(actionCode)
                        }
                    }
                }
            }
        )

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (!started || released || currentSession != sessionId) {
                return@addListener
            }
            try {
                cameraProvider = providerFuture.get()
                bindCamera(lifecycleOwner, currentSession)
            } catch (e: Exception) {
                notifyError("CAMERA_INIT_FAILED", e.message ?: "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        if (released) return
        cancelScheduledStart()
        stopInternal()
    }

    fun retry() {
        runOnMainThread {
            if (
                started && !released &&
                waitingForRetry.compareAndSet(true, false)
            ) {
                faceDispose?.retry()
            }
        }
    }

    /** 在当前采集会话内切换前、后摄像头。 */
    fun toggleCamera() {
        val targetCameraId = if (cameraId == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        switchCamera(targetCameraId)
    }

    /** 切换到指定的前/后摄像头：0 前置，1 后置。 */
    fun switchCamera(newCameraId: Int) {
        if (!isSupportedCameraId(newCameraId)) {
            notifyError("INVALID_CAMERA_ID", "cameraId must be 0 (front) or 1 (back)")
            return
        }

        runOnMainThread {
            if (released) return@runOnMainThread
            if (newCameraId == cameraId && cameraControl != null) return@runOnMainThread

            // start() 可能正在等待 View 布局或 CameraProvider，此时先记住目标镜头。
            // Provider 就绪后会直接按最新 cameraId 绑定。
            if (!started || cameraProvider == null) {
                cameraId = newCameraId
                return@runOnMainThread
            }

            val lifecycleOwner = findActivity() as? LifecycleOwner
            if (lifecycleOwner == null) {
                notifyError(
                    "LIFECYCLE_OWNER_REQUIRED",
                    "The component host must implement LifecycleOwner"
                )
                return@runOnMainThread
            }

            previewStreaming = false
            previewFallbackTried = false
            previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            bindCamera(
                lifecycleOwner,
                sessionId,
                newCameraId,
                allowFallback = false,
                notifySwitch = true
            )
        }
    }

    /** CameraProvider 就绪且另一颗前/后摄像头存在时返回 true。 */
    fun canSwitchCamera(): Boolean {
        val provider = cameraProvider ?: return false
        val targetCameraId = if (cameraId == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        return hasCamera(provider, targetCameraId)
    }

    fun release() {
        if (released) return
        cancelScheduledStart()
        stopInternal()
        released = true
        resultCallback = null
        tipsCallback = null
        errorCallback = null
        cameraChangedCallback = null
        analysisExecutor.shutdownNow()
        resultExecutor.shutdownNow()
    }

    override fun onDetachedFromWindow() {
        // 标准组件由 onUnmounted、兼容组件由 NVBeforeUnload 负责最终 release。
        // 这里仅停止相机，避免 native-view 临时重新挂载时实例被永久标记 released。
        cancelScheduledStart()
        stopInternal()
        super.onDetachedFromWindow()
    }

    private fun cancelScheduledStart() {
        if (startScheduled) {
            removeOnLayoutChangeListener(startOnLayoutListener)
            startScheduled = false
        }
    }

    private fun scheduleStartAfterLayout() {
        if (!startScheduled) {
            startScheduled = true
            addOnLayoutChangeListener(startOnLayoutListener)
        }
    }

    private fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        currentSession: Long,
        requestedCameraId: Int = cameraId,
        allowFallback: Boolean = true,
        notifySwitch: Boolean = false
    ): Boolean {
        val provider = cameraProvider ?: return false
        val selection = createCompatibleCameraSelector(
            provider,
            requestedCameraId,
            allowFallback
        )
        if (selection == null) {
            if (notifySwitch) {
                notifyError(
                    "CAMERA_NOT_AVAILABLE",
                    "Requested camera is not available: $requestedCameraId"
                )
            }
            return false
        }

        val surfaceRotation = resolveSurfaceRotation()

        val newPreview = Preview.Builder()
            .setTargetRotation(surfaceRotation)
            .build()
        newPreview.setSurfaceProvider(previewView.surfaceProvider)

        val analysisBuilder = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setTargetRotation(surfaceRotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)

        val newImageAnalysis = analysisBuilder.build()
        val previousBindingGeneration = cameraBindingGeneration
        val analyzerGeneration = previousBindingGeneration + 1
        // 不使用 SAM Lambda。UTS 插件与 CameraX 分别经过 D8 处理时，Lambda 合成类
        // 可能不会生成 Analyzer 默认方法的转发实现，运行时会抛 AbstractMethodError。
        newImageAnalysis.setAnalyzer(
            analysisExecutor,
            object : ImageAnalysis.Analyzer {
                override fun analyze(imageProxy: ImageProxy) {
                    try {
                        if (
                            started && !released && currentSession == sessionId &&
                            analyzerGeneration == cameraBindingGeneration &&
                            !encodingResult.get()
                        ) {
                            faceDispose?.dispose(DataConvertUtils.imageProxy2Bitmap(imageProxy))
                        }
                    } catch (e: Exception) {
                        notifyError(
                            "FRAME_PROCESS_FAILED",
                            e.message ?: "Camera frame processing failed"
                        )
                    } finally {
                        imageProxy.close()
                    }
                }

                override fun getDefaultTargetResolution(): Size? = null

                override fun getTargetCoordinateSystem(): Int =
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL

                override fun updateTransform(matrix: Matrix?) = Unit
            }
        )

        val previousPreview = boundPreview
        val previousImageAnalysis = boundImageAnalysis
        val previousCameraSelector = boundCameraSelector
        cameraBindingGeneration = analyzerGeneration

        try {
            unbindOwnedUseCases(provider)
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                selection.cameraSelector,
                newPreview,
                newImageAnalysis
            )

            var actualCameraId = selection.cameraId
            val reportedCameraId = camera.cameraInfo.lensFacing
            if (isSupportedCameraId(reportedCameraId)) {
                actualCameraId = reportedCameraId
            }

            boundPreview = newPreview
            boundImageAnalysis = newImageAnalysis
            boundCameraSelector = selection.cameraSelector
            cameraControl = camera.cameraControl
            cameraId = actualCameraId
            previousImageAnalysis?.clearAnalyzer()

            try {
                camera.cameraControl.setLinearZoom(linearZoom)
            } catch (zoomError: Exception) {
                Log.w(TAG, "Unable to restore linear zoom on the selected camera", zoomError)
            }

            observePreviewStream(lifecycleOwner, currentSession, analyzerGeneration)
            if (notifySwitch || actualCameraId != requestedCameraId) {
                notifyCameraChanged(actualCameraId)
            }
            return true
        } catch (e: LinkageError) {
            newImageAnalysis.clearAnalyzer()
            cameraBindingGeneration = previousBindingGeneration
            restorePreviousUseCases(
                provider,
                lifecycleOwner,
                previousCameraSelector,
                previousPreview,
                previousImageAnalysis
            )
            val code = if (notifySwitch) "CAMERA_SWITCH_FAILED" else "CAMERA_DEPENDENCY_CONFLICT"
            notifyError(code, e.message ?: "CameraX binary dependency conflict")
            return false
        } catch (e: Exception) {
            newImageAnalysis.clearAnalyzer()
            cameraBindingGeneration = previousBindingGeneration
            restorePreviousUseCases(
                provider,
                lifecycleOwner,
                previousCameraSelector,
                previousPreview,
                previousImageAnalysis
            )
            val code = if (notifySwitch) "CAMERA_SWITCH_FAILED" else "CAMERA_BIND_FAILED"
            notifyError(code, e.message ?: "Camera bind failed")
            return false
        }
    }

    private fun observePreviewStream(
        lifecycleOwner: LifecycleOwner,
        currentSession: Long,
        currentBindingGeneration: Long
    ) {
        previewView.previewStreamState.removeObservers(lifecycleOwner)
        previewView.previewStreamState.observe(lifecycleOwner) { state ->
            if (
                started && !released && currentSession == sessionId &&
                currentBindingGeneration == cameraBindingGeneration
            ) {
                previewStreaming = state == PreviewView.StreamState.STREAMING
            }
        }

        postDelayed({
            if (
                !started || released || currentSession != sessionId ||
                currentBindingGeneration != cameraBindingGeneration || previewStreaming
            ) {
                return@postDelayed
            }

            if (!previewFallbackTried) {
                previewFallbackTried = true
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                bindCamera(
                    lifecycleOwner,
                    currentSession,
                    cameraId,
                    allowFallback = true,
                    notifySwitch = false
                )
            } else {
                notifyError(
                    "CAMERA_PREVIEW_NOT_STREAMING",
                    "Camera opened but preview did not start streaming"
                )
            }
        }, PREVIEW_START_TIMEOUT_MS)
    }

    private fun encodeAndDispatch(
        currentSession: Long,
        cropped: android.graphics.Bitmap,
        silentScore: Float,
        origin: android.graphics.Bitmap
    ) {
        if (!encodingResult.compareAndSet(false, true)) return

        resultExecutor.execute {
            try {
                val croppedBase64 = BitmapUtils.bitmapToBase64(cropped)
                val originBase64 = BitmapUtils.bitmapToBase64(origin)
                post {
                    if (started && !released && currentSession == sessionId) {
                        try {
                            resultCallback?.invoke(croppedBase64, silentScore, originBase64)
                        } catch (e: Exception) {
                            Log.e(TAG, "Result callback failed", e)
                        }
                    }
                }
            } catch (e: Exception) {
                notifyError("BITMAP_ENCODE_FAILED", e.message ?: "Bitmap Base64 encode failed")
            } finally {
                encodingResult.set(false)
            }
        }
    }

    private fun stopInternal() {
        started = false
        unregisterDisplayListener()
        sessionId++
        cameraBindingGeneration++
        previewStreaming = false
        previewFallbackTried = false
        encodingResult.set(false)
        waitingForRetry.set(false)
        try {
            (findActivity() as? LifecycleOwner)?.let { lifecycleOwner ->
                previewView.previewStreamState.removeObservers(lifecycleOwner)
            }
            cameraProvider?.let { provider -> unbindOwnedUseCases(provider) }
        } catch (_: Exception) {
        }
        boundImageAnalysis?.clearAnalyzer()
        boundPreview = null
        boundImageAnalysis = null
        boundCameraSelector = null
        cameraControl = null
        cameraProvider = null
        faceDispose?.release()
        faceDispose = null
    }

    private fun showProcessTips(actionCode: Int) {
        val textRes = when (actionCode) {
            VerifyStatus.VERIFY_DETECT_TIPS_ENUM.NO_FACE_REPEATEDLY -> R.string.no_face_detected_tips
            VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_SMALL -> R.string.come_closer_tips
            VerifyStatus.VERIFY_DETECT_TIPS_ENUM.FACE_TOO_LARGE -> R.string.far_away_tips
            VerifyStatus.ALIVE_DETECT_TYPE_ENUM.CLOSE_EYE -> R.string.no_close_eye_tips
            VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_CENTER -> R.string.keep_face_tips
            VerifyStatus.ALIVE_DETECT_TYPE_ENUM.TILT_HEAD -> R.string.no_tilt_head_tips
            VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_LEFT -> R.string.head_turn_left_tips
            VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_RIGHT -> R.string.head_turn_right_tips
            VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_UP -> R.string.no_look_up_tips
            VerifyStatus.ALIVE_DETECT_TYPE_ENUM.HEAD_DOWN -> R.string.no_look_down_tips
			VerifyStatus.ALIVE_DETECT_TYPE_ENUM.FACE_UNSTABLE -> R.string.keep_face_still_tips
		
            else -> 0
        }

        val message = if (textRes != 0) context.getString(textRes) else "Tips Code: $actionCode"
        if (faceCoverVisible && textRes != 0) {
            faceCoverView.setTipsText(textRes)
        }
        // FaceCoverView 的内部提示刷新可能修改自身 visibility；业务明确关闭时必须兜底隐藏。
        applyFaceCoverVisibility()
        try {
            tipsCallback?.invoke(actionCode, message)
        } catch (e: Exception) {
            Log.e(TAG, "Tips callback failed", e)
        }
    }

    private fun createCompatibleCameraSelector(
        provider: ProcessCameraProvider,
        preferredLensFacing: Int,
        allowFallback: Boolean
    ): CameraSelection? {
        val fallbackLensFacing = if (preferredLensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }

        return when {
            hasCamera(provider, preferredLensFacing) -> CameraSelection(
                cameraSelector(preferredLensFacing),
                preferredLensFacing
            )

            !allowFallback -> null

            hasCamera(provider, fallbackLensFacing) -> CameraSelection(
                cameraSelector(fallbackLensFacing),
                fallbackLensFacing
            )

            else -> CameraSelection(
                CameraSelector.Builder()
                    .addCameraFilter { cameraInfos -> cameraInfos }
                    .build(),
                preferredLensFacing
            )
        }
    }

    private fun hasCamera(provider: ProcessCameraProvider, lensFacing: Int): Boolean {
        return try {
            provider.hasCamera(cameraSelector(lensFacing))
        } catch (_: Exception) {
            false
        }
    }

    private fun cameraSelector(lensFacing: Int): CameraSelector =
        CameraSelector.Builder().requireLensFacing(lensFacing).build()

    private fun unbindOwnedUseCases(provider: ProcessCameraProvider) {
        val preview = boundPreview
        val imageAnalysis = boundImageAnalysis
        when {
            preview != null && imageAnalysis != null -> provider.unbind(preview, imageAnalysis)
            preview != null -> provider.unbind(preview)
            imageAnalysis != null -> provider.unbind(imageAnalysis)
        }
    }

    private fun restorePreviousUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector?,
        preview: Preview?,
        imageAnalysis: ImageAnalysis?
    ) {
        if (selector == null || preview == null || imageAnalysis == null) {
            cameraControl = null
            return
        }

        try {
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageAnalysis
            )
            cameraControl = camera.cameraControl
        } catch (restoreError: Exception) {
            cameraControl = null
            Log.e(TAG, "Failed to restore previous camera after switch failure", restoreError)
        }
    }

    private fun notifyCameraChanged(newCameraId: Int) {
        try {
            cameraChangedCallback?.invoke(newCameraId)
        } catch (e: Exception) {
            Log.e(TAG, "Camera changed callback failed", e)
        }
    }

    private fun isSupportedCameraId(value: Int): Boolean =
        value == CameraSelector.LENS_FACING_FRONT || value == CameraSelector.LENS_FACING_BACK

    private fun isSupportedRotationDegrees(value: Int): Boolean =
        value == AUTO_ROTATION_DEGREES || value == 0 || value == 90 ||
            value == 180 || value == 270

    private fun registerDisplayListener() {
        if (rotationDegrees != AUTO_ROTATION_DEGREES || displayListenerRegistered) return
        displayManager?.registerDisplayListener(displayListener, null)
        displayListenerRegistered = displayManager != null
    }

    private fun unregisterDisplayListener() {
        if (!displayListenerRegistered) return
        displayManager?.unregisterDisplayListener(displayListener)
        displayListenerRegistered = false
    }

    private fun resolveSurfaceRotation(): Int {
        if (rotationDegrees != AUTO_ROTATION_DEGREES) {
            return toSurfaceRotation(rotationDegrees)
        }
        return previewView.display?.rotation ?: Surface.ROTATION_0
    }

    private fun updateUseCaseTargetRotation() {
        if (!started || released || rotationDegrees != AUTO_ROTATION_DEGREES) return
        val surfaceRotation = resolveSurfaceRotation()
        boundPreview?.targetRotation = surfaceRotation
        boundImageAnalysis?.targetRotation = surfaceRotation
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            post(action)
        }
    }

    private fun applyFaceCoverVisibility() {
        faceCoverView.visibility = if (faceCoverVisible) View.VISIBLE else View.GONE
    }

    private fun toSurfaceRotation(value: Int): Int {
        return when (value) {
            90 -> Surface.ROTATION_90
            180 -> Surface.ROTATION_180
            270 -> Surface.ROTATION_270
            else -> Surface.ROTATION_0
        }
    }

    private fun findActivity(): Activity? {
        var currentContext: Context? = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return currentContext as? Activity
    }

    private fun notifyError(code: String, message: String) {
        post {
            try {
                errorCallback?.invoke(code, message)
            } catch (e: Exception) {
                Log.e(TAG, "Error callback failed", e)
            }
        }
    }

    private data class CameraSelection(
        val cameraSelector: CameraSelector,
        val cameraId: Int
    )

    private companion object {
        const val TAG = "CaptureFaceNativeView"
        const val PREVIEW_START_TIMEOUT_MS = 2500L
        const val AUTO_ROTATION_DEGREES = -1
    }
}
