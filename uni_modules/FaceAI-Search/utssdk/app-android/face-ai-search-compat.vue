<template>
  <view class="face-ai-search-compat-fallback"></view>
</template>

<script lang="uts">
import FaceSearchNativeView from "uts.sdk.modules.uniFaceAISDK.FaceSearchNativeView"

export default {
  name: "face-ai-search-compat",
  emits: ["ready", "result", "tips", "error", "camera-change"],
  props: {
    searchThreshold: { type: Number, default: 0.86 },
    searchOneTime: { type: Boolean, default: false },
    searchTimeOut: { type: Number, default: 4000 },
    needLivenessCheck: { type: Boolean, default: true },
    cameraId: { type: Number, default: 0 },
    linearZoom: { type: Number, default: 0.12 },
    rotationDegrees: { type: Number, default: -1 },
    showFaceCover: { type: Boolean, default: true },
    autoStart: { type: Boolean, default: true }
  },
  NVLoad(): FaceSearchNativeView {
    const searchView = new FaceSearchNativeView(this.$androidContext!)
    searchView.setFaceCoverVisible(this.showFaceCover)
    searchView.setResultCallback((matchesJson: string, liveness: number, base64: string) => {
      const result = new Map<string, any>()
      result.set("matchesJson", matchesJson)
      result.set("liveness", liveness)
      result.set("base64", base64)
      this.$emit("result", result)
    })
    searchView.setTipsCallback((code: number, message: string) => {
      const tips = new Map<string, any>()
      tips.set("code", code)
      tips.set("message", message)
      this.$emit("tips", tips)
    })
    searchView.setErrorCallback((code: string, message: string) => {
      const error = new Map<string, any>()
      error.set("code", code)
      error.set("message", message)
      this.$emit("error", error)
    })
    searchView.setCameraChangedCallback((cameraId: number) => {
      const result = new Map<string, any>()
      result.set("cameraId", cameraId)
      this.$emit("camera-change", result)
    })
    return searchView
  },
  NVLoaded() {
    this.$emit("ready")
    if (this.autoStart) this.start()
  },
  NVBeforeUnload() {
    this.$el?.release()
  },
  unmounted() {
    this.$el?.release()
  },
  methods: {
    start() {
      this.$el?.setFaceCoverVisible(this.showFaceCover)
      this.$el?.start(
        this.searchThreshold.toFloat(),
        this.searchOneTime,
        this.searchTimeOut.toInt(),
        this.needLivenessCheck,
        this.cameraId.toInt(),
        this.linearZoom.toFloat(),
        this.rotationDegrees.toInt()
      )
    },
    stop() {
      this.$el?.stop()
    },
    switchCamera(cameraId: number) {
      this.$el?.switchCamera(cameraId.toInt())
    },
    toggleCamera() {
      this.$el?.toggleCamera()
    },
    canSwitchCamera(): boolean {
      return this.$el?.canSwitchCamera() ?? false
    }
  },
  expose: ["start", "stop", "switchCamera", "toggleCamera", "canSwitchCamera"]
}
</script>

<style>
.face-ai-search-compat-fallback {
  width: 100%;
  height: 100%;
  background-color: #000000;
}
</style>
