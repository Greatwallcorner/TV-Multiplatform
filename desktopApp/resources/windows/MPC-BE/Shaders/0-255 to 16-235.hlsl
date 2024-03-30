// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);

#define const_1 ( 16.0 / 255.0)
#define const_2 (219.0 / 255.0)

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	// original pixel
	float4 c0 = tex2D(s0, tex);

	return (c0 * const_2) + const_1;
}
