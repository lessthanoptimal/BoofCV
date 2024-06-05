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

/**
 * Generic interface for accessing data used to read PLY files
 *
 * @author Peter Abeles
 */
public interface PlyReader {
	void initialize( int vertexes, int triangles, boolean color );

	void addVertex( double x, double y, double z, int rgb );

	void addPolygon( int[] indexes, int offset, int length );

	/**
	 * Adds coordinates for texture in the ply file
	 *
	 * @param count Number of 2D points in the coordinates
	 * @param coor Array storing the 2D coordinates, interleaved (x,y)
	 */
	void addTexture( int count, float[] coor );
}
