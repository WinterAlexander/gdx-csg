package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;
import com.winteralexander.gdx.utils.math.FloatUtil;

/**
 * Extension of libGDX's {@link com.badlogic.gdx.math.Intersector} that adds support for some extra
 * intersection detection
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class IntersectorPlus {
	/**
	 * Computes the intersection of 2 rays in 3D space, if there is a single intersection. If there
	 * an infinite amount of intersections (same rays), returns false. The output is only modified
	 * when this function returns true.
	 *
	 * @param first first ray to find intersection
	 * @param second second ray to find intersection
	 * @param tol tolerance for the intersection
	 * @param out output intersection point
	 * @return true if there is one intersection between the two rays, false otherwise
	 */
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

		Ray intersectRay = new Ray();
		rayFromIntersection(first, second, tol, intersectRay);

		float dist1A = intersectRay.direction.dot(first.p1.x - intersectRay.origin.x,
				first.p1.y - intersectRay.origin.y,
				first.p1.z - intersectRay.origin.z);
		float dist1B = intersectRay.direction.dot(first.p2.x - intersectRay.origin.x,
				first.p2.y - intersectRay.origin.y,
				first.p2.z - intersectRay.origin.z);
		float dist1C = intersectRay.direction.dot(first.p3.x - intersectRay.origin.x,
				first.p3.y - intersectRay.origin.y,
				first.p3.z - intersectRay.origin.z);

		float dist2A = intersectRay.direction.dot(second.p1.x - intersectRay.origin.x,
				second.p1.y - intersectRay.origin.y,
				second.p1.z - intersectRay.origin.z);
		float dist2B = intersectRay.direction.dot(second.p2.x - intersectRay.origin.x,
				second.p2.y - intersectRay.origin.y,
				second.p2.z - intersectRay.origin.z);
		float dist2C = intersectRay.direction.dot(second.p3.x - intersectRay.origin.x,
				second.p3.y - intersectRay.origin.y,
				second.p3.z - intersectRay.origin.z);

		float startDist1 = FloatUtil.min(dist1A, dist1B, dist1C);
		float endDist1 = FloatUtil.max(dist1A, dist1B, dist1C);

		float startDist2 = FloatUtil.min(dist2A, dist2B, dist2C);
		float endDist2 = FloatUtil.max(dist2A, dist2B, dist2C);

		boolean intersection = endDist1 > startDist2 && startDist1 < endDist2
				|| endDist2 < startDist1 + tol && startDist2 < endDist1;

		if(!intersection)
			return TriangleIntersectionResult.NONE;

		out.a.set(intersectRay.direction)
				.scl(Math.max(startDist1, startDist2))
				.add(intersectRay.origin);
		out.b.set(intersectRay.direction)
				.scl(Math.min(endDist1, endDist2))
				.add(intersectRay.origin);

		return TriangleIntersectionResult.NONCOPLANAR_FACE_FACE;
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
