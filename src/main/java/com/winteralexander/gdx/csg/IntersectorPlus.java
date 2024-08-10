package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;

import static com.winteralexander.gdx.utils.math.MathUtil.pow2;
import static java.lang.Math.abs;
import static java.lang.Math.max;

/**
 * Extension of libGDX's {@link com.badlogic.gdx.math.Intersector} that adds support for some extra
 * intersection detection
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class IntersectorPlus {
	private static final Ray tmpIntersectRay = new Ray(), tmpEdgeLine = new Ray();
	private static final Vector3 tmpIntersection = new Vector3();
	private static final Segment tmpSegment1 = new SegmentPlus(), tmpSegment2 = new SegmentPlus();

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

		double sx = o1.x - o2.x;
		double sy = o1.y - o2.y;
		double sz = o1.z - o2.z;

		double denom1 = d2.y * d1.x - d1.y * d2.x;
		double denom2 = d2.z * d1.y - d1.z * d2.y;
		double denom3 = d2.x * d1.z - d1.x * d2.z;

		double t;

		// means the rays are collinear
		if(denom1 == 0 && denom2 == 0 && denom3 == 0)
			return false;

		// for the sake of precision, use the largest dominator for the computation
		if(abs(denom1) > max(abs(denom2), abs(denom3)))
			t = (sy * d2.x - sx * d2.y) / denom1;
		else if(abs(denom2) > abs(denom3))
			t = (sz * d2.y - sy * d2.z) / denom2;
		else
			t = (sx * d2.z - sz * d2.x) / denom3;

		double t2;

		// for the sake of precision, compute t2 from t using the largest component
		if(abs(d2.x) > max(abs(d2.y), abs(d2.z)))
			t2 = t * d1.x / d2.x + sx / d2.x;
		else if(abs(d2.y) > abs(d2.z))
			t2 = t * d1.y / d2.y + sy / d2.y;
		else
			t2 = t * d1.z / d2.z + sz / d2.z;

		double x1 = o1.x + d1.x * t;
		double y1 = o1.y + d1.y * t;
		double z1 = o1.z + d1.z * t;

		double x2 = o2.x + d2.x * t2;
		double y2 = o2.y + d2.y * t2;
		double z2 = o2.z + d2.z * t2;

		if(pow2(x1 - x2) + pow2(y1 - y2) + pow2(z1 - z2) > tolerance)
			return false;

		out.set((float)x1, (float)y1, (float)z1);
		return true;
	}

	public static TriangleIntersectionResult intersectTriangleTriangle(Triangle first,
	                                                                   Triangle second,
	                                                                   float tol,
	                                                                   Segment out) {
		return intersectTriangleTriangle(first, second, tol, out);
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
	 * @param ignoreCoplanar if true, coplanar triangles will be considered as non intersecting,
	 * increasing performance as coplanar triangle intersection won't need to be checked
	 * @param out segment of the intersection, only set if applicable based on the result.
	 * @return result of the intersection
	 */
	public static TriangleIntersectionResult intersectTriangleTriangle(Triangle first,
	                                                                   Triangle second,
	                                                                   float tol,
																	   boolean ignoreCoplanar,
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
			return !ignoreCoplanar
					&& signFace1Vert1 == 0
					&& intersectCoplanarTriangles(first, second, tol)
						? TriangleIntersectionResult.COPLANAR_FACE_FACE
						: TriangleIntersectionResult.NONE;

		rayFromIntersection(first, second, tol, tmpIntersectRay);

		intersectTriangleRay(first, tmpIntersectRay, tol, tmpSegment1);
		intersectTriangleRay(second, tmpIntersectRay, tol, tmpSegment2);

		float dist1A = tmpIntersectRay.direction.dot(
				tmpSegment1.a.x - tmpIntersectRay.origin.x,
				tmpSegment1.a.y - tmpIntersectRay.origin.y,
				tmpSegment1.a.z - tmpIntersectRay.origin.z);
		float dist1B = tmpIntersectRay.direction.dot(
				tmpSegment1.b.x - tmpIntersectRay.origin.x,
				tmpSegment1.b.y - tmpIntersectRay.origin.y,
				tmpSegment1.b.z - tmpIntersectRay.origin.z);

		float dist2A = tmpIntersectRay.direction.dot(
				tmpSegment2.a.x - tmpIntersectRay.origin.x,
				tmpSegment2.a.y - tmpIntersectRay.origin.y,
				tmpSegment2.a.z - tmpIntersectRay.origin.z);
		float dist2B = tmpIntersectRay.direction.dot(
				tmpSegment2.b.x - tmpIntersectRay.origin.x,
				tmpSegment2.b.y - tmpIntersectRay.origin.y,
				tmpSegment2.b.z - tmpIntersectRay.origin.z);

		float startDist1 = Math.min(dist1A, dist1B);
		float endDist1 = max(dist1A, dist1B);

		float startDist2 = Math.min(dist2A, dist2B);
		float endDist2 = max(dist2A, dist2B);

		boolean intersection = endDist1 > startDist2 && startDist1 < endDist2
				|| endDist2 < startDist1 + tol && startDist2 < endDist1;

		if(!intersection)
			return TriangleIntersectionResult.NONE;

		out.a.set(tmpIntersectRay.direction)
				.scl(max(startDist1, startDist2))
				.add(tmpIntersectRay.origin);
		out.b.set(tmpIntersectRay.direction)
				.scl(Math.min(endDist1, endDist2))
				.add(tmpIntersectRay.origin);

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
		if(abs(out.direction.x) > tol) {
			out.origin.x = 0;
			out.origin.y = (d2 * normalFace1.z - d1 * normalFace2.z) / out.direction.x;
			out.origin.z = (d1 * normalFace2.y - d2 * normalFace1.y) / out.direction.x;
		} else if(abs(out.direction.y) > tol) {
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
		return abs(plane.normal.dot(ray.direction)) <= tolerance
				&& abs(plane.normal.dot(ray.origin) + plane.getD()) <= tolerance;
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
		return abs(normal.dot(ray.direction)) <= tolerance
				&& abs(normal.dot(ray.origin) - normal.dot(triangle.p1)) <= tolerance;
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
		if(abs(denom) > tolerance) {
			// not coplanar, single point intersection
			float t = -(ray.origin.dot(normal) + d) / denom;
			if(t < 0)
				return false;

			out.a.set(ray.origin).mulAdd(ray.direction, t);
			out.b.set(ray.origin).mulAdd(ray.direction, t);
			return true;
		}

		if(abs(normal.dot(ray.origin) + d) > tolerance)
			return false; // parallel but not coplanar

		tmpEdgeLine.origin.set(triangle.p1);
		tmpEdgeLine.direction.set(triangle.p2).sub(triangle.p1);

		int countIntersections = 0;

		if(intersectRayRay(ray, tmpEdgeLine, tolerance, tmpIntersection)) {
			float t = tmpEdgeLine.direction.dot(tmpIntersection.x - tmpEdgeLine.origin.x,
					tmpIntersection.y - tmpEdgeLine.origin.y,
					tmpIntersection.z - tmpEdgeLine.origin.z);
			float tEnd = tmpEdgeLine.direction.dot(triangle.p2.x - tmpEdgeLine.origin.x,
					triangle.p2.y - tmpEdgeLine.origin.y,
					triangle.p2.z - tmpEdgeLine.origin.z);

			if(t >= -tolerance && t <= tEnd + tolerance) {
				out.a.set(tmpIntersection);
				countIntersections++;
			}
		}

		tmpEdgeLine.origin.set(triangle.p2);
		tmpEdgeLine.direction.set(triangle.p3).sub(triangle.p2);

		if(intersectRayRay(ray, tmpEdgeLine, tolerance, tmpIntersection)) {
			float t = tmpEdgeLine.direction.dot(tmpIntersection.x - tmpEdgeLine.origin.x,
					tmpIntersection.y - tmpEdgeLine.origin.y,
					tmpIntersection.z - tmpEdgeLine.origin.z);
			float tEnd = tmpEdgeLine.direction.dot(triangle.p3.x - tmpEdgeLine.origin.x,
					triangle.p3.y - tmpEdgeLine.origin.y,
					triangle.p3.z - tmpEdgeLine.origin.z);

			if(t >= -tolerance && t <= tEnd + tolerance) {
				(countIntersections == 0 ? out.a : out.b).set(tmpIntersection);
				countIntersections++;

				if(countIntersections == 2)
					return true;
			}
		}

		tmpEdgeLine.origin.set(triangle.p3);
		tmpEdgeLine.direction.set(triangle.p1).sub(triangle.p3);

		if(intersectRayRay(ray, tmpEdgeLine, tolerance, tmpIntersection)) {
			float t = tmpEdgeLine.direction.dot(tmpIntersection.x - tmpEdgeLine.origin.x,
					tmpIntersection.y - tmpEdgeLine.origin.y,
					tmpIntersection.z - tmpEdgeLine.origin.z);
			float tEnd = tmpEdgeLine.direction.dot(triangle.p1.x - tmpEdgeLine.origin.x,
					triangle.p1.y - tmpEdgeLine.origin.y,
					triangle.p1.z - tmpEdgeLine.origin.z);

			if(t >= -tolerance && t <= tEnd + tolerance) {
				(countIntersections == 0 ? out.a : out.b).set(tmpIntersection);
				countIntersections++;

				if(countIntersections == 2)
					return true;
			}
		}

		if(countIntersections == 0)
			return false;

		throw new IllegalStateException("Only intersected with 1 side of a triangle, " +
				"which shouldn't happen");
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
