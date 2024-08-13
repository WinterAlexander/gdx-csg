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
	private final MeshVertex[] vertices = new MeshVertex[3];
	private final Triangle tmpTriangle = new Triangle();

	public MeshFace(MeshVertex v1, MeshVertex v2, MeshVertex v3) {
		ensureNotNull(v1, "v1");
		ensureNotNull(v2, "v2");
		ensureNotNull(v3, "v3");
		this.vertices[0] = v1;
		this.vertices[1] = v2;
		this.vertices[2] = v3;
	}

	public Vector3 getNormal() {
		return getTriangle().getNormal();
	}

	public Vector3 getPosition1() {
		return vertices[0].getPosition();
	}

	public Vector3 getPosition2() {
		return vertices[1].getPosition();
	}

	public Vector3 getPosition3() {
		return vertices[2].getPosition();
	}

	public MeshVertex getV1() {
		return vertices[0];
	}

	public MeshVertex getV2() {
		return vertices[1];
	}

	public MeshVertex getV3() {
		return vertices[2];
	}

	public Triangle getTriangle() {
		tmpTriangle.set(getPosition1(), getPosition2(), getPosition3());
		return tmpTriangle;
	}

	public MeshVertex[] getVertices() {
		return vertices;
	}
}
