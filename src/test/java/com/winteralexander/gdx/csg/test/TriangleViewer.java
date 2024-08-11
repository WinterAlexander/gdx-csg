package com.winteralexander.gdx.csg.test;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Segment;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.winteralexander.gdx.csg.Face;
import com.winteralexander.gdx.csg.IntersectorPlus;
import com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult;
import com.winteralexander.gdx.csg.SegmentPlus;
import com.winteralexander.gdx.csg.Triangle;
import com.winteralexander.gdx.utils.input.InputUtil;

import java.util.function.Consumer;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT;
import static com.winteralexander.gdx.csg.IntersectorPlus.TriangleIntersectionResult.NONE;

/**
 * Debug viewer to visualize triangle intersections
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class TriangleViewer {
	private final static int DEFAULT_ATTRIBUTES = VertexAttributes.Usage.Position
			| VertexAttributes.Usage.Normal
			| VertexAttributes.Usage.Tangent
			| VertexAttributes.Usage.TextureCoordinates;

	public static void main(String[] args) {
		new LwjglApplication(new ApplicationListener() {
			private ModelBatch modelBatch;
			private Viewport viewport;

			private Model model1, model2;

			private Triangle triangle1, triangle2;

			private ModelInstance instance1, instance2;
			private PerspectiveCamera cam;
			private ShapeRenderer debugRenderer;

			private TriangleIntersectionResult intersection = NONE;

			private final Segment intersectionSegment = new SegmentPlus();

			@Override
			public void create() {
				modelBatch = new ModelBatch();

				ModelBuilder builder = new ModelBuilder();
				model1 = generateSixFacedCube(builder);

				model2 = generateSixFacedCube(builder);
				model2.meshes.get(0).transform(
						new Matrix4()
								.setToRotation(new Vector3(0f, 1f, 0f), 45f)
								.translate(0f, 0.8f, 0f));

				instance1 = new ModelInstance(model1);
				instance2 = new ModelInstance(model2);
				Pixmap red = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
				red.setColor(Color.RED);
				red.fill();

				Pixmap blue = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
				blue.setColor(Color.BLUE);
				blue.fill();

				Texture tex1 = new Texture(red);
				Texture tex2 = new Texture(blue);

				for(int i = 0; i < 6; i++) {
					instance1.materials.get(i).set(TextureAttribute.createDiffuse(tex1));
					instance2.materials.get(i).set(TextureAttribute.createDiffuse(tex2));
				}
				instance1.transform.idt();
				instance1.calculateTransforms();
				instance2.transform.idt();
				instance2.calculateTransforms();
				viewport = new FitViewport(16f, 9f);

				cam = new PerspectiveCamera(67f, 16f, 9f);
				cam.position.set(10f, 0f, 0f);
				cam.lookAt(0f, 0f, 0f);

				debugRenderer = new ShapeRenderer();
				debugRenderer.setAutoShapeType(true);

				InputUtil.registerInput(new CameraInputController(cam));

				Array<Face> mesh1Faces = new Array<>();
				Array<Face> mesh2Faces = new Array<>();

				for(int i = 0; i < instance1.model.meshes.get(0).getNumIndices() / 3; i++)
					mesh1Faces.add(new Face(instance1.model.meshes.get(0), i) {{
						color = Color.RED;
					}});

				for(int i = 0; i < instance2.model.meshes.get(0).getNumIndices() / 3; i++)
					mesh2Faces.add(new Face(instance2.model.meshes.get(0), i) {{
						color = Color.BLUE;
					}});


				triangle1 = new Triangle(0f, 0f, 0f,
						0f, 1f, 0f,
						0f, 0f, 1f);

				triangle2 = new Triangle(1f, 0f, 0f,
						1f, 1f, 0f,
						1f, 0f, 1f);

				Face.__debugOnlyRenderables.clear();
				Face.__debugOnlyRenderables.addFirst(r -> {
					r.setColor(intersection == NONE ? Color.RED : Color.GREEN);
					r.line(triangle1.p1, triangle1.p2);
					r.line(triangle1.p2, triangle1.p3);
					r.line(triangle1.p3, triangle1.p1);

					r.line(triangle2.p1, triangle2.p2);
					r.line(triangle2.p2, triangle2.p3);
					r.line(triangle2.p3, triangle2.p1);

					r.setColor(Color.BLUE);
					r.line(intersectionSegment.a, intersectionSegment.b);
				});
				InputUtil.registerInput(new InputAdapter() {
					@Override
					public boolean keyDown(int keycode) {
						float amount = (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ? -1f : 1f) * Gdx.graphics.getDeltaTime();
						switch(keycode) {
							case Input.Keys.X:
								triangle2.add(amount, 0f, 0f);
								return true;
							case Input.Keys.Y:
								triangle2.add(0f, amount, 0f);
								return true;
							case Input.Keys.Z:
								triangle2.add(0f, 0f, amount);
								return true;
							case Input.Keys.R:
								triangle2.p1.add(amount, 0f, 0f);
								return true;
							case Input.Keys.P:
								System.out.println("expected.a.set(" +
										intersectionSegment.a.x + "f, " +
										intersectionSegment.a.y + "f, " +
										intersectionSegment.a.z + "f);");
								System.out.println("expected.b.set(" +
										intersectionSegment.b.x + "f, " +
										intersectionSegment.b.y + "f, " +
										intersectionSegment.b.z + "f);");
								System.out.println("tri1.set(" +
										triangle1.p1.x + "f, " +
										triangle1.p1.y + "f, " +
										triangle1.p1.z + "f, " +
										triangle1.p2.x + "f, " +
										triangle1.p2.y + "f, " +
										triangle1.p2.z + "f, " +
										triangle1.p3.x + "f, " +
										triangle1.p3.y + "f, " +
										triangle1.p3.z + "f);");
								System.out.println("tri2.set(" +
										triangle2.p1.x + "f, " +
										triangle2.p1.y + "f, " +
										triangle2.p1.z + "f, " +
										triangle2.p2.x + "f, " +
										triangle2.p2.y + "f, " +
										triangle2.p2.z + "f, " +
										triangle2.p3.x + "f, " +
										triangle2.p3.y + "f, " +
										triangle2.p3.z + "f);");
								return true;
						}

						return false;
					}
				});
			}

			@Override
			public void resize(int width, int height) {
				viewport.update(width, height, true);
			}

			@Override
			public void render() {
				intersection = IntersectorPlus.intersectTriangleTriangle(triangle1, triangle2, 1e-5f, intersectionSegment);

				Gdx.gl.glClearColor(0, 0, 0, 0);
				Gdx.gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

				cam.update();
				viewport.apply();

				modelBatch.begin(cam);
				//modelBatch.render(instance1);
				//modelBatch.render(instance2);
				modelBatch.end();

				debugRenderer.setProjectionMatrix(cam.combined);
				debugRenderer.begin();

				for(Consumer<ShapeRenderer> renderable : Face.__debugOnlyRenderables)
					renderable.accept(debugRenderer);

				debugRenderer.end();
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