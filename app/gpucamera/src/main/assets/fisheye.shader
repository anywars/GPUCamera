#extension GL_OES_EGL_image_external : require
#ifdef GL_ES
precision mediump float;
#endif
const float PI = 3.1415926535;


uniform vec3 uBCS;
uniform samplerExternalOES uTexture;
varying vec2 vTextureCoordinate;

const float aperture = 180.0;
vec4 fisheye() {
	float apertureHalf = 0.5 * aperture * (PI / 180.0);
	float maxFactor = sin(apertureHalf);
	vec2 pos = 2.0 * vTextureCoordinate.st - 1.0;

	float len = length(pos);
	if (len > 1.0) {
		return vec4(0.0, 0.0, 0.0, 1.0);
	} else {
		float x = maxFactor * pos.x;
		float y = maxFactor * pos.y;
		float n = length(vec2(x, y));
		float z = sqrt(1.0 - n * n);
		float r = atan(n, z) / PI;
		float phi = atan(y, x);
		float u = r * cos(phi) + 0.5;
		float v = r * sin(phi) + 0.5;
		return texture2D(uTexture, vec2(u, v));
	}
}

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
    vec4 color = fisheye();
   gl_FragColor = brightnessContrastSaturation(color, uBCS.r, uBCS.g, uBCS.b);
}