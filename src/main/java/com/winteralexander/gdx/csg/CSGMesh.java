package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult;
import com.winteralexander.gdx.utils.math.VectorUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.NONCOPLANAR_FACE_FACE;
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

	private final FloatArray tmpFaceIntersections = new FloatArray();

	private final Array<MeshFace> toRemove = new Array<>();
	private final Array<MeshFace> toAdd = new Array<>();
	private final Array<MeshVertex> tmpNewVertices = new Array<>();

	private final Ray tmpRay = new Ray();
	private final SegmentPlus tmpSegment = new SegmentPlus();

	public float tolerance = 1e-5f;

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

	/**
	 * Merges this CSG mesh with the provided CSGMesh. Does not perform any deep copying of the
	 * vertices or faces, if that is needed, copy the provided {@link CSGMesh} beforehand.
	 *
	 * @param other mesh to merge
	 */
	public void mergeWith(CSGMesh other) {
		this.vertices.addAll(other.vertices);
		this.faces.addAll(other.faces);
	}

	public void splitTriangles(CSGMesh other) {
		tmpNewVertices.clear();
		for(int i = 0; i < faces.size; i++) {
			for(MeshFace otherFace : other.faces) {
				TriangleIntersectionResult result = intersectTriangleTriangle(faces.get(i).getTriangle(),
						otherFace.getTriangle(), tolerance, intersectSegment);
				if(result == NONCOPLANAR_FACE_FACE) {
					plane.set(otherFace.getPosition1(), otherFace.getNormal());
					splitFace(i, plane);
				}
			}
		}
		tmpNewVertices.clear();
	}

	private void splitFace(int faceIndex, Plane plane) {
		MeshFace face = faces.get(faceIndex);
		face.getTriangle().toArray(tmpArray);
		Intersector.splitTriangle(tmpArray, plane, splitTriangle);

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

		if(tmpV1.epsilonEquals(tmpV2, tolerance)
		|| tmpV1.epsilonEquals(tmpV3, tolerance)
		|| tmpV2.epsilonEquals(tmpV3, tolerance))
			return;
			//throw new IllegalStateException("Triangle has duplicate points");

		MeshVertex vertex1 = null, vertex2 = null, vertex3 = null;

		for(MeshVertex faceVertex : face.getVertices()) {
			if(faceVertex.getPosition().epsilonEquals(tmpV1, tolerance))
				vertex1 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV2, tolerance))
				vertex2 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV3, tolerance))
				vertex3 = faceVertex;
		}

		for(MeshVertex addedVertex : tmpNewVertices) {
			if(addedVertex.getPosition().epsilonEquals(tmpV1, tolerance))
				vertex1 = addedVertex;
			if(addedVertex.getPosition().epsilonEquals(tmpV2, tolerance))
				vertex2 = addedVertex;
			if(addedVertex.getPosition().epsilonEquals(tmpV3, tolerance))
				vertex3 = addedVertex;
		}

		if(vertex1 == null) {
			vertex1 = new MeshVertex();
			vertex1.getPosition().set(tmpV1);
			vertex1.getNormal().set(face.getNormal());
			tmpNewVertices.add(vertex1);
			vertices.add(vertex1);
		}

		if(vertex2 == null) {
			vertex2 = new MeshVertex();
			vertex2.getPosition().set(tmpV2);
			vertex2.getNormal().set(face.getNormal());
			tmpNewVertices.add(vertex2);
			vertices.add(vertex2);
		}

		if(vertex3 == null) {
			vertex3 = new MeshVertex();
			vertex3.getPosition().set(tmpV3);
			vertex3.getNormal().set(face.getNormal());
			tmpNewVertices.add(vertex3);
			vertices.add(vertex3);
		}

		MeshFace newFace = new MeshFace(vertex1, vertex2, vertex3);
		toAdd.add(newFace);
	}

	public void classifyFaces(CSGMesh other) {
		vertexStatus.clear();
		for(MeshVertex vertex : vertices)
			vertexStatus.put(vertex, other.computeInsideStatus(vertex.getPosition()));
	}

	/**
	 * Computes the {@link InsideStatus} of a given position using ray intersections with the faces
	 * of this mesh
	 *
	 * @param position position to check
	 * @return inside, outside or on the boundary
	 */
	public InsideStatus computeInsideStatus(Vector3 position) {
		int countIntersect = 0;
		tmpRay.set(position.x, position.y, position.z, 0f, 1f, 0f);
		tmpFaceIntersections.clear();

		faceLoop:
		for(MeshFace face : faces) {
			if(!intersectTriangleRay(face.getTriangle(), tmpRay, tolerance, tmpSegment))
				continue;

			float t = tmpRay.direction.dot(tmpSegment.a.x - tmpRay.origin.x,
					tmpSegment.a.y - tmpRay.origin.y,
					tmpSegment.a.z - tmpRay.origin.z);

			if(Math.abs(tmpRay.direction.dot(face.getNormal())) <= tolerance) {
				float t2 = tmpRay.direction.dot(tmpSegment.b.x - tmpRay.origin.x,
						tmpSegment.b.y - tmpRay.origin.y,
						tmpSegment.b.z - tmpRay.origin.z);

				if(Math.min(t, t2) < tolerance && Math.max(t, t2) > -tolerance)
					return InsideStatus.BOUNDARY;

				continue;
			}

			if(Math.abs(t) < tolerance)
				return InsideStatus.BOUNDARY;

			if(t < 0f)
				continue;

			for(int i = 0; i < tmpFaceIntersections.size; i++)
				if(Math.abs(t - tmpFaceIntersections.get(i)) <= tolerance)
					continue faceLoop;

			tmpFaceIntersections.add(t);
			countIntersect++;
		}
		tmpFaceIntersections.clear();
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
					|| status3 == InsideStatus.INSIDE
					|| status1 == InsideStatus.BOUNDARY
					&& status2 == InsideStatus.BOUNDARY
					&& status3 == InsideStatus.BOUNDARY;

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

	public void invertTriangles() {
		for(MeshFace face : faces) {
			MeshVertex v3 = face.getVertices()[2];
			face.getVertices()[2] = face.getVertices()[1];
			face.getVertices()[1] = v3;
		}

		for(MeshVertex vertex : vertices)
			vertex.getNormal().scl(-1f);
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

	public CSGMesh cpy() {
		Array<MeshVertex> verts = new Array<>();
		Array<MeshFace> faces = new Array<>();

		vertexIndices.clear();
		int i = 0;
		for(MeshVertex v : vertices) {
			verts.add(new MeshVertex(v));
			vertexIndices.put(v, i);
			i++;
		}

		for(MeshFace f : this.faces)
			faces.add(new MeshFace(verts.get(vertexIndices.get(f.getV1(), -1)),
					verts.get(vertexIndices.get(f.getV2(), -1)),
					verts.get(vertexIndices.get(f.getV3(), -1))));

		return new CSGMesh(verts, faces, attributes);
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
