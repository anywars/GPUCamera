//
// Created by user on 2017. 9. 14..
//

#include "ImageLib.h"


const GLfloat ImageLib::m_fVertices[] = {
    -1.f, 1.f, 0.0f, 1.f,   // Position 0
    0.0f,  1.f,             // TexCoord 0
    1.f, 1.f, 0.0f, 1.0f,    // Position 1
    1.f,  1.f,              // TexCoord 1
    -1.f, -1.f, 0.0f, 1.0f,  // Position 2
    0.0f,  0.0f,            // TexCoord 2
    1.f,  -1.f, 0.0f, 1.f,  // Position 3
    1.f,  0.0f              // TexCoord 3
};

const GLushort ImageLib::m_fIndices[] = {
    0, 1, 2, 3
};


ImageLib::ImageLib() {
    SetScale(1, 1);
    SetAngle(0);

    for (int i=0; i<4; i++) {
        m_fFace[i] = 0;
    }

    for (int i=0; i<3; i++) {
        m_fBCS[i] = 1.f;
    }
    m_iHue = 1;
}

ImageLib::~ImageLib() {
}

void ImageLib::Init() {
    const char* pDefaultVertexShader =
        "attribute vec4 aPosition;\n"
        "attribute vec2 aTexCoord;\n"

        "uniform mat4 uScale;\n"
        "uniform mat4 uRotate;\n"

        "varying vec2 vTextureCoordinate;\n"

        "void main() {\n"
        "	gl_Position = uScale * uRotate * aPosition;\n"
        "	vTextureCoordinate = aTexCoord;\n"
        "}";

    const char* pDefaultFragmentShader =
        "#extension GL_OES_EGL_image_external : require\n"
        "#ifdef GL_ES\n"
        "precision mediump float;\n"
        "#endif\n"

        "uniform samplerExternalOES uTexture;"
        "varying vec2 vTextureCoordinate;\n"

        "void main() {\n"
        "   gl_FragColor = texture2D(uTexture, vTextureCoordinate);\n"
        "}\n";

    glClearColor(.0f, .0f, .0f, 1.0f);

    int size = strlen(pDefaultVertexShader) * sizeof(char *);
    m_pVertexShader = (char *) malloc(size);
    strcpy(m_pVertexShader, pDefaultVertexShader);

    size = strlen(pDefaultFragmentShader) * sizeof(char *);
    m_pFragmentShader = (char *) malloc(size);
    strcpy(m_pFragmentShader, pDefaultFragmentShader);

    m_bNeedCompile = true;
}

void ImageLib::SetUp(int width, int height) {
    m_fWidth = width;
    m_fHeight = height;

    m_bNeedCompile = true;
    glViewport(0, 0, width, height);
};

void ImageLib::Draw() {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

    if (m_bNeedCompile) {
        CreateProgram(m_pVertexShader, m_pFragmentShader);
        if (!m_hProgram) {
            LOGE("Could not create fragment shader program....");
        }
        m_bNeedCompile = false;
    }
    glUseProgram(m_hProgram);


    GLuint ph   = glGetAttribLocation(m_hProgram, "aPosition");
    glEnableVertexAttribArray(ph);
    CheckGlError("glEnableVertexAttribArray ph");

    GLuint tch	= glGetAttribLocation(m_hProgram, "aTexCoord");
    glEnableVertexAttribArray(tch);
    CheckGlError("glEnableVertexAttribArray tch");

    GLuint uf = glGetUniformLocation(m_hProgram, "uFace");
    if (uf != GL_INVALID_VALUE) {
        glUniform4f(uf, m_fFace[0], m_fFace[1], m_fFace[2], m_fFace[3]);
    }

    GLuint uw = glGetUniformLocation(m_hProgram, "uWidth");
    if (uw != GL_INVALID_VALUE) {
        glUniform1f(uw, m_fWidth);
    }

    GLuint uh = glGetUniformLocation(m_hProgram, "uHeight");
    if (uh != GL_INVALID_VALUE) {
        glUniform1f(uh, m_fHeight);
    }

    GLuint us   = glGetUniformLocation(m_hProgram, "uScale");
    if (us != GL_INVALID_VALUE) {
        glUniformMatrix4fv(us, 1, GL_FALSE, m_fScale);
    }

    GLuint ur   = glGetUniformLocation(m_hProgram, "uRotate");
    if (ur != GL_INVALID_VALUE) {
        glUniformMatrix4fv(ur, 1, GL_FALSE, m_fRotate);
    }

    GLuint ubcs = glGetUniformLocation(m_hProgram, "uBCS");
    if (ubcs != GL_INVALID_VALUE) {
        glUniform3f(ubcs, m_fBCS[0], m_fBCS[1], m_fBCS[2]);
    }

    GLuint uhue = glGetUniformLocation(m_hProgram, "uHue");
    if (uhue != GL_INVALID_VALUE) {
        glUniform1i(uhue, m_iHue);
    }


    GLuint th	= glGetUniformLocation(m_hProgram, "uTexture");
    glEnableVertexAttribArray(th);

    glVertexAttribPointer(ph, 4, GL_FLOAT, GL_FALSE, m_iStride, m_fVertices);
    CheckGlError("glVertexAttribPointer ph");

    glVertexAttribPointer(tch, 2, GL_FLOAT, GL_FALSE, m_iStride, &m_fVertices[4]);
    CheckGlError("glVertexAttribPointer tch");

    glDrawElements(GL_TRIANGLE_STRIP, 4, GL_UNSIGNED_SHORT, m_fIndices);
    CheckGlError("gldrawElements");

    glDisableVertexAttribArray(ph);
    glDisableVertexAttribArray(tch);
}

void ImageLib::CreateProgram(const char *pVertexSource, const char *pFragmentSource) {
    GLuint hVertexShader = LoadShader(GL_VERTEX_SHADER, pVertexSource);
    if (!hVertexShader) {
        return;
    }

    GLuint hFragmentShader = LoadShader(GL_FRAGMENT_SHADER, pFragmentSource);
    if (!hFragmentShader) {
        return;
    }

    m_hProgram = glCreateProgram();
    if (m_hProgram) {
        glAttachShader(m_hProgram, hVertexShader);
        CheckGlError("glAttachShader");

        glAttachShader(m_hProgram, hFragmentShader);
        CheckGlError("glAttachShader");

        glLinkProgram(m_hProgram);
        GLint linkStatus = GL_FALSE;
        glGetProgramiv(m_hProgram, GL_LINK_STATUS, &linkStatus);

        LOGI("Program Linked!");
        if (linkStatus != GL_TRUE) {
            GLint bufLength = 0;
            glGetProgramiv(m_hProgram, GL_INFO_LOG_LENGTH, &bufLength);
            if (bufLength) {
                char* buf = (char*) malloc(bufLength);
                if (buf) {
                    glGetProgramInfoLog(m_hProgram, bufLength, NULL, buf);
                    LOGE("Could not link program:\n%s\n", buf);
                    free(buf);
                }
            }
            glDeleteProgram(m_hProgram);
            m_hProgram = 0;
        }
    }
}

bool ImageLib::CheckGlError(const char *op) {
    GLint error = glGetError();
    if (error != 0) {
        LOGE("after %s() glError (0x%x)", op, error);
        return false;
    }
    return true;
}

GLuint ImageLib::LoadShader(GLenum shaderType, const char* pSource) {
    GLuint shader = glCreateShader(shaderType);
    if (shader) {
        glShaderSource(shader, 1, &pSource, NULL);
        glCompileShader(shader);
        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint infoLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
            if (infoLen) {
                char* buf = (char*) malloc(infoLen);
                if (buf) {
                    glGetShaderInfoLog(shader, infoLen, NULL, buf);
                    LOGE("Could not compile shader %d:\n%s\n",
                         shaderType, buf);
                    free(buf);
                }
                glDeleteShader(shader);
                shader = 0;
            }
        }
    }
    return shader;
}

void ImageLib::SetScale(float x, float y) {
    for (int i=0; i<16; i++) {
        m_fScale[i] = 0.f;
    }

    m_fScale[0] = x;
    m_fScale[5] = -y;
    m_fScale[10] = 1.f;
    m_fScale[15] = 1.f;
}

void ImageLib::SetPosition(float x, float y) {


}

void ImageLib::SetAngle(float angle) {
    for (int i=0; i<16; i++) {
        m_fRotate[i] = 0.f;
    }

    m_fRotate[1] = 1.f;
    m_fRotate[4] = -1.f;
    m_fRotate[10] = 1.f;
    m_fRotate[15] = 1.f;
}


void ImageLib::SetFragmentShader(const char *shader) {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    free(m_pFragmentShader);

    int size = strlen(shader) * sizeof(char *);
    m_pFragmentShader = (char *) malloc(size);
    strcpy(m_pFragmentShader, shader);

    m_bNeedCompile = true;
}

void ImageLib::SetVertexShader(const char *shader) {
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    free(m_pVertexShader);

    int size = strlen(shader) * sizeof(char *);
    m_pVertexShader = (char *) malloc(size);
    strcpy(m_pVertexShader, shader);

    m_bNeedCompile = true;
}

void ImageLib::SetFaceDetect(float top, float left, float right, float bottom) {
    m_fFace[0] = top;
    m_fFace[1] = left;
    m_fFace[2] = right;
    m_fFace[3] = bottom;
}

void ImageLib::SetBrightnessContrastSaturation(float fBrightness, float fContrast,
                                               float fSaturation) {
    m_fBCS[0] = fBrightness;
    m_fBCS[1] = fContrast;
    m_fBCS[2] = fSaturation;
}

void ImageLib::SetHueType(int type) {
    m_iHue = type;
}