package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;

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

	private final VertexAttributes attributes;

	private final ObjectIntMap<MeshVertex> vertexIndices = new ObjectIntMap<>();

	public CSGMesh(Array<MeshVertex> vertices,
	               Array<MeshFace> faces,
	               VertexAttributes attributes) {
		ensureNotNull(vertices, "vertices");
		ensureNotNull(faces, "faces");
		ensureNotNull(attributes, "attributes");
		this.vertices = vertices;
		this.faces = faces;
		this.attributes = attributes;
	}

	public Array<MeshVertex> getVertices() {
		return vertices;
	}

	public Array<MeshFace> getFaces() {
		return faces;
	}

	public Mesh toMesh() {
		Mesh mesh = new Mesh(true, vertices.size, faces.size * 3, attributes);

		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(true);

		int vertexSize = mesh.getVertexSize() / 4;
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset;

		buffer.limit(vertices.size * vertexSize);
		idxBuffer.limit(faces.size * 3);

		vertexIndices.clear();
		for(int i = 0; i < vertices.size; i++) {
			MeshVertex vertex = vertices.get(i);
			buffer.position(i * vertexSize + posOffset);
			buffer.put(vertex.getPosition().x);
			buffer.put(vertex.getPosition().y);
			buffer.put(vertex.getPosition().z);
			vertexIndices.put(vertex, i);
		}

		for(int i = 0; i < faces.size; i++) {
			MeshFace face = faces.get(i);
			idxBuffer.position(i * 3);

			int idx1 = vertexIndices.get(face.getV1(), -1);
			int idx2 = vertexIndices.get(face.getV2(), -1);
			int idx3 = vertexIndices.get(face.getV3(), -1);

			if(idx1 == -1 || idx2 == -1 || idx3 == -1)
				throw new IllegalStateException("CSGMesh has a face refering to a vertex not in " +
						"the mesh. Face #" + i + " has vertices " +
						"#" + idx1 + ", #" + idx2 + " and #" + idx3);

			idxBuffer.put((short)idx1);
			idxBuffer.put((short)idx2);
			idxBuffer.put((short)idx3);
		}
		vertexIndices.clear();

		return mesh;
	}

	public static CSGMesh fromMesh(Mesh mesh) {
		Array<MeshVertex> vertices = new Array<>(mesh.getNumVertices());
		Array<MeshFace> faces = new Array<>(mesh.getNumIndices());

		FloatBuffer buffer = mesh.getVerticesBuffer(false);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(false);

		int vertexSize = mesh.getVertexSize() / 4;
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset;

		for(int i = 0; i < mesh.getNumVertices(); i++) {
			MeshVertex vertex = new MeshVertex();
			vertex.getPosition().set(buffer.get(i * vertexSize + posOffset),
					buffer.get(i * vertexSize + posOffset + 1),
					buffer.get(i * vertexSize + posOffset + 2));
			vertices.add(vertex);
		}

		for(int i = 0; i < mesh.getNumIndices() / 3; i++) {
			short v1 = idxBuffer.get(i * 3);
			short v2 = idxBuffer.get(i * 3 + 1);
			short v3 = idxBuffer.get(i * 3 + 2);
			MeshFace face = new MeshFace(vertices.get(v1),
					vertices.get(v2),
					vertices.get(v3));
			faces.add(face);
		}

		return new CSGMesh(vertices, faces, mesh.getVertexAttributes());
	}
}
