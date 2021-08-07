package com.anypeace.gc.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class Utils {
    companion object {
        private val TAG: String = Utils::class.java.simpleName
        private val FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        fun inputStreamToString(`is`: InputStream?): String? {
            val sbText = StringBuilder()
            val buffer = ByteArray(1024)
            try {
                var readCnt: Int
                while (`is`!!.read(buffer).also { readCnt = it } != -1) {
                    sbText.append(String(buffer, 0, readCnt))
                }
            } catch (e: Exception) {
                Log.e(TAG, "", e)
            } finally {
                if (`is` != null) try {
                    `is`.close()
                } catch (e: Exception) {
                }
            }
            return sbText.toString()
        }

        fun saveBitmap(context: Context, bitmap: Bitmap?): String? {
            val path = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                File(context.externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")

            } else {
                File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "${System.currentTimeMillis()}.jpg")
            }

            val w = bitmap!!.width
            val h = bitmap.height
            val m = Matrix()
            m.postRotate(180f)
            m.postScale(-1f, 1f)

            val bmp = Bitmap.createBitmap(bitmap, 0, 0, w, h, m, true)

            var isSuccess = false
            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(path)
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, fos)
                isSuccess = true
            } catch (e: Exception) {
                Log.e(TAG, "File output error...", e)
            } finally {
                if (fos != null) try {
                    fos.close()
                } catch (e: Exception) {
                }
            }

            if (!bmp.isRecycled)
                bmp.recycle()
            return if (isSuccess) path.absolutePath else null
        }

        fun getCurrentDate(): String? {
            return FORMAT.format(Date())
        }
    }
}