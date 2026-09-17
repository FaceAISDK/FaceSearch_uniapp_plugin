package uts.sdk.modules.uniFaceAISDK

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Gravity
import android.view.Surface
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
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
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

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
    private val faceCoverTipsView = TextView(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val resultExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val encodingResult = AtomicBoolean(false)
    private val waitingForRetry = AtomicBoolean(false)
    private val retryScheduled = AtomicBoolean(false)
    private val nextFrameAnalysisAtMs = AtomicLong(0L)
    private val frameErrorReported = AtomicBoolean(false)
    private val frameTimeoutReported = AtomicBoolean(false)
    private val lastSdkCallbackAtMs = AtomicLong(0L)
    private val retainedFrameBitmaps = Collections.newSetFromMap(
        ConcurrentHashMap<Bitmap, Boolean>()
    )
    private val pendingTipsCode = AtomicInteger(NO_PENDING_TIPS)
    private val pendingTipsSession = AtomicLong(NO_SESSION)
    private val tipsDispatchScheduled = AtomicBoolean(false)

    @Volatile
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraControl: CameraControl? = null
    private var boundPreview: Preview? = null
    private var boundImageAnalysis: ImageAnalysis? = null
    private var boundCameraSelector: CameraSelector? = null
    @Volatile
    private var faceDispose: CaptureFaceDispose? = null
    private var resultCallback: ((String, Float, String) -> Unit)? = null
    private var tipsCallback: ((Int, String) -> Unit)? = null
    private var errorCallback: ((String, String) -> Unit)? = null
    private var cameraChangedCallback: ((Int) -> Unit)? = null

    private var performanceMode = CaptureFaceDispose.PERFORMANCE_MODE_FAST
    private var needLivenessCheck = false
    @Volatile
    private var cameraId = CameraSelector.LENS_FACING_FRONT
    private var linearZoom = 0.01f
    private var rotationDegrees = AUTO_ROTATION_DEGREES
    @Volatile
    private var faceCoverVisible = false
    @Volatile
    private var faceCoverTipsVisible = false
    @Volatile
    private var forceFitCenterPreview = false
    @Volatile
    private var started = false
    @Volatile
    private var released = false
    @Volatile
    private var sessionId = 0L
    private var startScheduled = false
    private var scheduledStartGeneration = 0L
    private var cameraInitializing = false
    private var restartAfterAttach = false
    private var previewStreaming = false
    private var previewFallbackTried = false
    @Volatile
    private var cameraBindingGeneration = 0L
    private var cameraTransitionInProgress = false
    private var pendingCameraId: Int? = null
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

    private val tipsDispatchRunnable = Runnable {
        tipsDispatchScheduled.set(false)
        val tipsSession = pendingTipsSession.getAndSet(NO_SESSION)
        val actionCode = pendingTipsCode.getAndSet(NO_PENDING_TIPS)
        if (
            actionCode != NO_PENDING_TIPS && started && !released &&
            tipsSession == sessionId
        ) {
            showProcessTips(actionCode)
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
                val generation = scheduledStartGeneration
                view.post {
                    if (!startScheduled || generation != scheduledStartGeneration) return@post
                    startScheduled = false
                    startCameraWhenReady()
                }
            }
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        clipChildren = true
        clipToPadding = true

        // native-view 中 TextureView 可能因为宿主合成层级而只显示黑色。
        // PERFORMANCE 优先使用 SurfaceView，更适合 CameraX 原生预览嵌入场景。
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        // 标准模式和兼容模式组件固定铺满预览；全屏 UTS API 会单独强制 FIT_CENTER。
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        // FaceCoverView 自带的文字与圆形共用 visibility，无法分别控制；清空后改由
        // 独立 TextView 显示过程提示，让 showFaceCover 只负责圆形遮罩。
        faceCoverView.setTipsText(0)
        faceCoverView.visibility = View.GONE
        faceCoverTipsView.apply {
            text = context.getString(R.string.sdk_init)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(dp(16), dp(6), dp(16), dp(6))
            background = GradientDrawable().apply {
                setColor(Color.argb(153, 0, 0, 0))
                cornerRadius = dp(18).toFloat()
            }
            visibility = View.GONE
        }
        addView(
            previewView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        addView(
            faceCoverView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        addView(
            faceCoverTipsView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            ).apply {
                marginStart = dp(10)
                marginEnd = dp(10)
            }
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
                applyPreviewScaleType()
            }
        }
    }

    fun setForceFitCenterPreview(force: Boolean) {
        forceFitCenterPreview = force
        runOnMainThread {
            if (!released) {
                applyPreviewScaleType()
            }
        }
    }

    fun setFaceCoverTipsVisible(visible: Boolean) {
        faceCoverTipsVisible = visible
        runOnMainThread {
            if (!released) {
                applyFaceCoverTipsVisibility()
                requestLayout()
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val contentWidth = width
        val contentHeight = height
        if (contentWidth <= 0 || contentHeight <= 0) return

        val shortEdge = minOf(contentWidth, contentHeight)
        val circleMargin = if (faceCoverTipsVisible) {
            shortEdge / FACE_COVER_MARGIN_WITH_TIPS_DIVISOR
        } else {
            dp(FACE_COVER_MARGIN_WITHOUT_TIPS_DP)
        }

        // SDK 的 FaceCoverView 会在竖屏时将圆心上移短边的 1/8。把它的布局高度
        // 向下扩展两倍该偏移量，抵消上移，同时保持遮罩覆盖整个可见区域。
        val sdkVerticalOffset = shortEdge / FACE_COVER_SDK_VERTICAL_OFFSET_DIVISOR
        val coverHeight = if (contentWidth <= contentHeight) {
            contentHeight + sdkVerticalOffset * 2
        } else {
            contentHeight
        }
        faceCoverView.layout(0, 0, contentWidth, coverHeight)
        faceCoverView.setMargin(circleMargin)

        // 提示文本紧贴圆形框上方，并保留 5dp 间隔。
        val circleRadius = shortEdge / 2f - circleMargin
        val circleTop = contentHeight / 2f - circleRadius
        val tipsWidth = faceCoverTipsView.measuredWidth
        val tipsHeight = faceCoverTipsView.measuredHeight
        val tipsLeft = (contentWidth - tipsWidth) / 2
        val tipsBottom = (circleTop - dp(FACE_COVER_TIPS_GAP_DP)).toInt()
        val tipsTop = (tipsBottom - tipsHeight).coerceAtLeast(0)
        faceCoverTipsView.layout(
            tipsLeft,
            tipsTop,
            tipsLeft + tipsWidth,
            tipsTop + tipsHeight
        )
    }

    /**
     * 开始一次抓拍会话。成功后会暂停检测，调用 retry() 才会进入下一轮。
     * 相同参数的重复 start() 是幂等的，避免页面重复更新导致相机反复解绑、绑定。
     */
    @JvmOverloads
    fun start(
        performanceMode: Int = CaptureFaceDispose.PERFORMANCE_MODE_FAST,
        needLivenessCheck: Boolean = false,
        cameraId: Int = CameraSelector.LENS_FACING_FRONT,
        linearZoom: Float = 0.01f,
        rotationDegrees: Int = AUTO_ROTATION_DEGREES
    ) {
        runOnMainThread {
            if (released) {
                notifyError("VIEW_RELEASED", "CaptureFaceNativeView has been released")
                return@runOnMainThread
            }

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notifyError("CAMERA_PERMISSION_REQUIRED", "Camera permission is required")
                return@runOnMainThread
            }

            if (!isSupportedCameraId(cameraId)) {
                notifyError("INVALID_CAMERA_ID", "cameraId must be 0 (front) or 1 (back)")
                return@runOnMainThread
            }

            if (!isSupportedRotationDegrees(rotationDegrees)) {
                notifyError(
                    "INVALID_ROTATION_DEGREES",
                    "rotationDegrees must be -1 (auto), 0, 90, 180 or 270"
                )
                return@runOnMainThread
            }

            val normalizedPerformanceMode = performanceMode.coerceIn(
                CaptureFaceDispose.PERFORMANCE_MODE_NO_LIMIT,
                CaptureFaceDispose.PERFORMANCE_MODE_ACCURATE
            )
            val normalizedZoom = linearZoom.coerceIn(0f, 1f)
            val sameConfiguration =
                this.performanceMode == normalizedPerformanceMode &&
                    this.needLivenessCheck == needLivenessCheck &&
                    this.cameraId == cameraId &&
                    this.linearZoom == normalizedZoom &&
                    this.rotationDegrees == rotationDegrees

            if (sameConfiguration && (started || cameraInitializing || startScheduled)) {
                if (waitingForRetry.get()) retry()
                return@runOnMainThread
            }

            cancelScheduledStart()
            restartAfterAttach = false
            this.performanceMode = normalizedPerformanceMode
            this.needLivenessCheck = needLivenessCheck
            this.cameraId = cameraId
            this.linearZoom = normalizedZoom
            this.rotationDegrees = rotationDegrees

            if (!isAttachedToWindow || width == 0 || height == 0) {
                scheduleStartAfterLayout()
                return@runOnMainThread
            }

            startCameraWhenReady()
        }
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
        claimActiveInstance(this)?.stopForReplacement()
        this.started = true
        cameraInitializing = true
        registerDisplayListener()
        previewStreaming = false
        previewFallbackTried = false
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        val currentSession = ++sessionId
        waitingForRetry.set(false)
        nextFrameAnalysisAtMs.set(SystemClock.elapsedRealtime() + SDK_START_SETTLE_MS)
        frameErrorReported.set(false)
        frameTimeoutReported.set(false)
        lastSdkCallbackAtMs.set(SystemClock.elapsedRealtime())
        queueFaceDisposeCreation(currentSession)

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (!started || released || currentSession != sessionId) {
                return@addListener
            }
            try {
                cameraInitializing = false
                cameraProvider = providerFuture.get()
                cameraTransitionInProgress = true
                if (!bindCamera(lifecycleOwner, currentSession)) {
                    stopInternal()
                }
            } catch (e: Exception) {
                cameraInitializing = false
                stopInternal()
                notifyError("CAMERA_INIT_FAILED", e.message ?: "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        runOnMainThread {
            if (released) return@runOnMainThread
            restartAfterAttach = false
            cancelScheduledStart()
            stopInternal()
        }
    }

    fun retry() {
        if (
            !started || released || !waitingForRetry.get() ||
            !retryScheduled.compareAndSet(false, true)
        ) {
            return
        }

        val currentSession = sessionId
        try {
            analysisExecutor.execute {
                try {
                    if (
                        started && !released && currentSession == sessionId &&
                        waitingForRetry.get()
                    ) {
                        val dispose = faceDispose ?: return@execute
                        val retried = synchronized(SDK_CALL_LOCK) {
                            if (
                                started && !released && currentSession == sessionId &&
                                dispose === faceDispose
                            ) {
                                dispose.retry()
                                true
                            } else {
                                false
                            }
                        }
                        if (!retried) return@execute
                        // SDK 状态重置完成后再放行帧分析，避免首帧与 retry() 并发。
                        nextFrameAnalysisAtMs.set(
                            SystemClock.elapsedRealtime() + SDK_RETRY_SETTLE_MS
                        )
                        frameErrorReported.set(false)
                        waitingForRetry.set(false)
                    }
                } catch (e: Exception) {
                    notifyError("CAPTURE_RETRY_FAILED", e.message ?: "Capture retry failed")
                } finally {
                    retryScheduled.set(false)
                }
            }
        } catch (e: Exception) {
            retryScheduled.set(false)
            notifyError("CAPTURE_RETRY_FAILED", e.message ?: "Capture retry failed")
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

            // CameraX 的解绑/绑定与 Surface 建立不可重入，只保留最后一次切换目标。
            if (cameraTransitionInProgress) {
                pendingCameraId = newCameraId
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

            performCameraSwitch(lifecycleOwner, newCameraId)
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
        runOnMainThread {
            if (released) return@runOnMainThread
            released = true
            restartAfterAttach = false
            cancelScheduledStart()
            stopInternal()
            resultCallback = null
            tipsCallback = null
            errorCallback = null
            cameraChangedCallback = null
            // 等待已排队的 SDK release / Bitmap 回收完成，避免 shutdownNow() 遗留资源。
            analysisExecutor.shutdown()
            resultExecutor.shutdown()
        }
    }

    override fun onDetachedFromWindow() {
        // 标准组件由 onUnmounted、兼容组件由 NVBeforeUnload 负责最终 release。
        // 临时 detach 仅停止相机，并在 attach 后使用原参数恢复，避免返回页面黑屏。
        restartAfterAttach = !released && (started || cameraInitializing || startScheduled)
        cancelScheduledStart()
        stopInternal()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (restartAfterAttach && !released) {
            restartAfterAttach = false
            if (width == 0 || height == 0) {
                scheduleStartAfterLayout()
            } else {
                post { startCameraWhenReady() }
            }
        }
    }

    private fun cancelScheduledStart() {
        scheduledStartGeneration++
        if (startScheduled) {
            removeOnLayoutChangeListener(startOnLayoutListener)
            startScheduled = false
        }
    }

    private fun scheduleStartAfterLayout() {
        if (!startScheduled) {
            scheduledStartGeneration++
            startScheduled = true
            addOnLayoutChangeListener(startOnLayoutListener)
        }
    }

    private fun queueFaceDisposeCreation(currentSession: Long) {
        try {
            SDK_LIFECYCLE_EXECUTOR.execute {
                try {
                    val newDispose = synchronized(SDK_CALL_LOCK) {
                        ensureFaceSdkInitialized(context.applicationContext)
                        CaptureFaceDispose(
                            context,
                            performanceMode,
                            needLivenessCheck,
                            object : AddFaceCallBack() {
                                override fun onCompleted(
                                    cropped: Bitmap,
                                    silentScore: Float,
                                    origin: Bitmap
                                ) {
                                    markSdkCallback(cropped, origin)
                                    if (
                                        !started || released || currentSession != sessionId
                                    ) {
                                        recycleResultBitmaps(cropped, origin)
                                        return
                                    }
                                    if (!waitingForRetry.compareAndSet(false, true)) {
                                        recycleResultBitmaps(cropped, origin)
                                        return
                                    }
                                    encodeAndDispatch(
                                        currentSession,
                                        cropped,
                                        silentScore,
                                        origin
                                    )
                                }

                                override fun onProcessTips(actionCode: Int) {
                                    markSdkCallback()
                                    if (started && !released && currentSession == sessionId) {
                                        enqueueProcessTips(currentSession, actionCode)
                                    }
                                }
                            }
                        )
                    }

                    if (started && !released && currentSession == sessionId) {
                        faceDispose = newDispose
                    } else {
                        synchronized(SDK_CALL_LOCK) { newDispose.release() }
                    }
                } catch (e: LinkageError) {
                    failSdkInitialization(currentSession, e)
                } catch (e: Exception) {
                    failSdkInitialization(currentSession, e)
                }
            }
        } catch (e: RejectedExecutionException) {
            failSdkInitialization(currentSession, e)
        }
    }

    private fun failSdkInitialization(currentSession: Long, error: Throwable) {
        post {
            if (started && !released && currentSession == sessionId) {
                stopInternal()
                notifyError("SDK_INIT_FAILED", error.message ?: "Capture SDK initialization failed")
            }
        }
    }

    private fun enqueueProcessTips(currentSession: Long, actionCode: Int) {
        pendingTipsSession.set(currentSession)
        pendingTipsCode.set(actionCode)
        if (tipsDispatchScheduled.compareAndSet(false, true)) {
            postDelayed(tipsDispatchRunnable, TIPS_DISPATCH_INTERVAL_MS)
        }
    }

    private fun markSdkCallback(vararg resultBitmaps: Bitmap) {
        lastSdkCallbackAtMs.set(SystemClock.elapsedRealtime())
        frameTimeoutReported.set(false)
        resultBitmaps.forEach { bitmap -> retainedFrameBitmaps.remove(bitmap) }
    }

    private fun retainFrameBitmap(bitmap: Bitmap) {
        retainedFrameBitmaps.add(bitmap)
        postDelayed({
            if (retainedFrameBitmaps.remove(bitmap)) {
                recycleBitmap(bitmap)
            }
        }, FRAME_BITMAP_RETENTION_MS)
    }

    private fun discardFrameBitmap(bitmap: Bitmap?) {
        if (bitmap == null) return
        retainedFrameBitmaps.remove(bitmap)
        recycleBitmap(bitmap)
    }

    private fun resetSdkAfterCallbackTimeout(
        currentSession: Long,
        dispose: CaptureFaceDispose
    ): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSdkCallbackAtMs.get() < FRAME_PROCESS_TIMEOUT_MS) return false

        if (frameTimeoutReported.compareAndSet(false, true)) {
            notifyError(
                "FRAME_PROCESS_TIMEOUT",
                "Face detector produced no feedback for 10 seconds and was reset"
            )
        }
        return synchronized(SDK_CALL_LOCK) {
            if (
                started && !released && currentSession == sessionId &&
                dispose === faceDispose
            ) {
                dispose.retry()
                lastSdkCallbackAtMs.set(now)
                nextFrameAnalysisAtMs.set(now + SDK_RETRY_SETTLE_MS)
                true
            } else {
                false
            }
        }
    }

    private fun reserveFrameAnalysisSlot(): Boolean {
        val now = SystemClock.elapsedRealtime()
        while (true) {
            val nextAllowed = nextFrameAnalysisAtMs.get()
            if (now < nextAllowed) return false
            if (nextFrameAnalysisAtMs.compareAndSet(nextAllowed, now + FRAME_ANALYSIS_INTERVAL_MS)) {
                return true
            }
        }
    }

    private fun performCameraSwitch(lifecycleOwner: LifecycleOwner, newCameraId: Int) {
        cameraTransitionInProgress = true
        previewStreaming = false
        previewFallbackTried = false
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        if (!bindCamera(
                lifecycleOwner,
                sessionId,
                newCameraId,
                allowFallback = false,
                notifySwitch = true
            )
        ) {
            cameraTransitionInProgress = false
            drainPendingCameraSwitch()
        }
    }

    private fun drainPendingCameraSwitch() {
        if (cameraTransitionInProgress || !started || released) return
        val targetCameraId = pendingCameraId ?: return
        pendingCameraId = null
        if (targetCameraId == cameraId) return
        val lifecycleOwner = findActivity() as? LifecycleOwner ?: return
        performCameraSwitch(lifecycleOwner, targetCameraId)
    }

    private fun stopForReplacement() {
        runOnMainThread {
            if (released) return@runOnMainThread
            restartAfterAttach = false
            cancelScheduledStart()
            stopInternal()
            notifyError(
                "CAPTURE_SESSION_REPLACED",
                "Another capture component has taken over the camera"
            )
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
            cameraTransitionInProgress = false
            notifyError(
                "CAMERA_NOT_AVAILABLE",
                "Requested camera is not available: $requestedCameraId"
            )
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
                    var frameBitmap: Bitmap? = null
                    try {
                        if (
                            started && !released && currentSession == sessionId &&
                            analyzerGeneration == cameraBindingGeneration &&
                            !encodingResult.get() && !waitingForRetry.get() &&
                            reserveFrameAnalysisSlot()
                        ) {
                            val dispose = faceDispose
                            if (dispose != null) {
                                if (resetSdkAfterCallbackTimeout(currentSession, dispose)) return
                                frameBitmap = DataConvertUtils.imageProxy2Bitmap(imageProxy)
                                retainFrameBitmap(frameBitmap)
                                synchronized(SDK_CALL_LOCK) {
                                    if (
                                        started && !released && currentSession == sessionId &&
                                        dispose === faceDispose
                                    ) {
                                        frameBitmap?.let { bitmap ->
                                            dispose.dispose(bitmap)
                                            // SDK 异步持有该 Bitmap，所有权已移交给 SDK。
                                            frameBitmap = null
                                            frameErrorReported.set(false)
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: LinkageError) {
                        discardFrameBitmap(frameBitmap)
                        if (frameErrorReported.compareAndSet(false, true)) {
                            notifyError(
                                "FRAME_PROCESS_FAILED",
                                e.message ?: "Camera frame processing dependency failed"
                            )
                        }
                    } catch (e: Exception) {
                        discardFrameBitmap(frameBitmap)
                        // 连续帧失败只上报一次，避免错误事件淹没主线程和 JS bridge。
                        if (frameErrorReported.compareAndSet(false, true)) {
                            notifyError(
                                "FRAME_PROCESS_FAILED",
                                e.message ?: "Camera frame processing failed"
                            )
                        }
                    } finally {
                        discardFrameBitmap(frameBitmap)
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
            // 首次初始化也回传最终绑定的镜头，调用方可确认传入的 cameraId 是否生效，
            // 或设备缺少目标镜头时是否发生了自动降级。
            notifyCameraChanged(actualCameraId)
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
            cameraTransitionInProgress = false
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
            cameraTransitionInProgress = false
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
                if (previewStreaming) {
                    cameraTransitionInProgress = false
                    drainPendingCameraSwitch()
                }
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
                cameraTransitionInProgress = true
                if (!bindCamera(
                    lifecycleOwner,
                    currentSession,
                    cameraId,
                    allowFallback = true,
                    notifySwitch = false
                )) {
                    stopInternal()
                }
            } else {
                stopInternal()
                notifyError(
                    "CAMERA_PREVIEW_NOT_STREAMING",
                    "Camera opened but preview did not start streaming"
                )
            }
        }, PREVIEW_START_TIMEOUT_MS)
    }

    private fun encodeAndDispatch(
        currentSession: Long,
        cropped: Bitmap,
        silentScore: Float,
        origin: Bitmap
    ) {
        if (!encodingResult.compareAndSet(false, true)) {
            recycleResultBitmaps(cropped, origin)
            return
        }

        try {
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
                    recycleResultBitmaps(cropped, origin)
                    encodingResult.set(false)
                }
            }
        } catch (e: RejectedExecutionException) {
            recycleResultBitmaps(cropped, origin)
            encodingResult.set(false)
            if (!released) {
                notifyError("BITMAP_ENCODE_FAILED", "Bitmap encoder is not available")
            }
        }
    }

    private fun stopInternal() {
        started = false
        cameraInitializing = false
        unregisterDisplayListener()
        sessionId++
        cameraBindingGeneration++
        previewStreaming = false
        previewFallbackTried = false
        cameraTransitionInProgress = false
        pendingCameraId = null
        waitingForRetry.set(false)
        retryScheduled.set(false)
        nextFrameAnalysisAtMs.set(0L)
        frameErrorReported.set(false)
        frameTimeoutReported.set(false)
        lastSdkCallbackAtMs.set(0L)
        val framesToRecycleAfterRelease = retainedFrameBitmaps.toList()
        retainedFrameBitmaps.clear()
        pendingTipsCode.set(NO_PENDING_TIPS)
        pendingTipsSession.set(NO_SESSION)
        tipsDispatchScheduled.set(false)
        removeCallbacks(tipsDispatchRunnable)
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
        val disposeToRelease = faceDispose
        faceDispose = null
        clearActiveInstance(this)
        if (disposeToRelease != null) {
            try {
                SDK_LIFECYCLE_EXECUTOR.execute {
                    synchronized(SDK_CALL_LOCK) {
                        try {
                            disposeToRelease.release()
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to release capture SDK", e)
                        } finally {
                            framesToRecycleAfterRelease.forEach { bitmap ->
                                recycleBitmap(bitmap)
                            }
                        }
                    }
                }
            } catch (e: RejectedExecutionException) {
                Log.w(TAG, "Capture SDK executor is already closed", e)
                framesToRecycleAfterRelease.forEach { bitmap -> recycleBitmap(bitmap) }
            }
        } else {
            framesToRecycleAfterRelease.forEach { bitmap -> recycleBitmap(bitmap) }
        }
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
        if (textRes != 0) {
            faceCoverTipsView.text = message
            applyFaceCoverTipsVisibility()
        }
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

    private fun applyPreviewScaleType() {
        previewView.scaleType = if (forceFitCenterPreview) {
            PreviewView.ScaleType.FIT_CENTER
        } else {
            PreviewView.ScaleType.FILL_CENTER
        }
    }

    private fun applyFaceCoverTipsVisibility() {
        faceCoverTipsView.visibility = if (faceCoverTipsVisible) View.VISIBLE else View.GONE
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

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

    private fun recycleResultBitmaps(cropped: Bitmap?, origin: Bitmap?) {
        recycleBitmap(cropped)
        if (origin !== cropped) recycleBitmap(origin)
    }

    private fun recycleBitmap(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        try {
            bitmap.recycle()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to recycle capture bitmap", e)
        }
    }

    private data class CameraSelection(
        val cameraSelector: CameraSelector,
        val cameraId: Int
    )

    private companion object {
        const val TAG = "CaptureFaceNativeView"
        const val PREVIEW_START_TIMEOUT_MS = 2500L
        const val SDK_START_SETTLE_MS = 700L
        const val SDK_RETRY_SETTLE_MS = 120L
        const val FRAME_ANALYSIS_INTERVAL_MS = 333L
        const val FRAME_PROCESS_TIMEOUT_MS = 10_000L
        const val FRAME_BITMAP_RETENTION_MS = 2_000L
        const val TIPS_DISPATCH_INTERVAL_MS = 250L
        const val NO_PENDING_TIPS = Int.MIN_VALUE
        const val NO_SESSION = -1L
        const val AUTO_ROTATION_DEGREES = -1
        const val FACE_COVER_MARGIN_WITH_TIPS_DIVISOR = 13
        const val FACE_COVER_MARGIN_WITHOUT_TIPS_DP = 4
        const val FACE_COVER_TIPS_GAP_DP = 5
        const val FACE_COVER_SDK_VERTICAL_OFFSET_DIVISOR = 8

        private val SDK_CALL_LOCK = Any()
        private val SDK_LIFECYCLE_EXECUTOR: ExecutorService =
            Executors.newSingleThreadExecutor()
        private var faceSdkInitialized = false
        private var activeInstance: WeakReference<CaptureFaceNativeView>? = null

        @Synchronized
        private fun ensureFaceSdkInitialized(appContext: Context) {
            if (faceSdkInitialized) return
            FaceSDKConfig.init(appContext)
            faceSdkInitialized = true
        }

        @Synchronized
        private fun claimActiveInstance(
            instance: CaptureFaceNativeView
        ): CaptureFaceNativeView? {
            val previous = activeInstance?.get()
            activeInstance = WeakReference(instance)
            return previous?.takeIf { it !== instance }
        }

        @Synchronized
        private fun clearActiveInstance(instance: CaptureFaceNativeView) {
            if (activeInstance?.get() === instance) {
                activeInstance = null
            }
        }
    }
}
