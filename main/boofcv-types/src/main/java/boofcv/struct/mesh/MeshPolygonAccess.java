/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.mesh;

import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;

/**
 * Provides access to an arbitrary mesh. The mesh is assumed to be stored in a format with planar polygons.
 * The idea is that you can access a mesh in this abstract format and not need to convert it to some other common
 * format first
 */
public interface MeshPolygonAccess {
	/** Number of polygons in this mesh */
	int size();

	/**
	 * Retries a planar polygon in 3D
	 *
	 * @param which Which polygon should it access
	 * @param vertexes Storage for vertexes on the polygon
	 */
	void getPolygon( int which, DogArray<Point3D_F64> vertexes );
}
