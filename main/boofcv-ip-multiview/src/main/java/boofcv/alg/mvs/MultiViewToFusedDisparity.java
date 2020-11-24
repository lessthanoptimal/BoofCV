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

import boofcv.abst.disparity.StereoDisparity;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyDistortImageOps;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageDimension;
import boofcv.struct.image.ImageGray;
import georegression.struct.se.Se3_F64;
import gnu.trove.map.TIntObjectMap;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Given a set of distorted images with their known extrinsic and intrinsic parameters, compute a fused disparity
 * image. Each pair of images has it's own disparity image computed independently of all the others. This set
 * of disparity images are then fused together using {@link FuseDisparityImages}.
 *
 * Inside the code the stereo pairs often refer to a left and right camera. That is just a convention. The target
 * camera, which is common to all pairs, is referenced as the left camera and all the others as the right camera.
 * If the "right" camera is really to the left, above, below, ... etc, the rectified view will be flipped and/or
 * rotated to force the left/right convention during disparity computation.
 *
 * @author Peter Abeles
 */
public class MultiViewToFusedDisparity<Image extends ImageGray<Image>> implements VerbosePrint {

	/** Provides access to intermediate stereo results */
	private @Getter @Setter @Nullable Listener<Image> listener;

	/** Computes disparity between each image pair. Must be set. */
	@Getter @Setter @Nullable StereoDisparity<Image, GrayF32> stereoDisparity = null;

	//------------ References to input objects
	private SceneStructureMetric scene; // Camera placement and parameters
	private TIntObjectMap<Image> images; // Look up camera images

	/** The computed disparity image in distorted pixel coordinates */
	final @Getter GrayF32 fusedDisparity = new GrayF32(1, 1);
	/** Disparity parameters for fused disparity image */
	final @Getter DisparityParameters fusedParam = new DisparityParameters();

	// Storage for stereo disparity results
	final StereoResults results = new StereoResults();
	// Fuses multiple disparity images together provided they have the same "left" view
	final FuseDisparityImages performFusion = new FuseDisparityImages();

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

	/**
	 * Resets data structures and a describe of the scene and images to be processed
	 *
	 * @param scene Description of each view and their scene
	 * @param images List of images which are to be processed
	 */
	public void initialize( SceneStructureMetric scene, TIntObjectMap<Image> images ) {
		BoofMiscOps.checkTrue(!images.isEmpty());
		BoofMiscOps.checkTrue(scene.views.size >= 2);

		this.scene = scene;
		this.images = images;

		Image img = (Image)images.values()[0];
		rectified1 = img.createNew(1, 1);
		rectified2 = img.createNew(1, 1);
	}

	/**
	 * Computes the disparity between the target and each of the views it has been paired with then fuses all
	 * of these into a single disparity image in the target's original pixel coordinates.
	 *
	 * @param targetID Which view will act as the common ("left") camera
	 * @param pairs Which views will the disparity be computed against
	 * @return true if successful or false if it failed
	 */
	public boolean process( int targetID, GrowQueue_I32 pairs ) {
		requireNonNull(stereoDisparity, "setStereoDisparity must be configured");

		// Precompute what little can be for the target / left / image1
		Image image1 = images.get(targetID);
		int targetCamera = scene.views.get(targetID).camera;
		computeRectification.setView1(scene.cameras.get(targetCamera).model, image1.width, image1.height);
		scene.getWorldToView(scene.views.get(targetID), world_to_left, tmpse3);
		world_to_left.invert(left_to_world);

		// Compute disparity for all the images it has been paired with and add them to the fusion algorithm
		performFusion.initialize(computeRectification.intrinsic1, computeRectification.view1_dist_to_undist);
		for (int i = 0; i < pairs.size; i++) {
			if (verbose != null) verbose.println("Computing stereo for view " + pairs.get(i));
			computeDisparity(image1, pairs.get(i), results);
			// allow access to the disparity before it's discarded
			if (listener != null) listener.handlePairDisparity(targetID, pairs.get(i),
					rectified1, rectified2,
					results.disparity, results.mask, results.param, results.rect1);
			performFusion.addDisparity(results.disparity, results.mask, results.param, results.rect1);
		}

		if (verbose != null) verbose.println("Created fused stereo disparity image");

		// Fuse all of these into a single disparity image
		if (!performFusion.process(fusedDisparity)) {
			if (verbose != null) verbose.println("Failed to fuse together disparity images");
			return false;
		}

		fusedParam.disparityMin = performFusion.getFusedDisparityMin();
		fusedParam.disparityRange = performFusion.getFusedDisparityRange();
		fusedParam.pinhole.setTo(computeRectification.intrinsic1);
		fusedParam.baseline = performFusion.getFusedBaseline();
		CommonOps_DDRM.setIdentity(fusedParam.rectifiedR);

		return true;
	}

	/**
	 * Computes the disparity between the common view "left" view and the specified "right"
	 *
	 * @param image1 Image for the left view
	 * @param rightID Which view to use for the right view
	 */
	private void computeDisparity( Image image1, int rightID, StereoResults info ) {
		// Look up the right stereo image
		Image image2 = images.get(rightID);
		int rightCamera = scene.views.get(rightID).camera;

		// Compute the baseline between the two cameras
		scene.getWorldToView(scene.views.get(rightID), world_to_right, tmpse3);
		left_to_world.concat(world_to_right, left_to_right);

		// Compute rectification data
		computeRectification.processView2(scene.cameras.get(rightCamera).model,
				image2.getWidth(), image2.getHeight(), left_to_right);

		// Save the results
		info.param.rectifiedR.set(computeRectification.rectifiedRotation);
		info.rect1.set(computeRectification.rect1);

		// New calibration matrix,
		info.rectifiedK.set(computeRectification.rectifiedK);

		ImageDistort<Image, Image> distortLeft =
				RectifyDistortImageOps.rectifyImage(computeRectification.intrinsic1,
						computeRectification.rect1_F32, BorderType.EXTENDED, image1.getImageType());
		ImageDistort<Image, Image> distortRight =
				RectifyDistortImageOps.rectifyImage(computeRectification.intrinsic2,
						computeRectification.rect2_F32, BorderType.EXTENDED, image2.getImageType());

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
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = out;
	}

	/**
	 * Results of disparity computation
	 */
	static class StereoResults {
		// disparity image
		GrayF32 disparity;
		// mask indicating which pixels are invalid because they are outside the FOV
		final GrayU8 mask = new GrayU8(1, 1);
		// Geometric description of the disparity
		final DisparityParameters param = new DisparityParameters();
		// Rectification matrix for view-1
		final DMatrixRMaj rect1 = new DMatrixRMaj(3, 3);
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
