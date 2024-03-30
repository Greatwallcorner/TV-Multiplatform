// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);
float4 p0 :  register(c0);

#define width  (p0[0])
#define height (p0[1])
#define clock  (p0[3])

#define PI acos(-1)

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float4 c0 = tex2D(s0, tex);
	float3 lightsrc = float3(sin(clock * PI / 1.5) / 2 + 0.5, cos(clock * PI) / 2 + 0.5, 1);
	float3 light = normalize(lightsrc - float3(tex.x, tex.y, 0));
	c0 *= pow(dot(light, float3(0, 0, 1)), 50);

	return c0;
}
