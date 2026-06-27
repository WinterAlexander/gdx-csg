package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import org.junit.Test;

/**
 * Tests the CSGMesh operations with a libGDX model builder, which is convenient since it requires
 * no GPU initialization
 * <p>
 * Created on 2026-06-26.
 *
 * @author Alexander Winter
 */
public class CSGMeshWithBuilderTest {
	@Test
	public void testBuildWrench() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("wrench", GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position
				| VertexAttributes.Usage.Normal
				| VertexAttributes.Usage.Tangent, new Material());

		CylinderShapeBuilder.build(partBuilder, 0.15f, 0.02f, 0.15f, 10);


	}
}
