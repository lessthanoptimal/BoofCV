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

package boofcv.alg.mvs.impl;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.misc.BoofLambdas;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_F64;

/**
 * Implementation of functions in {@link boofcv.alg.mvs.MultiViewStereoOps}.
 *
 * @author Peter Abeles
 */
public class ImplMultiViewStereoOps {
	public static void disparityToCloud( GrayF32 disparity,
										 DisparityParameters parameters,
										 BoofLambdas.PixXyzConsumer_F64 consumer ) {

		final CameraPinhole intrinsic = parameters.pinhole;
		final double baseline = parameters.baseline;
		final double disparityMin = parameters.disparityMin;
		final double[] R = parameters.rotateToRectified.data;
		// pixel in normalized image coordinates
		final Point2D_F64 norm = new Point2D_F64();

		for (int pixY = 0; pixY < disparity.height; pixY++) {
			int indexDisp = disparity.startIndex + pixY*disparity.stride;

			for (int pixX = 0; pixX < disparity.width; pixX++, indexDisp++) {
				float d = disparity.data[indexDisp];
				if (d >= parameters.disparityRange)
					continue;

				PerspectiveOps.convertPixelToNorm(intrinsic, pixX, pixY, norm);

				// Compute the point in rectified reference frame
				double Z = baseline*intrinsic.fx/(d + disparityMin);
				double X = Z*norm.x;
				double Y = Z*norm.y;

				// Rotate back into the camera reference frame
				// This is the transpose of the 3x3 rotation matrix.
				// It's in a row-major format (row,col) = R[row*3+col]
				double outX = R[0]*X + R[3]*Y + R[6]*Z;
				double outY = R[1]*X + R[4]*Y + R[7]*Z;
				double outZ = R[2]*X + R[5]*Y + R[8]*Z;

				consumer.process(pixX, pixY, outX, outY, outZ);
			}
		}
	}

	public static void disparityToCloud( GrayF32 disparity,
										 DisparityParameters parameters,
										 PixelTransform<Point2D_F64> pixelToNorm,
										 BoofLambdas.PixXyzConsumer_F64 consumer ) {

		final CameraPinhole intrinsic = parameters.pinhole;
		final double baseline = parameters.baseline;
		final double disparityMin = parameters.disparityMin;
		final double[] R = parameters.rotateToRectified.data;

		// pixel in normalized image coordinates
		final Point2D_F64 norm = new Point2D_F64();

		for (int pixY = 0; pixY < disparity.height; pixY++) {
			int indexDisp = disparity.startIndex + pixY*disparity.stride;

			for (int pixX = 0; pixX < disparity.width; pixX++, indexDisp++) {
				float d = disparity.data[indexDisp];
				if (d >= parameters.disparityRange)
					continue;

				// Converts the pixel into normalized image coordinate
				pixelToNorm.compute(pixX, pixY, norm);

				// Compute the point in rectified reference frame
				double Z = baseline*intrinsic.fx/(d + disparityMin);
				double X = Z*norm.x;
				double Y = Z*norm.y;

				// Rotate back into the camera reference frame
				// This is the transpose of the 3x3 rotation matrix.
				// It's in a row-major format (row,col) = R[row*3+col]
				double outX = R[0]*X + R[3]*Y + R[6]*Z;
				double outY = R[1]*X + R[4]*Y + R[7]*Z;
				double outZ = R[2]*X + R[5]*Y + R[8]*Z;

				consumer.process(pixX, pixY, outX, outY, outZ);
			}
		}
	}

	public static void disparityToCloud( GrayU8 disparity,
										 DisparityParameters parameters,
										 PixelTransform<Point2D_F64> pixelToNorm,
										 BoofLambdas.PixXyzConsumer_F64 consumer ) {

		final CameraPinhole intrinsic = parameters.pinhole;
		final double baseline = parameters.baseline;
		final double disparityMin = parameters.disparityMin;
		final double[] R = parameters.rotateToRectified.data;

		// pixel in normalized image coordinates
		final Point2D_F64 norm = new Point2D_F64();

		for (int pixY = 0; pixY < disparity.height; pixY++) {
			int indexDisp = disparity.startIndex + pixY*disparity.stride;

			for (int pixX = 0; pixX < disparity.width; pixX++, indexDisp++) {
				int d = disparity.data[indexDisp] & 0xFF;
				if (d >= parameters.disparityRange)
					continue;

				// Converts the pixel into normalized image coordinate
				pixelToNorm.compute(pixX, pixY, norm);

				// Compute the point in rectified reference frame
				double Z = baseline*intrinsic.fx/(d + disparityMin);
				double X = Z*norm.x;
				double Y = Z*norm.y;

				// Rotate back into the camera reference frame
				// This is the transpose of the 3x3 rotation matrix.
				// It's in a row-major format (row,col) = R[row*3+col]
				double outX = R[0]*X + R[3]*Y + R[6]*Z;
				double outY = R[1]*X + R[4]*Y + R[7]*Z;
				double outZ = R[2]*X + R[5]*Y + R[8]*Z;

				consumer.process(pixX, pixY, outX, outY, outZ);
			}
		}
	}

	public static void inverseToCloud( GrayF32 inverseDepthImage,
									   PixelTransform<Point2D_F64> pixelToNorm,
									   BoofLambdas.PixXyzConsumer_F64 consumer ) {

		// pixel in normalized image coordinates
		final Point2D_F64 norm = new Point2D_F64();

		for (int pixY = 0; pixY < inverseDepthImage.height; pixY++) {
			int indexDisp = inverseDepthImage.startIndex + pixY*inverseDepthImage.stride;

			for (int pixX = 0; pixX < inverseDepthImage.width; pixX++, indexDisp++) {
				float inv = inverseDepthImage.data[indexDisp];

				// Skip over invalid and infinite points
				if (inv <= 0.0f)
					continue;

				// Converts the pixel into normalized image coordinate
				pixelToNorm.compute(pixX, pixY, norm);

				// Compute the point in rectified reference frame
				double X = norm.x/inv;
				double Y = norm.y/inv;
				double Z = 1.0/inv;

				consumer.process(pixX, pixY, X, Y, Z);
			}
		}
	}

	public static float averageScore( GrayU8 disparity, int disparityRange, GrayF32 score ) {
		InputSanityCheck.checkSameShape(disparity, score);

		float sum = 0.0f;
		int count = 0;
		for (int y = 0; y < disparity.height; y++) {
			int indexDisp = disparity.startIndex + y*disparity.stride;
			int indexScor = score.startIndex + y*score.stride;

			int end = indexDisp + disparity.width;
			while (indexDisp < end) {
				int d = disparity.data[indexDisp++] & 0xFF;
				float s = score.data[indexScor++];
				if (d >= disparityRange)
					continue;
				sum += s;
				count++;
			}
		}

		return sum/count;
	}

	public static float averageScore( GrayF32 disparity, float disparityRange, GrayF32 score ) {
		InputSanityCheck.checkSameShape(disparity, score);

		float sum = 0.0f;
		int count = 0;
		for (int y = 0; y < disparity.height; y++) {
			int indexDisp = disparity.startIndex + y*disparity.stride;
			int indexScor = score.startIndex + y*score.stride;

			int end = indexDisp + disparity.width;
			while (indexDisp < end) {
				float d = disparity.data[indexDisp++];
				float s = score.data[indexScor++];
				if (d >= disparityRange)
					continue;
				sum += s;
				count++;
			}
		}

		return sum/count;
	}

	public static void invalidateUsingError( GrayU8 disparity, int disparityRange, GrayF32 score, float threshold ) {
		InputSanityCheck.checkSameShape(disparity, score);

		for (int y = 0; y < disparity.height; y++) {
			int indexDisp = disparity.startIndex + y*disparity.stride;
			int indexScor = score.startIndex + y*score.stride;

			int end = indexDisp + disparity.width;
			while (indexDisp < end) {
				float s = score.data[indexScor++];
				if (s > threshold) {
					disparity.data[indexDisp] = (byte)disparityRange;
				}
				indexDisp++;
			}
		}
	}

	public static void invalidateUsingError( GrayF32 disparity, float disparityRange, GrayF32 score, float threshold ) {
		InputSanityCheck.checkSameShape(disparity, score);

		for (int y = 0; y < disparity.height; y++) {
			int indexDisp = disparity.startIndex + y*disparity.stride;
			int indexScor = score.startIndex + y*score.stride;

			int end = indexDisp + disparity.width;
			while (indexDisp < end) {
				float s = score.data[indexScor++];
				if (s > threshold) {
					disparity.data[indexDisp] = disparityRange;
				}
				indexDisp++;
			}
		}
	}
}
