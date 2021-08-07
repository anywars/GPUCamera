package com.anypeace.gc.common

import android.graphics.Bitmap

class ImageLib {
    companion object {
        init {
            System.loadLibrary("gpucamera")
        }

        private var instance: ImageLib? = null
        fun instance(): ImageLib = this.instance ?: synchronized(this) {
            instance ?: ImageLib().also { instance = it }
        }
    }

    external fun stringFromJNI(): String
    external fun init()
    external fun setUp(width: Int, height: Int)
    external fun draw()
    external fun setScale(x: Float, y: Float)
    external fun setFragmentShader(shader: String?)
    external fun setVertexShader(shader: String?)
    external fun faceDetect(top: Float, left: Float, right: Float, bottom: Float)
    external fun setBCS(brightness: Float, contrast: Float, saturation: Float)
    external fun setHueType(type: Int)
    external fun takePicture(bitmap: Bitmap?)

}