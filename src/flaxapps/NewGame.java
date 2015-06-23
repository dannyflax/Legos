package flaxapps;

import static java.awt.event.KeyEvent.VK_DOWN;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static java.awt.event.KeyEvent.VK_LEFT;
import static java.awt.event.KeyEvent.VK_RIGHT;
import static java.awt.event.KeyEvent.VK_SPACE;
import static java.awt.event.KeyEvent.VK_UP;
import static javax.media.opengl.GL.GL_COLOR_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_BUFFER_BIT;
import static javax.media.opengl.GL.GL_DEPTH_TEST;
import static javax.media.opengl.GL.GL_NICEST;
import static javax.media.opengl.GL.GL_ONE;
import static javax.media.opengl.GL.GL_SRC_ALPHA;
import static javax.media.opengl.GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_SMOOTH;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.color.ColorSpace;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.jogamp.opengl.util.FPSAnimator;

import flaxapps.jogl_util.ModelControl;
import flaxapps.jogl_util.Shader_Manager;
import flaxapps.jogl_util.Vertex;

/**
 * @author Danny Flax
 */

public class NewGame implements GLEventListener, KeyListener, java.awt.event.MouseListener{

	
	private static String TITLE = "Game Template";
	private static int CANVAS_WIDTH = 600; // width of the drawable
	private static int CANVAS_HEIGHT = 700; // height of the drawable
	private static final int FPS = 100; // animator's target frames per second
	final static JFrame frame = new JFrame();
	
	double matModelView[] = new double[16], matProjection[] = new double[16]; 
	int viewport[] = new int[4]; 
	
	public static JPanel mainPanel;
	
	ModelControl lego;
	
	int woodTexture;

	Shader_Manager sm = new Shader_Manager();

	int standardShaderNoTx;
	
	public boolean controlled = true;
	
	boolean up = false;
	boolean left = false;
	boolean right = false;
	boolean down = false;
	
	boolean U = false;
	boolean J = false;
	

	private GLU glu; // for the GL Utility
	
	// The world
	Point c_mpos;
	Point p_mpos;

	public float lookUpMax = (float) -80.0;
	public float lookUpMin = (float) 80.0;

	float[] cOffset = { 0.0f, 0.0f, 0.0f, 1.0f };

	
	
	// x and z position of the player, y is 0
	public float posX = 0.0f;
	public float posZ = 0.0f;
	public float posY = 0.0f;

	public float headingY = 0; // heading of player, about y-axis
	public float lookUpAngle = 0.0f;

	private float moveIncrement = .1f;
	// private float turnIncrement = 1.5f; // each turn in degree

	static GLCanvas canvas;

	/** The entry main() method */
	public static void main(String[] args) {
		
		// Create the OpenGL rendering canvas
		canvas = new GLCanvas(); // heavy-weight GLCanvas

		canvas.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
		NewGame renderer = new NewGame();
		canvas.addGLEventListener(renderer);

		canvas.addKeyListener(renderer);
		canvas.addMouseListener(renderer);
		
		canvas.setFocusable(true);

		canvas.requestFocus();

		// Create a animator that drives canvas' display() at the specified FPS.
		final FPSAnimator animator = new FPSAnimator(canvas, FPS, true);
		
		frame.getContentPane().add(canvas);
		
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// Use a dedicate thread to run the stop() to ensure that the
				// animator stops before program exits.
				new Thread() {
					@Override
					public void run() {
						animator.stop(); // stop the animator loop
						System.exit(0);
					}
				}.start();
			}
		});
		frame.setTitle(TITLE);
		frame.pack();

		frame.setVisible(true);
		animator.start(); // start the animation loop
	}

	// ------ Implement methods declared in GLEventListener ------

	@Override
	public void init(GLAutoDrawable drawable) {
		
		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL graphics context
		
		glu = new GLU(); // get GL Utilities
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // set background (clear) color
		gl.glClearDepth(1.0f); // set clear depth value to farthest
		gl.glEnable(GL_DEPTH_TEST); // enables depth testing
		gl.glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST); // best
															// perspective
																// correction
		gl.glShadeModel(GL_SMOOTH); // blends colors nicely, and smoothes out
									// lighting
	
		// Read the world
		try {
			standardShaderNoTx = sm.init("resources/standard_notx", gl);
		} catch (IOException e1) {
			e1.printStackTrace();
		}		
	
		// Blending control
		gl.glColor4f(1.0f, 1.0f, 1.0f, 0.5f); // Brightness with alpha
		// Blending function For translucency based On source alpha value
		gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE);
		
		
		lego = new ModelControl();
		
		try {
			lego.loadModelData("resources/Lego.obj");
		} catch (IOException ex) {
			Logger.getLogger(NewGame.class.getName()).log(
					Level.SEVERE, null, ex);
		}
		
	}

	
	@Override
	public void display(GLAutoDrawable drawable) {
				
		/**
		 * Initial control logic
		 */
		if (up) {
			posX -= (float) Math.sin(Math.toRadians(headingY)) * moveIncrement;
			posZ -= (float) Math.cos(Math.toRadians(headingY)) * moveIncrement;
		}
		if (down) {
			// Player move out, posX and posZ become bigger
			posX += (float) Math.sin(Math.toRadians(headingY)) * moveIncrement;
			posZ += (float) Math.cos(Math.toRadians(headingY)) * moveIncrement;
		}
		if (left) {
			// Player move out, posX and posZ become bigger
			posX -= (float) Math.sin(Math.toRadians(headingY + 90.0))
					* moveIncrement;
			posZ -= (float) Math.cos(Math.toRadians(headingY + 90.0))
					* moveIncrement;
		}
		if (right) {
			// Player move out, posX and posZ become bigger
			posX -= (float) Math.sin(Math.toRadians(headingY - 90.0))
					* moveIncrement;
			posZ -= (float) Math.cos(Math.toRadians(headingY - 90.0))
					* moveIncrement;
		}
		
		if (U){
			posY += moveIncrement;
		}
		
		if (J){
			posY -= moveIncrement;
		}
		
			
		
		/**
		 * Drawing code
		 */
	
		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context
		gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear color
																// and depth
																// buffers
		
		
		gl.glLoadIdentity(); // reset the model-view matrix
		gl.glEnable(GL.GL_TEXTURE_2D);
		
		/** Initial camera adjustment code **/
		
		// Rotate up and down to look up and down
		gl.glRotatef(lookUpAngle, 1.0f, 0, 0);
		// Player at headingY. Rotate the scene by -headingY instead (add 360 to
		// get a
		// positive angle)
		gl.glRotatef(360.0f - headingY, 0, 1.0f, 0);
		
		gl.glTranslatef(-posX, -posY, -posZ);
		
		gl.glGetDoublev( GL2.GL_MODELVIEW_MATRIX, matModelView, 0); 
		gl.glGetDoublev( GL2.GL_PROJECTION_MATRIX, matProjection, 0); 
		gl.glGetIntegerv( GL2.GL_VIEWPORT, viewport, 0); 
		
		gl.glUseProgram(standardShaderNoTx);

		int color = gl.glGetUniformLocation(standardShaderNoTx,"color2");
		gl.glUniform4f(color, 1.0f, 0.0f, 0.0f, 0.0f);
		
		float r = 5.0f;
		
//		lego.drawModel(new Vertex(0.0f,0.0f,r), gl, 0.0f);
//		lego.drawModel(new Vertex(0.0f,0.0f,-r), gl, 0.0f);
		lego.drawModel(new Vertex(r,0.0f,0.0f), gl, 0.0f);
//		lego.drawModel(new Vertex(-r,0.0f,0.0f), gl, 0.0f);
//		lego.drawModel(new Vertex(0.0f,-r,0.0f), gl, 0.0f);
//		lego.drawModel(new Vertex(0.0f,r,0.0f), gl, 0.0f);

		
		/** Cleanup code **/

		gl.glFlush();
		gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA,
				GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_COLOR);
		
	}

	/**
	 * Call-back handler for window re-size event. Also called when the drawable
	 * is first set to visible.
	 */
	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width,
			int height) {
		
		GL2 gl = drawable.getGL().getGL2(); // get the OpenGL 2 graphics context
		
		if (height == 0)
			height = 1; // prevent divide by zero
		float aspect = (float) width / height;
	
		// Set the view port (display area) to cover the entire window
		gl.glViewport(0, 0, width, height);
	
		// Setup perspective projection, with aspect ratio matches viewport
		gl.glMatrixMode(GL_PROJECTION); // choose projection matrix
		gl.glLoadIdentity(); // reset projection matrix
		glu.gluPerspective(45.0, aspect, 0.1, 100.0); // fovy, aspect, zNear,
														// zFar
	
		// Enable the model-view transform
		gl.glMatrixMode(GL_MODELVIEW);
		gl.glLoadIdentity(); // reset
		
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
	}

	class im {
		public ByteBuffer b;
		public int wi;
		public int he;
	}

	/**
	 * Called back before the OpenGL context is destroyed. Release resource such
	 * as buffers.
	 */
	
	public im makeImg(String txt) {
		BufferedImage bufferedImage = null;
		int w = 0;
		int h = 0;
		
		try {
			FileInputStream fStream = new FileInputStream(new File(txt));
			bufferedImage = ImageIO.read(fStream);
			w = bufferedImage.getWidth();
			h = bufferedImage.getHeight();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		WritableRaster raster = Raster.createInterleavedRaster(
				DataBuffer.TYPE_BYTE, w, h, 4, null);
		ComponentColorModel colorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8,
						8, 8 }, true, false, ComponentColorModel.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);
		BufferedImage dukeImg = new BufferedImage(colorModel, raster, false,
				null);
		
		Graphics2D g = dukeImg.createGraphics();
		g.drawImage(bufferedImage, null, null);
		DataBufferByte dukeBuf = (DataBufferByte) raster.getDataBuffer();
		byte[] dukeRGBA = dukeBuf.getData();
		ByteBuffer bb = ByteBuffer.wrap(dukeRGBA);
		bb.position(0);
		bb.mark();
		im i = new im();
		i.b = bb;
		i.he = h;
		i.wi = w;
		return i;
	}

	
	
	int uniqueID = 4;
	
	/**
	 * 
	 * @param gl
	 * 		The GL context on which we setup the texture
	 * @param txt
	 * 		The name of the image file from which to build the texture
	 * @return
	 * 		The integer ID pointing to the texture
	 * @ensures
	 * 		Image at location @{code txt} becomes a texture loaded in context @{code gl}
	 * 		with a unique ID
	 */
	public int setUp2DText(GL2 gl, String txt) {
		im mud = this.makeImg(txt);
	
		gl.glBindTexture(GL.GL_TEXTURE_2D, uniqueID);
		
		gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL.GL_RGBA, mud.wi, mud.he, 0,
				GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, mud.b);
	
		// Use nearer filter if image is larger than the original texture
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER,
				GL.GL_NEAREST);
		
		// Use nearer filter if image is smaller than the original texture
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER,
				GL.GL_NEAREST);
		
		// For texture coordinates more than 1, set to wrap mode to GL_REPEAT
		// for
		// both S and T axes (default setting is GL_CLAMP)
		
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_LINEAR);
		
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_LINEAR);
		
		uniqueID++;
		
		return uniqueID-1;
	}

	/**
	 * Called back immediately after the OpenGL context is initialized. Can be
	 * used to perform one-time initialization. Run only once.
	 */

	public static BufferedImage componentToImage(Component component,
			Rectangle region) throws IOException {
		BufferedImage img = new BufferedImage(component.getWidth(),
				component.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
		Graphics2D g = (Graphics2D) img.getGraphics();
		g.setColor(component.getForeground());
		g.setFont(component.getFont());

		component.paintAll(g);
		if (region == null) {
			region = new Rectangle(0, 0, img.getWidth(), img.getHeight());
		}
		return img.getSubimage(region.x, region.y, region.width, region.height);
	}
	

	// ----- Implement methods declared in KeyListener -----

	@Override
	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		case VK_SPACE:
			System.out.println("X: "+posX+", Y: "+posY+", Z: "+posZ+", HeadingY: " + headingY + ", Look up angle: " + lookUpAngle);
			break;
		case VK_LEFT:
			headingY+=5;
			break;
		case VK_RIGHT:
			headingY-=5;
			break;
		case VK_DOWN:
			lookUpAngle+=5;
			break;
		case VK_UP:
			lookUpAngle-=5;
			break;
		case VK_ESCAPE:
			frame.dispose();
			System.exit(0);	
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		switch (e.getKeyChar()) {

		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		double[] startPos = new double[3];
		double[] endPos = new double[3];
		
		int x = arg0.getX();
		int y = arg0.getY();
		
		double winX = (double)x; 
		double winY = viewport[3] - (double)y; 
		
		GLU glu = new GLU();

		
		
		glu.gluUnProject(winX, winY, 0.0, matModelView, 0, matProjection, 0, viewport, 0, startPos, 0); 
		
		glu.gluUnProject(winX, winY, 100.0, matModelView, 0, matProjection, 0, viewport, 0, endPos, 0); 
			
		Vertex start = new Vertex((float)startPos[0],(float)startPos[1],(float)startPos[2]);
		Vertex end = new Vertex((float)endPos[0],(float)endPos[1],(float)endPos[2]);
		
		Vertex goal = new Vertex(5.0f,0.0f,0.0f);
		
		Vertex v1 = Vertex.add(goal, Vertex.scalar(start, -1.0f));
		Vertex v2 = Vertex.add(end, Vertex.scalar(start, -1.0f));
		double dot1 = Vertex.dot(v1, v2);
		double dot2 = Vertex.dot(v1, v1);
		double proj = dot2/dot1;
		
		Vertex closest = Vertex.add(start, Vertex.scalar(v2, (float)proj));
		
		System.out.println("Distance: "+Vertex.distance(closest, goal));
	}


	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	

	@Override
	public void mousePressed(MouseEvent arg0) {}

	@Override
	public void mouseReleased(MouseEvent arg0) {}



}




