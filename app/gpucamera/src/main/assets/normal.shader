#extension GL_OES_EGL_image_external : require
#ifdef GL_ES
precision mediump float;
#endif


uniform vec3 uBCS;
uniform samplerExternalOES uTexture;
varying vec2 vTextureCoordinate;

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
   vec4 color = texture2D(uTexture, vTextureCoordinate);
   gl_FragColor = brightnessContrastSaturation(color, uBCS.r, uBCS.g, uBCS.b);
}