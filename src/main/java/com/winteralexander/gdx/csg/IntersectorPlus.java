package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;

import static com.winteralexander.gdx.csg.IntersectorPlus.LineIntersectionResult.*;
import static com.winteralexander.gdx.utils.math.MathUtil.pow2;
import static java.lang.Math.*;

/**
 * Extension of libGDX's {@link com.badlogic.gdx.math.Intersector} that adds support for some extra
 * intersection detection. The functions of this class are not thread safe.
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class IntersectorPlus {
	private static final Ray tmpIntersectRay = new Ray();
	private static final Ray tmpEdgeLine1 = new Ray(),
			tmpEdgeLine2 = new Ray(),
			tmpEdgeLine3 = new Ray();
	private static final Vector3 tmpIntersection1 = new Vector3(),
			tmpIntersection2 = new Vector3(),
			tmpIntersection3 = new Vector3();
	private static final Segment tmpSegment1 = new SegmentPlus(), tmpSegment2 = new SegmentPlus();
	private static final Vector3 tmpSegmentDir1 = new Vector3(), tmpSegmentDir2 = new Vector3();
	private static final Triangle tmpTriangle = new Triangle();

	/**
	 * Computes the intersection of 2 rays in 3D space. If there an infinite amount of intersections
	 * (same rays), returns {@link LineIntersectionResult#COLLINEAR} and the output is not set. The
	 * output is only modified when this function returns {@link LineIntersectionResult#POINT}.
	 *
	 * @param first first ray to find intersection
	 * @param second second ray to find intersection
	 * @param tolerance tolerance to use to determine if the rays intersect or not
	 * @param out output intersection point
	 * @return result of the intersection, which is either no intersection, a point or collinear
	 */
	public static LineIntersectionResult intersectRayRay(Ray first,
	                                                     Ray second,
	                                                     float tolerance,
	                                                     Vector3 out) {
		return intersectRayRay(first.origin, first.direction,
				second.origin, second.direction, tolerance, out);
	}

	/**
	 * @see #intersectRayRay(Ray, Ray, float, Vector3)
	 */
	public static LineIntersectionResult intersectRayRay(Vector3 origin1,
	                                                     Vector3 direction1,
	                                                     Vector3 origin2,
	                                                     Vector3 direction2,
	                                                     float tolerance,
	                                                     Vector3 out) {
		double sx = origin1.x - origin2.x;
		double sy = origin1.y - origin2.y;
		double sz = origin1.z - origin2.z;

		double denom1 = direction2.y * direction1.x - direction1.y * direction2.x;
		double denom2 = direction2.z * direction1.y - direction1.z * direction2.y;
		double denom3 = direction2.x * direction1.z - direction1.x * direction2.z;

		double t;

		// means the rays are collinear
		if(Math.abs(denom1) <= tolerance
				&& Math.abs(denom2) <= tolerance
				&& Math.abs(denom3) <= tolerance) {
			float originDst2 = (pow2((float)sx) + pow2((float)sy) + pow2((float)sz));

			if(originDst2 <= tolerance)
				return LineIntersectionResult.COLLINEAR;

			float len2 = originDst2 * direction1.len2();
			return Math.abs(pow2(direction1.dot((float)sx, (float)sy, (float)sz)) / len2 - 1f) <= tolerance
					? LineIntersectionResult.COLLINEAR
					: LineIntersectionResult.NONE;
		}

		// for the sake of precision, use the largest dominator for the computation
		if(abs(denom1) > max(abs(denom2), abs(denom3)))
			t = (sy * direction2.x - sx * direction2.y) / denom1;
		else if(abs(denom2) > abs(denom3))
			t = (sz * direction2.y - sy * direction2.z) / denom2;
		else
			t = (sx * direction2.z - sz * direction2.x) / denom3;

		double t2;

		// for the sake of precision, compute t2 from t using the largest component
		if(abs(direction2.x) > max(abs(direction2.y), abs(direction2.z)))
			t2 = t * direction1.x / direction2.x + sx / direction2.x;
		else if(abs(direction2.y) > abs(direction2.z))
			t2 = t * direction1.y / direction2.y + sy / direction2.y;
		else
			t2 = t * direction1.z / direction2.z + sz / direction2.z;

		double x1 = origin1.x + direction1.x * t;
		double y1 = origin1.y + direction1.y * t;
		double z1 = origin1.z + direction1.z * t;

		double x2 = origin2.x + direction2.x * t2;
		double y2 = origin2.y + direction2.y * t2;
		double z2 = origin2.z + direction2.z * t2;

		if(pow2(x1 - x2) + pow2(y1 - y2) + pow2(z1 - z2) > tolerance)
			return LineIntersectionResult.NONE;

		out.set((float)x1, (float)y1, (float)z1);
		return POINT;
	}

	/**
	 * Computes the intersection of 2 segments. The intersection can either be a point or another
	 * collinear segment. If the intersection is a point, it is set in the out parameter. If the
	 * segments are collinear but not intersecting, {@link LineIntersectionResult#NONE} is returned.
	 *
	 * @param first first segment to check for intersection
	 * @param second second segment to check for intersection
	 * @param tol tolerance to use for computations
	 * @param out intersection point if non-collinear intersection
	 * @return result of the intersection
	 */
	public static LineIntersectionResult intersectSegmentSegment(Segment first,
	                                                             Segment second,
	                                                             float tol,
	                                                             Vector3 out) {
		return intersectSegmentSegment(first.a, first.b, second.a, second.b, tol, out);
	}

	/**
	 * @see #intersectSegmentSegment(Segment, Segment, float, Vector3)
	 */
	public static LineIntersectionResult intersectSegmentSegment(Vector3 firstStart,
	                                                             Vector3 firstEnd,
	                                                             Vector3 secondStart,
	                                                             Vector3 secondEnd,
	                                                             float tol,
	                                                             Vector3 out) {
		tmpSegmentDir1.set(firstEnd).sub(firstStart);
		tmpSegmentDir2.set(secondEnd).sub(secondStart);

		LineIntersectionResult result = intersectRayRay(firstStart, tmpSegmentDir1,
				secondStart, tmpSegmentDir2, tol, out);

		if(result == NONE)
			return NONE;

		if(result == COLLINEAR) {
			float t1 = tmpSegmentDir1.dot(secondStart.x - firstStart.x,
					secondStart.y - firstStart.y,
					secondStart.z - firstStart.z) / tmpSegmentDir1.len2();
			float t2 = tmpSegmentDir1.dot(secondEnd.x - firstStart.x,
					secondEnd.y - firstStart.y,
					secondEnd.z - firstStart.z) / tmpSegmentDir1.len2();

			float tMin = min(t1, t2);
			float tMax = max(t1, t2);

			if(tMin > 1f + tol || tMax < -tol)
				return NONE;

			return tMin < 1f - tol && tMax > tol ? COLLINEAR : POINT;
		}

		float t1 = tmpSegmentDir1.dot(out.x - firstStart.x,
				out.y - firstStart.y,
				out.z - firstStart.z) / tmpSegmentDir1.len2();
		float t2 = tmpSegmentDir2.dot(out.x - secondStart.x,
				out.y - secondStart.y,
				out.z - secondStart.z) / tmpSegmentDir2.len2();

		if(t1 < -tol || t1 > 1f + tol || t2 < -tol || t2 > 1f + tol)
			return NONE;

		return POINT;
	}

	/**
	 * @see #intersectTriangleTriangle(Triangle, Triangle, float, Segment)
	 */
	public static TriangleIntersectionResult intersectTriangleTriangle(Triangle first,
	                                                                   Triangle second,
	                                                                   float tol,
	                                                                   Segment out) {
		return intersectTriangleTriangle(first, second, tol, false, out);
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
		if(signFace1Vert1 == signFace1Vert2 && signFace1Vert2 == signFace1Vert3) {
			// if they are all 0, they are all in the same plane
			if(signFace1Vert1 != 0 || ignoreCoplanar)
				return TriangleIntersectionResult.NONE;

			for(int i = 0; i < 3; i++) {
				Vector3 start = first.getPoint(i + 1);
				Vector3 end = first.getPoint((i + 1) % 3 + 1);
				for(int j = 0; j < 3; j++) {

					LineIntersectionResult result = intersectSegmentSegment(start,
							end,
							second.getPoint(j + 1),
							second.getPoint((j + 1) % 3 + 1),
							tol,
							tmpIntersection1);

					if(result == COLLINEAR) {
						Vector3 otherPointA = first.getPoint((i + 2) % 3 + 1);
						Vector3 otherPointB = second.getPoint((j + 2) % 3 + 1);

						Vector3 perp = tmpIntersection1.set(end).sub(start);
						perp.crs(first.getNormal());
						boolean sameDir = Math.signum(perp.dot(otherPointA)) == Math.signum(perp.dot(otherPointB));
						return sameDir ? TriangleIntersectionResult.COPLANAR_FACE_FACE
								: TriangleIntersectionResult.EDGE_EDGE;
					}
				}
			}

			for(int i = 0; i < 3; i++) {
				Vector3 a = first.getPoint(i + 1);
				Vector3 a1 = tmpIntersection1.set(first.getPoint((i + 1) % 3 + 1)).sub(a);
				Vector3 a2 = tmpIntersection2.set(first.getPoint((i + 2) % 3 + 1)).sub(a);
				for(int j = 0; j < 3; j++) {
					Vector3 b = second.getPoint(j + 1);
					Vector3 b1 = tmpSegmentDir1.set(second.getPoint((j + 1) % 3 + 1)).sub(b);
					Vector3 b2 = tmpSegmentDir2.set(second.getPoint((j + 2) % 3 + 1)).sub(b);

					if(a.epsilonEquals(b, tol)) {
						boolean overlap = isBetween(a1, a2, b1)
								|| isBetween(a1, a2, b2)
								|| isBetween(b1, b2, a1)
								|| isBetween(b1, b2, a2);
						return overlap
								? TriangleIntersectionResult.COPLANAR_FACE_FACE
								: TriangleIntersectionResult.POINT;
					}
				}
			}

			for(int i = 0; i < 3; i++) {
				Vector3 a = first.getPoint(i + 1);
				Vector3 e1a = first.getPoint((i + 1) % 3 + 1);
				Vector3 e2a = first.getPoint((i + 2) % 3 + 1);
				for(int j = 0; j < 3; j++) {
					Vector3 b = second.getPoint(j + 1);
					Vector3 e1b = second.getPoint((j + 1) % 3 + 1);
					Vector3 e2b = second.getPoint((j + 2) % 3 + 1);

					if(intersectSegmentSegment(a, e1a, e1b, e2b, tol, tmpIntersection1) == POINT
							&& tmpIntersection1.epsilonEquals(a, tol)) {

						Vector3 perp = tmpIntersection1.set(e1b).sub(e2b);
						perp.crs(first.getNormal());

						boolean sameDir = Math.signum(perp.dot(e1a)) == Math.signum(perp.dot(b));
						return sameDir ? TriangleIntersectionResult.COPLANAR_FACE_FACE
								: TriangleIntersectionResult.EDGE_EDGE;
					}

					if(intersectSegmentSegment(e1a, e2a, b, e2b, tol, tmpIntersection1) == POINT
							&& tmpIntersection1.epsilonEquals(b, tol)) {

						Vector3 perp = tmpIntersection1.set(e1a).sub(e2a);
						perp.crs(first.getNormal());

						boolean sameDir = Math.signum(perp.dot(e2b)) == Math.signum(perp.dot(a));
						return sameDir ? TriangleIntersectionResult.COPLANAR_FACE_FACE
								: TriangleIntersectionResult.EDGE_EDGE;
					}
				}
			}

			return intersectCoplanarTriangles(first, second, tol)
					? TriangleIntersectionResult.COPLANAR_FACE_FACE
					: TriangleIntersectionResult.NONE;
		}

		rayFromIntersection(first, second, tol, tmpIntersectRay);

		if(!intersectTriangleRay(first, tmpIntersectRay, tol, tmpSegment1)
		|| !intersectTriangleRay(second, tmpIntersectRay, tol, tmpSegment2))
			return TriangleIntersectionResult.NONE;

		boolean firstIsEdge = !tmpSegment1.a.epsilonEquals(tmpSegment1.b, tol)
				&& (intersectSegmentSegment(tmpSegment1.a, tmpSegment1.b, first.p1, first.p2, tol, tmpIntersection1) == COLLINEAR
				|| intersectSegmentSegment(tmpSegment1.a, tmpSegment1.b, first.p2, first.p3, tol, tmpIntersection1) == COLLINEAR
				|| intersectSegmentSegment(tmpSegment1.a, tmpSegment1.b, first.p3, first.p1, tol, tmpIntersection1) == COLLINEAR);
		boolean secondIsEdge = !tmpSegment2.a.epsilonEquals(tmpSegment2.b, tol)
				&& (intersectSegmentSegment(tmpSegment2.a, tmpSegment2.b, second.p1, second.p2, tol, tmpIntersection2) == COLLINEAR
				|| intersectSegmentSegment(tmpSegment2.a, tmpSegment2.b, second.p2, second.p3, tol, tmpIntersection2) == COLLINEAR
				|| intersectSegmentSegment(tmpSegment2.a, tmpSegment2.b, second.p3, second.p1, tol, tmpIntersection2) == COLLINEAR);

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

		float startDist1 = min(dist1A, dist1B);
		float endDist1 = max(dist1A, dist1B);

		float startDist2 = min(dist2A, dist2B);
		float endDist2 = max(dist2A, dist2B);

		boolean intersection = endDist1 > startDist2 - tol && startDist1 < endDist2 + tol
				|| endDist2 > startDist1 - tol && startDist2 < endDist1 + tol;

		if(!intersection)
			return TriangleIntersectionResult.NONE;

		out.a.set(tmpIntersectRay.direction)
				.scl(max(startDist1, startDist2))
				.add(tmpIntersectRay.origin);
		out.b.set(tmpIntersectRay.direction)
				.scl(min(endDist1, endDist2))
				.add(tmpIntersectRay.origin);

		if(out.a.epsilonEquals(out.b, tol))
			return TriangleIntersectionResult.POINT;

		if(firstIsEdge && secondIsEdge)
			return TriangleIntersectionResult.EDGE_EDGE;

		if(firstIsEdge || secondIsEdge)
			return TriangleIntersectionResult.EDGE_FACE;

		return TriangleIntersectionResult.NONCOPLANAR_FACE_FACE;
	}

	private static boolean isBetween(Vector3 first, Vector3 second, Vector3 between) {
		float lenFirst = first.len();
		Vector3 middle = tmpIntersection3.set(first).scl(1f / lenFirst).mulAdd(second, 1f / second.len()).nor();
		float d = middle.dot(first) / lenFirst;
		float d2 = middle.dot(between) / between.len();
		if(d2 < 0f)
			return false;
		return d2 > d;
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
	 * @param tol tolerance to use for computations
	 * @param out segment of the intersection, both ends the same if the intersection is a point
	 * @return true if the triangle and the ray intersect, otherwise false
	 */
	public static boolean intersectTriangleRay(Triangle triangle,
	                                           Ray ray,
	                                           float tol,
	                                           Segment out) {
		Vector3 normal = triangle.getNormal();
		float d = -normal.dot(triangle.p1);
		float denom = ray.direction.dot(triangle.getNormal());
		if(abs(denom) > tol) {
			// not coplanar, single point intersection
			float t = -(ray.origin.dot(normal) + d) / denom;
			if(t < -tol)
				return false;

			tmpSegmentDir1.set(triangle.p2).sub(triangle.p1);
			tmpSegmentDir2.set(tmpSegmentDir1).crs(normal);

			float len2 = tmpSegmentDir1.len2();
			float height2 = tmpSegmentDir2.dot(triangle.p3.x - triangle.p1.x,
					triangle.p3.y - triangle.p1.y,
					triangle.p3.z - triangle.p1.z);

			float x = ray.origin.x + ray.direction.x * t;
			float y = ray.origin.y + ray.direction.y * t;
			float z = ray.origin.z + ray.direction.z * t;

			float pU = tmpSegmentDir1.dot(x - triangle.p1.x, y - triangle.p1.y, z - triangle.p1.z) / len2;
			float pV = tmpSegmentDir2.dot(x - triangle.p1.x, y - triangle.p1.y, z - triangle.p1.z) / height2;

			float p3U = tmpSegmentDir1.dot(triangle.p3.x - triangle.p1.x,
					triangle.p3.y - triangle.p1.y,
					triangle.p3.z - triangle.p1.z) / len2;

			if(!inTriangle(pU, pV, p3U, tol))
				return false;

			out.b.set(out.a.set(x, y, z));
			return true;
		}

		if(abs(normal.dot(ray.origin) + d) > tol)
			return false; // parallel but not coplanar

		tmpEdgeLine1.origin.set(triangle.p1);
		tmpEdgeLine1.direction.set(triangle.p2).sub(triangle.p1);

		tmpEdgeLine2.origin.set(triangle.p2);
		tmpEdgeLine2.direction.set(triangle.p3).sub(triangle.p2);

		tmpEdgeLine3.origin.set(triangle.p3);
		tmpEdgeLine3.direction.set(triangle.p1).sub(triangle.p3);

		int countIntersections = 0;

		LineIntersectionResult result1 = intersectRayRay(ray, tmpEdgeLine1, tol,
				tmpIntersection1);
		LineIntersectionResult result2 = intersectRayRay(ray, tmpEdgeLine2, tol,
				tmpIntersection2);
		LineIntersectionResult result3 = intersectRayRay(ray, tmpEdgeLine3, tol,
				tmpIntersection3);

		if(result1 != NONE
				&& result2 != NONE
				&& tmpIntersection2.epsilonEquals(tmpIntersection1, tol))
			result2 = NONE;

		if(result1 != NONE
				&& result3 != NONE
				&& tmpIntersection3.epsilonEquals(tmpIntersection1, tol))
			result3 = NONE;

		if(result2 != NONE
				&& result3 != NONE
				&& tmpIntersection3.epsilonEquals(tmpIntersection2, tol))
			result3 = NONE;

		if(result1 == COLLINEAR) {
			if(result2 == COLLINEAR || result3 == COLLINEAR)
				throw new IllegalStateException("Multiple triangle edges collinear with ray");
			out.a.set(triangle.p1);
			out.b.set(triangle.p2);
			return true;
		}

		if(result2 == COLLINEAR) {
			if(result3 == COLLINEAR)
				throw new IllegalStateException("Multiple triangle edges collinear with ray");
			out.a.set(triangle.p2);
			out.b.set(triangle.p3);
			return true;
		}

		if(result3 == COLLINEAR) {
			out.a.set(triangle.p3);
			out.b.set(triangle.p1);
			return true;
		}

		if(result1 == POINT) {
			float t = tmpEdgeLine1.direction.dot(tmpIntersection1.x - tmpEdgeLine1.origin.x,
					tmpIntersection1.y - tmpEdgeLine1.origin.y,
					tmpIntersection1.z - tmpEdgeLine1.origin.z);
			float tEnd = tmpEdgeLine1.direction.dot(triangle.p2.x - tmpEdgeLine1.origin.x,
					triangle.p2.y - tmpEdgeLine1.origin.y,
					triangle.p2.z - tmpEdgeLine1.origin.z);

			if(t >= -tol && t <= tEnd + tol) {
				out.a.set(tmpIntersection1);
				countIntersections++;
			}
		}

		if(result2 == POINT) {
			float t = tmpEdgeLine2.direction.dot(tmpIntersection2.x - tmpEdgeLine2.origin.x,
					tmpIntersection2.y - tmpEdgeLine2.origin.y,
					tmpIntersection2.z - tmpEdgeLine2.origin.z);
			float tEnd = tmpEdgeLine2.direction.dot(triangle.p3.x - tmpEdgeLine2.origin.x,
					triangle.p3.y - tmpEdgeLine2.origin.y,
					triangle.p3.z - tmpEdgeLine2.origin.z);

			if(t >= -tol && t <= tEnd + tol) {
				(countIntersections == 0 ? out.a : out.b).set(tmpIntersection2);
				countIntersections++;

				if(countIntersections == 2)
					return true;
			}
		}

		if(result3 == POINT) {
			float t = tmpEdgeLine3.direction.dot(tmpIntersection3.x - tmpEdgeLine3.origin.x,
					tmpIntersection3.y - tmpEdgeLine3.origin.y,
					tmpIntersection3.z - tmpEdgeLine3.origin.z);
			float tEnd = tmpEdgeLine3.direction.dot(triangle.p1.x - tmpEdgeLine3.origin.x,
					triangle.p1.y - tmpEdgeLine3.origin.y,
					triangle.p1.z - tmpEdgeLine3.origin.z);

			if(t >= -tol && t <= tEnd + tol) {
				(countIntersections == 0 ? out.a : out.b).set(tmpIntersection3);
				countIntersections++;

				if(countIntersections == 2)
					return true;
			}
		}

		if(countIntersections == 0)
			return false;

		out.b.set(out.a);
		return true;
	}

	private static boolean inTriangle(float pU, float pV, float p3U, float tol) {
		// check pV > 0 (point is above the floor), applies to all cases
		if(pV < -tol)
			return false;

		if(p3U < tol) {
			if(p3U > -tol) {
				// .
				// |\
				// ._\
				// this is for the case where p3U is on top of p1 so close to 0, in this case
				// check pU > 0 and pV < 1 - pU (under the diagonal)
				if(pU < -tol || pV - (1f - pU) > tol)
					return false;
			} else {
				// ._
				//  \ - _
				//   \____- .
				// this is for the case where p3U is to the left of p1, in this case check
				// for both diagonals
				if(pV - pU / p3U < tol || pV - (1 - pU) / (1 - p3U) > tol)
					return false;
			}
		} else {
			if(pU < -tol || pV - pU / p3U > tol)
				return false;

			if(Math.abs(p3U - 1f) > tol) {
				if((p3U < 1f ? 1f : -1f) * (pV - (1 - pU) / (1 - p3U)) > tol)
					return false;
			} else if(pU - 1f > tol)
				return false;
		}

		return true;
	}

	/**
	 * Test whether 2 given co-planar triangles are intersecting or not. This function assumes the
	 * provided triangles are co-planar and if they aren't, the result is undefined.
	 *
	 * @param first first triangle to check
	 * @param second second triangle to check
	 * @param tol distance at which 2 floating points are considered to be the same
	 * @return true if they are intersecting, otherwise false
	 */
	public static boolean intersectCoplanarTriangles(Triangle first,
	                                                 Triangle second,
	                                                 float tol) {
		tmpSegmentDir1.set(first.p2).sub(first.p1);
		tmpSegmentDir2.set(first.p3).sub(first.p1);

		float longestSide1 = Math.max(tmpSegmentDir1.len2(), tmpSegmentDir2.len2());
		longestSide1 = Math.max(longestSide1,
				pow2(first.p2.x - first.p3.x) +
						pow2(first.p2.y - first.p3.y) +
						pow2(first.p2.z - first.p3.z));

		float longestSide2 = pow2(second.p2.x - second.p3.x) +
				pow2(second.p2.y - second.p3.y) +
				pow2(second.p2.z - second.p3.z);
		longestSide2 = Math.max(longestSide2, pow2(second.p1.x - second.p2.x) +
				pow2(second.p1.y - second.p2.y) +
				pow2(second.p1.z - second.p2.z));
		longestSide2 = Math.max(longestSide2, pow2(second.p1.x - second.p3.x) +
				pow2(second.p1.y - second.p3.y) +
				pow2(second.p1.z - second.p3.z));

		// if the second triangle's longest side is larger than the largest side of the first one,
		// we know for sure the second triangle can't fit into the first one
		if(longestSide2 > longestSide1)
			return intersectCoplanarTriangles(second, first, tol);

		// otherwise the first triangle can't fit into the second one

		tmpTriangle.set(second).sub(first.p1);
		tmpSegmentDir2.set(tmpSegmentDir1).crs(first.getNormal()).scl(-1f);

		float len2 = tmpSegmentDir1.len2();
		float height2 = tmpSegmentDir2.dot(first.p3.x - first.p1.x,
				first.p3.y - first.p1.y,
				first.p3.z - first.p1.z);

		float peakU = tmpSegmentDir1.dot(first.p3.x - first.p1.x,
				first.p3.y - first.p1.y,
				first.p3.z - first.p1.z) / len2;

		float p1U = tmpSegmentDir1.dot(tmpTriangle.p1) / len2;
		float p1V = tmpSegmentDir2.dot(tmpTriangle.p1) / height2;

		float p2U = tmpSegmentDir1.dot(tmpTriangle.p2) / len2;
		float p2V = tmpSegmentDir2.dot(tmpTriangle.p2) / height2;

		float p3U = tmpSegmentDir1.dot(tmpTriangle.p3) / len2;
		float p3V = tmpSegmentDir2.dot(tmpTriangle.p3) / height2;

		if(inTriangle(p1U, p1V, peakU, tol))
			return true;

		if(inTriangle(p2U, p2V, peakU, tol))
			return true;

		if(inTriangle(p3U, p3V, peakU, tol))
			return true;

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
	 * Result of a Line-Line intersection where the lines can either be rays or segments
	 */
	public enum LineIntersectionResult {
		/**
		 * Result when the lines do not intersect
		 */
		NONE,

		/**
		 * Result when the lines are intersecting but collinear
		 */
		COLLINEAR,

		/**
		 * Result when the lines are intersecting at a single point
		 */
		POINT
	}

	/**
	 * Result of a Triangle-Triangle intersection
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
