package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.EllipseShapeBuilder;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntIntMap;
import com.badlogic.gdx.utils.ShortArray;
import com.winteralexander.gdx.utils.BufferUtil;
import com.winteralexander.gdx.utils.ReflectionUtil;
import com.winteralexander.gdx.utils.math.direction.AxisDirection6;
import com.winteralexander.gdx.utils.math.direction.GridCorner;
import com.winteralexander.gdx.utils.math.shape3d.Triangle;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;
import static java.lang.Math.abs;

/**
 * Utility class for operations on meshes and mesh builders
 * <p>
 * Created on 2024-08-28.
 *
 * @author Alexander Winter
 */
public class MeshBuilderUtil {
	private static final Vector2 tmpVec2 = new Vector2();
	private static final Vector3 tmpVec3 = new Vector3();

	private static final Triangle tmpTriangle = new Triangle();

	private static final IntIntMap tmpVerticesTriCount = new IntIntMap();

	private MeshBuilderUtil() {}

	public static Vertex getVertex(MeshBuilder meshBuilder, int index, Vertex out) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		int biNorOffset = ReflectionUtil.get(meshBuilder, "biNorOffset");
		int tangentOffset = ReflectionUtil.get(meshBuilder, "tangentOffset");
		int uvOffset = meshBuilder.getAttributes()
				.getOffset(VertexAttributes.Usage.TextureCoordinates, -1);
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		int offset = index * meshBuilder.getFloatsPerVertex() + posOffset;
		if(out.position == null)
			out.position = new Vector3();
		BufferUtil.getVector3(vertices, offset, out.position);

		if(norOffset != -1) {
			offset = index * meshBuilder.getFloatsPerVertex() + norOffset;
			if(out.normal == null)
				out.normal = new Vector3();
			BufferUtil.getVector3(vertices, offset, out.normal);
		} else
			out.normal = null;

		if(biNorOffset != -1) {
			offset = index * meshBuilder.getFloatsPerVertex() + biNorOffset;
			if(out.binormal == null)
				out.binormal = new Vector3();
			BufferUtil.getVector3(vertices, offset, out.binormal);
		} else
			out.binormal = null;

		if(tangentOffset != -1) {
			offset = index * meshBuilder.getFloatsPerVertex() + tangentOffset;
			if(out.tangent == null)
				out.tangent = new Vector3();
			BufferUtil.getVector3(vertices, offset, out.tangent);
		} else
			out.tangent = null;

		if(uvOffset != -1) {
			offset = index * meshBuilder.getFloatsPerVertex() + uvOffset;
			if(out.uv == null)
				out.uv = new Vector2();
			BufferUtil.getVector2(vertices, offset, out.uv);
		} else
			out.uv = null;
		return out;
	}

	public static void setVertex(MeshBuilder meshBuilder, int index, Vertex vertex) {
		setVertex(meshBuilder, index,
				vertex.position, vertex.normal, vertex.binormal, vertex.tangent, vertex.uv);
	}

	public static void setVertex(MeshBuilder meshBuilder,
	                              int index,
	                              Vector3 position,
	                              Vector3 normal,
	                              Vector3 binormal,
	                              Vector3 tangent,
	                              Vector2 uv) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		int biNorOffset = ReflectionUtil.get(meshBuilder, "biNorOffset");
		int tangentOffset = ReflectionUtil.get(meshBuilder, "tangentOffset");
		int uvOffset = meshBuilder.getAttributes()
				.getOffset(VertexAttributes.Usage.TextureCoordinates, -1);
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		if(position != null) {
			int offset = index * meshBuilder.getFloatsPerVertex() + posOffset;
			BufferUtil.putVector3(vertices, offset, position);
		}

		if(norOffset != -1 && normal != null) {
			int offset = index * meshBuilder.getFloatsPerVertex() + norOffset;
			BufferUtil.putVector3(vertices, offset, normal);
		}

		if(biNorOffset != -1 && binormal != null) {
			int offset = index * meshBuilder.getFloatsPerVertex() + biNorOffset;
			BufferUtil.putVector3(vertices, offset, binormal);
		}

		if(tangentOffset != -1 && tangent != null) {
			int offset = index * meshBuilder.getFloatsPerVertex() + tangentOffset;
			BufferUtil.putVector3(vertices, offset, tangent);
		}

		if(uvOffset != -1 && uv != null) {
			int offset = index * meshBuilder.getFloatsPerVertex() + tangentOffset;
			BufferUtil.putVector2(vertices, offset, uv);
		}
	}

	/**
	 * Builds a rectangle from 4 points, a normal and a texture region. The region will apply UVs
	 * to the rectangle built into the mesh part builder. This function will ensure the face is
	 * properly oriented toward the normal, flipping it if needed.
	 *
	 * @param builder builder to build the rectangle into
	 * @param p1 first point of the rectangle, bottom left of UV texture
	 * @param p2 second point of the rectangle, bottom right of UV texture
	 * @param p3 third point of the rectangle, top right of UV texture
	 * @param p4 fourth point of the rectangle, top left of UV texture
	 * @param normal normal of the rectangle
	 * @param region region to give UVs
	 */
	public static void quad(MeshPartBuilder builder,
	                        Vector3 p1, Vector3 p2,
	                        Vector3 p3, Vector3 p4,
	                        Vector3 normal,
	                        TextureRegion region) {
		boolean flip = normal.dot(tmpVec3.set(p2).sub(p1)
				.crs(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)) < 0f;
		short corner00 = builder.vertex(p1, normal, Color.WHITE,
				tmpVec2.set(region.getU(), region.getV2()));
		short corner10 = builder.vertex(p2, normal, Color.WHITE,
				tmpVec2.set(region.getU2(), region.getV2()));
		short corner11 = builder.vertex(p3, normal, Color.WHITE,
				tmpVec2.set(region.getU2(), region.getV()));
		short corner01 = builder.vertex(p4, normal, Color.WHITE,
				tmpVec2.set(region.getU(), region.getV()));
		builder.rect(corner00,
				flip ? corner01 : corner10,
				corner11,
				flip ? corner10 : corner01);
	}

	public static void quad(MeshPartBuilder builder,
	                        Vector3 p1, Vector3 p2,
	                        Vector3 p3, Vector3 p4,
	                        Vector3 normal) {
		boolean flip = normal.dot(tmpVec3.set(p2).sub(p1)
				.crs(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)) < 0f;
		short corner00 = builder.vertex(p1, normal, Color.WHITE,
				tmpVec2.set(0f, 1f));
		short corner10 = builder.vertex(p2, normal, Color.WHITE,
				tmpVec2.set(1f, 1f));
		short corner11 = builder.vertex(p3, normal, Color.WHITE,
				tmpVec2.set(1f, 0f));
		short corner01 = builder.vertex(p4, normal, Color.WHITE,
				tmpVec2.set(0f, 0f));
		builder.rect(corner00,
				flip ? corner01 : corner10,
				corner11,
				flip ? corner10 : corner01);
	}

	public static void transform(Model model, Matrix4 transform) {
		transform(model, transform, transform.cpy().inv().tra());
	}

	public static void transform(Model model, Matrix4 transform, Matrix4 normalTransform) {
		for(Mesh mesh : model.meshes)
			transform(mesh, transform);

		for(MeshPart meshPart : model.meshParts)
			meshPart.update();
	}

	public static void transform(Mesh mesh, Matrix4 transform) {
		transform(mesh, transform, transform.cpy().inv().tra());
	}

	public static void transform(Mesh mesh, Matrix4 transform, Matrix4 normalTransform) {
		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		int posOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;
		VertexAttribute norAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Normal);
		VertexAttribute tanAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Tangent);
		VertexAttribute binAttr = mesh.getVertexAttribute(VertexAttributes.Usage.BiNormal);

		Vector3 vec3 = new Vector3();

		int norOffset = norAttr == null ? -1 : norAttr.offset / 4;
		int tanOffset = tanAttr == null ? -1 : tanAttr.offset / 4;
		int binOffset = binAttr == null ? -1 : binAttr.offset / 4;
		for(int i = 0; i < mesh.getNumVertices(); i++) {
			int offset = i * mesh.getVertexSize() / 4 + posOffset;
			BufferUtil.getVector3(buffer, offset, vec3);
			vec3.mul(transform);
			BufferUtil.putVector3(buffer, offset, vec3);

			if(norOffset != -1) {
				offset = i * mesh.getVertexSize() / 4 + norOffset;
				BufferUtil.getVector3(buffer, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(buffer, offset, vec3);
			}

			if(tanOffset != -1) {
				offset = i * mesh.getVertexSize() / 4 + tanOffset;
				BufferUtil.getVector3(buffer, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(buffer, offset, vec3);
			}

			if(binOffset != -1) {
				offset = i * mesh.getVertexSize() / 4 + binOffset;
				BufferUtil.getVector3(buffer, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(buffer, offset, vec3);
			}
		}
	}

	public static void offset(MeshBuilder meshBuilder, float x, float y, float z) {
		offset(meshBuilder, 0, meshBuilder.getNumVertices(), x, y, z);
	}

	public static void offset(MeshBuilder meshBuilder, int startIndex, int endIndex, float x, float y, float z) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");
		for(int i = startIndex; i < endIndex; i++) {
			int offset = i * meshBuilder.getFloatsPerVertex() + posOffset;
			vertices.set(offset, vertices.get(offset) + x);
			vertices.set(offset + 1, vertices.get(offset + 1) + y);
			vertices.set(offset + 2, vertices.get(offset + 2) + z);
		}
	}

	public static void transform(MeshBuilder meshBuilder,
	                             Matrix4 transform) {
		transform(meshBuilder, transform, transform.cpy().inv().tra());
	}

	public static void transform(MeshBuilder meshBuilder,
	                             Matrix4 transform,
	                             Matrix4 normalTransform) {
		transform(meshBuilder, 0, meshBuilder.getNumVertices(), transform, normalTransform);
	}


	public static void transform(MeshBuilder meshBuilder,
	                             int startIndex,
	                             int endIndex,
	                             Matrix4 transform) {
		transform(meshBuilder, startIndex, endIndex, transform, transform.cpy().inv().tra());
	}

	public static void transform(MeshBuilder meshBuilder,
	                             int startIndex,
	                             int endIndex,
	                             Matrix4 transform,
	                             Matrix4 normalTransform) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		int biNorOffset = ReflectionUtil.get(meshBuilder, "biNorOffset");
		int tangentOffset = ReflectionUtil.get(meshBuilder, "tangentOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		Vector3 vec3 = new Vector3();

		for(int i = startIndex; i < endIndex; i++) {
			int offset = i * meshBuilder.getFloatsPerVertex() + posOffset;
			BufferUtil.getVector3(vertices, offset, vec3);
			vec3.mul(transform);
			BufferUtil.putVector3(vertices, offset, vec3);

			if(norOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + norOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}

			if(biNorOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + biNorOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}

			if(tangentOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + tangentOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}
		}
	}

	public static Vector3 getPosition(MeshBuilder meshBuilder, int vertex, Vector3 out) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		int offset = vertex * meshBuilder.getFloatsPerVertex() + posOffset;
		BufferUtil.getVector3(vertices, offset, out);
		return out;
	}

	public static Vector3 getNormal(MeshBuilder meshBuilder, int vertex, Vector3 out) {
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		if(norOffset == -1)
			throw new IllegalArgumentException("MeshBuilder has no normals");

		int offset = vertex * meshBuilder.getFloatsPerVertex() + norOffset;
		BufferUtil.getVector3(vertices, offset, out);
		return out;
	}

	public static void setNormal(MeshBuilder meshBuilder, int vertex, Vector3 normal) {
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");


		if(norOffset == -1)
			throw new IllegalArgumentException("MeshBuilder has no normals");

		int offset = vertex * meshBuilder.getFloatsPerVertex() + norOffset;
		BufferUtil.putVector3(vertices, offset, normal);
	}

	public static Vector3 getBinormal(MeshBuilder meshBuilder, int vertex, Vector3 out) {
		int biNorOffset = ReflectionUtil.get(meshBuilder, "biNorOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		if(biNorOffset == -1)
			throw new IllegalArgumentException("MeshBuilder has no binormals");

		int offset = vertex * meshBuilder.getFloatsPerVertex() + biNorOffset;
		BufferUtil.getVector3(vertices, offset, out);
		return out;
	}

	public static void setBinormal(MeshBuilder meshBuilder, int vertex, Vector3 normal) {
		int biNorOffset = ReflectionUtil.get(meshBuilder, "biNorOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");


		if(biNorOffset == -1)
			throw new IllegalArgumentException("MeshBuilder has no normals");

		int offset = vertex * meshBuilder.getFloatsPerVertex() + biNorOffset;
		BufferUtil.putVector3(vertices, offset, normal);
	}

	public static Vector3 getTangent(MeshBuilder meshBuilder, int vertex, Vector3 out) {
		int tanOffset = ReflectionUtil.get(meshBuilder, "tangentOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		if(tanOffset == -1)
			throw new IllegalArgumentException("MeshBuilder has no tangents");

		int offset = vertex * meshBuilder.getFloatsPerVertex() + tanOffset;
		BufferUtil.getVector3(vertices, offset, out);
		return out;
	}

	public static void setTangent(MeshBuilder meshBuilder, int vertex, Vector3 normal) {
		int tanOffset = ReflectionUtil.get(meshBuilder, "tangentOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");


		if(tanOffset == -1)
			throw new IllegalArgumentException("MeshBuilder has no tangents");

		int offset = vertex * meshBuilder.getFloatsPerVertex() + tanOffset;
		BufferUtil.putVector3(vertices, offset, normal);
	}

	public static void recomputeNormals(MeshBuilder meshBuilder, IntArray vertexIndices) {
		tmpVerticesTriCount.clear();
		for(int i = 0; i < vertexIndices.size; i++)
			tmpVerticesTriCount.put(vertexIndices.get(i), 0);

		for(int i = 0; i < vertexIndices.size; i++) {
			int idx = vertexIndices.get(i);
			setNormal(meshBuilder, idx, Vector3.Zero);
		}

		ShortArray indices = ReflectionUtil.get(meshBuilder, "indices");
		int triCount = meshBuilder.getNumIndices() / 3;
		for(int tri = 0; tri < triCount; tri++) {
			int v1 = indices.get(tri * 3);
			int v2 = indices.get(tri * 3 + 1);
			int v3 = indices.get(tri * 3 + 2);

			if(!tmpVerticesTriCount.containsKey(v1)
					&& !tmpVerticesTriCount.containsKey(v2)
					&& !tmpVerticesTriCount.containsKey(v3))
				continue;

			getPosition(meshBuilder, v1, tmpTriangle.p1);
			getPosition(meshBuilder, v2, tmpTriangle.p2);
			getPosition(meshBuilder, v3, tmpTriangle.p3);

			Vector3 normal = tmpTriangle.getNormal();

			if(tmpVerticesTriCount.containsKey(v1)) {
				tmpVerticesTriCount.getAndIncrement(v1, 0, 1);
				setNormal(meshBuilder, v1, getNormal(meshBuilder, v1, tmpVec3).add(normal));
			}

			if(tmpVerticesTriCount.containsKey(v2)) {
				tmpVerticesTriCount.getAndIncrement(v2, 0, 1);
				setNormal(meshBuilder, v2, getNormal(meshBuilder, v2, tmpVec3).add(normal));
			}

			if(tmpVerticesTriCount.containsKey(v3)) {
				tmpVerticesTriCount.getAndIncrement(v3, 0, 1);
				setNormal(meshBuilder, v3, getNormal(meshBuilder, v3, tmpVec3).add(normal));
			}
		}

		for(int i = 0; i < vertexIndices.size; i++) {
			int idx = vertexIndices.get(i);
			setNormal(meshBuilder, idx,
					getNormal(meshBuilder, idx, tmpVec3).scl(1f / tmpVerticesTriCount.get(idx, 1)));
		}
		tmpVerticesTriCount.clear();
	}

	public static void dynamicTransform(MeshBuilder meshBuilder,
	                                    int startIndex,
	                                    int endIndex,
	                                    BiConsumer<Vector3, Matrix4> dynamicTransform) {
		dynamicTransform(meshBuilder, startIndex, endIndex,
				(position, transformOut, normalTransformOut) -> {
			dynamicTransform.accept(position, transformOut);
			normalTransformOut.set(transformOut).inv().tra();
		});
	}

	public static void dynamicTransform(MeshBuilder meshBuilder,
	                                    BiConsumer<Vector3, Matrix4> dynamicTransform) {
		dynamicTransform(meshBuilder, (position, transformOut, normalTransformOut) -> {
			dynamicTransform.accept(position, transformOut);
			normalTransformOut.set(transformOut).inv().tra();
		});
	}

	public static void dynamicTransform(MeshBuilder meshBuilder,
	                                    DynamicTransform dynamicTransform) {
		dynamicTransform(meshBuilder, 0, meshBuilder.getNumVertices(), dynamicTransform);
	}

	public static void dynamicTransform(MeshBuilder meshBuilder,
	                                    int startIndex,
	                                    int endIndex,
	                                    DynamicTransform dynamicTransform) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		int biNorOffset = ReflectionUtil.get(meshBuilder, "biNorOffset");
		int tangentOffset = ReflectionUtil.get(meshBuilder, "tangentOffset");
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		Vector3 vec3 = new Vector3();
		Matrix4 transform = new Matrix4();
		Matrix4 normalTransform = new Matrix4();

		for(int i = startIndex; i < endIndex; i++) {
			int offset = i * meshBuilder.getFloatsPerVertex() + posOffset;
			BufferUtil.getVector3(vertices, offset, vec3);
			transform.idt();
			normalTransform.idt();
			dynamicTransform.transform(vec3, transform, normalTransform);
			vec3.mul(transform);
			BufferUtil.putVector3(vertices, offset, vec3);

			if(norOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + norOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}

			if(biNorOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + biNorOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}

			if(tangentOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + tangentOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}
		}
	}

	public static void dynamicTransformInterpolateUVs(MeshBuilder meshBuilder,
	                                                  int startIndex,
	                                                  int endIndex,
	                                                  DynamicTransform dynamicTransform) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		int biNorOffset = ReflectionUtil.get(meshBuilder, "biNorOffset");
		int tangentOffset = ReflectionUtil.get(meshBuilder, "tangentOffset");
		int uvOffset = meshBuilder.getAttributes()
				.getOffset(VertexAttributes.Usage.TextureCoordinates, -1);
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");
		ShortArray indices = ReflectionUtil.get(meshBuilder, "indices");

		Vector2 uv = new Vector2();
		Vector2 uv1 = new Vector2(), uv2 = new Vector2(), uv3 = new Vector2();
		Vector3 vec3 = new Vector3(), delta = new Vector3(), tmpIntersect = new Vector3();
		Matrix4 transform = new Matrix4();
		Matrix4 normalTransform = new Matrix4();
		Triangle tri = new Triangle();
		Plane plane = new Plane();

		Vector3 pos1 = new Vector3(), pos2 = new Vector3(), pos3 = new Vector3();

		for(int i = startIndex; i < endIndex; i++) {
			int offset = i * meshBuilder.getFloatsPerVertex() + posOffset;
			BufferUtil.getVector3(vertices, offset, vec3);
			delta.set(vec3);
			transform.idt();
			normalTransform.idt();
			dynamicTransform.transform(vec3, transform, normalTransform);
			vec3.mul(transform);
			delta.sub(vec3);
			if(uvOffset != -1 && delta.len2() > 0f) {
				uv.setZero();
				int triCount = 0;
				for(int triangle = 0; triangle < meshBuilder.getNumIndices() / 3; triangle++) {
					int v1 = indices.get(triangle * 3);
					int v2 = indices.get(triangle * 3 + 1);
					int v3 = indices.get(triangle * 3 + 2);

					if(v1 != i && v2 != i && v3 != i)
						continue;

					BufferUtil.getVector2(vertices, v1 * meshBuilder.getFloatsPerVertex() + uvOffset, uv1);
					BufferUtil.getVector2(vertices, v2 * meshBuilder.getFloatsPerVertex() + uvOffset, uv2);
					BufferUtil.getVector2(vertices, v3 * meshBuilder.getFloatsPerVertex() + uvOffset, uv3);

					int offset1 = v1 * meshBuilder.getFloatsPerVertex() + posOffset;
					int offset2 = v2 * meshBuilder.getFloatsPerVertex() + posOffset;
					int offset3 = v3 * meshBuilder.getFloatsPerVertex() + posOffset;
					BufferUtil.getVector3(vertices, offset1, pos1);
					BufferUtil.getVector3(vertices, offset2, pos2);
					BufferUtil.getVector3(vertices, offset3, pos3);

					float totalArea = tri.set(pos1, pos2, pos3).getArea();

					plane.set(pos2, tmpIntersect.set(pos3).sub(pos2).crs(tri.getNormal()));
					boolean in1 = plane.testPoint(vec3) != Plane.PlaneSide.Front;

					plane.set(pos1, tmpIntersect.set(pos1).sub(pos3).crs(tri.getNormal()));
					boolean in2 = plane.testPoint(vec3) != Plane.PlaneSide.Front;

					plane.set(pos1, tmpIntersect.set(pos2).sub(pos1).crs(tri.getNormal()));
					boolean in3 = plane.testPoint(vec3) != Plane.PlaneSide.Front;

					plane.set(pos1, tri.getNormal());
					float area1 = tri.set(vec3, pos2, pos3).getArea(plane) * (in1 ? 1f : -1f);
					float area2 = tri.set(pos1, vec3, pos3).getArea(plane) * (in2 ? 1f : -1f);
					float area3 = tri.set(pos1, pos2, vec3).getArea(plane) * (in3 ? 1f : -1f);

					float alpha1 = area1 / totalArea;
					float alpha2 = area2 / totalArea;
					float alpha3 = area3 / totalArea;

					float u = alpha1 * uv1.x + alpha2 * uv2.x + alpha3 * uv3.x;
					float v = alpha1 * uv1.y + alpha2 * uv2.y + alpha3 * uv3.y;
					uv.add(u, v);
					triCount++;
				}

				if(triCount > 0) {
					uv.scl(1f / triCount);

					BufferUtil.putVector2(vertices, i * meshBuilder.getFloatsPerVertex() + uvOffset, uv);
				}
			}

			BufferUtil.putVector3(vertices, offset, vec3);

			if(norOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + norOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}

			if(biNorOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + biNorOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}

			if(tangentOffset != -1) {
				offset = i * meshBuilder.getFloatsPerVertex() + tangentOffset;
				BufferUtil.getVector3(vertices, offset, vec3);
				vec3.mul(normalTransform).nor();
				BufferUtil.putVector3(vertices, offset, vec3);
			}
		}
	}

	public static void addMeshWithOffset(MeshPartBuilder builder, Mesh mesh,
	                                     float x, float y, float z) {
		Vector3 tmpVec3 = new Vector3();
		FloatArray tmpVertices = new FloatArray();
		ShortArray tmpIndices = new ShortArray();

		int numFloats = mesh.getNumVertices() * mesh.getVertexSize() / 4;
		tmpVertices.clear();
		tmpVertices.ensureCapacity(numFloats);
		tmpVertices.size = numFloats;
		mesh.getVertices(tmpVertices.items);

		for(int i = 0; i < mesh.getNumVertices(); i++) {
			int offset = i * mesh.getVertexSize() / 4 +
					mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;

			BufferUtil.getVector3(tmpVertices, offset, tmpVec3);
			tmpVec3.add(x, y, z);
			BufferUtil.putVector3(tmpVertices, offset, tmpVec3);
		}

		tmpIndices.clear();
		tmpIndices.ensureCapacity(mesh.getNumIndices());
		tmpIndices.size = mesh.getNumIndices();
		mesh.getIndices(0, mesh.getNumIndices(), tmpIndices.items, 0);

		builder.addMesh(tmpVertices.items, tmpIndices.items, 0, mesh.getNumIndices());
	}

	public static void addMeshWithTransform(MeshPartBuilder builder, Mesh mesh, Matrix4 transform) {
		addMeshWithTransform(builder, mesh, transform, false);
	}

	public static void addMeshWithTransform(MeshPartBuilder builder,
	                                        Mesh mesh,
	                                        Matrix4 transform,
	                                        boolean flipTriangles) {
		if(mesh.getNumVertices() == 0 || mesh.getNumIndices() == 0)
			return;

		Vector3 tmpVec3 = new Vector3();
		FloatArray tmpVertices = new FloatArray();
		ShortArray tmpIndices = new ShortArray();
		Matrix4 invTrans = transform.cpy().inv().tra();

		int numFloats = mesh.getNumVertices() * mesh.getVertexSize() / 4;
		tmpVertices.clear();
		tmpVertices.ensureCapacity(numFloats);
		tmpVertices.size = numFloats;
		mesh.getVertices(tmpVertices.items);

		for(int i = 0; i < mesh.getNumVertices(); i++) {
			int offset = i * mesh.getVertexSize() / 4 +
					mesh.getVertexAttribute(VertexAttributes.Usage.Position).offset / 4;

			BufferUtil.getVector3(tmpVertices, offset, tmpVec3);
			tmpVec3.mul(transform);
			BufferUtil.putVector3(tmpVertices, offset, tmpVec3);

			if(mesh.getVertexAttribute(VertexAttributes.Usage.Normal) != null) {
				int norOffset = i * mesh.getVertexSize() / 4 + mesh.getVertexAttribute(VertexAttributes.Usage.Normal).offset / 4;

				BufferUtil.getVector3(tmpVertices, norOffset, tmpVec3);
				tmpVec3.mul(invTrans);
				BufferUtil.putVector3(tmpVertices, norOffset, tmpVec3);
			}
		}

		tmpIndices.clear();
		tmpIndices.ensureCapacity(mesh.getNumIndices());
		tmpIndices.size = mesh.getNumIndices();
		mesh.getIndices(0, mesh.getNumIndices(), tmpIndices.items, 0);

		if(flipTriangles) {
			for(int tri = 0; tri < tmpIndices.size / 3; tri++) {
				short v1 = tmpIndices.get(tri * 3);
				short v2 = tmpIndices.get(tri * 3 + 1);
				tmpIndices.set(tri * 3, v2);
				tmpIndices.set(tri * 3 + 1, v1);
			}
		}

		builder.addMesh(tmpVertices.items, tmpIndices.items, 0, tmpIndices.size);
	}

	public static void rect(MeshPartBuilder meshBuilder,
	                         float x1, float y1, float z1,
	                         float x2, float y2, float z2,
	                         float x3, float y3, float z3,
	                         float x4, float y4, float z4,
	                         float nx, float ny, float nz,
	                         float u1, float v1,
	                         float u2, float v2) {
		MeshPartBuilder.VertexInfo fVert1 = new MeshPartBuilder.VertexInfo();
		MeshPartBuilder.VertexInfo fVert2 = new MeshPartBuilder.VertexInfo();
		MeshPartBuilder.VertexInfo fVert3 = new MeshPartBuilder.VertexInfo();
		MeshPartBuilder.VertexInfo fVert4 = new MeshPartBuilder.VertexInfo();

		fVert1.setNor(nx, ny, nz);
		fVert2.setNor(nx, ny, nz);
		fVert3.setNor(nx, ny, nz);
		fVert4.setNor(nx, ny, nz);

		fVert1.setPos(x1, y1, z1).setUV(u1, v2);
		fVert2.setPos(x2, y2, z2).setUV(u2, v2);
		fVert3.setPos(x3, y3, z3).setUV(u2, v1);
		fVert4.setPos(x4, y4, z4).setUV(u1, v1);

		meshBuilder.ensureVertices(4);
		meshBuilder.rect(fVert1,
				fVert2,
				fVert3,
				fVert4);
	}

	public static void setAllUVs(Mesh mesh, float u, float v) {
		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		int uvOffset = mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates).offset;

		for(int i = 0; i < mesh.getNumVertices(); i++) {
			//noinspection RedundantCast
			((Buffer)buffer).position(i * mesh.getVertexSize() / 4 + uvOffset / 4);
			buffer.put(u);
			buffer.put(v);
		}
	}

	public static void setAllUVs(MeshBuilder meshBuilder, float u, float v) {
		setAllUVs(meshBuilder, 0, u, v);
	}

	public static void setAllUVs(MeshBuilder meshBuilder, int startIndex, float u, float v) {
		setAllUVs(meshBuilder, startIndex, meshBuilder.getNumVertices(), u, v);
	}

	public static void setAllUVs(MeshBuilder meshBuilder, int startIndex, int endIndex, float u, float v) {
		FloatArray buffer = ReflectionUtil.get(meshBuilder, "vertices");
		int uvOffset = meshBuilder.getAttributes().findByUsage(VertexAttributes.Usage.TextureCoordinates).offset;

		for(int i = startIndex; i < endIndex; i++) {
			int pos = i * meshBuilder.getFloatsPerVertex() + uvOffset / 4;
			buffer.set(pos, u);
			buffer.set(pos + 1, v);
		}
	}

	public static void transformUVs(Mesh mesh, TextureRegion region) {
		transformUVs(mesh, 0, mesh.getNumVertices(), region);
	}

	public static void transformUVs(Mesh mesh, int startIndex, int endIndex, TextureRegion region) {
		transformUVs(mesh, startIndex, endIndex,
				region.getU(), region.getV(), region.getU2(), region.getV2());
	}

	public static void transformUVs(Mesh mesh, float u1, float v1, float u2, float v2) {
		transformUVs(mesh, 0, mesh.getNumVertices(), u1, v1, u2, v2);
	}

	public static void transformUVs(Mesh mesh, int startIndex, int endIndex, float u1, float v1, float u2, float v2) {
		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		int uvOffset = mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates).offset;

		for(int i = startIndex; i < endIndex; i++) {
			int index = i * mesh.getVertexSize() / 4 + uvOffset / 4;
			//noinspection RedundantCast
			((Buffer)buffer).position(index);
			float u = buffer.get();
			float v = buffer.get();
			//noinspection RedundantCast
			((Buffer)buffer).position(index);
			buffer.put(u * (u2 - u1) + u1);
			buffer.put(v * (v2 - v1) + v1);
		}
	}

	public static void transformUVs(MeshBuilder meshBuilder, TextureRegion region) {
		transformUVs(meshBuilder, 0, meshBuilder.getNumVertices(), region);
	}

	public static void transformUVs(MeshBuilder meshBuilder, int startIndex, int endIndex, TextureRegion region) {
		transformUVs(meshBuilder, startIndex, endIndex,
				region.getU(), region.getV(), region.getU2(), region.getV2());
	}

	public static void transformUVs(MeshBuilder meshBuilder, float u1, float v1, float u2, float v2) {
		transformUVs(meshBuilder, 0, meshBuilder.getNumVertices(), u1, v1, u2, v2);
	}

	public static void transformUVs(MeshBuilder meshBuilder, int startIndex, int endIndex, float u1, float v1, float u2, float v2) {
		FloatArray buffer = ReflectionUtil.get(meshBuilder, "vertices");
		int uvOffset = meshBuilder.getAttributes().findByUsage(VertexAttributes.Usage.TextureCoordinates).offset;

		for(int i = startIndex; i < endIndex; i++) {
			int pos = i * meshBuilder.getFloatsPerVertex() + uvOffset / 4;
			buffer.set(pos, buffer.get(pos) * (u2 - u1) + u1);
			buffer.set(pos + 1, buffer.get(pos + 1) * (v2 - v1) + v1);
		}
	}

	public static void transformConeUVsToCircular(MeshBuilder meshBuilder,
	                                              int startIndex,
	                                              int endIndex) {
		FloatArray buffer = ReflectionUtil.get(meshBuilder, "vertices");
		int uvOffset = meshBuilder.getAttributes().findByUsage(VertexAttributes.Usage.TextureCoordinates).offset;

		for(int i = startIndex; i < endIndex; i++) {
			int pos = i * meshBuilder.getFloatsPerVertex() + uvOffset / 4;
			float u = buffer.get(pos);
			float v = buffer.get(pos + 1);

			if(u == 0.5f && v == 0f) {
				v += 0.5f;
			} else if(v == 1f) {
				v = (MathUtils.sin(u * MathUtils.PI2) + 1f) / 2f;
				u = (MathUtils.cos(u * MathUtils.PI2) + 1f) / 2f;
			} else
				throw new IllegalArgumentException("Provided meshBuilder has non cone UVs " +
						"(" + u + ", " + v + ")");

			buffer.set(pos, u);
			buffer.set(pos + 1, v);
		}
	}

	public static int indexOfFirstMatchingNormal(Mesh mesh,
	                                             Predicate<Vector3> predicate) {
		return indexOfFirstMatchingNormal(mesh, 0, predicate);
	}

	public static int indexOfFirstMatchingNormal(Mesh mesh,
	                                             int startIndex,
	                                             Predicate<Vector3> predicate) {
		FloatBuffer buffer = mesh.getVerticesBuffer(true);
		int norOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Normal).offset;
		Vector3 normal = new Vector3();

		for(int i = startIndex; i < mesh.getNumVertices(); i++) {
			//noinspection RedundantCast
			((Buffer)buffer).position(i * mesh.getVertexSize() / 4 + norOffset / 4);
			float x = buffer.get();
			float y = buffer.get();
			float z = buffer.get();

			normal.set(x, y, z);

			if(predicate.test(normal)) {
				return i;
			}
		}

		return -1;
	}

	public static int indexOfFirstMatchingNormal(MeshBuilder meshBuilder,
	                                             Predicate<Vector3> predicate) {
		return indexOfFirstMatchingNormal(meshBuilder, 0, predicate);
	}

	public static int indexOfFirstMatchingNormal(MeshBuilder meshBuilder,
	                                             int startIndex,
	                                             Predicate<Vector3> predicate) {
		FloatArray buffer = ReflectionUtil.get(meshBuilder, "vertices");
		int norOffset = meshBuilder.getAttributes().findByUsage(VertexAttributes.Usage.Normal).offset;
		Vector3 normal = new Vector3();

		for(int i = startIndex; i < meshBuilder.getNumVertices(); i++) {
			int index = i * meshBuilder.getFloatsPerVertex() + norOffset / 4;
			float x = buffer.get(index);
			float y = buffer.get(index + 1);
			float z = buffer.get(index + 2);

			normal.set(x, y, z);

			if(predicate.test(normal)) {
				return i;
			}
		}

		return -1;
	}

	public static Model createTwoTexturedCylinder(ModelBuilder builder,
	                                              float width, float height, float depth,
	                                              int divisions,
												  Material material,
												  long attributes,
	                                              TextureRegion sideRegion,
	                                              TextureRegion topRegion) {
		Model cylinder = builder.createCylinder(width, height, depth, divisions,
				material, attributes);
		int index = MeshBuilderUtil.indexOfFirstMatchingNormal(cylinder.meshes.get(0),
				nor -> abs(nor.y) == 1f);

		if(index == -1)
			throw new IllegalStateException("Failure to identify index of start of top and " +
					"bottom in generated cylinder mesh");
		MeshBuilderUtil.transformUVs(cylinder.meshes.get(0), 0, index, sideRegion);
		MeshBuilderUtil.transformUVs(cylinder.meshes.get(0), index,
				cylinder.meshes.get(0).getNumVertices(), topRegion);
		return cylinder;
	}

	public static void createTwoTexturedCylinder(MeshBuilder builder,
	                                             float width, float height, float depth,
	                                             int divisions,
	                                             TextureRegion sideRegion,
	                                             TextureRegion topRegion) {
		int startIndex = builder.getNumVertices();
		CylinderShapeBuilder.build(builder, width, height, depth, divisions);
		int index = MeshBuilderUtil.indexOfFirstMatchingNormal(builder, startIndex,
				nor -> abs(nor.y) == 1f);

		if(index == -1)
			throw new IllegalStateException("Failure to identify index of start of top and " +
					"bottom in generated cylinder mesh");
		MeshBuilderUtil.transformUVs(builder, startIndex, index, sideRegion);
		MeshBuilderUtil.transformUVs(builder, index, builder.getNumVertices(), topRegion);
	}

	public static void createTippedCylinder(MeshBuilder builder,
	                                        float width, float height, float depth,
	                                        float tipHeight,
	                                        int divisions,
	                                        TextureRegion sideRegion,
	                                        TextureRegion topTipRegion,
	                                        TextureRegion bottomTipRegion,
											boolean flipBottomRegion,
	                                        TextureRegion topRegion) {
		if(height < tipHeight * 2f)
			throw new IllegalArgumentException("height must be greater than twice the tipHeight");

		Matrix4 tmpMat4 = new Matrix4();

		int startIndex = builder.getNumVertices();
		CylinderShapeBuilder.build(builder, width, height - tipHeight * 2f, depth, divisions, 0f, 360f, false);
		MeshBuilderUtil.transform(builder, tmpMat4.setToTranslation(0f, -height / 2f + tipHeight / 2f, 0f));
		int sideIndex = builder.getNumVertices();

		CylinderShapeBuilder.build(builder, width, tipHeight, depth, divisions, 0f, 360f, false);
		MeshBuilderUtil.transform(builder, tmpMat4.setToTranslation(0f, height - tipHeight, 0f));
		int tipIndex = builder.getNumVertices();
		CylinderShapeBuilder.build(builder, width, tipHeight, depth, divisions, 0f, 360f, false);
		MeshBuilderUtil.transform(builder, tmpMat4.setToTranslation(0f, -height / 2f + tipHeight / 2f, 0f));
		int bottomTipIndex = builder.getNumVertices();

		EllipseShapeBuilder.build(builder,
				width, depth, 0, 0, divisions,
				0, height / 2f, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0f, 360f);
		EllipseShapeBuilder.build(builder,
				width, depth, 0, 0, divisions,
				0, -height / 2f, 0, 0, -1, 0, -1, 0, 0, 0, 0, 1, -180f, 180f);

		MeshBuilderUtil.transformUVs(builder, startIndex, sideIndex, sideRegion);
		MeshBuilderUtil.transformUVs(builder, sideIndex, tipIndex, topTipRegion);
		MeshBuilderUtil.transformUVs(builder, tipIndex, bottomTipIndex, bottomTipRegion);
		if(flipBottomRegion)
			MeshBuilderUtil.transformUVs(builder, tipIndex, bottomTipIndex, 0f, 1f, 1f, 0f);
		MeshBuilderUtil.transformUVs(builder, bottomTipIndex, builder.getNumVertices(), topRegion);
	}

	public static void createSixSidedBox(MeshBuilder builder,
	                                     float width, float height, float depth) {
		Vector3 position = new Vector3();
		Vector3 perp1 = new Vector3();
		Vector3 perp2 = new Vector3();
		short[] corners = new short[4];
		for(AxisDirection6 face : AxisDirection6.values) {
			perp1.set(face.asVector().y, face.asVector().z, face.asVector().x);
			perp2.set(face.asVector().z, face.asVector().x, face.asVector().y);

			int i = 0;
			for(GridCorner corner : GridCorner.values) {
				position.set(face.asVector())
						.mulAdd(perp1, corner.x())
						.mulAdd(perp2, corner.y())
						.scl(width / 2f, height / 2f, depth / 2f);
				float UV1 = (corner.x() + 1f) / 2f;
				float UV2 = (corner.y() + 1f) / 2f;
				boolean firstIsY = perp1.y != 0f;
				corners[i] = builder.vertex(position,
						face.asVector(),
						Color.WHITE,
						new Vector2(firstIsY ? UV2 : UV1, firstIsY ? UV1 : UV2));
				i++;
			}

			boolean flip = face.ordinal() > 2;

			builder.rect(corners[0],
					flip ? corners[2] : corners[1],
					corners[3],
					flip ? corners[1] : corners[2]);
		}
	}

	public static Attribute[] withoutNulls(Attribute... attributes) {
		int nonNullCount = 0;
		for(Attribute attr : attributes)
			if(attr != null)
				nonNullCount++;
		Attribute[] attrs = new Attribute[nonNullCount];
		nonNullCount = 0;
		for(Attribute attr : attributes)
			if(attr != null)
				attrs[nonNullCount++] = attr;
		return attrs;
	}

	public static void dynamicUVTransform(MeshBuilder partBuilder,
	                                      DynamicUVTransform uvTransform) {
		dynamicUVTransform(partBuilder, 0, partBuilder.getNumVertices(), uvTransform);
	}

	public static void dynamicUVTransform(MeshBuilder meshBuilder,
	                                      int startIndex,
	                                      int endIndex,
	                                      DynamicUVTransform uvTransform) {
		int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
		int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
		int uvOffset = meshBuilder.getAttributes().findByUsage(VertexAttributes.Usage.TextureCoordinates).offset / 4;
		FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");

		Vector3 pos = new Vector3(), nor = new Vector3();
		Vector2 uv = new Vector2();

		for(int i = startIndex; i < endIndex; i++) {
			int offset = i * meshBuilder.getFloatsPerVertex() + posOffset;
			BufferUtil.getVector3(vertices, offset, pos);
			offset = i * meshBuilder.getFloatsPerVertex() + norOffset;
			BufferUtil.getVector3(vertices, offset, nor);

			offset = i * meshBuilder.getFloatsPerVertex() + uvOffset;
			BufferUtil.getVector2(vertices, offset, uv);
			uvTransform.transform(pos, nor, uv);
			BufferUtil.putVector2(vertices, offset, uv);
		}
	}


	/**
	 * Subdivides the edge by adding a new vertex at the mid point. See
	 * {@link #subdivideEdge(MeshBuilder, short, short, MeshPartBuilder.VertexInfo)} for more info.
	 *
	 * @see #subdivideEdge(MeshBuilder, short, short, MeshPartBuilder.VertexInfo)
	 * @param meshBuilder mesh builder in which to subdivide edge
	 * @param vA id of first vertex in the edge
	 * @param vB id of second vertex in the edge
	 * @return id of vertex that was added or -1 if the edge was not found
	 */
	public static short subdivideEdge(MeshBuilder meshBuilder,
	                                 short vA,
	                                 short vB) {
		return subdivideEdge(meshBuilder, vA, vB, null);
	}

	/**
	 * Subdivides an edge in a mesh builder by adding a new vertex, deleting 2 faces and creating 4
	 * new faces, e.g.
	 * <pre>
	 *     .             .
	 *    / \           /|\
	 *   /   \         / | \
	 *  .-----.  -->  .--.--.
	 *   \   /         \ | /
	 *    \ /           \|/
	 *     .             .
	 * </pre>
	 *
	 * @param meshBuilder mesh builder in which to subdivide edge
	 * @param vA id of first vertex in the edge
	 * @param vB id of second vertex in the edge
	 * @param newVertex new vertex, or null to create one at the midpoint
	 * @return id of vertex that was added or -1 if the edge was not found
	 */
	public static short subdivideEdge(MeshBuilder meshBuilder,
	                                 short vA,
	                                 short vB,
	                                 MeshPartBuilder.VertexInfo newVertex) {
		ensureNotNull(meshBuilder, "meshBuilder");
		if(vA == vB)
			throw new IllegalArgumentException("Vertices A and B must be the bounds of an edge " +
					"and cannot be the same");

		if(newVertex == null) {
			FloatArray vertices = ReflectionUtil.get(meshBuilder, "vertices");
			int posOffset = ReflectionUtil.get(meshBuilder, "posOffset");
			int norOffset = ReflectionUtil.get(meshBuilder, "norOffset");
			int uvOffset = ReflectionUtil.get(meshBuilder, "uvOffset");

			newVertex = new MeshPartBuilder.VertexInfo();
			Vector3 v1 = new Vector3();
			Vector3 v2 = new Vector3();
			Vector2 uv1 = new Vector2();
			Vector2 uv2 = new Vector2();

			if(posOffset != -1) {
				BufferUtil.getVector3(vertices, vA * meshBuilder.getFloatsPerVertex() + posOffset, v1);
				BufferUtil.getVector3(vertices, vB * meshBuilder.getFloatsPerVertex() + posOffset, v2);
				newVertex.position.set(v1).scl(0.5f).mulAdd(v2, 0.5f);
				newVertex.hasPosition = true;
			}

			if(norOffset != -1) {
				BufferUtil.getVector3(vertices, vA * meshBuilder.getFloatsPerVertex() + norOffset, v1);
				BufferUtil.getVector3(vertices, vB * meshBuilder.getFloatsPerVertex() + norOffset, v2);
				newVertex.normal.set(v1).scl(0.5f).mulAdd(v2, 0.5f).nor();
				newVertex.hasNormal = true;
			}

			if(uvOffset != -1) {
				BufferUtil.getVector2(vertices, vA * meshBuilder.getFloatsPerVertex() + uvOffset, uv1);
				BufferUtil.getVector2(vertices, vB * meshBuilder.getFloatsPerVertex() + uvOffset, uv2);
				newVertex.uv.set(uv1).scl(0.5f).mulAdd(uv2, 0.5f);
				newVertex.hasUV = true;
			}
		}

		short vN = -1;

		ShortArray indices = ReflectionUtil.get(meshBuilder, "indices");
		int triCount = meshBuilder.getNumIndices() / 3;
		for(int tri = 0; tri < triCount; tri++) {
			short v1 = indices.get(tri * 3);
			short v2 = indices.get(tri * 3 + 1);
			short v3 = indices.get(tri * 3 + 2);

			boolean vApresent = vA == v1 || vA == v2 || vA == v3;
			boolean vBpresent = vB == v1 || vB == v2 || vB == v3;

			if(!vApresent || !vBpresent)
				continue;

			if(vN == -1)
				vN = meshBuilder.vertex(newVertex);

			if(vA == v1)
				indices.set(tri * 3, vN);
			else if(vA == v2)
				indices.set(tri * 3 + 1, vN);
			else
				indices.set(tri * 3 + 2, vN);

			if(vB == v1)
				indices.add(vN, v2, v3);
			else if(vB == v2)
				indices.add(v1, vN, v3);
			else
				indices.add(v1, v2, vN);
		}

		return vN;
	}

	public class Vertex {
		public Vector3 position, normal, binormal, tangent;
		public Vector2 uv;
	}

	@FunctionalInterface
	public interface DynamicTransform {
		void transform(Vector3 position,
		               Matrix4 transformOut,
		               Matrix4 normalTransformOut);
	}

	@FunctionalInterface
	public interface DynamicUVTransform {
		void transform(Vector3 position, Vector3 normal, Vector2 uv);
	}

}
