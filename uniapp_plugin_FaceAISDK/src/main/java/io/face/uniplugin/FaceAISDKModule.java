package io.face.uniplugin;

import static com.faceAI.demo.FaceSDKConfig.CACHE_BASE_FACE_DIR;
import static com.faceAI.demo.FaceSDKConfig.CACHE_FACE_LOG_DIR;
import static com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity.ADD_FACE_IMAGE_TYPE_KEY;
import static com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity.ADD_FACE_PERFORMANCE_MODE;
import static com.faceAI.demo.SysCamera.verify.FaceVerificationActivity.USER_FACE_ID_KEY;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;
import androidx.annotation.NonNull;
import com.ai.face.base.baseImage.FaceAIUtils;
import com.ai.face.base.baseImage.FaceEmbedding;
import com.faceAI.demo.FaceSDKConfig;
import com.faceAI.demo.FaceAINaviActivity;
import com.alibaba.fastjson.JSONObject;
import com.faceAI.demo.SysCamera.verify.FaceVerificationActivity;
import com.faceAI.demo.SysCamera.addFace.AddFaceImageActivity;
import com.faceAI.demo.SysCamera.verify.LivenessDetectActivity;
import com.faceAI.demo.base.utils.BitmapUtils;
import com.faceAI.demo.base.utils.VoicePlayer;
import org.jetbrains.annotations.NotNull;
import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

/**
 * 扩展方法必须加上@UniJSMethod (uiThread = false or true) 注解。
 * UniApp 会根据注解来判断当前方法是否要运行在 UI 线程，和当前方法是否是扩展方法。
 *
 * UniApp是根据反射来进行调用 Module 扩展方法，所以Module中的扩展方法必须是 public 类型。
 *
 * 相机权限自行管理。
 *
 */
public class FaceAISDKModule extends UniModule {
    private String TAG = "FaceAISDKModule";
    private static int REQUEST_CODE_FOR_FACE_LIVENESS= 10085; //去人脸识别
    private static int REQUEST_CODE_FOR_FACE_VERIFY = 10086; //去人脸识别
    private static int REQUEST_CODE_FOR_ADD_FACE = 10087; //去添加人脸
    private UniJSCallback faceVerifyCallBack,addFaceCallBack,livenessCallback;


    public FaceAISDKModule() {
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            FaceSDKConfig.init(mUniSDKInstance.getContext());
            //语音提示
            VoicePlayer.getInstance().init(mUniSDKInstance.getContext());
        }
    }



    /**
     * 监测onActivityResult 回调数据 ？
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE_FOR_FACE_VERIFY ) {
            if(resultCode == Activity.RESULT_OK){
                /**
                 * 0 用户取消
                 * 1 人脸识别成功
                 * 2 活体分数过低
                 * 3 活体检测超时
                 * 4 人脸识别相似度低于阈值
                 */
                if(faceVerifyCallBack != null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("code", data.getIntExtra("code", 0));
                    jsonObject.put("msg", data.getStringExtra("msg"));
                    jsonObject.put("similarity", data.getFloatExtra("similarity",0.0f));

                    //把Bitmap 返回
                    String faceFilePath = CACHE_FACE_LOG_DIR + "verifyBitmap";
                    //人脸识别的场景Bitmap 返回
                    jsonObject.put("faceBase64", BitmapUtils.bitmapToBase64(faceFilePath));

                    faceVerifyCallBack.invoke(jsonObject);
                }
            }

        } else if(requestCode == REQUEST_CODE_FOR_ADD_FACE) {
            if(resultCode == Activity.RESULT_OK){
                /**
                 * 0 用户取消
                 * 1 添加人脸成功
                 */
                if(addFaceCallBack != null) {
                    String faceID = data.getStringExtra("faceID");
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("code", data.getIntExtra("code", 0));
                    jsonObject.put("msg", data.getStringExtra("msg"));
                    jsonObject.put("faceID", faceID);

                    addFaceCallBack.invoke(jsonObject);
                }
            }

        }else if(requestCode == REQUEST_CODE_FOR_FACE_LIVENESS) {
            if (resultCode == Activity.RESULT_OK) {
                /**
                 * 人脸图返回去
                 */
                if (livenessCallback != null) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("code", data.getIntExtra("code", 0));
                    jsonObject.put("msg", data.getStringExtra("msg"));
                    jsonObject.put("silentLivenessScore",data.getFloatExtra("silentLivenessScore",0));
                    //把Bitmap 返回
                    String faceFilePath = CACHE_FACE_LOG_DIR + "liveBitmap";
                    //活体检测通过的人脸Bitmap 返回
                    jsonObject.put("faceBase64", BitmapUtils.bitmapToBase64(faceFilePath));
                    livenessCallback.invoke(jsonObject);
                }
            }
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * 账号换设备登录的时候需要同步你的账号在服务器上的人脸到SDK
     * 请把人脸图转为Base64编码 Base64.NO_WRAP
     *
     * @param jsonObject
     * @param callback
     */
    @UniJSMethod(uiThread = true)
    public void insertFace2SDK(JSONObject jsonObject, UniJSCallback callback)  {
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            //查询是否存在FaceID
            FaceSDKConfig.init(mUniSDKInstance.getContext());

            String faceID=jsonObject.getString("faceID");
            String faceBase64=jsonObject.getString("faceBase64");  //这里接收也改为Base64编码的人脸图
            Bitmap bitmap= BitmapUtils.base64ToBitmap(faceBase64);

            if(bitmap == null) {
                Toast.makeText(mUniSDKInstance.getContext(), "人脸Base64图解析失败",Toast.LENGTH_SHORT).show();
                JSONObject json = new JSONObject();
                json.put("code", 0);
                json.put("msg", "insertFace2SDK 失败");
                json.put("faceID", faceID);
                callback.invoke(json);
                return;
            }

            //其他地方同步过来的人脸可能是不规范的没有经过校准的人脸图（证件照，多人脸，过小等）。disposeBaseFaceImage处理
            FaceAIUtils.Companion.getInstance(((Activity) mUniSDKInstance.getContext()).getApplication())
                    .disposeBaseFaceImage(mUniSDKInstance.getContext(),bitmap, new FaceAIUtils.Callback() {
                        //处理优化人脸成功完成去初始化引擎
                        @Override
                        public void onSuccess(@NonNull Bitmap bitmap, @NonNull float[] faceEmbedding) {
                            FaceEmbedding.saveEmbedding(mUniSDKInstance.getContext(), faceID, faceEmbedding); //本地保存人脸特征向量
                            BitmapUtils.saveDisposedBitmap(bitmap, FaceSDKConfig.CACHE_BASE_FACE_DIR,faceID); //裁剪好的人脸保存本地

                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("code", 1);
                            jsonObject.put("msg", "insertFace 2 SDK成功");
                            jsonObject.put("faceID", faceID);
                            callback.invoke(jsonObject);
                        }

                        //底片处理异常的信息回调
                        @Override
                        public void onFailed(@NotNull String msg, int errorCode) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("code", 0);
                            jsonObject.put("msg", msg+"-"+errorCode);
                            jsonObject.put("faceID", faceID);
                            callback.invoke(jsonObject);
                        }
                    });

        }
    }


    /**
     * 是否存在faceID 对应的人脸
     *
     * @param jsonObject
     * @param callback
     */
    @UniJSMethod(uiThread = true)
    public void isFaceExist(JSONObject jsonObject, UniJSCallback callback) {
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            //查询是否存在FaceID
            FaceSDKConfig.init(mUniSDKInstance.getContext());
            String faceID=jsonObject.getString("faceID");
//            boolean isExist=FaceSDKConfig.isFaceIDExist(faceID);

            var isExist=true;

            float[] faceEmbedding = FaceEmbedding.loadEmbedding(mUniSDKInstance.getContext(), faceID);

            // 去Path 路径读取有没有faceID 对应的处理好的人脸Bitmap
            String faceFilePath = FaceSDKConfig.CACHE_BASE_FACE_DIR + faceID;
            Bitmap baseBitmap = BitmapFactory.decodeFile(faceFilePath);


            if (faceEmbedding.length == 0||baseBitmap==null) {
                isExist=false;
            }

            callback.invoke(isExist);
        }
    }


    /**
     * 调用SDK 录入人脸
     *
     * @param jsonObject
     * @param callback
     */
    @UniJSMethod(uiThread = true)
    public void addFaceImage(JSONObject jsonObject, UniJSCallback callback) {
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            addFaceCallBack=callback;
            //人脸图保存路径初始化
            FaceSDKConfig.init(mUniSDKInstance.getContext());
            Intent intent=new Intent(mUniSDKInstance.getContext(), AddFaceImageActivity.class)
                    .putExtra(ADD_FACE_IMAGE_TYPE_KEY, AddFaceImageActivity.AddFaceImageTypeEnum.FACE_VERIFY.name());

            intent.putExtra(USER_FACE_ID_KEY,jsonObject.getString("faceID"));
            //录入人脸的精度 1 快速模式  2 精确模式
            intent.putExtra(ADD_FACE_PERFORMANCE_MODE,jsonObject.getIntValue("addFacePerformanceMode"));

            ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE_FOR_ADD_FACE);
        }
    }

    /**
     * 人脸识别
     *
     * @param jsonObject
     * @param callback
     */
    @UniJSMethod (uiThread = true)
    public void faceVerify(JSONObject jsonObject, UniJSCallback callback) {
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {

            faceVerifyCallBack=callback;
            FaceSDKConfig.init(mUniSDKInstance.getContext());

            Intent intent=new Intent(mUniSDKInstance.getContext(), FaceVerificationActivity.class);
            intent.putExtra(USER_FACE_ID_KEY,jsonObject.getString("faceID"));
            intent.putExtra(FaceVerificationActivity.THRESHOLD_KEY,jsonObject.getFloatValue("threshold"));
            intent.putExtra(FaceVerificationActivity.FACE_LIVENESS_TYPE,jsonObject.getIntValue("faceLivenessType"));
            intent.putExtra(FaceVerificationActivity.MOTION_STEP_SIZE,jsonObject.getIntValue("motionStepSize"));
            intent.putExtra(FaceVerificationActivity.MOTION_TIMEOUT,jsonObject.getIntValue("verifyTimeOut"));
            intent.putExtra(FaceVerificationActivity.SILENT_THRESHOLD_KEY,jsonObject.getFloatValue("silentThreshold"));
            intent.putExtra(FaceVerificationActivity.MOTION_LIVENESS_TYPES,jsonObject.getString("motionLivenessTypes"));

            ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE_FOR_FACE_VERIFY);
        }
    }


    /**
     * 仅活体检测
     *
     * @param jsonObject
     * @param callback
     */
    @UniJSMethod (uiThread = true)
    public void livenessVerify(JSONObject jsonObject, UniJSCallback callback) {
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {

            livenessCallback=callback;
            FaceSDKConfig.init(mUniSDKInstance.getContext());

            Intent intent=new Intent(mUniSDKInstance.getContext(), LivenessDetectActivity.class);
            intent.putExtra(LivenessDetectActivity.FACE_LIVENESS_TYPE,jsonObject.getIntValue("faceLivenessType"));
            intent.putExtra(LivenessDetectActivity.MOTION_STEP_SIZE,jsonObject.getIntValue("motionStepSize"));
            intent.putExtra(LivenessDetectActivity.MOTION_TIMEOUT,jsonObject.getIntValue("verifyTimeOut"));
            intent.putExtra(LivenessDetectActivity.SILENT_THRESHOLD_KEY,jsonObject.getFloatValue("silentThreshold"));

            ((Activity)mUniSDKInstance.getContext()).startActivityForResult(intent, REQUEST_CODE_FOR_FACE_LIVENESS);
        }
    }


    /**
     * 跳转到关于
     *
     */
    @UniJSMethod (uiThread = true)
    public void gotoAboutFaceAIPage(){
        if(mUniSDKInstance != null && mUniSDKInstance.getContext() instanceof Activity) {
            FaceSDKConfig.init(mUniSDKInstance.getContext());
            Intent enumIntent =new  Intent(mUniSDKInstance.getContext(), FaceAINaviActivity.class);

            mUniSDKInstance.getContext().startActivity(enumIntent);
        }
    }



}
