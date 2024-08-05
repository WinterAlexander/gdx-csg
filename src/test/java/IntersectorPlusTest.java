import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.winteralexander.IntersectorPlus;
import org.junit.Test;

import java.util.Random;

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

		IntersectorPlus.rayRayIntersection(ray1, ray2, 1e-5f, intersection);
		assertEquals(new Vector3(15f, 15f, 15f), intersection);

		ray1.origin.set(0f, 0f, 0f);
		ray1.direction.set(-61f, 31f, 12f).nor();

		ray2.origin.set(15f, 15f, 15f);
		ray2.direction.set(-9f, 10f, 55f).nor();

		boolean result = IntersectorPlus.rayRayIntersection(ray1, ray2, 1e-5f, intersection);
		//assertFalse(result);
	}

	@Test
	public void testRayRayIntersectionRandom() {
		Ray ray1 = new Ray();
		Ray ray2 = new Ray();
		Random r = new Random();
		Vector3 tmpIntersection = new Vector3();
		Vector3 tmpComputedIntersection = new Vector3();
		for(int i = 0; i < 1000; i++) {
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
			while(ray2.direction.len2() == 0f);

			float posRay1 = r.nextFloat() * 2f - 1f;
			float posRay2 = r.nextFloat() * 2f - 1f;
			ray1.getEndPoint(tmpIntersection, posRay1);
			ray2.origin.set(tmpIntersection);
			ray2.origin.mulAdd(ray2.direction, posRay2);
			assertTrue(IntersectorPlus.rayRayIntersection(ray1, ray2, 1e-5f,
					tmpComputedIntersection));
			assertTrue(tmpIntersection.epsilonEquals(tmpComputedIntersection, 1e-3f));
		}
	}
}
