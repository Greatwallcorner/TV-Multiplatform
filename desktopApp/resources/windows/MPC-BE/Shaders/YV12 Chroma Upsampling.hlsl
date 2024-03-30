// $MinimumShaderProfile: ps_2_0

/*
YV12 chroma upsampling fixer
by Kurt Bernhard 'Leak' Pruenner

Use with YV12 output if the half-resolution chroma
gets upsampled in hardware by doubling the values
instead of interpolating between them.

(i.e. if you're getting blocky red edges on dark
backgrounds...)
*/

sampler s0 : register(s0);
float4 p0 :  register(c0);
float4 p1 :  register(c1);

#define width  (p0[0])
#define height (p0[1])

float4 getPixel(float2 tex, float dx, float dy)
{
	tex.x += dx;
	tex.y += dy;

	return tex2D(s0, tex);
}

float4 rgb2yuv(float4 rgb)
{
	float4x4 coeffs = {
		 0.299,  0.587,  0.114, 0.000,
		-0.147, -0.289,  0.436, 0.000,
		 0.615, -0.515, -0.100, 0.000,
		 0.000,  0.000,  0.000, 0.000
	};

	return mul(coeffs, rgb);
}

float4 yuv2rgb(float4 yuv)
{
	float4x4 coeffs = {
		1.000,  0.000,  1.140, 0.000,
		1.000, -0.395, -0.581, 0.000,
		1.000,  2.032,  0.000, 0.000,
		0.000,  0.000,  0.000, 0.000
	};

	return mul(coeffs, yuv);
}

float4 main(float2 tex : TEXCOORD0) : COLOR {
	float dx = 1 / width;
	float dy = 1 / height;

	float4 yuv00 = rgb2yuv(getPixel(tex, -dx, -dy));
	float4 yuv01 = rgb2yuv(getPixel(tex, -dx,   0));
	float4 yuv02 = rgb2yuv(getPixel(tex, -dx,  dy));
	float4 yuv10 = rgb2yuv(getPixel(tex,   0, -dy));
	float4 yuv11 = rgb2yuv(getPixel(tex,   0,   0));
	float4 yuv12 = rgb2yuv(getPixel(tex,   0,  dy));
	float4 yuv20 = rgb2yuv(getPixel(tex,  dx, -dy));
	float4 yuv21 = rgb2yuv(getPixel(tex,  dx,   0));
	float4 yuv22 = rgb2yuv(getPixel(tex,  dx,  dy));

	float4 yuv =
		(yuv00 * 1 + yuv01 * 2 + yuv02 * 1 +
		 yuv10 * 2 + yuv11 * 4 + yuv12 * 2 +
		 yuv20 * 1 + yuv21 * 2 + yuv22 * 1) / 16;

	yuv.r = yuv11.r;

	return yuv2rgb(yuv);
}
