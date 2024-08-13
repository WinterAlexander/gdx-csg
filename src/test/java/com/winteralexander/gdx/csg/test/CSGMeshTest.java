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
import com.winteralexander.gdx.csg.CSGMesh;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the functionality of {@link CSGMesh}
 * <p>
 * Created on 2024-08-12.
 *
 * @author Alexander Winter
 */
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

	@SuppressWarnings("unchecked")
	@Test
	public void testConversion() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(2, 10f, 3f, new Material(), VertexAttributes.Usage.Position);
		Mesh mesh = box.meshes.get(0);
		CSGMesh csg = CSGMesh.fromMesh(mesh);

		Mesh newMesh = csg.toMesh();

		assertEquals(mesh.getNumVertices(), newMesh.getNumVertices());
		assertEquals(mesh.getNumIndices(), newMesh.getNumIndices());
	}
}
