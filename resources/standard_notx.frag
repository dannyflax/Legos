varying vec3 N;
varying float NdotL;
uniform vec4 color2;
void main()
{
    vec4 color = vec4(1.0,0.0,0.0,1.0);
    gl_FragColor = color2*max(0.4,NdotL);
}
