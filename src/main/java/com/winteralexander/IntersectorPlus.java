package com.winteralexander;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

/**
 * Undocumented :(
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class IntersectorPlus {
	public static boolean rayRayIntersection(Ray first, Ray second, float tol, Vector3 out) {
		//x = x1 + a1*t = x2 + b1*s
		//y = y1 + a2*t = y2 + b2*s
		//z = z1 + a3*t = z2 + b3*s

		Vector3 d1 = first.direction;
		Vector3 d2 = second.direction;
		Vector3 o1 = first.origin;
		Vector3 o2 = second.origin;

		float t;
		if(Math.abs(d1.y * d2.x - d1.x * d2.y) > tol) {
			t = (-o1.y * d2.x + o2.y * d2.x + d2.y * o1.x - d2.y * o2.x)
					/ (d1.y * d2.x - d1.x * d2.y);
		} else if(Math.abs(-d1.x * d2.z + d1.z * d2.x) > tol) {
			t = -(-d2.z * o1.x + d2.z * o2.x + d2.x * o1.z - d2.x * o2.z)
					/ (-d1.x * d2.z + d1.z * d2.x);
		} else if(Math.abs(-d1.z * d2.y + d1.y * d2.z) > tol) {
			t = (o1.z * d2.y - o2.z * d2.y - d2.z * o1.y + d2.z * o2.y)
					/ (-d1.z * d2.y + d1.y * d2.z);
		} else
			return false;

		float x = o1.x + d1.x * t;
		float y = o1.y + d1.y * t;
		float z = o1.z + d1.z * t;

		out.set(x, y, z);
		return true;
	}
}
