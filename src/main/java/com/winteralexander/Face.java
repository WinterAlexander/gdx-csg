package com.winteralexander;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.IndexData;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.VertexData;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Queue;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

import static com.badlogic.gdx.graphics.VertexAttributes.Usage.Position;
import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * TODO Undocumented :(
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class Face {
	public static final Queue<Consumer<ShapeRenderer>> __debugOnlyRenderables = new Queue<>();

	public final Mesh mesh;

	public int v1, v2, v3;

	public Color color = Color.WHITE;

	private final Vector3 tmpPos1 = new Vector3(),
			tmpPos2 = new Vector3(),
			tmpPos3 = new Vector3();

	private final Vector3 tmpNormal = new Vector3();

	public Face(Mesh mesh, int triangleIndex) {
		ensureNotNull(mesh, "mesh");
		this.mesh = mesh;
		ShortBuffer buffer = mesh.getIndicesBuffer(false);
		v1 = buffer.get(triangleIndex * 3);
		v2 = buffer.get(triangleIndex * 3 + 1);
		v3 = buffer.get(triangleIndex * 3 + 2);


		__debugOnlyRenderables.addFirst(renderer -> {
			renderer.setColor(color);
			renderer.line(getPosition1(), getPosition2());
			renderer.line(getPosition2(), getPosition3());
			renderer.line(getPosition3(), getPosition1());
		});
	}

	public Vector3 getNormal() {
		Vector3 p1 = getPosition1();
		Vector3 p2 = getPosition2();
		Vector3 p3 = getPosition3();

		tmpNormal.set(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
		tmpNormal.crs(p3.x-p1.x, p3.y-p1.y, p3.z-p1.z);
		tmpNormal.nor();

		return tmpNormal;
	}

	public float signedDistanceFromPlane(Vector3 point) {
		Vector3 normal = getNormal();
		float a = normal.x;
		float b = normal.y;
		float c = normal.z;
		Vector3 v1 = getPosition1();
		float d = -(a * v1.x + b * v1.y + c * v1.z);
		return a * point.x + b * point.y + c * point.z + d;
	}

	public boolean intersects(Face other) {

		//distance from the face1 vertices to the face2 plane
		float distFace1Vert1 = other.signedDistanceFromPlane(getPosition1());
		float distFace1Vert2 = other.signedDistanceFromPlane(getPosition2());
		float distFace1Vert3 = other.signedDistanceFromPlane(getPosition3());

		//distances signs from the face1 vertices to the face2 plane
		int signFace1Vert1 = (distFace1Vert1>TOL? 1 :(distFace1Vert1<-TOL? -1 : 0));
		signFace1Vert2 = (distFace1Vert2>TOL? 1 :(distFace1Vert2<-TOL? -1 : 0));
		signFace1Vert3 = (distFace1Vert3>TOL? 1 :(distFace1Vert3<-TOL? -1 : 0));

	}


	public Vector3 getPosition1() {
		getVertexPosition(mesh, v1, tmpPos1);
		return tmpPos1;
	}

	public Vector3 getPosition2() {
		getVertexPosition(mesh, v2, tmpPos2);
		return tmpPos2;
	}

	public Vector3 getPosition3() {
		getVertexPosition(mesh, v3, tmpPos3);
		return tmpPos3;
	}

	public static void getVertexPosition(Mesh mesh, int vertexId, Vector3 out) {
		int vSize = mesh.getVertexAttributes().vertexSize / 4;
		VertexAttribute posAttr = mesh.getVertexAttribute(Position);
		FloatBuffer buffer = mesh.getVerticesBuffer(false);
		float x = buffer.get(vSize * vertexId + posAttr.offset);
		float y = buffer.get(vSize * vertexId + posAttr.offset + 1);
		float z = buffer.get(vSize * vertexId + posAttr.offset + 2);
		out.set(x, y, z);
	}
}
