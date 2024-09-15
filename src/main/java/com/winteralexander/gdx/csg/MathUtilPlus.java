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
	public static boolean isBetween(Vector3 first, Vector3 second, Vector3 between) {
		first = first.cpy().nor();
		second = second.cpy().nor();
		Vector3 middle = first.cpy().add(second).nor();
		float d = middle.dot(first);
		float d2 = middle.dot(between.cpy().nor());
		if(d2 < 0f)
			return false;
		return d2 > d;
	}
}
