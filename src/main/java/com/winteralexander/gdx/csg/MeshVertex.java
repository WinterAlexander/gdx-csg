package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * A vertex in a {@link CSGMesh}
 * <p>
 * Created on 2024-08-11.
 *
 * @author Alexander Winter
 */
public class MeshVertex {
	private final Vector3 position;
	private final Vector3 normal;
	private final Color color;
	private final Vector2 texCoordinates;

	public MeshVertex() {
		this(new Vector3(), new Vector3(), new Color(), new Vector2());
	}

	public MeshVertex(Vector3 position,
	                  Vector3 normal,
	                  Color color,
	                  Vector2 texCoordinates) {
		ensureNotNull(position, "position");
		ensureNotNull(normal, "normal");
		ensureNotNull(color, "color");
		ensureNotNull(texCoordinates, "texCoordinates");
		this.position = position;
		this.normal = normal;
		this.color = color;
		this.texCoordinates = texCoordinates;
	}

	public Vector3 getPosition() {
		return position;
	}

	public Vector3 getNormal() {
		return normal;
	}

	public Color getColor() {
		return color;
	}

	public Vector2 getTexCoordinates() {
		return texCoordinates;
	}
}
