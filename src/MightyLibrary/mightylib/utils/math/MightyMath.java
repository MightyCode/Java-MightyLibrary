package MightyLibrary.mightylib.utils.math;

import MightyLibrary.mightylib.utils.math.geometry.EDirection3D;
import org.joml.*;

import java.lang.Math;
import java.util.ArrayList;

/**
 * Math class.
 * This class is used to compute complicated calculations.
 *
 * @author MightyCode
 * @version 1.0
 */
public abstract class MightyMath {
	private MightyMath() {}

	public static Vector3f LeftVector() { return new Vector3f(0, 0, -1); }
	public static Vector3f RightVector() { return new Vector3f(0, 0, 1); }

	public static Vector3f UpVector() { return new Vector3f(0, 1, 0); }
	public static Vector3f DownVector() { return new Vector3f(0, -1, 0); }

	public static Vector3f ForwardVector() { return new Vector3f(1, 0, 0); }
	public static Vector3f BackwardVector() { return new Vector3f(-1, 0, 0); }

	public static Vector3f OnesVector() { return new Vector3f(1); }
	public static Vector3f ZerosVector () { return new Vector3f(0); }

	public static float PI_FLOAT = (float)(Math.PI);

	/**
	 * Calculate the position of a number in the first interval to transpose this number in an second interval.
	 *
	 * @param x The number in the first interval.
	 * @param a First number of the first interval.
	 * @param b Latest number of the first interval.
	 * @param c First number of the second interval..
	 * @param d Latest number of the second interval.
	 *
	 * @return result
	*/
	public static float Mapf(float x, float a, float b, float c, float d) {
		return (x - a) / (b - a) * (d - c) + c;
	}

	public static double Mapd(double x, double a, double b, double c, double d) {
		return (x - a) / (b - a) * (d - c) + c;
	}


	public static float MapLogf(float x, float a, float b, float c, float d) {
		return (float)Math.exp(Mapf((float)Math.log(x), (float)Math.log(a), (float)Math.log(b), (float)Math.log(c), (float)Math.log(d)));
	}

	public static int Sum(int[] table){
		int i = 0;
		int sum = 0;
		while (i < table.length){
			sum+=table[i];
			++i;
		}
		return sum;
	}

	public static float Sum(float[] sum){
		return Sum(sum, 0, sum.length);
	}

	public static float Sum(float[] sum, int start, int end){
		float result = 0;
		for (int  i = start; i < end; ++i){
			result += sum[i];
		}

		return result;
	}



	public static Float Sum(ArrayList<Float> list){
		float sum = 0;
		for (Float value : list){
			sum += value;
		}

		return sum;
	}

	public static double toRads(double angle){
		return angle * (Math.PI/180.0);
	}

	public static float toRads(float angle){
		return (float)(angle * (Math.PI/180.0));
	}

	public static double toDegrees(double rad){
		return rad * (180.0/ Math.PI);
	}

	public static float toDegrees(float rad){
		return (float)(rad * (180.0/ Math.PI));
	}

	public static Vector2f RotatePointAround(Vector2f pt, Vector2f ref, float angle){
		double s = Math.cos(angle);
		double c = Math.sin(angle);

		return new Vector2f(
				(float)(c * (pt.x - ref.x) - s * (pt.y - ref.y) + ref.x),
				(float)(s * (pt.x - ref.x) + c * (pt.y - ref.y)  + ref.y)
		);
	}

	public static Vector2f ProjectPointOnAxis(Vector2f pt, Vector2f axis){
		float value = (pt.x * axis.x + pt.y * axis.y) / (axis.x * axis.x + axis.y * axis.y);

		return new Vector2f(axis.x * value, axis.y * value);

	}

	public static Vector3f ToVector(EDirection3D direction3D){
		switch (direction3D){
			case Forward:
				return ForwardVector();
			case Backward:
				return BackwardVector();
			case Left:
				return LeftVector();
			case Right:
				return RightVector();
			case Up:
				return UpVector();
			case Down:
				return DownVector();
			case None:
			default:
				return ZerosVector();
		}
	}

	/**
	 *
	 *
	 * @param point x, y as value
	 * @param line x1, y1, x2, y2 as value
	 * @param precision the precision of the calculation
	 * @return true if the point is over the line
	 */
	public static boolean isPositionOverridingLine(Vector2f point, Vector4f line, float precision){
		// ax + b -> line;
		float a = (line.y - line.w) / (line.x - line.z);
		float b = line.y - a * line.x;

		float xInf = Math.min(line.x, line.z);
		float xSup = Math.max(line.x, line.z);
		float yInf = Math.min(line.y, line.w);
		float ySup = Math.max(line.y, line.w);

		return a * point.x + b - precision <= point.y &&
				point.y <= a * point.x + b + precision &&
				xInf - precision <= point.x &&
				point.x <= xSup + precision &&
				yInf - precision <= point.y &&
				point.y <= ySup + precision;
	}
}