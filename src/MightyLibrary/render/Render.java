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
