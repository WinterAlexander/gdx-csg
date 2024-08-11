package com.winteralexander.gdx.csg;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Segment;

/**
 * Extension of {@link Segment} with convenience null constructor and toString function
 * <p>
 * Created on 2024-08-09.
 *
 * @author Alexander Winter
 */
public class SegmentPlus extends Segment {
	public SegmentPlus() {
		this(0f, 0f, 0f, 0f, 0f, 0f);
	}

	public SegmentPlus(Vector3 a, Vector3 b) {
		super(a, b);
	}

	public SegmentPlus(float aX, float aY, float aZ,
	                   float bX, float bY, float bZ) {
		super(aX, aY, aZ, bX, bY, bZ);
	}

	public boolean epsilonEquals(Segment other, float epsilon) {
		return a.epsilonEquals(other.a, epsilon) && b.epsilonEquals(other.b, epsilon)
				|| a.epsilonEquals(other.b, epsilon) && b.epsilonEquals(other.a, epsilon);
	}

	@Override
	public String toString() {
		return a + " -> " + b;
	}
}
