/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinhole;
import boofcv.struct.distort.PixelTransform;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import lombok.Getter;
import org.ddogleg.sorting.QuickSelect;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.ops.DConvertMatrixStruct;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Given a set of disparity images, all of which were computed from the same left image, fuse into a single
 * disparity image which should have better fill in and lower noise. The output disparity image will be in
 * the original image's pixel coordinate and will not be a rectified image. The disparity for each pixel
 * is selected using a median filter.
 *
 * The fused disparity image will always have a disparityMin of 0 and disparityRange of 100.
 * The baseline is computed dynamically to ensure that max value
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MultiBaselineDisparityMedian implements VerbosePrint {

	// Disparity parameters for fused view
	@Getter final CameraPinhole fusedIntrinsic = new CameraPinhole();
	@Getter final int fusedDisparityMin = 0; // fixed
	@Getter final int fusedDisparityRange = 100; // fixed. 100 seemed reasonable
	@Getter double fusedBaseline; // computed dynamically

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
	 * @param intrinsic Camera parameters for the fused disparity image
	 * @param pixelDist_to_Undist Transform from distorted to undistorted pixels
	 */
	public void initialize( CameraPinhole intrinsic, PixelTransform<Point2D_F64> pixelDist_to_Undist ) {
		this.fusedIntrinsic.setTo(intrinsic);
		this.images.reset();
		this.fused.resize(intrinsic.width, intrinsic.height);
		this.pixelOrig_to_Undist = pixelDist_to_Undist;
	}

	/**
	 * Adds a disparity image to the list
	 *
	 * @param disparity The disparity image. Does not need to be same shape as original.
	 * @param mask Indicates which pixels could have usable disparity information
	 * @param parameters Disparity parameters for this stereo pair.
	 * @param undist_to_rect_px Rectification matrix (3x3) from undistorted to rectified pixel coordinates
	 */
	public void addDisparity( GrayF32 disparity, GrayU8 mask, DisparityParameters parameters,
							  DMatrixRMaj undist_to_rect_px ) {
		InputSanityCheck.checkSameShape(disparity, mask);
		parameters.checkValidity();

		DisparityImage d = images.grow();
		d.disparity.setTo(disparity);
		d.mask.setTo(mask);
		d.undist_to_rect_px.setTo(undist_to_rect_px);
		d.parameters.setTo(parameters);
	}

	/**
	 * Processes all the disparity images and creates a composite disparity image
	 *
	 * NOTE: The rectifcation matrix and the rectification rotation matrix will be identity.
	 *
	 * @param disparity (Output) Disparity of the composite view. This will be in the original distorted pixels.
	 * @return true if successful or false if it failed
	 */
	public boolean process( GrayF32 disparity ) {
		checkTrue(!images.isEmpty(), "No images have been added");

		disparity.reshape(fused.width, fused.height);

		// select the initial value for the fused baseline using the inputs. Want to avoid being way too small.
		fusedBaseline = 0;
		for (int i = 0; i < images.size; i++) {
			fusedBaseline = Math.max(fusedBaseline, images.get(i).parameters.baseline);
		}

		// For each image, map valid pixels back into the original and add to that
		for (int i = 0; i < images.size; i++) {
			if (!addToFusedImage(images.get(i)))
				return false;
		}

		// Combine all the disparity information together robustly
		if (!computeFused(disparity)) {
			if (verbose != null)
				verbose.println("FAILED: Not a single disparity computed in any of the images. images.size=" + images.size);
			return false;
		}

		// Adjust stereo parameters to ensure the fixed range is good
		return computeDynamicParameters(disparity);
	}

	/**
	 * Adds valid disparity values inside of this into the fused image
	 *
	 * @return true if successful and false if it failed
	 */
	boolean addToFusedImage( DisparityImage image ) {
		final GrayF32 disparity = image.disparity;
		final GrayU8 mask = image.mask;
		final DisparityParameters imageParam = image.parameters;

		// Only do the int to float and double to float conversion once
		final float imageRange = imageParam.disparityRange;
		final float imageMin = imageParam.disparityMin;
		final double imageFocalX = imageParam.pinhole.fx;
		final double imageBaseline = imageParam.baseline;
		final CameraPinhole imagePinhole = imageParam.pinhole;

		DConvertMatrixStruct.convert(image.undist_to_rect_px, rect);

		// fused image undistorted pixel coordinates
		Point2D_F64 undistPix = new Point2D_F64();
		// rectified image coordinates
		Point2D_F64 rectPix = new Point2D_F64();

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

				// Make sure it's inside the disparity image before sampling
				if (rectPixX >= mask.width || rectPixY >= mask.height)
					continue;

				// If marked as invalid don't sample here
				if (mask.unsafe_get(rectPixX, rectPixY) == 0)
					continue;

				// Sample the disparity image and make sure it has a valid value
				float imageDisp = disparity.unsafe_get(rectPixX, rectPixY);
				if (imageDisp >= imageRange)
					continue;
				// Don't trust the disparity if it's at the upper or lower extremes. It's likely that the true value
				// was above or below but it wasn't allowed to match there
//				if (imageDisp < 1.0f || imageDisp > imageRange-1.0f)
//					continue;  TODO consider in the future once there are metrics

				float d = imageDisp + imageMin;
				if (d != 0) {
					// Convert the disparity from "image" into "fused image"
					// First compute the 3D point in the rectified coordinate system
					double rectZ = imageBaseline*imageFocalX/d;
					double rectX = rectZ*(rectPixX - imagePinhole.cx)/imagePinhole.fx;
					double rectY = rectZ*(rectPixY - imagePinhole.cy)/imagePinhole.fy;
					// Go from rectified to left camera, which is the fused camera
					double worldZ = dotRightCol(imageParam.rotateToRectified, rectX, rectY, rectZ);
					// Now that we know Z we can compute the disparity
					float fusedDisp = (float)(fusedBaseline*fusedIntrinsic.fx/worldZ);

					fused.get(origPixX, origPixY).add(fusedDisp);
				} else {
					// Points at infinity are a special case. They will remain at infinity
					fused.get(origPixX, origPixY).add(0.0f);
				}
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
	boolean computeFused( GrayF32 disparity ) {
		// If there's one pixel with a valid value this will pass
		boolean singleValidPixel = false;

		for (int y = 0; y < fused.height; y++) {
			int indexOut = disparity.startIndex + y*disparity.stride;
			for (int x = 0; x < fused.width; x++) {
				DogArray_F32 values = fused.get(x, y);
				float outputValue;
				if (values.size == 0) {
					// mark this pixel as invalid. The disparity will be rescaled later on and the max value at this
					// time isn't known
					outputValue = Float.MAX_VALUE;
				} else if (values.size == 1) {
					singleValidPixel = true;
					outputValue = values.data[0];
				} else if (values.size == 2) {
					singleValidPixel = true;
					outputValue = 0.5f*(values.data[0] + values.data[1]);
				} else {
					// median value
					outputValue = QuickSelect.select(values.data, values.size/2, values.size);
					singleValidPixel = true;
				}
				disparity.data[indexOut++] = outputValue;
			}
		}

		return singleValidPixel;
	}

	/**
	 * The baseline and disparityMin are dynamically computed to ensure a range of 100. After this adjustment the
	 * resulting point cloud should be unchanged.
	 *
	 * @return true if a disparity value greater than zero was found
	 */
	boolean computeDynamicParameters( GrayF32 disparity ) {
		// Find the min and max values for scaling the baseline
		float dispMax = 0;
		for (int y = 0; y < disparity.height; y++) {
			int index = disparity.startIndex + y*disparity.stride;
			for (int x = 0; x < disparity.width; x++) {
				float d = disparity.data[index++];
				if (d == Float.MAX_VALUE)
					continue;
				dispMax = Math.max(dispMax, d);
			}
		}

		if (dispMax <= 0.0) {
			if (verbose != null) verbose.println("FAILED: all valid points are at infinity");
			return false;
		}

		// -1 because range is the number of possible values. The max range is range-1.
		final float scale = (float)((this.fusedDisparityRange - 1)/Math.ceil(dispMax));
		this.fusedBaseline *= scale;

		// Update the disparity image to include these adjustments
		final float fRange = fusedDisparityRange;
		for (int y = 0; y < disparity.height; y++) {
			int index = disparity.startIndex + y*disparity.stride;
			for (int x = 0; x < disparity.width; x++, index++) {
				float d = disparity.data[index];
				if (d == Float.MAX_VALUE) {
					disparity.data[index] = fRange;
					continue;
				}
				disparity.data[index] = disparity.data[index]*scale;
				if (UtilEjml.isUncountable(disparity.data[index]))
					throw new RuntimeException("BUG");
			}
		}

		return true;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/** All the information for a disparity image */
	static class DisparityImage {
		/** The found disparity image */
		public final GrayF32 disparity = new GrayF32(1, 1);
		/** Mask which indicates pixels that have a source inside the original image and are valid */
		public final GrayU8 mask = new GrayU8(1, 1);
		/** Rectification matrix/homography. From undistorted pixel to rectified pixel */
		public final DMatrixRMaj undist_to_rect_px = new DMatrixRMaj(3, 3);
		/** Geometric meaning of disparity fo this view */
		public final DisparityParameters parameters = new DisparityParameters();

		public void reset() {
			disparity.reshape(1, 1);
			mask.reshape(1, 1);
			CommonOps_DDRM.fill(undist_to_rect_px, 0);
			parameters.reset();
		}
	}

	/**
	 * Contains disparity information mapped to original distorted pixels.
	 */
	static class FusedImage {
		public final DogArray<DogArray_F32> pixels = new DogArray<>(DogArray_F32::new, DogArray_F32::reset);
		public int width, height;

		public DogArray_F32 get( int x, int y ) {
			return pixels.get(y*width + x);
		}

		public void resize( int width, int height ) {
			pixels.reset();
			pixels.resize(width*height);
			this.width = width;
			this.height = height;
		}
	}
}
