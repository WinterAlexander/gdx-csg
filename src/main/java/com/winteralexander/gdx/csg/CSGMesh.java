package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;
import com.badlogic.gdx.utils.*;
import com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult;
import com.winteralexander.gdx.utils.io.Serializable;
import com.winteralexander.gdx.utils.math.VectorUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;
import static com.winteralexander.gdx.csg.IntersectorPlus.LineIntersectionResult.COLLINEAR;
import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.COPLANAR_FACE_FACE;
import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.NONCOPLANAR_FACE_FACE;
import static com.winteralexander.gdx.csg.IntersectorPlus.intersectTriangleRay;
import static com.winteralexander.gdx.csg.IntersectorPlus.intersectTriangleTriangle;
import static com.winteralexander.gdx.utils.Validation.ensureNotNull;
import static com.winteralexander.gdx.utils.io.SerializationUtil.readVec3;
import static com.winteralexander.gdx.utils.io.SerializationUtil.writeVec3;
import static com.winteralexander.gdx.utils.io.StreamUtil.*;

/**
 * A mesh for CSG operation. A {@link CSGMesh} can be built from a {@link Mesh} and then can
 * generate a {@link Mesh} back once the CSG operations are completed
 * <p>
 * Created on 2024-08-11.
 *
 * @author Alexander Winter
 */
public class CSGMesh implements Serializable {
	private final Array<MeshVertex> vertices;
	private final Array<MeshFace> faces;

	private VertexAttributes attributes;

	private final ObjectIntMap<MeshVertex> vertexIndices = new ObjectIntMap<>();
	private final ObjectMap<MeshVertex, InsideStatus> vertexStatus = new ObjectMap<>();
	private final ObjectSet<MeshVertex> usedVertices = new ObjectSet<>();
	// list of faces which intersect in a coplanar way
	private final HashSet<MeshFace> boundaryFaces = new HashSet<>();
	private final Array<Segment> cutEdges = new Array<>();

	private final SegmentPlus intersectSegment = new SegmentPlus();
	private final Plane plane = new Plane();

	private final float[] tmpArray = new float[9];
	private final Intersector.SplitTriangle splitTriangle = new Intersector.SplitTriangle(3);

	private final Vector3 tmpV1 = new Vector3(),
			tmpV2 = new Vector3(),
			tmpV3 = new Vector3();
	private final Vector3 tmpSegmentIntersection = new Vector3();

	private final Array<MeshFace> toRemove = new Array<>();
	private final Array<MeshFace> toAdd = new Array<>();
	private final HashMap<MeshVertex, MeshFace> tmpNewVertices = new HashMap<>();

	private final Ray tmpRay = new Ray();
	private final SegmentPlus tmpSegment = new SegmentPlus();

	private CSGConfiguration config = CSGConfiguration.DEFAULT;

	public CSGMesh() {
		this(new Array<>(),
				new Array<>(),
				null);
	}

	public CSGMesh(Array<MeshVertex> vertices,
	               Array<MeshFace> faces,
	               VertexAttributes attributes) {
		ensureNotNull(vertices, "vertices");
		ensureNotNull(faces, "faces");
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
		boundaryFaces.clear();
		for(int i = 0; i < faces.size; i++) {
			for(MeshFace otherFace : other.faces) {
				MeshFace face = faces.get(i);
				TriangleIntersectionResult result = intersectTriangleTriangle(face.getTriangle(),
						otherFace.getTriangle(), config.tolerance, intersectSegment);
				if(result == NONCOPLANAR_FACE_FACE) {
					cutEdges.add(intersectSegment.cpy());
					plane.set(otherFace.getPosition1(), otherFace.getNormal());
					splitFace(i, plane);

					if(boundaryFaces.contains(face)) {
						boundaryFaces.remove(face);
						i--;
						break; // go back to determine again if the face that was just split is a
						// boundary face
					}

				} else if(result == COPLANAR_FACE_FACE) {
					if(config.enableBoundaryFaces)
						boundaryFaces.add(face);
				}
			}
		}
		tmpNewVertices.clear();

		for(int j = 0; j < 10; j++)
			for(int i = 0; i < faces.size; i++) {
				checkForMergeWithNeighbors(faces.get(i));
			}
	}

	private void splitFace(int faceIndex, Plane plane) {
		MeshFace face = faces.get(faceIndex);
		face.getTriangle().toArray(tmpArray);
		Intersector.splitTriangle(tmpArray, plane, splitTriangle);

		for(int i = 0; i < splitTriangle.numBack; i++)
			processSplitTriangle(face, splitTriangle.back, i * 9);

		for(int i = 0; i < splitTriangle.numFront; i++)
			processSplitTriangle(face, splitTriangle.front, i * 9);
		faces.set(faceIndex, toAdd.get(0));
		faces.addAll(toAdd, 1, toAdd.size - 1);

		//for(MeshFace newFace : toAdd)
		//	checkForMergeWithNeighbors(newFace);
		toAdd.clear();
	}

	private void checkForMergeWithNeighbors(MeshFace face) {
		if(!config.enableMerging)
			return;

		faceLoop:
		for(int i = 0; i < faces.size; i++) {
			MeshFace current = faces.get(i);

			if(current == face)
				continue;

			int countMatching = 0;
			MeshVertex firstMatch = null, secondMatch = null;
			MeshVertex nonMatchingA = null, nonMatchingB = null;

			if(face.getV1() == current.getV1()
			|| face.getV1() == current.getV2()
			|| face.getV1() == current.getV3()) {
				countMatching++;
				firstMatch = face.getV1();
			} else
				nonMatchingA = face.getV1();

			if(face.getV2() == current.getV1()
			|| face.getV2() == current.getV2()
			|| face.getV2() == current.getV3()) {
				countMatching++;
				if(firstMatch == null)
					firstMatch = face.getV2();
				else
					secondMatch = face.getV2();
			} else
				nonMatchingA = face.getV2();

			if(face.getV3() == current.getV1()
			|| face.getV3() == current.getV2()
			|| face.getV3() == current.getV3()) {
				countMatching++;
				if(firstMatch == null)
					firstMatch = face.getV3();
				else
					secondMatch = face.getV3();
			} else
				nonMatchingA = face.getV3();

			if(countMatching == 3)
				continue;
				//throw new IllegalStateException("Duplicate triangles in mesh");

			if(countMatching != 2)
				continue;

			if(current.getV1() != firstMatch && current.getV1() != secondMatch)
				nonMatchingB = current.getV1();
			else if(current.getV2() != firstMatch && current.getV2() != secondMatch)
				nonMatchingB = current.getV2();
			else if(current.getV3() != firstMatch && current.getV3() != secondMatch)
				nonMatchingB = current.getV3();

			boolean collinearWithFirst = IntersectorPlus.intersectSegmentSegment(
					nonMatchingA.getPosition(), nonMatchingB.getPosition(),
					nonMatchingA.getPosition(), firstMatch.getPosition(),
					config.tolerance, tmpSegmentIntersection) == COLLINEAR;

			boolean collinearWithSecond = IntersectorPlus.intersectSegmentSegment(
					nonMatchingA.getPosition(), nonMatchingB.getPosition(),
					nonMatchingA.getPosition(), secondMatch.getPosition(),
					config.tolerance, tmpSegmentIntersection) == COLLINEAR;

			if(!collinearWithFirst && !collinearWithSecond)
				continue;

			if(collinearWithFirst && collinearWithSecond)
				continue;
				//throw new IllegalStateException("Invalid 2 faces");

			for(Segment segment : cutEdges)
				if(IntersectorPlus.intersectSegmentSegment(segment.a, segment.b,
						firstMatch.getPosition(), secondMatch.getPosition(),
						config.tolerance, tmpSegmentIntersection) == COLLINEAR) {
					continue faceLoop;
				}

			if(collinearWithFirst) {
				face.getVertices()[0] = nonMatchingA;
				face.getVertices()[1] = secondMatch;
				face.getVertices()[2] = nonMatchingB;

				if(face.getNormal().dot(current.getNormal()) < 0f) {
					face.getVertices()[1] = nonMatchingB;
					face.getVertices()[2] = secondMatch;
				}
			} else {
				face.getVertices()[0] = nonMatchingA;
				face.getVertices()[1] = firstMatch;
				face.getVertices()[2] = nonMatchingB;

				if(face.getNormal().dot(current.getNormal()) < 0f) {
					face.getVertices()[1] = nonMatchingB;
					face.getVertices()[2] = firstMatch;
				}
			}

			faces.removeIndex(i);
			checkForMergeWithNeighbors(face);
			return;
		}
	}

	private void interpolate(MeshVertex out,
	                         MeshVertex v1, float w1,
	                         MeshVertex v2, float w2,
	                         MeshVertex v3, float w3) {
		out.getNormal().set(0f, 0f, 0f)
				.mulAdd(v1.getNormal(), w1)
				.mulAdd(v2.getNormal(), w2)
				.mulAdd(v3.getNormal(), w3)
				.nor();

		out.getTangent().set(0f, 0f, 0f)
				.mulAdd(v1.getTangent(), w1)
				.mulAdd(v2.getTangent(), w2)
				.mulAdd(v3.getTangent(), w3)
				.nor();

		for(int i = 0; i < out.getOtherAttributes().length; i++)
			out.getOtherAttributes()[i] = v1.getOtherAttributes()[i] * w1 +
					v2.getOtherAttributes()[i] * w2 +
					v3.getOtherAttributes()[i] * w3;
	}

	private void processSplitTriangle(MeshFace face, float[] array, int offset) {
		VectorUtil.setFromArray(tmpV1, array, offset);
		VectorUtil.setFromArray(tmpV2, array, offset + 3);
		VectorUtil.setFromArray(tmpV3, array, offset + 6);

		if(tmpV1.epsilonEquals(tmpV2, config.tolerance)
		|| tmpV1.epsilonEquals(tmpV3, config.tolerance)
		|| tmpV2.epsilonEquals(tmpV3, config.tolerance))
			return;
			//throw new IllegalStateException("Triangle has duplicate points");

		MeshVertex vertex1 = null, vertex2 = null, vertex3 = null;

		for(MeshVertex faceVertex : face.getVertices()) {
			if(faceVertex.getPosition().epsilonEquals(tmpV1,config. tolerance))
				vertex1 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV2, config.tolerance))
				vertex2 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV3, config.tolerance))
				vertex3 = faceVertex;
		}

		for(MeshVertex addedVertex : tmpNewVertices.keySet()) {
			if(!tmpNewVertices.get(addedVertex).getNormal().epsilonEquals(face.getNormal(),
					config.tolerance))
				continue;

			if(addedVertex.getPosition().epsilonEquals(tmpV1, config.tolerance))
				vertex1 = addedVertex;
			if(addedVertex.getPosition().epsilonEquals(tmpV2, config.tolerance))
				vertex2 = addedVertex;
			if(addedVertex.getPosition().epsilonEquals(tmpV3, config.tolerance))
				vertex3 = addedVertex;
		}

		if(vertex1 == null) {
			vertex1 = new MeshVertex(face.getV1().getOtherAttributes().length);
			vertex1.getPosition().set(tmpV1);
			Vector3 bary = face.getTriangle().getBarycentricCoordinates(tmpV1);
			interpolate(vertex1, face.getV1(), bary.x, face.getV2(), bary.y, face.getV3(), bary.z);
			tmpNewVertices.put(vertex1, face);
			vertices.add(vertex1);
		}

		if(vertex2 == null) {
			vertex2 = new MeshVertex(face.getV1().getOtherAttributes().length);
			vertex2.getPosition().set(tmpV2);
			Vector3 bary = face.getTriangle().getBarycentricCoordinates(tmpV2);
			interpolate(vertex2, face.getV1(), bary.x, face.getV2(), bary.y, face.getV3(), bary.z);
			tmpNewVertices.put(vertex2, face);
			vertices.add(vertex2);
		}

		if(vertex3 == null) {
			vertex3 = new MeshVertex(face.getV1().getOtherAttributes().length);
			vertex3.getPosition().set(tmpV3);
			Vector3 bary = face.getTriangle().getBarycentricCoordinates(tmpV3);
			interpolate(vertex3, face.getV1(), bary.x, face.getV2(), bary.y, face.getV3(), bary.z);
			tmpNewVertices.put(vertex3, face);
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
		tmpRay.set(position.x, position.y, position.z, 0f, 1f, 0f);
		float minT = Float.POSITIVE_INFINITY;
		boolean upFacing = false;

		faceLoop:
		for(MeshFace face : faces) {
			if(!intersectTriangleRay(face.getTriangle(), tmpRay, config.tolerance, tmpSegment))
				continue;

			float t = tmpRay.direction.dot(tmpSegment.a.x - tmpRay.origin.x,
					tmpSegment.a.y - tmpRay.origin.y,
					tmpSegment.a.z - tmpRay.origin.z);
			float d = tmpRay.direction.dot(face.getNormal());

			if(Math.abs(d) <= config.tolerance) {
				float t2 = tmpRay.direction.dot(tmpSegment.b.x - tmpRay.origin.x,
						tmpSegment.b.y - tmpRay.origin.y,
						tmpSegment.b.z - tmpRay.origin.z);

				if(Math.min(t, t2) < config.tolerance && Math.max(t, t2) > -config.tolerance)
					return InsideStatus.BOUNDARY;

				continue;
			}

			if(Math.abs(t) < config.tolerance)
				return InsideStatus.BOUNDARY;

			if(t < 0f)
				continue;

			if (Math.abs(t - minT) < config.tolerance) {
				upFacing = upFacing && d > 0f;
			} else if(t < minT) {
				minT = t;
				upFacing = d > 0f;
			}

		}
		return upFacing ? InsideStatus.INSIDE : InsideStatus.OUTSIDE;
	}

	public void removeFaces(boolean inside, boolean boundary) {
		for(MeshFace face : faces) {
			InsideStatus status1 = vertexStatus.get(face.getV1());
			InsideStatus status2 = vertexStatus.get(face.getV2());
			InsideStatus status3 = vertexStatus.get(face.getV3());

			if(status1 == null || status2 == null || status3 == null)
				throw new IllegalStateException("Some vertices are not classified");

			boolean isBoundaryFace = boundaryFaces.contains(face)
					&& status1 == InsideStatus.BOUNDARY
					&& status2 == InsideStatus.BOUNDARY
					&& status3 == InsideStatus.BOUNDARY;

			boolean isFaceInside = status1 == InsideStatus.INSIDE
					|| status2 == InsideStatus.INSIDE
					|| status3 == InsideStatus.INSIDE
					|| status1 == InsideStatus.BOUNDARY
					&& status2 == InsideStatus.BOUNDARY
					&& status3 == InsideStatus.BOUNDARY
					&& !isBoundaryFace;

			boolean isFaceOutside = status1 == InsideStatus.OUTSIDE
					|| status2 == InsideStatus.OUTSIDE
					|| status3 == InsideStatus.OUTSIDE;

			if(isFaceInside && isFaceOutside)
				throw new IllegalStateException("Failure to split face");

			if(isBoundaryFace && boundary)
				toRemove.add(face);
			else if(isFaceInside && inside)
				toRemove.add(face);
			else if(isFaceOutside && !inside)
				toRemove.add(face);
		}
		faces.removeAll(toRemove, true);
		toRemove.clear();

		deleteFacelessVertices();
	}

	public void deleteFacelessVertices() {
		usedVertices.clear();
		for(MeshFace face : faces) {
			usedVertices.add(face.getV1());
			usedVertices.add(face.getV2());
			usedVertices.add(face.getV3());
		}

		for(int i = 0; i < vertices.size; i++) {
			if(!usedVertices.contains(vertices.get(i))) {
				vertices.removeIndex(i);
				i--;
			}
		}
		usedVertices.clear();
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

	@Override
	public void readFrom(InputStream stream) throws IOException {
		vertices.clear();
		faces.clear();

		int attrCount = readUnsignedByte(stream);
		VertexAttribute[] attrs = new VertexAttribute[attrCount];
		for(int i = 0; i < attrCount; i++) {
			attrs[i] = new VertexAttribute(readInt(stream),
					readInt(stream),
					readInt(stream),
					readBoolean(stream),
					readUTF(stream),
					readInt(stream));
		}

		attributes = new VertexAttributes(attrs);

		int vertexCount = readInt(stream);
		int vertexAttribsSize = readUnsignedByte(stream);

		for(int i = 0; i < vertexCount; i++) {
			MeshVertex meshVertex = new MeshVertex(vertexAttribsSize);
			readVec3(stream, meshVertex.getPosition());
			readVec3(stream, meshVertex.getNormal());
			readVec3(stream, meshVertex.getTangent());
			for(int j = 0; j < vertexAttribsSize; j++)
				meshVertex.getOtherAttributes()[j] = readFloat(stream);
			vertices.add(meshVertex);
		}

		int faceCount = readInt(stream);
		for(int i = 0; i < faceCount; i++) {
			faces.add(new MeshFace(vertices.get(readShort(stream)),
					vertices.get(readShort(stream)),
					vertices.get(readShort(stream))));
		}
	}

	@Override
	public void writeTo(OutputStream stream) throws IOException {
		writeByte(stream, attributes.size());
		for(VertexAttribute attribute : attributes) {
			writeInt(stream, attribute.usage);
			writeInt(stream, attribute.numComponents);
			writeInt(stream, attribute.type);
			writeBoolean(stream, attribute.normalized);
			writeUTF(stream, attribute.alias);
			writeInt(stream, attribute.unit);
		}

		writeInt(stream, vertices.size);
		writeByte(stream, vertices.get(0).getOtherAttributes().length);
		vertexIndices.clear();
		int i = 0;
		for(MeshVertex vertex : vertices) {
			writeVec3(stream, vertex.getPosition());
			writeVec3(stream, vertex.getNormal());
			writeVec3(stream, vertex.getTangent());
			for(float f : vertex.getOtherAttributes())
				writeFloat(stream, f);
			vertexIndices.put(vertex, i);
			i++;
		}

		writeInt(stream, faces.size);
		for(MeshFace face : faces) {
			writeShort(stream, vertexIndices.get(face.getV1(), -1));
			writeShort(stream, vertexIndices.get(face.getV2(), -1));
			writeShort(stream, vertexIndices.get(face.getV3(), -1));
		}
		vertexIndices.clear();
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

	public MeshPart toMeshPart(Mesh mesh) {
		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(true);

		int vertexSize = mesh.getVertexSize() / 4;

		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int norOffset = norAttr == null ? -1 : norAttr.offset / 4;
		int tanOffset = tanAttr == null ? -1 : tanAttr.offset / 4;

		buffer.limit((mesh.getNumVertices() + vertices.size) * vertexSize);
		idxBuffer.limit(mesh.getNumIndices() + faces.size * 3);

		int vOffset = mesh.getNumVertices() * vertexSize;

		vertexIndices.clear();
		for(int i = 0; i < vertices.size; i++) {
			MeshVertex vertex = vertices.get(i);
			buffer.position(vOffset + i * vertexSize + posOffset);
			buffer.put(vertex.getPosition().x);
			buffer.put(vertex.getPosition().y);
			buffer.put(vertex.getPosition().z);
			if(norOffset != -1) {
				buffer.position(vOffset + i * vertexSize + norOffset);
				buffer.put(vertex.getNormal().x);
				buffer.put(vertex.getNormal().y);
				buffer.put(vertex.getNormal().z);
			}

			if(tanOffset != -1) {
				buffer.position(vOffset + i * vertexSize + tanOffset);
				buffer.put(vertex.getTangent().x);
				buffer.put(vertex.getTangent().y);
				buffer.put(vertex.getTangent().z);
			}

			int j = 0;
			for(VertexAttribute attr : attributes) {
				if(attr.usage == VertexAttributes.Usage.Position
						|| attr.usage == VertexAttributes.Usage.Normal
						|| attr.usage == VertexAttributes.Usage.Tangent)
					continue;

				buffer.position(vOffset + i * vertexSize + attr.offset / 4);
				for(int k = 0; k < attr.getSizeInBytes() / 4; k++)
					buffer.put(vertex.getOtherAttributes()[j++]);
			}

			vertexIndices.put(vertex, i);
		}

		int fOffset = mesh.getNumIndices();

		for(int i = 0; i < faces.size; i++) {
			MeshFace face = faces.get(i);
			idxBuffer.position(fOffset + i * 3);

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

		return new MeshPart("id" + UUID.randomUUID(),
				mesh, mesh.getNumIndices() / 3, faces.size, GL_TRIANGLES);
	}

	public Mesh toMesh() {
		Mesh mesh = new Mesh(true, vertices.size, faces.size * 3, attributes);

		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(true);

		int vertexSize = mesh.getVertexSize() / 4;

		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int norOffset = norAttr == null ? -1 : norAttr.offset / 4;
		int tanOffset = tanAttr == null ? -1 : tanAttr.offset / 4;

		buffer.limit(vertices.size * vertexSize);
		idxBuffer.limit(faces.size * 3);

		vertexIndices.clear();
		for(int i = 0; i < vertices.size; i++) {
			MeshVertex vertex = vertices.get(i);
			buffer.position(i * vertexSize + posOffset);
			buffer.put(vertex.getPosition().x);
			buffer.put(vertex.getPosition().y);
			buffer.put(vertex.getPosition().z);
			if(norOffset != -1) {
				buffer.position(i * vertexSize + norOffset);
				buffer.put(vertex.getNormal().x);
				buffer.put(vertex.getNormal().y);
				buffer.put(vertex.getNormal().z);
			}

			if(tanOffset != -1) {
				buffer.position(i * vertexSize + tanOffset);
				buffer.put(vertex.getTangent().x);
				buffer.put(vertex.getTangent().y);
				buffer.put(vertex.getTangent().z);
			}

			int j = 0;
			for(VertexAttribute attr : attributes) {
				if(attr.usage == VertexAttributes.Usage.Position
				|| attr.usage == VertexAttributes.Usage.Normal
				|| attr.usage == VertexAttributes.Usage.Tangent)
					continue;

				buffer.position(i * vertexSize + attr.offset / 4);
				for(int k = 0; k < attr.getSizeInBytes() / 4; k++)
					buffer.put(vertex.getOtherAttributes()[j++]);
			}

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

	public InsideStatus getInsideStatus(MeshVertex vertex) {
		return vertexStatus.get(vertex);
	}

	public void clearInsideStatus() {
		vertexStatus.clear();
	}

	public Array<MeshVertex> getVertices() {
		return vertices;
	}

	public Array<MeshFace> getFaces() {
		return faces;
	}

	public CSGConfiguration getConfig() {
		return config;
	}

	public void setConfig(CSGConfiguration config) {
		this.config = config;
	}

	public VertexAttributes getAttributes() {
		return attributes;
	}

	public void setAttributes(VertexAttributes attributes) {
		this.attributes = attributes;
	}

	public enum InsideStatus {
		INSIDE, BOUNDARY, OUTSIDE
	}

	public static CSGMesh fromMeshPart(MeshPart meshPart) {
		Array<MeshVertex> vertices = new Array<>(meshPart.size);
		Array<MeshFace> faces = new Array<>(meshPart.size / 3);

		FloatBuffer buffer = meshPart.mesh.getVerticesBuffer(false);
		ShortBuffer idxBuffer = meshPart.mesh.getIndicesBuffer(false);

		int vertexSize = meshPart.mesh.getVertexSize() / 4;

		VertexAttribute norAttr = meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int otherAttrCount = vertexSize - (3 + (norAttr == null ? 0 : 3) + (tanAttr == null ? 0 : 3));

		IntMap<MeshVertex> meshVertices = new IntMap<>();

		int start = meshPart.offset / 3;
		int end = start + meshPart.size / 3;

		for(int i = start; i < end; i++) {
			short v1 = idxBuffer.get(i * 3);
			short v2 = idxBuffer.get(i * 3 + 1);
			short v3 = idxBuffer.get(i * 3 + 2);

			MeshVertex vertex1 = meshVertices.get(v1);
			MeshVertex vertex2 = meshVertices.get(v2);
			MeshVertex vertex3 = meshVertices.get(v3);

			if(vertex1 == null) {
				vertex1 = new MeshVertex(otherAttrCount);
				readVertex(meshPart.mesh, buffer, i, vertex1);
				meshVertices.put(v1, vertex1);
				vertices.add(vertex1);
			}

			if(vertex2 == null) {
				vertex2 = new MeshVertex(otherAttrCount);
				readVertex(meshPart.mesh, buffer, i, vertex2);
				meshVertices.put(v2, vertex2);
				vertices.add(vertex2);
			}

			if(vertex3 == null) {
				vertex3 = new MeshVertex(otherAttrCount);
				readVertex(meshPart.mesh, buffer, i, vertex3);
				meshVertices.put(v3, vertex3);
				vertices.add(vertex3);
			}

			faces.add(new MeshFace(vertex1, vertex2, vertex3));
		}

		return new CSGMesh(vertices, faces, meshPart.mesh.getVertexAttributes());
	}

	public static CSGMesh fromMesh(Mesh mesh) {
		Array<MeshVertex> vertices = new Array<>(mesh.getNumVertices());
		Array<MeshFace> faces = new Array<>(mesh.getNumIndices() / 3);

		FloatBuffer buffer = mesh.getVerticesBuffer(false);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(false);

		int vertexSize = mesh.getVertexSize() / 4;

		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int otherAttrCount = vertexSize - (3 + (norAttr == null ? 0 : 3) + (tanAttr == null ? 0 : 3));

		for(int i = 0; i < mesh.getNumVertices(); i++) {
			MeshVertex vertex = new MeshVertex(otherAttrCount);
			readVertex(mesh, buffer, i, vertex);
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


	private static void readVertex(Mesh mesh, FloatBuffer buffer, int index, MeshVertex out) {
		int vertexSize = mesh.getVertexSize() / 4;
		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		int norOffset = norAttr == null ? -1 : norAttr.offset / 4;
		int tanOffset = tanAttr == null ? -1 : tanAttr.offset / 4;

		out.getPosition().set(buffer.get(index * vertexSize + posOffset),
				buffer.get(index * vertexSize + posOffset + 1),
				buffer.get(index * vertexSize + posOffset + 2));
		if(norOffset != -1)
			out.getNormal().set(buffer.get(index * vertexSize + norOffset),
					buffer.get(index * vertexSize + norOffset + 1),
					buffer.get(index * vertexSize + norOffset + 2));
		if(tanOffset != -1)
			out.getTangent().set(buffer.get(index * vertexSize + tanOffset),
					buffer.get(index * vertexSize + tanOffset + 1),
					buffer.get(index * vertexSize + tanOffset + 2));

		int j = 0;
		for(VertexAttribute attr : mesh.getVertexAttributes()) {
			if(attr.usage == VertexAttributes.Usage.Position
					|| attr.usage == VertexAttributes.Usage.Normal
					|| attr.usage == VertexAttributes.Usage.Tangent)
				continue;

			for(int k = 0; k < attr.getSizeInBytes() / 4; k++)
				out.getOtherAttributes()[j++] =
						buffer.get(index * vertexSize + attr.offset / 4 + k);
		}
	}
}
