/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.InputSanityCheck;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

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
	final @Getter FastQueue<Point3D_F64> cloud = new FastQueue<>(Point3D_F64::new, p -> p.setTo(0, 0, 0));
	/** List of indices which specify the cloud size when a view 'i' was added. idx[i] &le; cloud < idx[i+1] */
	final @Getter GrowQueue_I32 viewPointIdx = new GrowQueue_I32();

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
	 * Add points from the disparity image which have not been masked out.
	 *
	 * NOTE: The reason point and pixel transforms are used is that combined disparity images might include lens
	 * distortion.
	 *
	 * @param disparity (Input) Disparity image
	 * @param mask (Input,Output) Mask that specifies which points in the disparity image are valid. When an existing
	 * point in the cloud hits this image the mask is updated.
	 * @param world_to_view (Input) Transform from world to view reference frame
	 * @param parameters (Input) Describes how to interpret the disparity values
	 * @param rectNorm_to_dispPixel (Input) Transform from rectified normalized image coordinates into disparity pixels
	 * @param dispPixel_to_rectNorm (Input) Transform from disparity pixels into rectified normalized image coordinates.
	 * @return The index of the view that can be used to retrieve the specified points added
	 */
	public int addDisparity( GrayF32 disparity, GrayU8 mask, Se3_F64 world_to_view, DisparityParameters parameters,
							 Point2Transform2_F64 rectNorm_to_dispPixel,
							 PixelTransform<Point2D_F64> dispPixel_to_rectNorm ) {
		InputSanityCheck.checkSameShape(disparity, mask);

		MultiViewStereoOps.maskOutPointsInCloud(cloud.toList(), disparity, parameters, world_to_view,
				rectNorm_to_dispPixel, disparitySimilarTol, mask);

		// normalized image coordinates of disparity image
		final Point2D_F64 norm = new Point2D_F64();
		// 3D point in rectified stereo reference frame
		final Point3D_F64 rectP = new Point3D_F64();
		// 3D point in left stereo camera reference frame
		final Point3D_F64 leftP = new Point3D_F64();

		final CameraPinhole intrinsic = parameters.pinhole;
		final double baseline = parameters.baseline;
		final double disparityMin = parameters.disparityMin;

		for (int y = 0; y < disparity.height; y++) {
			int indexDisp = y*disparity.stride + disparity.startIndex;
			int indexMask = y*mask.stride + mask.startIndex;

			for (int x = 0; x < disparity.width; x++, indexDisp++, indexMask++) {
				// Check to see if it has been masked out as invalid
				if (mask.data[indexMask] != 0)
					continue;
				// Get the disparity and see if it has a valid value
				float d = disparity.data[indexDisp];
				if (d >= parameters.disparityRange)
					continue;

				// Get normalized image coordinates.
				dispPixel_to_rectNorm.compute(x, y, norm);

				// Find 3D point in rectified reference frame
				rectP.z = baseline*intrinsic.fx/(d + disparityMin);
				rectP.x = rectP.z*norm.x;
				rectP.y = rectP.z*norm.y;

				// Rectified left camera to native left camera
				GeometryMath_F64.multTran(parameters.rotateToRectified, rectP, leftP);

				// Left to world frame
				SePointOps_F64.transformReverse(world_to_view, leftP, cloud.grow());
			}
		}

		// Denote where this set of points end
		viewPointIdx.add(cloud.size());

		return this.viewPointIdx.size - 1;
	}
}
