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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Segment;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.winteralexander.gdx.csg.MeshFace;
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
public class TriangleViewer implements ApplicationListener {
	private static final Queue<Consumer<ShapeRenderer>> __debugOnlyRenderables = new Queue<>();

	private final static int DEFAULT_ATTRIBUTES = VertexAttributes.Usage.Position
			| VertexAttributes.Usage.Normal
			| VertexAttributes.Usage.Tangent
			| VertexAttributes.Usage.TextureCoordinates;
	private Viewport viewport;

	private final Array<Triangle> triangles = new Array<>();

	private final Plane tmpPlane = new Plane();

	private PerspectiveCamera cam;
	private ShapeRenderer debugRenderer;

	private TriangleIntersectionResult intersection = NONE;

	private final Segment intersectionSegment = new SegmentPlus();

	public TriangleViewer(Triangle... triangles) {
		this.triangles.addAll(triangles);
	}

	@Override
	public void create() {
		Pixmap red = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		red.setColor(Color.RED);
		red.fill();

		Pixmap blue = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		blue.setColor(Color.BLUE);
		blue.fill();

		viewport = new FitViewport(16f, 9f);

		cam = new PerspectiveCamera(67f, 16f, 9f);
		cam.position.set(10f, 0f, 0f);
		cam.lookAt(0f, 0f, 0f);
		cam.near = 0.01f;

		debugRenderer = new ShapeRenderer();
		debugRenderer.setAutoShapeType(true);

		InputUtil.registerInput(new CameraInputController(cam));

		__debugOnlyRenderables.clear();
		__debugOnlyRenderables.addFirst(r -> {
			r.setColor(intersection == NONE
					? Color.RED
					: intersection == null
						? Color.PURPLE
						: Color.GREEN);
			for(Triangle triangle : triangles) {
				r.line(triangle.p1, triangle.p2);
				r.line(triangle.p2, triangle.p3);
				r.line(triangle.p3, triangle.p1);
			}

			r.setColor(Color.BLUE);
			r.line(intersectionSegment.a, intersectionSegment.b);
		});
		InputUtil.registerInput(new InputAdapter() {
			@Override
			public boolean keyDown(int keycode) {
				float amount = (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ? -1f : 1f)
						* Gdx.graphics.getDeltaTime();
				switch(keycode) {
					case Input.Keys.X:
						triangles.get(1).add(amount, 0f, 0f);
						return true;
					case Input.Keys.Y:
						triangles.get(1).add(0f, amount, 0f);
						return true;
					case Input.Keys.Z:
						triangles.get(1).add(0f, 0f, amount);
						return true;
					case Input.Keys.R:
						triangles.get(1).p1.add(amount, 0f, 0f);
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
								triangles.get(0).p1.x + "f, " +
								triangles.get(0).p1.y + "f, " +
								triangles.get(0).p1.z + "f, " +
								triangles.get(0).p2.x + "f, " +
								triangles.get(0).p2.y + "f, " +
								triangles.get(0).p2.z + "f, " +
								triangles.get(0).p3.x + "f, " +
								triangles.get(0).p3.y + "f, " +
								triangles.get(0).p3.z + "f);");
						System.out.println("tri2.set(" +
								triangles.get(1).p1.x + "f, " +
								triangles.get(1).p1.y + "f, " +
								triangles.get(1).p1.z + "f, " +
								triangles.get(1).p2.x + "f, " +
								triangles.get(1).p2.y + "f, " +
								triangles.get(1).p2.z + "f, " +
								triangles.get(1).p3.x + "f, " +
								triangles.get(1).p3.y + "f, " +
								triangles.get(1).p3.z + "f);");
						return true;
					case Input.Keys.S:
						if(intersection != NONE) {
							float[] tri1 = new float[9];
							float[] tri2 = new float[9];
							triangles.get(0).toArray(tri1);
							triangles.get(1).toArray(tri2);
							tmpPlane.set(triangles.get(1).p1, triangles.get(1).getNormal());
							Intersector.SplitTriangle split = new Intersector.SplitTriangle(3);
							Intersector.splitTriangle(tri1, tmpPlane, split);

							for(int i = 0; i < split.numFront; i++)
								triangles.add(new Triangle(split.front, i * 9));

							for(int i = 0; i < split.numBack; i++)
								triangles.add(new Triangle(split.back, i * 9));


							tmpPlane.set(triangles.get(0).p1, triangles.get(0).getNormal());
							Intersector.splitTriangle(tri2, tmpPlane, split);

							for(int i = 0; i < split.numFront; i++)
								triangles.add(new Triangle(split.front, i * 9));

							for(int i = 0; i < split.numBack; i++)
								triangles.add(new Triangle(split.back, i * 9));
						}
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

		try {
			intersection = IntersectorPlus.intersectTriangleTriangle(triangles.get(0),
					triangles.get(1), 1e-5f, intersectionSegment);
		} catch(Exception ex) {
			intersection = null;
		}

		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		cam.update();
		viewport.apply();

		debugRenderer.setProjectionMatrix(cam.combined);
		debugRenderer.begin();

		for(Consumer<ShapeRenderer> renderable : __debugOnlyRenderables)
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

	public static void main(String[] args) {
		new LwjglApplication(new TriangleViewer(new Triangle(0f, 0f, 0f,
				0f, 1f, 0f,
				0f, 0f, 1f), new Triangle(0f, 1f, 0f,
				1f, 0f, 0f,
				1f, 1f, 0f)));
	}
}