/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.meshing;

import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;

/**
 * Samples disparity image in a regular grid pattern. If it finds 4 corners that are valid it creates 2 triangles
 * from them. Can't get much simpler than this.
 *
 * @author Peter Abeles
 */
public class DisparityToMeshGridSample {
	/** If the disparity difference in a sample region is greater than this, the region is skipped */
	public float maxDisparityJump = 2.0f;

	/** Number of pixels in the regular grid that it samples. Relative to (w+h)/2 */
	public ConfigLength samplePeriod = ConfigLength.fixed(4);

	/** Found mesh */
	@Getter VertexMesh mesh = new VertexMesh();

	/** Pixel coordinate each vertex came from. Useful for adding color */
	@Getter DogArray<Point2D_F64> vertexPixels = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);

	// Workspace
	Point3D_F64 p0 = new Point3D_F64();
	Point3D_F64 p1 = new Point3D_F64();
	Point3D_F64 p2 = new Point3D_F64();
	Point3D_F64 p3 = new Point3D_F64();

	/**
	 * Processes the disparity image and returns all the found 3D meshes
	 *
	 * @param parameters Stereo parameters
	 * @param disparity disparity image
	 */
	public void process( DisparityParameters parameters, GrayF32 disparity ) {
		mesh.reset();
		vertexPixels.reset();

		int skip = samplePeriod.computeI((disparity.width + disparity.height)/2);

		// Step through the entire image
		for (int y = 0; y < disparity.height - skip; y += skip) {
			int y1 = y + skip;
			for (int x = 0; x < disparity.width - skip; x += skip) {
				int x1 = x + skip;

				double d0 = disparity.get(x, y);
				double d1 = disparity.get(x1, y);
				double d2 = disparity.get(x1, y1);
				double d3 = disparity.get(x, y1);

				// If there's a large change in disparity it's probably not part of the same surface
				if (Math.abs(d0 - d2) > maxDisparityJump)
					continue;
				if (Math.abs(d0 - d1) > maxDisparityJump)
					continue;
				if (Math.abs(d0 - d3) > maxDisparityJump)
					continue;

				if (!parameters.pixelTo3D(x, y, d0, p0))
					continue;
				if (!parameters.pixelTo3D(x1, y, d1, p1))
					continue;
				if (!parameters.pixelTo3D(x1, y1, d2, p2))
					continue;
				if (!parameters.pixelTo3D(x, y1, d3, p3))
					continue;

				// save where they came from
				vertexPixels.grow().setTo(x, y);
				vertexPixels.grow().setTo(x1, y);
				vertexPixels.grow().setTo(x1, y1);
				vertexPixels.grow().setTo(x, y1);

				// Create two triangles
				int idx = mesh.vertexes.size();
				mesh.vertexes.append(p3);
				mesh.vertexes.append(p2);
				mesh.vertexes.append(p1);
				mesh.vertexes.append(p0);

				mesh.triangles.add(idx);
				mesh.triangles.add(idx + 1);
				mesh.triangles.add(idx + 2);
				mesh.triangles.add(idx);
				mesh.triangles.add(idx + 2);
				mesh.triangles.add(idx + 3);
			}
		}
	}
}
