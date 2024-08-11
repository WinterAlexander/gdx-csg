package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;
import com.winteralexander.gdx.csg.IntersectorPlus;
import com.winteralexander.gdx.csg.IntersectorPlus.LineIntersectionResult;
import com.winteralexander.gdx.csg.SegmentPlus;
import com.winteralexander.gdx.csg.Triangle;
import org.junit.Test;

import java.util.Random;

import static com.winteralexander.gdx.csg.IntersectorPlus.*;
import static com.winteralexander.gdx.csg.IntersectorPlus.LineIntersectionResult.*;
import static org.junit.Assert.*;

/**
 * Unit test for {@link IntersectorPlus}
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class IntersectorPlusTest {
	@Test
	public void testRayRayIntersection() {
		Ray ray1 = new Ray();
		ray1.origin.set(0f, 0f, 0f);
		ray1.direction.set(1f, 1f, 1f).nor();

		Ray ray2 = new Ray();
		ray2.origin.set(15f, 15f, 15f);
		ray2.direction.set(-9f, 10f, 55f).nor();

		Vector3 intersection = new Vector3();

		assertEquals(POINT, intersectRayRay(ray1, ray2, 1e-5f, intersection));
		assertTrue(intersection.epsilonEquals(15f, 15f, 15f, 1e-5f));

		ray1.origin.set(0f, 0f, 0f);
		ray1.direction.set(-61f, 31f, 12f).nor();

		ray2.origin.set(15f, 15f, 15f);
		ray2.direction.set(-9f, 10f, 55f).nor();

		LineIntersectionResult result = intersectRayRay(ray1, ray2, 1e-5f, intersection);
		assertEquals(NONE, result);
	}

	@Test
	public void testRayRayCollinear() {
		Ray ray1 = new Ray();
		ray1.origin.set(0f, 0f, 0f);
		ray1.direction.set(1f, 1f, 1f).nor();

		Ray ray2 = new Ray();
		ray2.origin.set(15f, 15f, 15f);
		ray2.direction.set(-1f, -1f, -1f).nor();

		Vector3 intersection = new Vector3();

		assertEquals(LineIntersectionResult.COLLINEAR, intersectRayRay(ray1, ray2, 1e-5f, intersection));
	}

	@Test
	public void testRayRayAdversarialCase() {
		// this test case reproduced a precision issue with a prior version of the algorithm
		// remains present to ensure good functioning of the new algorithm
		Ray ray1 = new Ray();
		ray1.origin.set(0.9067098f, -0.8748553f, 0.0036551952f);
		ray1.direction.set(-0.26122704f, 0.6468483f, -0.71648294f);

		Ray ray2 = new Ray();
		ray2.origin.set(0.80948985f, -1.0571202f, -0.2630019f);
		ray2.direction.set(-0.3424806f, 0.01793293f, -0.93935376f);

		Vector3 expectedIntersection = new Vector3(0.9840071f, -1.0662583f, 0.21566316f);

		Vector3 intersection = new Vector3();

		assertEquals(POINT,
				intersectRayRay(ray1, ray2, 1e-4f, intersection));

		float precision = 0f;

		precision = Math.max(precision, Math.abs(expectedIntersection.x - intersection.x));
		precision = Math.max(precision, Math.abs(expectedIntersection.y - intersection.y));
		precision = Math.max(precision, Math.abs(expectedIntersection.z - intersection.z));

		System.out.println("Precision: " + precision);

		if(precision > 1e-6f)
			fail("Computed intersection point has too poor precision");
	}

	@Test
	public void testRayRayIntersectionRandom() {
		Ray ray1 = new Ray();
		Ray ray2 = new Ray();
		Random r = new Random();
		Vector3 tmpIntersection = new Vector3();
		Vector3 tmpComputedIntersection = new Vector3();
		float worstPrecision = 0f;

		for(int i = 0; i < 100_000; i++) {
			ray1.origin.set(r.nextFloat() * 2f - 1f,
					r.nextFloat() * 2f - 1f,
					r.nextFloat() * 2f - 1f);
			do
				ray1.direction.set(r.nextFloat() * 2f - 1f,
						r.nextFloat() * 2f - 1f,
						r.nextFloat() * 2f - 1f).nor();
			while(ray1.direction.len2() == 0f);

			do
				ray2.direction.set(r.nextFloat() * 2f - 1f,
						r.nextFloat() * 2f - 1f,
						r.nextFloat() * 2f - 1f).nor();
			while(ray2.direction.len2() == 0f
					|| Math.abs(ray2.direction.dot(ray1.direction)) > 0.90f);

			float posRay1 = r.nextFloat() * 2f - 1f;
			float posRay2 = r.nextFloat() * 2f - 1f;
			ray1.getEndPoint(tmpIntersection, posRay1);
			ray2.origin.set(tmpIntersection);
			ray2.origin.mulAdd(ray2.direction, posRay2);

			assertEquals(POINT,
					intersectRayRay(ray1, ray2, 1e-6f, tmpComputedIntersection));

			float precision = 0f;

			precision = Math.max(precision, Math.abs(tmpIntersection.x - tmpComputedIntersection.x));
			precision = Math.max(precision, Math.abs(tmpIntersection.y - tmpComputedIntersection.y));
			precision = Math.max(precision, Math.abs(tmpIntersection.z - tmpComputedIntersection.z));
			worstPrecision = Math.max(worstPrecision, precision);

			if(precision > 1e-5f)
				fail("Computed intersection point has too poor precision " + precision);
		}

		System.out.println("Worst precision: " + worstPrecision);
	}

	@Test
	public void testSegmentSegmentIntersection() {
		Segment segment1 = new Segment(0f, 0f, 0f, 1f, 1f, 0f);
		Segment segment2 = new Segment(0f, 1f, 0f, 1f, 0f, 0f);

		Vector3 intersection = new Vector3();

		assertEquals(POINT, intersectSegmentSegment(segment1, segment2, 1e-5f, intersection));
		assertTrue(intersection.epsilonEquals(0.5f, 0.5f, 0f, 1e-5f));

		segment1.a.set(0f, 0f, 0f);
		segment1.b.set(1f, 1f, 0f);

		segment2.a.set(0f, 5f, 0f);
		segment2.b.set(5f, 0f, 0f);

		assertEquals(NONE, intersectSegmentSegment(segment1,
				segment2, 1e-5f, intersection));

		segment1.a.set(0f, 0f, 0f);
		segment1.b.set(1f, 1f, 0f);

		segment2.a.set(0.5f, 0.5f, 0f);
		segment2.b.set(6f, 6f, 0f);

		assertEquals(COLLINEAR, intersectSegmentSegment(segment1,
				segment2, 1e-5f, intersection));

		segment1.a.set(0f, 0f, 0f);
		segment1.b.set(1f, 1f, 0f);

		segment2.a.set(5f, 5f, 0f);
		segment2.b.set(6f, 6f, 0f);

		assertEquals(NONE, intersectSegmentSegment(segment1,
				segment2, 1e-5f, intersection));
	}

	@Test
	public void testTriangleRay() {
		Triangle triangle = new Triangle(0f, 0f, 0f,
				0f, 1f, 0f,
				0f, 1f, 1f);

		Ray ray = new Ray();
		ray.origin.set(0f, 0.5f, 0f);
		ray.direction.set(0f, 0f, 1f);

		Segment segment = new SegmentPlus();

		assertTrue(IntersectorPlus.intersectTriangleRay(triangle, ray, 1e-5f, segment));

		assertTrue(segment.a.epsilonEquals(0.0f, 0.5f, 0.0f));
		assertTrue(segment.b.epsilonEquals(0.0f, 0.5f, 0.5f));

		ray.origin.set(-1f, 0.5f, 0.25f);
		ray.direction.set(1f, 0f, 0f);

		assertTrue(IntersectorPlus.intersectTriangleRay(triangle, ray, 1e-5f, segment));

		assertTrue(segment.a.epsilonEquals(0.0f, 0.5f, 0.25f));
		assertTrue(segment.b.epsilonEquals(0.0f, 0.5f, 0.25f));

		ray.origin.set(-1f, 0.5f, 0.25f);
		ray.direction.set(0f, 1f, 0f);

		assertFalse(IntersectorPlus.intersectTriangleRay(triangle, ray, 1e-5f, segment));
	}

	@Test
	public void testCoplanarTriangleIntersection() {
		Triangle tri1 = new Triangle(
				0f, 0f, 0f,
				0f, 1f, 0f,
				0f, 0f, 1f
		);

		Triangle tri2 = new Triangle(
				0f, 1f, 1f,
				0f, -0.5f, 1f,
				0f, 1f, -0.5f
		);

		assertTrue(intersectCoplanarTriangles(tri1, tri2, 1e-5f));

		tri2.set(0f, 1f, 1f,
				0f, 0.2f, 1f,
				0f, 1f, 0.2f);

		assertFalse(intersectCoplanarTriangles(tri1, tri2, 1e-5f));

		tri2.set(0f, 1f, 1f,
				0f, 0f, 1f,
				0f, 1f, 0f);

		assertTrue(intersectCoplanarTriangles(tri1, tri2, 1e-5f));

		tri2.set(0f, 0f, 0f,
				0f, 0f, -1f,
				0f, -1f, 0f);

		assertTrue(intersectCoplanarTriangles(tri1, tri2, 1e-5f));

		tri2.set(tri1);

		assertTrue(intersectCoplanarTriangles(tri1, tri2, 1e-5f));
	}

	@Test
	public void testTriangleTriangle() {
		Triangle tri1 = new Triangle(), tri2 = new Triangle();

		Segment segment = new SegmentPlus(), expected = new SegmentPlus();

		tri1.set(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
		tri2.set(0.49824142f, 0.0f, 0.0f, 0.14803512f, 1.0f, 0.0f, 0.14803512f, 0.0f, 1.0f);

		assertEquals(TriangleIntersectionResult.NONE, intersectTriangleTriangle(tri1, tri2, 1e-5f, segment));

		expected.a.set(0.0f, 0.0027782063f, 0.0027782067f);
		expected.b.set(0.0f, 0.0027782063f, 0.0027782067f);
		tri1.set(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f);
		tri2.set(0.15553457f, 0.0f, 0.1826436f, 1.0338287f, 1.0f, 0.1826436f, 1.0338287f, 0.0f, 1.1826437f);

		assertEquals(TriangleIntersectionResult.NONE, intersectTriangleTriangle(tri1, tri2, 1e-5f, segment));
	}
}
