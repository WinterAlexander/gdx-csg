package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.Array;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * A mesh for CSG operation. A {@link CSGMesh} can be built from a {@link Mesh} and then can
 * generate a {@link Mesh} back once the CSG operations are completed
 * <p>
 * Created on 2024-08-11.
 *
 * @author Alexander Winter
 */
public class CSGMesh {
	private final Array<MeshVertex> vertices;
	private final Array<MeshFace> faces;

	public CSGMesh() {
		this(new Array<>(), new Array<>());
	}

	public CSGMesh(Array<MeshVertex> vertices, Array<MeshFace> faces) {
		ensureNotNull(vertices, "vertices");
		ensureNotNull(faces, "faces");
		this.vertices = vertices;
		this.faces = faces;
	}

	public Array<MeshVertex> getVertices() {
		return vertices;
	}

	public Array<MeshFace> getFaces() {
		return faces;
	}

	public Mesh toMesh() {
		return null;
	}

	public static CSGMesh fromMesh(Mesh mesh) {
		Array<MeshVertex> vertices = new Array<>(mesh.getNumVertices());
		Array<MeshFace> faces = new Array<>(mesh.getNumIndices());

		FloatBuffer buffer = mesh.getVerticesBuffer(false);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(false);

		int vertexSize = mesh.getVertexSize();
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset;

		for(int i = 0; i < vertices.size; i++) {
			MeshVertex vertex = new MeshVertex();
			vertex.getPosition().set(buffer.get(i * vertexSize + posOffset),
					buffer.get(i * vertexSize + posOffset + 1),
					buffer.get(i * vertexSize + posOffset + 2));
			vertices.add(vertex);
		}

		for(int i = 0; i < faces.size; i++) {
			short v1 = idxBuffer.get(i * 3);
			short v2 = idxBuffer.get(i * 3 + 1);
			short v3 = idxBuffer.get(i * 3 + 2);
			MeshFace face = new MeshFace(vertices.get(v1), vertices.get(v2), vertices.get(v3));
			faces.add(face);
		}

		return new CSGMesh(vertices, faces);
	}
}
