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

import boofcv.alg.geo.rectify.DisparityParameters;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.DConvertMatrixStruct;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Given a set of disparity images, all of which were computed from the same left image, fuse into a single
 * disparity image. Stereo disparity pixel error is used to select the best disparity value when there's
 * ambiguity.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MultiBaselineDisparityErrors implements VerbosePrint {
	/** Selected baseline to represent the fused stereo system */
	@Getter public double fusedBaseline = 0.0;

	// undistorted to distorted pixel coordinates
	PixelTransform<Point2D_F64> pixelOrig_to_Undist;

	// A copy of all the input disparity images
	final @Getter DogArray<DisparityImage> images = new DogArray<>(DisparityImage::new, DisparityImage::reset);

	// Where the combined results are stored
	final @Getter FusedImage fused = new FusedImage();

	// Storage for transform from rectified to unrectified pixels
	private final Homography2D_F64 rect = new Homography2D_F64();

	@Nullable PrintStream verbose;

	/**
	 * Must call before adding images. Specifies the size of the original image before rectification and clears
	 * previously saved results.
	 *
	 * @param width Width of common image
	 * @param height Height of common image
	 * @param dist_to_undist Transform from distorted to undistorted pixels
	 */
	public void initialize( int width, int height, PixelTransform<Point2D_F64> dist_to_undist ) {
		this.images.reset();
		this.fused.resize(width, height);
		this.pixelOrig_to_Undist = dist_to_undist;
	}

	/**
	 * Adds a disparity image to the list
	 *
	 * @param disparity The disparity image. Does not need to be same shape as original.
	 * @param score Fit score for disparity measurements
	 * @param parameters Disparity parameters for this stereo pair.
	 * @param undist_to_rect_px Rectification matrix (3x3) from undistorted to rectified pixel coordinates
	 */
	public void addDisparity( GrayF32 disparity, GrayF32 score, DisparityParameters parameters,
							  DMatrixRMaj undist_to_rect_px ) {
		parameters.checkValidity();

		DisparityImage d = images.grow();
		d.disparity.setTo(disparity);
		d.score.setTo(score);
		d.undist_to_rect_px.setTo(undist_to_rect_px);
		d.parameters.setTo(parameters);
	}

	/**
	 * <p>Processes all the disparity images and creates a composite inverse depth image image</p>
	 *
	 * NOTE: The rectifcation matrix and the rectification rotation matrix will be identity.
	 *
	 * @param inverseDepth (Output) Inverse depth image
	 * @return true if successful or false if it failed
	 */
	public boolean process( GrayF32 inverseDepth ) {
		checkTrue(!images.isEmpty(), "No images have been added");
		inverseDepth.reshape(fused.width, fused.height);

		if (verbose != null)
			verbose.printf("Fusing: shape=%dx%d images.size=%d", fused.width, fused.height, images.size);

		// Select the largest baseline to be representative
		fusedBaseline = 0;

		// For each image, map valid pixels back into the original and add to that
		for (int i = 0; i < images.size; i++) {
			if (!addToFusedImage(images.get(i)))
				return false;

			fusedBaseline = Math.max(fusedBaseline, images.get(i).parameters.getBaseline());
		}

		// Combine all the disparity information together robustly
		if (!computeFused(inverseDepth)) {
			if (verbose != null)
				verbose.println("FAILED: Not a single valid pixel in fused disparity. images.size=" + images.size);
			return false;
		}

		return true;
	}

	/**
	 * Adds valid disparity values inside of this into the fused image
	 *
	 * @return true if successful and false if it failed
	 */
	boolean addToFusedImage( DisparityImage image ) {
		final GrayF32 disparityImage = image.disparity;
		final GrayF32 scores = image.score;
		final DisparityParameters imageParam = image.parameters;

		// Only do the int to float and double to float conversion once
		final float imageRange = imageParam.disparityRange;
		final float imageMin = imageParam.disparityMin;
		final double imageFocalX = imageParam.pinhole.fx;
		final double imageBaseline = imageParam.baseline;
		final CameraPinhole imagePinhole = imageParam.pinhole;

		DConvertMatrixStruct.convert(image.undist_to_rect_px, rect);

		// fused image undistorted pixel coordinates
		var undistPix = new Point2D_F64();
		// rectified image coordinates
		var rectPix = new Point2D_F64();

		// To avoid sampling issues, go from fused image to disparity image
		for (int origPixY = 0; origPixY < fused.height; origPixY++) {
			for (int origPixX = 0; origPixX < fused.width; origPixX++) {
				// Go from distorted to undistorted pixels
				pixelOrig_to_Undist.compute(origPixX, origPixY, undistPix);
				// undistorted to rectified pixels
				HomographyPointOps_F64.transform(rect, undistPix.x, undistPix.y, rectPix);

				// Make sure it's inside the disparity image before sampling. Only checking lower bound because of the
				// tricked used below
				if (rectPix.x < 0.0 || rectPix.y < 0.0)
					continue;

				// Discretize the point so that a pixel can be read. Round instead of floor to reduce error.
				int rectPixX = (int)(rectPix.x + 0.5);
				int rectPixY = (int)(rectPix.y + 0.5);

				// Final part of bounds check
				if (rectPixX >= disparityImage.width || rectPixY >= disparityImage.height)
					continue;

				// Sample the disparity image and make sure it has a valid value
				float imageDisp = disparityImage.unsafe_get(rectPixX, rectPixY);
				if (imageDisp >= imageRange)
					continue;

				// Don't trust the disparity if it's at the upper or lower extremes. It's likely that the true value
				// was above or below but it wasn't allowed to match there
//				if (imageDisp < 1.0f || imageDisp > imageRange-1.0f)
//					continue;  TODO consider in the future once there are metrics

				// The disparity
				float d = imageDisp + imageMin;

				// Convert the disparity from "image" into "fused image"
				// First compute the 3D point in the rectified coordinate system
				// This is a homogenous coordinate where w = d. It can handle infinity
				double rectZ = imageBaseline*imageFocalX;
				double rectX = rectZ*(rectPixX - imagePinhole.cx)/imagePinhole.fx;
				double rectY = rectZ*(rectPixY - imagePinhole.cy)/imagePinhole.fy;

				// Go from rectified to left camera, which is the fused camera
				double worldZ = dotRightCol(imageParam.rotateToRectified, rectX, rectY, rectZ);

				// Save inverse depth
				fused.get(origPixX, origPixY).add((float)(d/worldZ));
				fused.getScore(origPixX, origPixY).add(scores.get(rectPixX, rectPixY));
			}
		}
		return true;
	}

	/**
	 * Computes the z component only of R'*[x;y;z]
	 */
	double dotRightCol( DMatrixRMaj R, double x, double y, double z ) {
		return R.data[2]*x + R.data[5]*y + R.data[8]*z;
	}

	/**
	 * Computes the fused output image. The median value is used if a pixel has more than 2 values. Otherwise the
	 * mean will be used.
	 *
	 * @return true if the disparity image is not entirely empty
	 */
	boolean computeFused( GrayF32 inverseDepthImage ) {
		// If there's one pixel with a valid value this will pass
		boolean atLeastOneValidPixel = false;

		for (int y = 0; y < fused.height; y++) {
			int indexOut = inverseDepthImage.startIndex + y*inverseDepthImage.stride;
			for (int x = 0; x < fused.width; x++) {
				DogArray_F32 values = fused.get(x, y);
				float inverseDepth;
				if (values.size == 0) {
					// No depth information here. Mark it as invalid
					inverseDepth = -1f;
				} else if (values.size == 1) {
					atLeastOneValidPixel = true;
					inverseDepth = values.data[0];
				} else {
					DogArray_F32 scores = fused.getScore(x, y);
					inverseDepth = 0.0f;
					float sumWeights = 0.0f;
					for (int i = 0; i < values.size; i++) {
						// assumed that score is an error. e.g. >= 0 and 0 = perfect
						float w = 1.0f/(1e-4f + scores.get(i));
						inverseDepth += w*values.get(i);
						sumWeights += w;
					}
					inverseDepth /= sumWeights;
					atLeastOneValidPixel = true;
				}

				inverseDepthImage.data[indexOut++] = inverseDepth;
			}
		}

		return atLeastOneValidPixel;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/** All the information for a disparity image */
	static class DisparityImage {
		/** The found disparity image */
		public final GrayF32 disparity = new GrayF32(1, 1);

		/** Fit score for each pixel in disparity image */
		public final GrayF32 score = new GrayF32(1, 1);

		/** Rectification matrix/homography. From undistorted pixel to rectified pixel */
		public final DMatrixRMaj undist_to_rect_px = new DMatrixRMaj(3, 3);

		/** Geometric meaning of disparity fo this view */
		public final DisparityParameters parameters = new DisparityParameters();

		public void reset() {
			disparity.reshape(1, 1);
			CommonOps_DDRM.fill(undist_to_rect_px, 0);
			parameters.reset();
		}
	}

	/**
	 * Contains depth information mapped to original distorted pixels.
	 */
	static class FusedImage {
		// inverse depth estimates at each pixel coordinate
		public final DogArray<DogArray_F32> pixels = new DogArray<>(DogArray_F32::new, DogArray_F32::reset);
		public final DogArray<DogArray_F32> scores = new DogArray<>(DogArray_F32::new, DogArray_F32::reset);
		public int width, height;

		public DogArray_F32 get( int x, int y ) {
			return pixels.get(y*width + x);
		}

		public DogArray_F32 getScore( int x, int y ) {
			return scores.get(y*width + x);
		}

		public void resize( int width, int height ) {
			pixels.reset().resize(width*height);
			scores.reset().resize(width*height);
			this.width = width;
			this.height = height;
		}
	}
}
