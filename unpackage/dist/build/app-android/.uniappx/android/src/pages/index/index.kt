@file:Suppress("UNCHECKED_CAST", "USELESS_CAST", "INAPPLICABLE_JVM_NAME", "UNUSED_ANONYMOUS_PARAMETER", "NAME_SHADOWING", "UNNECESSARY_NOT_NULL_ASSERTION")
package uni.UNIC3F93BB
import io.dcloud.uniapp.*
import io.dcloud.uniapp.extapi.*
import io.dcloud.uniapp.framework.*
import io.dcloud.uniapp.runtime.*
import io.dcloud.uniapp.vue.*
import io.dcloud.uniapp.vue.shared.*
import io.dcloud.uts.*
import io.dcloud.uts.Map
import io.dcloud.uts.Set
import io.dcloud.uts.UTSAndroid
import kotlin.properties.Delegates
import uts.sdk.modules.FaceAISearch.startFaceSearch
import uts.sdk.modules.FaceAISearch.insertFaceSearchFeature
import uts.sdk.modules.FaceAISearch.insertManyFeatures
import uts.sdk.modules.FaceAISearch.addFaceSearchFeature
import uts.sdk.modules.FaceAISearch.deleteFaceSearchFeature
import uts.sdk.modules.FaceAISearch.faceSearch
import uts.sdk.modules.FaceAISearch.ResultJSON
open class GenPagesIndexIndex : BasePage {
    constructor(__ins: ComponentInternalInstance, __renderer: String?) : super(__ins, __renderer) {
        onLoad(fun(_: OnLoadOptions) {}, __ins)
    }
    @Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")
    override fun `$render`(): Any? {
        val _ctx = this
        val _cache = this.`$`.renderCache
        return _cE("view", null, _uA(
            _cE("button", _uM("class" to "gray-button", "onClick" to _ctx.startFaceSearchDemo), "打开持续人脸搜索", 8, _uA(
                "onClick"
            )),
            _cE("button", _uM("class" to "gray-button", "onClick" to _ctx.faceSearchDemo), "1:N人脸搜索识别", 8, _uA(
                "onClick"
            )),
            _cE("button", _uM("class" to "gray-button", "onClick" to _ctx.addFaceSearchFeatureDemo), "1:N人脸搜索录入人脸", 8, _uA(
                "onClick"
            )),
            _cE("button", _uM("class" to "gray-button", "onClick" to _ctx.deleteFaceSearchFeatureDemo), "删除人脸搜索特征值", 8, _uA(
                "onClick"
            )),
            _cE("button", _uM("class" to "gray-button", "onClick" to _ctx.insertFaceSearchFeatureDemo), "同步人脸搜索特征值", 8, _uA(
                "onClick"
            )),
            _cE("button", _uM("class" to "gray-button", "onClick" to _ctx.insertManyFaceFeatureSDemo), "批量同步人脸搜索特征值", 8, _uA(
                "onClick"
            )),
            _cE("view", _uM("class" to "result-box"), _uA(
                _cE("view", null, " Email: FaceAISDK.Service@gmail.com"),
                _cE("scroll-view", _uM("scroll-y" to "true", "class" to "scroll-view-box"), _uA(
                    _cE("text", _uM("class" to "text-content"), _tD(_ctx.faceAIResult), 1)
                ))
            ))
        ))
    }
    open var faceID: String by `$data`
    open var faceFeature: String by `$data`
    open var faceJSONFeatures: String by `$data`
    open var faceAIResult: String by `$data`
    @Suppress("USELESS_CAST")
    override fun data(): Map<String, Any?> {
        return _uM("faceID" to "Test", "faceFeature" to "faceFeature is a string with lenth 1024", "faceJSONFeatures" to "faceFeature is a string with lenth 1024", "faceAIResult" to "faceAIResult")
    }
    open var startFaceSearchDemo = ::gen_startFaceSearchDemo_fn
    open fun gen_startFaceSearchDemo_fn() {
        startFaceSearch(fun(jsonStr: String){
            console.log("收到搜索结果:", jsonStr)
            this.faceAIResult = "【人脸搜索回调】\n" + jsonStr
        }
        )
    }
    open var faceSearchDemo = ::gen_faceSearchDemo_fn
    open fun gen_faceSearchDemo_fn() {
        faceSearch(fun(result: ResultJSON){
            this.faceAIResult = JSON.stringify(result)
        }
        )
    }
    open var addFaceSearchFeatureDemo = ::gen_addFaceSearchFeatureDemo_fn
    open fun gen_addFaceSearchFeatureDemo_fn() {
        addFaceSearchFeature(this.faceID, 1, fun(result: ResultJSON){
            this.faceAIResult = JSON.stringify(result)
        }
        )
    }
    open var deleteFaceSearchFeatureDemo = ::gen_deleteFaceSearchFeatureDemo_fn
    open fun gen_deleteFaceSearchFeatureDemo_fn() {
        deleteFaceSearchFeature(this.faceID, fun(result: ResultJSON){
            this.faceAIResult = JSON.stringify(result)
        }
        )
    }
    open var insertFaceSearchFeatureDemo = ::gen_insertFaceSearchFeatureDemo_fn
    open fun gen_insertFaceSearchFeatureDemo_fn() {
        insertFaceSearchFeature(this.faceID, this.faceFeature, "tag", "group", fun(result: ResultJSON){
            this.faceAIResult = JSON.stringify(result)
        }
        )
    }
    open var insertManyFaceFeatureSDemo = ::gen_insertManyFaceFeatureSDemo_fn
    open fun gen_insertManyFaceFeatureSDemo_fn() {
        insertManyFeatures(JSON_FACE_FEATURES_DATA, fun(result: ResultJSON){
            this.faceAIResult = JSON.stringify(result)
        }
        )
    }
    companion object {
        val styles: Map<String, Map<String, Map<String, Any>>> by lazy {
            _nCS(_uA(
                styles0
            ), _uA(
                GenApp.styles
            ))
        }
        val styles0: Map<String, Map<String, Map<String, Any>>>
            get() {
                return _uM("result-box" to _pS(_uM("marginTop" to "20rpx", "marginRight" to "20rpx", "marginBottom" to "20rpx", "marginLeft" to "20rpx")), "scroll-view-box" to _pS(_uM("height" to "400rpx", "borderTopWidth" to 1, "borderRightWidth" to 1, "borderBottomWidth" to 1, "borderLeftWidth" to 1, "borderTopStyle" to "solid", "borderRightStyle" to "solid", "borderBottomStyle" to "solid", "borderLeftStyle" to "solid", "borderTopColor" to "#cccccc", "borderRightColor" to "#cccccc", "borderBottomColor" to "#cccccc", "borderLeftColor" to "#cccccc", "borderTopLeftRadius" to "10rpx", "borderTopRightRadius" to "10rpx", "borderBottomRightRadius" to "10rpx", "borderBottomLeftRadius" to "10rpx", "backgroundColor" to "#f8f8f8", "paddingTop" to "15rpx", "paddingRight" to "15rpx", "paddingBottom" to "15rpx", "paddingLeft" to "15rpx", "boxSizing" to "border-box")), "text-content" to _pS(_uM("fontSize" to "28rpx", "color" to "#333333", "whiteSpace" to "pre-wrap")), "gray-button" to _pS(_uM("backgroundColor" to "#ffffff", "color" to "#800080", "borderTopWidth" to "medium", "borderRightWidth" to "medium", "borderBottomWidth" to "medium", "borderLeftWidth" to "medium", "borderTopStyle" to "none", "borderRightStyle" to "none", "borderBottomStyle" to "none", "borderLeftStyle" to "none", "borderTopColor" to "#000000", "borderRightColor" to "#000000", "borderBottomColor" to "#000000", "borderLeftColor" to "#000000")))
            }
        var inheritAttrs = true
        var inject: Map<String, Map<String, Any?>> = _uM()
        var emits: Map<String, Any?> = _uM()
        var props = _nP(_uM())
        var propsNeedCastKeys: UTSArray<String> = _uA()
        var components: Map<String, CreateVueComponent> = _uM()
    }
}
