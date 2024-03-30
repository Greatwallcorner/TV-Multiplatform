// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);
float4 p0 :  register(c0);

#define width  (p0[0])
#define height (p0[1])

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float4 c0 = 0;

	float2 ar = float2(16, 9);
	float h = (1 - width / height * ar.y / ar.x) / 2;

	if (tex.y >= h && tex.y <= 1 - h) {
		c0 = tex2D(s0, tex);
	}

	return c0;
}
