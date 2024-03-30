// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);
float4 p0 :  register(c0);

#define width  (p0[0])
#define height (p0[1])

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float dx = 1 / width;
	float dy = 1 / height;

	float4 c1 = tex2D(s0, tex + float2(-dx, -dy));
	float4 c2 = tex2D(s0, tex + float2(  0, -dy));
	float4 c4 = tex2D(s0, tex + float2(-dx,   0));
	float4 c6 = tex2D(s0, tex + float2( dx,   0));
	float4 c8 = tex2D(s0, tex + float2(  0,  dy));
	float4 c9 = tex2D(s0, tex + float2( dx,  dy));

	float4 c0 = (-c1 - c2 - c4 + c6 + c8 + c9);
	c0 = (c0.r + c0.g + c0.b) / 3 + 0.5;

	return c0;
}
