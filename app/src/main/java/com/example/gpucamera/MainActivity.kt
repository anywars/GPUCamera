package com.example.gpucamera

import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.anypeace.gc.camera.CameraHelper
import com.example.gpucamera.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val REQ_PERMISSION = 100
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val deniedPermissions = arrayListOf<String>()
        val pi = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        pi.requestedPermissions?.forEach { p ->
            if (p != "com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE" &&
                    ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED) {
                deniedPermissions.add(p)
            }
        }

        @TargetApi(Build.VERSION_CODES.M)
        if (deniedPermissions.isNotEmpty()) {
            requestPermissions(deniedPermissions.toTypedArray(), REQ_PERMISSION)
        }
        else {
            init()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSION) {
            if (isPermission(permissions)) {
                init()
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun isPermission(permissions: Array<out String>): Boolean {
        for (p in permissions) {
            if (ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_DENIED) {
                if (!shouldShowRequestPermissionRationale(p)) {
                    Toast.makeText(this, "권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
                return false
            }
        }
        return true
    }

    override fun onResume() {
        binding.gvPreview.onResume()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.gvPreview.onPause()
    }


    private val filters = arrayListOf(
        "billboard.shader",
        "fisheye.shader",
        "hueSeeker.shader",
        "mirror.shader",
//        "mosaic.shader",
        "normal.shader",
        "retro.shader",
        "sketch.shader",
        "toon.shader"
    )
    private var currentFilter = 0
    private val detector by lazy {
        GestureDetectorCompat(this, object: GestureDetector.OnGestureListener {
            override fun onDown(e: MotionEvent?): Boolean = true
            override fun onShowPress(e: MotionEvent?) {}
            override fun onLongPress(e: MotionEvent?) {}
            override fun onSingleTapUp(e: MotionEvent?): Boolean = true
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean = true

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (velocityX > 1) {
                    currentFilter++
                    if (currentFilter >= filters.count())
                        currentFilter = 0
                }
                else {
                    currentFilter--
                    if (currentFilter < 0)
                        currentFilter = filters.count()-1
                }
                setFilter(filters[currentFilter])
                return true
            }
        })
    }

    private fun init() {
        binding.gvPreview.setCameraType(CameraHelper.CameraType.FRONT)
        binding.gvPreview.setBCS(.7f, 1.2f, 1f)
        binding.gvPreview.setOnTouchListener { _, event ->
            return@setOnTouchListener detector.onTouchEvent(event)
        }

        setFilter(filters[currentFilter])
    }

    private fun setFilter(filterName: String) {
        binding.gvPreview.setFilterByName(filterName)
        binding.tvFilter.text = filterName
    }
}