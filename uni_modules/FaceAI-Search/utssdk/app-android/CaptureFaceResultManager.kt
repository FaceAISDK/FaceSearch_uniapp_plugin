package uts.sdk.modules.uniFaceAISDK

/**
 * 连接全屏 CaptureFaceActivity 与 UTS API。
 * 页面内嵌组件使用 CaptureFaceNativeView 的实例回调，不经过此全局管理器。
 */
object CaptureFaceResultManager {
    private var resultCallback: ((String, Float, String) -> Unit)? = null
    private var errorCallback: ((String, String) -> Unit)? = null

    fun setResultCallback(callback: (String, Float, String) -> Unit) {
        resultCallback = callback
    }

    fun setErrorCallback(callback: (String, String) -> Unit) {
        errorCallback = callback
    }

    fun sendResult(croppedBase64: String, silentScore: Float, originBase64: String) {
        resultCallback?.invoke(croppedBase64, silentScore, originBase64)
    }

    fun sendError(code: String, message: String) {
        errorCallback?.invoke(code, message)
    }

    fun clear() {
        resultCallback = null
        errorCallback = null
    }
}
