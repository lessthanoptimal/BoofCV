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

import boofcv.alg.geo.robust.RansacCalibrated2;
import boofcv.alg.structure.LookUpSimilarImages;
import boofcv.alg.structure.PairwiseImageGraph;
import boofcv.struct.ConfigLength;
import boofcv.struct.distort.Point2Transform3_F64;
import boofcv.struct.distort.Point3Transform2_F64;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.geo.AssociatedPair3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a {@link PairwiseImageGraph} that describes the relationship between each view. It takes full advantage
 * of prior information from the known {@link MultiCameraSystem}.
 *
 * @author Peter Abeles
 */
public class GeneratePairwiseGraphFromMultiCameraSystem {
	/** PairwiseImageGraph which contains information on the scene and amount of 3D information between views */
	@Getter PairwiseImageGraph pairwise = new PairwiseImageGraph();

	/** Config: Specifies max reproejction error for inlier. If relative, then width + height */
	@Getter public ConfigLength maxReprojectionError = ConfigLength.relative(0.006, 2);

	/** Config: Number of RANSAC iterations needed when estimating the baseline */
	@Getter @Setter public int numberOfIterations = 500;

	// Checks to see if two views where captured at the same time by the multi-camera system
	final CheckSynchronized checkSynchronized;

	// Describes the multi-camera system being used
	MultiCameraSystem sensors;
	Map<String, String> viewToCamera = new HashMap<>();

	// Storage for pointing vector of each observation
	Map<String, ViewObservations> viewToInfo = new HashMap<>();
	DogArray<ViewObservations> listViewInfo = new DogArray<>(ViewObservations::new, ViewObservations::reset);

	// Storage for pixel observations
	DogArray<Point2D_F64> pixels = new DogArray<>(Point2D_F64::new, Point2D_F64::zero);

	// Storage for views which are similar to the target view
	List<String> foundSimilar = new ArrayList<>();

	// Which features in the two views are paired with each other
	DogArray<AssociatedIndex> pairs = new DogArray<>(AssociatedIndex::new);
	FastArray<AssociatedIndex> pairsInliers = new FastArray<>(AssociatedIndex.class);

	// Which of the pairs are inside the inlier set
	DogArray_I32 inliersIdx = new DogArray_I32();

	// Scores relationship between views when extrinsics is known
	EpipolarCalibratedScore3D scorer;

	// Used to estimate the extrinsics when it's not already known
	final RansacCalibrated2<Se3_F64, AssociatedPair3D> robustExtrinsic;

	// Storage for associated views that {@link #robustExtrinsic} can understand
	final DogArray<AssociatedPair3D> associations = new DogArray<>(AssociatedPair3D::new);

	public GeneratePairwiseGraphFromMultiCameraSystem( MultiCameraSystem sensors,
													   RansacCalibrated2<Se3_F64, AssociatedPair3D> robustExtrinsic,
													   EpipolarCalibratedScore3D scorer,
													   CheckSynchronized checkSynchronized) {
		this.sensors = sensors;
		this.robustExtrinsic = robustExtrinsic;
		this.scorer = scorer;
		this.checkSynchronized = checkSynchronized;
	}

	public void process( MultiCameraSystem sensors, LookUpSimilarImages similarImages ) {
		this.sensors = sensors;
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
	}

	/**
	 * Loads pixels observations for a view then computing pointing vectors for each observation and saves the result
	 */
	protected void computePointingVectors( String viewId, LookUpSimilarImages similarImages) {
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

	/**
	 * Score connections between views to determine how much 3D information is contained between them using
	 * a heuristic. Care is taken to avoid processing the same connection twice. Results are stored in a
	 * {@link PairwiseImageGraph}.
	 *
	 * @param viewA The target view who's connections are going to be examined.
	 * @param similarImages Used to look which views have a relationship with the target view.
	 */
	protected void scoreConnectedViews( String viewA, LookUpSimilarImages similarImages) {
		PairwiseImageGraph.View pa = pairwise.createNode(viewA);
		ViewObservations obsA = viewToInfo.get(viewA);
		MultiCameraSystem.Camera cameraA = sensors.lookupCamera(viewToCamera.get(viewA));
		Point3Transform2_F64 pointingToPixelA = cameraA.intrinsics.distortStoP_F64();

		// To avoid considering the same pair twice, filter out views with a higher index
		similarImages.findSimilar(viewA, ( v ) -> obsA.index > viewToInfo.get(v).index, foundSimilar);

		// Storage for extrinsics / baseline
		Se3_F64 b_to_a = new Se3_F64();

		// Go through all connected views
		for (int i = 0; i < foundSimilar.size(); i++) {
			String viewB = foundSimilar.get(i);
			ViewObservations obsB = viewToInfo.get(viewB);
			MultiCameraSystem.Camera cameraB = sensors.lookupCamera(viewToCamera.get(viewB));
			Point3Transform2_F64 pointingToPixelB = cameraB.intrinsics.distortStoP_F64();

			// Which image features have been paired together
			similarImages.lookupAssociated(viewB, pairs);

			// If cameras are synchronized then the extrinsics is known
			if (checkSynchronized.isSynchronized(viewA, viewB)) {
				sensors.computeSrcToDst(viewB, viewA, b_to_a);
			} else {
				// Estimate the extrinsics robustly
				if (!estimateBaseline(viewA, viewB, cameraA, cameraB, pointingToPixelA, pointingToPixelB, b_to_a)) {
					continue;
				}
			}

			// Compute then save the score
			scorer.process(cameraA.shape, cameraB.shape, pointingToPixelA, pointingToPixelB,
					obsA.pointing.toList(), obsB.pointing.toList(), pairs.toList(), b_to_a, inliersIdx);

			// Update the graph
			PairwiseImageGraph.View pb = pairwise.createNode(viewB);
			PairwiseImageGraph.Motion motion = pairwise.connect(pa, pb);
			motion.score3D = scorer.getScore();
			motion.is3D = scorer.is3D();
//			motion.inliers TODO fill this in
		}
	}

	/** Robustly estimates the baseline between two views when the extrinsics is unknown */
	protected boolean estimateBaseline( String viewA, String viewB,
										MultiCameraSystem.Camera cameraA, MultiCameraSystem.Camera cameraB,
										Point3Transform2_F64 pointingToPixelA, Point3Transform2_F64 pointingToPixelB,
										Se3_F64 b_to_a) {
		// Compute RANSAC inlier tolerance relative to image size
		double errorTolA = maxReprojectionError.compute(cameraA.getSideLength());
		double errorTolB = maxReprojectionError.compute(cameraB.getSideLength());

		// Configure RANSAC
		robustExtrinsic.setMaxIterations(numberOfIterations);

		// NOTE: This uses a single tolerance for both images. Not ideal...
		// maybe get around this issue by computing error in fractional pixels?
		robustExtrinsic.setThresholdFit((errorTolA + errorTolB)/2);

		robustExtrinsic.setDistortion(0, pointingToPixelA);
		robustExtrinsic.setDistortion(1, pointingToPixelB);

		// Convert observations to a format that the robust estimator understands
		associations.resetResize(pairs.size());
		DogArray<Point3D_F64> obsA = viewToInfo.get(viewA).pointing;
		DogArray<Point3D_F64> obsB = viewToInfo.get(viewB).pointing;
		for (int i = 0; i < pairs.size(); i++) {
			AssociatedPair3D ap = associations.get(i);
			AssociatedIndex ai = pairs.get(i);
			ap.p1.setTo(obsA.get(ai.src));
			ap.p2.setTo(obsB.get(ai.dst));
		}

		// Estimate the extrinsics
		if (!robustExtrinsic.process(associations.toList())) {
			// Something went horribly wrong. Probably bad data. In that event we don't want to use this
			// pair of views
			return false;
		}

		// Create a list of inlier observations
		pairsInliers.reset();
		int N = robustExtrinsic.getMatchSet().size();
		for (int i = 0; i < N; i++) {
			pairsInliers.add(pairs.get(robustExtrinsic.getInputIndex(i)));
		}

		// Get the found extrinsics
		Se3_F64 a_to_b = robustExtrinsic.getModelParameters();
		a_to_b.invert(b_to_a);

		return true;
	}

	/**
	 * Precomputed information for a view's observations
	 */
	public static class ViewObservations {
		// Index of this view in the array
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
