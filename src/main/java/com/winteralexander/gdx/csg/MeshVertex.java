package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Vector3;

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
	private final Vector3 normal, binormal, tangent;
	private final float[] otherAttributes;

	public MeshVertex(int otherAttributesCount) {
		this(new Vector3(), new Vector3(), new Vector3(), new Vector3(), new float[otherAttributesCount]);
	}

	public MeshVertex(Vector3 position,
	                  Vector3 normal,
	                  Vector3 binormal,
	                  Vector3 tangent,
	                  float[] otherAttributes) {
		ensureNotNull(position, "position");
		ensureNotNull(normal, "normal");
		ensureNotNull(binormal, "binormal");
		ensureNotNull(tangent, "tangent");
		ensureNotNull(otherAttributes, "otherAttributes");
		this.position = position;
		this.normal = normal;
		this.binormal = binormal;
		this.tangent = tangent;
		this.otherAttributes = otherAttributes;
	}

	public MeshVertex(MeshVertex other) {
		this(other.position.cpy(),
				other.normal.cpy(),
				other.binormal.cpy(),
				other.tangent.cpy(),
				Arrays.copyOf(other.otherAttributes, other.otherAttributes.length));
	}

	public Vector3 getPosition() {
		return position;
	}

	public Vector3 getNormal() {
		return normal;
	}

	public Vector3 getBinormal() {
		return binormal;
	}

	public Vector3 getTangent() {
		return tangent;
	}

	public float[] getOtherAttributes() {
		return otherAttributes;
	}
}
