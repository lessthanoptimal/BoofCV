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

package boofcv.alg.mvs;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import java.util.List;

/**
 * Creates a point cloud from multiple disparity images. An effort is made to avoid adding the same point twice
 * to the cloud. Initially the cloud is kept in separate lists to make it easy to see which view contributed
 * what points to the cloud.
 *
 * @author Peter Abeles
 */
public class CreateCloudFromDisparityImages {

	/**
	 * If the cloud point projects to a pixel with a disparity within this tolerance the pixel will not be added.
	 * Units are in disparity pixels.
	 */
	public double disparitySimilarTol = 1.0;

	/** List of all the points in the cloud */
	final @Getter DogArray<Point3D_F64> cloud = new DogArray<>(Point3D_F64::new, p -> p.setTo(0, 0, 0));
	/** List of indices which specify the cloud size when a view 'i' was added. idx[i] &le; cloud < idx[i+1] */
	final @Getter DogArray_I32 viewPointIdx = new DogArray_I32();

	// Masked used to filter out duplicate points
	final GrayU8 duplicateMask = new GrayU8(1, 1);

	/**
	 * Clears previously added views and points.
	 */
	public void reset() {
		cloud.reset();
		viewPointIdx.reset();
		viewPointIdx.add(0);
	}

	/**
	 * Adds a point cloud. This can be used to add prior data.
	 *
	 * @param cloud (Input) A point cloud. This is copied.
	 * @return The index of the view that can be used to retrieve the specified points added
	 */
	public int addCloud( List<Point3D_F64> cloud ) {
		viewPointIdx.add(this.cloud.size + cloud.size());
		this.cloud.copyAll(cloud, ( s, d ) -> d.setTo(s));
		return this.viewPointIdx.size - 1;
	}

	/**
	 * <p>Add points from the inverse depth image</p>
	 *
	 * NOTE: The reason point and pixel transforms are used is that combined disparity images might include lens
	 * distortion.
	 *
	 * @param inverseDepth (Input) Disparity image
	 * @param world_to_view (Input) Transform from world to view reference frame
	 * @param norm_to_pixel (Input) Transform from normalized image coordinates into pixels
	 * @param pixel_to_norm (Input) Transform from pixels into normalized image coordinates.
	 * @return The index of the view that can be used to retrieve the specified points added
	 */
	public int addInverseDepth( GrayF32 inverseDepth, Se3_F64 world_to_view,
								Point2Transform2_F64 norm_to_pixel,
								PixelTransform<Point2D_F64> pixel_to_norm ) {

		// TODO disparitySimilarTol compute this dynamically based on stereo baseline
		duplicateMask.reshape(inverseDepth);
		GImageMiscOps.fill(duplicateMask, 0);
		MultiViewStereoOps.maskOutPointsInCloud(cloud.toList(), inverseDepth, world_to_view,
				norm_to_pixel, disparitySimilarTol, duplicateMask);

		// normalized image coordinates of disparity image
		final Point2D_F64 norm = new Point2D_F64();
		// 3D point in stereo camera reference frame
		final Point3D_F64 camP = new Point3D_F64();

		for (int y = 0; y < inverseDepth.height; y++) {
			int indexDisp = y*inverseDepth.stride + inverseDepth.startIndex;
			int indexMask = y*duplicateMask.stride + duplicateMask.startIndex;

			for (int x = 0; x < inverseDepth.width; x++, indexDisp++, indexMask++) {
				// Check to see if it has been masked out as invalid
				if (duplicateMask.data[indexMask] != 0)
					continue;
				float inv = inverseDepth.data[indexDisp];

				// if the inverse is invalid or at infinity, skip
				// points at infinity can't be handled since cloud is in cartesian coordinates
				if (inv <= 0.0)
					continue;

				// Get normalized image coordinates.
				pixel_to_norm.compute(x, y, norm);

				// Find 3D point in camera reference frame
				camP.x = norm.x/inv;
				camP.y = norm.y/inv;
				camP.z = 1.0/inv;

				// Left to world frame
				SePointOps_F64.transformReverse(world_to_view, camP, cloud.grow());
			}
		}

		// Denote where this set of points end
		viewPointIdx.add(cloud.size());

		return this.viewPointIdx.size - 1;
	}
}
