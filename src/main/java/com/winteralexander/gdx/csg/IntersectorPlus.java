package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;

import static com.winteralexander.gdx.utils.math.MathUtil.pow2;
import static com.winteralexander.gdx.utils.math.VectorUtil.getComponent;

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
	 * @param tolerance tolerance to use to determine if the rays intersect or not
	 * @param out output intersection point
	 * @return true if there is one intersection between the two rays, false otherwise
	 */
	public static boolean intersectRayRay(Ray first, Ray second, float tolerance, Vector3 out) {
		Vector3 d1 = first.direction;
		Vector3 d2 = second.direction;
		Vector3 o1 = first.origin;
		Vector3 o2 = second.origin;

		int firstComp = 0, secondComp;

		for(int i = 1; i < 3; i++)
			if(Math.abs(getComponent(d1, i)) > Math.abs(getComponent(d1, firstComp)))
				firstComp = i;

		secondComp = firstComp == 0 ? 1 : 0;

		for(int i = secondComp + 1; i < 3; i++) {
			if(i == firstComp)
				continue;
			if(Math.abs(getComponent(d2, i)) > Math.abs(getComponent(d2, firstComp)))
				secondComp = i;
		}

		double D1a = getComponent(d1, firstComp);
		double D1b = getComponent(d1, secondComp);
		double D2a = getComponent(d2, firstComp);
		double D2b = getComponent(d2, secondComp);
		double Sa = (double)getComponent(o1, firstComp) - getComponent(o2, firstComp);
		double Sb = (double)getComponent(o1, secondComp) - getComponent(o2, secondComp);

		double denom = 1.0 - D1b * D2a / (D2b * D1a);

		if(denom == 0.0)
			return false;

		double t = (Sb * D2a / (D2b * D1a) - Sa / D1a) / denom;
		double t2 = t * D1b / D2b + Sb / D2b;

		float x1 = (float)(o1.x + d1.x * t);
		float y1 = (float)(o1.y + d1.y * t);
		float z1 = (float)(o1.z + d1.z * t);

		float x2 = (float)(o2.x + d2.x * t2);
		float y2 = (float)(o2.y + d2.y * t2);
		float z2 = (float)(o2.z + d2.z * t2);

		if(pow2(x1 - x2) + pow2(y1 - y2) + pow2(z1 - z2) > tolerance)
			return false;

		out.set(x1, y1, z1);
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
		Segment segment1 = new Segment(0f, 0f, 0f, 0f, 0f, 0f);
		Segment segment2 = new Segment(0f, 0f, 0f, 0f, 0f, 0f);

		intersectTriangleRay(first, intersectRay, tol, segment1);
		intersectTriangleRay(second, intersectRay, tol, segment2);

		float dist1A = intersectRay.direction.dot(
				segment1.a.x - intersectRay.origin.x,
				segment1.a.y - intersectRay.origin.y,
				segment1.a.z - intersectRay.origin.z);
		float dist1B = intersectRay.direction.dot(
				segment1.b.x - intersectRay.origin.x,
				segment1.b.y - intersectRay.origin.y,
				segment1.b.z - intersectRay.origin.z);

		float dist2A = intersectRay.direction.dot(
				segment2.a.x - intersectRay.origin.x,
				segment2.a.y - intersectRay.origin.y,
				segment2.a.z - intersectRay.origin.z);
		float dist2B = intersectRay.direction.dot(
				segment2.b.x - intersectRay.origin.x,
				segment2.b.y - intersectRay.origin.y,
				segment2.b.z - intersectRay.origin.z);

		float startDist1 = Math.min(dist1A, dist1B);
		float endDist1 = Math.max(dist1A, dist1B);

		float startDist2 = Math.min(dist2A, dist2B);
		float endDist2 = Math.max(dist2A, dist2B);

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
	 * Tests whether the given plane and the ray are coplanar
	 *
	 * @param plane plane to check
	 * @param ray ray to check
	 * @param tolerance tolerance to use for computations
	 * @return true if coplanar, otherwise false
	 */
	public static boolean areCoplanar(Plane plane,
	                                  Ray ray,
	                                  float tolerance) {
		return Math.abs(plane.normal.dot(ray.direction)) <= tolerance
				&& Math.abs(plane.normal.dot(ray.origin) + plane.getD()) <= tolerance;
	}

	/**
	 * Tests whether the given triangle and the ray are coplanar
	 *
	 * @param triangle triangle to check
	 * @param ray ray to check
	 * @param tolerance tolerance to use for computations
	 * @return true if coplanar, otherwise false
	 */
	public static boolean areCoplanar(Triangle triangle,
	                                  Ray ray,
	                                  float tolerance) {
		Vector3 normal = triangle.getNormal();
		return Math.abs(normal.dot(ray.direction)) <= tolerance
				&& Math.abs(normal.dot(ray.origin) - normal.dot(triangle.p1)) <= tolerance;
	}


	/**
	 * Finds the intersection between a ray and a triangle. This intersection can be either a point
	 * or a segment of the ray, which only happens in the case where the ray and the triangle are
	 * co planar.
	 *
	 * @param triangle the triangle to check for intersection
	 * @param ray the ray to check for intersection
	 * @param tolerance tolerance to use for computations
	 * @param out segment of the intersection, both ends the same if the intersection is a point
	 * @return true if the triangle and the ray intersect, otherwise false
	 */
	public static boolean intersectTriangleRay(Triangle triangle,
	                                           Ray ray,
	                                           float tolerance,
	                                           Segment out) {
		Vector3 normal = triangle.getNormal();
		float d = -normal.dot(triangle.p1);
		float denom = ray.direction.dot(triangle.getNormal());
		if(Math.abs(denom) > tolerance) {
			float t = -(ray.origin.dot(normal) + d) / denom;
			if (t < 0) return false;

			out.a.set(ray.origin).mulAdd(ray.direction, t);
			out.b.set(ray.origin).mulAdd(ray.direction, t);
			return true;
		}

		if(Math.abs(normal.dot(ray.origin) + d) > tolerance)
			return false; // parallel but not coplanar



		return false;
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
