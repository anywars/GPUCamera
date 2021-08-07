package com.anypeace.gc.view

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.opengl.GLSurfaceView
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import android.view.View
import android.view.WindowManager
import com.anypeace.gc.camera.CameraHelper
import com.anypeace.gc.common.ImageLib
import com.anypeace.gc.common.Utils
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class GLPreviewView : GLSurfaceView, GLSurfaceView.Renderer, CameraHelper.OnRequestRendererListener {
    private val TAG = GLPreviewView::class.java.simpleName
    private val ORIENTATION_0 = ExifInterface.ORIENTATION_NORMAL
    private val ORIENTATION_90 = ExifInterface.ORIENTATION_ROTATE_90
    private val ORIENTATION_180 = ExifInterface.ORIENTATION_ROTATE_180
    private val ORIENTATION_270 = ExifInterface.ORIENTATION_ROTATE_270

    private var viewWidth = 0
    private var viewHeight = 0
    private var isPause = false
    var scale: Scale? = null

    private var helper: CameraHelper? = null
    private var type: CameraHelper.CameraType = CameraHelper.CameraType.BACK



    interface OnTakeListener {
        fun onTakePicture(file:String)
    }

    var filterName = "normal.shader"

    var takeListener:OnTakeListener? = null

    private var mOrientation = 0
    private val mOrientationListener: OrientationEventListener = object: OrientationEventListener(context) {
        override fun onOrientationChanged(i: Int) {
            if (i >= 360 - 45 && i < 360 || i in 0..44) {
                mOrientation = ORIENTATION_0
            } else if (i >= 45 && i < 180 - 45) {
                mOrientation = ORIENTATION_90
            } else if (i >= 180 - 45 && i < 180 + 45) {
                mOrientation = ORIENTATION_180
            } else if (i >= 180 + 45 && i < 360 - 45) {
                mOrientation = ORIENTATION_270
            }
        }
    }

    constructor(context: Context?): super(context)
    constructor(context: Context?, attrs: AttributeSet?): super(context, attrs)

    init {
        helper = CameraHelper.newInstance(context)
        setEGLContextClientVersion(2)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
        requestRender()

        ImageLib.instance().init()

        helper?.detectListener = object : CameraHelper.OnDetectListener {
            override fun onFaceDetect(bounds: Rect) {
                if (filterName == "mosaic.shader") {
                    val previewSize = helper?.getPreviewSize()

                    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    var w = 0f
                    var h = 0f
                    when (val displayRotation = wm.defaultDisplay.rotation) {
                        Surface.ROTATION_0, Surface.ROTATION_180 -> {
                            w = viewHeight.toFloat() * abs(scale!!.x)
                            h = viewWidth.toFloat() * abs(scale!!.y)
                        }
                        Surface.ROTATION_90, Surface.ROTATION_270 -> {
                            w = viewWidth.toFloat() //* abs(scale!!.y)
                            h = viewHeight.toFloat() //* abs(scale!!.x)
                        }
                        else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
                    }

                    val rect = RectF(
                        bounds.left.toFloat() / w,
                        bounds.top.toFloat() / h,
                        bounds.right.toFloat() / w,
                        bounds.bottom.toFloat() / h)
                    Log.i(TAG, "=== rect: $rect, (${w}x${h}) ===")
                    ImageLib.instance().faceDetect(rect.top, rect.left, rect.right, rect.bottom)
                }
            }
        }
    }

    // SurfaceView Override Section //
    override fun onResume() {
        super.onResume()
        try {
            mOrientationListener.enable()
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        isPause = false
    }

    override fun onPause() {
        isPause = true
        helper!!.closeCamera()
        try {
            mOrientationListener.disable()
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        super.onPause()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        viewHeight = View.resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        ImageLib.instance().setUp(viewWidth, viewHeight)
    }


    // Renderer Override Section //
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        helper?.openCamera(type, viewWidth, viewHeight)
        helper?.rendererListener = this
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewWidth = width
        viewHeight = height
        ImageLib.instance().setUp(viewWidth, viewHeight)
        calScale()
    }

    override fun onDrawFrame(gl: GL10) {
        helper?.updateTexture()
        ImageLib.instance().draw()

        if (isTakePicture) {
            val bmp = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
            ImageLib.instance().takePicture(bmp)
            val file = Utils.saveBitmap(context, bmp)
            bmp!!.recycle()
            requestRender()

            try {
                context.sendBroadcast(Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.parse("file://$file")))
                val exif = ExifInterface(file!!)
                exif.setAttribute(ExifInterface.TAG_ORIENTATION, mOrientation.toString())
                exif.setAttribute(ExifInterface.TAG_DATETIME, Utils.getCurrentDate())
                exif.saveAttributes()

                takeListener?.onTakePicture(file)
            } catch (e: Exception) {
                Log.e(TAG, "ExifInterface open error...", e)
            }
            isTakePicture = false
        }
    }

    override fun onRequestRenderer() {
        if (!isPause) requestRender()
    }

    fun setCameraType(type: CameraHelper.CameraType) {
        this.type = type
    }

    fun getCameraType(): CameraHelper.CameraType? {
        return type
    }

    fun setFilterByName(name: String?) {
        if (TextUtils.isEmpty(name)) return
        val shader = Utils.inputStreamToString(resources.assets.open(name!!))
        if (!TextUtils.isEmpty(shader)) {
            filterName = name
            calScale()
            ImageLib.instance().setFragmentShader(shader!!)
        }
    }

    fun switchCamera() {
        type = if (type === CameraHelper.CameraType.FRONT) {
            CameraHelper.CameraType.BACK
        } else {
            CameraHelper.CameraType.FRONT
        }

        helper?.closeCamera()
        helper?.openCamera(type, viewWidth, viewHeight)
        calScale()
    }

    var isTakePicture = false
    fun takePicture() {
        queueEvent {
            isTakePicture = true
        }
    }

    fun setBCS(brightness: Float, contrast: Float, saturation: Float) {
        ImageLib.instance().setBCS(brightness, contrast, saturation)
    }

    fun setFocus(x: Float, y: Float) {
        helper?.setFocus(x, y)
    }

    private fun calScale() {
        val s: CameraHelper.PreviewSize = helper!!.getPreviewSize() ?: return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var w = 0f
        var h = 0f
        when (val displayRotation = wm.defaultDisplay.rotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                w = s.height.toFloat()
                h = s.width.toFloat()
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                w = s.width.toFloat()
                h = s.height.toFloat()
            }
            else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
        }

        val ratioSurface = viewWidth.toFloat() / viewHeight.toFloat()
        val ratioPreview = w / h
        val scaleX: Float
        val scaleY: Float
        if (ratioSurface > ratioPreview) {
            scaleX = 1f
            scaleY = ratioSurface / ratioPreview
        } else {
            scaleX = ratioPreview / ratioSurface
            scaleY = 1f
        }

        scale =
        if (filterName == "fisheye.shader") {
            Scale(1f, -ratioSurface)
        }
        else {
            Scale(scaleX, if (this.type == CameraHelper.CameraType.FRONT) -scaleY else scaleY)
        }
        ImageLib.instance().setScale(scale!!.x, scale!!.y)
    }

    class Scale(var x: Float, var y: Float)
}