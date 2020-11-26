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
import boofcv.alg.distort.pinhole.PixelTransformPinholeNorm_F64;
import boofcv.alg.mvs.impl.ImplMultiViewStereoOps;
import boofcv.misc.BoofLambdas;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Useful functions when performing multi-view stereo.
 *
 * @author Peter Abeles
 */
public class MultiViewStereoOps {
	/**
	 * <p>Masks out point in a disparity image which appear to be too similar to what's already in a point cloud. This
	 * is done to avoid adding the same point twice</p>
	 *
	 * <p>NOTE: The reason a transform is required to go from norm to pixel for stereo, which is normally a simple
	 * equation, is that the disparity image might be a fused disparity image that includes lens distortion</p>
	 *
	 * @param cloud (Input) set of 3D points
	 * @param disparity (Input) Disparity image.
	 * @param parameters (Input) Parameters needed to understand geometric meaning of disparity values.
	 * @param cloud_to_stereo (Input) Transform from point cloud to rectified stereo coordinate systems.
	 * @param rectNorm_to_dispPixel (Input) Transform from undistorted normalized image coordinates in rectified
	 * reference frame into disparity pixels.
	 * @param tolerance (Input) How similar the projected point and observed disparity need to be for it to be
	 * considered the same.
	 * @param mask (Input,Output) On input it indicates which pixels are already masked out and new points are added to
	 * it for output.
	 */
	public static void maskOutPointsInCloud( final List<Point3D_F64> cloud,
											 final GrayF32 disparity,
											 final DisparityParameters parameters,
											 final Se3_F64 cloud_to_stereo,
											 final Point2Transform2_F64 rectNorm_to_dispPixel,
											 final double tolerance,
											 final GrayU8 mask ) {
		InputSanityCheck.checkSameShape(disparity, mask);
		parameters.checkValidity();

		// d = baseline*f/z
		final double baselineFocal = parameters.baseline*parameters.pinhole.fx;

		// 3D coordinate of point in original camera reference frame
		Point3D_F64 cameraPt = new Point3D_F64();
		// 3D coordinate of point in rectified reference frame
		Point3D_F64 rectPt = new Point3D_F64();
		// Pixel coordinate in disparity image
		Point2D_F64 pixel = new Point2D_F64();

		for (int cloudIdx = 0; cloudIdx < cloud.size(); cloudIdx++) {
			// find the point in the camera's reference frame
			Point3D_F64 cloudPt = cloud.get(cloudIdx);
			SePointOps_F64.transform(cloud_to_stereo, cloudPt, cameraPt);
			if (cameraPt.z <= 0.0)
				continue;

			// Convert it into rectified camera coordinates
			GeometryMath_F64.mult(parameters.rotateToRectified, cameraPt, rectPt);

			// Find the pixel it's projected onto
			rectNorm_to_dispPixel.compute(rectPt.x/rectPt.z, rectPt.y/rectPt.z, pixel);

			// Discretize the coordinate so that it can be looked up in the image
			// Rounding minimized the expected error and less sensitive to noise
			int px = (int)(pixel.x + 0.5); // Round. Kinda. -0.9 will result in 0. All positive numbers are correct.
			int py = (int)(pixel.y + 0.5); // The check below will fix this issue. Much faster than round()

			// Make sure it's inside the image
			if (pixel.x < -0.5 || pixel.y < -0.5 || px >= disparity.width || py >= disparity.height)
				continue;

			// Make sure this pixel isn't already invalidated
			if (mask.unsafe_get(px, py) != 0)
				continue;

			double imagD = disparity.unsafe_get(px, py);
			if (imagD >= parameters.disparityRange)
				continue;

			// Compute the disparity this would have
			double projD = baselineFocal/rectPt.z - parameters.disparityMin;
			if (projD < 0.0 || projD > parameters.disparityRange)
				continue;

			// See if the disparities are too similar and it should be masked out
			if (Math.abs(imagD - projD) > tolerance)
				continue;

			mask.unsafe_set(px, py, 1);
		}
	}

	/**
	 * Converts the disparity image into a point cloud. In this case we will assume that the disparity image
	 * does not include lens distortion, as is typical but not always true. Output cloud will be in camera frame
	 * and not rectified frame.
	 *
	 * @param disparity (Input) Disparity image
	 * @param mask (Input) Mask specifying valid pixels in disparity image
	 * @param parameters (Input) Parameters which describe the meaning of values in the disparity image
	 */
	public static void disparityToCloud( GrayF32 disparity, GrayU8 mask,
										 DisparityParameters parameters,
										 BoofLambdas.PixXyzConsumer_F64 consumer ) {
		ImplMultiViewStereoOps.disparityToCloud(disparity, mask, parameters, consumer);
	}

	/**
	 * Converts the disparity image into a point cloud. Output cloud will be in camera frame
	 * and not rectified frame.
	 *
	 * @param disparity (Input) Disparity image
	 * @param parameters (Input) Parameters which describe the meaning of values in the disparity image
	 * @param pixelToNorm (Input) Normally this can be set to null. If not null, then it specifies how to convert
	 * pixels into normalized image coordinates. Almost always a disparity image has no lens distortion,
	 * but in the unusual situation that it does (i.e. fused disparity) then you need to pass this in.
	 * @param consumer (Output) Passes along the rectified pixel coordinate and 3D (X,Y,Z) for each
	 * valid disparity point
	 */
	public static void disparityToCloud( ImageGray<?> disparity,
										 DisparityParameters parameters,
										 @Nullable PixelTransform<Point2D_F64> pixelToNorm,
										 BoofLambdas.PixXyzConsumer_F64 consumer ) {
		if (pixelToNorm == null)
			pixelToNorm = new PixelTransformPinholeNorm_F64().fset(parameters.pinhole);

		if (disparity instanceof GrayF32) {
			ImplMultiViewStereoOps.disparityToCloud((GrayF32)disparity, parameters, pixelToNorm, consumer);
		} else if (disparity instanceof GrayU8) {
			ImplMultiViewStereoOps.disparityToCloud((GrayU8)disparity, parameters, pixelToNorm, consumer);
		} else {
			throw new IllegalArgumentException("Unknown image type. " + disparity.getClass().getSimpleName());
		}
	}
}
