package com.winteralexander;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;

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

	/**
	 * Performs triangle-triangle intersection. If the result is
	 * {@link TriangleIntersectionResult#NONE}, the segment output parameter is left untouched. If
	 * it is {@link TriangleIntersectionResult#POINT} both ends of the segment will be set to the
	 * intersection point. If the result is {@link TriangleIntersectionResult#FACE_FACE} the output
	 * segment is left unset. In any other cases, the output segment is set to be the intersection
	 * of the 2 triangles as specified in the corresponding {@link TriangleIntersectionResult}.
	 *
	 * @param tri1p1 point 1 of triangle 1
	 * @param tri1p2 point 2 of triangle 1
	 * @param tri1p3 point 3 of triangle 1
	 * @param tri2p1 point 1 of triangle 2
	 * @param tri2p2 point 2 of triangle 2
	 * @param tri2p3 point 3 of triangle 2
	 * @param tolerance distance at which 2 floating points are considered to be the same
	 * @param out segment of the intersection, only set if applicable based on the result.
	 * @return result of the intersection
	 */
	public static TriangleIntersectionResult triangleTriangleIntersection(Vector3 tri1p1,
	                                                                      Vector3 tri1p2,
	                                                                      Vector3 tri1p3,
	                                                                      Vector3 tri2p1,
	                                                                      Vector3 tri2p2,
	                                                                      Vector3 tri2p3,
																		  float tolerance,
	                                                                      Segment out) {
		/*Vector3 tri1Nor = new Vector3();
		Vector3 tri2Nor = new Vector3();

		tri1Nor.set(tri1p2.x - tri1p1.x, tri1p2.y - tri1p1.y, tri1p2.z - tri1p1.z);
		tri1Nor.crs(tri1p3.x - tri1p1.x, tri1p3.y - tri1p1.y, tri1p3.z - tri1p1.z);
		tri1Nor.nor();

		tri2Nor.set(tri2p2.x - tri2p1.x, tri2p2.y - tri2p1.y, tri2p2.z - tri2p1.z);
		tri2Nor.crs(tri2p3.x - tri2p1.x, tri2p3.y - tri2p1.y, tri2p3.z - tri2p1.z);
		tri2Nor.nor();

		//distance from the face1 vertices to the face2 plane
		float distFace1Vert1 = signedDistanceFromPlane(getPosition1());
		float distFace1Vert2 = signedDistanceFromPlane(getPosition2());
		float distFace1Vert3 = signedDistanceFromPlane(getPosition3());

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

		return segmentsIntersect;*/

		return TriangleIntersectionResult.NONE;
	}

/*
	private static float signedDistanceFromPlane(Vector3 point) {
		Vector3 normal = getNormal();
		float a = normal.x;
		float b = normal.y;
		float c = normal.z;
		Vector3 v1 = getPosition1();
		float d = -(a * v1.x + b * v1.y + c * v1.z);
		return a * point.x + b * point.y + c * point.z + d;
	}
*/
	/**
	 * Result of a Triangle Triangle intersection
	 */
	public enum TriangleIntersectionResult {
		/**
		 * Result when the triangles do not intersect.
		 */
		NONE,

		/**
		 * Result when the triangles only intersect at a single point.
		 */
		POINT,

		/**
		 * Result when the triangles share part of an edge.
		 * In this case the intersection is defined by a line segment which corresponds to the part
		 * of the edges that intersect.
		 */
		EDGE_EDGE,

		/**
		 * Result when one triangle's edge intersects the other triangle's face.
		 * In this case the intersection is defined by a line segment which corresponds to the part
		 * of the edge that intersect with the face.
		 */
		EDGE_FACE,

		/**
		 * Result when the 2 triangles are coplanar and their faces overlap.
		 * In this case the intersection is defined by a polygon which corresponds to the shared
		 * area of both triangles.
		 */
		FACE_FACE,

		/**
		 * Result when the 2 triangles cross each other in none of the cases described above.
		 * In this case the intersection is defined by a line segment which corresponds to the
		 * line at which the 2 triangles are crossing.
		 */
		CROSS
	}
}
