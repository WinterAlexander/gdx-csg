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
import com.badlogic.gdx.math.collision.Ray;
import com.winteralexander.gdx.csg.CSGMesh;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;
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
				/*assertFalse(csg.getVertices()
						.get(i)
						.getPosition()
						.epsilonEquals(csg.getVertices()
								.get(j)
								.getPosition(), 1e-6f));*/
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConversion() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		//Model second = builder.createBox(1f, 1f, 1f, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createSphere(1f, 1f, 1f, 15, 15, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		//Model second = builder.createCylinder(0.8f, 1f, 0.8f, 15, new Material(), VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh mesh = box.meshes.get(0);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				//.scale(1.1f, 1f, 0.8f)
				.translate(0.5f, 0.5f, 0.5f));
		CSGMesh csg = CSGMesh.fromMesh(mesh);
		CSGMesh otherCsg = CSGMesh.fromMesh(other);

		CSGMesh copy1 = csg.cpy();
		CSGMesh copy2 = otherCsg.cpy();

		csg.splitTriangles(copy2);
		otherCsg.splitTriangles(copy1);

		csg.classifyFaces(copy2);
		otherCsg.classifyFaces(copy1);

		//csg.removeFaces(true);
		//otherCsg.removeFaces(false);

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

		Display.destroy();
		Gdx.gl = null;
		Gdx.graphics = null;
		Gdx.gl20 = null;
		Gdx.gl30 = null;
		Gdx.gl31 = null;
		Gdx.gl32 = null;

		//ModelViewer.start(box, second);
		CSGMeshViewer.start(new CSGMesh[] { csg, otherCsg }, new Ray[0]);
	}
}
