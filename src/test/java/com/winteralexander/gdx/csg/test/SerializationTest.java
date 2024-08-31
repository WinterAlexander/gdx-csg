package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.winteralexander.gdx.csg.CSGMesh;
import com.winteralexander.gdx.csg.MeshFace;
import com.winteralexander.gdx.csg.MeshVertex;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Tests the ability of a {@link CSGMesh} to be serialized
 * <p>
 * Created on 2024-08-30.
 *
 * @author Alexander Winter
 */
public class SerializationTest {
	@Test
	public void testSerialization() throws IOException {
		CSGMesh mesh = new CSGMesh();

		mesh.setAttributes(new VertexAttributes(VertexAttribute.Position()));

		mesh.getVertices().add(new MeshVertex(new Vector3(0f, 0f, 0f), new Vector3(0f, 1f, 0f), new Vector3(1f, 0f, 0f), new float[0]));
		mesh.getVertices().add(new MeshVertex(new Vector3(1f, 0f, 0f), new Vector3(0f, 1f, 0f), new Vector3(1f, 0f, 0f), new float[0]));
		mesh.getVertices().add(new MeshVertex(new Vector3(0f, 0f, 1f), new Vector3(0f, 1f, 0f), new Vector3(1f, 0f, 0f), new float[0]));

		mesh.getFaces().add(new MeshFace(mesh.getVertices().get(0),
				mesh.getVertices().get(1),
				mesh.getVertices().get(2)));

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		mesh.writeTo(outputStream);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

		CSGMesh other = new CSGMesh();

		other.readFrom(inputStream);

		assertEquals(mesh.getVertices().size, other.getVertices().size);
		assertEquals(mesh.getFaces().size, other.getFaces().size);
	}
}
