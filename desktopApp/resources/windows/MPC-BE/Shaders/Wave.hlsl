// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);
float4 p0 :  register(c0);

#define width  (p0[0])
#define height (p0[1])
#define clock  (p0[3])

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float4 c0 = 0;

	tex.x += sin(tex.x + clock / 0.3) / 20;
	tex.y += sin(tex.x + clock / 0.3) / 20;

	if (tex.x >= 0 && tex.x <= 1 && tex.y >= 0 && tex.y <= 1) {
		c0 = tex2D(s0, tex);
	}

	return c0;
}
