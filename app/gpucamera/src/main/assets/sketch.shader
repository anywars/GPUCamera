#extension GL_OES_EGL_image_external : require
#ifdef GL_ES
precision mediump float;
#endif

uniform vec3 uBCS;
uniform samplerExternalOES uTexture;
varying vec2 vTextureCoordinate;

const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);
const float intensity = 1.0;

uniform mediump float uWidth;
uniform mediump float uHeight;

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

vec4 sketch() {
	mediump vec3 textureColor = texture2D(uTexture, vTextureCoordinate).rgb;
	mediump vec2 stp0 = vec2(1.0 / uWidth, 0.0);
	mediump vec2 st0p = vec2(0.0, 1.0 / uHeight);
	mediump vec2 stpp = vec2(1.0 / uWidth, 1.0 / uHeight);
	mediump vec2 stpm = vec2(1.0 / uWidth, -1.0 / uHeight);

	mediump float im1m1 = dot(texture2D(uTexture, vTextureCoordinate - stpp).rgb, W);
	mediump float ip1p1 = dot(texture2D(uTexture, vTextureCoordinate + stpp).rgb, W);
	mediump float im1p1 = dot(texture2D(uTexture, vTextureCoordinate - stpm).rgb, W);
	mediump float ip1m1 = dot(texture2D(uTexture, vTextureCoordinate + stpm).rgb, W);
	mediump float im10 = dot(texture2D(uTexture, vTextureCoordinate - stp0).rgb, W);
	mediump float ip10 = dot(texture2D(uTexture, vTextureCoordinate + stp0).rgb, W);
	mediump float i0m1 = dot(texture2D(uTexture, vTextureCoordinate - st0p).rgb, W);
	mediump float i0p1 = dot(texture2D(uTexture, vTextureCoordinate + st0p).rgb, W);
	mediump float h = -im1p1 - 2.0 * i0p1 - ip1p1 + im1m1 + 2.0 * i0m1 + ip1m1;
	mediump float v = -im1m1 - 2.0 * im10 - im1p1 + ip1m1 + 2.0 * ip10 + ip1p1;
	mediump float mag = 1.0 - length(vec2(h, v));
	mediump vec3 target = vec3(mag);
	return vec4(mix(textureColor, target, intensity), 1.0);
}

void main() {
   vec4 color = sketch();
   gl_FragColor = brightnessContrastSaturation(color, uBCS.r, uBCS.g, uBCS.b);
}