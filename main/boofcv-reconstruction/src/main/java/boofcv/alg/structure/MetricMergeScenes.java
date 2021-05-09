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
import boofcv.struct.image.ImageDimension;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_B;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.VerbosePrint;
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

	/** Used to refine the scene */
	@Getter RefineMetricWorkingGraph refiner = new RefineMetricWorkingGraph();

	// Found views which are common between the two scenes
	DogArray<CommonView> commonViews = new DogArray<>(CommonView::new);
	// Found scale and SE3 relating the two views. Global initial estimate
	ScaleSe3_F64 src_to_dst = new ScaleSe3_F64();

	// The work place scene where the results are temporarily stored. This way the 'src' and 'dst' are not
	// modified until we are sure the merge worked
	SceneWorkingGraph scene = new SceneWorkingGraph();

	// Look up table indicating which views in 'scene' have been added
	DogArray_B knownViews = new DogArray_B();
	// Input image dimension, needed for physical constraints checks
	List<ImageDimension> dimensions = new ArrayList<>();

	// Initially views are added without inliers as they are referenced by another view being optimized
	// These are next in line to have their inlier sets added
	List<SceneWorkingGraph.View> viewsNeedingInliers = new ArrayList<>();

	// List of views in 'src' that already have had their inlier sets added to the scene
	Set<String> viewsWithInliers = new HashSet<>();

	// Used to see if any views that are not known yet have been added. If not we will need to add some
	int unknownViewsAdded;

	// Local workspace
	Se3_F64 src_to_view = new Se3_F64();
	Se3_F64 transform_dst_to_src = new Se3_F64();

	@Nullable PrintStream verbose;

	public MetricMergeScenes() {
		checks.maxFractionFail = 1.0;
//		refiner.verboseViewInfo = false;
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
		dimensions.clear();
		viewsNeedingInliers.clear();
		viewsWithInliers.clear();

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

		if (verbose != null) verbose.println("computed scene transform from view='"+best.src.pview.id+"'");

		// Create a scene just from common views and the views that are in their inlier sets
		createSceneOfCommon(src, dst);
		// Make sure there are unknown views. Otherwise this is pointless
		if (unknownViewsAdded == 0)
			addViewsWhichReferenceViewsWithInliers(src);

		if (unknownViewsAdded == 0)
			throw new RuntimeException("src is either a bad graph or a subset");

		if (verbose != null)
			verbose.printf("src.size=%d dst.size=%d, common.size=%d scene.size=%d, scene_scale=%.2e\n",
					src.listViews.size(), dst.listViews.size(), commonViews.size, scene.listViews.size(), src_to_dst.scale);

		// Add all the views while merging them in
		incrementallyAddViewsToScene(db, src);

//		checks.checkPhysicalConstraints(refiner.bundleAdjustment, dimensions);

		// Sanity check to make sure the entire src has been merged
		int estimatedSrcSize = commonViews.size + knownViews.count(false);
		BoofMiscOps.checkEq(estimatedSrcSize, src.listViews.size(), "Bug in merge or bug in src scene");

		if (verbose != null) verbose.println("Final working scene.size="+scene.listViews.size());

		// Copy the results over
		mergeWorkingSceneIntoDst(dst);

		if (verbose != null) verbose.println("merged dst.size="+dst.listViews.size());

		return true;
	}

	/**
	 * Grows the work scene by adding views which are closest to the dst scene's border first. This avoids issue
	 * where the two scenes have diverged so much that distant views in src converge to a bad minimum
	 */
	private void incrementallyAddViewsToScene( LookUpSimilarImages db, SceneWorkingGraph src ) {
		// TODO consider locking a view if it was added a couple of iterations ago to speed things up?
		int iteration = 0;
		while (true) {
			if (verbose != null) verbose.printf("Iteration=%d, scene.size=%d\n", iteration++, scene.listViews.size());

			BoofMiscOps.checkEq(scene.listViews.size(), knownViews.size);

			// TODO when a view is first added do not use inliers that would be triangulated without info from dst

			boolean success = refiner.process(db, scene, utils -> {
				for (int i = 0; i < knownViews.size; i++) {
					utils.structure.cameras.get(i).known = knownViews.get(i);
					utils.structure.motions.get(i).known = knownViews.get(i);
				}
			});

			if (!success) {
				throw new RuntimeException("Refine failed");
			}

			// TODO sanity check and fix minor errors if needed

			// If a view was added before, but without its inlier sets, add those inlier sets now and add
			// the views it references without inlier sets
			unknownViewsAdded = 0;
			int sizeBefore = viewsNeedingInliers.size();
			for (int i = 0; i < sizeBefore; i++) {
				SceneWorkingGraph.View wview = viewsNeedingInliers.get(i);
				SceneWorkingGraph.View origView = src.views.get(wview.pview.id);

				DogArray<SceneWorkingGraph.InlierInfo> inliersSrc = origView.inliers;

				viewsWithInliers.add(wview.pview.id);
				if (verbose != null) verbose.println("Adding inliers to view='"+wview.pview.id+"' size="+inliersSrc.size);

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
				addViewsWhichReferenceViewsWithInliers(src);

			// it didn't add any new views, so either it's all done of the graph is bad
			if (unknownViewsAdded == 0)
				break;
		}
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
			dimensions.add(wview.imageDimension);
			viewsWithInliers.add(wview.pview.id);
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

		dimensions.add(copyView.imageDimension);

		if (!markAsKnown)
			convertNewViewCoordinateSystem(copyView);

		if (!markAsKnown)
			unknownViewsAdded++;
	}

	private void addViewsWhichReferenceViewsWithInliers( SceneWorkingGraph src ) {
		for (int i = 0; i < src.listViews.size(); i++) {
			SceneWorkingGraph.View wview = src.listViews.get(i);
			if (scene.views.containsKey(wview.pview.id))
				continue;

			boolean referencesKnown = false;
			DogArray<SceneWorkingGraph.InlierInfo> inliers = wview.inliers;
			escape:
			for (int inlierIdx = 0; inlierIdx < inliers.size; inlierIdx++) {
				SceneWorkingGraph.InlierInfo info = inliers.get(inlierIdx);
				for (int infoIdx = 0; infoIdx < info.views.size; infoIdx++) {
					String id = info.views.get(infoIdx).id;
					if (!viewsWithInliers.contains(id))
						continue;
					if (verbose != null)
						verbose.println("Adding view-no-inliers id='" + wview.pview.id + "' references'" + id + "' f="+wview.intrinsic.f);
					referencesKnown = true;
					break escape;
				}
			}

			if (!referencesKnown)
				continue;

			copyIntoSceneJustState(src, false, wview.pview);
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

			if (dstView == null)
				continue;

			// use the worst score of the pair as being pessimistic seems to work best in reconstruction
			double score = Math.min(srcView.getBestInlierScore(), dstView.getBestInlierScore());
			commonViews.grow().setTo(srcView, dstView, score);

			if (verbose != null) {
				verbose.printf("common.id='%s' src.f=%.2f dst.f=%.2f\n",
						srcView.pview.id, srcView.intrinsic.f, dstView.intrinsic.f);
			}
		}

		if (verbose != null) {
			verbose.print("Common: size="+commonViews.size+" views={ ");
			for (int i = 0; i < commonViews.size; i++) {
				verbose.print("'"+commonViews.get(i).src.pview.id+"' ");
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
