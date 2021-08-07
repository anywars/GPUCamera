#extension GL_OES_EGL_image_external : require
#ifdef GL_ES
precision mediump float;
#endif

const float step_w = 0.0015625;
const float step_h = 0.0027778;
const float grid = 8.0;

uniform vec3 uBCS;
uniform samplerExternalOES uTexture;
varying vec2 vTextureCoordinate;

const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);
const float intensity = 1.0;

uniform mediump float uWidth;
uniform mediump float uHeight;

const float QUANTIZE = 7.0;
const float THRESH = 0.90;

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

vec4 toon() {
	mediump vec3 textureColor = texture2D(uTexture, vTextureCoordinate).rgb;
	mediump vec2 stp0 = vec2(1.0 / uWidth,	 0.0);
	mediump vec2 st0p = vec2(0.0, 1.0 / uHeight);
	mediump vec2 stpp = vec2(1.0 / uWidth, 1.0 / uHeight);
	mediump vec2 stpm = vec2(1.0 / uWidth, -1.0 / uHeight);
	mediump float i00   = dot(textureColor, W);
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
	if (mag < THRESH) {
		return vec4(0.0, 0.0, 0.0, 1.0);
	} else {
		vec3 tmpColor = texture2D(uTexture, vTextureCoordinate).rgb;
		tmpColor *= QUANTIZE;
		tmpColor += vec3(0.5);
		tmpColor = vec3(ivec3(tmpColor)) / QUANTIZE;
		return vec4(tmpColor, 1.0);
	}
}

void main() {
   vec4 color = toon();
   gl_FragColor = brightnessContrastSaturation(color, uBCS.r, uBCS.g, uBCS.b);
}