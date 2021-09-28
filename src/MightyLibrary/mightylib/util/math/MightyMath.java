package MightyLibrary.mightylib.util.math;

import java.util.ArrayList;

/**
 * Math class.
 * This class is used to compute complicated calculations.
 *
 * @author MightyCode
 * @version 1.0
 */
public class MightyMath {

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
	public static float map(float x, float a, float b, float c, float d) {
		return (x - a) / (b - a) * (d - c) + c;
	}

	public static int sum(int[] table){
		int i = 0;
		int sum = 0;
		while (i < table.length){
			sum+=table[i];
			++i;;
		}
		return sum;
	}

	public static float sum(float[] sum){
		return sum(sum, 0, sum.length);
	}

	public static float sum(float[] sum, int start, int end){
		float result = 0;
		for (int  i = start; i < end; ++i){
			result += sum[i];
		}

		return result;
	}



	public static Float sum(ArrayList<Float> list){
		float sum = 0;
		for (Float value : list){
			sum += value;
		}

		return sum;
	}

	public static double rads(double angle){
		return angle * (java.lang.Math.PI/180.0);
	}

	public static float rads(float angle){
		return (float)(angle * (java.lang.Math.PI/180.0));
	}

	public static double angles(double rad){
		return rad * (180.0/java.lang.Math.PI);
	}

	public static float angles(float rad){
		return (float)(rad * (180.0/java.lang.Math.PI));
	}
}