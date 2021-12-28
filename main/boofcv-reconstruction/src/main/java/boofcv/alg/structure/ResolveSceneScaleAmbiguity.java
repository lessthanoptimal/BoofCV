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

package boofcv.alg.structure;

import boofcv.abst.geo.TriangulateNViewsMetricH;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.distort.Point2Transform2_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * This determines how to convert the scale from one "scene" to another scene, given a common view and features.
 * The inputs are a set of observations and views from each scene. Observations in the common
 * view between the two scenes must be identical.
 * The approach involves determining the depth by triangulation of each image feature
 * given the two scenes. Then each feature is used to estimate a potential scale. The scales are put into a list
 * and the most common solution is returned.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public class ResolveSceneScaleAmbiguity implements VerbosePrint {
	/** A window of size fraction*(number of estimates) will be done when searching for the true scale */
	public double windowFraction = 0.1;
	/** Specifies which triangulation algorithm will be used */
	public TriangulateNViewsMetricH triangulator = FactoryMultiView.triangulateNViewMetricH(null);
	/** Used to identify points that are at or near infinity that should not be considered */
	public double infinityTol = 1e-12;

	// Number of image features that are observed in all the views
	int numFeatures;

	// Storage for information on each of the scenes
	final SceneInfo scene1 = new SceneInfo();
	final SceneInfo scene2 = new SceneInfo();

	//----------------- Internal Work Space
	Se3_F64 zero_to_world = new Se3_F64();
	Se3_F64 scene1_to_view = new Se3_F64();
	Se3_F64 view_to_scene2 = new Se3_F64();

	Point2D_F64 pixel = new Point2D_F64();

	// Storage for pixels in normalized image coordinates
	DogArray<Point2D_F64> workNormalized = new DogArray<>(Point2D_F64::new);

	// Storage for scale estimates from each feature
	DogArray_F64 scales = new DogArray_F64();

	@Nullable PrintStream verbose;

	/**
	 * Resets internal data structures and specifies number of features
	 */
	public void initialize( int numFeatures ) {
		this.numFeatures = numFeatures;
		this.scene1.reset();
		this.scene2.reset();
	}

	/**
	 * Specifies information from the first scene.
	 *
	 * @param features Provides access to all features in each view, except the first view
	 * @param listWorldToView Pose of each view in the world frame of the scene
	 * @param intrinsics Conversion from pixel to normalized image coordinate for each view
	 */
	public void setScene1( FeatureObservations features,
						   List<Se3_F64> listWorldToView,
						   List<Point2Transform2_F64> intrinsics ) {
		scene1.features = features;
		scene1.listWorldToView = listWorldToView;
		scene1.intrinsics = intrinsics;
	}

	/**
	 * Specifies information from the second scene.
	 *
	 * @param features Provides access to all features in each view, except the first view
	 * @param listWorldToView Pose of each view in the world frame of the scene
	 * @param intrinsics Conversion from pixel to normalized image coordinate for each view
	 */
	public void setScene2( FeatureObservations features,
						   List<Se3_F64> listWorldToView,
						   List<Point2Transform2_F64> intrinsics ) {
		scene2.features = features;
		scene2.listWorldToView = listWorldToView;
		scene2.intrinsics = intrinsics;
	}

	/**
	 * Process both scenes and determines how to convert the scale and SE3 between the two scenes
	 *
	 * @param scene1_to_scene2 (Output) Storage for transform from scene 1 to scene 2
	 * @return true if successful
	 */
	public boolean process( ScaleSe3_F64 scene1_to_scene2 ) {
		// Give it a horrible answer so that we will know something went very wrong if the return value was ignored
		scene1_to_scene2.scale = Double.NaN;

		BoofMiscOps.checkTrue(scene1.isInitialized(), "Must specify scene1");
		BoofMiscOps.checkTrue(scene2.isInitialized(), "Must specify scene2");
		if (numFeatures <= 0)
			return false;

		// sanity check that observations in view=0 are the same
		sanityCheckCommonObservations(0);
		sanityCheckCommonObservations(numFeatures/2);

		// Pre-allocate memory
		workNormalized.resetResize(numFeatures);

		scales.reset();
		scales.reserve(numFeatures);

		if (verbose != null) {
			printSceneInfo(scene1);
			printSceneInfo(scene2);
		}

		// Compute the local transforms
		computeLocalTransforms(requireNonNull(scene1.listWorldToView), scene1.listZeroToView);
		computeLocalTransforms(requireNonNull(scene2.listWorldToView), scene2.listZeroToView);

		// Keep track of why a scale estimate failed for debugging
		int failedTriangulate = 0;
		int failedInfinity = 0;
		int failedBehind1 = 0;
		int failedBehind2 = 0;

		for (int featureIdx = 0; featureIdx < numFeatures; featureIdx++) {
			// Triangulate the feature in both views
			if (!triangulate(scene1, featureIdx) || !triangulate(scene2, featureIdx)) {
				failedTriangulate++;
				continue;
			}

			// Ensure the norm is 1
			scene1.location.normalize();
			scene2.location.normalize();

			// If the point is at or near to infinity skip it since there will be more 3D information
			if (Math.abs(scene1.location.w) <= infinityTol || Math.abs(scene2.location.w) <= infinityTol) {
				failedInfinity++;
				continue;
			}

			// scale te locations so that the depths can be compared
			// depth is +z axis
			double z1 = scene1.location.z/scene1.location.w;
			double z2 = scene2.location.z/scene2.location.w;

			// See if it's behind the camera, which should be impossible
			if (z1 <= 0.0) {
				failedBehind1++;
				continue;
			} else if (z2 <= 0.0) {
				failedBehind2++;
				continue;
			}

			// Compute the scale factor using depth (z-axis)
			scales.add(z2/z1);
		}

		boolean success = selectBestScale(scene1_to_scene2);

		if (verbose != null) {
			verbose.println("scales.size=" + scales.size + "/" + numFeatures +
					" depth_check: tri=" + failedTriangulate + " inf=" + failedInfinity + " behind1=" + failedBehind1 +
					" behind2=" + failedBehind2 + ", success=" + success + " scale=" + scene1_to_scene2.scale);
		}

		return success;
	}

	void printSceneInfo( SceneInfo scene ) {
		Objects.requireNonNull(verbose);
		List<Se3_F64> listWorldToView = Objects.requireNonNull(scene.listWorldToView);
		verbose.print("Global Transforms: { ");
		for (int i = 0; i < listWorldToView.size(); i++) {
			Se3_F64 s = listWorldToView.get(i);
			verbose.printf("(%5.2f %5.2f %5.2f) ", s.T.x, s.T.y, s.T.z);
		}
		verbose.println("}");
	}

	/**
	 * After each feature contributes an estimate for the scale, select a value for the scale
	 */
	private boolean selectBestScale( ScaleSe3_F64 scene1_to_scene2 ) {
		// get the scale and handle pathological situations
		if (scales.size == 0) {
			if (verbose != null) verbose.println("failed to select scale since scale.size=0");
			// There are no valid estimates to use
			return false;
		} else if (scales.size == 1) {
			scene1_to_scene2.scale = scales.get(0);
		} else {
			scene1_to_scene2.scale = selectScaleMinimumLocalVariance();
		}

		// Use the scale factor to compute the transform from one scene to the other
		scene1_to_view.setTo(requireNonNull(scene1.listWorldToView).get(0));
		scene1_to_view.T.scale(scene1_to_scene2.scale);
		requireNonNull(scene2.listWorldToView).get(0).invert(view_to_scene2);
		scene1_to_view.concat(view_to_scene2, scene1_to_scene2.transform);

		return true;
	}

	/**
	 * The observation between the two scenes in the common view should be the same
	 */
	private void sanityCheckCommonObservations( int featureIdx ) {
		requireNonNull(scene1.features);
		requireNonNull(scene2.features);

		scene1.features.getPixel(0, featureIdx, pixel);
		double x = pixel.x;
		double y = pixel.y;
		scene2.features.getPixel(0, featureIdx, pixel);
		BoofMiscOps.checkTrue(x == pixel.x);
		BoofMiscOps.checkTrue(y == pixel.y);
	}

	/**
	 * Here, we assume that around the true scale there will be a tightly packed set of scale estimates. After sorting
	 * all the scales, we then look for a cluster of scale values that are similar by searching for a cluster
	 * with a small difference in value
	 */
	double selectScaleMinimumLocalVariance() {
		scales.sort();

		// 1/10 is arbitrary for the spread
		int window = Math.max(1, (int)(scales.size*windowFraction));
		window += window%2 == 0 ? 1 : 0; // make sure it's odd/symmetric

		double bestValue = scales.get(window/2);
		double bestRange = scales.get(window - 1) - scales.get(0);

		for (int i = window; i < scales.size; i++) {
			double range = scales.get(i) - scales.get(i - window + 1);
			if (range < bestRange) {
				bestRange = range;
				bestValue = scales.get(i - window/2);
			}
		}

		return bestValue;
	}

	/**
	 * Computes local transform from world transforms for a scene
	 */
	void computeLocalTransforms( List<Se3_F64> listWorldToView, DogArray<Se3_F64> listZeroToView ) {
		listWorldToView.get(0).invert(zero_to_world);
		listZeroToView.reset();
		// Set the first transform to be identity since view zero is the origin
		listZeroToView.grow().reset();
		for (int view = 1; view < listWorldToView.size(); view++) {
			zero_to_world.concat(listWorldToView.get(view), listZeroToView.grow());
		}

		// TODO rescale translation so that it's around 1.0. WIll need to save this scale and take it in account later on
		// Rescale translation so that it's around 1.0 to improve quality of triangulation results
//		double numericalScale = 0.0;
//		for (int view = 1; view < listWorldToView.size(); view++) {
//			numericalScale = Math.max(numericalScale, listZeroToView.get(view).T.norm());
//		}
//		for (int view = 1; view < listWorldToView.size(); view++) {
//			listZeroToView.get(view).T.divide(numericalScale);
//		}

		if (verbose != null) {
			verbose.print("Local Transforms: { ");
			for (int i = 0; i < listZeroToView.size(); i++) {
				Se3_F64 s = listZeroToView.get(i);
				verbose.printf("(%5.2f %5.2f %5.2f) ", s.T.x, s.T.y, s.T.z);
			}
			verbose.println("}");
		}
	}

	/**
	 * Triangulates the specified feature in the scene using all the values
	 */
	boolean triangulate( SceneInfo scene, int featureIdx ) {
		requireNonNull(scene.features);
		requireNonNull(scene.intrinsics);

		workNormalized.reset();
		for (int view = 0; view < scene.listZeroToView.size(); view++) {
			scene.features.getPixel(view, featureIdx, pixel);
			scene.intrinsics.get(view).compute(pixel.x, pixel.y, workNormalized.grow());
		}

		return triangulator.triangulate(workNormalized.toList(), scene.listZeroToView.toList(), scene.location);
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/**
	 * Used to lookup feature pixel observations in a scene.
	 */
	@FunctionalInterface
	public interface FeatureObservations {
		/**
		 * Retrieves a feature's observed pixel coordinate
		 *
		 * @param view Index of the view in the inliers list. 0=common view
		 * @param feature Which feature you wish to access
		 * @param pixel (Output) storage for the feature's observed pixel coordinate
		 */
		void getPixel( int view, int feature, Point2D_F64 pixel );
	}

	/**
	 * Storage for all the information about one scene
	 */
	static class SceneInfo {
		//-------------- Inputs
		// Used to access feature pixel coordinates
		@Nullable FeatureObservations features;
		// Location of each view in the scene. index=0 is the common view always
		@Nullable List<Se3_F64> listWorldToView;
		// pixel to normalized image coordinate for each view
		@Nullable List<Point2Transform2_F64> intrinsics;

		//--------------- Workspace
		// Storage for coordinate transforms from common to a view
		final DogArray<Se3_F64> listZeroToView = new DogArray<>(Se3_F64::new);
		// Storage for triangulated feature
		final Point4D_F64 location = new Point4D_F64();

		public boolean isInitialized() {
			return features != null && listWorldToView != null && intrinsics != null;
		}

		public void reset() {
			features = null;
			listWorldToView = null;
			intrinsics = null;
			listZeroToView.reset();
			location.setTo(0, 0, 0, 0);
		}
	}
}
