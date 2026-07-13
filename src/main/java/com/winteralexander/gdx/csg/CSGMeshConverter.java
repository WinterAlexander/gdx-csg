package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.utils.*;
import com.winteralexander.gdx.utils.BufferUtil;
import com.winteralexander.gdx.utils.ReflectionUtil;
import com.winteralexander.gdx.utils.collection.CollectionUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.UUID;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;

/**
 * Utility class to convert CSG meshes to and from other mesh formats
 * <p>
 * Created on 2026-06-25.
 *
 * @author Alexander Winter
 */
public class CSGMeshConverter {
	private static final ObjectIntMap<MeshVertex> tmpVertexIndices = new ObjectIntMap<>();
	private static final IntArray tmpDeadTriangles = new IntArray();

	private CSGMeshConverter() {}

	public static CSGMesh meshPartToCSGMesh(MeshPart meshPart) {
		Array<MeshVertex> vertices = new Array<>(meshPart.size);
		Array<MeshFace> faces = new Array<>(meshPart.size / 3);

		FloatBuffer buffer = meshPart.mesh.getVerticesBuffer(false);
		ShortBuffer idxBuffer = meshPart.mesh.getIndicesBuffer(false);

		int vertexSize = meshPart.mesh.getVertexSize() / 4;

		VertexAttribute norAttr = meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int otherAttrCount = vertexSize
				- (3 + (norAttr == null ? 0 : 3) + (tanAttr == null ? 0 : 3));

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

	public static CSGMesh meshToCSGMesh(Mesh mesh) {
		Array<MeshVertex> vertices = new Array<>(mesh.getNumVertices());
		Array<MeshFace> faces = new Array<>(mesh.getNumIndices() / 3);

		FloatBuffer buffer = mesh.getVerticesBuffer(false);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(false);

		int vertexSize = mesh.getVertexSize() / 4;

		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int otherAttrCount = vertexSize
				- (3 + (norAttr == null ? 0 : 3) + (tanAttr == null ? 0 : 3));

		for(int i = 0; i < mesh.getNumVertices(); i++) {
			MeshVertex vertex = new MeshVertex(otherAttrCount);
			readVertex(mesh, buffer, i, vertex);
			vertices.add(vertex);
		}

		for(int i = 0; i < mesh.getNumIndices() / 3; i++) {
			short v1 = idxBuffer.get(i * 3);
			short v2 = idxBuffer.get(i * 3 + 1);
			short v3 = idxBuffer.get(i * 3 + 2);
			MeshFace face = new MeshFace(vertices.get(v1), vertices.get(v2), vertices.get(v3));
			faces.add(face);
		}

		return new CSGMesh(vertices, faces, mesh.getVertexAttributes());
	}
	public static CSGMesh partBuilderToCSGMesh(MeshPartBuilder partBuilder) {
		return partBuilderToCSGMesh(partBuilder, null);
	}

	public static CSGMesh partBuilderToCSGMesh(MeshPartBuilder partBuilder, IntArray vertexIndices) {
		if(!(partBuilder instanceof MeshBuilder))
			throw new IllegalArgumentException("Unsupported builder: " + partBuilder);

		MeshBuilder builder = (MeshBuilder)partBuilder;

		Array<MeshVertex> vertices = new Array<>(builder.getNumVertices());
		IntIntMap indexMap = null;
		if(vertexIndices != null)
			indexMap = new IntIntMap();
		Array<MeshFace> faces = new Array<>(builder.getNumIndices() / 3);
		VertexAttributes attrs = builder.getAttributes();

		FloatArray buffer = ReflectionUtil.get(builder, "vertices");
		ShortArray idxBuffer = ReflectionUtil.get(builder, "indices");
		int posOffset = ReflectionUtil.get(builder, "posOffset");
		int norOffset = ReflectionUtil.get(builder, "norOffset");
		int biNorOffset = ReflectionUtil.get(builder, "biNorOffset");
		int tanOffset = ReflectionUtil.get(builder, "tangentOffset");

		int otherAttrCount = builder.getFloatsPerVertex()
				- (3 + (norOffset == -1 ? 0 : 3) + (biNorOffset == -1 ? 0 : 3)
				+ (tanOffset == -1 ? 0 : 3));

		int count = vertexIndices == null ? builder.getNumVertices() : vertexIndices.size;
		for(int i = 0; i < count; i++) {
			MeshVertex vertex = new MeshVertex(otherAttrCount);
			readVertex(builder,
					buffer,
					attrs,
					posOffset,
					norOffset,
					biNorOffset,
					tanOffset,
					vertexIndices == null ? i : vertexIndices.get(i),
					vertex);
			if(vertexIndices != null)
				indexMap.put(vertexIndices.get(i), i);
			vertices.add(vertex);
		}

		for(int i = 0; i < builder.getNumIndices() / 3; i++) {
			int v1 = idxBuffer.get(i * 3);
			int v2 = idxBuffer.get(i * 3 + 1);
			int v3 = idxBuffer.get(i * 3 + 2);
			if(vertexIndices != null) {
				v1 = indexMap.get(v1, -1);
				v2 = indexMap.get(v2, -1);
				v3 = indexMap.get(v3, -1);

				if(v1 == -1 || v2 == -1 || v3 == -1)
					continue; // ignore triangles not involving selected vertices
			}

			MeshFace face = new MeshFace(vertices.get(v1), vertices.get(v2), vertices.get(v3));
			faces.add(face);
		}

		return new CSGMesh(vertices, faces, attrs);
	}

	public static MeshPart csgMeshToMeshPart(CSGMesh csgMesh, Mesh mesh) {
		Array<MeshVertex> vertices = csgMesh.getVertices();
		Array<MeshFace> faces = csgMesh.getFaces();
		VertexAttributes attributes = csgMesh.getAttributes();

		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(true);

		int vertexSize = mesh.getVertexSize() / 4;

		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute biNorAttr = mesh.getVertexAttribute(VertexAttributes.Usage.BiNormal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int norOffset = norAttr == null ? -1 : norAttr.offset / 4;
		int biNorOffset = biNorAttr == null ? -1 : biNorAttr.offset / 4;
		int tanOffset = tanAttr == null ? -1 : tanAttr.offset / 4;

		buffer.limit((mesh.getNumVertices() + vertices.size) * vertexSize);
		idxBuffer.limit(mesh.getNumIndices() + faces.size * 3);

		int vOffset = mesh.getNumVertices() * vertexSize;

		tmpVertexIndices.clear();
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

			if(biNorOffset != -1) {
				buffer.position(vOffset + i * vertexSize + biNorOffset);
				buffer.put(vertex.getBinormal().x);
				buffer.put(vertex.getBinormal().y);
				buffer.put(vertex.getBinormal().z);
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

			tmpVertexIndices.put(vertex, i);
		}

		int fOffset = mesh.getNumIndices();

		for(int i = 0; i < faces.size; i++) {
			MeshFace face = faces.get(i);
			idxBuffer.position(fOffset + i * 3);

			int idx1 = tmpVertexIndices.get(face.getV1(), -1);
			int idx2 = tmpVertexIndices.get(face.getV2(), -1);
			int idx3 = tmpVertexIndices.get(face.getV3(), -1);

			if(idx1 == -1 || idx2 == -1 || idx3 == -1)
				throw new IllegalStateException("CSGMesh has a face refering to a vertex not in "
						+ "the mesh. Face #" + i + " has vertices "
						+ "#" + idx1 + ", #" + idx2 + " and #" + idx3);

			idxBuffer.put((short)idx1);
			idxBuffer.put((short)idx2);
			idxBuffer.put((short)idx3);
		}
		tmpVertexIndices.clear();

		return new MeshPart("id" + UUID.randomUUID(),
				mesh,
				mesh.getNumIndices() / 3,
				faces.size,
				GL_TRIANGLES);
	}

	public static Mesh csgMeshToMesh(CSGMesh csgMesh) {
		Array<MeshVertex> vertices = csgMesh.getVertices();
		Array<MeshFace> faces = csgMesh.getFaces();
		VertexAttributes attributes = csgMesh.getAttributes();
		Mesh mesh = new Mesh(true, vertices.size, faces.size * 3, attributes);

		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		ShortBuffer idxBuffer = mesh.getIndicesBuffer(true);

		int vertexSize = mesh.getVertexSize() / 4;

		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute biNorAttr = mesh.getVertexAttribute(VertexAttributes.Usage.BiNormal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int norOffset = norAttr == null ? -1 : norAttr.offset / 4;
		int biNorOffset = biNorAttr == null ? -1 : biNorAttr.offset / 4;
		int tanOffset = tanAttr == null ? -1 : tanAttr.offset / 4;

		buffer.limit(vertices.size * vertexSize);
		idxBuffer.limit(faces.size * 3);

		tmpVertexIndices.clear();
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

			if(biNorOffset != -1) {
				buffer.position(i * vertexSize + biNorOffset);
				buffer.put(vertex.getBinormal().x);
				buffer.put(vertex.getBinormal().y);
				buffer.put(vertex.getBinormal().z);
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

			tmpVertexIndices.put(vertex, i);
		}

		for(int i = 0; i < faces.size; i++) {
			MeshFace face = faces.get(i);
			idxBuffer.position(i * 3);

			int idx1 = tmpVertexIndices.get(face.getV1(), -1);
			int idx2 = tmpVertexIndices.get(face.getV2(), -1);
			int idx3 = tmpVertexIndices.get(face.getV3(), -1);

			if(idx1 == -1 || idx2 == -1 || idx3 == -1)
				throw new IllegalStateException("CSGMesh has a face refering to a vertex not in "
						+ "the mesh. Face #" + i + " has vertices "
						+ "#" + idx1 + ", #" + idx2 + " and #" + idx3);

			idxBuffer.put((short)idx1);
			idxBuffer.put((short)idx2);
			idxBuffer.put((short)idx3);
		}
		tmpVertexIndices.clear();

		return mesh;
	}

	public static void csgMeshToPartBuilder(CSGMesh csgMesh, MeshPartBuilder partBuilder) {
		if(!(partBuilder instanceof MeshBuilder))
			throw new IllegalArgumentException("Unsupported builder: " + partBuilder);

		MeshBuilder builder = (MeshBuilder)partBuilder;

		FloatArray buffer = ReflectionUtil.get(builder, "vertices");
		ShortArray idxBuffer = ReflectionUtil.get(builder, "indices");
		int vertexSize = ((MeshBuilder)partBuilder).getFloatsPerVertex();

		tmpVertexIndices.clear();
		int index = buffer.size / vertexSize;
		buffer.setSize(buffer.size + csgMesh.getVertices().size * vertexSize);
		for(MeshVertex vertex : csgMesh.getVertices()) {
			writeVertex(buffer, vertex, index, vertexSize, builder.getAttributes());
			tmpVertexIndices.put(vertex, index);
			index++;
		}

		index = idxBuffer.size / 3;
		idxBuffer.setSize(idxBuffer.size + csgMesh.getFaces().size * 3);
		for(MeshFace face : csgMesh.getFaces())
			writeFace(idxBuffer, face, index++, tmpVertexIndices);

		tmpVertexIndices.clear();
	}

	public static void csgMeshToPartBuilder(CSGMesh csgMesh, MeshPartBuilder partBuilder, IntArray insertIndices) {
		if(insertIndices == null) {
			csgMeshToPartBuilder(csgMesh, partBuilder);
			return;
		}

		if(!(partBuilder instanceof MeshBuilder))
			throw new IllegalArgumentException("Unsupported builder: " + partBuilder);

		MeshBuilder builder = (MeshBuilder)partBuilder;

		FloatArray buffer = ReflectionUtil.get(builder, "vertices");
		ShortArray idxBuffer = ReflectionUtil.get(builder, "indices");
		int vertexSize = ((MeshBuilder)partBuilder).getFloatsPerVertex();
		int maxInsertVertexId = CollectionUtil.max(insertIndices);

		if(insertIndices.size > csgMesh.getVertices().size
				&& ((MeshBuilder)partBuilder).getNumVertices() - 1 > maxInsertVertexId)
			throw new IllegalArgumentException("CSGMesh cannot be fully fill the insertion "
					+ "region without leaving gaps in the vertex array");

		IntSet insertVerticesSet = CollectionUtil.toGdxSet(insertIndices);

		tmpDeadTriangles.clear();
		getTrianglesOfVertices(idxBuffer, insertVerticesSet, tmpDeadTriangles);

		buffer.ensureCapacity(Math.max(0, (csgMesh.getVertices().size - insertIndices.size) * vertexSize));
		for(int i = 0; i < csgMesh.getVertices().size; i++) {
			MeshVertex vertex = csgMesh.getVertices().get(i);

			int index = i >= insertIndices.size ? buffer.size / vertexSize : insertIndices.get(i);

			if((index + 1) * vertexSize > buffer.size)
				buffer.setSize((index + 1) * vertexSize);

			writeVertex(buffer, vertex, index, vertexSize, builder.getAttributes());

			tmpVertexIndices.put(vertex, index);
		}

		if(csgMesh.getVertices().size < insertIndices.size)
			buffer.setSize((insertIndices.get(csgMesh.getVertices().size - 1) + 1) * vertexSize);

		idxBuffer.ensureCapacity(Math.max(0, (csgMesh.getFaces().size - tmpDeadTriangles.size) * 3));
		for(int i = 0; i < csgMesh.getFaces().size; i++) {
			int index = idxBuffer.size / 3 + 1;
			if(i < tmpDeadTriangles.size)
				index = tmpDeadTriangles.get(i);

			if(index * 3 >= idxBuffer.size)
				idxBuffer.setSize((index + 1) * 3);

			MeshFace face = csgMesh.getFaces().get(i);
			writeFace(idxBuffer, face, index, tmpVertexIndices);
		}

		for(int i = tmpDeadTriangles.size - 1; i >= csgMesh.getFaces().size; i--) {
			idxBuffer.removeIndex(tmpDeadTriangles.get(i) * 3 + 2);
			idxBuffer.removeIndex(tmpDeadTriangles.get(i) * 3 + 1);
			idxBuffer.removeIndex(tmpDeadTriangles.get(i) * 3);
		}

		tmpDeadTriangles.clear();
		tmpVertexIndices.clear();

		ReflectionUtil.set(builder, "vindex", builder.getNumVertices());
		ReflectionUtil.set(builder, "lastIndex", builder.getNumVertices() - 1);
	}

	private static void getTrianglesOfVertices(ShortArray idxBuffer,
	                                           IntSet indices,
	                                           IntArray outTriangles) {
		for(int tri = 0; tri < idxBuffer.size / 3; tri++) {
			int v1 = idxBuffer.get(tri * 3);
			int v2 = idxBuffer.get(tri * 3 + 1);
			int v3 = idxBuffer.get(tri * 3 + 2);

			if(indices.contains(v1) || indices.contains(v2) || indices.contains(v3))
				outTriangles.add(tri);
		}
	}

	private static void readVertex(Mesh mesh, FloatBuffer buffer, int index, MeshVertex out) {
		int vertexSize = mesh.getVertexSize() / 4;
		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute biNorAttr = mesh.getVertexAttribute(VertexAttributes.Usage.BiNormal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		int norOffset = norAttr == null ? -1 : norAttr.offset / 4;
		int biNorOffset = biNorAttr == null ? -1 : biNorAttr.offset / 4;
		int tanOffset = tanAttr == null ? -1 : tanAttr.offset / 4;

		out.getPosition().set(buffer.get(index * vertexSize + posOffset),
				buffer.get(index * vertexSize + posOffset + 1),
				buffer.get(index * vertexSize + posOffset + 2));
		if(norOffset != -1)
			out.getNormal().set(buffer.get(index * vertexSize + norOffset),
					buffer.get(index * vertexSize + norOffset + 1),
					buffer.get(index * vertexSize + norOffset + 2));
		if(biNorOffset != -1)
			out.getBinormal().set(buffer.get(index * vertexSize + biNorOffset),
					buffer.get(index * vertexSize + biNorOffset + 1),
					buffer.get(index * vertexSize + biNorOffset + 2));
		if(tanOffset != -1)
			out.getTangent().set(buffer.get(index * vertexSize + tanOffset),
					buffer.get(index * vertexSize + tanOffset + 1),
					buffer.get(index * vertexSize + tanOffset + 2));

		int j = 0;
		for(VertexAttribute attr : mesh.getVertexAttributes()) {
			if(attr.usage == VertexAttributes.Usage.Position
					|| attr.usage == VertexAttributes.Usage.Normal
					|| attr.usage == VertexAttributes.Usage.BiNormal
					|| attr.usage == VertexAttributes.Usage.Tangent)
				continue;

			for(int k = 0; k < attr.getSizeInBytes() / 4; k++)
				out.getOtherAttributes()[j++] = buffer.get(index * vertexSize + attr.offset / 4
						+ k);
		}
	}


	private static void readVertex(MeshBuilder builder,
	                               FloatArray buffer,
	                               VertexAttributes attrs,
	                               int posOffset,
	                               int norOffset,
	                               int biNorOffset,
	                               int tanOffset,
	                               int vertexIndex,
	                               MeshVertex out) {

		BufferUtil.getVector3(buffer,
				vertexIndex * builder.getFloatsPerVertex() + posOffset,
				out.getPosition());

		if(norOffset != -1)
			BufferUtil.getVector3(buffer,
					vertexIndex * builder.getFloatsPerVertex() + norOffset,
					out.getNormal());

		if(biNorOffset != -1)
			BufferUtil.getVector3(buffer,
					vertexIndex * builder.getFloatsPerVertex() + biNorOffset,
					out.getBinormal());

		if(tanOffset != -1)
			BufferUtil.getVector3(buffer,
					vertexIndex * builder.getFloatsPerVertex() + tanOffset,
					out.getTangent());

		int bufferIndex = 0;
		for(VertexAttribute attr : attrs) {
			if(attr.usage == VertexAttributes.Usage.Position
					|| attr.usage == VertexAttributes.Usage.Normal
					|| attr.usage == VertexAttributes.Usage.BiNormal
					|| attr.usage == VertexAttributes.Usage.Tangent)
				continue;

			for(int i = 0; i < attr.numComponents; i++) {
				int index = vertexIndex * builder.getFloatsPerVertex() + attr.offset / 4 + i;
				out.getOtherAttributes()[bufferIndex] = buffer.get(index);
				bufferIndex++;
			}
		}
	}

	private static void writeVertex(FloatArray buffer,
	                                MeshVertex vertex,
	                                int index,
	                                int vertexSize,
	                                VertexAttributes attributes) {
		int posOffset = attributes.getOffset(VertexAttributes.Usage.Position, -1);
		int norOffset = attributes.getOffset(VertexAttributes.Usage.Normal, -1);
		int biNorOffset = attributes.getOffset(VertexAttributes.Usage.BiNormal, -1);
		int tanOffset = attributes.getOffset(VertexAttributes.Usage.Tangent, -1);

		BufferUtil.putVector3(buffer, index * vertexSize + posOffset, vertex.getPosition());

		if(norOffset != -1)
			BufferUtil.putVector3(buffer, index * vertexSize + norOffset, vertex.getNormal());

		if(biNorOffset != -1)
			BufferUtil.putVector3(buffer, index * vertexSize + biNorOffset, vertex.getBinormal());

		if(tanOffset != -1)
			BufferUtil.putVector3(buffer, index * vertexSize + tanOffset, vertex.getTangent());

		int j = 0;
		for(VertexAttribute attr : attributes) {
			if(attr.usage == VertexAttributes.Usage.Position
					|| attr.usage == VertexAttributes.Usage.Normal
					|| attr.usage == VertexAttributes.Usage.BiNormal
					|| attr.usage == VertexAttributes.Usage.Tangent)
				continue;

			for(int k = 0; k < attr.getSizeInBytes() / 4; k++)
				buffer.set(index * vertexSize + attr.offset / 4 + k,
						vertex.getOtherAttributes()[j++]);
		}
	}

	private static void writeFace(ShortArray idxBuffer,
	                              MeshFace face,
	                              int index,
	                              ObjectIntMap<MeshVertex> vertexIndices) {

		int idx1 = vertexIndices.get(face.getV1(), -1);
		int idx2 = vertexIndices.get(face.getV2(), -1);
		int idx3 = vertexIndices.get(face.getV3(), -1);

		if(idx1 == -1 || idx2 == -1 || idx3 == -1)
			throw new IllegalStateException("CSGMesh has a face refering to a vertex not in "
					+ "the mesh. Face #" + index + " has vertices "
					+ "#" + idx1 + ", #" + idx2 + " and #" + idx3);

		idxBuffer.set(index * 3, (short)idx1);
		idxBuffer.set(index * 3 + 1, (short)idx2);
		idxBuffer.set(index * 3 + 2, (short)idx3);
	}

}
