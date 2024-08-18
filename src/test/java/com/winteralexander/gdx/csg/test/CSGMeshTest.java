package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.winteralexander.gdx.csg.CSGMesh;
import com.winteralexander.gdx.csg.CSGUtil;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;

/**
 * Unit tests for the functionality of {@link CSGMesh}
 * <p>
 * Created on 2024-08-12.
 *
 * @author Alexander Winter
 */
@Ignore
public class CSGMeshTest {
	@BeforeClass
	public static void initGL() throws Exception {
		LwjglNativesLoader.load();
		Class<LwjglGraphics> gfx = LwjglGraphics.class;
		Constructor<LwjglGraphics> cons = gfx.getDeclaredConstructor(LwjglApplicationConfiguration.class);
		cons.setAccessible(true);
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.undecorated = true;
		config.width = 1;
		config.height = 1;

		Gdx.graphics = cons.newInstance(config);

		Method method = gfx.getDeclaredMethod("setupDisplay");
		method.setAccessible(true);
		method.invoke(Gdx.graphics);
	}

	@Test
	public void ensureNotDupVertices() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createSphere(1f, 1f, 1f, 10, 10, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh mesh = box.meshes.get(0);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				//.scale(1.1f, 1f, 0.8f)
				.translate(0f, 0.8f, 0f));
		CSGMesh csg = CSGMesh.fromMesh(mesh);
		CSGMesh otherCsg = CSGMesh.fromMesh(other);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		CSGMesh copy1 = csg.cpy();
		CSGMesh copy2 = otherCsg.cpy();

		csg.splitTriangles(copy2);
		otherCsg.splitTriangles(copy1);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		csg.classifyFaces(copy2);
		otherCsg.classifyFaces(copy1);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		csg.removeFaces(true);
		otherCsg.removeFaces(false);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		otherCsg.invertTriangles();

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);
	}

	private static void assertNoDupVertex(CSGMesh csg) {
		for(int i = 0; i < csg.getVertices().size; i++) {
			for(int j = i + 1; j < csg.getVertices().size; j++) {
				if(i == j)
					continue;

				assertNotSame(csg.getVertices().get(i), csg.getVertices().get(j));
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCubeSphereSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model sphere = builder.createSphere(1f, 1f, 1f, 50, 50, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.3f, 0.3f, 0.3f));

		CSGUtil.subtraction(box, sphereMesh);

		Display.destroy();
		Gdx.gl = null;
		Gdx.graphics = null;
		Gdx.gl20 = null;
		Gdx.gl30 = null;
		Gdx.gl31 = null;
		Gdx.gl32 = null;

		ModelViewer.start(box);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCubeCylinderSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createCylinder(0.8f, 1f, 0.8f, 25, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0f, 0.8f, 0f));

		CSGUtil.subtraction(box, other);

		Display.destroy();
		Gdx.gl = null;
		Gdx.graphics = null;
		Gdx.gl20 = null;
		Gdx.gl30 = null;
		Gdx.gl31 = null;
		Gdx.gl32 = null;

		ModelViewer.start(box);
	}


	@SuppressWarnings("unchecked")
	@Test
	public void testCubePrismSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh mesh = box.meshes.get(0);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.scale(1.1f, 1f, 0.8f)
				.translate(0f, 0.95f, 0f));
		CSGMesh csg = CSGMesh.fromMesh(mesh);
		CSGMesh otherCsg = CSGMesh.fromMesh(other);

		CSGMesh copy1 = csg.cpy();
		CSGMesh copy2 = otherCsg.cpy();

		csg.splitTriangles(copy2);
		otherCsg.splitTriangles(copy1);

		csg.classifyFaces(copy2);
		otherCsg.classifyFaces(copy1);

		csg.removeFaces(true);
		otherCsg.removeFaces(false);

		otherCsg.invertTriangles();

		Mesh newMesh = csg.toMesh();

		//assertEquals(mesh.getNumVertices(), newMesh.getNumVertices());
		//assertEquals(mesh.getNumIndices(), newMesh.getNumIndices());

		box.meshes.set(0, newMesh);
		box.meshParts.get(0).set("box", newMesh, 0, newMesh.getNumIndices(), GL_TRIANGLES);
		box.meshParts.get(0).update();

		Mesh secondNewMesh = otherCsg.toMesh();

		second.meshes.set(0, secondNewMesh);
		second.meshParts.get(0).set("box", secondNewMesh, 0, secondNewMesh.getNumIndices(), GL_TRIANGLES);
		second.meshParts.get(0).update();

		ModelViewer.start(box, second);
		//CSGMeshViewer.start(new CSGMesh[] { csg, otherCsg }, new Ray[0]);
	}

	@Test
	public void testCubeSphereIntersection() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model sphere = builder.createSphere(1f, 1f, 1f, 50, 50, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.3f, 0.3f, 0.3f));

		CSGUtil.intersection(box, sphereMesh);

		ModelViewer.start(box);
	}

	@Test
	public void testCubeSphereUnion() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model sphere = builder.createSphere(1f, 1f, 1f, 50, 50, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.3f, 0.3f, 0.3f));

		CSGUtil.union(box, sphereMesh);

		ModelViewer.start(box);
	}

	@Test
	public void testCylinderUnions() {
		ModelBuilder builder = new ModelBuilder();
		Model cylinder = builder.createCylinder(0.5f, 2f, 0.5f, 10, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh cylMesh = cylinder.meshes.get(0);
		CSGMesh cylinder1 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(1f, 0f, 0f), 90f));

		CSGMesh cylinder2 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 90f));

		CSGMesh cylinder3 = CSGMesh.fromMesh(cylMesh);

		CSGMesh copy1 = cylinder1.cpy();
		CSGMesh copy2 = cylinder2.cpy();

		copy1.splitTriangles(cylinder2);
		copy2.splitTriangles(cylinder1);

		copy1.classifyFaces(cylinder2);
		copy2.classifyFaces(cylinder1);


		CSGMeshViewer.start(copy1, copy2);

		copy1.removeFaces(true);
		copy2.removeFaces(true);

		copy1.mergeWith(copy2);

		CSGMesh firstUnion = copy1;

		CSGMesh cylinders = CSGUtil.union(firstUnion, cylinder3);

		CSGMeshViewer.start(cylinders);
	}

	@Test
	public void testClassicExample() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model sphere = builder.createSphere(1.25f, 1.25f, 1.25f, 10, 10, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model cylinder = builder.createCylinder(0.5f, 2f, 0.5f, 10, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);

		CSGMesh boxCSG = CSGMesh.fromMesh(box.meshes.get(0));
		CSGMesh sphereCSG = CSGMesh.fromMesh(sphere.meshes.get(0));
		Mesh cylMesh = cylinder.meshes.get(0);

		CSGMesh cylinder1 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(1f, 0f, 0f), 90f));

		CSGMesh cylinder2 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 90f));

		CSGMesh cylinder3 = CSGMesh.fromMesh(cylMesh);

		CSGMesh cylinders = CSGUtil.union(CSGUtil.union(cylinder1, cylinder2), cylinder3);

		CSGMesh roundedBox = CSGUtil.intersection(boxCSG, sphereCSG);

		CSGMesh last = CSGUtil.subtraction(roundedBox, cylinders);

		box.meshes.set(0, last.toMesh());
		box.meshParts.get(0).set("box", box.meshes.get(0), 0, box.meshes.get(0).getNumIndices(), GL_TRIANGLES);
		box.meshParts.get(0).update();

		ModelViewer.start(box);
	}
}
