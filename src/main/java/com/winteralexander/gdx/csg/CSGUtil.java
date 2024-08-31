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
	 * @see #subtraction(Model, Model, CSGConfiguration)
	 */
	public static void subtraction(Model minuend, Model subtrahend) {
		subtraction(minuend, subtrahend, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #intersection(Model, Model, CSGConfiguration)
	 */
	public static void intersection(Model first, Model second) {
		intersection(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #union(Model, Model, CSGConfiguration)
	 */
	public static void union(Model first, Model second) {
		union(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #subtraction(Model, Mesh, CSGConfiguration)
	 */
	public static void subtraction(Model minuend, Mesh subtrahend) {
		subtraction(minuend, subtrahend, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #subtraction(Model, CSGMesh, CSGConfiguration)
	 */
	public static void subtraction(Model minuend, CSGMesh subtrahend) {
		subtraction(minuend, subtrahend, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #union(Model, Mesh, CSGConfiguration)
	 */
	public static void union(Model first, Mesh second) {
		union(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #union(Model, CSGMesh, CSGConfiguration)
	 */
	public static void union(Model first, CSGMesh second) {
		union(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #intersection(Model, Mesh, CSGConfiguration)
	 */
	public static void intersection(Model first, Mesh second) {
		intersection(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #intersection(Model, CSGMesh, CSGConfiguration)
	 */
	public static void intersection(Model first, CSGMesh second) {
		intersection(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #subtraction(Mesh, Mesh, CSGConfiguration)
	 */
	public static Mesh subtraction(Mesh minuend, Mesh subtrahend) {
		return subtraction(minuend, subtrahend, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #union(Mesh, Mesh, CSGConfiguration)
	 */
	public static Mesh union(Mesh first, Mesh second) {
		return union(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #intersection(Mesh, Mesh, CSGConfiguration)
	 */
	public static Mesh intersection(Mesh first, Mesh second) {
		return intersection(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #subtraction(CSGMesh, CSGMesh, CSGConfiguration)
	 */
	public static CSGMesh subtraction(CSGMesh minuend, CSGMesh subtrahend) {
		return subtraction(minuend, subtrahend, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #union(CSGMesh, CSGMesh, CSGConfiguration)
	 */
	public static CSGMesh union(CSGMesh first, CSGMesh second) {
		return union(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * @see #intersection(CSGMesh, CSGMesh, CSGConfiguration)
	 */
	public static CSGMesh intersection(CSGMesh first, CSGMesh second) {
		return intersection(first, second, CSGConfiguration.DEFAULT);
	}

	/**
	 * Performs subtraction on a given {@link Model} and modifies this model internally.
	 * Subtraction subtracts the second Model from the first one, deleting the part of the first
	 * mesh that is in the second and adding to it the inverted part of the second mesh that is in
	 * the first.
	 * Does not support {@link Model} with multiple mesh parts per meshes.
	 * @param minuend model to perform subtraction on
	 * @param subtrahend mesh to be subtracted from the model
	 * @param config CSG configuration to use
	 */
	public static void subtraction(Model minuend, Model subtrahend, CSGConfiguration config) {
		for(Mesh mesh : subtrahend.meshes)
			subtraction(minuend, mesh, config);
	}

	/**
	 * Performs union on a given {@link Model} and modifies this model internally. Union combines
	 * both meshes and removes the intersection of the 2 meshes (the insides).
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform union on
	 * @param second mesh to be added to the model
	 * @param config CSG configuration to use
	 */
	public static void intersection(Model first, Model second, CSGConfiguration config) {
		for(Mesh mesh : second.meshes)
			intersection(first, mesh, config);
	}

	/**
	 * Performs intersection on a given {@link Model} and modifies this model internally.
	 * Intersection includes the parts of the meshes that is within each other and deletes
	 * everything else.
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform intersection on
	 * @param second mesh to be intersected with the model
	 * @param config CSG configuration to use
	 */
	public static void union(Model first, Model second, CSGConfiguration config) {
		for(Mesh mesh : second.meshes)
			union(first, mesh, config);
	}

	/**
	 * Performs subtraction on a given {@link Model} and modifies this model internally.
	 * Subtraction subtracts the second mesh from the first one, deleting the part of the first mesh
	 * that is in the second and adding to it the inverted part of the second mesh that is in the
	 * first.
	 * Does not support {@link Model} with multiple mesh parts per meshes.
	 * @param minuend model to perform subtraction on
	 * @param subtrahend mesh to be subtracted from the model
	 * @param config CSG configuration to use
	 */
	public static void subtraction(Model minuend, Mesh subtrahend, CSGConfiguration config) {
		subtraction(minuend, CSGMesh.fromMesh(subtrahend), config);
	}

	/**
	 * Performs subtraction on a given {@link Model} and modifies this model internally.
	 * Subtraction subtracts the second mesh from the first one, deleting the part of the first mesh
	 * that is in the second and adding to it the inverted part of the second mesh that is in the
	 * first.
	 * Does not support {@link Model} with multiple mesh parts per meshes.
	 * @param minuend model to perform subtraction on
	 * @param subtrahend mesh to be subtracted from the model
	 * @param config CSG configuration to use
	 */
	public static void subtraction(Model minuend, CSGMesh subtrahend, CSGConfiguration config) {
		for(int i = 0; i < minuend.meshes.size; i++) {
			Mesh oldMesh = minuend.meshes.get(i);
			Mesh newMesh = subtraction(CSGMesh.fromMesh(minuend.meshes.get(i)),
					subtrahend, config).toMesh();
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
	 * @param config CSG configuration to use
	 */
	public static void union(Model first, Mesh second, CSGConfiguration config) {
		union(first, CSGMesh.fromMesh(second), config);
	}

	/**
	 * Performs union on a given {@link Model} and modifies this model internally. Union combines
	 * both meshes and removes the intersection of the 2 meshes (the insides).
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform union on
	 * @param second mesh to be added to the model
	 * @param config CSG configuration to use
	 */
	public static void union(Model first, CSGMesh second, CSGConfiguration config) {
		for(int i = 0; i < first.meshes.size; i++) {
			Mesh oldMesh = first.meshes.get(i);
			Mesh newMesh = union(CSGMesh.fromMesh(first.meshes.get(i)), second, config).toMesh();
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
	 * @param config CSG configuration to use
	 */
	public static void intersection(Model first, Mesh second, CSGConfiguration config) {
		intersection(first, CSGMesh.fromMesh(second), config);
	}

	/**
	 * Performs intersection on a given {@link Model} and modifies this model internally.
	 * Intersection includes the parts of the meshes that is within each other and deletes
	 * everything else.
	 * Does not support {@link Model} with multiple mesh parts per meshes
	 *
	 * @param first model to perform intersection on
	 * @param second mesh to be intersected with the model
	 * @param config CSG configuration to use
	 */
	public static void intersection(Model first, CSGMesh second, CSGConfiguration config) {
		for(int i = 0; i < first.meshes.size; i++) {
			Mesh oldMesh = first.meshes.get(i);
			Mesh newMesh = intersection(CSGMesh.fromMesh(first.meshes.get(i)),
					second, config).toMesh();
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
	 * @param config CSG configuration to use
	 * @return result of the subtraction
	 */
	public static Mesh subtraction(Mesh minuend, Mesh subtrahend, CSGConfiguration config) {
		return subtraction(CSGMesh.fromMesh(minuend), CSGMesh.fromMesh(subtrahend), config)
				.toMesh();
	}

	/**
	 * Creates a CSG union of the 2 provided libGDX {@link Mesh} and returns a new mesh that is the
	 * result of the union, removing any inner intersection to the meshes. The use of this function
	 * is not recommended if the caller intends to perform multiple CSG operations on the meshes.
	 * Instead {@link #union(CSGMesh, CSGMesh)} should be used to avoid converting from {@link Mesh}
	 * to {@link CSGMesh} multiple times
	 *
	 * @param first first mesh in the union
	 * @param second second mesh in the union
	 * @param config CSG configuration to use
	 * @return new mesh created to be the union of the 2 provided meshes
	 */
	public static Mesh union(Mesh first, Mesh second, CSGConfiguration config) {
		return union(CSGMesh.fromMesh(first), CSGMesh.fromMesh(second), config).toMesh();
	}

	/**
	 * Creates a CSG intersection of the 2 provided libGDX {@link Mesh} and returns a new mesh that
	 * is the result of the intersection. The use of this function is not recommended if the caller
	 * intends to perform multiple CSG operations on the meshes. Instead
	 * {@link #intersection(CSGMesh, CSGMesh)} should be used to avoid converting from {@link Mesh}
	 * to {@link CSGMesh} multiple times
	 *
	 * @param first first mesh in the intersection
	 * @param second second mesh in the intersection
	 * @param config CSG configuration to use
	 * @return new mesh created to be the intersection of the 2 provided meshes
	 */
	public static Mesh intersection(Mesh first, Mesh second, CSGConfiguration config) {
		return intersection(CSGMesh.fromMesh(first), CSGMesh.fromMesh(second), config).toMesh();
	}

	/**
	 * Performs CSG subtraction the 2 provided {@link CSGMesh} and returns a new {@link CSGMesh}
	 * for the result
	 *
	 * @param minuend starting mesh
	 * @param subtrahend mesh to subtract from minuend
	 * @param config CSG configuration to use
	 * @return new mesh which is the result of the subtraction
	 */
	public static CSGMesh subtraction(CSGMesh minuend,
	                                  CSGMesh subtrahend,
	                                  CSGConfiguration config) {
		CSGMesh copy1 = minuend.cpy();
		CSGMesh copy2 = subtrahend.cpy();
		copy1.setConfig(config);
		copy2.setConfig(config);

		copy1.splitTriangles(subtrahend);
		copy2.splitTriangles(minuend);

		copy1.classifyFaces(subtrahend);
		copy2.classifyFaces(minuend);

		copy1.removeFaces(true, true);
		copy2.removeFaces(false, true);

		copy2.invertTriangles();
		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		return copy1;
	}

	/**
	 * Creates a union of the 2 provided {@link CSGMesh}, removing any inner intersection and
	 * returns the result
	 *
	 * @param first first member of the mesh union
	 * @param second second member of the mesh union
	 * @param config CSG configuration to use
	 * @return result of the union
	 */
	public static CSGMesh union(CSGMesh first, CSGMesh second, CSGConfiguration config) {
		CSGMesh copy1 = first.cpy();
		CSGMesh copy2 = second.cpy();
		copy1.setConfig(config);
		copy2.setConfig(config);

		copy1.splitTriangles(second);
		copy2.splitTriangles(first);

		copy1.classifyFaces(second);
		copy2.classifyFaces(first);

		copy1.removeFaces(true, false);
		copy2.removeFaces(true, true);

		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		return copy1;
	}

	/**
	 * Creates an intersection of the 2 provided {@link CSGMesh} and returns the result
	 *
	 * @param first first member of the intersection
	 * @param second second member of the intersection
	 * @param config CSG configuration to use
	 * @return result of the intersection
	 */
	public static CSGMesh intersection(CSGMesh first, CSGMesh second, CSGConfiguration config) {
		CSGMesh copy1 = first.cpy();
		CSGMesh copy2 = second.cpy();
		copy1.setConfig(config);
		copy2.setConfig(config);

		copy1.splitTriangles(second);
		copy2.splitTriangles(first);

		copy1.classifyFaces(second);
		copy2.classifyFaces(first);

		copy1.removeFaces(false, false);
		copy2.removeFaces(false, true);

		copy1.mergeWith(copy2);
		copy1.clearInsideStatus();

		return copy1;
	}
}
