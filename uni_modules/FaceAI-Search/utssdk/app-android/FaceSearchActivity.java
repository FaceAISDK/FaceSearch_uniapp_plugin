package uts.sdk.modules.uniFaceAISDK;

import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_ANGLE_NOT_FIT;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.EMGINE_INITING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_DIR_EMPTY;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_SIZE_FIT;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_TOO_LARGE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.FACE_TOO_SMALL;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.MASK_DETECTION;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_LIVE_FACE;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.NO_MATCHED;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCHING;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.SEARCH_PREPARED;
import static com.ai.face.faceSearch.search.SearchProcessTipsCode.THRESHOLD_ERROR;
import static com.faceAI.demo.FaceAISettingsActivity.FRONT_BACK_CAMERA_FLAG;
import static com.faceAI.demo.FaceAISettingsActivity.SYSTEM_CAMERA_DEGREE;
import static com.faceAI.demo.FaceSDKConfig.CACHE_SEARCH_FACE_DIR;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageProxy;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.ai.face.base.view.camera.CameraXBuilder;
import com.ai.face.core.utils.FaceAICameraType;
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.GraphicOverlay;
import com.ai.face.faceSearch.search.SearchProcessBuilder;
import com.ai.face.faceSearch.search.SearchProcessCallBack;
import com.ai.face.faceSearch.utils.FaceSearchResult;
import com.faceAI.demo.R;
import com.faceAI.demo.SysCamera.camera.FaceCameraXFragment;
import com.faceAI.demo.base.AbsBaseActivity;
import com.faceAI.demo.base.utils.VoicePlayer;
import com.faceAI.demo.base.utils.BitmapUtils;
import com.faceAI.demo.base.view.FaceCoverView;
import com.google.gson.Gson;
import java.util.Iterator;
import java.util.List;

/**
 * RGB摄像头动作活体检测+1:N 人脸搜索识别。
 * <p>
 * 采用传统 findViewById 方式，适配 HBuilder 云打包环境。
 * @author FaceAISDK.Service@gmail.com
 */
public class FaceSearchActivity extends AbsBaseActivity {
    public static final String THRESHOLD_KEY = "THRESHOLD_KEY";
    public static final String NEED_FACE_LIVE = "NEED_FACE_LIVE";
    public static final String SEARCH_ONE_TIME = "SEARCH_ONE_TIME";
    public static final String IS_CAMERA_SIZE_HIGH = "IS_CAMERA_SIZE_HIGH";
    public static final String SEARCH_TIME_OUT = "SEARCH_TIME_OUT";

    private float searchThreshold = 0.85f;
    private boolean searchOneTime = false;
    private int searchTimeOut = 4000;
    private boolean isCameraSizeHigh = false;
    private int cameraLensFacing;
    private boolean needFaceLive = false;

    // UI 控件变量
    private ImageView closeBtn;
    private ImageView switchButton;
    private GraphicOverlay graphicOverlay;
    private FaceCoverView faceCover;

    private FaceCameraXFragment cameraXFragment;
    private boolean pauseSearch = false;
    private long searchStartTime = 0;

    private void getIntentParams() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(THRESHOLD_KEY)) {
                searchThreshold = intent.getFloatExtra(THRESHOLD_KEY, 0.85f);
            }
            if (intent.hasExtra(SEARCH_ONE_TIME)) {
                searchOneTime = intent.getBooleanExtra(SEARCH_ONE_TIME, false);
            }
            if (intent.hasExtra(NEED_FACE_LIVE)) {
                needFaceLive = intent.getBooleanExtra(NEED_FACE_LIVE, false);
            }

            if (intent.hasExtra(SEARCH_TIME_OUT)) {
                searchTimeOut = intent.getIntExtra(SEARCH_TIME_OUT, 4000);
            }
            if (intent.hasExtra(IS_CAMERA_SIZE_HIGH)) {
                isCameraSizeHigh = intent.getBooleanExtra(IS_CAMERA_SIZE_HIGH, false);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemUI();
        
        // 1. 设置布局
        setContentView(R.layout.activity_face_search);

        // 2. 初始化控件
        closeBtn = findViewById(R.id.close);
        switchButton = findViewById(R.id.switch_button);
        graphicOverlay = findViewById(R.id.graphicOverlay);
        faceCover = findViewById(R.id.face_cover);
        applySafeAreaInsets();

        // 3. 设置点击事件
        closeBtn.setOnClickListener(v -> finish());

        getIntentParams();

        SharedPreferences sharedPref = getSharedPreferences("FaceAISDK_SP", Context.MODE_PRIVATE);
        cameraLensFacing = sharedPref.getInt(FRONT_BACK_CAMERA_FLAG, 0);
        int degree = sharedPref.getInt(SYSTEM_CAMERA_DEGREE, getWindowManager().getDefaultDisplay().getRotation());

        CameraXBuilder cameraXBuilder = new CameraXBuilder.Builder()
                .setCameraLensFacing(cameraLensFacing)
                .setLinearZoom(0.01f)
                .setRotation(degree)
                .setCameraSizeHigh(isCameraSizeHigh)
                .create();

        cameraXFragment = FaceCameraXFragment.newInstance(cameraXBuilder);
        switchButton.setOnClickListener(v -> cameraXFragment.switchCamera());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_camerax, cameraXFragment)
                .commit();

        initFaceSearchParam();
    }

    private void initFaceSearchParam() {
        SearchProcessBuilder faceProcessBuilder = new SearchProcessBuilder.Builder(this)
                .setLifecycleOwner(this)
                .setCameraType(FaceAICameraType.SYSTEM_CAMERA)
                .setNeedFaceLiveness(true)
                .setSearchType(SearchProcessBuilder.SearchType.N_SEARCH_1)
                .setThreshold(searchThreshold)
                .setCallBackAllMatch(true)
                .setSearchIntervalTime(1700)
				.setSearchTimeOut(4000)    //搜索超时时间，超时后会提示无结果,默认3000，范围[3000,6000]毫秒
                .setMirror(cameraLensFacing == CameraSelector.LENS_FACING_FRONT)
                .setProcessCallBack(new SearchProcessCallBack() {
                    @Override
                    public void onFaceMatched(List<FaceSearchResult> matchedResults, Bitmap searchBitmap, float liveness) {
                        Iterator<FaceSearchResult> iterator = matchedResults.iterator();
                        while (iterator.hasNext()) {
                            FaceSearchResult item = iterator.next();
                            if (TextUtils.isEmpty(item.getFaceName()) && item.getFaceScore() == 0.0f) {
                                iterator.remove();
                            }
                        }
                        String json = new Gson().toJson(matchedResults);
                        String base64 = BitmapUtils.bitmapToBase64(searchBitmap);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FaceResultManager.INSTANCE.sendResult(json, liveness, base64);
                                if (searchOneTime) {
							            new Handler(Looper.getMainLooper()).postDelayed(() -> {
											FaceSearchActivity.this.finish();
							            }, 1111);		
                                }
                            }
                        });
                    }

                    @Override
                    public void onMostSimilar(String faceID, float score, Bitmap bitmap, float livenessValue) {
						
                    }

                    @Override
                    public void onFaceDetected(List<FaceSearchResult> result) {
                        // 替换 binding.graphicOverlay
                        graphicOverlay.drawRect(result);
                    }

                    @Override
                    public void onProcessTips(int i) {
                        showFaceSearchPrecessTips(i);
                    }

                    @Override
                    public void onLog(String log) {}

                }).create();

        FaceSearchEngine.Companion.getInstance().initSearchParams(faceProcessBuilder);
		searchStartTime=System.currentTimeMillis();

        cameraXFragment.setOnAnalyzerListener(new FaceCameraXFragment.onAnalyzeData() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                if (!isDestroyed() && !isFinishing() && !pauseSearch) {
                    FaceSearchEngine.Companion.getInstance().runSearchWithImageProxy(imageProxy, 0);
                }
            }

            @Override
            public void backImageSize(int imageWidth, int imageHeight) {
                // 替换 binding.graphicOverlay
                graphicOverlay.setCameraInfo(imageWidth, imageHeight, cameraXFragment.isFrontCamera());
            }
        });
    }

    private void showFaceSearchPrecessTips(int code) {
        switch (code) {
            case NO_MATCHED:
                setSecondTips(R.string.no_matched_face);
                if (searchOneTime) {
                    if ((System.currentTimeMillis() - searchStartTime) > searchTimeOut) {
                        FaceResultManager.INSTANCE.sendResult("[]", 0.0f, "");
                        FaceSearchActivity.this.finish();
                    }
                } else {
                    FaceResultManager.INSTANCE.sendResult("[]", 0.0f, "");
                }
                break;
            case FACE_ANGLE_NOT_FIT:
                setSecondTips(R.string.face_angle_not_fit);
                break;
            case FACE_DIR_EMPTY:
                setSearchTips(R.string.local_face_database_empty);
                Toast.makeText(this, R.string.no_face_data_tips, Toast.LENGTH_LONG).show();
                break;
            case EMGINE_INITING:
                setSearchTips(R.string.sdk_init);
                break;
            case SEARCH_PREPARED:
                setSearchTips(R.string.keep_face_tips);
                break;
            case SEARCHING:
                setSearchTips(R.string.keep_face_tips);
                break;
            case NO_LIVE_FACE:
                setSearchTips(R.string.no_face_detected_tips);
                break;
            case FACE_TOO_SMALL:
                setSecondTips(R.string.come_closer_tips);
                break;
            case FACE_TOO_LARGE:
                setSecondTips(R.string.far_away_tips);
                break;
            case FACE_SIZE_FIT:
                setSecondTips(0);
                break;
            case THRESHOLD_ERROR:
                setSearchTips(R.string.search_threshold_scope_tips);
                break;
            case MASK_DETECTION:
                setSearchTips(R.string.no_mask_please);
                break;
            default:
                if (faceCover != null) faceCover.setTipsText("Tips Code：" + code);
                break;
        }
    }

    private void setSearchTips(int resId) {
        if (faceCover != null) faceCover.setTipsText(resId);
    }

    private void setSecondTips(int resId) {
        if (faceCover != null) faceCover.setSecondTipsText(resId);
    }

    private void applySafeAreaInsets() {
        View root = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safeInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );
            boolean isRtl = ViewCompat.getLayoutDirection(view)
                    == ViewCompat.LAYOUT_DIRECTION_RTL;
            int startInset = isRtl ? safeInsets.right : safeInsets.left;
            int endInset = isRtl ? safeInsets.left : safeInsets.right;

            ViewGroup.MarginLayoutParams closeParams =
                    (ViewGroup.MarginLayoutParams) closeBtn.getLayoutParams();
            closeParams.setMarginStart(dp(7) + startInset);
            closeParams.topMargin = dp(15) + safeInsets.top;
            closeBtn.setLayoutParams(closeParams);

            ViewGroup.MarginLayoutParams switchParams =
                    (ViewGroup.MarginLayoutParams) switchButton.getLayoutParams();
            switchParams.setMarginEnd(dp(15) + endInset);
            switchParams.topMargin = dp(22) + safeInsets.top;
            switchButton.setLayoutParams(switchParams);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        FaceSearchEngine.Companion.getInstance().stopSearchProcess();
        if (!isChangingConfigurations()) {
            FaceResultManager.INSTANCE.clear();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        pauseSearch = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        pauseSearch = true;
    }
}
