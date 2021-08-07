#extension GL_OES_EGL_image_external : require
#ifdef GL_ES
precision mediump float;
#endif

uniform mediump int uHue;
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

vec4 hueSeeker(vec3 rgb) {
	float h, s, l;
	float maxval = max(rgb.r, max(rgb.g, rgb.b));
	float minval = min(rgb.r, min(rgb.g, rgb.b));
	float delta = maxval - minval;
	l = (minval + maxval) / 2.0;
	s = 0.0;
	if (l > 0.0 && l < 1.0)
		s = delta / (l < 0.5 ? 2.0 * l : 2.0 - 2.0 * l);
	h = 0.0;
	if (delta > 0.0) {
		if (rgb.r == maxval && rgb.g != maxval)
			h += (rgb.g - rgb.b) / delta;
		if (rgb.g == maxval && rgb.b != maxval)
			h += 2.0 + (rgb.b - rgb.r) / delta;
		if (rgb.b == maxval && rgb.r != maxval)
			h += 4.0 + (rgb.r - rgb.g) / delta;
		h *= 60.0;
	}
	float y = 0.3 * rgb.r + 0.59 * rgb.g + 0.11 * rgb.b;
	vec3 result;

    bool isSeeker = false;
    if (uHue == 1) { // red //
        isSeeker = h <= 10.0 || h >= 337.0;
    }
    else if (uHue == 2) { // blue //
        isSeeker = h >= 180.0 && h <= 250.0;
    }
    else if (uHue == 3) { // yellow //
        isSeeker = h >= 42.0 && h <= 69.0;
    }
    else if (uHue == 4) { // green //
        isSeeker = h >= 70.0 && h <= 164.0;
    }
    else {
        isSeeker = false;
    }

	if (isSeeker)
		result = rgb;
	else
		result = vec3(y, y, y);
	return vec4(result, 1.0);
}

void main() {
   vec4 color = hueSeeker(texture2D(uTexture, vTextureCoordinate).rgb);
   gl_FragColor = brightnessContrastSaturation(color, uBCS.r, uBCS.g, uBCS.b);
}