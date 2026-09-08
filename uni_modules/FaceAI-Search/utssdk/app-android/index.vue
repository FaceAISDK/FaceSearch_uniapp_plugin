<template>
  <view class="face-ai-capture-compat-fallback"></view>
</template>

<script lang="uts">
import CaptureFaceNativeView from "uts.sdk.modules.uniFaceAISDK.CaptureFaceNativeView"

export default {
  name: "face-ai-capture-compat",
  emits: ["ready", "result", "tips", "error", "camera-change"],
  props: {
    performanceMode: {
      type: Number,
      default: 1
    },
    needLivenessCheck: {
      type: Boolean,
      default: true
    },
    cameraId: {
      type: Number,
      default: 0
    },
    linearZoom: {
      type: Number,
      default: 0.12
    },
    rotationDegrees: {
      type: Number,
      default: -1
    },
    showFaceCover: {
      type: Boolean,
      default: false
    },
    autoStart: {
      type: Boolean,
      default: true
    }
  },
  NVLoad(): CaptureFaceNativeView {
    const captureView = new CaptureFaceNativeView(this.$androidContext!)
    captureView.setFaceCoverVisible(this.showFaceCover)
    captureView.setResultCallback((
      croppedBase64: string,
      silentScore: number,
      originBase64: string
    ) => {
      const result = new Map<string, any>()
      result.set("croppedBase64", croppedBase64)
      result.set("silentScore", silentScore)
      result.set("originBase64", originBase64)
      this.$emit("result", result)
    })
    captureView.setTipsCallback((code: number, message: string) => {
      const tips = new Map<string, any>()
      tips.set("code", code)
      tips.set("message", message)
      this.$emit("tips", tips)
    })
    captureView.setErrorCallback((code: string, message: string) => {
      const error = new Map<string, any>()
      error.set("code", code)
      error.set("message", message)
      this.$emit("error", error)
    })
    captureView.setCameraChangedCallback((cameraId: number) => {
      const result = new Map<string, any>()
      result.set("cameraId", cameraId)
      this.$emit("camera-change", result)
    })
    return captureView
  },
  NVLoaded() {
    this.$emit("ready")
    if (this.autoStart) {
      this.start()
    }
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
        this.performanceMode.toInt(),
        this.needLivenessCheck,
        this.cameraId.toInt(),
        this.linearZoom.toFloat(),
        this.rotationDegrees.toInt()
      )
    },
    stop() {
      this.$el?.stop()
    },
    retry() {
      this.$el?.retry()
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
  expose: ["start", "stop", "retry", "switchCamera", "toggleCamera", "canSwitchCamera"]
}
</script>

<style>
.face-ai-capture-compat-fallback {
  width: 100%;
  height: 100%;
  background-color: #f2f2f2;
}
</style>
