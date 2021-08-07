#ifndef FILTERCAMERA_IMAGELIB_H
#define FILTERCAMERA_IMAGELIB_H

#include <cstdlib>
#include <cstring>
#include <cmath>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#include <android/log.h>

#define	TAG    "ANYPEACE_LIB"
#define	LOGI(...)  __android_log_print(ANDROID_LOG_INFO,TAG,__VA_ARGS__)
#define	LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,TAG,__VA_ARGS__)


class ImageLib {
public:
    ImageLib();
    ~ImageLib();

    void Init();
    void SetUp(int width, int height);
    void Draw();
    void SetScale(float x, float y);
    void SetAngle(float angle);
    void SetPosition(float x, float y);
    void SetFragmentShader(const char* shader);
    void SetVertexShader(const char* shader);
    void SetFaceDetect(float top, float left, float right, float bottom);
    void SetBrightnessContrastSaturation(float fBrightness, float fContrast, float fSaturation);
    void SetHueType(int type);

protected:
    static const GLfloat m_fVertices[24];
    static const GLushort m_fIndices[4];

private:
    GLuint m_hProgram;
    bool m_bNeedCompile = true;

    float m_fWidth;
    float m_fHeight;

    GLfloat m_fScale[16];
    GLfloat m_fRotate[16];
    GLfloat m_fFace[4];
    GLfloat m_fBCS[3];
    GLint m_iHue;
    char *m_pVertexShader;
    char *m_pFragmentShader;
    const int m_iStride = 6 * sizeof(GLfloat);

    void CreateProgram(const char* pVertexSource, const char* pFragmentSource);
    GLuint LoadShader(GLenum shaderType, const char* pSource);
    bool CheckGlError(const char* op);
};

#endif //FILTERCAMERA_IMAGELIB_H
