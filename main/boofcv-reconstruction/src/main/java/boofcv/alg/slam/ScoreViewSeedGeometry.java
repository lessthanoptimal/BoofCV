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
import boofcv.struct.feature.AssociatedIndex;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scores how good each view would be as a seed to initialize the coordinate system. View pairs it known extrinsics
 * are given preference over pairs which are unknown.
 *
 * @author Peter Abeles
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

	// Which features in the two views are paired with each other
	DogArray<AssociatedIndex> pairs = new DogArray<>(AssociatedIndex::new);
	// Which of the pairs are inside the inlier set
	DogArray_I32 inliersIdx = new DogArray_I32();

	// Scores relationship between views when extrinsics is known
	EpipolarCalibratedScore3D scoreKnown;
	// Scores relationship between views when extrinsics is not known
	EpipolarCalibratedScore3D scoreNotKnown;

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

	protected void scoreConnectedViews( String viewA, LookUpSimilarImages similarImages ) {
		PairwiseImageGraph.View pa = pairwise.createNode(viewA);
		ViewObservations obsA = viewToInfo.get(viewA);

		// To avoid considering the same pair twice, filter out views with a higher index
		similarImages.findSimilar(viewA, ( v ) -> obsA.index > viewToInfo.get(v).index, foundSimilar);

		Se3_F64 workExtrinsics = new Se3_F64();

		for (int i = 0; i < foundSimilar.size(); i++) {
			String viewB = foundSimilar.get(i);
			ViewObservations obsB = viewToInfo.get(viewB);

			PairwiseImageGraph.View pb = pairwise.createNode(viewB);
			PairwiseImageGraph.Motion motion = pairwise.connect(pa, pb);

			// List of common observations
			similarImages.lookupAssociated(viewB, pairs);

			// Compute the score differently depending on if the extrinsic relationship is known
			EpipolarCalibratedScore3D scorer;
			@Nullable Se3_F64 b_to_a = null;
			if (checkSynchronized.isSynchronized(viewA, viewB)) {
				b_to_a = sensors.computeSrcToDst(viewB, viewA, workExtrinsics);
				scorer = scoreKnown;
			} else {
				scorer = scoreNotKnown;
			}

			// Compute then save the score
//			scorer.process(obsA.pointing.toList(), obsB.pointing.toList(), pairs.toList(), b_to_a, inliersIdx);
			motion.score3D = scoreKnown.getScore();
			motion.is3D = scorer.is3D();
		}
	}

	public static class ViewObservations {
		int index;
		// pixel observation converted to a pointing vector in 3D
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
