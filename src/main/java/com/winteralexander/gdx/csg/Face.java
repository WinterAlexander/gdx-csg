package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;
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

	private final Ray intersectRay = new Ray();

	private final Segment segment1 = new Segment(0f, 0f, 0f, 0f, 0f, 0f),
			segment2 = new Segment(0f, 0f, 0f, 0f, 0f, 0f);

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
		tmpNormal.crs(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z);
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

	public boolean intersects(Face other, float tol) {

		//distance from the face1 vertices to the face2 plane
		float distFace1Vert1 = other.signedDistanceFromPlane(getPosition1());
		float distFace1Vert2 = other.signedDistanceFromPlane(getPosition2());
		float distFace1Vert3 = other.signedDistanceFromPlane(getPosition3());

		//distances signs from the face1 vertices to the face2 plane
		int signFace1Vert1 = (distFace1Vert1 > tol ? 1 : (distFace1Vert1 < -tol ? -1 : 0));
		int signFace1Vert2 = (distFace1Vert2 > tol ? 1 : (distFace1Vert2 < -tol ? -1 : 0));
		int signFace1Vert3 = (distFace1Vert3 > tol ? 1 : (distFace1Vert3 < -tol ? -1 : 0));

		if(signFace1Vert1 == signFace1Vert2 && signFace1Vert2 == signFace1Vert3)
			return false;

		//distance from the face2 vertices to the face1 plane
		float distFace2Vert1 = signedDistanceFromPlane(other.getPosition1());
		float distFace2Vert2 = signedDistanceFromPlane(other.getPosition2());
		float distFace2Vert3 = signedDistanceFromPlane(other.getPosition3());

		//distances signs from the face2 vertices to the face1 plane
		int signFace2Vert1 = (distFace2Vert1 > tol ? 1 : (distFace2Vert1 < -tol ? -1 : 0));
		int signFace2Vert2 = (distFace2Vert2 > tol ? 1 : (distFace2Vert2 < -tol ? -1 : 0));
		int signFace2Vert3 = (distFace2Vert3 > tol ? 1 : (distFace2Vert3 < -tol ? -1 : 0));

		//if the signs are not equal...
		if(signFace2Vert1 == signFace2Vert2 && signFace2Vert2 == signFace2Vert3)
			return false;

		rayFromIntersection(this, other, tol, intersectRay);
		segmentFromIntersection(this,
				signFace1Vert1, signFace1Vert2, signFace1Vert3,
				intersectRay, tol, segment1);
		segmentFromIntersection(other,
				signFace2Vert1, signFace2Vert2, signFace2Vert3,
				intersectRay, tol, segment2);

		float startDist1 = intersectRay.direction.dot(segment1.a.x - intersectRay.origin.x,
				segment1.a.y - intersectRay.origin.y,
				segment1.a.z - intersectRay.origin.z);
		float endDist1 = intersectRay.direction.dot(segment1.b.x - intersectRay.origin.x,
				segment1.b.y - intersectRay.origin.y,
				segment1.b.z - intersectRay.origin.z);

		float startDist2 = intersectRay.direction.dot(segment2.a.x - intersectRay.origin.x,
				segment2.a.y - intersectRay.origin.y,
				segment2.a.z - intersectRay.origin.z);
		float endDist2 = intersectRay.direction.dot(segment2.b.x - intersectRay.origin.x,
				segment2.b.y - intersectRay.origin.y,
				segment2.b.z - intersectRay.origin.z);

		boolean segmentsIntersect = endDist1 < startDist2 + tol
				|| endDist2 < startDist1 + tol;

		return segmentsIntersect;
	}

	private static void rayFromIntersection(Face face1, Face face2, float tol, Ray out) {
		Vector3 normalFace1 = face1.getNormal();
		Vector3 normalFace2 = face2.getNormal();

		//direction: cross product of the faces normals
		out.direction.set(normalFace1).crs(normalFace2);

		//if direction lenght is not zero (the planes aren't parallel )...
		if (!(out.direction.len() < tol)) {
			//getting a line point, zero is set to a coordinate whose direction
			//component isn't zero (line intersecting its origin plan)
			Vector3 v1p = face1.getPosition1();
			Vector3 v2p = face2.getPosition1();

			float d1 = -(normalFace1.x * v1p.x + normalFace1.y * v1p.y + normalFace1.z * v1p.z);
			float d2 = -(normalFace2.x * v2p.x + normalFace2.y * v2p.y + normalFace2.z * v2p.z);
			if(Math.abs(out.direction.x) > tol) {
				out.origin.x = 0;
				out.origin.y = (d2 * normalFace1.z - d1 * normalFace2.z) / out.direction.x;
				out.origin.z = (d1 * normalFace2.y - d2 * normalFace1.y) / out.direction.x;
			} else if(Math.abs(out.direction.y) > tol) {
				out.origin.x = (d1 * normalFace2.z - d2 * normalFace1.z) / out.direction.y;
				out.origin.y = 0;
				out.origin.z = (d2 * normalFace1.x - d1 * normalFace2.x) / out.direction.y;
			} else {
				out.origin.x = (d2 * normalFace1.y - d1 * normalFace2.y) / out.direction.z;
				out.origin.y = (d1 * normalFace2.x - d2 * normalFace1.x) / out.direction.z;
				out.origin.z = 0;
			}
		}

		out.direction.nor();
	}

	private static void segmentFromIntersection(Face face,
	                                            int signV1, int signV2, int signV3,
												Ray ray,
												float tol,
	                                            Segment out) {
		int countSet = 0;
		if(signV1 == 0) {
			out.a.set(face.getPosition1());
			countSet++;
			if(signV2 == signV3) {
				out.b.set(out.a);
				return;
			}
		}

		if(signV2 == 0) {
			(countSet == 0 ? out.a : out.b).set(face.getPosition2());
			countSet++;
			if(countSet == 2)
				return;
			if(signV1 == signV3) {
				out.b.set(out.a);
				return;
			}
		}

		if(signV3 == 0) {
			(countSet == 0 ? out.a : out.b).set(face.getPosition3());
			countSet++;
			if(countSet == 2)
				return;
			if(signV1 == signV2) {
				out.b.set(out.a);
				return;
			}
		}

		Ray edgeRay = new Ray();
		Vector3 point = new Vector3();

		if((signV1 == 1 && signV2 == -1) || (signV1 == -1 && signV2 == 1)) {
			edgeRay.origin.set(face.getPosition1());
			edgeRay.direction.set(face.getPosition2()).sub(edgeRay.origin).nor();
			if(!IntersectorPlus.intersectRayRay(ray, edgeRay, tol, point))
				throw new IllegalStateException("Rays do not intersect");
			(countSet == 0 ? out.a : out.b).set(point);
			countSet++;
			if(countSet == 2) {
				swapIfNeeded(ray, out);
				return;
			}
		}

		if((signV2 == 1 && signV3 == -1) || (signV2 == -1 && signV3 == 1)) {
			edgeRay.origin.set(face.getPosition2());
			edgeRay.direction.set(face.getPosition3()).sub(edgeRay.origin).nor();
			if(!IntersectorPlus.intersectRayRay(ray, edgeRay, tol, point))
				throw new IllegalStateException("Rays do not intersect");
			(countSet == 0 ? out.a : out.b).set(point);
			countSet++;
			if(countSet == 2) {
				swapIfNeeded(ray, out);
				return;
			}
		}

		if((signV3 == 1 && signV1 == -1) || (signV3 == -1 && signV1 == 1)) {
			edgeRay.origin.set(face.getPosition3());
			edgeRay.direction.set(face.getPosition1()).sub(edgeRay.origin).nor();
			if(!IntersectorPlus.intersectRayRay(ray, edgeRay, tol, point))
				throw new IllegalStateException("Rays do not intersect");
			(countSet == 0 ? out.a : out.b).set(point);
			countSet++;
			if(countSet == 2) {
				swapIfNeeded(ray, out);
			}
		}
	}

	private static void swapIfNeeded(Ray ray, Segment segment) {
		float startDist = ray.direction.dot(segment.a.x - ray.origin.x,
				segment.a.y - ray.origin.y,
				segment.a.z - ray.origin.z);
		float endDist = ray.direction.dot(segment.b.x - ray.origin.x,
				segment.b.y - ray.origin.y,
				segment.b.z - ray.origin.z);

		if(startDist > endDist) {
			Vector3 vec = new Vector3();
			vec.set(segment.a);
			segment.a.set(segment.b);
			segment.b.set(vec);
		}
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
