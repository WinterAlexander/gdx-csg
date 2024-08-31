package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglFileHandle;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Vector3;
import com.winteralexander.gdx.csg.*;
import com.winteralexander.gdx.csg.test.debugviewer.CSGMeshViewer;
import org.junit.Ignore;
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

	@Test
	@Ignore
	public void testSubtractionFromSerialized() throws IOException {
		CSGMesh minuend = new CSGMesh();
		minuend.readFrom(new LwjglFileHandle("minuend.csgmesh", Files.FileType.Internal).read());
		CSGMesh subtrahend = new CSGMesh();
		subtrahend.readFrom(new LwjglFileHandle("subtrahend.csgmesh", Files.FileType.Internal).read());

		CSGMesh copy1 = minuend.cpy();
		CSGMesh copy2 = subtrahend.cpy();
		copy1.setConfig(CSGConfiguration.DEFAULT);
		copy2.setConfig(CSGConfiguration.DEFAULT);

		copy1.splitTriangles(subtrahend);
		copy2.splitTriangles(minuend);

		copy1.classifyFaces(subtrahend);
		copy2.classifyFaces(minuend);

		CSGMeshViewer.start(copy1, copy2);

		copy1.removeFaces(true, true);
		copy2.removeFaces(false, true);

		copy2.invertTriangles();
		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		CSGMeshViewer.start(copy1);
	}

}
