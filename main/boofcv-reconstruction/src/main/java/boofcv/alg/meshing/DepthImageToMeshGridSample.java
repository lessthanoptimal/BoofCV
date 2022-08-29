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
import boofcv.struct.distort.PixelTransform;
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
public class DepthImageToMeshGridSample {
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

	// normalized image coordinates
	Point2D_F64 norm = new Point2D_F64();

	/**
	 * Processes the disparity image and returns all the found 3D meshes
	 *
	 * @param parameters Stereo parameters
	 * @param disparity disparity image
	 */
	public void processDisparity( DisparityParameters parameters, GrayF32 disparity ) {
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

				if (!parameters.pixelToLeft3D(x, y, d0, p0))
					continue;
				if (!parameters.pixelToLeft3D(x1, y, d1, p1))
					continue;
				if (!parameters.pixelToLeft3D(x1, y1, d2, p2))
					continue;
				if (!parameters.pixelToLeft3D(x, y1, d3, p3))
					continue;

				// save where they came from
				vertexPixels.grow().setTo(x, y);
				vertexPixels.grow().setTo(x1, y);
				vertexPixels.grow().setTo(x1, y1);
				vertexPixels.grow().setTo(x, y1);

				// Define a square
				int idx = mesh.vertexes.size();
				mesh.vertexes.append(p0);
				mesh.vertexes.append(p1);
				mesh.vertexes.append(p2);
				mesh.vertexes.append(p3);

				mesh.indexes.add(idx + 3);
				mesh.indexes.add(idx + 2);
				mesh.indexes.add(idx + 1);
				mesh.indexes.add(idx);

				mesh.offsets.add(mesh.indexes.size);
			}
		}
	}

	/**
	 * Processes the inverse depth image and returns all the found 3D meshes
	 *
	 * @param inverseDepth Stereo parameters
	 */
	public void processInvDepth( GrayF32 inverseDepth,
								 PixelTransform<Point2D_F64> pixelToNorm,
								 float maxInverseJump ) {
		mesh.reset();
		vertexPixels.reset();

		int skip = samplePeriod.computeI((inverseDepth.width + inverseDepth.height)/2);

		// Step through the entire image
		for (int y = 0; y < inverseDepth.height - skip; y += skip) {
			int y1 = y + skip;
			for (int x = 0; x < inverseDepth.width - skip; x += skip) {
				int x1 = x + skip;

				double d0 = inverseDepth.get(x, y);
				double d1 = inverseDepth.get(x1, y);
				double d2 = inverseDepth.get(x1, y1);
				double d3 = inverseDepth.get(x, y1);

				// Skip over points at infinity or invalid values
				if (d0 <= 0.0 || d1 <= 0.0 || d2 <= 0.0 || d3 <= 0.0)
					continue;

				// If there's a large change in inverse depth it's probably not part of the same surface
				if (Math.abs(d0 - d2) > maxInverseJump)
					continue;
				if (Math.abs(d0 - d1) > maxInverseJump)
					continue;
				if (Math.abs(d0 - d3) > maxInverseJump)
					continue;

				pixelToPoint(x,y,d0,pixelToNorm, p0);
				pixelToPoint(x1,y,d1,pixelToNorm, p1);
				pixelToPoint(x1,y1,d2,pixelToNorm, p2);
				pixelToPoint(x,y1,d3,pixelToNorm, p3);

				// save where they came from
				vertexPixels.grow().setTo(x, y);
				vertexPixels.grow().setTo(x1, y);
				vertexPixels.grow().setTo(x1, y1);
				vertexPixels.grow().setTo(x, y1);

				// Define a square
				int idx = mesh.vertexes.size();
				mesh.vertexes.append(p0);
				mesh.vertexes.append(p1);
				mesh.vertexes.append(p2);
				mesh.vertexes.append(p3);

				mesh.indexes.add(idx + 3);
				mesh.indexes.add(idx + 2);
				mesh.indexes.add(idx + 1);
				mesh.indexes.add(idx);

				mesh.offsets.add(mesh.indexes.size);
			}
		}
	}

	private void pixelToPoint(int x, int y, double invDepth,
							  PixelTransform<Point2D_F64> pixelToNorm, Point3D_F64 p)  {
		pixelToNorm.compute(x,y, norm);
		p.setTo(norm.x/invDepth, norm.y/invDepth, 1.0/invDepth);
	}
}
