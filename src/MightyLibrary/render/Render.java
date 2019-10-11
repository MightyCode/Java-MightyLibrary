package MightyLibrary.render;

import static org.lwjgl.opengl.GL11.*;

/**
 * Class with utility.
 * @author MightyCode
 * @version 1.1
 */
public abstract class Render {

	/**
	 * Set the 2D view.
	 */
	public static void glEnable2D() {
		int[] vPort = new int[4];

		glGetIntegerv(GL_VIEWPORT, vPort);

		glMatrixMode(GL_PROJECTION);
		glPushMatrix();
		glLoadIdentity();

		glOrtho(0, vPort[2], vPort[3], 0, -1, 1);
		glMatrixMode(GL_MODELVIEW);
		glPushMatrix();
		glLoadIdentity();
	}

	/**
	 * Clear the screen with white color.
	 */
	public static void clear(){
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * Set the clear color.
	 */
	public static void setClearColor(float color1, float color2, float color3, float alpha){
		glClearColor(color1, color2, color3, alpha);
	}
	public static void setClearColor(int color1, int color2, int color3, float alpha){
		glClearColor((float)color1/255, (float)color2/255, (float)color3/255, alpha);
	}

	/**
	 * Surcharge method to set color without alpha.
	 */
	public static void setClearColor(float color1, float color2, float color3){ glClearColor(color1, color2, color3, 1f);}


	/**
	 * Surcharge method to set the clear color.
	 */
	public static void setClearColor(float color, float alpha){ glClearColor(color, color, color, alpha);}

	/**
	 * Surcharge method to set the clear color without alpha.
	 */
	public static void setClearColor(float color){ glClearColor(color, color, color, 1f);}

	/**
	 * Set the 3D view.
	 */
	public static void glDisable2D() {
		glMatrixMode(GL_PROJECTION);
		glPopMatrix();
		glMatrixMode(GL_MODELVIEW);
		glPopMatrix();
	}

	public static void setViewPort(int width, int height){
		glViewport(0, 0, width, height);
	}
}
