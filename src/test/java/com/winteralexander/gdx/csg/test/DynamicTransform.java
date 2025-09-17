package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

/**
 * Represent a mesh transformation that varies based on the position of the vertex
 * <p>
 * Created on 2025-02-09.
 *
 * @author Alexander Winter
 */
@FunctionalInterface
public interface DynamicTransform {
	void transform(Vector3 position,
	               Matrix4 transformOut,
	               Matrix4 normalTransformOut);
}
