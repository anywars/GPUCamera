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

vec4 billboard() {
	float offx = floor(vTextureCoordinate.s / (grid * step_w));
	float offy = floor(vTextureCoordinate.t / (grid * step_h));
	vec3 res = texture2D(uTexture, vec2(offx * grid * step_w , offy * grid * step_h)).rgb;
	vec2 prc = fract(vTextureCoordinate.st / vec2(grid * step_w, grid * step_h));
	vec2 pw = pow(abs(prc - 0.5), vec2(2.0));
	float  rs = pow(0.45, 2.0);
	float gr = smoothstep(rs - 0.1, rs + 0.1, pw.x + pw.y);
	float y = (res.r + res.g + res.b) / 3.0;
	vec3 ra = res / y;
	float ls = 0.3;
	float lb = ceil(y / ls);
	float lf = ls * lb + 0.3;
	res = lf * res;
	return vec4(mix(res, vec3(0.1, 0.1, 0.1), gr), 1.0);
}

void main() {
    vec4 color = billboard();
   gl_FragColor = brightnessContrastSaturation(color, uBCS.r, uBCS.g, uBCS.b);
}