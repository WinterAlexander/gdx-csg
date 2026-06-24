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
import com.winteralexander.gdx.csg.test.debugviewer.ModelViewer;
import com.winteralexander.gdx.utils.collection.CollectionUtil;
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

		int firstBoxStart = partBuilder.getNumVertices();
		BoxShapeBuilder.build(partBuilder, 1f, 1f, 1f);
		int firstBoxEnd = partBuilder.getNumVertices();

		partBuilder.setVertexTransform(new Matrix4().setToTranslation(0f, 1f, -1f));
		SphereShapeBuilder.build(partBuilder, 1f, 1f, 1f, 5, 5);
		partBuilder.setVertexTransform(null);

		IntArray indices = CollectionUtil.arrayFromRange(firstBoxStart, firstBoxEnd);
		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder, indices);

		partBuilder.setVertexTransform(new Matrix4().setToTranslation(0f, 1f, 1f));
		SphereShapeBuilder.build(partBuilder, 1f, 1f, 1f, 5, 5);
		partBuilder.setVertexTransform(null);
		sphereVertexCount += partBuilder.getNumVertices() - firstBoxEnd;

		int secondBoxStart = partBuilder.getNumVertices();
		BoxShapeBuilder.build(partBuilder, 1.5f, 0.5f, 0.5f);
		int secondBoxEnd = partBuilder.getNumVertices();

		partBuilder.setVertexTransform(new Matrix4().setToTranslation(-1f, 1f, 0f));
		SphereShapeBuilder.build(partBuilder, 1f, 1f, 1f, 5, 5);
		partBuilder.setVertexTransform(null);
		sphereVertexCount += partBuilder.getNumVertices() - secondBoxEnd;

		indices.clear();
		CollectionUtil.fillFromRange(secondBoxStart, secondBoxEnd, indices);

		CSGMesh second = CSGMesh.fromBuilder(partBuilder, indices);
		CSGMesh result = CSGUtil.subtraction(csgMesh, second);

		indices.clear();
		CollectionUtil.fillFromRange(firstBoxStart, firstBoxEnd, indices);
		CollectionUtil.fillFromRange(secondBoxStart, secondBoxEnd, indices);

		result.getVertices().forEach(v -> v.getPosition().add(0f, 3f, 0f));
		result.toBuilder(partBuilder, indices);

		// validates it properly deleted the old vertices
		assertEquals(sphereVertexCount + 56, partBuilder.getNumVertices());
	}
}
