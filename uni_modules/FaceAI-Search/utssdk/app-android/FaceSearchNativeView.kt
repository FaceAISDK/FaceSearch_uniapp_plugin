package uts.sdk.modules.uniFaceAISDK

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.hardware.display.DisplayManager
import android.os.Looper
import android.text.TextUtils
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
import com.ai.face.core.utils.FaceAICameraType
import com.ai.face.faceSearch.search.FaceSearchEngine
import com.ai.face.faceSearch.search.GraphicOverlay
import com.ai.face.faceSearch.search.SearchProcessBuilder
import com.ai.face.faceSearch.search.SearchProcessCallBack
import com.ai.face.faceSearch.search.SearchProcessTipsCode
import com.ai.face.faceSearch.utils.FaceSearchResult
import com.faceAI.demo.FaceSDKConfig
import com.faceAI.demo.R
import com.faceAI.demo.base.utils.BitmapUtils
import com.faceAI.demo.base.view.FaceCoverView
import com.google.gson.Gson
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 可嵌入页面的 1:N 相机人脸搜索 View，供标准模式和兼容模式组件共用
 * @author FaceAISDK.Service@gmail.com
 */
@Keep
class FaceSearchNativeView(context: Context) : FrameLayout(context) {

    private val previewView = PreviewView(context)
    private val graphicOverlay = GraphicOverlay(context, null)
    private val faceCoverView = FaceCoverView(context)
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val oneTimeResultSent = AtomicBoolean(false)

    @Volatile
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraControl: CameraControl? = null
    private var boundPreview: Preview? = null
    private var boundImageAnalysis: ImageAnalysis? = null
    private var boundCameraSelector: CameraSelector? = null
    private var resultCallback: ((String, Float, String) -> Unit)? = null
    private var tipsCallback: ((Int, String) -> Unit)? = null
    private var errorCallback: ((String, String) -> Unit)? = null
    private var cameraChangedCallback: ((Int) -> Unit)? = null

    private var searchThreshold = 0.86f
    private var searchOneTime = false
    private var searchTimeOut = 4000
    private var needLivenessCheck = true
    @Volatile
    private var cameraId = CameraSelector.LENS_FACING_FRONT
    private var linearZoom = 0.12f
    private var rotationDegrees = AUTO_ROTATION_DEGREES
    @Volatile
    private var faceCoverVisible = true
    private var faceCoverLayoutReady = false
    private var searchStartTime = 0L
    @Volatile
    private var started = false
    @Volatile
    private var released = false
    @Volatile
    private var sessionId = 0L
    private var startScheduled = false
    private var previewStreaming = false
    private var previewFallbackTried = false
    @Volatile
    private var cameraBindingGeneration = 0L
    @Volatile
    private var overlayInfoGeneration = 0L
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
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        faceCoverView.alpha = 0f
        faceCoverView.visibility = View.INVISIBLE
        faceCoverView.addOnLayoutChangeListener { _, left, top, right, bottom,
                                                  oldLeft, oldTop, oldRight, oldBottom ->
            if (
                right > left && bottom > top &&
                (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop)
            ) {
                updateEmbeddedFaceCoverLayout()
            }
        }
        addView(previewView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(graphicOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(faceCoverView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyFaceCoverVisibility()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (width <= 0 || height <= 0) return

        faceCoverLayoutReady = false
        faceCoverView.alpha = 0f
        if (faceCoverVisible) faceCoverView.visibility = View.INVISIBLE
    }

    private fun updateEmbeddedFaceCoverLayout() {
        val width = faceCoverView.width
        val height = faceCoverView.height
        if (width <= 0 || height <= 0) return

        // 与全屏 UTS API 保持相同圆框比例：竖屏短边留 1/8，横屏留 1/9。
        val marginDivisor = if (height > width) 8 else 7
        faceCoverView.setMargin(minOf(width, height) / marginDivisor) //这个margin应该是距离更短边的Margin

        if (height > width) {
            // SDK 竖屏默认把圆心向上偏移短边的 1/8；嵌入组件时恢复垂直居中。
            val offsetY = height / 2f - faceCoverView.mCenterPoint.y
            faceCoverView.mCenterPoint.offset(0f, offsetY)
            faceCoverView.mArcRectF.offset(0f, offsetY)
            faceCoverView.mCirclePaddingBottom = 0
            faceCoverView.mGradientMatrix.setRotate(
                270f,
                faceCoverView.mCenterPoint.x,
                faceCoverView.mCenterPoint.y
            )
            faceCoverView.mProgressPaint.shader?.setLocalMatrix(faceCoverView.mGradientMatrix)
        }

        faceCoverLayoutReady = true
        applyFaceCoverVisibility()
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
            if (!released) applyFaceCoverVisibility()
        }
    }

    /** 重复调用会使用最新参数重启搜索会话。 */
    @JvmOverloads
    fun start(
        searchThreshold: Float = 0.86f,
        searchOneTime: Boolean = false,
        searchTimeOut: Int = 4000,
        needLivenessCheck: Boolean = true,
        cameraId: Int = CameraSelector.LENS_FACING_FRONT,
        linearZoom: Float = 0.12f,
        rotationDegrees: Int = AUTO_ROTATION_DEGREES
    ) {
        if (released) {
            notifyError("VIEW_RELEASED", "FaceSearchNativeView has been released")
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

        this.searchThreshold = searchThreshold
        this.searchOneTime = searchOneTime
        this.searchTimeOut = searchTimeOut.coerceIn(3000, 6000)
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

    fun stop() {
        if (released) return
        cancelScheduledStart()
        stopInternal()
    }

    fun toggleCamera() {
        val targetCameraId = if (cameraId == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        switchCamera(targetCameraId)
    }

    fun switchCamera(newCameraId: Int) {
        if (!isSupportedCameraId(newCameraId)) {
            notifyError("INVALID_CAMERA_ID", "cameraId must be 0 (front) or 1 (back)")
            return
        }

        runOnMainThread {
            if (released) return@runOnMainThread
            if (newCameraId == cameraId && cameraControl != null) return@runOnMainThread
            if (!started || cameraProvider == null) {
                cameraId = newCameraId
                return@runOnMainThread
            }

            val lifecycleOwner = findActivity() as? LifecycleOwner
            if (lifecycleOwner == null) {
                notifyError("LIFECYCLE_OWNER_REQUIRED", "The component host must implement LifecycleOwner")
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
    }

    override fun onDetachedFromWindow() {
        cancelScheduledStart()
        stopInternal()
        super.onDetachedFromWindow()
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
        started = true
        registerDisplayListener()
        previewStreaming = false
        previewFallbackTried = false
        previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        oneTimeResultSent.set(false)
        val currentSession = ++sessionId
        FaceSDKConfig.init(context)

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (!started || released || currentSession != sessionId) return@addListener
            try {
                cameraProvider = providerFuture.get()
                bindCamera(lifecycleOwner, currentSession)
            } catch (e: Exception) {
                notifyError("CAMERA_INIT_FAILED", e.message ?: "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun scheduleStartAfterLayout() {
        if (!startScheduled) {
            startScheduled = true
            addOnLayoutChangeListener(startOnLayoutListener)
        }
    }

    private fun cancelScheduledStart() {
        if (startScheduled) {
            removeOnLayoutChangeListener(startOnLayoutListener)
            startScheduled = false
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
        val selection = createCompatibleCameraSelector(provider, requestedCameraId, allowFallback)
        if (selection == null) {
            if (notifySwitch) {
                notifyError("CAMERA_NOT_AVAILABLE", "Requested camera is not available: $requestedCameraId")
            }
            return false
        }

        val surfaceRotation = resolveSurfaceRotation()
        val newPreview = Preview.Builder().setTargetRotation(surfaceRotation).build()
        newPreview.setSurfaceProvider(previewView.surfaceProvider)
        val newImageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setTargetRotation(surfaceRotation)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()

        val previousBindingGeneration = cameraBindingGeneration
        val analyzerGeneration = previousBindingGeneration + 1
        newImageAnalysis.setAnalyzer(
            analysisExecutor,
            object : ImageAnalysis.Analyzer {
                override fun analyze(imageProxy: ImageProxy) {
                    try {
                        if (
                            started && !released && currentSession == sessionId &&
                            analyzerGeneration == cameraBindingGeneration
                        ) {
                            if (overlayInfoGeneration != analyzerGeneration) {
                                overlayInfoGeneration = analyzerGeneration
                                val imageWidth = imageProxy.width
                                val imageHeight = imageProxy.height
                                post {
                                    if (isCurrentBinding(currentSession, analyzerGeneration)) {
                                        graphicOverlay.setCameraInfo(
                                            imageWidth,
                                            imageHeight,
                                            cameraId == CameraSelector.LENS_FACING_FRONT
                                        )
                                    }
                                }
                            }
                            FaceSearchEngine.getInstance().runSearchWithImageProxy(imageProxy, 0)
                        }
                    } catch (e: Exception) {
                        notifyError("FRAME_PROCESS_FAILED", e.message ?: "Camera frame processing failed")
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
        val previousCameraId = cameraId
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
            if (isSupportedCameraId(reportedCameraId)) actualCameraId = reportedCameraId

            boundPreview = newPreview
            boundImageAnalysis = newImageAnalysis
            boundCameraSelector = selection.cameraSelector
            cameraControl = camera.cameraControl
            cameraId = actualCameraId
            initSearchProcess(
                lifecycleOwner,
                currentSession,
                analyzerGeneration,
                actualCameraId
            )
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
            try {
                provider.unbind(newPreview, newImageAnalysis)
            } catch (_: Exception) {
            }
            boundPreview = previousPreview
            boundImageAnalysis = previousImageAnalysis
            boundCameraSelector = previousCameraSelector
            cameraId = previousCameraId
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
        } catch (e: Exception) {
            newImageAnalysis.clearAnalyzer()
            try {
                provider.unbind(newPreview, newImageAnalysis)
            } catch (_: Exception) {
            }
            boundPreview = previousPreview
            boundImageAnalysis = previousImageAnalysis
            boundCameraSelector = previousCameraSelector
            cameraId = previousCameraId
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
        }
        return false
    }

    private fun initSearchProcess(
        lifecycleOwner: LifecycleOwner,
        currentSession: Long,
        currentBindingGeneration: Long,
        actualCameraId: Int
    ) {
        FaceSearchEngine.getInstance().stopSearchProcess()
        val builder = SearchProcessBuilder.Builder(context)
            .setLifecycleOwner(lifecycleOwner)
            .setCameraType(FaceAICameraType.SYSTEM_CAMERA)
            .setNeedFaceLiveness(needLivenessCheck)
            .setSearchType(SearchProcessBuilder.SearchType.N_SEARCH_1)
            .setThreshold(searchThreshold)
            .setCallBackAllMatch(true)
            .setSearchIntervalTime(1700L)
            .setSearchTimeOut(searchTimeOut.toLong())
            .setMirror(actualCameraId == CameraSelector.LENS_FACING_FRONT)
            .setProcessCallBack(object : SearchProcessCallBack() {
                override fun onFaceMatched(
                    results: MutableList<FaceSearchResult>?,
                    bitmap: Bitmap?,
                    liveness: Float
                ) {
                    if (
                        !isCurrentBinding(currentSession, currentBindingGeneration) ||
                        bitmap == null
                    ) return
                    val matchedResults = results ?: mutableListOf()
                    matchedResults.removeAll {
                        TextUtils.isEmpty(it.faceName) && it.faceScore == 0.0f
                    }
                    dispatchResult(
                        currentSession,
                        currentBindingGeneration,
                        Gson().toJson(matchedResults),
                        liveness,
                        bitmap
                    )
                }

                override fun onFaceDetected(results: MutableList<FaceSearchResult>?) {
                    val detectedResults = if (results == null) null else ArrayList(results)
                    post {
                        if (!isCurrentBinding(currentSession, currentBindingGeneration)) return@post
                        if (detectedResults == null) graphicOverlay.clearRect()
                        else graphicOverlay.drawRect(detectedResults)
                    }
                }

                override fun onProcessTips(code: Int) {
                    post {
                        if (isCurrentBinding(currentSession, currentBindingGeneration)) {
                            showProcessTips(currentSession, currentBindingGeneration, code)
                        }
                    }
                }
            })
            .create()
        FaceSearchEngine.getInstance().initSearchParams(builder)
        searchStartTime = System.currentTimeMillis()
    }

    private fun dispatchResult(
        currentSession: Long,
        currentBindingGeneration: Long,
        matchesJson: String,
        liveness: Float,
        bitmap: Bitmap
    ) {
        if (searchOneTime && !oneTimeResultSent.compareAndSet(false, true)) return
        try {
            val base64 = BitmapUtils.bitmapToBase64(bitmap)
            post {
                if (!isCurrentBinding(currentSession, currentBindingGeneration)) {
                    if (searchOneTime && isCurrentSession(currentSession)) {
                        oneTimeResultSent.set(false)
                    }
                    return@post
                }
                try {
                    resultCallback?.invoke(matchesJson, liveness, base64)
                } catch (e: Exception) {
                    Log.e(TAG, "Result callback failed", e)
                }
                if (searchOneTime) stopInternal()
            }
        } catch (e: Exception) {
            if (searchOneTime) oneTimeResultSent.set(false)
            notifyError("BITMAP_ENCODE_FAILED", e.message ?: "Bitmap Base64 encode failed")
        }
    }

    private fun dispatchEmptyResult(currentSession: Long, currentBindingGeneration: Long) {
        if (!isCurrentBinding(currentSession, currentBindingGeneration)) return
        if (searchOneTime && !oneTimeResultSent.compareAndSet(false, true)) return
        try {
            resultCallback?.invoke("[]", 0.0f, "")
        } catch (e: Exception) {
            Log.e(TAG, "Empty result callback failed", e)
        }
        if (searchOneTime) stopInternal()
    }

    private fun showProcessTips(
        currentSession: Long,
        currentBindingGeneration: Long,
        code: Int
    ) {
        var primaryTextRes = 0
        var secondaryTextRes = 0
        when (code) {
            SearchProcessTipsCode.NO_MATCHED -> {
                secondaryTextRes = R.string.no_matched_face
                if (!searchOneTime || System.currentTimeMillis() - searchStartTime >= searchTimeOut) {
                    dispatchEmptyResult(currentSession, currentBindingGeneration)
                }
            }
            SearchProcessTipsCode.FACE_ANGLE_NOT_FIT -> secondaryTextRes = R.string.face_angle_not_fit
            SearchProcessTipsCode.LOCAL_FACE_DATABASE_EMPTY ->
                primaryTextRes = R.string.local_face_database_empty
            SearchProcessTipsCode.ENGINE_INITING -> primaryTextRes = R.string.sdk_init
            SearchProcessTipsCode.SEARCH_PREPARED,
            SearchProcessTipsCode.SEARCHING -> primaryTextRes = R.string.keep_face_tips
            SearchProcessTipsCode.NO_LIVE_FACE -> primaryTextRes = R.string.no_face_detected_tips
            SearchProcessTipsCode.FACE_TOO_SMALL -> secondaryTextRes = R.string.come_closer_tips
            SearchProcessTipsCode.FACE_TOO_LARGE -> secondaryTextRes = R.string.far_away_tips
            SearchProcessTipsCode.FACE_SIZE_FIT -> faceCoverView.setSecondTipsText("")
            SearchProcessTipsCode.THRESHOLD_ERROR -> primaryTextRes = R.string.search_threshold_scope_tips
            SearchProcessTipsCode.MASK_DETECTION -> primaryTextRes = R.string.no_mask_please
            else -> if (faceCoverVisible) faceCoverView.setTipsText("Tips Code: $code")
        }

        if (faceCoverVisible) {
            when {
                primaryTextRes != 0 -> faceCoverView.setTipsText(primaryTextRes)
                secondaryTextRes != 0 -> faceCoverView.setSecondTipsText(secondaryTextRes)
            }
        }
        applyFaceCoverVisibility()

        val messageRes = if (primaryTextRes != 0) primaryTextRes else secondaryTextRes
        val message = if (messageRes != 0) context.getString(messageRes) else "Tips Code: $code"
        try {
            tipsCallback?.invoke(code, message)
        } catch (e: Exception) {
            Log.e(TAG, "Tips callback failed", e)
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
                isCurrentSession(currentSession) &&
                currentBindingGeneration == cameraBindingGeneration
            ) {
                previewStreaming = state == PreviewView.StreamState.STREAMING
            }
        }

        postDelayed({
            if (
                !isCurrentSession(currentSession) ||
                currentBindingGeneration != cameraBindingGeneration || previewStreaming
            ) return@postDelayed

            if (!previewFallbackTried) {
                previewFallbackTried = true
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                bindCamera(lifecycleOwner, currentSession, cameraId)
            } else {
                notifyError(
                    "CAMERA_PREVIEW_NOT_STREAMING",
                    "Camera opened but preview did not start streaming"
                )
            }
        }, PREVIEW_START_TIMEOUT_MS)
    }

    private fun stopInternal() {
        started = false
        unregisterDisplayListener()
        sessionId++
        cameraBindingGeneration++
        overlayInfoGeneration = 0L
        previewStreaming = false
        previewFallbackTried = false
        try {
            FaceSearchEngine.getInstance().stopSearchProcess()
            (findActivity() as? LifecycleOwner)?.let {
                previewView.previewStreamState.removeObservers(it)
            }
            cameraProvider?.let { unbindOwnedUseCases(it) }
        } catch (_: Exception) {
        }
        boundImageAnalysis?.clearAnalyzer()
        boundPreview = null
        boundImageAnalysis = null
        boundCameraSelector = null
        cameraControl = null
        cameraProvider = null
        graphicOverlay.clearRect()
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
            hasCamera(provider, preferredLensFacing) ->
                CameraSelection(cameraSelector(preferredLensFacing), preferredLensFacing)
            !allowFallback -> null
            hasCamera(provider, fallbackLensFacing) ->
                CameraSelection(cameraSelector(fallbackLensFacing), fallbackLensFacing)
            else -> CameraSelection(
                CameraSelector.Builder().addCameraFilter { cameraInfos -> cameraInfos }.build(),
                preferredLensFacing
            )
        }
    }

    private fun hasCamera(provider: ProcessCameraProvider, lensFacing: Int): Boolean = try {
        provider.hasCamera(cameraSelector(lensFacing))
    } catch (_: Exception) {
        false
    }

    private fun cameraSelector(lensFacing: Int): CameraSelector =
        CameraSelector.Builder().requireLensFacing(lensFacing).build()

    private fun unbindOwnedUseCases(provider: ProcessCameraProvider) {
        val preview = boundPreview
        val analysis = boundImageAnalysis
        when {
            preview != null && analysis != null -> provider.unbind(preview, analysis)
            preview != null -> provider.unbind(preview)
            analysis != null -> provider.unbind(analysis)
        }
    }

    private fun restorePreviousUseCases(
        provider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        selector: CameraSelector?,
        preview: Preview?,
        analysis: ImageAnalysis?
    ) {
        if (selector == null || preview == null || analysis == null) {
            cameraControl = null
            return
        }
        try {
            val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            cameraControl = camera.cameraControl
        } catch (e: Exception) {
            cameraControl = null
            Log.e(TAG, "Failed to restore previous camera after switch failure", e)
        }
    }

    private fun notifyCameraChanged(newCameraId: Int) {
        try {
            cameraChangedCallback?.invoke(newCameraId)
        } catch (e: Exception) {
            Log.e(TAG, "Camera changed callback failed", e)
        }
    }

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
        if (rotationDegrees != AUTO_ROTATION_DEGREES) return toSurfaceRotation(rotationDegrees)
        return previewView.display?.rotation ?: Surface.ROTATION_0
    }

    private fun updateUseCaseTargetRotation() {
        if (!started || released || rotationDegrees != AUTO_ROTATION_DEGREES) return
        val rotation = resolveSurfaceRotation()
        overlayInfoGeneration = 0L
        boundPreview?.targetRotation = rotation
        boundImageAnalysis?.targetRotation = rotation
    }

    private fun toSurfaceRotation(value: Int): Int = when (value) {
        90 -> Surface.ROTATION_90
        180 -> Surface.ROTATION_180
        270 -> Surface.ROTATION_270
        else -> Surface.ROTATION_0
    }

    private fun isSupportedCameraId(value: Int): Boolean =
        value == CameraSelector.LENS_FACING_FRONT || value == CameraSelector.LENS_FACING_BACK

    private fun isSupportedRotationDegrees(value: Int): Boolean =
        value == AUTO_ROTATION_DEGREES || value == 0 || value == 90 ||
            value == 180 || value == 270

    private fun applyFaceCoverVisibility() {
        if (!faceCoverVisible) {
            faceCoverView.alpha = 0f
            faceCoverView.visibility = View.GONE
            return
        }
        if (!faceCoverLayoutReady) {
            faceCoverView.visibility = View.INVISIBLE
            return
        }

        val needsStableReveal = faceCoverView.visibility != View.VISIBLE || faceCoverView.alpha == 0f
        faceCoverView.visibility = View.VISIBLE
        if (!needsStableReveal) return

        // FaceCoverView 变为可见时会异步播放开圈动画；等该任务入队后直接落到最终帧。
        faceCoverView.post {
            if (released || !faceCoverVisible || !faceCoverLayoutReady) return@post
            faceCoverView.mOpenAnimator?.cancel()
            faceCoverView.mCurrentRadius = faceCoverView.mTargetRadius
            faceCoverView.alpha = 1f
            faceCoverView.invalidate()
        }
    }

    private fun isCurrentSession(currentSession: Long): Boolean =
        started && !released && currentSession == sessionId

    private fun isCurrentBinding(currentSession: Long, currentBindingGeneration: Long): Boolean =
        isCurrentSession(currentSession) && currentBindingGeneration == cameraBindingGeneration

    private fun findActivity(): Activity? {
        var currentContext: Context? = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return currentContext as? Activity
    }

    private fun runOnMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else post(action)
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
        const val TAG = "FaceSearchNativeView"
        const val PREVIEW_START_TIMEOUT_MS = 2500L
        const val AUTO_ROTATION_DEGREES = -1
    }
}
