// $MinimumShaderProfile: ps_3_0

sampler s0 : register(s0);
float4 p0 :  register(c0);

#define width  (p0[0])
#define height (p0[1])
#define val0 (1.0)
#define val1 (0.125)
#define effect_width (0.1)

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float dx = 0.0f;
	float dy = 0.0f;
	float fTap = effect_width;

	float4 cAccum = tex2D(s0, tex) * val0;

	for (int iDx = 0; iDx < 16; ++iDx) {
		dx = fTap / width;
		dy = fTap / height;

		cAccum += tex2D(s0, tex + float2(-dx, -dy)) * val1;
		cAccum += tex2D(s0, tex + float2(  0, -dy)) * val1;
		cAccum += tex2D(s0, tex + float2(-dx,   0)) * val1;
		cAccum += tex2D(s0, tex + float2( dx,   0)) * val1;
		cAccum += tex2D(s0, tex + float2(  0,  dy)) * val1;
		cAccum += tex2D(s0, tex + float2( dx,  dy)) * val1;
		cAccum += tex2D(s0, tex + float2(-dx, +dy)) * val1;
		cAccum += tex2D(s0, tex + float2(+dx, -dy)) * val1;

		fTap  += 0.1f;
	}

	return (cAccum / 16.0f);
}
