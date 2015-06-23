varying vec3 N;
varying float NdotL;
uniform vec4 color2;
uniform sampler2D mainTexture;
void main()
{
    vec4 color = texture2D(mainTexture,gl_TexCoord[0].st);
    gl_FragColor = color*max(0.4,NdotL);
}