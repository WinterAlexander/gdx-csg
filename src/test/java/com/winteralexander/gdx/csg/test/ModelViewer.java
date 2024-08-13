package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Segment;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.winteralexander.gdx.csg.IntersectorPlus;
import com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult;
import com.winteralexander.gdx.csg.MeshFace;
import com.winteralexander.gdx.csg.SegmentPlus;
import com.winteralexander.gdx.csg.Triangle;
import com.winteralexander.gdx.utils.input.InputUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT;
import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.NONE;

/**
 * Debug viewer to visualize models
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class ModelViewer implements ApplicationListener {
	private static final Queue<Consumer<ShapeRenderer>> __debugOnlyRenderables = new Queue<>();

	private final static int DEFAULT_ATTRIBUTES = VertexAttributes.Usage.Position
			| VertexAttributes.Usage.Normal
			| VertexAttributes.Usage.Tangent
			| VertexAttributes.Usage.TextureCoordinates;

	private ModelBatch modelBatch;
	private Viewport viewport;

	private final Array<Model> models = new Array<>();

	private Array<ModelInstance> instances = new Array<>();
	private PerspectiveCamera cam;
	private ShapeRenderer debugRenderer;

	public ModelViewer(Model... model) {
		models.addAll(model);
	}

	@Override
	public void create() {
		modelBatch = new ModelBatch(new DefaultShaderProvider());

		for(Model model : models)
			instances.add(new ModelInstance(model));
		Pixmap red = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		red.setColor(Color.RED);
		red.fill();

		Pixmap blue = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		blue.setColor(Color.BLUE);
		blue.fill();

		Texture tex1 = new Texture(red);
		Texture tex2 = new Texture(blue);

		int j = 0;
		for(ModelInstance instance : instances) {
			for(int i = 0; i < instance.materials.size; i++) {
				instance.materials.get(i).set(TextureAttribute.createDiffuse(j % 2 == 0 ? tex1 : tex2));
			}
			j++;
		}
		for(ModelInstance instance : instances) {
			instance.transform.idt();
			instance.calculateTransforms();
		}
		viewport = new FitViewport(16f, 9f);

		cam = new PerspectiveCamera(67f, 16f, 9f);
		cam.position.set(10f, 0f, 0f);
		cam.lookAt(0f, 0f, 0f);
		cam.near = 0.01f;

		debugRenderer = new ShapeRenderer();
		debugRenderer.setAutoShapeType(true);

		InputUtil.registerInput(new CameraInputController(cam));
		__debugOnlyRenderables.addFirst(r -> {
			int i = 0;
			for(ModelInstance instance : instances) {
				r.setColor(i % 2 == 0 ? Color.RED : Color.BLUE);
				for(Mesh mesh : instance.model.meshes) {
					FloatBuffer vBuffer = mesh.getVerticesBuffer(false);
					ShortBuffer idxBuffer = mesh.getIndicesBuffer(false);
					for(int tri = 0; tri < mesh.getNumIndices() / 3; tri++) {
						short v1 = idxBuffer.get(tri * 3);
						short v2 = idxBuffer.get(tri * 3 + 1);
						short v3 = idxBuffer.get(tri * 3 + 2);

						float x1 = vBuffer.get(v1 * mesh.getVertexSize() / 4);
						float y1 = vBuffer.get(v1 * mesh.getVertexSize() / 4 + 1);
						float z1 = vBuffer.get(v1 * mesh.getVertexSize() / 4 + 2);

						float x2 = vBuffer.get(v2 * mesh.getVertexSize() / 4);
						float y2 = vBuffer.get(v2 * mesh.getVertexSize() / 4 + 1);
						float z2 = vBuffer.get(v2 * mesh.getVertexSize() / 4 + 2);

						float x3 = vBuffer.get(v3 * mesh.getVertexSize() / 4);
						float y3 = vBuffer.get(v3 * mesh.getVertexSize() / 4 + 1);
						float z3 = vBuffer.get(v3 * mesh.getVertexSize() / 4 + 2);
						r.line(x1, y1, z1, x2, y2, z2);
						r.line(x2, y2, z2, x3, y3, z3);
						r.line(x3, y3, z3, x1, y1, z1);
					}
				}
				i++;
			}
		});
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height, true);
	}

	@Override
	public void render() {
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		cam.update();
		viewport.apply();

		if(!Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
			modelBatch.begin(cam);
			for(ModelInstance instance : instances)
				modelBatch.render(instance);
			modelBatch.end();
		} else {
			debugRenderer.setProjectionMatrix(cam.combined);
			debugRenderer.begin();

			for(Consumer<ShapeRenderer> renderable : __debugOnlyRenderables)
				renderable.accept(debugRenderer);

			debugRenderer.end();
		}

	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {

	}

	public static void main(String[] args) {
		new LwjglApplication(new ModelViewer() {
			@Override
			public void create() {

				ModelBuilder builder = new ModelBuilder();
				Model model1 = generateSixFacedCube(builder);

				Model model2 = generateSixFacedCube(builder);
				model2.meshes.get(0).transform(
						new Matrix4()
								.setToRotation(new Vector3(0f, 1f, 0f), 45f)
								.translate(0f, 0.8f, 0f));
				ModelViewer viewer = this;
				viewer.models.add(model1, model2);
				super.create();
			}
		});
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
}