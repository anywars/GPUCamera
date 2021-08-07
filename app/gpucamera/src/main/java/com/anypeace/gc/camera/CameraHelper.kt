package com.anypeace.gc.camera

import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.os.Build


open class CameraHelper(protected val context: Context) {
    protected var previewSurface: SurfaceTexture? = null
    var rendererListener: OnRequestRendererListener? = null
    var detectListener: OnDetectListener? = null

    var isEnableFaceDetect = false

    companion object {
        fun newInstance(context: Context): CameraHelper {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CameraHelperBase(context)
            }
            else {
                CameraHelperLowerLollipop(context)
            }
        }
    }

    open fun updateTexture() {
        previewSurface?.updateTexImage()
    }

    open fun getPreviewSize(): PreviewSize? = null
    open fun openCamera(type :CameraType, width: Int, height: Int) {}
    open fun closeCamera() {}
    open fun setFocus(x:Float, y:Float) {}


    interface OnRequestRendererListener {
        fun onRequestRenderer()
    }

    interface OnDetectListener {
        fun onFaceDetect(bounds: Rect)
    }

    class PreviewSize(val width: Int, val height: Int) {}

    enum class CameraType(private var type: Int) {
        FRONT(0xFF0000), BACK(0xFF0001);
        fun type(): Int = type
    }

}

