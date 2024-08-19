package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.lang.reflect.Array;
import java.util.Arrays;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * A vertex in a {@link CSGMesh}
 * <p>
 * Created on 2024-08-11.
 *
 * @author Alexander Winter
 */
public class MeshVertex {
	private final Vector3 position;
	private final Vector3 normal;
	private final float[] otherAttributes;

	public MeshVertex(int otherAttributesCount) {
		this(new Vector3(), new Vector3(), new float[otherAttributesCount]);
	}

	public MeshVertex(Vector3 position,
	                  Vector3 normal,
	                  float[] otherAttributes) {
		ensureNotNull(position, "position");
		ensureNotNull(normal, "normal");
		ensureNotNull(otherAttributes, "otherAttributes");
		this.position = position;
		this.normal = normal;
		this.otherAttributes = otherAttributes;
	}

	public MeshVertex(MeshVertex other) {
		this(other.position.cpy(),
				other.normal.cpy(),
				Arrays.copyOf(other.otherAttributes, other.otherAttributes.length));
	}

	public Vector3 getPosition() {
		return position;
	}

	public Vector3 getNormal() {
		return normal;
	}

	public float[] getOtherAttributes() {
		return otherAttributes;
	}
}
