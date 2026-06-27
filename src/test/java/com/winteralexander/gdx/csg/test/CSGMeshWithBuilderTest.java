package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.utils.IntArray;
import com.winteralexander.gdx.csg.CSGMesh;
import com.winteralexander.gdx.csg.CSGUtil;
import com.winteralexander.gdx.csg.test.debugviewer.CSGMeshViewer;
import com.winteralexander.gdx.csg.test.debugviewer.TriangleViewer;
import com.winteralexander.gdx.utils.collection.CollectionUtil;
import com.winteralexander.gdx.utils.math.shape3d.Intersector3D;
import com.winteralexander.gdx.utils.math.shape3d.SegmentPlus;
import com.winteralexander.gdx.utils.math.shape3d.Triangle;
import org.junit.Test;

import static com.winteralexander.gdx.utils.math.shape3d.Intersector3D.intersectTriangleTriangle;
import static org.junit.Assert.assertEquals;

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
		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder);
		csgMesh.getVertices().forEach(v -> v.getPosition().add(0f, 0f, -0.5f));

		int idxStart = partBuilder.getNumVertices();
		CylinderShapeBuilder.build(partBuilder, 0.05f, 0.02f, 0.05f, 10);

		CSGMesh substrahend = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(idxStart, partBuilder.getNumVertices()));
		substrahend.getVertices().forEach(v -> v.getPosition().add(-0.05f, 0f, -0.5f));

		CSGUtil.subtraction(csgMesh, substrahend);
		// test is to ensure this doesn't crash
	}
}
