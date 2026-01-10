// 插件对外暴露能力的总入口在 interface.uts ，他与Android/ios 目录下的 index.uts的关系是声明和实现的关系。

/**
 * 开启持续人脸搜索 (Activity 常驻)
 * @param searchThreshold 人脸搜索阈值 (只有人脸库中匹配到的人脸相似度大于此才有结果返回)
 * @param searchOneTime 是否仅搜索一次 (搜索页持续搜索接收结果 还是仅仅搜索一次返回一次结果)
 * @param isCameraSizeHigh 是否高分辨率 (true，高分辨率模式，远距离识别更佳，但会牺牲性能和速度)
 * @param cameraId 摄像头ID (前置摄像头 1，后置摄像头。否则进入兼容模式（部分摄像头需适配）)
 * @param callback 接收搜索结果 JSON 字符串的回调
 */
export type StartFaceSearch = (
    searchThreshold: number,
    searchOneTime: boolean,
    isCameraSizeHigh: boolean,
    cameraId: number,
    callback: (jsonResult: string) => void
) => void


/**
 * 「1:N人脸搜索」录入人脸特征值
 * 
 * @param faceID 用户ID
 * @param callback 结果回调
 */
export type AddFaceSearchFeature = (
    faceID:string,
	addFacePerformanceMode:number,
	callback : (result : ResultJSON) => void) => void
	
	
	
/**
 * 「1:N人脸搜索」删除一张人脸照片
 * 
 * @param faceID 用户ID
 * @param callback 结果回调
 */
export type DeleteFaceSearchFeature = (
    faceID:string,
	callback : (result : ResultJSON) => void) => void



/**
 * 「1:N人脸搜索」录入一张人脸照片
 * 
 * @param faceID 用户ID
 * @param faceFeature 1024长度的提取的人脸特征值
 * @param tag   标签
 * @param group 分组
 * @param callback 结果回调
 */
export type InsertFaceSearchFeature = (
	faceID : string,
	faceFeature : string, 
	tag : string, 
	group : string,
	callback : (result : ResultJSON) => void) => void
	
	
/**
 * 「1:N人脸搜索」录入一张人脸照片
 * 
 * @param jsonFaceFeatures json 格式数组
 * @param callback 结果回调
 */
export type InsertManyFeatures = (
	jsonFaceFeatures : string,
	callback : (result : ResultJSON) => void) => void
	


/**
 * 业务方传给FaceAISDK 插件基础参数
 */
export type ResultJSON = {
  code: number,       //code 含义参考Readme 
  msg: string, 
  faceID: string,     
  faceFeature: string,  //人脸特征值
  faceBase64: string    //人脸图Base64编码
}