/*
 * Copyright (c) 2025, Peter Abeles. All Rights Reserved.
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
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;

/**
 * Dynamically computes vertex color based on surface normal relative angle. Green intensity is based on normal
 * angle while red and blue colors are based on the x-y vector angle. It can be applied to the global or view
 * coordinate systems.
 */
public class MeshColorYawRBNormalG implements RenderMesh.SurfaceColor {

	// The mesh
	protected VertexMesh mesh;
	// color applied to each vertex
	protected int[] colors;

	// precomputed lookup table for colors red and blue, based on angle
	protected int[] red;
	protected int[] blue;

	// If applied to global frame or view frame
	protected boolean useGlobal;

	public MeshColorYawRBNormalG( VertexMesh mesh, boolean useGlobal ) {
		this.mesh = mesh;
		this.useGlobal = useGlobal;

		colors = new int[mesh.size()];

		red = new int[361];
		blue = new int[361];
		int half = 255/2;
		int diff = 255 - half;
		for (int degree = 0; degree < red.length; degree++) {
			double yaw = degree*Math.PI/180.0;
			red[degree] = (int)(Math.sin(yaw)*half + diff);
			blue[degree] = (int)(Math.sin(yaw + (4*Math.PI/3))*half + diff);
		}

		if (useGlobal) {
			apply(new Se3_F64());
		}
	}

	@Override public void setWorldToView( Se3_F64 worldToCamera ) {
		if (!useGlobal)
			apply(worldToCamera);
	}

	public void apply( Se3_F64 worldToCamera ) {
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

			GeometryMath_F64.mult(worldToCamera.R, normal, normal);

			// Dot will be -1 to 1. -1 is pointing towards the camera
			double dot = normal.dot(axisZ);
			dot /= normal.norm();

			double yaw = Math.atan2(normal.y, normal.x);
			int iyaw = (int)(360*(yaw + Math.PI)/(2.0*Math.PI));
			int idot = (int)(dot*dot*256)%256;

			colors[i] = 0xFF000000 | (red[iyaw] << 16) | (idot << 8) | blue[iyaw];
		}
	}

	@Override public int surfaceRgb( int i ) {
		return colors[i];
	}
}
