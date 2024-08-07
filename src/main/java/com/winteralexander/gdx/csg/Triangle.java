package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Vector3;

/**
 * TODO Undocumented :(
 * <p>
 * Created on 2024-08-07.
 *
 * @author Alexander Winter
 */
public class Triangle {
	public final Vector3 p1 = new Vector3();
	public final Vector3 p2 = new Vector3();
	public final Vector3 p3 = new Vector3();

	private final Vector3 normal = new Vector3();

	public Triangle() {}

	public Triangle(float x1, float y1, float z1,
	                float x2, float y2, float z2,
	                float x3, float y3, float z3) {
		p1.set(x1, y1, z1);
		p2.set(x2, y2, z2);
		p3.set(x3, y3, z3);
	}

	public Triangle(Vector3 p1, Vector3 p2, Vector3 p3) {
		this.p1.set(p1);
		this.p2.set(p2);
		this.p3.set(p3);
	}

	public Vector3 getNormal() {
		normal.set(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
		normal.crs(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z);
		normal.nor();

		return normal;
	}
}
