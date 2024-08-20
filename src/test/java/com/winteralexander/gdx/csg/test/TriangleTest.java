package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.math.Vector3;
import com.winteralexander.gdx.csg.Triangle;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the features of {@link Triangle}
 * <p>
 * Created on 2024-08-19.
 *
 * @author Alexander Winter
 */
public class TriangleTest {
	@Test
	public void testBarycentricCoordinates() {
		Triangle triangle = new Triangle();
		Vector3 point = new Vector3();
		Vector3 expected = new Vector3();

		triangle.p1.set(0f, 0f, 0f);
		triangle.p2.set(2f, 0f, 0f);
		triangle.p3.set(0f, 2f, 0f);

		point.set(triangle.p1);
		expected.set(1f, 0f, 0f);
		assertEquals(expected, triangle.getBarycentricCoordinates(point));

		point.set(triangle.p2);
		expected.set(0f, 1f, 0f);
		assertEquals(expected, triangle.getBarycentricCoordinates(point));

		point.set(triangle.p3);
		expected.set(0f, 0f, 1f);
		assertEquals(expected, triangle.getBarycentricCoordinates(point));

		point.set(triangle.p1).scl(0.5f).mulAdd(triangle.p2, 0.5f);
		expected.set(0.5f, 0.5f, 0f);
		assertEquals(expected, triangle.getBarycentricCoordinates(point));

		triangle.p1.set(0f, 0f, 0f);
		triangle.p2.set(0f, 0f, 2f);
		triangle.p3.set(0f, 2f, 0f);

		point.set(triangle.p3);
		expected.set(0f, 0f, 1f);
		assertEquals(expected, triangle.getBarycentricCoordinates(point));

		Random random = new Random();

		for(int i = 0; i < 1000; i++) {
			triangle.p1.set(random.nextFloat() * 10f - 5f,
					random.nextFloat() * 10f - 5f,
					random.nextFloat() * 10f - 5f);
			do {
				triangle.p2.set(random.nextFloat() * 10f - 5f,
						random.nextFloat() * 10f - 5f,
						random.nextFloat() * 10f - 5f);
			} while(triangle.p2.dst2(triangle.p1) < 1e-3f);
			do {
				triangle.p3.set(random.nextFloat() * 10f - 5f,
						random.nextFloat() * 10f - 5f,
						random.nextFloat() * 10f - 5f);
			} while(triangle.p3.dst2(triangle.p1) < 1e-3f
				|| triangle.p3.dst2(triangle.p2) < 1e-3f);

			float alpha = random.nextFloat();
			float beta = random.nextFloat() * (1f - alpha);
			float gamma = 1f - alpha - beta;

			point.set(triangle.p1).scl(alpha).mulAdd(triangle.p2, beta).mulAdd(triangle.p3, gamma);
			expected.set(alpha, beta, gamma);
			assertTrue(expected.epsilonEquals(triangle.getBarycentricCoordinates(point), 1e-5f));
		}
	}
}
