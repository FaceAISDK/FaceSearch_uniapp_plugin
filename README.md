## 关于工程说明
  本工程是针对uniApp 制作的插件（本工程非UTS插件不可用于uniApp X）

  1. 「uniapp_plugin_FaceAISDK」 是Android原生语言部分插件，如果你想修改优化插件实现可以用Android Studio参考源码修改
  2. **「uniapp使用插件示例工程」** 是uniApp 使用演示Demo，使用HBuildX 打开，如果你觉得插件够用不想改插件只想使用，聚焦关注此就行

  Email: FaceAISDK.Service@gmail.com   
  iOS SDK： https://github.com/FaceAISDK/FaceAISDK_iOS  
  Android： https://github.com/FaceAISDK/FaceAISDK_Android     
  **其他实现**  
  **React native** https://github.com/zkteco-home/react-native-face-ai  
  **uniApp UTS插件：**  https://github.com/FaceAISDK/FaceAISDK_uniapp_UTS  

   
# FaceAI人脸识别SDK Uni-App 插件

保护伞FaceAI SDK是设备端可离线不联网 人脸识别、动作及近红外IR活体检测、人脸图质量检测
以及人脸搜索（1:N和M:N）SDK，可快速集成实现人脸识别，人脸搜索功能。  
**所有识别都在设备终端本地离线执行，SDK不用联网，不保存不上传任何人脸信息敏感资料更具隐私安全**  

FaceAI SDK主页：https://github.com/FaceAISDK/FaceAISDK_Android   
FaceAI App Android 体验包 https://www.pgyer.com/faceVerify （体验全程可开飞行模式✈️）  
 
2023年我们开始封装移动端FaceAI人脸识别SDK，逐步完善后为了Uni-App 开发者更加方便的接入本SDK  
我们封装了UniPlugin-FaceAISDK 插件，插件一期共提供4个方法接口
  - 1  isFaceExist()      检测是否存在faceID 对应的人脸
  - 2  addFaceImage()     调用SDK录入人脸 (强烈推荐用SDK人脸录入，不要随便拍张照)
  - 3  faceVerify()       人脸识别以及活体检测
  - 4  insertFace2SDK()   同步账号在服务器上的人脸到SDK
  - 5  livenessVerify()   人脸活体检测（RGB静默活体分数和摄像头有关）

插件的使用需要你熟悉Uni-App的开发的基础知识以及少量的原生插件集成经验，相关知识可以参考Uni-App官网
注意 **调试包有相似度分数等提示，正式版没有**

## 插件集成说明
- 如果你是Uni-App 你可以用**HBuilderX4.5.7**打开uniapp示例工程源码/unipluginDemo 自定义基座运行
- 如果你想根据FaceAI SDK 拓展更多功能你可以使用Android Studio **2024.3.1**以上导入本工程完善

IDE版本兼容性问题参考官方，本库开发版本如上；欢迎大家遇到问题提issue，我会及时回复的。
 
## 1. isFaceExist(jsonObject, callback)
检测是否存在faceID 对应的人脸，如果没有你可以先检查你的服务器是否存在，服务器也不存在可以调用SDK
的方法addFaceImage 进行录入人脸

**faceID** 你要检测的人脸ID标识符，在你的业务体系中唯一不可重复，你可以用账户名，身份证号等  

传入参数 jsonObject 就一个字段 faceID  
返回参数 callback 会返回检测的结果 true 存在 false 不存在  

## 2. addFaceImage(jsonObject, callback)
调用SDK 录入人脸，SDK 会对人脸录入进行合规检查  
传入参数 jsonObject 字段 
"faceID": "yourFaceID_uniApp", //对应需要识别的人脸ID  
"addFacePerformanceMode": 2   // 1 快速模式  2 精确模式"

返回参数 callback 会返回添加结果json  
{  
    "code": "0 用户取消/失败,  1:录入完成",  
    "msg":"code 对应的提示",  
    "faceID": "录入人脸对应的face ID",  
    "faceBase64":"人脸图像faceBase64转码"  
}  


## 3. faceVerify(jsonObject, callback)
调用SDK 录入人脸，SDK 会对人脸录入进行合规检查  
**传入参数 jsonObject**  
{  
   "faceID": "yourFaceID_uniApp", //对应需要识别的人脸ID  
   "threshold": 0.85,      //人脸识别相似度阈值  
   "silentThreshold": 0.85,//活体分数通过阈值  
   "faceLivenessType": 0,  //活体检测类型 //0 NONE无活体; 1 SILENT静默活体和摄像头成像能力有关;  2 MOTION动作活体;  3 SILENT_MOTION 动作活体+静默  
   "motionStepSize": 1,    //动作活体检测动作数量。比如2 表示需要用户做2个动作才能通过活体检测  
   "verifyTimeOut": 16，   //活体检测超时时间 秒  
   "motionLivenessTypes": "1,2,3,4,5"  //动作活体类型组合。1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头
}

**返回参数 callback 会返回识别结果**   
{  
  "code": "0", //0用户取消，1识别成功，2RGB静默分数过低，3动作活体超时， 4识别失败，5多次未检测到人脸，6，识别被暂停，9活体检测完成
   "msg":"code 对应的提示",  
   "faceID": "录入人脸对应的face ID",  
   "faceBase64":"人脸场景图像faceBase64转码"  
}

## 4. insertFace2SDK(jsonObject, callback) 
同步账号在服务器上的人脸到SDK，比如你在A设备登录的时候已经录入人脸并且同步到你的服务器了    
那么登录B设备后不用调用SDK 再次录入人脸，你仅仅需要把你的业务服务器人脸图数据同步到SDK本地就可以  
{  
  "faceID": "yourFaceID_uniApp", //对应需要识别的人脸ID  
  "faceBase64": "图片Base64 编码String 后返回", //人脸识别相似度阈值
}

## 5. livenessVerify(jsonObject, callback)
调用SDK 录入人脸，SDK 会对人脸录入进行合规检查  
**传入参数 jsonObject**  
{
  "silentThreshold": 0.85, //活体分数通过阈值  
  "faceLivenessType": 0,  //活体检测类型  //0 NONE无活体;  1 SILENT静默活体和摄像头成像能力有关;  2 MOTION动作活体;  3 SILENT_MOTION 动作+静默;  
  "motionStepSize": 1,    //动作活体检测动作数量。比如2 表示需要用户做2个动作才能通过活体检测  
  "verifyTimeOut": 16,    //活体检测超时时间 秒  
  "motionLivenessTypes": "1,2,3,4,5"  //动作活体类型组合。1.张张嘴 2.微笑 3.眨眨眼 4.摇头 5.点头
}

**返回参数 callback 会返回识别结果**   
{  
  "code": "0", //0用户取消，3动作活体超时，5多次未检测到人脸，6，识别被暂停，9活体检测完成
  "msg":"code 对应的提示",  
  "faceID": "录入人脸对应的face ID",  
  "faceBase64":"人脸场景图像faceBase64转码"  
}


