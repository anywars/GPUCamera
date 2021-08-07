package com.anypeace.gc.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.Log
import java.io.IOException

class CameraHelperLowerLollipop(context: Context) : CameraHelper(context) {
    private val TAG = CameraHelperLowerLollipop::class.java.simpleName

    private var previewSize: Camera.Size? = null
    private var camera: Camera? = null

    override fun openCamera(type: CameraType, width: Int, height: Int) {
        var cameraId: Int = when (type) {
            CameraType.BACK -> CameraInfo.CAMERA_FACING_BACK
            else -> CameraInfo.CAMERA_FACING_FRONT
        }
        cameraId = getCameraId(cameraId)
        if (cameraId == -1) return
        camera = Camera.open(cameraId)
        val params = camera?.parameters
        if (params?.supportedFocusModes!!.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
        }
        previewSize = chooseOptimalSize(params.supportedPreviewSizes, width, height)
        if (previewSize != null) {
            params.setPreviewSize(previewSize!!.width, previewSize!!.height)
        }
        camera?.parameters = params
        startPreview()
    }

    override fun closeCamera() {
        camera!!.stopPreview()
        try {
            camera!!.setPreviewTexture(null)
        } catch (e: IOException) {
            Log.e(TAG, "Error Stop PreviewTexture...", e)
        }
        camera!!.release()
        camera = null
    }

    private fun getCameraId(facing: Int): Int {
        val numberOfCameras = Camera.getNumberOfCameras()
        val info = CameraInfo()
        for (i in 0 until numberOfCameras) {
            Camera.getCameraInfo(i, info)
            if (info.facing == facing) {
                return i
            }
        }
        return -1
    }

    @SuppressLint("Recycle")
    private fun startPreview() {
        if (camera == null) {
            return
        }
        previewSurface?.setOnFrameAvailableListener(null)
        previewSurface = SurfaceTexture(0)
        previewSurface?.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        previewSurface?.setOnFrameAvailableListener(OnFrameAvailableListener { if (rendererListener != null) rendererListener?.onRequestRenderer() })
        try {
            camera!!.setPreviewTexture(previewSurface)
        } catch (e: IOException) {
            Log.e(TAG, "Error Bind Surface...", e)
        }
        camera!!.startPreview()
    }

    override fun getPreviewSize(): PreviewSize {
        return if (previewSize != null) PreviewSize(
            previewSize!!.width,
            previewSize!!.height
        ) else PreviewSize(0, 0)
    }

    override fun updateTexture() {
        if (camera != null) super.updateTexture()
    }

    override fun setFocus(x: Float, y: Float) {}

    private fun chooseOptimalSize(choices: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = w.toDouble() / h
        if (choices == null || choices.size == 0) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE
        for (size in choices) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in choices) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }
}