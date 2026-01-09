package uts.sdk.modules.uniFaceAISDK

/**
 * 全局单例管理器，用于连接 Activity 和 UTS
 */
object FaceResultManager {

    // 1. 【修改】改名为 internalCallback 并设为 private
    // 这样就不会自动生成冲突的 setCallback 方法了
    private var internalCallback: ((String) -> Unit)? = null

    // 2. 【保持不变】供 UTS 调用的设置方法
    fun setCallback(cb: (String) -> Unit) {
        this.internalCallback = cb
    }

    // 3. 【修改】发送数据时，调用新的变量名
    fun sendResult(json: String) {
        internalCallback?.invoke(json)
    }
}