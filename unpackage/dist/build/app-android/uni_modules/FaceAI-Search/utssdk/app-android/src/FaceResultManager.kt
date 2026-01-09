package uts.sdk.modules.uniFaceAISDK

/**
 * 全局单例管理器，用于连接 Activity 和 UTS
 */
object FaceResultManager {
    // 1. 确认接口名称是 OnFaceResultListener
    interface OnFaceResultListener {
        fun onResult(json: String)
    }

    // 持有监听器的引用
    var listener: OnFaceResultListener? = null

    // 2. 【新增】显式定义一个设置方法，供 UTS 调用
    fun setCallback(listener: OnFaceResultListener) {
        this.listener = listener
    }

    // Activity 调用此方法发送数据
    fun sendResult(json: String) {
        listener?.onResult(json)
    }
}