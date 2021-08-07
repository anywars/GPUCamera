#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include "ImageLib.h"


extern "C" {

    ImageLib *g_pImageLib;


    JNIEXPORT jstring JNICALL
    Java_com_anypeace_gc_common_ImageLib_stringFromJNI(
            JNIEnv *env,
            jobject /* this */) {
        std::string hello = "Hello from C++!!!!";
        return env->NewStringUTF(hello.c_str());
    }


    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_init(
            JNIEnv *env,
            jobject /* this */) {

        if (!g_pImageLib)
            g_pImageLib = new ImageLib();
        g_pImageLib->Init();
    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_setFragmentShader(
            JNIEnv *env,
            jobject /* this */,
            jstring shader) {
        const char *strShader = env->GetStringUTFChars(shader, JNI_FALSE);
        g_pImageLib->SetFragmentShader(strShader);
        env->ReleaseStringUTFChars(shader, strShader);
    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_setVertexShader(
            JNIEnv *env,
            jobject /* this */,
            jstring shader) {
        const char *strShader = env->GetStringUTFChars(shader, JNI_FALSE);
        g_pImageLib->SetVertexShader(strShader);
        env->ReleaseStringUTFChars(shader, strShader);


    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_setScale(
            JNIEnv *env,
            jobject /* this */,
            jfloat x,
            jfloat y) {
        g_pImageLib->SetScale(x, y);
    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_setUp(
            JNIEnv *env,
            jobject /* this */,
            jint width,
            jint height) {
        g_pImageLib->SetUp(width, height);
    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_draw(
            JNIEnv *env,
            jobject /* this */) {
        g_pImageLib->Draw();
    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_faceDetect(
            JNIEnv *env,
            jobject /* this */,
            jfloat top,
            jfloat left,
            jfloat right,
            jfloat bottom) {
        if (g_pImageLib)
            g_pImageLib->SetFaceDetect(top, left, right, bottom);
    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_setBCS(
            JNIEnv *env,
            jobject /* this */,
            jfloat brightness,
            jfloat contrast,
            jfloat saturation) {
        if (g_pImageLib)
            g_pImageLib->SetBrightnessContrastSaturation(brightness, contrast, saturation);
    }

    JNIEXPORT void JNICALL
    Java_com_anypeace_gc_common_ImageLib_setHueType(
            JNIEnv *env,
            jobject /* this */,
            jint hueType) {
        if (g_pImageLib)
            g_pImageLib->SetHueType(hueType);
    }

    JNIEXPORT void JNICALL Java_com_anypeace_gc_common_ImageLib_takePicture(
            JNIEnv *env,
            jobject /* this */,
            jobject bitmap) {

        AndroidBitmapInfo info;
        int *pixels;

        int ret;

        glPixelStorei(GL_PACK_ALIGNMENT, 1);

        if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
            LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
            return;
        }

        if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            LOGE("Bitmap format is not RGB_565");
            return;
        }

        if ((ret = AndroidBitmap_lockPixels(env, bitmap, (void **) &pixels)) < 0) {
            LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        }

        glReadPixels(0, 0, info.width, info.height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        AndroidBitmap_unlockPixels(env, bitmap);
    }

}