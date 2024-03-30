// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);
float4 p0 :  register(c0);
float4 p1 :  register(c1);

#define width  (p0[0])
#define height (p0[1])

#define val0 (2.0)
#define val1 (-0.125)
#define effect_width (1.6)

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float dx = effect_width / width;
	float dy = effect_width / height;

	float4 c1 = tex2D(s0, tex + float2(-dx, -dy)) * val1;
	float4 c2 = tex2D(s0, tex + float2(  0, -dy)) * val1;
	float4 c3 = tex2D(s0, tex + float2(-dx,   0)) * val1;
	float4 c4 = tex2D(s0, tex + float2( dx,   0)) * val1;
	float4 c5 = tex2D(s0, tex + float2(  0,  dy)) * val1;
	float4 c6 = tex2D(s0, tex + float2( dx,  dy)) * val1;
	float4 c7 = tex2D(s0, tex + float2(-dx, +dy)) * val1;
	float4 c8 = tex2D(s0, tex + float2(+dx, -dy)) * val1;
	float4 c9 = tex2D(s0, tex) * val0;

	float4 c0 = (c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9);

	return c0;
}
