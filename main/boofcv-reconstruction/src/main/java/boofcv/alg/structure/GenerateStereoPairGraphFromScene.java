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

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.mvs.StereoPairGraph;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import gnu.trove.impl.Constants;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_F64;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Objects;
import java.util.Set;

/**
 * Given the {@link SceneStructureMetric sparse reconstruction}, create a {@link StereoPairGraph} that
 * describes which views are compatible for computing dense stereo disparity from.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class GenerateStereoPairGraphFromScene implements VerbosePrint {
	/** The computed graph describing views with a stereo relationship */
	final @Getter StereoPairGraph stereoGraph = new StereoPairGraph();

	/** Intended to stop small number of observations causing large swings in score. Larger means more smoothing. */
	public double countSmootherParam = 10.0;

	/** Minimum fraction for common features between the two frames */
	public double minimumCommonFeaturesFrac = 0.5;

	/** If the predicted disparity is above this value the score will not improve. */
	public double targetDisparity = 50.0;

	// Used to store which features are observed by which views for faster look up
	final DogArray<View> views = new DogArray<>(View::new);

	// Internal workspace. See function used for details
	final TIntIntMap pointMap = new TIntIntHashMap(500, Constants.DEFAULT_LOAD_FACTOR, -1, -1);
	final DogArray_F64 acuteAngles = new DogArray_F64();

	// If not null then print verbose debug info
	@Nullable PrintStream verbose;

	/**
	 * Computes a {@link StereoPairGraph} from the sparse scene graph
	 *
	 * @param viewIdx_to_imageId (Input) Look up table from view index in scene to the image ID/name
	 * @param scene (Input) Sparse 3D scene
	 */
	public void process( TIntObjectMap<String> viewIdx_to_imageId, SceneStructureMetric scene ) {
		stereoGraph.reset();

		// Determine which views are visible to each other
		matchPointsToViews(scene);

		estimateRadiansToPixels(scene);

		// Use the previously found connected views to compute the graph and score using geometric info
		createStereoGraph(viewIdx_to_imageId);
	}

	/**
	 * Using the center of the image, estimate the relationship between angle (radians) and image pixels. This is only
	 * an approximation and is typically worse the farther you are from the center. This is especially true in
	 * fisheye images. Also disparity is computed from rectified images which lack distortion. An accurate
	 * model would be more complex.
	 */
	private void estimateRadiansToPixels( SceneStructureMetric scene ) {
		double oneDegree = Math.sin(UtilAngle.degreeToRadian(5.0));
		Point2D_F64 pixelA = new Point2D_F64();
		Point2D_F64 pixelB = new Point2D_F64();

		for (int i = 0; i < scene.views.size; i++) {
			SceneStructureMetric.View view = scene.views.get(i);
			BundleAdjustmentCamera camera = Objects.requireNonNull(scene.getViewCamera(view).model);

			camera.project(0.0, 0.0, 1.0, pixelA);
			camera.project(oneDegree, 0.0, 1.0, pixelB);

			views.get(i).radianToPixels = pixelA.distance(pixelB)/UtilAngle.degreeToRadian(5.0);
		}
	}

	/**
	 * Creates a lookup table from view to points and find the vector going from view to point
	 */
	protected void matchPointsToViews( SceneStructureMetric scene ) {
		views.resize(scene.views.size);

		var world_to_view = new Se3_F64();
		var tmpSE = new Se3_F64();

		// Go through each point since that's where the information for frame visibility is stored
		for (int pointIdx = 0; pointIdx < scene.points.size; pointIdx++) {
			SceneStructureCommon.Point p = scene.points.get(pointIdx);

			// Iterate through each view this point has been seen in. A view can only see this point once
			for (int i = 0; i < p.views.size; i++) {
				int viewIdxA = p.views.get(i);
				View viewA = views.get(viewIdxA);

				// save the reference to the point which is visible in this view
				viewA.pointIndexes.add(pointIdx);

				// Compute the vector from the view's origin to point
				// use the world axis and not the local one
				scene.getWorldToView(scene.views.get(viewIdxA), world_to_view, tmpSE);

				computePointingVector(world_to_view, p, scene.isHomogenous(), viewA.pointing.grow());

				// Connect this view to the other views
				for (int j = i + 1; j < p.views.size; j++) {
					connect(viewIdxA, p.views.get(j));
				}
			}
		}
	}

	/**
	 * Computes the pointing vector between the view and point in world frame.
	 *
	 * @param pointing (Output) Cartesian vector pointing from the view to the point.
	 */
	protected void computePointingVector( Se3_F64 world_to_view, SceneStructureCommon.Point p,
										  boolean homogenous, Vector3D_F64 pointing ) {
		double x = p.coordinate[0];
		double y = p.coordinate[1];
		double z = p.coordinate[2];
		double w = homogenous ? p.coordinate[3] : 1.0;

		// pointing = [ I | T] h, where h is homogenous coordinates and T is view's location
		pointing.x = x + world_to_view.T.x*w;
		pointing.y = y + world_to_view.T.y*w;
		pointing.z = z + world_to_view.T.z*w;
		pointing.normalize();

		// There's a sign ambiguity in the homogenous case that needs to be resolved
		if (!homogenous)
			return;

		// It can be resolved by assuming a camera can't see backwards. Direction of camera can be determined
		// by looking at the orientation / rotation matrix. Dot product of pointing and +z should be the same

		// Normal vector pointing along +z axis
		DMatrixRMaj R = world_to_view.R;
		double n_x = R.unsafe_get(0, 2);
		double n_y = R.unsafe_get(1, 2);
		double n_z = R.unsafe_get(2, 2);

		if (pointing.dot(n_x, n_y, n_z) < 0.0) {
			pointing.scale(-1);
		}
	}

	/** Connects two views together and makes sure it isn't done twice */
	protected void connect( int viewIdxA, int viewIdxB ) {
		View viewA = views.get(viewIdxA);
		View viewB = views.get(viewIdxB);

		// See comment below about swapping a map in
		if (!viewA.connectedViews.contains(viewIdxB))
			viewA.connectedViews.add(viewIdxB);
		if (!viewB.connectedViews.contains(viewIdxA))
			viewB.connectedViews.add(viewIdxA);
	}

	/**
	 * Creates the output graph. A vertex is created for each view in the scene. The quality between connected
	 * views is found by looking at the angle between points and the number of common points
	 */
	protected void createStereoGraph( TIntObjectMap<String> viewToId ) {
		// Find the maximum number of features in any value
		int maxFeaturesInView = 0;
		// Create vertexes in output graph
		for (int viewIdxA = 0; viewIdxA < views.size; viewIdxA++) {
			stereoGraph.addVertex(viewToId.get(viewIdxA), viewIdxA);
			maxFeaturesInView = Math.max(views.get(viewIdxA).pointing.size, maxFeaturesInView);
		}

		// Add connections between vertexes and their quality
		for (int viewIdxA = 0; viewIdxA < views.size; viewIdxA++) {
			View viewA = views.get(viewIdxA);
			String nameA = viewToId.get(viewIdxA);

			for (int i = 0; i < viewA.connectedViews.size; i++) {
				int viewIdxB = viewA.connectedViews.get(i);

				// Avoid double counting this pair
				if (viewIdxB <= viewIdxA)
					continue;

				View viewB = views.get(viewIdxB);
				String nameB = viewToId.get(viewIdxB);

				// Find the angle difference between observations in each view
				// Good quality 3D pairs will have more large angle points
				findCommonFeatureAngles(viewA, viewB);

				// If only a small fraction overlap don't consider the match
				if (acuteAngles.size < Math.min(viewA.pointing.size, viewB.pointing.size)*minimumCommonFeaturesFrac)
					continue;

				acuteAngles.sort();
				// 50% gives a good idea of the average angle between
				// 95% stereo information is often driven by the larger disparities, which will be correlated with
				//     larger angles. This is weighted more below
				double angle50 = acuteAngles.getFraction(0.50);
				double angle95 = acuteAngles.getFraction(0.95);
				// Predict what the disparity would be. Use the camera of 50 and 95. Maybe this can be improved
				double radianToPixels = Math.min(viewA.radianToPixels, viewB.radianToPixels);
				double qualityDisparity = Math.min(1.0, radianToPixels*(angle50 + angle95)/(2.0*targetDisparity));

				// Prefer a larger fraction of common features between the two views, but smooth it to avoid
				// erratic results when the number is small
				double qualityCommon = (countSmootherParam + acuteAngles.size)/(countSmootherParam + maxFeaturesInView);

				// NOTE: One possible improvement is to take in account the area of overlap not just the number
				//       of common features between the two images

				double quality = qualityDisparity*qualityCommon;

				if (verbose != null)
					verbose.printf("Quality: %4s - %4s %.2f : disp=%.2f com=%.2f\n",
							nameA, nameB, quality, qualityDisparity, qualityCommon);

				if (quality != 0.0)
					stereoGraph.connect(nameA, nameB, quality);
			}
		}
	}

	/** Finds common features between the two views and computes the acute angles between them */
	protected void findCommonFeatureAngles( View viewA, View viewB ) {
		pointMap.clear();
		acuteAngles.reset();

		// create a mapping from point to observation index
		viewA.pointIndexes.forIdx(( idxA, pointIdx ) -> pointMap.put(pointIdx, idxA));

		// If both views observe the same point it's a pair and we compute the acute angle
		viewB.pointIndexes.forIdx(( idxB, pointIdx ) -> {
			int idxA = pointMap.get(pointIdx);
			if (idxA == -1)
				return;

			acuteAngles.add(viewA.pointing.get(idxA).acute(viewB.pointing.get(idxB)));
		});
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> set ) {
		this.verbose = out;
	}

	/** Points visible in the view */
	protected static class View {
		// which points are observed by this view
		final DogArray_I32 pointIndexes = new DogArray_I32();
		// Pointing vector from view location to point in world frame
		final DogArray<Vector3D_F64> pointing = new DogArray<>(Vector3D_F64::new);
		final DogArray_I32 connectedViews = new DogArray_I32();
		// NOTE: Replacing connectedViews with a set would make sense, but the number of common views
		//       is so small it's unlikely to make a difference and set implementations could internally
		//       create memory

		/** Crude approximation for converting radians to pixels at image center */
		double radianToPixels;

		public void reset() {
			pointIndexes.reset();
			pointing.reset();
			connectedViews.reset();
			radianToPixels = 0.0;
		}
	}
}
