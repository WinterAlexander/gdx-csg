package com.winteralexander.gdx.csg.test;

import com.winteralexander.gdx.csg.CSGMesh;
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


		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		mesh.writeTo(outputStream);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());

		CSGMesh other = new CSGMesh();

		other.readFrom(inputStream);

		//assertEquals(mesh.tolerance)
	}
}
