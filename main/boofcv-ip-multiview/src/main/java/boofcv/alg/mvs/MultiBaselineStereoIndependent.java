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

import boofcv.BoofVerbose;
import boofcv.abst.disparity.DisparitySmoother;
import boofcv.abst.disparity.StereoDisparity;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.misc.BoofLambdas;
import boofcv.misc.BoofMiscOps;
import boofcv.misc.LookUpImages;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.*;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <p>Solution for the Multi Baseline Stereo (MBS) problem which uses independently computed stereo
 * disparity images [1] with one common "center" image. Internally it calls another algorithm
 * to decide how to fuse the different options into a single disparity image. The output disparity image
 * is in the original image's pixel coordinates and not a rectified image.
 * </p>
 *
 * Steps:
 * <ol>
 *     <li>Input: The scene (view locations, camera parameters), image references, center image, and stereo pairs</li>
 *     <li>For each paired image:</li>
 *     <ol>
 *         <li>Rectify</li>
 *         <li>Compute the disparity</li>
 *         <li>Pass to MBS disparity algorithm</li>
 *     </ol>
 *     <li>Compute single disparity image for output</li>
 * </ol>
 *
 * <p>[1] There is no citation since this wasn't based on any specific paper but created out of a need to reuse
 * existing stereo code based on a high level description of MVS pipeline.</p>
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MultiBaselineStereoIndependent<Image extends ImageGray<Image>> implements VerbosePrint {

	/** Provides access to intermediate stereo results */
	private @Getter @Setter @Nullable Listener<Image> listener;

	/** Computes disparity between each image pair. Must be set. */
	@Getter @Setter @Nullable StereoDisparity<Image, GrayF32> stereoDisparity = null;

	/** Optional removal of speckle noise from disparity image. */
	@Getter @Setter @Nullable DisparitySmoother<Image, GrayF32> disparitySmoother = null;

	/** Used to retrieve images by their ID */
	@Getter @Setter @Nullable LookUpImages lookUpImages = null;

	//------------ Profiling information
	/** Sum of disparity calculations */
	@Getter double timeDisparity;
	/** Sum of disparity smoothing */
	@Getter double timeDisparitySmooth;
	/** Total time spent looking up images */
	@Getter double timeLookUpImages;
	/** Total time for all processing */
	@Getter double timeTotal;

	//------------ References to input objects
	private SceneStructureMetric scene; // Camera placement and parameters

	/** The computed disparity image in distorted pixel coordinates */
	final @Getter GrayF32 fusedDisparity = new GrayF32(1, 1);
	/** Disparity parameters for fused disparity image */
	final @Getter DisparityParameters fusedParam = new DisparityParameters();

	// Storage for stereo disparity results
	final StereoResults results = new StereoResults();
	// Fuses multiple disparity images together provided they have the same "left" view
	MultiBaselineDisparityMedian performFusion = new MultiBaselineDisparityMedian();

	// Storage for the original images in the stereo pair
	Image image1, image2;

	// Storage for rectified stereo images
	Image rectified1, rectified2;

	// Computes parameters how to rectify given the results from bundle adjustment
	BundleToRectificationStereoParameters computeRectification = new BundleToRectificationStereoParameters();

	// Specifies the relationships between reference frames
	final Se3_F64 left_to_world = new Se3_F64();
	final Se3_F64 left_to_right = new Se3_F64();
	final Se3_F64 world_to_left = new Se3_F64();
	final Se3_F64 world_to_right = new Se3_F64();
	final Se3_F64 tmpse3 = new Se3_F64();

	// Where verbose stdout is printed to
	@Nullable PrintStream verbose = null;
	@Nullable @Getter @Setter PrintStream verboseProfiling = null;

	public MultiBaselineStereoIndependent( LookUpImages lookUpImages, ImageType<Image> imageType ) {
		this(imageType);
		this.lookUpImages = lookUpImages;
	}

	public MultiBaselineStereoIndependent( ImageType<Image> imageType ) {
		this.rectified1 = imageType.createImage(1, 1);
		this.rectified2 = imageType.createImage(1, 1);
		this.image1 = imageType.createImage(1, 1);
		this.image2 = imageType.createImage(1, 1);
	}

	/**
	 * Computes the disparity between the target and each of the views it has been paired with then fuses all
	 * of these into a single disparity image in the target's original pixel coordinates.
	 *
	 * @param scene Contains extrinsic and intrinsics for each view
	 * @param targetIdx The "center" view which is common to all stereo pairs
	 * @param pairIdxs List of views (as indexes) which will act as the second image in the stereo pairs
	 * @param sbaIndexToViewID Look up table from view index to view ID
	 * @return true if successful or false if it failed
	 */
	public boolean process( SceneStructureMetric scene, int targetIdx, DogArray_I32 pairIdxs,
							BoofLambdas.IndexToString sbaIndexToViewID ) {
		// Reset profiling
		timeDisparity = 0;
		timeDisparitySmooth = 0;
		timeTotal = 0;
		timeLookUpImages = 0;

		long time0 = System.nanoTime();
		requireNonNull(stereoDisparity, "stereoDisparity must be configured");
		requireNonNull(lookUpImages, "lookUpImages must be configured");
		this.scene = scene;

		// Load the "center" image and initialize related data structures
		if (!lookUpImages.loadImage(sbaIndexToViewID.process(targetIdx), image1)) {
			if (verbose != null) verbose.println("Failed to load center image[" + targetIdx + "]");
			return false;
		}
		long time1 = System.nanoTime();
		timeLookUpImages += (time1 - time0)*1e-6;

		int targetCamera = scene.views.get(targetIdx).camera;
		computeRectification.setView1(scene.cameras.get(targetCamera).model, image1.width, image1.height);
		scene.getWorldToView(scene.views.get(targetIdx), world_to_left, tmpse3);
		world_to_left.invert(left_to_world);

		// Compute disparity for all the images it has been paired with and add them to the fusion algorithm
		performFusion.initialize(computeRectification.intrinsic1, computeRectification.view1_dist_to_undist);
		for (int i = 0; i < pairIdxs.size; i++) {
//			if (verbose != null) verbose.println("Computing stereo for view.idx=" + pairIdxs.get(i));

			if (!computeDisparity(image1, pairIdxs.get(i), sbaIndexToViewID.process(pairIdxs.get(i)), results)) {
				if (verbose != null) verbose.println("FAILED: disparity view.idx=" + pairIdxs.get(i));
				continue;
			}

			// allow access to the disparity before it's discarded
			if (listener != null) listener.handlePairDisparity(targetIdx, pairIdxs.get(i),
					rectified1, rectified2,
					results.disparity, results.mask, results.param, results.undist_to_rect1);
			performFusion.addDisparity(results.disparity, results.mask, results.param, results.undist_to_rect1);
		}

		if (verbose != null) verbose.println("Created fused stereo disparity image. inputs.size=" + pairIdxs.size);

		// Fuse all of these into a single disparity image
		if (!performFusion.process(fusedDisparity)) {
			if (verbose != null) verbose.println("FAILED: Can't fuse disparity images");
			return false;
		}

		fusedParam.disparityMin = performFusion.getFusedDisparityMin();
		fusedParam.disparityRange = performFusion.getFusedDisparityRange();
		fusedParam.pinhole.setTo(computeRectification.intrinsic1);
		fusedParam.baseline = performFusion.getFusedBaseline();
		CommonOps_DDRM.setIdentity(fusedParam.rotateToRectified);

		// Filter disparity
		filterDisparity(image1, fusedDisparity, fusedParam);

		timeTotal = (System.nanoTime() - time0)*1e-6;

		// Print out profiling information
		if (verboseProfiling != null) {
			verboseProfiling.printf(
					"Timing (ms), disp=%5.1f smooth=%5.1f lookup=%5.1f all=%5.1f, count=%d view='%s'\n",
					timeDisparity, timeDisparitySmooth, timeLookUpImages, timeTotal, pairIdxs.size,
					sbaIndexToViewID.process(targetIdx));
		}

		return true;
	}

	/**
	 * Computes the disparity between the common view "left" view and the specified "right"
	 *
	 * @param image1 Image for the left view
	 * @param rightIdx Which view to use for the right view
	 */
	private boolean computeDisparity( Image image1, int rightIdx, String rightID, StereoResults info ) {
		long time0 = System.nanoTime();
		// Look up the second image in the stereo image
		if (!Objects.requireNonNull(lookUpImages).loadImage(rightID, image2)) {
			if (verbose != null) verbose.println("Failed to load second image[" + rightIdx + "]");
			return false;
		}
		long time1 = System.nanoTime();
		timeLookUpImages += (time1 - time0)*1e-6;

		int rightCamera = scene.views.get(rightIdx).camera;

		// Compute the baseline between the two cameras
		scene.getWorldToView(scene.views.get(rightIdx), world_to_right, tmpse3);
		left_to_world.concat(world_to_right, left_to_right);

		// Compute rectification data
		computeRectification.processView2(scene.cameras.get(rightCamera).model,
				image2.getWidth(), image2.getHeight(), left_to_right);

		// Save the results
		info.param.rotateToRectified.setTo(computeRectification.rotate_orig_to_rect);
		info.undist_to_rect1.setTo(computeRectification.undist_to_rect1);

		// New calibration matrix,
		info.rectifiedK.setTo(computeRectification.rectifiedK);

		ImageDistort<Image, Image> distortLeft =
				RectifyDistortImageOps.rectifyImage(computeRectification.intrinsic1,
						computeRectification.undist_to_rect1_F32, BorderType.EXTENDED, image1.getImageType());
		ImageDistort<Image, Image> distortRight =
				RectifyDistortImageOps.rectifyImage(computeRectification.intrinsic2,
						computeRectification.undist_to_rect2_F32, BorderType.EXTENDED, image2.getImageType());

		ImageDimension rectifiedShape = computeRectification.rectifiedShape;
		info.mask.reshape(rectifiedShape.width, rectifiedShape.height);
		rectified1.reshape(rectifiedShape.width, rectifiedShape.height);
		rectified2.reshape(rectifiedShape.width, rectifiedShape.height);

		distortLeft.apply(image1, rectified1, info.mask);
		distortRight.apply(image2, rectified2);

		// Compute disparity from the rectified images
		requireNonNull(stereoDisparity).process(rectified1, rectified2);

		// Save the results
		info.disparity = stereoDisparity.getDisparity();

		DisparityParameters param = info.param;
		param.disparityMin = stereoDisparity.getDisparityMin();
		param.disparityRange = stereoDisparity.getDisparityRange();
		param.baseline = left_to_right.T.norm();
		PerspectiveOps.matrixToPinhole(info.rectifiedK, rectifiedShape.width, rectifiedShape.height, param.pinhole);

		long time2 = System.nanoTime();
		timeDisparity += (time2 - time1)*1e-6;

		// Filter disparity
		filterDisparity(rectified1, info.disparity, info.param);

		return true;
	}

	/**
	 * Remove speckle noise from the disparity image. Noise is often small disconnected regions. There will be
	 * false positives though.
	 */
	private void filterDisparity( Image left, GrayF32 disparity, DisparityParameters param ) {
		long time0 = System.nanoTime();
		if (disparitySmoother != null)
			disparitySmoother.process(left, disparity, param.disparityRange);
		timeDisparitySmooth += (System.nanoTime() - time0)*1e-6;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, performFusion);

		this.verboseProfiling = null;
		if (configuration != null && configuration.contains(BoofVerbose.RUNTIME)) {
			verboseProfiling = out;
		}
	}

	/**
	 * Results of disparity computation
	 */
	@SuppressWarnings({"NullAway.Init"})
	static class StereoResults {
		// disparity image
		GrayF32 disparity;
		// mask indicating which pixels are invalid because they are outside the FOV
		final GrayU8 mask = new GrayU8(1, 1);
		// Geometric description of the disparity
		final DisparityParameters param = new DisparityParameters();
		// Rectification matrix for view-1
		final DMatrixRMaj undist_to_rect1 = new DMatrixRMaj(3, 3);
		// Rectified camera's intrinsic parameters
		final DMatrixRMaj rectifiedK = new DMatrixRMaj(3, 3);
	}

	/**
	 * Used to gain access to intermediate results
	 */
	public interface Listener<RectImage> {
		/**
		 * Results of stereo processing between two views
		 *
		 * @param leftView View index in SBA
		 * @param rightView View index in SBA
		 * @param disparity Computed disparity image between these two views
		 * @param mask Disparity mask
		 * @param parameters Disparity parameters
		 * @param rect Disparity rectification matrix
		 */
		void handlePairDisparity( int leftView, int rightView,
								  RectImage rectLeft, RectImage rectRight,
								  GrayF32 disparity, GrayU8 mask,
								  DisparityParameters parameters, DMatrixRMaj rect );
	}
}
