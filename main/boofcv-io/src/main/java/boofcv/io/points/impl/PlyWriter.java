/*
 * Copyright (c) 2024, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.io.points.impl;

import georegression.struct.point.Point3D_F64;

/**
 * Generic interface for accessing data used to write PLY files
 *
 * @author Peter Abeles
 */
public interface PlyWriter {
	int getVertexCount();

	int getPolygonCount();

	/** True if a vertex has color information or not */
	boolean isColor();

	/** True if a vertex has a texture coordinate */
	boolean isTextured();

	void getVertex( int which, Point3D_F64 vertex );

	/**
	 * Color of a vertex
	 */
	int getColor( int which );

	/**
	 * Get the indexes of each vertex in the polygon. Returns the number of vertexes.
	 */
	int getIndexes( int which, int[] indexes );

	/**
	 * Reads the texture coordinate for the specified shape. Returns the number of vertexes for that shape.
	 *
	 * @param which Which shape
	 * @param coordinates Texture coordinate of the specified vertex
	 */
	int getTextureCoors( int which, float[] coordinates );
}
