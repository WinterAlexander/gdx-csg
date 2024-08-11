package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Vector3;

import static com.winteralexander.gdx.utils.Validation.ensureNotNull;

/**
 * A triangle in 3D space
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

	public Triangle(float[] array) {
		set(array);
	}

	public Triangle(float[] array, int offset) {
		set(array, offset);
	}

	public Triangle(float x1, float y1, float z1,
	                float x2, float y2, float z2,
	                float x3, float y3, float z3) {
		set(x1, y1, z1, x2, y2, z2, x3, y3, z3);
	}

	public Triangle(Vector3 p1, Vector3 p2, Vector3 p3) {
		set(p1, p2, p3);
	}

	public Vector3 getNormal() {
		normal.set(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
		normal.crs(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z);
		normal.nor();

		return normal;
	}

	public Triangle set(float x1, float y1, float z1,
	                    float x2, float y2, float z2,
	                    float x3, float y3, float z3) {
		p1.set(x1, y1, z1);
		p2.set(x2, y2, z2);
		p3.set(x3, y3, z3);
		return this;
	}

	public Triangle set(float[] array) {
		return set(array, 0);
	}

	public Triangle set(float[] array, int offset) {
		return set(array[offset], array[offset + 1], array[offset + 2],
				array[offset + 3], array[offset + 4], array[offset + 5],
				array[offset + 6], array[offset + 7], array[offset + 8]);
	}

	public Triangle set(Vector3 p1, Vector3 p2, Vector3 p3) {
		this.p1.set(p1);
		this.p2.set(p2);
		this.p3.set(p3);
		return this;
	}

	public Triangle set(Triangle other) {
		set(other.p1, other.p2, other.p3);
		return this;
	}

	public Triangle add(float x, float y, float z) {
		p1.add(x, y, z);
		p2.add(x, y, z);
		p3.add(x, y, z);
		return this;
	}

	public Triangle add(Vector3 addend) {
		return add(addend.x, addend.y, addend.z);
	}

	public Triangle sub(float x, float y, float z) {
		p1.sub(x, y, z);
		p2.sub(x, y, z);
		p3.sub(x, y, z);
		return this;
	}

	public Triangle sub(Vector3 substract) {
		return sub(substract.x, substract.y, substract.z);
	}

	public void toArray(float[] out) {
		toArray(out, 0);
	}

	public void toArray(float[] out, int offset) {
		ensureNotNull(out, "out");
		if(out.length < offset + 9)
			throw new IllegalArgumentException("Not enough space in provided array");

		out[offset] = p1.x;
		out[offset + 1] = p1.y;
		out[offset + 2] = p1.z;

		out[offset + 3] = p2.x;
		out[offset + 4] = p2.y;
		out[offset + 5] = p2.z;

		out[offset + 6] = p3.x;
		out[offset + 7] = p3.y;
		out[offset + 8] = p3.z;
	}
}
