package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult;
import com.winteralexander.gdx.utils.math.VectorUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.COPLANAR_FACE_FACE;
import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.NONE;
import static com.winteralexander.gdx.csg.IntersectorPlus.intersectTriangleTriangle;
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

	private final SegmentPlus intersectSegment = new SegmentPlus();
	private final Plane plane = new Plane();

	private final float[] tmpArray = new float[9];
	private final Intersector.SplitTriangle splitTriangle = new Intersector.SplitTriangle(3);

	private final Vector3 tmpV1 = new Vector3(),
			tmpV2 = new Vector3(),
			tmpV3 = new Vector3();

	private final Array<MeshFace> toRemove = new Array<>();
	private final Array<MeshFace> toAdd = new Array<>();
	private final Array<MeshVertex> tmpNewVertices = new Array<>();

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

	public void splitTriangles(CSGMesh other, FaceSplittingOperation operation) {
		for(MeshFace face : faces) {
			for(MeshFace otherFace : other.faces) {
				TriangleIntersectionResult result = intersectTriangleTriangle(face.getTriangle(),
						otherFace.getTriangle(), 1e-5f, intersectSegment);
				if(result != NONE && result != COPLANAR_FACE_FACE) {
					plane.set(otherFace.getPosition1(), otherFace.getNormal());
					splitFace(face, plane, operation);
				}
			}
		}
		faces.removeAll(toRemove, true);
		faces.addAll(toAdd);
		toRemove.clear();
		toAdd.clear();
	}

	public void splitFace(MeshFace face, Plane plane, FaceSplittingOperation operation) {
		face.getTriangle().toArray(tmpArray);
		Intersector.splitTriangle(tmpArray, plane, splitTriangle);
		toRemove.add(face);

		if(operation == FaceSplittingOperation.KEEP_BACK
		|| operation == FaceSplittingOperation.KEEP_BOTH)
			for(int i = 0; i < splitTriangle.numBack; i++) {
				processSplitTriangle(face, splitTriangle.back, i * 9);
			}

		if(operation == FaceSplittingOperation.KEEP_FRONT
		|| operation == FaceSplittingOperation.KEEP_BOTH)
			for(int i = 0; i < splitTriangle.numFront; i++) {
				processSplitTriangle(face, splitTriangle.front, i * 9);
			}
	}

	private void processSplitTriangle(MeshFace face, float[] array, int offset) {
		VectorUtil.setFromArray(tmpV1, array, offset);
		VectorUtil.setFromArray(tmpV2, array, offset + 3);
		VectorUtil.setFromArray(tmpV3, array, offset + 6);

		MeshVertex vertex1 = null, vertex2 = null, vertex3 = null;

		for(MeshVertex faceVertex : face.getVertices()) {
			if(faceVertex.getPosition().epsilonEquals(tmpV1, 1e-5f))
				vertex1 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV2, 1e-5f))
				vertex2 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV3, 1e-5f))
				vertex3 = faceVertex;
		}

		for(MeshVertex addedVertex : tmpNewVertices) {
			if(addedVertex.getPosition().epsilonEquals(tmpV1, 1e-5f))
				vertex1 = addedVertex;
			if(addedVertex.getPosition().epsilonEquals(tmpV2, 1e-5f))
				vertex2 = addedVertex;
			if(addedVertex.getPosition().epsilonEquals(tmpV3, 1e-5f))
				vertex3 = addedVertex;
		}

		if(vertex1 == null) {
			vertex1 = new MeshVertex();
			vertex1.getPosition().set(tmpV1);
			vertex1.getNormal().set(face.getNormal());
			tmpNewVertices.add(vertex1);
		}

		if(vertex2 == null) {
			vertex2 = new MeshVertex();
			vertex2.getPosition().set(tmpV2);
			vertex2.getNormal().set(face.getNormal());
			tmpNewVertices.add(vertex2);
		}

		if(vertex3 == null) {
			vertex3 = new MeshVertex();
			vertex3.getPosition().set(tmpV3);
			vertex3.getNormal().set(face.getNormal());
			tmpNewVertices.add(vertex3);
		}

		vertices.addAll(tmpNewVertices);

		MeshFace newFace = new MeshFace(vertex1, vertex2, vertex3);
		toAdd.add(newFace);
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
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		int norOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Normal).offset / 4;

		buffer.limit(vertices.size * vertexSize);
		idxBuffer.limit(faces.size * 3);

		vertexIndices.clear();
		for(int i = 0; i < vertices.size; i++) {
			MeshVertex vertex = vertices.get(i);
			buffer.position(i * vertexSize + posOffset);
			buffer.put(vertex.getPosition().x);
			buffer.put(vertex.getPosition().y);
			buffer.put(vertex.getPosition().z);
			buffer.position(i * vertexSize + norOffset);
			buffer.put(vertex.getNormal().x);
			buffer.put(vertex.getNormal().y);
			buffer.put(vertex.getNormal().z);
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
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		int norOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Normal).offset / 4;

		for(int i = 0; i < mesh.getNumVertices(); i++) {
			MeshVertex vertex = new MeshVertex();
			vertex.getPosition().set(buffer.get(i * vertexSize + posOffset),
					buffer.get(i * vertexSize + posOffset + 1),
					buffer.get(i * vertexSize + posOffset + 2));
			vertex.getNormal().set(buffer.get(i * vertexSize + norOffset),
					buffer.get(i * vertexSize + norOffset + 1),
					buffer.get(i * vertexSize + norOffset + 2));
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

	public enum FaceSplittingOperation {
		KEEP_BACK, KEEP_FRONT, KEEP_BOTH
	}
}
