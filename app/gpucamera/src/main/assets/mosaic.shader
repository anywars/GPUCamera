#extension GL_OES_EGL_image_external : require
#ifdef GL_ES
precision mediump float;
#endif


uniform samplerExternalOES uTexture;
varying vec2 vTextureCoordinate;

uniform vec3 uBCS;
uniform vec4 uFace;
uniform mediump float uWidth;
uniform mediump float uHeight;
const float pixel_w = 20.0;
const float pixel_h = 20.0;
//const float stroke = .001;
const float vx_offset = .5;

const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);

vec4 brightnessContrastSaturation(vec4 color, float brt, float con, float sat) {
    vec3 black = vec3(0., 0., 0.);
    vec3 middle = vec3(0.5, 0.5, 0.5);
    float luminance = dot(color.rgb, W);
    vec3 gray = vec3(luminance, luminance, luminance);
    vec3 brtColor = mix(black, color.rgb, brt);
    vec3 conColor = mix(middle, brtColor, con);
    vec3 satColor = mix(gray, conColor, sat);
    return vec4(satColor, 1.0);
}

void main() {
    vec2 uv = vTextureCoordinate.xy;
    vec3 tc = vec3(1.0, 0.0, 0.0);
//    if ((uv.x-stroke < uFace.z && uv.x+stroke > uFace.z) ||
//        (uv.x-stroke < uFace.y && uv.x+stroke > uFace.y) ||
//        (uv.y-stroke < uFace.w && uv.y+stroke > uFace.w) ||
//        (uv.y-stroke < uFace.x && uv.y+stroke > uFace.x)) {
////        float dx = pixel_w*(1./uWidth);
////        float dy = pixel_h*(1./uHeight);
////        vec2 coord = vec2(dx*floor(uv.x/dx), dy*floor(uv.y/dy));
////        tc = texture2D(uTexture, coord).rgb;
//        tc = vec3(1, 1, 1);
//    }
    if (uv.x < uFace.z &&
        uv.x > uFace.y &&
        uv.y < uFace.w &&
        uv.y > uFace.x) {
        float dx = pixel_w*(1./uWidth);
        float dy = pixel_h*(1./uHeight);
        vec2 coord = vec2(dx*floor(uv.x/dx), dy*floor(uv.y/dy));
        tc = texture2D(uTexture, coord).rgb;
    }
    else {
        tc = texture2D(uTexture, uv).rgb;
    }
    vec4 color = vec4(tc, 1.0);
    gl_FragColor = brightnessContrastSaturation(color, uBCS.r, uBCS.g, uBCS.b);
}