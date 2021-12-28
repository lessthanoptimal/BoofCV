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

import boofcv.alg.geo.bundle.cameras.BundlePinholeSimplified;
import boofcv.misc.BoofMiscOps;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Merges two scenes together after their metric elevation. The 'src' scene will be merged in to the 'dst' scene. The
 * dst scene is the dominant one and assumed to be "correct" when there's a conflict. A merge can fail if a physical
 * constraint test fails ofter merging. If a merge fails then there's no modification to 'src' or 'dst' scenes.
 *
 * <pre>
 * Procedure:
 * 1) Find common views between the two scenes
 * 2) Find a transform that relates the coordinate system in 'src' scene to the 'dst' scene.
 * 3) Create a working scene that's a copy of 'src' but with 'dst' coordinate system
 * 4) Copy inlier sets, view location, and camera intrinsics from 'dst' into common views
 * 5) Refine the working scene
 * 6) Check common views for violations of physical constraints. Accept merger, and fix minor issues, if none.
 * </pre>
 *
 * Only common views are checked for logical inconsistencies since we assume rest of the 'src' scene is consistent
 * and not being modified as heavily by refinement. Only common views have their state forcibly changed.
 *
 * When copying inliers from 'dst' into the work scene, views in 'dst' that are not in 'src' are added.
 * They will be considers to be known when refining the working scene. This will anchor the coordinate system.
 * However, all other views will be optimized. The refined values for common views are not copied int to 'dst'.
 * This is to reduce the influence of errors in 'src' becoming part of 'dst'.
 *
 * @author Peter Abeles
 */
public class MetricMergeScenes implements VerbosePrint {

	/** Used to verify the correctness of the merger */
	public @Getter final MetricSanityChecks checks = new MetricSanityChecks();

	/** Helpful functions */
	public @Getter final SceneMergingOperations mergingOps = new SceneMergingOperations();

	/** If less than this number of features fail the physical constraint test, attempt to recover by removing them */
	public double fractionBadFeaturesRecover = 0.05;

	/** Used to refine the scene */
	@Getter RefineMetricWorkingGraph refiner = new RefineMetricWorkingGraph();

	// Found views which are common between the two scenes
	DogArray<CommonView> commonViews = new DogArray<>(CommonView::new);
	// Found scale and SE3 relating the two views. Global initial estimate
	ScaleSe3_F64 src_to_dst = new ScaleSe3_F64();

	// The work place scene where the results are temporarily stored. This way the 'src' and 'dst' are not
	// modified until we are sure the merge worked
	SceneWorkingGraph workScene = new SceneWorkingGraph();

	// Look up table indicating which views in 'scene' are not to be modified when refining
	DogArray_B knownViews = new DogArray_B();

	// Look up table indicating which cameras in 'scene' are not to be modified when refining
	DogArray_B knownCameras = new DogArray_B();

	// Local workspace
	Se3_F64 src_to_view = new Se3_F64();
	Se3_F64 transform_dst_to_src = new Se3_F64();

	@Nullable PrintStream verbose;

	public MetricMergeScenes() {
		// Don't spam stdout
		refiner.verboseViewInfo = false;
	}

	/**
	 * Merges the 'src' scene into 'dst'. Both scenes are only modified if true is returned.
	 *
	 * @param dbSimilar Contains image related information
	 * @param src The scene being merged into 'dst'
	 * @param dst Where the combined scene will be stored. This is assumed to be the 'more correct' scene
	 * @return true if src was merged into dst or false if it failed and nothing has been modified
	 */
	public boolean merge( LookUpSimilarImages dbSimilar, SceneWorkingGraph src, SceneWorkingGraph dst ) {
		// Find the common views
		findCommonViews(src, dst, commonViews, verbose);
		if (commonViews.isEmpty())
			return false;

		// Sort in order of "best" last
		Collections.sort(commonViews.toList(), ( a, b ) -> Double.compare(a.score, b.score));

		// Compute the coordinate transform between the scenes using the "best" pair of views
		CommonView best = commonViews.getTail();
		if (!mergingOps.computeSceneTransform(dbSimilar, src, dst, best.src, best.dst, src_to_dst))
			return false;
		src_to_dst.transform.invert(transform_dst_to_src);

		// Creates a working scene from 'src' but adding values from 'dst' as needed
		createWorkScene(src, dst);

		// Refine the working scene. This will help mend the differences between the two scenes
		if (!refiner.process(dbSimilar, workScene, utils -> {
			for (int i = 0; i < knownCameras.size; i++) {
				utils.structure.cameras.get(i).known = knownCameras.get(i);
			}
			for (int i = 0; i < knownViews.size; i++) {
				utils.structure.motions.get(i).known = knownViews.get(i);
			}
		})) {
			if (verbose != null) verbose.println("FAiLED: Refiner return false");
			return false;
		}

		// Examine all the common views to see if the physical constraints in 'src' inlier sets are still valid
		// If they are, fix minor errors, then the merge is considered to be successful
		if (!verifyAndFixConstraints(dbSimilar))
			return false;

		// Print the state of views which are not common so you can see how much they have changed.
		if (verbose != null) {
			for (int i = 0; i < workScene.listViews.size(); i++) {
				SceneWorkingGraph.View wview = workScene.listViews.get(i);
				if (dst.views.get(wview.pview.id) != null)
					continue;

				BundlePinholeSimplified intrinsic = workScene.getViewCamera(wview).intrinsic;
				verbose.printf("After id='%s' src={ f=%.1f k1=%.1e k2=%.1e }\n",
						wview.pview.id, intrinsic.f, intrinsic.k1, intrinsic.k2);
			}
		}

		// Copy the results to 'dst'.
		mergeWorkIntoDst(dst);

		return true;
	}

	/**
	 * Check all inlier sets to make sure that they are still consistent with the physical constraints. This will
	 * catch any bad errors. If a few observations are not consistent they will be removed from the inlier set
	 *
	 * @return true if everything looks good and the merge can be accepted.
	 */
	private boolean verifyAndFixConstraints( LookUpSimilarImages db ) {
		for (int i = 0; i < commonViews.size; i++) {
			SceneWorkingGraph.View viewSrc = commonViews.get(i).src;
			SceneWorkingGraph.View wview = workScene.lookupView(viewSrc.pview.id);

			// make sure it doesn't inspect inliers related to dst since those can't be modified
			int numInliersSrc = viewSrc.inliers.size;

			for (int inlierIdx = 0; inlierIdx < numInliersSrc; inlierIdx++) {
				if (verbose != null && checks.verbose == null)
					verbose.printf("Constraints: view.id='%s' inlierIdx=%d\n", wview.pview.id, inlierIdx);
				if (!checks.checkPhysicalConstraints(db, workScene, wview, inlierIdx)) {
					if (verbose != null) verbose.println("FAILED: constraints had a fatal error.");
					return false;
				}

				if (!removeBadFeatures(wview.inliers.get(inlierIdx)))
					return false;
			}
		}
		return true;
	}

	/**
	 * Removes features flagged by the sanity checks from the inlier set
	 *
	 * @param info The inlier set
	 * @return true No fatal error was detected or false if a fatal error wsa detected
	 */
	boolean removeBadFeatures( SceneWorkingGraph.InlierInfo info ) {
		int numInliers = info.getInlierCount();

		BoofMiscOps.checkEq(numInliers, checks.badFeatures.size);
		int countBadFeatures = checks.badFeatures.count(true);

		if (countBadFeatures > fractionBadFeaturesRecover*checks.badFeatures.size) {
			if (verbose != null)
				verbose.println("FAILED: Inlier set had too many bad features. bad=" + countBadFeatures + "/" + numInliers);
			return false;
		}

		// If there are no issues move on to the next view
		if (countBadFeatures == 0)
			return true;

		if (verbose != null)
			verbose.println("Removing bad features to try to fix. bad=" + countBadFeatures + "/" + numInliers);

		for (int obsIdx = numInliers - 1; obsIdx >= 0; obsIdx--) {
			// get the feature ID that the observation references and see if that feature was marked as bad
			if (!checks.badFeatures.get(obsIdx))
				continue;
			for (int viewIdx = 0; viewIdx < info.observations.size; viewIdx++) {
				info.observations.get(viewIdx).removeSwap(obsIdx);
			}
		}

		return true;
	}

	/**
	 * Creates a local work scene by making a copy of 'src' and copying over parts of 'dst' as needed
	 */
	void createWorkScene( SceneWorkingGraph src, SceneWorkingGraph dst ) {
		BoofMiscOps.checkTrue(!commonViews.isEmpty());

		// Local copy of the 'src' scene
		workScene.setTo(src);
		// All views and cameras that are in 'src' will be modified
		knownViews.resetResize(workScene.listViews.size(), false);
		knownCameras.resetResize(workScene.listCameras.size(), false);

		// Copy cameras from 'dst' into the workScene
		copyDstCamerasIntoWork(dst);

		// Change the scene's coordinate system and scale factor to match 'dst'
		for (int i = 0; i < workScene.listViews.size(); i++) {
			convertNewViewCoordinateSystem(workScene.listViews.get(i));
		}

		// Go through the common views and copy the state from 'dst' into it. This includes the inlier
		// sets from 'dst'. As needed, views in 'dst' will be added to the scene buy they will be "known"
		for (int i = 0; i < commonViews.size; i++) {
			SceneWorkingGraph.View viewDst = commonViews.get(i).dst;
			SceneWorkingGraph.View wview = workScene.lookupView(viewDst.pview.id);

			// Force the common views to match 'dst'
			wview.world_to_view.setTo(viewDst.world_to_view);

			// Add inliers from 'dst'
			DogArray<SceneWorkingGraph.InlierInfo> inliersDst = commonViews.get(i).dst.inliers;

			// Copy over the inlier information so that features can be triangulated
			for (int infoIdx = 0; infoIdx < inliersDst.size; infoIdx++) {
				SceneWorkingGraph.InlierInfo orig = inliersDst.get(infoIdx);
				SceneWorkingGraph.InlierInfo copy = wview.inliers.grow();
				copy.setTo(orig);

				addViewsButNoInliers(dst, orig.views, true);
			}
		}
	}

	/**
	 * If 'dst' has the same camera use its state instead since 'dst' is the dominant scene
	 */
	void copyDstCamerasIntoWork( SceneWorkingGraph dst ) {
		for (int srcCameraIdx = 0; srcCameraIdx < workScene.listCameras.size; srcCameraIdx++) {
			SceneWorkingGraph.Camera wrkCamera = workScene.listCameras.get(srcCameraIdx);
			SceneWorkingGraph.Camera dstCamera = dst.cameras.get(wrkCamera.indexDB);

			if (dstCamera == null)
				continue;

			// If there is a dst camera use that state of that one
			wrkCamera.intrinsic.setTo(dstCamera.intrinsic);
			wrkCamera.prior.setTo(dstCamera.prior);

			// Mark cameras that are in 'dst' as known so that their state doesn't get messed up
			knownCameras.set(srcCameraIdx, true);
		}
	}

	/**
	 * Copies views from 'workScene' scene into 'dst' that are not in 'dst'. If a view is already in 'dst' then
	 * only the inlier set in 'workScene' is copied. 'dst' is considered to be the dominant scene which is why we
	 * don't want to modify the state of common views.
	 *
	 * Please look at how 'workScene' is constructed to understand the inlier logic below
	 */
	void mergeWorkIntoDst( SceneWorkingGraph dst ) {
		for (int viewIdx = 0; viewIdx < workScene.listViews.size(); viewIdx++) {
			SceneWorkingGraph.View viewSrc = workScene.listViews.get(viewIdx);
			SceneWorkingGraph.View viewDst = dst.views.get(viewSrc.pview.id);

			// If the camera does not exist in 'dst' already add a copy of the 'src' camera in to it
			SceneWorkingGraph.Camera cameraSrc = workScene.getViewCamera(viewSrc);
			SceneWorkingGraph.Camera cameraDst = dst.cameras.get(cameraSrc.indexDB);
			if (cameraDst == null) {
				cameraDst = dst.addCameraCopy(cameraSrc);
			}

			if (viewDst != null) {
				// just need to copy the inliers over from src. 'dst' inliers are at the end
				int end = viewSrc.inliers.size - viewDst.inliers.size;
				for (int inlierIdx = 0; inlierIdx < end; inlierIdx++) {
					viewDst.inliers.grow().setTo(viewSrc.inliers.get(inlierIdx));
				}
			} else {
				viewDst = dst.addView(viewSrc.pview, cameraDst);
				viewDst.setTo(viewSrc);
				viewDst.index = dst.listViews.size() - 1; // setTo() overwrote the index
			}

			BoofMiscOps.checkTrue(!viewDst.inliers.isEmpty());
		}
	}

	/**
	 * Converts the coordinate system in 'src' into one that is compatible with 'dst'
	 */
	void convertNewViewCoordinateSystem( SceneWorkingGraph.View wview ) {
		src_to_view.setTo(wview.world_to_view);
		src_to_view.T.scale(src_to_dst.scale);
		transform_dst_to_src.concat(src_to_view, wview.world_to_view);
	}

	private void addViewsButNoInliers( SceneWorkingGraph origScene,
									   FastArray<PairwiseImageGraph.View> viewsToCopy,
									   boolean markAsKnown ) {
		for (int viewIdx = 0; viewIdx < viewsToCopy.size; viewIdx++) {
			PairwiseImageGraph.View pview = viewsToCopy.get(viewIdx);
			if (workScene.views.containsKey(pview.id))
				continue;

			// Make sure all the views which are referenced are added to the local sub-scene
			// They will be static so we don't need to add their inlier info too
			copyIntoSceneJustState(origScene, markAsKnown, pview);
			if (verbose != null) verbose.println("Adding view-no-inliers id='" + pview.id + "' known=" + markAsKnown);
		}
	}

	/**
	 * Copies the view's state from 'origScene' into the 'workScene' and marks the scene as known or not.
	 *
	 * @param origScene The scene which provides the view's state
	 * @param markAsKnown If the copied view will be marked as known or not
	 * @param pview Which view is to be copied
	 */
	private void copyIntoSceneJustState( SceneWorkingGraph origScene, boolean markAsKnown, PairwiseImageGraph.View pview ) {
		SceneWorkingGraph.View origView = Objects.requireNonNull(origScene.views.get(pview.id));

		SceneWorkingGraph.Camera origCamera = origScene.getViewCamera(origView);
		SceneWorkingGraph.Camera copyCamera = workScene.cameras.get(origCamera.indexDB);
		if (copyCamera == null) {
			copyCamera = workScene.addCameraCopy(origCamera);
			knownCameras.add(markAsKnown);
		}

		SceneWorkingGraph.View copyView = workScene.addView(pview, copyCamera);
		copyView.world_to_view.setTo(origView.world_to_view);
		knownViews.add(markAsKnown);
	}

	/**
	 * Finds all views which are common between the two scenes
	 */
	static void findCommonViews( SceneWorkingGraph src, SceneWorkingGraph dst,
								 DogArray<CommonView> commonViews,
								 @Nullable PrintStream verbose ) {
		commonViews.reset();

		// Go through all the views in the src list
		for (int srcViewIdx = 0; srcViewIdx < src.listViews.size(); srcViewIdx++) {
			SceneWorkingGraph.View srcView = src.listViews.get(srcViewIdx);
			SceneWorkingGraph.View dstView = dst.views.get(srcView.pview.id);

			if (dstView == null) {
				if (verbose != null) {
					BundlePinholeSimplified intrinsic = src.getViewCamera(srcView).intrinsic;
					verbose.printf("id='%s' src={ f=%.1f k1=%.1e k2=%.1e }\n",
							srcView.pview.id, intrinsic.f, intrinsic.k1, intrinsic.k2);
				}
				continue;
			}

			// use the worst score of the pair as being pessimistic seems to work best in reconstruction
			double score = Math.min(srcView.getBestInlierScore(), dstView.getBestInlierScore());
			commonViews.grow().setTo(srcView, dstView, score);

			if (verbose != null) {
				BundlePinholeSimplified srcIntrinsic = src.getViewCamera(srcView).intrinsic;
				BundlePinholeSimplified dstIntrinsic = dst.getViewCamera(dstView).intrinsic;

				verbose.printf("id='%s' src={ f=%.1f k1=%.1e k2=%.1e } dst={ f=%.1f k1=%.1e k2=%.1e }\n",
						srcView.pview.id,
						srcIntrinsic.f, srcIntrinsic.k1, srcIntrinsic.k2,
						dstIntrinsic.f, dstIntrinsic.k1, dstIntrinsic.k2);
			}
		}

		if (verbose != null) {
			verbose.print("Common: size=" + commonViews.size + "/" + src.listViews.size() + " views={ ");
			for (int i = 0; i < commonViews.size; i++) {
				verbose.print("'" + commonViews.get(i).src.pview.id + "' ");
			}
			verbose.println("}");
		}
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, checks, refiner);
	}

	/**
	 * Specifies which two 'views" in each scene reference the same pairwise view.
	 */
	@SuppressWarnings({"NullAway.Init"})
	public static class CommonView {
		public SceneWorkingGraph.View src;
		public SceneWorkingGraph.View dst;
		public double score; // TODO remove?

		public void setTo( SceneWorkingGraph.View src, SceneWorkingGraph.View dst, double score ) {
			this.src = src;
			this.dst = dst;
			this.score = score;
		}

		@Override public String toString() {
			return "{id='" + src.pview.id + "' src.index=" + src.index + " dst.index=" + dst.index + "}";
		}
	}
}
