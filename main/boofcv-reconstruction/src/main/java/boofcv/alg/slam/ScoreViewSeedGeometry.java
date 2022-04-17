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

package boofcv.alg.slam;

import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.struct.distort.Point2Transform3_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ddogleg.struct.DogArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores how good each view would be as a seed to initialize the coordinate system. View pairs it known extrinsics
 * are given preference over pairs which are unknown.
 */
public class ScoreViewSeedGeometry {

	CheckSynchronized checkSynchronized;

	MultiCameraSystem sensors;
	Map<String, String> viewToCamera = new HashMap<>();

	// Storage for pointing vector of each observation
	Map<String, ViewObservations> viewToInfo = new HashMap<>();
	DogArray<ViewObservations> listViewInfo = new DogArray<>(ViewObservations::new, ViewObservations::reset);

	// Storage for pixel observations
	DogArray<Point2D_F64> pixels = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);

	List<String> foundSimilar = new ArrayList<>();

	PairwiseImageGraph pairwise = new PairwiseImageGraph();

	public void process( LookUpSimilarImages similarImages ) {
		pairwise.reset();

		List<String> allViews = similarImages.getImageIDs();

		// Convert pixel observations to pointing vector for all views, plus initialize views in pairwise graph
		for (int i = 0; i < allViews.size(); i++) {
			pairwise.createNode(allViews.get(i));
			computePointingVectors(allViews.get(i), similarImages);
		}

		// Construct pairwise graph and score how informative relationships are between the views
		for (int i = 0; i < allViews.size(); i++) {
			scoreConnectedViews(allViews.get(i), similarImages);
		}

		// TODO score each view for being a seed based on its best neighbors
	}

	/**
	 * Loads pixels observations for a view then computing pointing vectors for each observation and saves the result
	 */
	protected void computePointingVectors( String viewId, LookUpSimilarImages similarImages ) {
		// Load pixel coordinates of observations
		similarImages.lookupPixelFeats(viewId, pixels);

		// Load conversion to pointing vector
		String cameraId = viewToCamera.get(viewId);
		Point2Transform3_F64 pixelToPointing = sensors.lookupCamera(cameraId).intrinsics.undistortPtoS_F64();

		// Transform points to pointing
		ViewObservations view = listViewInfo.grow();
		view.index = listViewInfo.size - 1;
		viewToInfo.put(viewId, view);

		for (int i = 0; i < pixels.size; i++) {
			Point2D_F64 pixel = pixels.get(i);
			pixelToPointing.compute(pixel.x, pixel.y, view.pointing.grow());
		}
	}

	protected void scoreConnectedViews( String viewTarget, LookUpSimilarImages similarImages ) {
		PairwiseImageGraph.View ptarget = pairwise.createNode(viewTarget);
		ViewObservations target = viewToInfo.get(viewTarget);

		// To avoid considering the same pair twice, filter out views with a higher index
		similarImages.findSimilar(viewTarget, ( v ) -> target.index > viewToInfo.get(v).index, foundSimilar);

		for (int i = 0; i < foundSimilar.size(); i++) {
			String similarID = foundSimilar.get(i);

			PairwiseImageGraph.View psimilar = pairwise.createNode(similarID);
			PairwiseImageGraph.Motion motion = pairwise.connect(ptarget, psimilar);

			// Compute the score differently depending on if the extrinsic relationship is known
			if (checkSynchronized.isSynchronized(viewTarget, similarID)) {
				evaluateKnownExtrinsics(motion);
			} else {
				evaluateUnknownRelationship(motion);
			}
		}
	}

	protected void evaluateKnownExtrinsics( PairwiseImageGraph.Motion motion ) {
		// TODO find common features
		// TODO triangulate, keep points in front of camera AND look at reprojection error
		// TODO keep inliers
		// TODO compute score based on delta if translation is set to zero
	}

	protected void evaluateUnknownRelationship( PairwiseImageGraph.Motion motion ) {
		// TODO find common features
		// TODO solve for pose and 3D points
		// TODO keep inliers
		// TODO compute score based on delta if translation is set to zero
	}

	public static class ViewObservations {
		int index;
		DogArray<Point3D_F64> pointing = new DogArray<>(Point3D_F64::new, Point3D_F64::zero);

		public void reset() {
			index = -1;
			pointing.reset();
		}
	}

	/**
	 * Checks to see if the two views were captured at the same moment in time.
	 */
	@FunctionalInterface
	public interface CheckSynchronized {
		boolean isSynchronized( String viewA, String viewB );
	}
}
