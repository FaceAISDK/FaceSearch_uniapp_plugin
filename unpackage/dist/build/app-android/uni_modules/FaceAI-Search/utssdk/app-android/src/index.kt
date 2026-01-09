@file:Suppress("UNCHECKED_CAST", "USELESS_CAST", "INAPPLICABLE_JVM_NAME", "UNUSED_ANONYMOUS_PARAMETER", "NAME_SHADOWING", "UNNECESSARY_NOT_NULL_ASSERTION")
package uts.sdk.modules.FaceAISearch
import android.app.Activity
import android.app.Application
import android.content.Intent
import com.ai.face.faceSearch.search.FaceSearchFeatureManger
import com.faceAI.demo.FaceSDKConfig
import com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity
import com.faceAI.demo.SysCamera.search.FaceSearch1NActivity
import com.faceAI.demo.base.utils.BitmapUtils
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import uts.sdk.modules.uniFaceAISDK.FaceResultManager
import uts.sdk.modules.uniFaceAISDK.FaceSearchActivity
import uts.sdk.modules.uniFaceAISDK.R
typealias StartFaceSearch = (callback: (jsonResult: String) -> Unit) -> Unit
typealias AddFaceSearchFeature = (faceID: String, addFacePerformanceMode: Number, callback: (result: ResultJSON) -> Unit) -> Unit
typealias DeleteFaceSearchFeature = (faceID: String, callback: (result: ResultJSON) -> Unit) -> Unit
typealias FaceSearch = (callback: (res: ResultJSON) -> Unit) -> Unit
typealias InsertFaceSearchFeature = (faceID: String, faceFeature: String, tag: String, group: String, callback: (result: ResultJSON) -> Unit) -> Unit
typealias InsertManyFeatures = (jsonFaceFeatures: String, callback: (result: ResultJSON) -> Unit) -> Unit
open class ResultJSON (
    @JsonNotNull
    open var code: Number,
    @JsonNotNull
    open var msg: String,
    @JsonNotNull
    open var faceID: String,
    @JsonNotNull
    open var faceFeature: String,
    @JsonNotNull
    open var faceBase64: String,
) : UTSObject()
val startFaceSearch: StartFaceSearch = fun(callback: (jsonResult: String) -> Unit) {
    val context = UTSAndroid.getUniActivity() as Activity
    FaceSDKConfig.init(context)
    FaceResultManager.setCallback(FaceResultManager.OnFaceResultListener(object : UTSJSONObject() {
        var onResult = fun(json: String){
            callback(json)
        }
    }))
    val intent = Intent(context, FaceSearchActivity().javaClass)
    context.startActivity(intent)
}
val insertFaceSearchFeature: InsertFaceSearchFeature = fun(faceID: String, faceFeature: String, tag: String, group: String, callback: (result: ResultJSON) -> Unit) {
    val context = UTSAndroid.getAppContext() as Application
    FaceSDKConfig.init(context)
    FaceSearchFeatureManger.getInstance(context).insertFaceFeature(faceID, faceFeature, System.currentTimeMillis(), tag, group)
    val resultJson = ResultJSON(code = 1, msg = "success", faceID = faceID, faceBase64 = "", faceFeature = "")
    callback(resultJson)
}
val insertManyFeatures: InsertManyFeatures = fun(jsonFaceFeatures: String, callback: (result: ResultJSON) -> Unit) {
    val context = UTSAndroid.getAppContext() as Application
    FaceSDKConfig.init(context)
    FaceSearchFeatureManger.getInstance(context).insertFeatures(jsonFaceFeatures)
    val resultJson = ResultJSON(code = 1, msg = "success", faceID = "", faceBase64 = "", faceFeature = "")
    callback(resultJson)
}
val deleteFaceSearchFeature: DeleteFaceSearchFeature = fun(faceID: String, callback: (result: ResultJSON) -> Unit) {
    val context = UTSAndroid.getAppContext() as Application
    FaceSDKConfig.init(context)
    FaceSearchFeatureManger.getInstance(context).deleteFaceFaceFeature(faceID)
    val resultJson = ResultJSON(code = 1, msg = "delete success", faceID = faceID, faceBase64 = "", faceFeature = "")
    callback(resultJson)
}
val addFaceSearchFeature: AddFaceSearchFeature = fun(faceID: String, addFacePerformanceMode: Number, callback: (result: ResultJSON) -> Unit) {
    val context = UTSAndroid.getUniActivity() as Activity
    FaceSDKConfig.init(context)
    val intent = Intent(context, AddFaceImageActivity().javaClass)
    intent.putExtra("ADD_FACE_IMAGE_TYPE_KEY", "FACE_SEARCH")
    intent.putExtra("ADD_FACE_PERFORMANCE_MODE", addFacePerformanceMode)
    context.startActivityForResult(intent, 10086)
    UTSAndroid.onAppActivityResult(fun(requestCode: Int, resultCode: Int, intentAct: Intent?){
        if (requestCode == 10086) {
            if (intentAct != null) {
                val codeNow: Number = intentAct.getIntExtra("code", 0) as Number
                val msgNow: String = intentAct.getStringExtra("msg") as String
                val faceFeatureNoew: String = intentAct.getStringExtra("faceFeature") as String
                var faceBase64 = "?"
                if (0 != codeNow) {
                    faceBase64 = BitmapUtils.bitmapToBase64(FaceSDKConfig.CACHE_BASE_FACE_DIR + faceID)
                }
                val resultJson = ResultJSON(code = codeNow, msg = msgNow, faceFeature = faceFeatureNoew, faceID = "", faceBase64 = faceBase64)
                console.log("添加人脸人脸：" + resultJson)
                callback(resultJson)
            } else {
                val resultJson = ResultJSON(code = -1, msg = "添加失败", faceFeature = "", faceID = "", faceBase64 = "")
                callback(resultJson)
            }
        }
    }
    )
}
val faceSearch: FaceSearch = fun(callback: (res: ResultJSON) -> Unit) {
    val context = UTSAndroid.getUniActivity() as Activity
    FaceSDKConfig.init(context)
    val intent = Intent(context, FaceSearch1NActivity().javaClass)
    context.startActivity(intent)
    val resultJson = ResultJSON(code = 1, msg = "开发测试中", faceID = "faceID8", faceBase64 = "64", faceFeature = "")
    callback(resultJson)
}
