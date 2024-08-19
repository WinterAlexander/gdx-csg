package com.winteralexander.gdx.csg.test.debugviewer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.winteralexander.gdx.csg.Triangle;
import com.winteralexander.gdx.utils.input.InputUtil;
import org.lwjgl.opengl.Display;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT;

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

	private ModelBatch shadowBatch;
	private DirectionalShadowLight shadowLight;

	private final Environment environment = new Environment();

	private final Array<Model> models = new Array<>();

	private Array<ModelInstance> instances = new Array<>();
	private PerspectiveCamera cam;
	private ShapeRenderer debugRenderer;

	private final Vector3 tmpVec3 = new Vector3();
	private final Triangle tmpTriangle = new Triangle();

	public ModelViewer(Model... model) {
		models.addAll(model);
	}

	@Override
	public void create() {
		modelBatch = new ModelBatch(new DefaultShaderProvider());

		shadowLight = new DirectionalShadowLight(1920 * 2, 1080 * 2, 16f / 4f, 9f / 4f, 0.01f, 100f);
		shadowLight.set(0.8f, 0.8f, 0.8f, -0.2f, -0.8f, -1f);
		environment.add(shadowLight);
		environment.shadowMap = shadowLight;
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));

		shadowBatch = new ModelBatch(new DepthShaderProvider());

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
			if(instance.model.meshes.get(0).getVertexAttribute(VertexAttributes.Usage.ColorPacked) != null
			|| instance.model.meshes.get(0).getVertexAttribute(VertexAttributes.Usage.ColorUnpacked) != null)
				continue;

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
				for(Mesh mesh : instance.model.meshes) {
					FloatBuffer vBuffer = mesh.getVerticesBuffer(false);
					ShortBuffer idxBuffer = mesh.getIndicesBuffer(false);

					int vSize = mesh.getVertexSize() / 4;
					int norOffset = mesh.getVertexAttribute(VertexAttributes.Usage.Normal).offset / 4;

					for(int tri = 0; tri < mesh.getNumIndices() / 3; tri++) {
						short v1 = idxBuffer.get(tri * 3);
						short v2 = idxBuffer.get(tri * 3 + 1);
						short v3 = idxBuffer.get(tri * 3 + 2);

						float x1 = vBuffer.get(v1 * vSize);
						float y1 = vBuffer.get(v1 * vSize + 1);
						float z1 = vBuffer.get(v1 * vSize + 2);

						float n1x = vBuffer.get(v1 * vSize + norOffset);
						float n1y = vBuffer.get(v1 * vSize + norOffset + 1);
						float n1z = vBuffer.get(v1 * vSize + norOffset + 2);

						float x2 = vBuffer.get(v2 * vSize);
						float y2 = vBuffer.get(v2 * vSize + 1);
						float z2 = vBuffer.get(v2 * vSize + 2);

						float n2x = vBuffer.get(v2 * vSize + norOffset);
						float n2y = vBuffer.get(v2 * vSize + norOffset + 1);
						float n2z = vBuffer.get(v2 * vSize + norOffset + 2);

						float x3 = vBuffer.get(v3 * vSize);
						float y3 = vBuffer.get(v3 * vSize + 1);
						float z3 = vBuffer.get(v3 * vSize + 2);

						float n3x = vBuffer.get(v3 * vSize + norOffset);
						float n3y = vBuffer.get(v3 * vSize + norOffset + 1);
						float n3z = vBuffer.get(v3 * vSize + norOffset + 2);

						tmpTriangle.set(x1, y1, z1, x2, y2, z2, x3, y3, z3);

						r.setColor(i % 2 == 0 ? Color.RED : Color.BLUE);
						r.line(x1, y1, z1, x2, y2, z2);
						r.line(x2, y2, z2, x3, y3, z3);
						r.line(x3, y3, z3, x1, y1, z1);

						r.setColor(Color.WHITE);
						tmpVec3.set(x1 + x2 + x3, y1 + y2 + y3, z1 + z2 + z3).scl(1f / 3f);
						Vector3 normal = tmpTriangle.getNormal();
						r.line(tmpVec3.x, tmpVec3.y, tmpVec3.z, tmpVec3.x + normal.x / 10f, tmpVec3.y + normal.y / 10f, tmpVec3.z + normal.z / 10f);

						r.setColor(Color.YELLOW);
						
						r.line(x1, y1, z1, x1 + n1x / 10f, y1 + n1y / 10f, z1 + n1z / 10f);
						r.line(x2, y2, z2, x2 + n2x / 10f, y2 + n2y / 10f, z2 + n2z / 10f);
						r.line(x3, y3, z3, x3 + n3x / 10f, y3 + n3y / 10f, z3 + n3z / 10f);
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

		shadowLight.begin(Vector3.Zero, cam.direction);
		shadowBatch.begin(shadowLight.getCamera());
		for(ModelInstance instance : instances)
			shadowBatch.render(instance);
		shadowBatch.end();
		shadowLight.end();

		viewport.apply();

		if(!Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
			modelBatch.begin(cam);
			for(ModelInstance instance : instances)
				modelBatch.render(instance, environment);
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

	public static void start(Model... models) {
		Display.destroy();
		Gdx.gl = null;
		Gdx.graphics = null;
		Gdx.gl20 = null;
		Gdx.gl30 = null;
		Gdx.gl31 = null;
		Gdx.gl32 = null;

		try {
			new LwjglApplication(new ModelViewer(models),
					new LwjglApplicationConfiguration() {{
						width = 1600;
						height = 900;
						forceExit = false;
					}}) {
				public Thread getMainThread() {
					return mainLoopThread;
				}
			}.getMainThread().join();
		} catch(InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}
}