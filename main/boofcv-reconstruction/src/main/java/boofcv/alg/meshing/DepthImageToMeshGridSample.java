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

package boofcv.alg.meshing;

import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.mesh.VertexMesh;
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
	/** Number of pixels in the regular grid that it samples. Relative to (w+h)/2 */
	public ConfigLength samplePeriod = ConfigLength.fixed(4);

	/** Found mesh */
	@Getter VertexMesh mesh = new VertexMesh();

	/** Pixel coordinate each vertex came from. Useful for adding color */
	@Getter DogArray<Point2D_F64> vertexPixels = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);

	GrayS32 indexImage = new GrayS32(1, 1);

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
	public void processDisparity( DisparityParameters parameters, GrayF32 disparity, float maxDisparityJump ) {
		mesh.reset();
		vertexPixels.reset();
		// Fill index image with -1, indicating its empty
		indexImage.reshape(disparity);
		ImageMiscOps.fill(indexImage, -1);

		int skip = samplePeriod.computeI((disparity.width + disparity.height)/2);

		// Step through the entire image
		for (int y = 0; y < disparity.height - skip; y += skip) {
			int y1 = y + skip;
			for (int x = 0; x < disparity.width - skip; x += skip) {
				int x1 = x + skip;

				float d0 = disparity.get(x, y);
				float d1 = disparity.get(x1, y);
				float d2 = disparity.get(x1, y1);
				float d3 = disparity.get(x, y1);

				// If there's a large change in disparity it's probably not part of the same surface
				if (Math.abs(d0 - d2) > maxDisparityJump)
					continue;
				if (Math.abs(d0 - d1) > maxDisparityJump)
					continue;
				if (Math.abs(d0 - d3) > maxDisparityJump)
					continue;

				if (!checkAddIndexForIDisparity(x, y, d0, parameters))
					continue;
				if (!checkAddIndexForIDisparity(x, y1, d1, parameters))
					continue;
				if (!checkAddIndexForIDisparity(x1, y1, d2, parameters))
					continue;
				if (!checkAddIndexForIDisparity(x1, y, d3, parameters))
					continue;

				mesh.faceOffsets.add(mesh.faceVertexes.size);
			}
		}
	}

	/**
	 * Adds vertexes as needed and specifies indexes in a mesh using the saved vertex indexes
	 */
	private boolean checkAddIndexForIDisparity( int x, int y, float disparity,
												DisparityParameters parameters ) {

		int index = indexImage.get(x, y);
		if (index == -1) {
			index = mesh.vertexes.size();
			indexImage.set(x, y, index);

			if (!parameters.pixelToLeft3D(x, y, disparity, p0))
				return false;

			mesh.vertexes.append(p0);

			// save where they came from, used to add color later
			vertexPixels.grow().setTo(x, y);
		}
		mesh.faceVertexes.add(index);
		return true;
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
		// Fill index image with -1, indicating its empty
		indexImage.reshape(inverseDepth);
		ImageMiscOps.fill(indexImage, -1);

		int skip = samplePeriod.computeI((inverseDepth.width + inverseDepth.height)/2);

		// Step through the entire image
		for (int y = 0; y < inverseDepth.height - skip; y += skip) {
			int y1 = y + skip;
			for (int x = 0; x < inverseDepth.width - skip; x += skip) {
				int x1 = x + skip;

				float d0 = inverseDepth.get(x, y);
				float d1 = inverseDepth.get(x1, y);
				float d2 = inverseDepth.get(x1, y1);
				float d3 = inverseDepth.get(x, y1);

				// Skip over points at infinity or invalid values
				if (d0 <= 0.0f || d1 <= 0.0f || d2 <= 0.0f || d3 <= 0.0f)
					continue;

				// If there's a large change in inverse depth it's probably not part of the same surface
				if (Math.abs(d0 - d2) > maxInverseJump)
					continue;
				if (Math.abs(d0 - d1) > maxInverseJump)
					continue;
				if (Math.abs(d0 - d3) > maxInverseJump)
					continue;

				checkAddIndexForInverseDepth(x, y, d0, pixelToNorm);
				checkAddIndexForInverseDepth(x, y1, d1, pixelToNorm);
				checkAddIndexForInverseDepth(x1, y1, d2, pixelToNorm);
				checkAddIndexForInverseDepth(x1, y, d3, pixelToNorm);

				mesh.faceOffsets.add(mesh.faceVertexes.size);
			}
		}
	}

	/**
	 * Adds vertexes as needed and specifies indexes in a mesh using the saved vertex indexes
	 */
	private void checkAddIndexForInverseDepth( int x, int y, float invDepth,
											   PixelTransform<Point2D_F64> pixelToNorm ) {
		int index = indexImage.get(x, y);
		if (index == -1) {
			index = mesh.vertexes.size();
			indexImage.set(x, y, index);

			pixelToNorm.compute(x, y, norm);
			p0.setTo(norm.x/invDepth, norm.y/invDepth, 1.0/invDepth);

			mesh.vertexes.append(p0);

			// save where they came from, used to add color later
			vertexPixels.grow().setTo(x, y);
		}
		mesh.faceVertexes.add(index);
	}
}
