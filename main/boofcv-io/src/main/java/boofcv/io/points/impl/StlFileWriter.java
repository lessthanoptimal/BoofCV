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

package boofcv.io.points.impl;

import boofcv.io.points.MeshPolygonAccess;
import boofcv.io.points.StlDataStructure;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;

import java.io.IOException;
import java.io.Writer;

/**
 * Writes a file that's in STL format in a {@link StlDataStructure} as input.
 *
 * @see StlDataStructure
 */
public class StlFileWriter {

	public void ascii( MeshPolygonAccess mesh, String name, Writer writer ) throws IOException {
		// Massage the name to make it compatible with this format
		String nameMassaged = name.trim().replaceAll("\\s", "");
		// Add the name line for the solid. Remove all white spaces to avoid any issues
		writer.write("solid " + name);

		var poly = new DogArray<>(Point3D_F64::new);
		for (int polygonIdx = 0; polygonIdx < mesh.size(); polygonIdx++) {
			mesh.getPolygon(polygonIdx, poly);

			// TODO fit plane to the polygon
			// TODO make sure normal is pointing in the correct direction
			// TODO convert plane to 2D
			// TODO break up into triangles
			// TODO convert triangles back to 3D and add to mesh
		}

		writer.write("endsolid " + name);
	}
}
