// $MinimumShaderProfile: ps_2_0

// Run this shader before scaling.

sampler s0 : register(s0);
float4 p0 :  register(c0);

#define height (p0[1])

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float4 c0 = tex2D(s0, tex);

	float2 h = float2(0, 1 / height);
	float4 c1 = tex2D(s0, tex - h);
	float4 c2 = tex2D(s0, tex + h);
	c0 = (c0 * 2 + c1 + c2) / 4;

	return c0;
}
