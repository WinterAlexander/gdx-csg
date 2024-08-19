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

	private final Vector3 tmpBarycentric = new Vector3();

	private final Vector3 dir1 = new Vector3(), dir2 = new Vector3();

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
		normal.set(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z);
		normal.crs(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
		normal.scl(-1f).nor();

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

	public Triangle sub(Vector3 subtract) {
		return sub(subtract.x, subtract.y, subtract.z);
	}

	public boolean epsilonEquals(Triangle other, float epsilon) {
		return p1.epsilonEquals(other.p1, epsilon)
				&& p2.epsilonEquals(other.p2, epsilon)
				&& p3.epsilonEquals(other.p3, epsilon);
	}

	public Vector3 getBarycentricCoordinates(Vector3 point) {
		dir1.set(p2).sub(p1);
		dir2.set(dir1).crs(getNormal());

		float len2 = dir1.len2();
		float height2 = dir2.dot(p3.x - p1.x,
				p3.y - p1.y,
				p3.z - p1.z);

		float x = dir1.dot(point.x - p1.x, point.y - p1.y, point.z - p1.z) / len2;
		float y = dir2.dot(point.x - p1.x, point.y - p1.y, point.z - p1.z) / height2;

		float xA = 0f;
		float yA = 0f;

		float xB = 1f;
		float yB = 0f;

		float xC = dir1.dot(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z) / len2;
		float yC = 1f;

		tmpBarycentric.x = (-(x - xB) * (yC - yB) + (y - yB) * (xC - xB))
				/ (-(xA - xB) * (yC - yB) + (yA - yB) * (xC - xB));
		tmpBarycentric.y = (-(x - xC) * (yA - yC) + (y - yC) * (xA - xC))
				/ (-(xB - xC) * (yA - yC) + (yB - yC) * (xA - xC));
		tmpBarycentric.z = 1f - tmpBarycentric.x - tmpBarycentric.y;

		return tmpBarycentric;
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

	@Override
	public String toString() {
		return p1 + ", " + p2 + ", " + p3;
	}
}
