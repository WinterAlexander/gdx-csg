package com.winteralexander.gdx.csg;

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
	public static boolean intersectRayRay(Ray first, Ray second, float tol, Vector3 out) {
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

		float dst = second.direction.dot(x - second.origin.x,
				y - second.origin.y,
				z - second.origin.z);

		float prevX = out.x;
		float prevY = out.y;
		float prevZ = out.z;
		second.getEndPoint(out, dst);
		if(!out.epsilonEquals(x, y, z, tol * 1e3f)) {
			out.set(prevX, prevY, prevZ);
			return false;
		}

		out.set(x, y, z);
		return true;
	}

	/**
	 * Performs triangle-triangle intersection. If the result is
	 * {@link TriangleIntersectionResult#NONE}, the segment output parameter is left untouched. If
	 * it is {@link TriangleIntersectionResult#POINT} both ends of the segment will be set to the
	 * intersection point. If the result is {@link TriangleIntersectionResult#COPLANAR_FACE_FACE}
	 * the output segment is left unset. In any other cases, the output segment is set to be the
	 * intersection of the 2 triangles as specified in the corresponding
	 * {@link TriangleIntersectionResult}.
	 *
	 * @param first first triangle
	 * @param second second triangle
	 * @param tol distance at which 2 floating points are considered to be the same
	 * @param out segment of the intersection, only set if applicable based on the result.
	 * @return result of the intersection
	 */
	public static TriangleIntersectionResult intersectTriangleTriangle(Triangle first,
	                                                                   Triangle second,
	                                                                   float tol,
	                                                                   Segment out) {
		//distance from the face1 vertices to the face2 plane
		float distFace1Vert1 = signedDistanceFromPlane(second, first.p1);
		float distFace1Vert2 = signedDistanceFromPlane(second, first.p2);
		float distFace1Vert3 = signedDistanceFromPlane(second, first.p3);

		//distances signs from the face1 vertices to the face2 plane
		int signFace1Vert1 = (distFace1Vert1 > tol ? 1 : (distFace1Vert1 < -tol ? -1 : 0));
		int signFace1Vert2 = (distFace1Vert2 > tol ? 1 : (distFace1Vert2 < -tol ? -1 : 0));
		int signFace1Vert3 = (distFace1Vert3 > tol ? 1 : (distFace1Vert3 < -tol ? -1 : 0));

		// if all points are on the same side of the plane
		if(signFace1Vert1 == signFace1Vert2 && signFace1Vert2 == signFace1Vert3)
			// if they are all 0, they are all in the same plane
			return signFace1Vert1 == 0 && intersectCoplanarTriangles(first, second, tol)
					? TriangleIntersectionResult.COPLANAR_FACE_FACE
					: TriangleIntersectionResult.NONE; // otherwise all on one side, no intersection

		//distance from the face2 vertices to the face1 plane
		float distFace2Vert1 = signedDistanceFromPlane(first, second.p1);
		float distFace2Vert2 = signedDistanceFromPlane(first, second.p2);
		float distFace2Vert3 = signedDistanceFromPlane(first, second.p3);

		//distances signs from the face2 vertices to the face1 plane
		int signFace2Vert1 = (distFace2Vert1 > tol ? 1 : (distFace2Vert1 < -tol ? -1 : 0));
		int signFace2Vert2 = (distFace2Vert2 > tol ? 1 : (distFace2Vert2 < -tol ? -1 : 0));
		int signFace2Vert3 = (distFace2Vert3 > tol ? 1 : (distFace2Vert3 < -tol ? -1 : 0));

		Ray intersectRay = new Ray();
		Segment segment1 = new Segment(0f, 0f, 0f, 0f, 0f, 0f);
		Segment segment2 = new Segment(0f, 0f, 0f, 0f, 0f, 0f);
		rayFromIntersection(first, second, tol, intersectRay);
		segmentFromIntersection(first,
				signFace1Vert1, signFace1Vert2, signFace1Vert3,
				intersectRay, tol, segment1);
		segmentFromIntersection(second,
				signFace2Vert1, signFace2Vert2, signFace2Vert3,
				intersectRay, tol, segment2);

		float distA1 = intersectRay.direction.dot(segment1.a.x - intersectRay.origin.x,
				segment1.a.y - intersectRay.origin.y,
				segment1.a.z - intersectRay.origin.z);
		float distB1 = intersectRay.direction.dot(segment1.b.x - intersectRay.origin.x,
				segment1.b.y - intersectRay.origin.y,
				segment1.b.z - intersectRay.origin.z);

		float distA2 = intersectRay.direction.dot(segment2.a.x - intersectRay.origin.x,
				segment2.a.y - intersectRay.origin.y,
				segment2.a.z - intersectRay.origin.z);
		float distB2 = intersectRay.direction.dot(segment2.b.x - intersectRay.origin.x,
				segment2.b.y - intersectRay.origin.y,
				segment2.b.z - intersectRay.origin.z);

		float startDist1 = Math.min(distA1, distB1);
		float endDist1 = Math.max(distA1, distB1);

		float startDist2 = Math.min(distA2, distB2);
		float endDist2 = Math.max(distA2, distB2);

		boolean segmentsIntersect = endDist1 < startDist2 + tol
				|| endDist2 < startDist1 + tol;

		return segmentsIntersect
				? TriangleIntersectionResult.NONCOPLANAR_FACE_FACE
				: TriangleIntersectionResult.NONE;
	}

	private static void rayFromIntersection(Triangle first,
	                                        Triangle second,
	                                        float tol,
	                                        Ray out) {
		Vector3 normalFace1 = first.getNormal();
		Vector3 normalFace2 = second.getNormal();

		// direction: cross product of the faces normals
		out.direction.set(normalFace1).crs(normalFace2);

		// getting a line point, zero is set to a coordinate whose direction
		// component isn't zero (line intersecting its origin plan)
		Vector3 v1p = first.p1;
		Vector3 v2p = second.p2;

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

		out.direction.nor();
	}

	private static void segmentFromIntersection(Triangle triangle,
	                                            int signV1, int signV2, int signV3,
	                                            Ray ray,
	                                            float tol,
	                                            Segment out) {
		int countSet = 0;
		if(signV1 == 0) {
			out.a.set(triangle.p1);
			countSet++;
			if(signV2 == signV3) {
				out.b.set(out.a);
				return;
			}
		}

		if(signV2 == 0) {
			(countSet == 0 ? out.a : out.b).set(triangle.p2);
			countSet++;
			if(countSet == 2)
				return;
			if(signV1 == signV3) {
				out.b.set(out.a);
				return;
			}
		}

		if(signV3 == 0) {
			(countSet == 0 ? out.a : out.b).set(triangle.p3);
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
			edgeRay.origin.set(triangle.p1);
			edgeRay.direction.set(triangle.p2).sub(edgeRay.origin).nor();
			if(!IntersectorPlus.intersectRayRay(ray, edgeRay, tol, point))
				throw new IllegalStateException("Rays do not intersect");
			(countSet == 0 ? out.a : out.b).set(point);
			countSet++;
			if(countSet == 2)
				return;
		}

		if((signV2 == 1 && signV3 == -1) || (signV2 == -1 && signV3 == 1)) {
			edgeRay.origin.set(triangle.p2);
			edgeRay.direction.set(triangle.p3).sub(edgeRay.origin).nor();
			if(!IntersectorPlus.intersectRayRay(ray, edgeRay, tol, point))
				throw new IllegalStateException("Rays do not intersect");
			(countSet == 0 ? out.a : out.b).set(point);
			countSet++;
			if(countSet == 2)
				return;
		}

		if((signV3 == 1 && signV1 == -1) || (signV3 == -1 && signV1 == 1)) {
			edgeRay.origin.set(triangle.p3);
			edgeRay.direction.set(triangle.p1).sub(edgeRay.origin).nor();
			if(!IntersectorPlus.intersectRayRay(ray, edgeRay, tol, point))
				throw new IllegalStateException("Rays do not intersect");
			(countSet == 0 ? out.a : out.b).set(point);
		}
	}

	/**
	 * Test whether 2 given co-planar triangles are intersecting or not. This function assumes the
	 * provided triangles are co-planar and if they aren't, the result is undefined.
	 *
	 * @param first first triangle to check
	 * @param second second triangle to check
	 * @param tolerance distance at which 2 floating points are considered to be the same
	 * @return true if they are intersecting, otherwise false
	 */
	public static boolean intersectCoplanarTriangles(Triangle first,
	                                                 Triangle second,
	                                                 float tolerance) {
		return false;
	}

	private static float signedDistanceFromPlane(Triangle triangle, Vector3 point) {
		Vector3 normal = triangle.getNormal();
		float a = normal.x;
		float b = normal.y;
		float c = normal.z;
		Vector3 v1 = triangle.p1;
		float d = -(a * v1.x + b * v1.y + c * v1.z);
		return a * point.x + b * point.y + c * point.z + d;
	}

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
		COPLANAR_FACE_FACE,

		/**
		 * Result when the 2 triangles are not coplanar and their faces cross.
		 * In this case the intersection is defined by a line segment which corresponds to the
		 * line at which the 2 triangles are crossing.
		 */
		NONCOPLANAR_FACE_FACE
	}
}
