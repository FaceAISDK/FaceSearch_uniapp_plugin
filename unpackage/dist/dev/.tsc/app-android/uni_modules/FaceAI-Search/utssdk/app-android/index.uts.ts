import { StartFaceSearch,AddFaceSearchFeature,DeleteFaceSearchFeature,FaceSearch,InsertFaceSearchFeature,ResultJSON, InsertManyFeatures} from '../interface.uts'

import Application from 'android.app.Application';
import Activity from 'android.app.Activity';
import Intent from 'android.content.Intent';
import FaceSDKConfig from "com.faceAI.demo.FaceSDKConfig";
import FaceAISDKNative from "uts.sdk.modules.uniFaceAISDK.FaceAISDKNative";
import FaceSearchActivity from "uts.sdk.modules.uniFaceAISDK.FaceSearchActivity";
import FaceResultManager from "uts.sdk.modules.uniFaceAISDK.FaceResultManager";
import System from 'java.lang.System'; // 引入 System 类以获取时间戳
import AddFaceImageActivity from "com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity";
import BitmapUtils from "com.faceAI.demo.base.utils.BitmapUtils";
import FaceSearch1NActivity from 'com.faceAI.demo.SysCamera.search.FaceSearch1NActivity';
import FaceSearchFeatureManger from 'com.ai.face.faceSearch.search.FaceSearchFeatureManger';

//当前libs目录下为空，复用了HelloKitty 的

//*******************************     下面是1:N 人脸搜索识别的方法（仅Android） ************************************************

 /**
  * 启动持续人脸搜索 Activity
  * 该方法会启动 Activity 并不关闭，持续回调数据
  */
 export const startFaceSearch: StartFaceSearch = function(callback: (jsonResult: string) => void) {
	 
	 UTSAndroid.getUniActivity()!.runOnUiThread(() => {
	 	const context = UTSAndroid.getUniActivity() as Activity
	 	FaceSDKConfig.init(context);
		// UTS 会自动将这个 (json: string) => void 转换为 Kotlin 的 (String) -> Unit
		FaceResultManager.setCallback((json: string) => {
		    callback(json);
		});
		 
		// 2. 启动搜索 Activity
		const intent = new Intent(context, FaceSearchActivity().javaClass);
		// 如果需要设置 Activity 为 SingleTop 或其他启动模式，可以在这里 addFlags
		context.startActivity(intent);	
	 });
 }
 


/**
 * 「1:N人脸搜索」人脸搜索人脸特征更新同步
 * 自行保证人脸特征值长度为1024
 */
export const insertFaceSearchFeature: InsertFaceSearchFeature = function (
    faceID: string,
    faceFeature: string,
    tag: string,
    group: string,
    callback: (result: ResultJSON) => void
) {
    // 1. 获取 Context
    const context = UTSAndroid.getAppContext() as Application
    FaceSDKConfig.init(context);

    // 2. 【校验】验证 faceID 不能为空
    if (faceID == null || faceID.trim() == "") {
        const errorResult: ResultJSON = {
            code: -1, // 错误码
            msg: "参数错误: faceID 不能为空",
            faceID: "",
            faceBase64: "",
            faceFeature: ""
        }
        __f__('error','at uni_modules/FaceAI-Search/utssdk/app-android/index.uts:67',errorResult.msg)
        callback(errorResult)
        return // 终止执行
    }

    // 3. 【校验】验证 faceFeature 长度必须为 1024
    if (faceFeature == null || faceFeature.length != 1024) {
        const currentLen = (faceFeature == null) ? 0 : faceFeature.length;
        const errorResult: ResultJSON = {
            code: -2, // 错误码
            msg: "参数错误: faceFeature 长度必须为 1024 (当前长度: " + currentLen + ")",
            faceID: faceID,
            faceBase64: "",
            faceFeature: ""
        }
        __f__('error','at uni_modules/FaceAI-Search/utssdk/app-android/index.uts:82',errorResult.msg)
        callback(errorResult)
        return // 终止执行
    }

    // 4. 【执行】调用 Native 方法，并增加异常捕获
    try {
        // tag 和 group 可以用来做标记和分组
        // 建议：如果 tag/group 为空，可以传默认值，防止 Native 端空指针（视 Native 实现而定）
        const safeTag = (tag == null) ? "" : tag;
        const safeGroup = (group == null) ? "" : group;

        const isSuccess = FaceSearchFeatureManger.getInstance(context)
            .insertFaceFeature(faceID, faceFeature, System.currentTimeMillis(), safeTag, safeGroup);

        // 假设 Native 方法返回 boolean (如果 Native 是 void，则这里只代表调用成功)
        // 构造成功回调
        const successResult: ResultJSON = {
            code: 1,
            msg: "success", // 或者 "插入成功"
            faceID: faceID,
            faceBase64: "",
            faceFeature: "" // 为了节省传输开销，回调通常不再把巨大的特征值传回去
        }
        callback(successResult)

    } catch (e: Exception) {
        // 5. 【异常处理】捕获 Java 层可能的崩溃
        const catchResult: ResultJSON = {
            code: -3,
            msg: "Native 执行异常: " + e.message,
            faceID: faceID,
            faceBase64: "",
            faceFeature: ""
        }
        __f__('error','at uni_modules/FaceAI-Search/utssdk/app-android/index.uts:117',"insertFaceSearchFeature error: " + e.message)
        callback(catchResult)
    }
}
 
 
 
 
 /**
  *  「1:N人脸搜索」批量插入人脸特征数据
  *   自行保证人脸特征值长度为1024
  */
 export const insertManyFeatures : InsertManyFeatures = 
   function (jsonFaceFeatures : string, 
   callback : (result : ResultJSON) => void) {
 	    const context = UTSAndroid.getAppContext() as Application
 	    FaceSDKConfig.init(context);	
 	
		let faceCount = FaceSearchFeatureManger.getInstance(context)
		                .insertFeatures(jsonFaceFeatures)
 	
 		const resultJson:ResultJSON={
 				code:faceCount,
 				msg:"insertManyFeatures",
 				faceID:"",
 				faceBase64:"",
 				faceFeature:""
 			}	
 		callback(resultJson)
 	})
 }
 
 
 
 
 
 /**
  *  「1:N人脸搜索」人脸搜索人脸特征更新同步
  */
 export const deleteFaceSearchFeature : DeleteFaceSearchFeature = 
   function (faceID : string, callback : (result : ResultJSON) => void) {
 	    const context = UTSAndroid.getAppContext() as Application
 	    FaceSDKConfig.init(context);	
 	
        //清除所有人脸搜索所有特征
        FaceSearchFeatureManger.getInstance(context).deleteFaceFaceFeature(faceID);
 	
 		const resultJson:ResultJSON={
 				code:1,
 				msg:"delete success",
 				faceID:faceID,
 				faceBase64:"",
 				faceFeature:""
 			}	
 		callback(resultJson)
 	})
 }
 

/**
 * 「1:N人脸搜索」跳转到Android SDK中原生相机页面处理人脸录入
 * 
 * @param faceID 用户ID
 * @param addFacePerformanceMode 添加人脸角度检测模式. 1 快速模式   2 精确模式
 * @param callback 结果回调
 */
export const addFaceSearchFeature : AddFaceSearchFeature = function (
    faceID:string,
	addFacePerformanceMode:number,
	callback : (result : ResultJSON) => void
) {
	
	UTSAndroid.getUniActivity()!.runOnUiThread(() => {
		const context = UTSAndroid.getUniActivity() as Activity
		FaceSDKConfig.init(context);
		const intent = new Intent(context, AddFaceImageActivity().javaClass)
		intent.putExtra("ADD_FACE_IMAGE_TYPE_KEY", "FACE_SEARCH");
		intent.putExtra("ADD_FACE_PERFORMANCE_MODE", addFacePerformanceMode);
		context.startActivityForResult(intent, 10086)
	});
	
    //语法不熟悉，先保证主流程跑通
	UTSAndroid.onAppActivityResult((requestCode : Int, resultCode : Int, intentAct?: Intent) => {
		if (requestCode == 10086) {
			if(intentAct!=null){
				const codeNow:number = intentAct.getIntExtra("code",0) as number
				const msgNow:string=intentAct.getStringExtra("msg") as string
                const faceFeatureNoew:string=intentAct.getStringExtra("faceFeature") as string
				
				let faceBase64="?"
				if(0!=codeNow){
					//用户取消了就不应该有这个值
					faceBase64=BitmapUtils.bitmapToBase64(FaceSDKConfig.CACHE_BASE_FACE_DIR+faceID)
				}
				 
				const resultJson:ResultJSON={
					code:codeNow,
					msg:msgNow,
					faceFeature: faceFeatureNoew,
					faceID:"",
					faceBase64:faceBase64
				}		
				__f__('log','at uni_modules/FaceAI-Search/utssdk/app-android/index.uts:219',"添加人脸人脸："+resultJson)
				callback(resultJson)
			}else{
				const resultJson:ResultJSON={
					code:-1,
					msg:"添加失败",
					faceFeature: "",
					faceID:"",
					faceBase64:""
				}
				
				callback(resultJson)
			}
		} 
	});
}



/**
 * 1:N，M：N 人脸搜索，开发测试中 完善一下参数
 * 
 */
export const faceSearch:FaceSearch = function(callback: (res: ResultJSON) => void){
	
	const context = UTSAndroid.getUniActivity() as Activity
	FaceSDKConfig.init(context);
	
	const intent = new Intent(context, FaceSearch1NActivity().javaClass)
	context.startActivity(intent)
	
	const resultJson:ResultJSON={
		code: 1,
		msg: "开发测试中",
		faceID: "faceID8",
		faceBase64: "64",
		faceFeature: ""
	}
    callback(resultJson)
} 
