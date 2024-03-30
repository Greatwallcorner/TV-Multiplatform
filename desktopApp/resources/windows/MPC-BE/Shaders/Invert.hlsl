// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float4 c0 = float4(1, 1, 1, 1) - tex2D(s0, tex);

	return c0;
}
