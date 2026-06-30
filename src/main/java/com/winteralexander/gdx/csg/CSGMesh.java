package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;
import com.badlogic.gdx.utils.*;
import com.winteralexander.gdx.utils.BufferUtil;
import com.winteralexander.gdx.utils.ReflectionUtil;
import com.winteralexander.gdx.utils.collection.CollectionUtil;
import com.winteralexander.gdx.utils.io.Serializable;
import com.winteralexander.gdx.utils.math.shape3d.SegmentPlus;
import com.winteralexander.gdx.utils.math.vector.VectorUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;
import static com.winteralexander.gdx.utils.Validation.ensureNotNull;
import static com.winteralexander.gdx.utils.io.SerializationUtil.readVec3;
import static com.winteralexander.gdx.utils.io.SerializationUtil.writeVec3;
import static com.winteralexander.gdx.utils.io.StreamUtil.*;
import static com.winteralexander.gdx.utils.math.shape3d.Intersector3D.LineIntersectionResult.COLLINEAR;
import static com.winteralexander.gdx.utils.math.shape3d.Intersector3D.*;
import static com.winteralexander.gdx.utils.math.shape3d.Intersector3D.TriangleIntersectionResult.*;

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
	private final ObjectMap<MeshFace, InsideStatus> faceStatus = new ObjectMap<>();
	private final ObjectSet<MeshVertex> usedVertices = new ObjectSet<>();
	// list of faces which intersect in a coplanar way
	private final HashSet<MeshFace> boundaryFaces = new HashSet<>();
	private final Array<Segment> cutEdges = new Array<>();

	private final SegmentPlus intersectSegment = new SegmentPlus();
	private final Plane plane = new Plane();

	private final float[] tmpArray = new float[9];
	private final Intersector.SplitTriangle splitTriangle = new Intersector.SplitTriangle(3);

	private final Vector3 tmpV1 = new Vector3(), tmpV2 = new Vector3(), tmpV3 = new Vector3();
	private final Vector3 tmpSegmentIntersection = new Vector3();

	private final Array<MeshFace> toRemove = new Array<>();
	private final Array<MeshFace> toAdd = new Array<>();
	private final HashMap<MeshVertex, MeshFace> tmpNewVertices = new HashMap<>();

	private final Ray tmpRay = new Ray();
	private final SegmentPlus tmpSegment = new SegmentPlus();

	private CSGConfiguration config = CSGConfiguration.DEFAULT;

	public CSGMesh() {
		this(new Array<>(), new Array<>(), null);
	}

	public CSGMesh(Array<MeshVertex> vertices, Array<MeshFace> faces, VertexAttributes attributes) {
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

	private void splitIfNeeded(int index, MeshFace face, MeshFace otherFace) {
		TriangleIntersectionResult result = intersectTriangleTriangle(face.getTriangle(),
				otherFace.getTriangle(),
				config.tolerance,
				intersectSegment);
		if(result == NONCOPLANAR_FACE_FACE) {
			cutEdges.add(intersectSegment.cpy());
			plane.set(otherFace.getPosition1(), otherFace.getNormal());
			splitFace(index, plane);
		} else if(result == EDGE_FACE) {
			boolean isEdgeFromFace = false;
			for(int j = 0; j < 3; j++) {
				Vector3 start = face.getTriangle().getPoint(j + 1);
				Vector3 end = face.getTriangle().getPoint((j + 1) % 3 + 1);
				if(intersectSegmentSegment(start,
						   end,
						   intersectSegment.a,
						   intersectSegment.b,
						   config.tolerance,
						   tmpSegmentIntersection)
						== COLLINEAR) {
					isEdgeFromFace = true;
					break;
				}
			}

			if(!isEdgeFromFace) {
				cutEdges.add(intersectSegment.cpy());
				plane.set(otherFace.getPosition1(), otherFace.getNormal());
				splitFace(index, plane);
			}
		}
	}

	public void splitTriangles(CSGMesh other) {
		tmpNewVertices.clear();
		boundaryFaces.clear();
		for(int j = 0; j < 10; j++)
			for(int i = 0; i < faces.size; i++)
				for(MeshFace otherFace : other.faces)
					// given splitFace may modify the faces array, must not put this at the outer level
					splitIfNeeded(i, faces.get(i), otherFace);

		if(config.enableBoundaryFaces)
			for(int i = 0; i < faces.size; i++) {
				MeshFace face = faces.get(i);
				for(MeshFace otherFace : other.faces) {
					if(face.getNormal().dot(otherFace.getNormal()) < 0.99f)
						continue;

					TriangleIntersectionResult
							result = intersectTriangleTriangle(face.getTriangle(),
									otherFace.getTriangle(),
									config.tolerance,
									intersectSegment);
					if(result == COPLANAR_FACE_FACE)
						boundaryFaces.add(face);
				}
			}
		tmpNewVertices.clear();
/*
		boolean mergedOne;
		do {
			mergedOne = false;
			for(int i = 0; i < faces.size; i++)
				mergedOne |= checkForMergeWithNeighbors(faces.get(i));
		} while(mergedOne);

		deleteFacelessVertices();*/
	}

	private void splitFace(int faceIndex, Plane plane) {
		MeshFace face = faces.get(faceIndex);
		face.getTriangle().toArray(tmpArray);
		Intersector.splitTriangle(tmpArray, plane, splitTriangle);

		if(splitTriangle.numBack == 0 && splitTriangle.numFront == 0)
			throw new IllegalStateException("Split face has no split result");

		for(int i = 0; i < splitTriangle.numBack; i++)
			processSplitTriangle(face, splitTriangle.back, i * 9);

		for(int i = 0; i < splitTriangle.numFront; i++)
			processSplitTriangle(face, splitTriangle.front, i * 9);
		if(toAdd.size == 0)
			return;

		faces.set(faceIndex, toAdd.get(0));
		faces.addAll(toAdd, 1, toAdd.size - 1);

		toAdd.clear();
	}

	private boolean checkForMergeWithNeighbors(MeshFace face) {
		if(!config.enableMerging)
			return false;

	faceLoop:
		for(int i = 0; i < faces.size; i++) {
			MeshFace current = faces.get(i);

			if(current == face)
				continue;

			int countMatching = 0;
			MeshVertex firstMatch = null, secondMatch = null;
			MeshVertex nonMatchingA = null, nonMatchingB = null;

			if(face.getV1() == current.getV1() || face.getV1() == current.getV2()
					|| face.getV1() == current.getV3()) {
				countMatching++;
				firstMatch = face.getV1();
			} else
				nonMatchingA = face.getV1();

			if(face.getV2() == current.getV1() || face.getV2() == current.getV2()
					|| face.getV2() == current.getV3()) {
				countMatching++;
				if(firstMatch == null)
					firstMatch = face.getV2();
				else
					secondMatch = face.getV2();
			} else
				nonMatchingA = face.getV2();

			if(face.getV3() == current.getV1() || face.getV3() == current.getV2()
					|| face.getV3() == current.getV3()) {
				countMatching++;
				if(firstMatch == null)
					firstMatch = face.getV3();
				else
					secondMatch = face.getV3();
			} else
				nonMatchingA = face.getV3();

			if(countMatching == 3)
				continue; // Duplicate triangles in mesh

			if(countMatching != 2)
				continue;

			if(current.getV1() != firstMatch && current.getV1() != secondMatch)
				nonMatchingB = current.getV1();
			else if(current.getV2() != firstMatch && current.getV2() != secondMatch)
				nonMatchingB = current.getV2();
			else if(current.getV3() != firstMatch && current.getV3() != secondMatch)
				nonMatchingB = current.getV3();

			boolean collinearWithFirst = intersectSegmentSegment(nonMatchingA.getPosition(),
												 nonMatchingB.getPosition(),
												 nonMatchingA.getPosition(),
												 firstMatch.getPosition(),
												 config.tolerance,
												 tmpSegmentIntersection)
					== COLLINEAR;

			boolean collinearWithSecond = intersectSegmentSegment(nonMatchingA.getPosition(),
												  nonMatchingB.getPosition(),
												  nonMatchingA.getPosition(),
												  secondMatch.getPosition(),
												  config.tolerance,
												  tmpSegmentIntersection)
					== COLLINEAR;

			if(!collinearWithFirst && !collinearWithSecond)
				continue;

			if(collinearWithFirst && collinearWithSecond)
				continue; // Invalid 2 faces

			for(Segment segment : cutEdges)
				if(intersectSegmentSegment(segment.a,
						   segment.b,
						   firstMatch.getPosition(),
						   secondMatch.getPosition(),
						   config.tolerance,
						   tmpSegmentIntersection)
						== COLLINEAR) {
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
			return true;
		}
		return false;
	}

	private void interpolate(MeshVertex out,
			MeshVertex v1,
			float w1,
			MeshVertex v2,
			float w2,
			MeshVertex v3,
			float w3) {
		out.getNormal()
				.set(0f, 0f, 0f)
				.mulAdd(v1.getNormal(), w1)
				.mulAdd(v2.getNormal(), w2)
				.mulAdd(v3.getNormal(), w3)
				.nor();

		out.getBinormal()
				.set(0f, 0f, 0f)
				.mulAdd(v1.getBinormal(), w1)
				.mulAdd(v2.getBinormal(), w2)
				.mulAdd(v3.getBinormal(), w3)
				.nor();

		out.getTangent()
				.set(0f, 0f, 0f)
				.mulAdd(v1.getTangent(), w1)
				.mulAdd(v2.getTangent(), w2)
				.mulAdd(v3.getTangent(), w3)
				.nor();

		for(int i = 0; i < out.getOtherAttributes().length; i++)
			out.getOtherAttributes()[i] = v1.getOtherAttributes()[i] * w1
					+ v2.getOtherAttributes()[i] * w2 + v3.getOtherAttributes()[i] * w3;
	}

	private void processSplitTriangle(MeshFace face, float[] array, int offset) {
		VectorUtil.setFromArray(tmpV1, array, offset);
		VectorUtil.setFromArray(tmpV2, array, offset + 3);
		VectorUtil.setFromArray(tmpV3, array, offset + 6);

		if(tmpV1.epsilonEquals(tmpV2, config.tolerance)
				|| tmpV1.epsilonEquals(tmpV3, config.tolerance)
				|| tmpV2.epsilonEquals(tmpV3, config.tolerance))
			return; // Triangle has duplicate points

		MeshVertex vertex1 = null, vertex2 = null, vertex3 = null;

		for(MeshVertex faceVertex : face.getVertices()) {
			if(faceVertex.getPosition().epsilonEquals(tmpV1, config.tolerance))
				vertex1 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV2, config.tolerance))
				vertex2 = faceVertex;
			if(faceVertex.getPosition().epsilonEquals(tmpV3, config.tolerance))
				vertex3 = faceVertex;
		}

		for(MeshVertex addedVertex : tmpNewVertices.keySet()) {
			if(!tmpNewVertices.get(addedVertex)
							.getNormal()
							.epsilonEquals(face.getNormal(), config.tolerance))
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
			vertexStatus.put(vertex, other.computeInsideStatus(vertex.getPosition(), config));
		faceStatus.clear();
		for(MeshFace face : faces) {
			boolean boundaryFace = getBoundaryFaces().contains(face);
			CSGMesh.InsideStatus status1 = getInsideStatus(face.getV1());
			CSGMesh.InsideStatus status2 = getInsideStatus(face.getV2());
			CSGMesh.InsideStatus status3 = getInsideStatus(face.getV3());

			if(status1 == null || status2 == null || status3 == null)
				throw new IllegalStateException("Some vertices are not classified");

			boolean allPointsBoundary = status1 == CSGMesh.InsideStatus.BOUNDARY
					&& status2 == CSGMesh.InsideStatus.BOUNDARY
					&& status3 == CSGMesh.InsideStatus.BOUNDARY;

			if(boundaryFace) {
				if(!allPointsBoundary)
					throw new IllegalStateException("Boundary face has points not on the boundary");
				faceStatus.put(face, InsideStatus.BOUNDARY);
				continue;
			}

			if(allPointsBoundary) {
				InsideStatus status = other.computeInsideStatus(
						tmpV1.set(face.getV1().getPosition())
								.add(face.getV2().getPosition())
								.add(face.getV3().getPosition())
								.scl(1f / 3f),
						config);
				if(status == InsideStatus.BOUNDARY)
					status = InsideStatus.INSIDE;
				faceStatus.put(face, status);
				continue;
			}

			boolean anyInside = status1 == CSGMesh.InsideStatus.INSIDE
					|| status2 == CSGMesh.InsideStatus.INSIDE
					|| status3 == CSGMesh.InsideStatus.INSIDE;

			boolean anyOutside = status1 == CSGMesh.InsideStatus.OUTSIDE
					|| status2 == CSGMesh.InsideStatus.OUTSIDE
					|| status3 == CSGMesh.InsideStatus.OUTSIDE;

			if(anyInside && anyOutside)
				throw new IllegalStateException("Failure to split face, some vertices are "
						+ "in and some are out");

			faceStatus.put(face, anyInside ? InsideStatus.INSIDE : InsideStatus.OUTSIDE);
		}
	}

	/**
	 * Computes the {@link InsideStatus} of a given position using ray intersections with the faces
	 * of this mesh
	 *
	 * @param position position to check
	 * @return inside, outside or on the boundary
	 */
	public InsideStatus computeInsideStatus(Vector3 position, CSGConfiguration config) {
		tmpRay.set(position.x,
				position.y,
				position.z,
				config.insideTestDirection.x,
				config.insideTestDirection.y,
				config.insideTestDirection.z);
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

			if(Math.abs(t - minT) < config.tolerance) {
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
			InsideStatus faceS = faceStatus.get(face);

			if(faceS == InsideStatus.BOUNDARY && boundary)
				toRemove.add(face);
			else if(faceS == InsideStatus.INSIDE && inside)
				toRemove.add(face);
			else if(faceS == InsideStatus.OUTSIDE && !inside)
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
			readVec3(stream, meshVertex.getBinormal());
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
			writeVec3(stream, vertex.getBinormal());
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
		return CSGMeshConverter.csgMeshToMeshPart(this, mesh);
	}

	public Mesh toMesh() {
		return CSGMeshConverter.csgMeshToMesh(this);
	}

	public void toBuilder(MeshPartBuilder partBuilder) {
		CSGMeshConverter.csgMeshToPartBuilder(this, partBuilder);
	}

	public void toBuilder(MeshPartBuilder partBuilder, IntArray insertIndices) {
		CSGMeshConverter.csgMeshToPartBuilder(this, partBuilder, insertIndices);
	}

	public InsideStatus getInsideStatus(MeshVertex vertex) {
		return vertexStatus.get(vertex);
	}

	public InsideStatus getInsideStatus(MeshFace face) {
		return faceStatus.get(face);
	}

	public void clearInsideStatus() {
		vertexStatus.clear();
		faceStatus.clear();
		boundaryFaces.clear();
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

	public HashSet<MeshFace> getBoundaryFaces() {
		return boundaryFaces;
	}

	public enum InsideStatus { INSIDE, BOUNDARY, OUTSIDE }

	public static CSGMesh fromMeshPart(MeshPart meshPart) {
		return CSGMeshConverter.meshPartToCSGMesh(meshPart);
	}

	public static CSGMesh fromMesh(Mesh mesh) {
		return CSGMeshConverter.meshToCSGMesh(mesh);
	}

	public static CSGMesh fromBuilder(MeshPartBuilder partBuilder) {
		return CSGMeshConverter.partBuilderToCSGMesh(partBuilder);
	}

	public static CSGMesh fromBuilder(MeshPartBuilder partBuilder, IntArray vertexIndices) {
		return CSGMeshConverter.partBuilderToCSGMesh(partBuilder, vertexIndices);
	}
}
