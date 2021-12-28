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

package boofcv.alg.structure.spawn;

import boofcv.abst.geo.selfcalib.ProjectiveToMetricCameras;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.structure.*;
import boofcv.factory.geo.ConfigSelfCalibDualQuadratic;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.ElevateViewInfo;
import boofcv.struct.geo.AssociatedTupleDN;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkEq;

/**
 * Given a view and set of views connected to it, attempt to create a new metric scene. First a projective scene
 * is found. From this projective scene and metric one is created. Then bundle adjustment is used to refine the
 * metric scene. Features are then sanity checked to see if the pass basic physical constraints, see
 * {@link MetricSanityChecks}. If too many fail then the reconstruction is aborted. Otherwise, all the failing
 * features are removed and assumed to be outliers and bundle adjustment is run again. If there are no more
 * bad features it's considered to be a successful reconstruction.
 *
 * @author Peter Abeles
 */
@SuppressWarnings("NullAway.Init")
public class MetricSpawnSceneFromView implements VerbosePrint {

	/** Computes the initial scene from the seed and some of it's neighbors */
	private final @Getter ProjectiveInitializeAllCommon initProjective;

	private final @Getter RefineMetricWorkingGraph refineWorking;

	/** Used elevate the projective scene into a metric scene */
	private @Getter @Setter ProjectiveToMetricCameras projectiveToMetric =
			FactoryMultiView.projectiveToMetric((ConfigSelfCalibDualQuadratic)null);

	/** If less than this number of features fail the physical constraint test, attempt to recover by removing them */
	public double fractionBadFeaturesRecover = 0.05;

	// Common functions used in projective reconstruction
	protected PairwiseGraphUtils utils;

	/** The found metric scene. Only valid if {@link #process} returns true. */
	private @Getter final SceneWorkingGraph scene = new SceneWorkingGraph();

	@Nullable PrintStream verbose;

	//------------ Internal Work Space
	List<String> viewIds = new ArrayList<>();
	DogArray<ElevateViewInfo> viewInfos = new DogArray<>(ElevateViewInfo::new);
	DogArray<DMatrixRMaj> cameraMatrices = new DogArray<>(() -> new DMatrixRMaj(3, 4));
	DogArray<AssociatedTupleDN> observations = new DogArray<>(AssociatedTupleDN::new);
	MetricCameras elevationResults = new MetricCameras();

	List<CameraPinholeBrown> listPriorCameras = new ArrayList<>();
	MetricSanityChecks checks = new MetricSanityChecks();

	public MetricSpawnSceneFromView( RefineMetricWorkingGraph refineWorking, PairwiseGraphUtils utils ) {
		this.refineWorking = refineWorking;
		this.initProjective = new ProjectiveInitializeAllCommon();
		this.initProjective.utils = utils;
		this.utils = utils;
	}

	public MetricSpawnSceneFromView() {
		this(new RefineMetricWorkingGraph(), new PairwiseGraphUtils());
	}

	/**
	 * Computes the metric scene given the seed and related views
	 *
	 * @param dbSimilar Image data base to retieve feature and shape info
	 * @param pairwise Pairwise graph
	 * @param seed The view which will be the origin of the metric scene
	 * @param motions edges in seed that were used to generate the score
	 * @return true if successful or false if it failed
	 */
	public boolean process( LookUpSimilarImages dbSimilar,
							LookUpCameraInfo dbCams,
							PairwiseImageGraph pairwise,
							PairwiseImageGraph.View seed,
							DogArray_I32 motions ) {
		scene.reset();
		var commonPairwise = new DogArray_I32();

		// Find the common features
		utils.findAllConnectedSeed(seed, motions, commonPairwise);
		if (commonPairwise.size < 6) { // if less than the minimum it will fail
			if (verbose != null) verbose.println("FAILED: Too few common features. seed.id='" + seed.id + "'");
			return false;
		}

		// initialize projective scene using common tracks
		if (!initProjective.projectiveSceneN(dbSimilar, dbCams, seed, commonPairwise, motions)) {
			if (verbose != null) verbose.println("FAILED: Initialize projective scene");
			return false;
		}

		// Elevate initial seed to metric
		if (!projectiveSeedToMetric(pairwise)) {
			if (verbose != null) verbose.println("FAILED: Projective to metric. seed.id='" + seed.id + "'");
			return false;
		}

		return refineAndRemoveBadFeatures(dbSimilar, seed);
	}

	/**
	 * Performs non-linear refinement while attempting to remove outliers
	 */
	private boolean refineAndRemoveBadFeatures( LookUpSimilarImages dbSimilar, PairwiseImageGraph.View seed ) {
		// Sanity check results against physical constraints
		listPriorCameras.clear();
		for (int i = 0; i < scene.listViews.size(); i++) {
			SceneWorkingGraph.Camera camera = scene.getViewCamera(scene.listViews.get(i));
			listPriorCameras.add(camera.prior);
		}

		// Try two passes before giving up
		for (int loop = 0; loop < 2; loop++) {
			// Refine initial estimate
			if (!refineWorking.process(dbSimilar, scene)) {
				if (verbose != null) verbose.println("FAILED: Refine metric. seed.id='" + seed.id + "'");
				return false;
			}

			if (!checks.checkPhysicalConstraints(refineWorking.metricSba, listPriorCameras)) {
				if (verbose != null) verbose.println("FAILED: Unrecoverable physical constraint");
				return false;
			}

			// All views have identical inliers
			int numInliers = scene.listViews.get(0).inliers.get(0).getInlierCount();
			BoofMiscOps.checkTrue(numInliers == checks.badFeatures.size);

			int countBadFeatures = checks.badFeatures.count(true);
			if (countBadFeatures > fractionBadFeaturesRecover*checks.badFeatures.size) {
				if (verbose != null)
					verbose.println("FAILED: Too many bad features. bad=" + countBadFeatures + "/" + numInliers);
				return false;
			}

			// Everything looks good!
			if (countBadFeatures == 0)
				return true;

			if (loop != 0) {
				if (verbose != null)
					verbose.println("FAILED: Couldn't fix the upgrade. bad=" + countBadFeatures + "/" + numInliers);
				return false;
			}

			if (verbose != null)
				verbose.println("Removed bad features. Optimizing again. bad=" + countBadFeatures + "/" + numInliers);

			// Remove the bad features and try again
			for (int inlierIdx = checks.badFeatures.size - 1; inlierIdx >= 0; inlierIdx--) {
				if (!checks.badFeatures.get(inlierIdx))
					continue;
				// Order of the inliers doesn't matter, just needs to be consistent across all views
				for (int listIdx = 0; listIdx < scene.listViews.size(); listIdx++) {
					SceneWorkingGraph.InlierInfo info = scene.listViews.get(listIdx).inliers.get(0);
					for (int viewIdx = 0; viewIdx < info.observations.size; viewIdx++) {
						info.observations.get(viewIdx).removeSwap(inlierIdx);
					}
				}
			}

			// Settings lens distortion back to zero since outliers can drive it to extremes that are hard to recover from
			for (int cameraIdx = 0; cameraIdx < scene.listCameras.size(); cameraIdx++) {
				SceneWorkingGraph.Camera c = scene.listCameras.get(cameraIdx);
				c.intrinsic.k1 = 0.0;
				c.intrinsic.k2 = 0.0;
			}
			// NOTE: Setting extrinsic and intrinsic back to original state before SBA is worth investigating
		}

		throw new RuntimeException("BUG! Should have already returned");
	}

	/**
	 * Elevate the initial projective scene into a metric scene.
	 */
	private boolean projectiveSeedToMetric( PairwiseImageGraph pairwise ) {
		// Get results in a format that 'projectiveToMetric' understands
		initProjective.lookupInfoForMetricElevation(viewIds, viewInfos, cameraMatrices, observations);

		// Pass the projective scene and elevate into a metric scene
		if (!projectiveToMetric.process(viewInfos.toList(), cameraMatrices.toList(),
				(List)observations.toList(), elevationResults)) {
			if (verbose != null) verbose.println("_ views=" + BoofMiscOps.toStringLine(viewIds));
			return false;
		}

		saveMetricSeed(pairwise, viewIds, initProjective.getInlierIndexes(), elevationResults, scene);

		return true;
	}

	/**
	 * Saves the elevated metric results to the scene. Each view is given a copy of the inlier that has been
	 * adjusted so that it is view zero.
	 *
	 * @param viewInlierIndexes Which observations in each view are part of the inlier set
	 */
	void saveMetricSeed( PairwiseImageGraph graph, List<String> viewIds,
						 FastAccess<DogArray_I32> viewInlierIndexes,
						 MetricCameras results,
						 SceneWorkingGraph scene ) {
		checkEq(viewIds.size(), results.motion_1_to_k.size + 1, "Implicit view[0] no included");
		checkEq(viewIds.size(), viewInlierIndexes.size());

		// Save the number of views in the seed
		scene.numSeedViews = viewIds.size();

		// Save the metric views
		for (int i = 0; i < viewIds.size(); i++) {
			PairwiseImageGraph.View pview = graph.lookupNode(viewIds.get(i));

			// See if a camera needs to be created for this view or if one already exists
			int cameraDbIdx = utils.dbCams.viewToCamera(pview.id);
			SceneWorkingGraph.Camera camera = scene.cameras.get(cameraDbIdx);
			if (camera == null) {
				camera = scene.addCamera(cameraDbIdx);
				utils.dbCams.lookupCalibration(cameraDbIdx, camera.prior);
				BundleAdjustmentOps.convert(results.intrinsics.get(i), camera.intrinsic);
			}

			// Save the extrinsics for this view
			SceneWorkingGraph.View wview = scene.addView(pview, camera);

			if (i > 0)
				wview.world_to_view.setTo(results.motion_1_to_k.get(i - 1));
		}

		// Create the inlier set for each view, but adjust it so that the target view is view[0] in the set
		for (int constructIdx = 0; constructIdx < viewIds.size(); constructIdx++) {
			SceneWorkingGraph.View wtarget = scene.lookupView(viewIds.get(constructIdx));
			SceneWorkingGraph.InlierInfo inlier = wtarget.inliers.grow();
			inlier.views.resize(viewIds.size());
			inlier.observations.resetResize(viewIds.size());
			for (int offset = 0; offset < viewIds.size(); offset++) {
				int viewIdx = (constructIdx + offset)%viewIds.size();

				inlier.views.set(offset, graph.lookupNode(viewIds.get(viewIdx)));
				inlier.observations.get(offset).setTo(viewInlierIndexes.get(viewIdx));
				checkEq(inlier.observations.get(offset).size, inlier.observations.get(0).size,
						"Each view should have the same number of observations");
			}
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, initProjective, checks);

		if (projectiveToMetric instanceof VerbosePrint) {
			BoofMiscOps.verboseChildren(verbose, configuration, (VerbosePrint)projectiveToMetric);
		}
	}
}
