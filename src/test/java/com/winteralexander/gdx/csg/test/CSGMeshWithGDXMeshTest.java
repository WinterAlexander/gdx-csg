package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl.LwjglGraphics;
import com.badlogic.gdx.backends.lwjgl.LwjglNativesLoader;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.winteralexander.gdx.csg.CSGConfiguration;
import com.winteralexander.gdx.csg.CSGMesh;
import com.winteralexander.gdx.csg.CSGUtil;
import com.winteralexander.gdx.csg.test.debugviewer.CSGMeshViewer;
import com.winteralexander.gdx.csg.test.debugviewer.ModelViewer;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.lwjgl.opengl.Display;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;

import static com.badlogic.gdx.graphics.GL20.GL_TRIANGLES;
import static com.badlogic.gdx.graphics.VertexAttributes.Usage.TextureCoordinates;
import static org.junit.Assert.assertNotSame;

/**
 * Unit tests for the functionality of {@link CSGMesh} that involves the loading of libGDX's LWJGL
 * graphics to convert {@link CSGMesh}es from {@link Mesh}. This test is ignored by default because
 * it cannot run on headless machines like Github CI.
 * <p>
 * Created on 2024-08-12.
 *
 * @author Alexander Winter
 */
@Ignore
public class CSGMeshWithGDXMeshTest {

	private final static int DEFAULT_ATTRIBUTES = VertexAttributes.Usage.Position
			| VertexAttributes.Usage.Normal
			| VertexAttributes.Usage.Tangent
			| TextureCoordinates;

	@BeforeClass
	public static void initGL() throws Exception {
		if(Gdx.gl != null) {
			Display.destroy();
			Gdx.gl = null;
			Gdx.graphics = null;
			Gdx.gl20 = null;
			Gdx.gl30 = null;
			Gdx.gl31 = null;
			Gdx.gl32 = null;
		}
		
		LwjglNativesLoader.load();
		Class<LwjglGraphics> gfx = LwjglGraphics.class;
		Constructor<LwjglGraphics> cons = gfx.getDeclaredConstructor(LwjglApplicationConfiguration.class);
		cons.setAccessible(true);
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.undecorated = true;
		config.width = 1;
		config.height = 1;

		Gdx.graphics = cons.newInstance(config);

		Method method = gfx.getDeclaredMethod("setupDisplay");
		method.setAccessible(true);
		method.invoke(Gdx.graphics);
	}

	private static Model generateSixFacedCube(ModelBuilder builder) {
		float s = 0.5f;
		builder.begin();
		builder.part("front",
				GL20.GL_TRIANGLES,
				DEFAULT_ATTRIBUTES,
				new Material()).rect(
				s, -s, -s,
				-s, -s, -s,
				-s, s, -s,
				s, s, -s,
				0f, 0f, -1f);
		builder.part("back",
				GL20.GL_TRIANGLES,
				DEFAULT_ATTRIBUTES,
				new Material()).rect(
				-s, -s, s,
				s, -s, s,
				s, s, s,
				-s, s, s,
				0f, 0f, 1f);
		builder.part("bottom",
				GL20.GL_TRIANGLES,
				DEFAULT_ATTRIBUTES,
				new Material()).rect(
				-s, -s, s,
				-s, -s, -s,
				s, -s, -s,
				s, -s, s,
				0f, -1f, 0f);
		builder.part("top",
				GL20.GL_TRIANGLES,
				DEFAULT_ATTRIBUTES,
				new Material()).rect(
				-s, s, -s,
				-s, s, s,
				s, s, s,
				s, s, -s,
				0f, 1f, 0f);
		builder.part("left",
				GL20.GL_TRIANGLES,
				DEFAULT_ATTRIBUTES,
				new Material()).rect(
				-s, -s, -s,
				-s, -s, s,
				-s, s, s,
				-s, s, -s,
				-1f, 0f, 0f);
		builder.part("right",
				GL20.GL_TRIANGLES,
				DEFAULT_ATTRIBUTES,
				new Material()).rect(
				s, -s, s,
				s, -s, -s,
				s, s, -s,
				s, s, s,
				1f, 0f, 0f);
		return builder.end();
	}

	private static void assertNoDupVertex(CSGMesh csg) {
		for(int i = 0; i < csg.getVertices().size; i++) {
			for(int j = i + 1; j < csg.getVertices().size; j++) {
				if(i == j)
					continue;

				assertNotSame(csg.getVertices().get(i), csg.getVertices().get(j));
			}
		}
	}

	@Test
	public void ensureNotDupVertices() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createSphere(1f, 1f, 1f, 10, 10, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh mesh = box.meshes.get(0);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				//.scale(1.1f, 1f, 0.8f)
				.translate(0f, 0.8f, 0f));
		CSGMesh csg = CSGMesh.fromMesh(mesh);
		CSGMesh otherCsg = CSGMesh.fromMesh(other);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		CSGMesh copy1 = csg.cpy();
		CSGMesh copy2 = otherCsg.cpy();

		csg.splitTriangles(copy2);
		otherCsg.splitTriangles(copy1);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		csg.classifyFaces(copy2);
		otherCsg.classifyFaces(copy1);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		csg.removeFaces(true, true);
		otherCsg.removeFaces(false, true);

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);

		otherCsg.invertTriangles();

		assertNoDupVertex(csg);
		assertNoDupVertex(otherCsg);
	}

	@Test
	public void testCubeCubeSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model cube = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh cubeMesh = cube.meshes.get(0);
		cubeMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0f, 0.3f, 0f));

		CSGUtil.subtraction(box, cubeMesh);

		ModelViewer.start(box);
	}


	@Test
	public void testCubeSubtractionCoplanar() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model cube = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh cubeMesh = cube.meshes.get(0);
		cubeMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.5f, 0.3f, 0f));

		CSGMesh minuend = CSGMesh.fromMesh(box.meshes.get(0));
		CSGMesh subtrahend = CSGMesh.fromMesh(cubeMesh);
		CSGMesh copy1 = minuend.cpy();
		CSGMesh copy2 = subtrahend.cpy();
		copy1.setConfig(CSGConfiguration.DEFAULT);
		copy2.setConfig(CSGConfiguration.DEFAULT);

		copy1.splitTriangles(subtrahend);
		copy2.splitTriangles(minuend);

		copy1.classifyFaces(subtrahend);
		copy2.classifyFaces(minuend);

		//CSGMeshViewer.start(copy1, copy2);

		CSGMeshViewer.start(copy1);
		CSGMeshViewer.start(copy2);

		copy1.removeFaces(true, true);
		copy2.removeFaces(false, true);

		CSGMeshViewer.start(copy1);
		CSGMeshViewer.start(copy2);

		copy2.invertTriangles();
		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		CSGMeshViewer.start(copy1);
	}

	private CSGMesh substractDebug(CSGMesh first, CSGMesh second, CSGConfiguration config) {
		CSGMesh copy1 = first.cpy();
		CSGMesh copy2 = second.cpy();
		copy1.setConfig(config);
		copy2.setConfig(config);

		CSGMeshViewer.start(copy1);
		CSGMeshViewer.start(copy2);

		copy1.splitTriangles(second);
		copy2.splitTriangles(first);

		copy1.classifyFaces(second);
		copy2.classifyFaces(first);

		CSGMeshViewer.start(copy1, copy2);

		copy1.removeFaces(true, true);
		copy2.removeFaces(false, true);

		copy2.invertTriangles();
		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		CSGMeshViewer.start(copy1);

		return copy1;
	}

	@Test
	public void testCubeCubeDoubleSubtraction() throws Exception {
		initGL();
		CSGMesh defaultTop, rightPath, leftPath, backPath, frontPath, stopPath;
		Matrix4 tmpMat4 = new Matrix4();
		float TILE_SIZE = 2f;
		float PATH_DEPTH = 0.1f;
		float PATH_WIDTH = 0.75f;
		float STOP_WIDTH = 1.5f;
		ModelBuilder builder = new ModelBuilder();

		Model flat = builder.createBox(TILE_SIZE, TILE_SIZE, TILE_SIZE,
				new Material(), DEFAULT_ATTRIBUTES);
		flat.meshes.get(0).transform(tmpMat4.idt().translate(TILE_SIZE / 2f,
				-TILE_SIZE / 2f,
				TILE_SIZE / 2f));

		defaultTop = CSGMesh.fromMesh(flat.meshes.get(0));

		Model pathBox = builder.createBox(TILE_SIZE, PATH_DEPTH * 2f, PATH_WIDTH,
				new Material(), DEFAULT_ATTRIBUTES);

		pathBox.meshes.get(0).transform(tmpMat4.idt().translate(TILE_SIZE - PATH_WIDTH / 2f, 0f, TILE_SIZE / 2f));
		rightPath = CSGMesh.fromMesh(pathBox.meshes.get(0));

		pathBox.meshes.get(0).transform(tmpMat4.idt().translate(PATH_WIDTH - TILE_SIZE, 0f, 0f));
		leftPath = CSGMesh.fromMesh(pathBox.meshes.get(0));

		pathBox.dispose();
		pathBox = builder.createBox(PATH_WIDTH, PATH_DEPTH * 2f, TILE_SIZE,
				new Material(), DEFAULT_ATTRIBUTES);

		pathBox.meshes.get(0).transform(tmpMat4.idt().translate(TILE_SIZE / 2f, 0f, TILE_SIZE - PATH_WIDTH / 2f));
		frontPath = CSGMesh.fromMesh(pathBox.meshes.get(0));

		pathBox.meshes.get(0).transform(tmpMat4.idt().translate(0f, 0f, PATH_WIDTH - TILE_SIZE));
		backPath = CSGMesh.fromMesh(pathBox.meshes.get(0));
		pathBox.dispose();

		pathBox = builder.createCylinder(STOP_WIDTH, PATH_DEPTH * 2f, STOP_WIDTH, 10,
				new Material(), DEFAULT_ATTRIBUTES);
		pathBox.meshes.get(0).transform(tmpMat4.idt().translate(TILE_SIZE / 2f, 0f, TILE_SIZE / 2f));

		stopPath = CSGMesh.fromMesh(pathBox.meshes.get(0));
		pathBox.dispose();
		CSGMesh current = defaultTop;
		CSGConfiguration config = CSGConfiguration.DEFAULT;

		boolean pathRight = false;
		boolean pathLeft = true;
		boolean pathFront = false;
		boolean pathBack = true;
		boolean isStop = true;

		if(pathRight)
			current = CSGUtil.subtraction(current, rightPath, config);
		if(pathLeft)
			current = CSGUtil.subtraction(current, leftPath, config);
		if(pathFront)
			current = CSGUtil.subtraction(current, frontPath, config);
		if(pathBack)
			current = substractDebug(current, backPath, config);
		if(isStop)
			current = substractDebug(current, stopPath, config);

		CSGMeshViewer.start(current);
		initGL();
		flat.meshes.set(0, current.toMesh());
		flat.meshParts.get(0).set("id", flat.meshes.get(0), 0, flat.meshes.get(0).getNumIndices(), GL_TRIANGLES);
		flat.meshParts.get(0).update();
		ModelViewer.start(flat);
	}

	@Test
	public void testCubeSphereSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model sphere = builder.createSphere(1f, 1f, 1f, 50, 50, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.3f, 0.3f, 0.3f));

		CSGUtil.subtraction(box, sphereMesh);

		ModelViewer.start(box);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCubeCylinderSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createCylinder(0.8f, 1f, 0.8f, 25, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0f, 0.8f, 0f));

		CSGUtil.subtraction(box, other);

		Display.destroy();
		Gdx.gl = null;
		Gdx.graphics = null;
		Gdx.gl20 = null;
		Gdx.gl30 = null;
		Gdx.gl31 = null;
		Gdx.gl32 = null;

		ModelViewer.start(box);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCubePrismSubtraction() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh mesh = box.meshes.get(0);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.scale(1.1f, 1f, 0.8f)
				.translate(0f, 0.95f, 0f));
		CSGMesh csg = CSGMesh.fromMesh(mesh);
		CSGMesh otherCsg = CSGMesh.fromMesh(other);

		CSGMesh copy1 = csg.cpy();
		CSGMesh copy2 = otherCsg.cpy();

		csg.splitTriangles(copy2);
		otherCsg.splitTriangles(copy1);

		csg.classifyFaces(copy2);
		otherCsg.classifyFaces(copy1);

		//CSGMeshViewer.start(csg, otherCsg);

		csg.removeFaces(true, true);
		otherCsg.removeFaces(false, true);

		otherCsg.invertTriangles();

		Mesh newMesh = csg.toMesh();

		//assertEquals(mesh.getNumVertices(), newMesh.getNumVertices());
		//assertEquals(mesh.getNumIndices(), newMesh.getNumIndices());

		box.meshes.set(0, newMesh);
		box.meshParts.get(0).set("box", newMesh, 0, newMesh.getNumIndices(), GL_TRIANGLES);
		box.meshParts.get(0).update();

		Mesh secondNewMesh = otherCsg.toMesh();

		second.meshes.set(0, secondNewMesh);
		second.meshParts.get(0).set("box", secondNewMesh, 0, secondNewMesh.getNumIndices(), GL_TRIANGLES);
		second.meshParts.get(0).update();

		ModelViewer.start(box, second);
		//CSGMeshViewer.start(new CSGMesh[] { csg, otherCsg }, new Ray[0]);
	}

	@Test
	public void testCubeSphereIntersection() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model sphere = builder.createSphere(1f, 1f, 1f, 50, 50, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.3f, 0.3f, 0.3f));

		CSGUtil.intersection(box, sphereMesh);

		ModelViewer.start(box);
	}

	@Test
	public void testCubeSphereUnion() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model sphere = builder.createSphere(1f, 1f, 1f, 50, 50, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.3f, 0.3f, 0.3f));

		CSGUtil.union(box, sphereMesh);

		ModelViewer.start(box);
	}

	@Test
	public void testSameUnion() {

		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Model second = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh other = second.meshes.get(0);
		other.transform(new Matrix4().translate(0f, 0.1f, 0f));
		CSGMesh mesh1 = CSGMesh.fromMesh(box.meshes.get(0));
		CSGMesh mesh2 = CSGMesh.fromMesh(other);

		CSGMesh copy1 = mesh1.cpy();
		CSGMesh copy2 = mesh2.cpy();

		copy1.splitTriangles(mesh2);
		copy2.splitTriangles(mesh1);

		copy1.classifyFaces(mesh2);
		copy2.classifyFaces(mesh1);

		CSGMeshViewer.start(copy1, copy2);

		copy1.removeFaces(true, false);
		copy2.removeFaces(true, true);

		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		CSGMeshViewer.start(copy1);

		CSGUtil.union(box, box.meshes.get(0));

		ModelViewer.start(box);
	}

	@Test
	public void testCylinderUnions() throws Exception {
		ModelBuilder builder = new ModelBuilder();
		Model cylinder = builder.createCylinder(0.5f, 2f, 0.5f, 10, new Material(),
				VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal);
		Mesh cylMesh = cylinder.meshes.get(0);
		CSGMesh cylinder1 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(1f, 0f, 0f), 90f));

		CSGMesh cylinder2 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 90f));

		CSGMesh cylinder3 = CSGMesh.fromMesh(cylMesh);

		CSGMesh firstUnion = CSGUtil.union(cylinder1, cylinder2);

		//CSGMeshViewer.start(firstUnion);

		CSGMesh copy1 = firstUnion.cpy();
		CSGMesh copy2 = cylinder3.cpy();

		copy1.splitTriangles(cylinder3);
		copy2.splitTriangles(firstUnion);

		copy1.classifyFaces(cylinder3);
		copy2.classifyFaces(firstUnion);

		CSGMeshViewer.start(copy1, copy2);

		copy1.removeFaces(true, false);
		copy2.removeFaces(true, false);

		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		CSGMesh cylinders = copy1;//CSGUtil.union(firstUnion, cylinder3);

		initGL();
		cylinder.meshes.set(0, cylinders.toMesh());
		cylinder.meshParts.get(0).set("box",
				cylinder.meshes.get(0), 0, cylinder.meshes.get(0).getNumIndices(), GL_TRIANGLES);
		cylinder.meshParts.get(0).update();

		ModelViewer.start(cylinder);
		//CSGMeshViewer.start(cylinders);
	}

	@Test
	public void testClassicExample() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.ColorPacked);

		for(Mesh mesh : box.meshes) {
			FloatBuffer buffer = mesh.getVerticesBuffer(true);
			for(int i = 0; i < mesh.getNumVertices(); i++) {
				buffer.position(i * mesh.getVertexSize() / 4 +
						mesh.getVertexAttribute(VertexAttributes.Usage.ColorPacked).offset / 4);

				buffer.put(Color.RED.toFloatBits());
			}
		}

		Model sphere = builder.createSphere(1.35f, 1.35f, 1.35f, 10, 10, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.ColorPacked);

		for(Mesh mesh : sphere.meshes) {
			FloatBuffer buffer = mesh.getVerticesBuffer(true);
			for(int i = 0; i < mesh.getNumVertices(); i++) {
				buffer.position(i * mesh.getVertexSize() / 4 +
						mesh.getVertexAttribute(VertexAttributes.Usage.ColorPacked).offset / 4);

				buffer.put(Color.BLUE.toFloatBits());
			}
		}

		Model cylinder = builder.createCylinder(0.6f, 2f, 0.6f, 10, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.ColorPacked);

		for(Mesh mesh : cylinder.meshes) {
			FloatBuffer buffer = mesh.getVerticesBuffer(true);
			for(int i = 0; i < mesh.getNumVertices(); i++) {
				buffer.position(i * mesh.getVertexSize() / 4 +
						mesh.getVertexAttribute(VertexAttributes.Usage.ColorPacked).offset / 4);

				buffer.put(Color.GREEN.toFloatBits());
			}
		}

		CSGMesh boxCSG = CSGMesh.fromMesh(box.meshes.get(0));
		CSGMesh sphereCSG = CSGMesh.fromMesh(sphere.meshes.get(0));
		Mesh cylMesh = cylinder.meshes.get(0);

		CSGMesh cylinder1 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(1f, 0f, 0f), 90f));

		CSGMesh cylinder2 = CSGMesh.fromMesh(cylMesh);

		cylMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 90f));

		CSGMesh cylinder3 = CSGMesh.fromMesh(cylMesh);

		//CSGMesh cylinders = CSGUtil.union(CSGUtil.union(cylinder1, cylinder2), cylinder3);

		CSGMesh roundedBox = CSGUtil.intersection(boxCSG, sphereCSG);

		CSGMesh last = CSGUtil.subtraction(roundedBox, cylinder1);
		last = CSGUtil.subtraction(last, cylinder2);
		last = CSGUtil.subtraction(last, cylinder3);
/*
		CSGMesh copy1 = roundedBox.cpy();
		CSGMesh copy2 = cylinders.cpy();

		copy1.splitTriangles(cylinders);
		copy2.splitTriangles(roundedBox);

		copy1.classifyFaces(cylinders);
		copy2.classifyFaces(roundedBox);

		Ray ray = new Ray();
		ray.set(0.19290544f, -0.5f, -0.1757075f, 0f, 1f, 0f);

		// issue with this is missing triangle
		CSGMeshViewer.start(new CSGMesh[]{ /*copy1,* cylinders }, new Ray[] { ray });

		copy1.removeFaces(true, false);
		copy2.removeFaces(false, true);

		copy2.invertTriangles();
		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		CSGMesh last = copy1;*/

		box.meshes.set(0, last.toMesh());
		box.meshParts.get(0).set("box",
				box.meshes.get(0), 0, box.meshes.get(0).getNumIndices(), GL_TRIANGLES);
		box.meshParts.get(0).update();

		ModelViewer.start(box);
	}

	@Test
	public void testWithTexturePrism() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);
		Model second = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);
		Mesh mesh = second.meshes.get(0);
		mesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0.25f, 0.25f, 0.25f));

		CSGUtil.subtraction(box, mesh);

		System.out.println("Vertex count: " + box.meshes.get(0).getNumVertices());

		ModelViewer.start(box);
	}

	@Test
	public void testWithTextureSphere() {
		ModelBuilder builder = new ModelBuilder();
		Model box = builder.createBox(1f, 1f, 1f, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);

		Model sphere = builder.createSphere(1.25f, 1.25f, 1.25f, 20, 20, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0f, 0f, 0f));

		Model bigger = builder.createSphere(1.4f, 1.4f, 1.4f, 20, 20, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);

		CSGUtil.subtraction(box, sphereMesh);
		CSGUtil.intersection(box, bigger.meshes.get(0));

		System.out.println("Vertex count: " + box.meshes.get(0).getNumVertices());

		ModelViewer.start(box);
	}

	@Test
	public void testCylinder() {
		ModelBuilder builder = new ModelBuilder();
		Model cylinder = builder.createCylinder(1f, 0.25f, 1f, 20, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);
		Model sphere = builder.createSphere(1.25f, 1.25f, 1.25f, 20, 20, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);
		Model cylinder2 = builder.createBox(1f, 0.25f, 1f, new Material(),
				VertexAttributes.Usage.Position
						| VertexAttributes.Usage.Normal
						| VertexAttributes.Usage.TextureCoordinates);
		Mesh sphereMesh = sphere.meshes.get(0);
		sphereMesh.transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0f, -0.55f, 0f));
		cylinder2.meshes.get(0).transform(new Matrix4().setToRotation(new Vector3(0f, 1f, 0f), 0f)
				.translate(0f, 0.15f, 0f));
/*
		FloatBuffer buffer = sphereMesh.getVerticesBuffer(true);
		for(int i = 0; i < sphereMesh.getNumVertices(); i++) {
			buffer.position(i * sphereMesh.getVertexSize() / 4 +
					sphereMesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates).offset / 4);

			buffer.put(1f);
			buffer.put(1f);
		}


		FloatBuffer buffer2 = cylinder2.meshes.get(0).getVerticesBuffer(true);
		for(int i = 0; i < cylinder2.meshes.get(0).getNumVertices(); i++) {
			buffer2.position(i * cylinder2.meshes.get(0).getVertexSize() / 4 +
					cylinder2.meshes.get(0).getVertexAttribute(VertexAttributes.Usage.TextureCoordinates).offset / 4);

			buffer2.put(1f);
			buffer2.put(1f);
		}
*/
		CSGUtil.subtraction(sphere, cylinder2.meshes.get(0));

		CSGUtil.subtraction(cylinder, sphere.meshes.get(0));

		ModelViewer.start(cylinder);
	}

	@Test
	public void testMeshPart() {
		ModelBuilder builder = new ModelBuilder();
		Model box = generateSixFacedCube(builder);

		Array<CSGMesh> csgMeshes = new Array<>();
		for(MeshPart meshPart : box.meshParts)
			csgMeshes.add(CSGMesh.fromMeshPart(meshPart));


	}
}
