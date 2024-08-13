package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult;
import com.winteralexander.gdx.utils.math.VectorUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.*;
import static com.winteralexander.gdx.csg.IntersectorPlus.intersectTriangleRay;
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
	private final ObjectMap<MeshVertex, InsideStatus> vertexStatus = new ObjectMap<>();

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

	private final Ray tmpRay = new Ray();
	private final SegmentPlus tmpSegment = new SegmentPlus();

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

	public void splitTriangles(CSGMesh other) {
		int initialSize = faces.size;
		for(int i = 0; i < faces.size; i++) {
			for(MeshFace otherFace : other.faces) {
				TriangleIntersectionResult result = intersectTriangleTriangle(faces.get(i).getTriangle(),
						otherFace.getTriangle(), 0f, intersectSegment);
				if(result == NONCOPLANAR_FACE_FACE) {
					plane.set(otherFace.getPosition1(), otherFace.getNormal());
					splitFace(i, plane);
				}
			}
		}
	}

	private void splitFace(int faceIndex, Plane plane) {
		MeshFace face = faces.get(faceIndex);
		face.getTriangle().toArray(tmpArray);
		Intersector.splitTriangle(tmpArray, plane, splitTriangle);

		tmpNewVertices.clear();
		for(int i = 0; i < splitTriangle.numBack; i++) {
			processSplitTriangle(face, splitTriangle.back, i * 9);
		}

		for(int i = 0; i < splitTriangle.numFront; i++) {
			processSplitTriangle(face, splitTriangle.front, i * 9);
		}
		faces.set(faceIndex, toAdd.get(0));
		faces.addAll(toAdd, 1, toAdd.size - 1);
		toAdd.clear();
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

	public void classifyFaces(CSGMesh other) {
		vertexStatus.clear();
		for(MeshVertex vertex : vertices)
			vertexStatus.put(vertex, other.getStatus(vertex.getPosition()));
	}

	public InsideStatus getStatus(Vector3 position) {
		int countIntersect = 0;
		tmpRay.set(position.x, position.y, position.z, 0f, 1f, 0f);

		for(MeshFace face : faces) {
			if(!intersectTriangleRay(face.getTriangle(), tmpRay, 1e-5f, tmpSegment))
				continue;

			if(!tmpSegment.a.epsilonEquals(tmpSegment.b, 1e-5f))
				continue;

			float t = tmpRay.direction.dot(tmpSegment.a.x - tmpRay.origin.x,
					tmpSegment.a.y - tmpRay.origin.y,
					tmpSegment.a.z - tmpRay.origin.z);

			if(Math.abs(t) < 1e-5f)
				return InsideStatus.BOUNDARY;

			if(t < 0f)
				continue;

			countIntersect++;
		}
		return countIntersect % 2 == 0 ? InsideStatus.OUTSIDE : InsideStatus.INSIDE;
	}

	public void removeFaces(boolean inside) {
		for(MeshFace face : faces) {
			InsideStatus status1 = vertexStatus.get(face.getV1());
			InsideStatus status2 = vertexStatus.get(face.getV2());
			InsideStatus status3 = vertexStatus.get(face.getV3());

			if(status1 == null || status2 == null || status3 == null)
				throw new IllegalStateException("Some vertices are not classified");

			boolean isFaceInside = status1 == InsideStatus.INSIDE
					|| status2 == InsideStatus.INSIDE
					|| status3 == InsideStatus.INSIDE;

			boolean isFaceOutside = status1 == InsideStatus.OUTSIDE
					|| status2 == InsideStatus.OUTSIDE
					|| status3 == InsideStatus.OUTSIDE;

			if(isFaceInside && isFaceOutside)
				throw new IllegalStateException("Failure to split face");

			if(isFaceInside == inside)
				toRemove.add(face);
		}
		faces.removeAll(toRemove, true);
		toRemove.clear();
	}

	public InsideStatus getInsideStatus(MeshVertex vertex) {
		return vertexStatus.get(vertex);
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

	public enum InsideStatus {
		INSIDE, BOUNDARY, OUTSIDE
	}
}
