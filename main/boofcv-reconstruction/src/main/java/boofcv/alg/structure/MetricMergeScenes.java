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

	// Local workspace
	Se3_F64 src_to_view = new Se3_F64();
	Se3_F64 transform_dst_to_src = new Se3_F64();

	@Nullable PrintStream verbose;

	public MetricMergeScenes() {
		// only fail on catastrophic errors
		checks.maxFractionFail = 1.0;
		// Don't spam stdout
		refiner.verboseViewInfo = false;
	}

	/**
	 * Merges the 'src' scene into 'dst'. Both scenes are only modified if true is returned.
	 *
	 * @param db Contains image related information
	 * @param src The scene being merged into 'dst'
	 * @param dst Where the combined scene will be stored. This is assumed to be the 'more correct' scene
	 * @return true if src was merged into dst or false if it failed and nothing has been modified
	 */
	public boolean merge( LookUpSimilarImages db, SceneWorkingGraph src, SceneWorkingGraph dst ) {
		// Find the common views
		findCommonViews(src, dst);
		if (commonViews.isEmpty())
			return false;

		// Sort in order of "best" last
		Collections.sort(commonViews.toList(), ( a, b ) -> Double.compare(a.score, b.score));

		// Compute the coordinate transform between the scenes using the "best" pair of views
		CommonView best = commonViews.getTail();
		mergingOps.computeSceneTransform(db, src, dst, best.src, best.dst, src_to_dst);
		src_to_dst.transform.invert(transform_dst_to_src);

		// Creates a working scene from 'src' but adding values from 'dst' as needed
		createWorkScene(src, dst);

		// Refine the working scene. This will help mend the differences between the two scenes
		if (!refiner.process(db, workScene, utils -> {
			for (int i = 0; i < knownViews.size; i++) {
				utils.structure.cameras.get(i).known = knownViews.get(i);
				utils.structure.motions.get(i).known = knownViews.get(i);
			}
		})) {
			if (verbose != null) verbose.println("FAiLED: Refiner return false");
			return false;
		}

		// Examine all the common views to see if the physical constraints in 'src' inlier sets are still valid
		// If they are, fix minor errors, then the merge is considered to be successful
		if (!verifyAndFixConstraints(db))
			return false;

		// Print the state of views which are not common so you can see how much they have changed.
		if (verbose != null) {
			for (int i = 0; i < workScene.listViews.size(); i++) {
				SceneWorkingGraph.View wview = workScene.listViews.get(i);
				if (dst.views.get(wview.pview.id) != null)
					continue;

				verbose.printf("After id='%s' src={ f=%.1f k1=%.1e k2=%.1e }\n",
						wview.pview.id, wview.intrinsic.f, wview.intrinsic.k1, wview.intrinsic.k2);
			}
		}

		// Copy the results to 'dst'.
		mergeWorkingSceneIntoDst(dst);

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

				SceneWorkingGraph.InlierInfo info = wview.inliers.get(inlierIdx);
				int numInliers = info.getInlierCount();

				BoofMiscOps.checkEq(numInliers, checks.badFeatures.size);
				int countBadFeatures = checks.badFeatures.count(true);

				if (countBadFeatures > fractionBadFeaturesRecover*checks.badFeatures.size) {
					// TODO print out more info about the views
					if (verbose != null)
						verbose.println("FAILED: Inlier set had too many bad features. bad=" + countBadFeatures + "/" + numInliers);
					return false;
				}

				// If there are no issues move on to the next view
				if (countBadFeatures == 0)
					continue;

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
			}
		}
		return true;
	}

	/**
	 * Creates a local work scene by making a copy of 'src' and copying over parts of 'dst' as needed
	 */
	private void createWorkScene( SceneWorkingGraph src, SceneWorkingGraph dst ) {
		// Local copy of the 'src' scene
		workScene.setTo(src);
		// All views that are in 'src' will be modified
		knownViews.resetResize(workScene.listViews.size(), false);

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
			wview.intrinsic.setTo(viewDst.intrinsic);

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
	 * Copy the working scene into 'dst'. We do not copy the common views over, only add their inliers. The idea
	 * being that we could have messed up their state when refining the working scene.
	 */
	private void mergeWorkingSceneIntoDst( SceneWorkingGraph dst ) {
		for (int viewIdx = 0; viewIdx < workScene.listViews.size(); viewIdx++) {
			SceneWorkingGraph.View wview = workScene.listViews.get(viewIdx);
			SceneWorkingGraph.View viewDst = dst.views.get(wview.pview.id);
			if (viewDst != null) {
				// just need to copy the inliers over from src. 'dst' inliers are at the end
				int end = wview.inliers.size - viewDst.inliers.size;
				for (int inlierIdx = 0; inlierIdx < end; inlierIdx++) {
					viewDst.inliers.grow().setTo(wview.inliers.get(inlierIdx));
				}
			} else {
				viewDst = dst.addView(wview.pview);
				viewDst.setTo(wview);
				viewDst.index = dst.listViews.size() - 1; // setTo() overwrote the index
			}

			BoofMiscOps.checkTrue(!viewDst.inliers.isEmpty());
		}
	}

	/**
	 * Converts the coordinate system in 'src' into one that is compatible with 'dst'
	 */
	private void convertNewViewCoordinateSystem( SceneWorkingGraph.View wview ) {
		src_to_view.setTo(wview.world_to_view);
		src_to_view.T.scale(src_to_dst.scale);
		transform_dst_to_src.concat(src_to_view, wview.world_to_view);
	}

	private void addViewsButNoInliers( SceneWorkingGraph origScene,
									   FastArray<PairwiseImageGraph.View> viewsToCopy,
									   boolean markAsKNown ) {
		for (int viewIdx = 0; viewIdx < viewsToCopy.size; viewIdx++) {
			PairwiseImageGraph.View pview = viewsToCopy.get(viewIdx);
			if (workScene.views.containsKey(pview.id))
				continue;

			// Make sure all the views which are referenced are added to the local sub-scene
			// They will be static so we don't need to add their inlier info too
			copyIntoSceneJustState(origScene, markAsKNown, pview);
			if (verbose != null) verbose.println("Adding view-no-inliers id='" + pview.id + "' known=" + markAsKNown);
		}
	}

	private void copyIntoSceneJustState( SceneWorkingGraph origScene, boolean markAsKnown, PairwiseImageGraph.View pview ) {
		SceneWorkingGraph.View origView = origScene.views.get(pview.id);
		SceneWorkingGraph.View copyView = workScene.addView(pview);
		copyView.imageDimension.setTo(origView.imageDimension);
		copyView.world_to_view.setTo(origView.world_to_view);
		copyView.intrinsic.setTo(origView.intrinsic);
		knownViews.add(markAsKnown);
	}

	/**
	 * Finds all views which are common between the two scenes
	 */
	public void findCommonViews( SceneWorkingGraph src, SceneWorkingGraph dst ) {
		commonViews.reset();

		// Go through all the views in the src list
		for (int srcViewIdx = 0; srcViewIdx < src.listViews.size(); srcViewIdx++) {
			SceneWorkingGraph.View srcView = src.listViews.get(srcViewIdx);
			SceneWorkingGraph.View dstView = dst.views.get(srcView.pview.id);

			if (dstView == null) {
				if (verbose != null) {
					verbose.printf("id='%s' src={ f=%.1f k1=%.1e k2=%.1e }\n",
							srcView.pview.id, srcView.intrinsic.f, srcView.intrinsic.k1, srcView.intrinsic.k2);
				}
				continue;
			}

			// use the worst score of the pair as being pessimistic seems to work best in reconstruction
			double score = Math.min(srcView.getBestInlierScore(), dstView.getBestInlierScore());
			commonViews.grow().setTo(srcView, dstView, score);

			if (verbose != null) {
				verbose.printf("id='%s' src={ f=%.1f k1=%.1e k2=%.1e } dst={ f=%.1f k1=%.1e k2=%.1e }\n",
						srcView.pview.id,
						srcView.intrinsic.f, srcView.intrinsic.k1, srcView.intrinsic.k2,
						dstView.intrinsic.f, dstView.intrinsic.k1, dstView.intrinsic.k2);
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
