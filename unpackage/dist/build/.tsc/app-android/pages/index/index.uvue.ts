 
	import {startFaceSearch,
		    insertFaceSearchFeature,
			insertManyFeatures,
	        addFaceSearchFeature,
			deleteFaceSearchFeature,
			faceSearch,ResultJSON} from "@/uni_modules/FaceAI-Search";
 
	const __sfc__ = defineComponent({
		  
		data() {
			return {
				faceID: 'faceSearchID',
				faceFeature: 'faceFeature is a string with lenth 1024',
				faceJSONFeatures: 'faceFeature is a string with lenth 1024',
				faceAIResult: 'faceAIResult'
			}
		},
		onLoad() {

		},
		
		methods: {
			
			/**
			 * 人脸搜索
			 */
			startFaceSearchDemo: function () {
			    startFaceSearch((jsonStr: string) => {
			        // 这里会持续不断地收到回调
			        console.log("收到搜索结果:", jsonStr);
			        
			        // 1. 简单展示，已经按照搜索匹配度降序排列
			        this.faceAIResult = "【实时回调】\n" + jsonStr;
			        
			        // 2. 如果需要处理数据，建议包裹 try-catch 解析 JSON
			        // try {
			        //    const results = JSON.parse(jsonStr);
			        //    if(results.length > 0) {
			        //       // 可以在这里做业务逻辑，比如签到、开门等
			        //    }
			        // } catch(e) { console.error(e) }
			    });
			},
			

			/**
			* 人脸搜索，插件beta版本
			*/
			faceSearchDemo: function () {
				faceSearch(
					(result: ResultJSON) => {
						this.faceAIResult =JSON.stringify(result)
					})
			},
			
			
			/**
			* 人脸搜索人脸特征录入，通过相机
			* 
			*/
			addFaceSearchFeatureDemo: function () {
				addFaceSearchFeature(
				     this.faceID,
					 1,           //1.快速模式2.精确模式
					 (result: ResultJSON)  => {
						//录入的人脸
						this.faceAIResult =JSON.stringify(result)
					})
			},
			
			/**
			* 删除人脸搜索人脸特征
			* 
			*/
			deleteFaceSearchFeatureDemo: function () {
				deleteFaceSearchFeature(
				     this.faceID,
					 (result: ResultJSON)  => {
						this.faceAIResult =JSON.stringify(result)
					})
			},
			
			
			/**
			* 人脸搜索人脸特征更新同步
			* 
			*/
			insertFaceSearchFeatureDemo: function () {
				insertFaceSearchFeature(
				   this.faceID,
				   this.faceFeature,
				   "tag",
				   "group",
					 (result: ResultJSON)  => {
						//录入的人脸
						this.faceAIResult =JSON.stringify(result)
					})
			},
			
			/**
			* 批量操作人脸搜索人脸特征更新同步
			* 
			*/
			insertFaceSearchFeatureSDemo: function () {
				insertManyFeatures(
				     this.faceID,
					 (result: ResultJSON)  => {
						this.faceAIResult =JSON.stringify(result)
					})

			}
			
	   }			

	})

export default __sfc__
function GenPagesIndexIndexRender(this: InstanceType<typeof __sfc__>): any | null {
const _ctx = this
const _cache = this.$.renderCache
  return _cE("view", null, [
    _cE("button", _uM({
      class: "gray-button",
      onClick: _ctx.startFaceSearchDemo
    }), "打开持续人脸搜索", 8 /* PROPS */, ["onClick"]),
    _cE("button", _uM({
      class: "gray-button",
      onClick: _ctx.faceSearchDemo
    }), "1:N人脸搜索识别", 8 /* PROPS */, ["onClick"]),
    _cE("button", _uM({
      class: "gray-button",
      onClick: _ctx.addFaceSearchFeatureDemo
    }), "1:N人脸搜索录入人脸", 8 /* PROPS */, ["onClick"]),
    _cE("button", _uM({
      class: "gray-button",
      onClick: _ctx.deleteFaceSearchFeatureDemo
    }), "删除人脸搜索特征值", 8 /* PROPS */, ["onClick"]),
    _cE("button", _uM({
      class: "gray-button",
      onClick: _ctx.insertFaceSearchFeatureDemo
    }), "同步人脸搜索特征值", 8 /* PROPS */, ["onClick"]),
    _cE("button", _uM({
      class: "gray-button",
      onClick: _ctx.insertFaceSearchFeatureSDemo
    }), "批量同步人脸搜索特征值", 8 /* PROPS */, ["onClick"]),
    _cE("view", _uM({ class: "result-box" }), [
      _cE("view", null, " Email: FaceAISDK.Service@gmail.com"),
      _cE("scroll-view", _uM({
        "scroll-y": "true",
        class: "scroll-view-box"
      }), [
        _cE("text", _uM({ class: "text-content" }), _tD(_ctx.faceAIResult), 1 /* TEXT */)
      ])
    ])
  ])
}
const GenPagesIndexIndexStyles = [_uM([["result-box", _pS(_uM([["marginTop", "20rpx"], ["marginRight", "20rpx"], ["marginBottom", "20rpx"], ["marginLeft", "20rpx"]]))], ["scroll-view-box", _pS(_uM([["height", "400rpx"], ["borderTopWidth", 1], ["borderRightWidth", 1], ["borderBottomWidth", 1], ["borderLeftWidth", 1], ["borderTopStyle", "solid"], ["borderRightStyle", "solid"], ["borderBottomStyle", "solid"], ["borderLeftStyle", "solid"], ["borderTopColor", "#cccccc"], ["borderRightColor", "#cccccc"], ["borderBottomColor", "#cccccc"], ["borderLeftColor", "#cccccc"], ["borderTopLeftRadius", "10rpx"], ["borderTopRightRadius", "10rpx"], ["borderBottomRightRadius", "10rpx"], ["borderBottomLeftRadius", "10rpx"], ["backgroundColor", "#f8f8f8"], ["paddingTop", "15rpx"], ["paddingRight", "15rpx"], ["paddingBottom", "15rpx"], ["paddingLeft", "15rpx"], ["boxSizing", "border-box"]]))], ["text-content", _pS(_uM([["fontSize", "28rpx"], ["color", "#333333"], ["whiteSpace", "pre-wrap"]]))], ["gray-button", _pS(_uM([["backgroundColor", "#ffffff"], ["color", "#800080"], ["borderTopWidth", "medium"], ["borderRightWidth", "medium"], ["borderBottomWidth", "medium"], ["borderLeftWidth", "medium"], ["borderTopStyle", "none"], ["borderRightStyle", "none"], ["borderBottomStyle", "none"], ["borderLeftStyle", "none"], ["borderTopColor", "#000000"], ["borderRightColor", "#000000"], ["borderBottomColor", "#000000"], ["borderLeftColor", "#000000"]]))]])]
