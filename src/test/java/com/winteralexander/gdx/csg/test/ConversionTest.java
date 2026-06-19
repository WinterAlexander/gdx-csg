package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.winteralexander.gdx.csg.CSGMesh;
import com.winteralexander.gdx.csg.CSGUtil;
import com.winteralexander.gdx.csg.test.debugviewer.CSGMeshViewer;
import com.winteralexander.gdx.csg.test.debugviewer.ModelViewer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

/**
 * Unit test that targets the conversion from {@link Mesh} or {@link MeshPartBuilder}
 * to {@link CSGMesh} and back
 * <p>
 * Created on 2026-06-18.
 *
 * @author Alexander Winter
 */
public class ConversionTest {

	@BeforeClass
	public static void initGL() throws Exception {
		if(Gdx.gl != null) {
			Display.destroy();
			Gdx.gl = null;
			Gdx.graphics = null;
			Gdx.gl20 = null;
			Gdx.gl30 = null;
			Gdx.gl31 = null;
			Gdx.gl32 = null;
		}

		LwjglNativesLoader.load();
		Class<LwjglGraphics> gfx = LwjglGraphics.class;
		Constructor<LwjglGraphics>
				cons = gfx.getDeclaredConstructor(LwjglApplicationConfiguration.class);
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
	public void testSingleVertexFromToBuilder() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("meshPart", GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
				new Material());

		partBuilder.vertex(new Vector3(0f, 10f, 0f), new Vector3(0f, 1f, 0f), null, null);

		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder);
		assertEquals(1, csgMesh.getVertices().size);
		assertEquals(new Vector3(0f, 10f, 0f), csgMesh.getVertices().get(0).getPosition());
		assertEquals(new Vector3(0f, 1f, 0f), csgMesh.getVertices().get(0).getNormal());

		csgMesh.toBuilder(partBuilder);

		assertEquals(2, partBuilder.getNumVertices());
	}

	@Test
	public void inplaceSubstraction() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("meshPart", GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
				new Material());

		BoxShapeBuilder.build(partBuilder, 1f, 1f, 1f);
		IntArray indices = new IntArray();
		for(int i = 0; i < partBuilder.getNumVertices(); i++)
			indices.add(i);
		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder);

		ModelBuilder builder2 = new ModelBuilder();
		builder2.begin();
		MeshBuilder partBuilder2 = (MeshBuilder)builder.part("meshPart", GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
				new Material());

		BoxShapeBuilder.build(partBuilder2, 0.5f, 0.5f, 0.5f);
		CSGMesh second = CSGMesh.fromBuilder(partBuilder2);
		CSGMeshViewer.start(csgMesh, second);
		CSGMesh result = CSGUtil.subtraction(csgMesh, second);
		CSGMeshViewer.start(result);
		result.toBuilder(partBuilder, indices);

		// validates it properly deleted the old vertices
		assertEquals(48, partBuilder.getNumVertices());
		Model model = builder.end();
		ModelViewer.start(model);
	}
}
