package com.anypeace.gc.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Point
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.text.TextUtils
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.*
import java.util.Collections.max
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CameraHelperBase(context: Context) : CameraHelper(context) {
    private val TAG = CameraHelperBase::class.java.simpleName

    private var cameraDevice: CameraDevice? = null
    private var previewRequest: CaptureRequest? = null
    private var previewBuilder: CaptureRequest.Builder? = null
    private var previewSession: CameraCaptureSession? = null
    private var cameraInfo: CameraCharacteristics? = null
    private var manager: WindowManager? = null

    private var previewSize: Size? = null
    private var cameraId: String? = null
    private var isFlashSupported = false
    private var isAFSupported = false
    private var sensorOrientation = 0

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private val cameraOpenCloseLock: Semaphore? = Semaphore(1)


    init {
        manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread!!.quitSafely()
            try {
                backgroundThread!!.join()
                backgroundThread = null
                backgroundHandler = null
            } catch (e: Exception) {
                Log.e(TAG, "", e)
            }
        }
    }

    override fun openCamera(type: CameraType, width: Int, height: Int) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) !== PackageManager.PERMISSION_GRANTED) {
            return
        }
        startBackgroundThread()
        setUpCameraOutputs(type, width, height)
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock!!.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            manager.openCamera(cameraId!!, mStateCallback, backgroundHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun closeCamera() {
        try {
            cameraOpenCloseLock?.acquire()
            if (previewSession != null) {
                previewSession!!.close()
                previewSession = null
            }
            if (cameraDevice != null) {
                cameraDevice!!.close()
                cameraDevice = null
            }
            stopBackgroundThread()

            if (previewSurface != null) {
                previewSurface = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        } finally {
            cameraOpenCloseLock!!.release()
        }
    }

    private fun startPreview() {
        if (cameraDevice == null || previewSize == null) {
            return
        }

        previewSurface = SurfaceTexture(0)
        previewSurface?.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
        previewSurface?.setOnFrameAvailableListener {
            rendererListener?.onRequestRenderer()
        }

        val surface = Surface(previewSurface)
        try {
            previewBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        previewBuilder?.addTarget(surface)
        try {
            cameraDevice?.createCaptureSession(listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(@NonNull session: CameraCaptureSession) {
                        if (cameraDevice == null) {
                            return
                        }

                        previewSession = session
                        previewBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        previewBuilder?.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)

                        previewBuilder?.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, CaptureRequest.STATISTICS_FACE_DETECT_MODE_SIMPLE)
                        if (isFlashSupported) {
                            previewBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
                        }
                        previewRequest = previewBuilder?.build()

                        try {
                            previewSession?.setRepeatingRequest(previewRequest!!, captureCallback, backgroundHandler)
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "", e)
                        }
                    }

                    override fun onConfigureFailed(@NonNull session: CameraCaptureSession) {
                        Toast.makeText(context, "onConfigureFailed", Toast.LENGTH_LONG).show()
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun setUpCameraOutputs(type: CameraType, width: Int, height: Int) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            var characteristics: CameraCharacteristics? = null
            for (cameraId in manager.cameraIdList) {
                val c = manager.getCameraCharacteristics(cameraId)
                val i = c.get(CameraCharacteristics.LENS_FACING)!!
                if (type === CameraType.FRONT && i == CameraCharacteristics.LENS_FACING_FRONT) {
                    this.cameraId = cameraId
                    characteristics = c
                } else if (type === CameraType.BACK && i == CameraCharacteristics.LENS_FACING_BACK) {
                    this.cameraId = cameraId
                    characteristics = c
                }
            }
            if (TextUtils.isEmpty(this.cameraId) || characteristics == null) {
                throw RuntimeException("Not supported camera type...")
            }
            cameraInfo = characteristics
            val map = cameraInfo!!.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: throw RuntimeException("Cannot get available preview/video sizes")

            val displayRotation = this.manager!!.defaultDisplay.rotation
            sensorOrientation = cameraInfo!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            var swappedDimensions = false
            when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 ->
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        swappedDimensions = true
                    }

                Surface.ROTATION_90, Surface.ROTATION_270 ->
                    if (sensorOrientation == 0 || sensorOrientation == 180) {
                        swappedDimensions = true
                    }

                else -> Log.e(TAG, "Display rotation is invalid: $displayRotation")
            }
            val displaySize = Point()
            this.manager!!.defaultDisplay.getSize(displaySize)
            var rotatedPreviewWidth = width
            var rotatedPreviewHeight = height
            var maxPreviewWidth = displaySize.x
            var maxPreviewHeight = displaySize.y
            if (swappedDimensions) {
                rotatedPreviewWidth = height
                rotatedPreviewHeight = width
                maxPreviewWidth = displaySize.y
                maxPreviewHeight = displaySize.x
            }
            val largest = max(listOf(*map.getOutputSizes(ImageFormat.YUV_420_888)), CompareSizesByArea())

            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture::class.java), rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest)

            val available = cameraInfo!!.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            isFlashSupported = available ?: false

            isAFSupported = cameraInfo!!.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)!! >= 1
        } catch (ce: CameraAccessException) {
            Log.e(TAG, "", ce)
            Toast.makeText(context, "Cannot access the camera", Toast.LENGTH_SHORT).show()
            if (context is Activity) {
                (context as Activity).finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
    }

    private val captureCallback: CaptureCallback = object : CaptureCallback() {
        override fun onCaptureProgressed(@NonNull session: CameraCaptureSession, @NonNull request: CaptureRequest, @NonNull partialResult: CaptureResult) {
            process(partialResult)
        }

        override fun onCaptureCompleted(@NonNull session: CameraCaptureSession, @NonNull request: CaptureRequest, @NonNull result: TotalCaptureResult) {
            process(result)
        }

        private fun process(result: CaptureResult) {
            val mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE)
            val faces = result.get(CaptureResult.STATISTICS_FACES)
            if (faces != null && faces.isNotEmpty() && mode != null) {
                val face = faces[0]
                val bounds = face.bounds
                detectListener?.onFaceDetect(bounds)

//                if (previewSize != null) {
//                    val w = previewSize!!.width.toFloat()
//                    val h = previewSize!!.height.toFloat()
//                    ImageLib.instance().faceDetect(
//                        bounds.top.toFloat() / h,
//                        bounds.left.toFloat() / w,
//                        bounds.right.toFloat() / w,
//                        bounds.bottom.toFloat() / h
//                    )
//                }
            }

        }
    }

    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(@NonNull camera: CameraDevice) {
            cameraOpenCloseLock!!.release()
            cameraDevice = camera
            startPreview()
        }

        override fun onDisconnected(@NonNull camera: CameraDevice) {
            cameraOpenCloseLock!!.release()
            camera.close()
            cameraDevice = null
        }

        override fun onError(@NonNull camera: CameraDevice, error: Int) {
            cameraOpenCloseLock!!.release()
            camera.close()
            cameraDevice = null
        }
    }

    override fun getPreviewSize(): PreviewSize? {
        return if (previewSize != null) PreviewSize(previewSize!!.width, previewSize!!.height) else null
    }

    override fun setFocus(x: Float, y: Float) {
        if (!isAFSupported) return
        val sensorArraySize = cameraInfo!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val yy = (x / previewSize!!.width.toFloat() * sensorArraySize!!.height().toFloat()).toInt()
        val xx = (y / previewSize!!.height.toFloat() * sensorArraySize.width().toFloat()).toInt()
        val halfTouchWidth = 150
        val halfTouchHeight = 150
        val focusArea = MeteringRectangle(kotlin.math.max(xx - halfTouchWidth, 0), kotlin.math.max(yy - halfTouchHeight, 0), halfTouchWidth * 2, halfTouchHeight * 2, MeteringRectangle.METERING_WEIGHT_MAX - 1)
        previewBuilder!!.set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(focusArea))
    }

    private fun chooseOptimalSize(choices: Array<Size>, textureWidth: Int, textureHeight: Int, maxWidth: Int, maxHeight: Int, aspectRatio: Size): Size? {
        val bigEnough = ArrayList<Size>()
        val notBigEnough = ArrayList<Size>()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.width <= maxWidth && option.height <= maxHeight &&
                    option.height == option.width * h / w) {
                if (option.width >= textureWidth &&
                    option.height >= textureHeight) {
                    bigEnough.add(option)
                } else {
                    notBigEnough.add(option)
                }
            }
        }

        if (bigEnough.isNotEmpty()) {
            return Collections.min(bigEnough, CompareSizesByArea())
        } else if (notBigEnough.isNotEmpty()) {
            return max(notBigEnough, CompareSizesByArea())
        } else {
            return choices[0]
        }
    }

    private class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }
    }
}