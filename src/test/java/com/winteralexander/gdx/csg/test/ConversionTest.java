package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.winteralexander.gdx.csg.CSGMesh;
import org.junit.Test;

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
	@Test
	public void testSingleVertexFromToBuilder() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshPartBuilder partBuilder = builder.part("meshPart", GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
				new Material());

		partBuilder.vertex(new Vector3(0f, 10f, 0f), new Vector3(0f, 1f, 0f), null, null);

		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder);
		assertEquals(1, csgMesh.getVertices().size);
		assertEquals(new Vector3(0f, 10f, 0f), csgMesh.getVertices().get(0).getPosition());
		assertEquals(new Vector3(0f, 1f, 0f), csgMesh.getVertices().get(0).getNormal());

		csgMesh.toBuilder(partBuilder);
	}
}
