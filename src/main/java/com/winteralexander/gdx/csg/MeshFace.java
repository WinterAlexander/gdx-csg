package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Vector3;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * A face in a {@link CSGMesh}, which is located in RAM instead of GPU memory for the sake of
 * performing CSG operations
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class MeshFace {
	private final MeshVertex v1, v2, v3;

	private final Triangle tmpTriangle = new Triangle();

	public MeshFace(MeshVertex v1, MeshVertex v2, MeshVertex v3) {
		ensureNotNull(v1, "v1");
		ensureNotNull(v2, "v2");
		ensureNotNull(v3, "v3");
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
	}

	public Vector3 getNormal() {
		return getTriangle().getNormal();
	}

	public Vector3 getPosition1() {
		return v1.getPosition();
	}

	public Vector3 getPosition2() {
		return v2.getPosition();
	}

	public Vector3 getPosition3() {
		return v3.getPosition();
	}

	public MeshVertex getV1() {
		return v1;
	}

	public MeshVertex getV2() {
		return v2;
	}

	public MeshVertex getV3() {
		return v3;
	}

	public Triangle getTriangle() {
		tmpTriangle.set(v1.getPosition(), v2.getPosition(), v3.getPosition());
		return tmpTriangle;
	}
}
