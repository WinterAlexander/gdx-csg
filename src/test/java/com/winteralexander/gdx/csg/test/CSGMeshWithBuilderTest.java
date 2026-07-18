package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.BoxShapeBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.CylinderShapeBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.winteralexander.gdx.csg.CSGConfiguration;
import com.winteralexander.gdx.csg.CSGMesh;
import com.winteralexander.gdx.csg.CSGUtil;
import com.winteralexander.gdx.csg.test.debugviewer.CSGMeshViewer;
import com.winteralexander.gdx.csg.test.debugviewer.ModelViewer;
import com.winteralexander.gdx.utils.collection.CollectionUtil;
import org.junit.Test;

/**
 * Tests the CSGMesh operations with a libGDX model builder, which is convenient since it requires
 * no GPU initialization
 * <p>
 * Created on 2026-06-26.
 *
 * @author Alexander Winter
 */
public class CSGMeshWithBuilderTest {
	@Test
	public void testBuildWrench() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("wrench",
				GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.Tangent,
				new Material());

		CylinderShapeBuilder.build(partBuilder, 0.15f, 0.02f, 0.15f, 10);
		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder);
		csgMesh.getVertices().forEach(v -> v.getPosition().add(0f, 0f, -0.5f));

		int idxStart = partBuilder.getNumVertices();
		CylinderShapeBuilder.build(partBuilder, 0.075f, 0.02f, 0.075f, 10);

		CSGMesh substrahend = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(idxStart, partBuilder.getNumVertices()));
		substrahend.getVertices().forEach(v -> v.getPosition().add(-0.05f, 0f, -0.5f));

		CSGMesh result = CSGUtil.subtraction(csgMesh, substrahend, new CSGConfiguration() {
			{
				tolerance = 1e-5f;
			}
		});
		result.toBuilder(partBuilder,
				CollectionUtil.arrayFromRange(0, partBuilder.getNumVertices()));
		// test is to ensure this doesn't crash
		// ModelViewer.start(builder);
	}

	@Test
	public void testBuildWrench2() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("wrench",
				GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.Tangent,
				new Material());

		CylinderShapeBuilder.build(partBuilder, 15f, 2f, 15f, 10);
		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder);
		csgMesh.getVertices().forEach(v -> v.getPosition().add(0f, 0f, -50f));

		int idxStart = partBuilder.getNumVertices();
		CylinderShapeBuilder.build(partBuilder, 7.5f, 2f, 7.5f, 10);

		CSGMesh substrahend = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(idxStart, partBuilder.getNumVertices()));
		substrahend.getVertices().forEach(v -> v.getPosition().add(-5f, 0f, -50f));

		CSGMesh result = CSGUtil.subtraction(csgMesh, substrahend, new CSGConfiguration());
		result.toBuilder(partBuilder,
				CollectionUtil.arrayFromRange(0, partBuilder.getNumVertices()));
		// test is to ensure this doesn't crash
		// ModelViewer.start(builder);
	}

	@Test
	public void testBuildWrenchTwoInARow() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("wrench",
				GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.Tangent,
				new Material());

		CylinderShapeBuilder.build(partBuilder, 0.15f, 0.025f, 0.15f, 10);
		CSGMesh csgMesh = CSGMesh.fromBuilder(partBuilder);

		int idxStart = partBuilder.getNumVertices();
		CylinderShapeBuilder.build(partBuilder, 0.12f, 0.025f, 0.12f, 10);

		CSGMesh substrahend = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(idxStart, partBuilder.getNumVertices()));
		substrahend.getVertices().forEach(v -> v.getPosition().add(0f, 0f, -0.03f));

		CSGMesh result = CSGUtil.subtraction(csgMesh, substrahend, new CSGConfiguration() {
			{
				tolerance = 1e-5f;
			}
		});
		result.toBuilder(partBuilder,
				CollectionUtil.arrayFromRange(0, partBuilder.getNumVertices()));

		int startSecond = partBuilder.getNumVertices();
		CylinderShapeBuilder.build(partBuilder, 0.15f, 0.02f, 0.15f, 10);
		csgMesh = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(startSecond, partBuilder.getNumVertices()));
		csgMesh.getVertices().forEach(v -> v.getPosition().add(0f, 0f, -0.5f));

		int startSecond2 = partBuilder.getNumVertices();
		CylinderShapeBuilder.build(partBuilder, 0.075f, 0.02f, 0.075f, 10);

		substrahend = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(startSecond2, partBuilder.getNumVertices()));
		substrahend.getVertices().forEach(v -> v.getPosition().add(-0.05f, 0f, -0.5f));

		result = CSGUtil.subtraction(csgMesh, substrahend, new CSGConfiguration() {
			{
				tolerance = 1e-5f;
			}
		});
		result.toBuilder(partBuilder,
				CollectionUtil.arrayFromRange(startSecond, partBuilder.getNumVertices()));
		// test is to ensure this doesn't crash
		// ModelViewer.start(builder);
	}

	@Test
	public void testCutHouse() {
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		MeshBuilder partBuilder = (MeshBuilder)builder.part("house",
				GL20.GL_TRIANGLES,
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.Tangent
						| VertexAttributes.Usage.TextureCoordinates,
				new Material());

		Vector3 tmpV0 = new Vector3();
		Vector3 tmpV1 = new Vector3();
		Vector3 tmpV2 = new Vector3();
		Vector3 tmpV3 = new Vector3();
		Vector3 tmpV4 = new Vector3();
		Matrix4 matTmp1 = new Matrix4();

		float BOTTOM_WIDTH = 1.1f;
		float TOP_WIDTH = 1f;
		float ROOF_WIDTH = 1.3f;
		float DOOR_WIDTH = 0.25f;
		float DOOR_HEIGHT = 0.5f;
		float WINDOW_WIDTH = 0.25f;
		float WINDOW_HEIGHT = 0.35f;

		float BODY_HEIGHT = 0.8f;
		float ROOF_HEIGHT = 0.5f;

		int startIdx = partBuilder.getNumVertices();

		tmpV0.set(0f, -1f, 0f);
		tmpV1.set(-BOTTOM_WIDTH / 2f, 0f, -BOTTOM_WIDTH / 2f);
		tmpV2.set(BOTTOM_WIDTH / 2f, 0f, -BOTTOM_WIDTH / 2f);
		tmpV3.set(-BOTTOM_WIDTH / 2f, 0f, BOTTOM_WIDTH / 2f);
		tmpV4.set(BOTTOM_WIDTH / 2f, 0f, BOTTOM_WIDTH / 2f);
		MeshBuilderUtil.quad(partBuilder, tmpV1, tmpV2, tmpV4, tmpV3, tmpV0);

		tmpV0.set(0f, 0f, 1f);
		tmpV1.set(-BOTTOM_WIDTH / 2f, 0f, BOTTOM_WIDTH / 2f);
		tmpV2.set(BOTTOM_WIDTH / 2f, 0f, BOTTOM_WIDTH / 2f);
		tmpV3.set(-TOP_WIDTH / 2f, BODY_HEIGHT, TOP_WIDTH / 2f);
		tmpV4.set(TOP_WIDTH / 2f, BODY_HEIGHT, TOP_WIDTH / 2f);
		matTmp1.idt().setToRotation(0f, 1f, 0f, 90f);

		for(int i = 0; i < 4; i++) {
			MeshBuilderUtil.quad(partBuilder, tmpV1, tmpV2, tmpV4, tmpV3, tmpV0);

			tmpV0.mul(matTmp1);
			tmpV1.mul(matTmp1);
			tmpV2.mul(matTmp1);
			tmpV3.mul(matTmp1);
			tmpV4.mul(matTmp1);
		}

		tmpV1.set(-ROOF_WIDTH / 2f, BODY_HEIGHT, -ROOF_WIDTH / 2f);
		tmpV2.set(-ROOF_WIDTH / 2f, BODY_HEIGHT, ROOF_WIDTH / 2f);
		tmpV3.set(0f, BODY_HEIGHT + ROOF_HEIGHT, -ROOF_WIDTH / 2f);
		tmpV4.set(0f, BODY_HEIGHT + ROOF_HEIGHT, ROOF_WIDTH / 2f);
		tmpV0.set(tmpV1).sub(tmpV2).crs(tmpV3.cpy().sub(tmpV2)).scl(-1f).nor();
		matTmp1.idt().setToRotation(0f, 1f, 0f, 180f);

		MeshBuilderUtil.quad(partBuilder, tmpV1, tmpV2, tmpV4, tmpV3, tmpV0);
		tmpV0.mul(matTmp1);
		tmpV1.mul(matTmp1);
		tmpV2.mul(matTmp1);
		tmpV3.mul(matTmp1);
		tmpV4.mul(matTmp1);
		MeshBuilderUtil.quad(partBuilder, tmpV1, tmpV2, tmpV4, tmpV3, tmpV0);

		tmpV1.set(-ROOF_WIDTH / 2f, BODY_HEIGHT, ROOF_WIDTH / 2f);
		tmpV2.set(ROOF_WIDTH / 2f, BODY_HEIGHT, ROOF_WIDTH / 2f);
		tmpV3.set(0f, BODY_HEIGHT + ROOF_HEIGHT, ROOF_WIDTH / 2f);
		matTmp1.idt().setToRotation(0f, 1f, 0f, 180f);

		int triIndex = partBuilder.getNumVertices();

		partBuilder.triangle(tmpV1, tmpV2, tmpV3);
		tmpV1.mul(matTmp1);
		tmpV2.mul(matTmp1);
		tmpV3.mul(matTmp1);
		partBuilder.triangle(tmpV1, tmpV2, tmpV3);

		int startChimney = partBuilder.getNumVertices();
		MeshBuilderUtil.createSixSidedBox(partBuilder, 0.15f, 0.4f, 0.15f);
		int startChimney2 = partBuilder.getNumVertices();
		MeshBuilderUtil.createSixSidedBox(partBuilder, 0.18f, 0.1f, 0.18f);

		MeshBuilderUtil.transform(partBuilder,
				startChimney,
				startChimney2,
				matTmp1.idt().translate(0.3f, 1.2f, -0.3f));
		MeshBuilderUtil.transform(partBuilder,
				startChimney2,
				partBuilder.getNumVertices(),
				matTmp1.idt().translate(0.3f, 1.3f, -0.3f));

		CSGMesh base = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(startIdx, partBuilder.getNumVertices()));

		int doorStartIdx = partBuilder.getNumVertices();
		MeshBuilderUtil.createSixSidedBox(partBuilder, 1f, 1f, 1f);
		int doorEndIdx = partBuilder.getNumVertices();

		MeshBuilderUtil.dynamicUVTransform(partBuilder,
				doorStartIdx,
				doorEndIdx,
				(position, normal, uv) -> {
					if(normal.z == -1f)
						return;
					uv.x = 0f;
					uv.y = 0f;
				});
		MeshBuilderUtil.transform(partBuilder,
				doorStartIdx,
				doorEndIdx,
				matTmp1.idt()
						.translate(0f, DOOR_HEIGHT / 2f + 0.01f, BOTTOM_WIDTH / 2f)
						.scl(DOOR_WIDTH, DOOR_HEIGHT, DOOR_WIDTH / 2f));

		int windowStartIdx = partBuilder.getNumVertices();
		MeshBuilderUtil.createSixSidedBox(partBuilder,
				WINDOW_WIDTH,
				WINDOW_HEIGHT,
				WINDOW_WIDTH / 2f);
		int windowEndIdx = partBuilder.getNumVertices();
		MeshBuilderUtil.dynamicUVTransform(partBuilder,
				windowStartIdx,
				windowEndIdx,
				(position, normal, uv) -> {
					if(normal.z == -1f)
						return;
					uv.x = 0f;
					uv.y = 0f;
				});
		MeshBuilderUtil.transform(partBuilder,
				windowStartIdx,
				windowEndIdx,
				matTmp1.idt().translate(0.325f, DOOR_HEIGHT, BOTTOM_WIDTH / 2f));

		int window2StartIdx = partBuilder.getNumVertices();
		MeshBuilderUtil.createSixSidedBox(partBuilder,
				WINDOW_WIDTH,
				WINDOW_HEIGHT,
				WINDOW_WIDTH / 2f);
		int window2EndIdx = partBuilder.getNumVertices();
		MeshBuilderUtil.dynamicUVTransform(partBuilder,
				window2StartIdx,
				window2EndIdx,
				(position, normal, uv) -> {
					if(normal.z == -1f)
						return;
					uv.x = 0f;
					uv.y = 0f;
				});
		MeshBuilderUtil.transform(partBuilder,
				window2StartIdx,
				window2EndIdx,
				matTmp1.idt().translate(-0.325f, DOOR_HEIGHT, BOTTOM_WIDTH / 2f));

		CSGMesh subtrahend = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(doorStartIdx, partBuilder.getNumVertices()));

		CSGUtil.subtraction(base,
					   subtrahend,
					   new CSGConfiguration() {
						   {
							   insideTestDirection.set(1f, 0f, 0f);
						   }
					   })
				.toBuilder(partBuilder,
						CollectionUtil.arrayFromRange(startIdx, partBuilder.getNumVertices()));

		MeshBuilderUtil.transform(partBuilder, matTmp1.idt().scale(0.5f, 0.5f, 0.5f));
		MeshBuilderUtil.transform(partBuilder, matTmp1.idt().translate(0f, 0.2f, -0.7f));

		int houseEnd = partBuilder.getNumVertices();
		CSGMesh house = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(0, houseEnd));

		BoxShapeBuilder.build(partBuilder, 1f, 1f, 1f);
		MeshBuilderUtil.transform(partBuilder, matTmp1.idt().translate(0f, 0f, -1f));

		CSGMesh cube = CSGMesh.fromBuilder(partBuilder,
				CollectionUtil.arrayFromRange(houseEnd, partBuilder.getNumVertices()));

		CSGMesh result = CSGUtil.subtraction(house, cube, new CSGConfiguration() {
			{
				insideTestDirection.set(1f, 0f, 0f);
			}
		});
		// CSGMeshViewer.start(result);

		result.toBuilder(partBuilder,
				CollectionUtil.arrayFromRange(0, partBuilder.getNumVertices()));
		// ModelViewer.start(builder);
	}
}
