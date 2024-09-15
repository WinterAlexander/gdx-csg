package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Vector3;
import com.winteralexander.gdx.utils.math.MathUtil;

/**
 * Extension to {@link MathUtil}
 * <p>
 * Created on 2024-09-15.
 *
 * @author Alexander Winter
 */
public class MathUtilPlus {
	private static final Vector3 tmpVec = new Vector3();

	public static boolean isBetween(Vector3 first, Vector3 second, Vector3 between) {
		Vector3 middle = tmpVec.set(first).nor().mulAdd(second, 1f / second.len()).nor();
		float d = middle.dot(first);
		float d2 = middle.dot(between) / between.len();
		if(d2 < 0f)
			return false;
		return d2 > d;
	}
}
