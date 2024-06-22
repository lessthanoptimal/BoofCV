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

package boofcv.gui.mesh;

import boofcv.struct.mesh.VertexMesh;
import boofcv.visualize.RenderMesh;
import georegression.geometry.UtilVector3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import org.ddogleg.struct.DogArray;
import org.ejml.UtilEjml;

/**
 * Different functions that compute a synthetic colors for each surface in a mesh.
 *
 * @author Peter Abeles
 */
public class MeshColorizeOps {
	/**
	 * Colorizes each surface based on the color of the first vertex
	 *
	 * @param mesh The mesh
	 * @param vertexColor Color for each vertex in the mesh
	 * @return SurfaceColor
	 */
	public static RenderMesh.SurfaceColor colorizeByVertex( VertexMesh mesh, int[] vertexColor ) {
		return ( shapeIdx ) -> vertexColor[mesh.faceVertexes.get(mesh.faceOffsets.get(shapeIdx))];
	}

	/**
	 * Computes the normal angle and varies the color between green and white depending on the acute angle
	 * relative to the world's z-xis
	 */
	public static RenderMesh.SurfaceColor colorizeByNormal( VertexMesh mesh ) {
		int[] colors = new int[mesh.size()];

		var facet = new DogArray<>(Point3D_F64::new);
		var va = new Vector3D_F64();
		var vb = new Vector3D_F64();
		var normal = new Vector3D_F64();
		var axisZ = new Vector3D_F64(0, 0, 1);

		for (int i = 0; i < mesh.size(); i++) {
			mesh.getFaceVectors(i, facet);

			// Handle case of invalid facet gracefully by assigning it to an arbitrary color
			if (facet.size < 3) {
				colors[i] = 0xFFFF0000;
				continue;
			}

			va.minus(facet.get(1), facet.get(0));
			vb.minus(facet.get(2), facet.get(0));
			normal.crossSetTo(va, vb);
			double angle = UtilVector3D_F64.acute(normal, axisZ);

			int green = (int)(0xFF*angle/UtilEjml.PI);
			colors[i] = 0xFF000000 | (green << 8);
		}

		return ( index ) -> colors[index];
	}
}
