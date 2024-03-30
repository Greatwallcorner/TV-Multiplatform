// $MinimumShaderProfile: ps_2_0

sampler s0 : register(s0);
float4 p0 :  register(c0);

#define width  (p0[0])
#define height (p0[1])

float4 main(float2 tex : TEXCOORD0) : COLOR
{
	float dx = 4 / width;
	float dy = 4 / height;

	float4 c2 = tex2D(s0, tex + float2(  0, -dy));
	float4 c4 = tex2D(s0, tex + float2(-dx,   0));
	float4 c5 = tex2D(s0, tex + float2(  0,   0));
	float4 c6 = tex2D(s0, tex + float2( dx,   0));
	float4 c8 = tex2D(s0, tex + float2(  0,  dy));

	float4 c0 = (-c2 - c4 + c5 * 4 - c6 - c8);
	if (length(c0) < 1.0) {
		c0 = float4(0, 0, 0, 0);
	} else {
		c0 = float4(1, 1, 1, 0);
	}

	return c0;
}
