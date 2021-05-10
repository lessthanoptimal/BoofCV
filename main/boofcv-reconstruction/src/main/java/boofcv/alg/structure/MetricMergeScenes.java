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
import org.ddogleg.struct.*;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Merges two scenes together after their metric elevation. The 'src' scene will be merged in to the 'dst' scene. The
 * dst scene is the dominant one and assumed to be "correct" when there's a conflict. An incremental approach
 * is taking to merging the scenes where views in 'src' are merged into 'dst' based on their graph distance
 * from the common/overlapping views.
 *
 * Merging two scenes is challenging since each scene can have a drastically different interpretation of reality
 * and yet be internally consistent. To start there is a scale ambiguity that needs to be resolved and the
 * coordinate systems (SE3) need to have a common origin. It's also not uncommon drastically different
 * focal lengths to be found, which leads to a different skewed version of reality.
 *
 * Common views are used as an anchor that forces the 'src' views to bend to the reality as seen by 'dst'. The
 * incremental approach reduces the chance of 'src' getting stuck in a local minimum while these two scenes are
 * being merged. At first a global transform was applied but that tended to get stuck in highly non-optimal
 * solutions.
 *
 * @author Peter Abeles
 */
public class MetricMergeScenes implements VerbosePrint {

	// Used to verify the correctness of the merger
	MetricSanityChecks checks = new MetricSanityChecks();

	// Helpful functions
	SceneMergingOperations mergingOps = new SceneMergingOperations();

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
	SceneWorkingGraph scene = new SceneWorkingGraph();

	// Number of inlier sets from 'dst' in each view
	DogArray_I32 countInliersDst = new DogArray_I32();

	// Look up table indicating which views in 'scene' are not to be modified when refining
	DogArray_B knownViews = new DogArray_B();

	// Initially views are added without inliers as they are referenced by another view being optimized
	// These are next in line to have their inlier sets added
	List<SceneWorkingGraph.View> viewsNeedingInliers = new ArrayList<>();

	// List of views in 'src' that already have had their inlier sets added to the scene
	Set<String> viewsWithInliers = new HashSet<>();

	Set<String> viewsNotMerged = new HashSet<>();

	// Used to see if any views that are not known yet have been added. If not we will need to add some
	int unknownViewsAdded;

	// Views in the scene which need to be validated/fixed still
	DogArray_I32 viewsToCheck = new DogArray_I32();

	// Local workspace
	Se3_F64 src_to_view = new Se3_F64();
	Se3_F64 transform_dst_to_src = new Se3_F64();

	@Nullable PrintStream verbose;

	public MetricMergeScenes() {
		checks.maxFractionFail = 1.0;
		refiner.verboseViewInfo = false;

		// This adds a filter that prevents features from being added to the SBA scene which would be
		// triangulated using views that have been merged in. This is to prevent the old 'src' state
		// from fighting the merger into 'dst'
		refiner.inlierFilter = ( view, info ) -> {
			for (int i = 0; i < info.views.size; i++) {
				SceneWorkingGraph.View wview = scene.views.get(info.views.get(i).id);

				// If it's "known" it doesn't require inliers. Probably from 'dst' scene
				if (knownViews.get(wview.index))
					continue;

				if (viewsNotMerged.contains(wview.pview.id))
					return false;
//				if (wview.inliers.isEmpty())
//					return false;
			}
			return true;
		};
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
		unknownViewsAdded = 0;
		viewsNeedingInliers.clear();
		viewsWithInliers.clear();
		viewsToCheck.reset();
		countInliersDst.reset();

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

		if (verbose != null) verbose.println("computed scene transform from view='" + best.src.pview.id + "'");

		// Create a scene just from common views and the views that are in their inlier sets
		createSceneOfCommon(src, dst);
		// Make sure there are unknown views. Otherwise this is pointless
		if (unknownViewsAdded == 0)
			addViewsWhichReferenceMergedViews(src);

		if (unknownViewsAdded == 0)
			throw new RuntimeException("src is either a bad graph or a subset");

		if (verbose != null)
			verbose.printf("src.size=%d dst.size=%d, common.size=%d scene.size=%d, scene_scale=%.2e\n",
					src.listViews.size(), dst.listViews.size(), commonViews.size, scene.listViews.size(), src_to_dst.scale);

		// Add all the views while merging them in
		if (!incrementallyAddViewsToScene(db, src)) {
			return false;
		}
		// TODO allow for partial merges

//		checks.checkPhysicalConstraints(refiner.bundleAdjustment, dimensions);

		// Sanity check to make sure the entire src has been merged
		int estimatedSrcSize = commonViews.size + knownViews.count(false);
		BoofMiscOps.checkEq(estimatedSrcSize, src.listViews.size(), "Bug in merge or bug in src scene");

		if (verbose != null) verbose.println("Final working scene.size=" + scene.listViews.size());

		// Copy the results over
		mergeWorkingSceneIntoDst(dst);

		if (verbose != null) verbose.println("merged dst.size=" + dst.listViews.size());

		return true;
	}

	/**
	 * Grows the work scene by adding views which are closest to the dst scene's border first. This avoids issue
	 * where the two scenes have diverged so much that distant views in src converge to a bad minimum
	 */
	private boolean incrementallyAddViewsToScene( LookUpSimilarImages db, SceneWorkingGraph src ) {
		// TODO consider locking a view if it was added a couple of iterations ago to speed things up?
		int iteration = 0;
		while (true) {
			if (verbose != null) verbose.printf("Iteration=%d, scene.size=%d !merge=%d\n",
					iteration++, scene.listViews.size(), viewsNotMerged.size());

			BoofMiscOps.checkEq(scene.listViews.size(), knownViews.size);

			if (!refiner.process(db, scene, utils -> {
				for (int i = 0; i < knownViews.size; i++) {
					utils.structure.cameras.get(i).known = knownViews.get(i);
					utils.structure.motions.get(i).known = knownViews.get(i);
				}
			})) {
				if (verbose != null) verbose.println("Refine failed. First pass.");
				return false;
			}

			// all the new views now have information from 'dst' and are now merged
			viewsNotMerged.clear();

			if (!optimizeIterateFix(db))
				return false;

			// TODO if a view passes the merge then don't optimize it any more

			viewsToCheck.reset();

			// If a view was added before, but without its inlier sets, add those inlier sets now and add
			// the views it references without inlier sets
			unknownViewsAdded = 0;
			int sizeBefore = viewsNeedingInliers.size();
			for (int i = 0; i < sizeBefore; i++) {
				SceneWorkingGraph.View wview = viewsNeedingInliers.get(i);
				SceneWorkingGraph.View origView = src.views.get(wview.pview.id);

				// Now that it has inliers we should verify its correctness with constraints
				viewsToCheck.add(wview.index);
				// Sanity check to make sure src and scene weren't mixed up
				BoofMiscOps.checkTrue(wview.index < scene.listViews.size());
				DogArray<SceneWorkingGraph.InlierInfo> inliersSrc = origView.inliers;

				viewsWithInliers.add(wview.pview.id);
				if (verbose != null)
					verbose.printf("Adding inliers. view='%s' size=%d, f=%.1f k1=%.2e k2=%.2e\n",
							wview.pview.id, inliersSrc.size, wview.intrinsic.f, wview.intrinsic.k1, wview.intrinsic.k2);

				// Copy over the inlier information so that features can be triangulated
				for (int infoIdx = 0; infoIdx < inliersSrc.size; infoIdx++) {
					SceneWorkingGraph.InlierInfo orig = inliersSrc.get(infoIdx);
					SceneWorkingGraph.InlierInfo copy = wview.inliers.grow();
					copy.setTo(orig);

					addViewsButNoInliers(src, orig.views, false);
				}
			}

			// Remove from the list views which have had their inliers added
			for (int i = 0; i < sizeBefore; i++) {
				viewsNeedingInliers.remove(0);
			}

			if (unknownViewsAdded == 0)
				addViewsWhichReferenceMergedViews(src);

			// it didn't add any new views, so either it's all done of the graph is bad
			if (unknownViewsAdded == 0)
				break;
		}

		return optimizeIterateFix(db);
	}

	private boolean optimizeIterateFix( LookUpSimilarImages db ) {
		CheckResults results = CheckResults.FATAL;
		// Attempt to fix views which were just added
		escape:
		for (int trial = 0; trial < 2; trial++) {
			// Run with all the inlier sets
			if (!refiner.process(db, scene, utils -> {
				for (int i = 0; i < knownViews.size; i++) {
					utils.structure.cameras.get(i).known = knownViews.get(i);
					utils.structure.motions.get(i).known = knownViews.get(i);
				}
			})) {
				if (verbose != null) verbose.println("Refine failed. Second pass.");
				return false;
			}

			results = examineAndFixViews(db);
			if (verbose != null) verbose.printf("Check/Fix: trial=%d results=%s\n", trial, results);
			switch (results) {
				case FATAL:
					return false;
				case PERFECT:
					break escape;
			}
		}

		return results == CheckResults.PERFECT;
	}

	private CheckResults examineAndFixViews( LookUpSimilarImages db ) {
		CheckResults results = CheckResults.PERFECT;
		for (int i = 0; i < viewsToCheck.size; i++) {
			int viewIdx = viewsToCheck.get(i);
			SceneWorkingGraph.View wview = scene.listViews.get(viewIdx);

			CheckResults viewResults = applyConstraintsAndFix(db, wview);
			if (viewResults == CheckResults.FATAL)
				return viewResults;
			if (viewResults.ordinal() > results.ordinal())
				results = viewResults;
		}
		return results;
	}

	private void mergeWorkingSceneIntoDst( SceneWorkingGraph dst ) {
		for (int viewIdx = 0; viewIdx < scene.listViews.size(); viewIdx++) {
			SceneWorkingGraph.View wview = scene.listViews.get(viewIdx);
			SceneWorkingGraph.View viewDst = dst.views.get(wview.pview.id);
			if (viewDst != null) {
				// just need to copy the inliers over
				for (int inlierIdx = viewDst.inliers.size; inlierIdx < wview.inliers.size; inlierIdx++) {
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

	private void createSceneOfCommon( SceneWorkingGraph src, SceneWorkingGraph dst ) {
		scene.reset();
		knownViews.reset();

		// Create the common views in the scene. These will all be considered known
		for (int i = 0; i < commonViews.size; i++) {
			SceneWorkingGraph.View orig = commonViews.get(i).dst;
			SceneWorkingGraph.View wview = scene.addView(orig.pview);
			wview.world_to_view.setTo(orig.world_to_view);
			wview.intrinsic.setTo(orig.intrinsic);
			wview.imageDimension.setTo(orig.imageDimension);

			knownViews.add(true);
			viewsWithInliers.add(wview.pview.id);
			viewsToCheck.add(i);
			countInliersDst.add(orig.inliers.size);
		}

		// Add the inliers from dst first. This is used to anchor the world in place
		for (int i = 0; i < commonViews.size; i++) {
			SceneWorkingGraph.View wview = scene.listViews.get(i);
			DogArray<SceneWorkingGraph.InlierInfo> inliersDst = commonViews.get(i).dst.inliers;

			// Copy over the inlier information so that features can be triangulated
			for (int infoIdx = 0; infoIdx < inliersDst.size; infoIdx++) {
				SceneWorkingGraph.InlierInfo orig = inliersDst.get(infoIdx);
				SceneWorkingGraph.InlierInfo copy = wview.inliers.grow();
				copy.setTo(orig);

				addViewsButNoInliers(dst, orig.views, true);
			}
		}

		// Now add inliers from the src and any views they reference.
		for (int i = 0; i < commonViews.size; i++) {
			SceneWorkingGraph.View wview = scene.listViews.get(i);
			DogArray<SceneWorkingGraph.InlierInfo> inliersSrc = commonViews.get(i).src.inliers;

			// Copy over the inlier information so that features can be triangulated
			for (int infoIdx = 0; infoIdx < inliersSrc.size; infoIdx++) {
				SceneWorkingGraph.InlierInfo orig = inliersSrc.get(infoIdx);
				SceneWorkingGraph.InlierInfo copy = wview.inliers.grow();
				copy.setTo(orig);

				addViewsButNoInliers(src, orig.views, false);
			}
		}
	}

	private void convertNewViewCoordinateSystem( SceneWorkingGraph.View wview ) {
		src_to_view.setTo(wview.world_to_view);
		src_to_view.T.scale(src_to_dst.scale);
		transform_dst_to_src.concat(src_to_view, wview.world_to_view);
		viewsNeedingInliers.add(wview);
	}

	private void addViewsButNoInliers( SceneWorkingGraph origScene,
									   FastArray<PairwiseImageGraph.View> viewsToCopy,
									   boolean markAsKNown ) {
		for (int viewIdx = 0; viewIdx < viewsToCopy.size; viewIdx++) {
			PairwiseImageGraph.View pview = viewsToCopy.get(viewIdx);
			if (scene.views.containsKey(pview.id))
				continue;

			// Make sure all the views which are referenced are added to the local sub-scene
			// They will be static so we don't need to add their inlier info too
			copyIntoSceneJustState(origScene, markAsKNown, pview);
			if (verbose != null) verbose.println("Adding view-no-inliers id='" + pview.id + "' known=" + markAsKNown);
		}
	}

	private void copyIntoSceneJustState( SceneWorkingGraph origScene, boolean markAsKnown, PairwiseImageGraph.View pview ) {
		SceneWorkingGraph.View origView = origScene.views.get(pview.id);
		SceneWorkingGraph.View copyView = scene.addView(pview);
		copyView.imageDimension.setTo(origView.imageDimension);
		copyView.world_to_view.setTo(origView.world_to_view);
		copyView.intrinsic.setTo(origView.intrinsic);
		knownViews.add(markAsKnown);

		// There are no inlier sets from the 'dst' scene which can't be modified
		countInliersDst.add(0);

		if (!markAsKnown)
			convertNewViewCoordinateSystem(copyView);

		if (!markAsKnown) {
			BoofMiscOps.checkTrue(viewsNotMerged.add(copyView.pview.id));
			unknownViewsAdded++;
		}
	}

	private void addViewsWhichReferenceMergedViews( SceneWorkingGraph src ) {

		int sizeBefore = viewsNeedingInliers.size();
		for (int i = 0; i < src.listViews.size(); i++) {
			SceneWorkingGraph.View srcWView = src.listViews.get(i);

			if (scene.views.containsKey(srcWView.pview.id))
				continue;

			boolean referencesKnown = false;
			DogArray<SceneWorkingGraph.InlierInfo> inliersSrc = srcWView.inliers;
			escape:
			for (int inlierIdx = 0; inlierIdx < inliersSrc.size; inlierIdx++) {
				SceneWorkingGraph.InlierInfo info = inliersSrc.get(inlierIdx);
				for (int infoIdx = 0; infoIdx < info.views.size; infoIdx++) {
					String id = info.views.get(infoIdx).id;
					if (!viewsWithInliers.contains(id))
						continue;
					if (verbose != null)
						verbose.println("Adding view id='" + srcWView.pview.id + "' references='" + id + "'");
					referencesKnown = true;
					break escape;
				}
			}

			if (!referencesKnown)
				continue;

			copyIntoSceneJustState(src, false, srcWView.pview);
		}

		// Add inliers to the views just added. Inliers are needed since otherwise there will
		// be no connection to the referenced view and the new view's state will not be updated
		for (int i = viewsNeedingInliers.size() - 1; i >= sizeBefore; i--) {
			SceneWorkingGraph.View wview = viewsNeedingInliers.remove(i);
			viewsWithInliers.add(wview.pview.id);
			viewsToCheck.add(wview.index);

			SceneWorkingGraph.View srcWView = src.lookupView(wview.pview.id);
			DogArray<SceneWorkingGraph.InlierInfo> inliersSrc = srcWView.inliers;

			for (int infoIdx = 0; infoIdx < inliersSrc.size; infoIdx++) {
				SceneWorkingGraph.InlierInfo orig = inliersSrc.get(infoIdx);
				SceneWorkingGraph.InlierInfo copy = wview.inliers.grow();
				copy.setTo(orig);

				addViewsButNoInliers(src, orig.views, false);
			}
		}
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

	CheckResults applyConstraintsAndFix( LookUpSimilarImages db, SceneWorkingGraph.View wview ) {
		if (scene.listViews.get(wview.index).inliers.isEmpty())
			throw new RuntimeException("BUG");

		// TODO reduce number of times pixels are looked up and normalized. Compute once for all sets
		// Loop through all inlier sets, except those from 'dst' since those can't be modified
		CheckResults results = CheckResults.PERFECT;
		for (int inlierIdx = countInliersDst.get(wview.index); inlierIdx < wview.inliers.size; inlierIdx++) {
			if (verbose != null && checks.verbose == null)
				verbose.printf("Constraints: view.id='%s' inlierIdx=%d\n", wview.pview.id, inlierIdx);
			if (!checks.checkPhysicalConstraints(db, scene, wview, inlierIdx))
				return CheckResults.FATAL;

			SceneWorkingGraph.InlierInfo info = wview.inliers.get(inlierIdx);
			int numInliers = info.getInlierCount();

			BoofMiscOps.checkEq(numInliers, checks.badFeatures.size);
			int countBadFeatures = checks.badFeatures.count(true);

			if (countBadFeatures > fractionBadFeaturesRecover*checks.badFeatures.size) {
				// TODO print out more info about the views
				if (verbose != null)
					verbose.println("FAILED: Inlier set had too many bad features. bad=" + countBadFeatures + "/" + numInliers);
				return CheckResults.FATAL;
			}

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
			results = CheckResults.RERUN;
		}

		return results;
	}

	@Override public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration, checks, refiner);
	}

	enum CheckResults {
		PERFECT,
		RERUN,
		FATAL
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
