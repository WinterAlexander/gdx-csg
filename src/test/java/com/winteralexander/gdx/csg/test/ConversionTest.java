package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntArray;
import com.winteralexander.gdx.csg.CSGMesh;
import com.winteralexander.gdx.csg.CSGUtil;
import com.winteralexander.gdx.csg.test.debugviewer.CSGMeshViewer;
import com.winteralexander.gdx.csg.test.debugviewer.ModelViewer;
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
		MeshBuilder partBuilder = (MeshBuilder)builder.part("meshPart",
				GL20.GL_TRIANGLES,
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
	public void inplaceSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("meshPart",
				GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal,
				new Material());

		partBuilder.setVertexTransform(new Matrix4().setToTranslation(1f, 1f, 0f));
		SphereShapeBuilder.build(partBuilder, 1f, 1f, 1f, 5, 5);
		partBuilder.setVertexTransform(null);
		int sphereVertexCount = partBuilder.getNumVertices();
		BoxShapeBuilder.build(partBuilder, 1f, 1f, 1f);
		IntArray indices = new IntArray();
		for(int i = sphereVertexCount; i < partBuilder.getNumVertices(); i++)
			indices.add(i);
		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder, indices);

		BoxShapeBuilder.build(partBuilder, 1.5f, 0.5f, 0.5f);

		int firstPartSize = indices.size;
		indices.clear();
		for(int i = sphereVertexCount + firstPartSize; i < partBuilder.getNumVertices(); i++)
			indices.add(i);
		CSGMesh second = CSGMesh.fromBuilder(partBuilder, indices);
		CSGMesh result = CSGUtil.subtraction(csgMesh, second);
		CSGMeshViewer.start(result);

		indices.clear();
		for(int i = sphereVertexCount; i < partBuilder.getNumVertices(); i++)
			indices.add(i);
		result.toBuilder(partBuilder, indices);

		// validates it properly deleted the old vertices
		assertEquals(sphereVertexCount + 56, partBuilder.getNumVertices());
		ModelViewer.start(builder);
	}
}
