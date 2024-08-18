package com.winteralexander.gdx.csg;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;

/**
 * Utility class to perform Constructive Solid Geometry (CSG) operations on libGDX {@link Mesh}es.
 * <p>
 * Created on 2024-08-11.
 *
 * @author Alexander Winter
 */
public class CSGUtil {
	private CSGUtil() {}

	/**
	 * Performs subtraction on a given {@link Model} and modifies this model internally.
	 * Subtraction subtracts the second mesh from the first one, deleting the part of the first mesh
	 * that is in the second and adding to it the inverted part of the second mesh that is in the
	 * first.
	 * Does not support {@link Model} with multiple mesh parts per meshes.
	 * @param minuend model to perform subtraction on
	 * @param subtrahend mesh to be subtracted from the model
	 */
	public static void subtraction(Model minuend, Mesh subtrahend) {
		subtraction(minuend, CSGMesh.fromMesh(subtrahend));
	}

	/**
	 * Performs subtraction on a given {@link Model} and modifies this model internally.
	 * Subtraction subtracts the second mesh from the first one, deleting the part of the first mesh
	 * that is in the second and adding to it the inverted part of the second mesh that is in the
	 * first.
	 * Does not support {@link Model} with multiple mesh parts per meshes.
	 * @param minuend model to perform subtraction on
	 * @param subtrahend mesh to be subtracted from the model
	 */
	public static void subtraction(Model minuend, CSGMesh subtrahend) {
		for(int i = 0; i < minuend.meshes.size; i++) {
			Mesh oldMesh = minuend.meshes.get(i);
			Mesh newMesh = subtraction(CSGMesh.fromMesh(minuend.meshes.get(i)), subtrahend).toMesh();
			minuend.meshes.set(i, newMesh);
			for(MeshPart part : minuend.meshParts) {
				if(part.mesh == oldMesh) {
					part.set(part.id, newMesh, 0, newMesh.getNumIndices(), part.primitiveType);
					part.update();
				}
			}
		}
	}

	/**
	 * Performs union on a given {@link Model} and modifies this model internally. Union combines
   	 * both meshes and removes the intersection of the 2 meshes (the insides).
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform union on
	 * @param second mesh to be added to the model
	 */
	public static void union(Model first, Mesh second) {
		union(first, CSGMesh.fromMesh(second));
	}

	/**
	 * Performs union on a given {@link Model} and modifies this model internally. Union combines
	 * both meshes and removes the intersection of the 2 meshes (the insides).
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform union on
	 * @param second mesh to be added to the model
	 */
	public static void union(Model first, CSGMesh second) {
		for(int i = 0; i < first.meshes.size; i++) {
			Mesh oldMesh = first.meshes.get(i);
			Mesh newMesh = union(CSGMesh.fromMesh(first.meshes.get(i)), second).toMesh();
			first.meshes.set(i, newMesh);
			for(MeshPart part : first.meshParts) {
				if(part.mesh == oldMesh) {
					part.set(part.id, newMesh, 0, newMesh.getNumIndices(), part.primitiveType);
					part.update();
				}
			}
		}
	}

	/**
	 * Performs intersection on a given {@link Model} and modifies this model internally.
	 * Intersection includes the parts of the meshes that is within each other and deletes
	 * everything else.
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform intersection on
	 * @param second mesh to be intersected with the model
	 */
	public static void intersection(Model first, Mesh second) {
		intersection(first, CSGMesh.fromMesh(second));
	}

	/**
	 * Performs intersection on a given {@link Model} and modifies this model internally.
	 * Intersection includes the parts of the meshes that is within each other and deletes
	 * everything else.
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform intersection on
	 * @param second mesh to be intersected with the model
	 */
	public static void intersection(Model first, CSGMesh second) {
		for(int i = 0; i < first.meshes.size; i++) {
			Mesh oldMesh = first.meshes.get(i);
			Mesh newMesh = intersection(CSGMesh.fromMesh(first.meshes.get(i)), second).toMesh();
			first.meshes.set(i, newMesh);
			for(MeshPart part : first.meshParts) {
				if(part.mesh == oldMesh) {
					part.set(part.id, newMesh, 0, newMesh.getNumIndices(), part.primitiveType);
					part.update();
				}
			}
		}
	}

	/**
	 * Performs CSG subtraction on the first mesh with the second mesh and returns a new mesh that
	 * is the result. The use of this function is not recommended if the caller intends to perform
	 * multiple CSG operations on the meshes. Instead {@link #subtraction(CSGMesh, CSGMesh)}
	 * should be used to avoid converting from {@link Mesh} to {@link CSGMesh} multiple times.
	 *
	 * @param minuend starting mesh
	 * @param subtrahend mesh to subtract
	 * @return result of the subtraction
	 */
	public static Mesh subtraction(Mesh minuend, Mesh subtrahend) {
		return subtraction(CSGMesh.fromMesh(minuend), CSGMesh.fromMesh(subtrahend)).toMesh();
	}

	public static Mesh union(Mesh first, Mesh second) {
		return union(CSGMesh.fromMesh(first), CSGMesh.fromMesh(second)).toMesh();
	}

	public static Mesh intersection(Mesh first, Mesh second) {
		return intersection(CSGMesh.fromMesh(first), CSGMesh.fromMesh(second)).toMesh();
	}

	public static CSGMesh subtraction(CSGMesh minuend, CSGMesh subtrahend) {
		CSGMesh copy1 = minuend.cpy();
		CSGMesh copy2 = subtrahend.cpy();

		copy1.splitTriangles(subtrahend);
		copy2.splitTriangles(minuend);

		copy1.classifyFaces(subtrahend);
		copy2.classifyFaces(minuend);

		copy1.removeFaces(true);
		copy2.removeFaces(false);

		copy2.invertTriangles();
		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		return copy1;
	}

	public static CSGMesh union(CSGMesh first, CSGMesh second) {
		CSGMesh copy1 = first.cpy();
		CSGMesh copy2 = second.cpy();

		copy1.splitTriangles(second);
		copy2.splitTriangles(first);

		copy1.classifyFaces(second);
		copy2.classifyFaces(first);

		copy1.removeFaces(true);
		copy2.removeFaces(true);

		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		return copy1;
	}

	public static CSGMesh intersection(CSGMesh first, CSGMesh second) {
		CSGMesh copy1 = first.cpy();
		CSGMesh copy2 = second.cpy();

		copy1.splitTriangles(second);
		copy2.splitTriangles(first);

		copy1.classifyFaces(second);
		copy2.classifyFaces(first);

		copy1.removeFaces(false);
		copy2.removeFaces(false);

		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		return copy1;
	}
}
