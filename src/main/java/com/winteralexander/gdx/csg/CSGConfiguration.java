package com.winteralexander.gdx.csg;

/**
 * Configuration for CSG operations
 * <p>
 * Created on 2024-08-22.
 *
 * @author Alexander Winter
 */
public class CSGConfiguration {
	public static final CSGConfiguration DEFAULT = new CSGConfiguration();

	public float tolerance = 1e-5f;
	public boolean enableMerging = true;
	public boolean enableBoundaryFaces = true;
}
