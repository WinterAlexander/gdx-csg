package com.winteralexander.gdx.csg.test.debugviewer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.math.collision.Segment;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Queue;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.winteralexander.gdx.csg.*;
import com.winteralexander.gdx.utils.input.InputUtil;
import com.winteralexander.gdx.utils.math.MathUtil;
import org.lwjgl.opengl.Display;

import java.util.function.Consumer;

import static com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT;
import static com.badlogic.gdx.graphics.GL20.GL_DEPTH_BUFFER_BIT;

/**
 * Debug viewer to visualize CSGMeshes
 * <p>
 * Created on 2024-08-04.
 *
 * @author Alexander Winter
 */
public class CSGMeshViewer implements ApplicationListener {
	private final Queue<Consumer<ShapeRenderer>> __debugOnlyRenderables = new Queue<>();

	private final static int DEFAULT_ATTRIBUTES = VertexAttributes.Usage.Position
			| VertexAttributes.Usage.Normal
			| VertexAttributes.Usage.Tangent
			| VertexAttributes.Usage.TextureCoordinates;

	private ModelBatch modelBatch;
	private Viewport viewport;

	private final Array<CSGMesh> meshes = new Array<>();
	private final Array<Ray> rays = new Array<Ray>();

	private final Array<MeshFace> highlighted = new Array<>();

	private PerspectiveCamera cam;
	private ShapeRenderer debugRenderer;

	private final Vector3 tmpVec3 = new Vector3();

	public CSGMeshViewer(CSGMesh... meshes) {
		this.meshes.addAll(meshes);
	}

	public CSGMeshViewer(CSGMesh[] meshes, Ray[] rays) {
		this.meshes.addAll(meshes);
		this.rays.addAll(rays);
	}

	@Override
	public void create() {
		modelBatch = new ModelBatch(new DefaultShaderProvider());

		viewport = new FitViewport(16f, 9f);

		cam = new PerspectiveCamera(67f, 16f, 9f);
		cam.position.set(3f, 0f, 0f);
		cam.lookAt(0f, 0f, 0f);
		cam.near = 0.01f;

		debugRenderer = new ShapeRenderer();
		debugRenderer.setAutoShapeType(true);

		InputUtil.registerInput(new CameraInputController(cam) {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				for(MeshFace face : highlighted)
					System.out.println("Highlighted: " + face.getTriangle().toString());

				return super.touchDown(screenX, screenY, pointer, button);
			}

			@Override
			public boolean mouseMoved(int screenX, int screenY) {

				highlighted.clear();
				Ray ray = new Ray();
				tmpVec3.set(screenX, screenY, 1f);
				cam.unproject(tmpVec3);
				ray.origin.set(cam.position);
				ray.direction.set(tmpVec3).sub(cam.position).nor();
				Segment segment = new SegmentPlus();

				int i = 0;
				for(CSGMesh mesh : meshes) {
					for(MeshFace face : mesh.getFaces()) {
						if(IntersectorPlus.intersectTriangleRay(face.getTriangle(), ray, 1e-6f, segment)) {
							highlighted.add(face);
						}
					}
					i++;
				}
				return super.mouseMoved(screenX, screenY);
			}

			{
				scrollFactor *= 0.1f;
			}
		});

		__debugOnlyRenderables.addFirst(r -> {
			int i = -1;
			for(CSGMesh mesh : meshes) {
				i++;
				r.set(ShapeRenderer.ShapeType.Line);
				for(MeshFace face : mesh.getFaces()) {
					CSGMesh.InsideStatus status1 = mesh.getInsideStatus(face.getV1());
					CSGMesh.InsideStatus status2 = mesh.getInsideStatus(face.getV2());
					CSGMesh.InsideStatus status3 = mesh.getInsideStatus(face.getV3());

					boolean isFaceInside = status1 == CSGMesh.InsideStatus.INSIDE
							|| status2 == CSGMesh.InsideStatus.INSIDE
							|| status3 == CSGMesh.InsideStatus.INSIDE
							|| status1 == CSGMesh.InsideStatus.BOUNDARY
							&& status2 == CSGMesh.InsideStatus.BOUNDARY
							&& status3 == CSGMesh.InsideStatus.BOUNDARY;

					boolean isFaceOutside = status1 == CSGMesh.InsideStatus.OUTSIDE
							|| status2 == CSGMesh.InsideStatus.OUTSIDE
							|| status3 == CSGMesh.InsideStatus.OUTSIDE;

					r.setColor(isFaceInside && isFaceOutside
							? Color.RED
							: !isFaceInside && !isFaceOutside
								? Color.YELLOW
								: isFaceInside
									? Color.BLUE
									: Color.GREEN);



					if(status1 == null || status2 == null || status3 == null) {
						r.setColor(Color.GRAY);
					}

					r.line(face.getPosition1(), face.getPosition2());
					r.line(face.getPosition2(), face.getPosition3());
					r.line(face.getPosition3(), face.getPosition1());

					r.setColor(Color.WHITE);
					tmpVec3.set(face.getPosition1())
							.add(face.getPosition2())
							.add(face.getPosition3())
							.scl(1f / 3f);
					Vector3 normal = face.getNormal();
					r.line(tmpVec3.x, tmpVec3.y, tmpVec3.z,
							tmpVec3.x + normal.x / 10f,
							tmpVec3.y + normal.y / 10f,
							tmpVec3.z + normal.z / 10f);
				}

				for(MeshFace face : mesh.getFaces()) {
					CSGMesh.InsideStatus status1 = mesh.getInsideStatus(face.getV1());
					CSGMesh.InsideStatus status2 = mesh.getInsideStatus(face.getV2());
					CSGMesh.InsideStatus status3 = mesh.getInsideStatus(face.getV3());

					boolean isFaceInside = status1 == CSGMesh.InsideStatus.INSIDE
							|| status2 == CSGMesh.InsideStatus.INSIDE
							|| status3 == CSGMesh.InsideStatus.INSIDE
							|| status1 == CSGMesh.InsideStatus.BOUNDARY
							&& status2 == CSGMesh.InsideStatus.BOUNDARY
							&& status3 == CSGMesh.InsideStatus.BOUNDARY;

					boolean isFaceOutside = status1 == CSGMesh.InsideStatus.OUTSIDE
							|| status2 == CSGMesh.InsideStatus.OUTSIDE
							|| status3 == CSGMesh.InsideStatus.OUTSIDE;

					if(!(isFaceInside && isFaceOutside))
						continue;

					r.setColor(Color.RED);
					r.line(face.getPosition1(), face.getPosition2());
					r.line(face.getPosition2(), face.getPosition3());
					r.line(face.getPosition3(), face.getPosition1());
				}

				for(MeshFace face : highlighted) {
					r.setColor(Color.WHITE);
					r.line(face.getPosition1(), face.getPosition2());
					r.line(face.getPosition2(), face.getPosition3());
					r.line(face.getPosition3(), face.getPosition1());
				}

				if(i == 0 && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT))
					continue;
				if(i == 1 && !Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))
					continue;
				r.set(ShapeRenderer.ShapeType.Filled);
				for(MeshVertex vertex : mesh.getVertices()) {
					CSGMesh.InsideStatus status = mesh.getInsideStatus(vertex);

					r.setColor(status == CSGMesh.InsideStatus.INSIDE
							? Color.BLUE
							: status == CSGMesh.InsideStatus.BOUNDARY
								? Color.YELLOW
								: Color.GREEN);
					r.set(ShapeRenderer.ShapeType.Filled);
					float vSize = 0.05f * MathUtil.sigmoid(cam.position.dst2(vertex.getPosition()));
					r.box(vertex.getPosition().x - vSize / 2f,
							vertex.getPosition().y - vSize / 2f,
							vertex.getPosition().z + vSize / 2f,
							vSize, vSize, vSize);
				}
			}


			r.set(ShapeRenderer.ShapeType.Line);
			r.setColor(Color.MAGENTA);
			for(Ray ray : rays) {
				tmpVec3.set(ray.origin).add(ray.direction);
				r.line(ray.origin, tmpVec3);
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

		debugRenderer.setProjectionMatrix(cam.combined);
		debugRenderer.begin();

		for(Consumer<ShapeRenderer> renderable : __debugOnlyRenderables)
			renderable.accept(debugRenderer);

		debugRenderer.end();

	}

	@Override
	public void pause() {}

	@Override
	public void resume() {}

	@Override
	public void dispose() {}

	public static void start(CSGMesh... meshes) {
		start(meshes, new Ray[0]);
	}

	public static void start(CSGMesh[] meshes, Ray[] rays) {

		if(Gdx.gl != null) {

			Display.destroy();
			Gdx.gl = null;
			Gdx.graphics = null;
			Gdx.gl20 = null;
			Gdx.gl30 = null;
			Gdx.gl31 = null;
			Gdx.gl32 = null;
		}

		try {
			new LwjglApplication(new CSGMeshViewer(meshes, rays),
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