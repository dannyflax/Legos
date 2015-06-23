varying vec3 N;
varying float NdotL;

void main()
{
    gl_TexCoord[0] = gl_MultiTexCoord0;
    
	vec3 lightDir;
	
    float angle = 0.0;
    
    vec3 normal = gl_Normal;
    
    normal.x = (normal.x*cos(angle) + normal.y*-1.0*sin(angle));
    normal.y = (normal.x*sin(angle) + normal.y*cos(angle));
    
	N = normalize(normal);
	lightDir = vec3(.5, .59, .25);
 
    lightDir = normalize(lightDir);
    
    NdotL = dot(N,lightDir);
        
	vec3 p = gl_Vertex.xyz;
	
    p.x = (p.x*cos(angle) + p.y*-sin(angle));
    p.y = (p.x*sin(angle) + p.y*cos(angle));
    
	gl_Position = gl_ModelViewProjectionMatrix * vec4(p,1.0);
}