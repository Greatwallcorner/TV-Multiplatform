// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float c = dot(tex2D(s0, tex), float4(0.2, 0.6, 0.1, 0.1));
	return float4(0, c, 0, 0);
}
