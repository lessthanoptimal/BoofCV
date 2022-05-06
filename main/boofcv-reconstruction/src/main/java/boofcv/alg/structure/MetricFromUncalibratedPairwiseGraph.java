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

package boofcv.alg.structure;

import boofcv.alg.structure.expand.MetricExpandByOneView;
import boofcv.alg.structure.spawn.MetricSpawnSceneFromView;
import boofcv.misc.BoofMiscOps;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fully computes views (intrinsics + SE3) for each view and saves which observations were inliers. This should
 * be considered a first pass and all optimization is done at a local level.
 *
 * <ol>
 * <li>Input: {@link PairwiseImageGraph} and {@link LookUpSimilarImages image information}</li>
 * <li>Selected a set of views to estimate a projective scene based on having good geometry.
 * {@link InitializeAllCommonProjective}</li>
 * <li>Metric elevation from initial seed views</li>
 * <li>Grow metric scene one at a time using previously found metric views.
 * {@link MetricExpandByOneView}</li>
 * <li>Stop when all views have been considered</li>
 * </ol>
 *
 * Output is contained in {@link SceneWorkingGraph} and accessible from TODO update. 3D point features
 * are not part of the output directly. Observations used are saved and can be used to triangulate the 3D features.
 * It's advisable to perform bundle adjustment and outlier rejection and the scene as a whole.
 *
 * <p>
 * <b>Important Note:</b> It's automatically assumed that the image center is the principle point and all
 * pixels are shifted by this amount. This means that the found intrinsic parameters will have (cx,cy) = (0,0).
 * </p>
 *
 * @author Peter Abeles
 */
public class MetricFromUncalibratedPairwiseGraph extends ReconstructionFromPairwiseGraph {

	/**
	 * When expanding a scene, SBA is applied to the entire scene until it has this many views. This often helps
	 * improve the initial metric scene estimate significantly, but can be expensive.
	 */
	public @Getter @Setter int refineSceneWhileExpandingMaxViews = 6;

	// Uses known metric views to expand the metric reconstruction by one view
	private final @Getter MetricExpandByOneView expandMetric = new MetricExpandByOneView();

	private final @Getter RefineMetricWorkingGraph refineWorking = new RefineMetricWorkingGraph();

	private final @Getter RefineMetricWorkingGraph refineBeforeMerge = new RefineMetricWorkingGraph();

	private final @Getter MetricSpawnSceneFromView spawnScene;

	private final @Getter MetricMergeScenes mergeScenes = new MetricMergeScenes();

	/** List of all the scenes. There can be multiple at the end if not everything is connected */
	final @Getter DogArray<SceneWorkingGraph> scenes =
			new DogArray<>(SceneWorkingGraph::new, SceneWorkingGraph::reset);

	SceneMergingOperations mergeOps = new SceneMergingOperations();

	/** Which scenes are include which views */
	PairwiseViewScenes scenesInEachView = new PairwiseViewScenes();

	public MetricFromUncalibratedPairwiseGraph( PairwiseGraphUtils utils ) {
		super(utils);
		expandMetric.utils = utils;

		spawnScene = new MetricSpawnSceneFromView(refineWorking, utils);
	}

	public MetricFromUncalibratedPairwiseGraph( ConfigProjectiveReconstruction config ) {
		this(new PairwiseGraphUtils(config));
	}

	public MetricFromUncalibratedPairwiseGraph() {
		this(new ConfigProjectiveReconstruction());
	}

	{
		// prune outlier observations and run SBA a second time
		refineWorking.metricSba.keepFraction = 0.95;
	}

	/**
	 * Performs a projective reconstruction of the scene from the views contained in the graph
	 *
	 * @param dbSimilar (input) Contains information on each image
	 * @param pairwise (input) Relationship between the images
	 * @return true if successful or false if it failed and results can't be used
	 */
	public boolean process( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams, PairwiseImageGraph pairwise ) {
		scenes.reset();

		// Declare storage for book keeping at each view
		scenesInEachView.initialize(pairwise);

		// Score nodes for their ability to be seeds
		Map<String, SeedInfo> mapScores = scoreNodesAsSeeds(pairwise, 2);

		if (seedScores.isEmpty()) {
			if (verbose != null) verbose.println("No seeds found. pairwise.size=" + pairwise.nodes.size);
		}

		// Create new seeds in priority of their scores
		selectAndSpawnSeeds(dbSimilar, dbCams, pairwise, seedScores, mapScores);
		// TODO change how initial seeds are handled. 1) Number should be dynamically adjusted. 2) Determine why
		//      it does much better when the number of views is 3. Something isn't handled correctly later on.

		// Questions:
		//  Ditch - why did both scenes grow to consume all views?

		if (scenes.isEmpty()) {
			if (verbose != null) verbose.println("Failed to upgrade any of the seeds to a metric scene.");
			return false;
		}

		if (verbose != null) verbose.println("Total Scenes: " + scenes.size);

		// Expand all the scenes until they can't any more
		expandScenes(dbSimilar, dbCams);

		// Refine all the scenes using all data available
		refineScenes(dbSimilar);

		// Merge scenes together until there are no more scenes which can be merged
		mergeScenes(dbSimilar);
		removeMergedScenes();
		// TODO local SBA with fixed parameters in master when merging

		// There can be multiple scenes at the end that are disconnected and share no views in common

		if (verbose != null) verbose.println("Done.");
		return true;
	}

	/**
	 * It will attempt to find a metric scene around the specified scene.
	 *
	 * @return true if successful or false if it failed
	 */
	@Override
	protected boolean spawnSceneFromSeed( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams,
										  PairwiseImageGraph pairwise, SeedInfo info ) {
		if (!spawnScene.process(dbSimilar, dbCams, pairwise, info.seed, info.motions)) {
			return false;
		}

		// Save the new scene
		SceneWorkingGraph scene = scenes.grow();
		scene.setTo(spawnScene.getScene());
		scene.index = scenes.size - 1;

		// The function for computing geometric score for an inlier set lies in this code so we have to add it here
		// The geometric score should be the same for all views
		double scoreGeometric = computeGeometricScore(scene, scene.listViews.get(0).inliers.get(0));
		for (int viewIdx = 0; viewIdx < scene.listViews.size(); viewIdx++) {
			scene.listViews.get(viewIdx).inliers.get(0).scoreGeometric = scoreGeometric;

			// Also add it to the list of scenes in each view
			scenesInEachView.getView(scene.listViews.get(viewIdx).pview).viewedBy.add(scene.index);
		}

		return true;
	}

	/**
	 * Expand the scenes until there are no more views they can be expanded into. A scene can expand into
	 * a view if it's connected to a view which already belongs to the scene and at least one of those
	 * connected views does not belong to any other scenes.
	 */
	void expandScenes( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCam ) {
		if (verbose != null) verbose.println("Expand Scenes: Finding open views in each scene");

		// Initialize the expansion by finding all the views each scene could expand into
		for (int sceneIdx = 0; sceneIdx < scenes.size; sceneIdx++) {
			SceneWorkingGraph scene = scenes.get(sceneIdx);

			// Mark views which were learned in the spawn as known
			scene.listViews.forEach(wv -> scene.exploredViews.add(wv.pview.id));

			// Add views which can be expanded into
			findAllOpenViews(scene);

			if (verbose != null) verbose.println("scene[" + sceneIdx + "].open.size=" + scene.open.size);
		}

		// Workspace for selecting which scene and view to expand into
		var best = new Expansion();
		var candidate = new Expansion();

		// Loop until it can't expand anymore
		while (true) {
			if (verbose != null) verbose.println("Selecting next scene/view to expand.");

			// Clear previous best results
			best.reset();

			// Go through each scene and select the view to expand into with the best score
			for (int sceneIdx = 0; sceneIdx < scenes.size; sceneIdx++) {
				SceneWorkingGraph scene = scenes.get(sceneIdx);
				if (scene.open.isEmpty())
					continue;

				// TODO consider removing scenes from the open list if other scenes have grown into this territory
				//      there is probably too much overlap now

				if (!selectNextToProcess(scene, candidate)) {
					// TODO remove this scene from the active list?
					if (verbose != null) verbose.println("_ No valid views left. open.size=" + scene.open.size);
					continue;
				}

				// If the candidate is better swap with the best
				if (candidate.score > best.score) {
					Expansion tmp = best;
					best = candidate;
					candidate = tmp;
				}
			}

			// See if there is nothing left to expand into
			if (best.score <= 0) {
				break;
			}

			// Get the view and remove it from the open list
			PairwiseImageGraph.View view = best.scene.open.removeSwap(best.openIdx);

			if (verbose != null)
				verbose.printf("Expanding scene[%d].view='%s' score=%.2f\n", best.scene.index, view.id, best.score);

			best.scene.listViews.forEach(v -> BoofMiscOps.checkTrue(!v.inliers.isEmpty()));

			if (!expandIntoView(dbSimilar, dbCam, best.scene, view)) {
				best.scene.listViews.forEach(v -> BoofMiscOps.checkTrue(!v.inliers.isEmpty()));
				continue;
			}

			best.scene.listViews.forEach(v -> BoofMiscOps.checkTrue(!v.inliers.isEmpty()));

			if (best.scene.listViews.size() > refineSceneWhileExpandingMaxViews)
				continue;

			refineWorking.process(dbSimilar, best.scene);
		}
	}

	/**
	 * Check to see the scene is allowed to expand from the specified view. The goal here is to have some redundancy
	 * in views between scenes but not let all scenes expand unbounded and end up as duplicates of each other.
	 *
	 * @return true if the scene can be expanded from this view
	 */
	boolean canSpawnFromView( SceneWorkingGraph scene, PairwiseImageGraph.View pview ) {
		// If no scene already contains the view then there are no restrictions
		if (scenesInEachView.getView(pview).viewedBy.size == 0)
			return true;

		// If another scene also occupies the view then we can only expand from it if it is part of the seed set
		// The idea is that the seed set could have produced a bad reconstruction that needs to be jumped over
		ViewScenes views = scenesInEachView.getView(pview);
		boolean usable = true;
		for (int idxA = 0; idxA < views.viewedBy.size; idxA++) {
			SceneWorkingGraph viewsByScene = scenes.get(views.viewedBy.get(idxA));
			BoofMiscOps.checkTrue(viewsByScene != scene, "Scene should not already have this view");

			// If it has an inlier set it was spawned outside of the seed set (except for the seed view)
			// and we should not expand in to it
			if (!viewsByScene.isSeedSet(pview.id)) {
				usable = false;
				break;
			}
		}
		return usable;
	}

	void refineScenes( LookUpSimilarImages dbSimilar ) {
		if (verbose != null) verbose.println("Refining all scenes before merging");
		for (int sceneIdx = 0; sceneIdx < scenes.size; sceneIdx++) {
			refineBeforeMerge.process(dbSimilar, scenes.get(sceneIdx));
		}
	}

	/**
	 * Merge the different scenes together if they share common views
	 */
	void mergeScenes( LookUpSimilarImages dbSimilar ) {
		if (verbose != null) verbose.println("Merging Scenes. scenes.size=" + scenes.size);

		SceneMergingOperations.SelectedScenes selected = new SceneMergingOperations.SelectedScenes();

		// Compute the number of views which are in common between all the scenes
		mergeOps.initializeViewCounts(scenesInEachView, scenes.size);

		// Merge views until views can no longer be merged
		while (mergeOps.selectScenesToMerge(selected)) {
			SceneWorkingGraph src = scenes.get(selected.sceneA);
			SceneWorkingGraph dst = scenes.get(selected.sceneB);

			// See if it needs to swap src and dst for merging
			if (!mergeOps.decideFirstIntoSecond(src, dst)) {
				SceneWorkingGraph tmp = dst;
				dst = src;
				src = tmp;
			}

			// Don't merge in this situation
			if (isSubset(src, dst)) {
				if (verbose != null)
					verbose.println("Merge results: src=" + src.index + " dst=" + dst.index + " sizes=(" +
							src.listViews.size() + " " + dst.listViews.size() + "), Removing: src is a subset.");
				mergeOps.toggleViewEnabled(src, scenesInEachView);
				BoofMiscOps.checkTrue(!mergeOps.enabledScenes.get(src.index), "Should be disabled now");
				BoofMiscOps.checkTrue(mergeOps.enabledScenes.get(dst.index), "Should be enabled now");
				continue;
			}

			if (verbose != null)
				verbose.println("Attempt merge: src=" + src.index + " dst=" + dst.index + " sizes=(" +
						src.listViews.size() + " " + dst.listViews.size() + ")");


			// Merge the two scenes together
			int sizeBefore = dst.listViews.size();
			if (!mergeScenes.merge(dbSimilar, src, dst)) {
				if (verbose != null)
					verbose.println("FAILED: Merged blocked until scenes modified. src=" + src.index + " dst=" + dst.index);
				// Mark merging these two scenes are impossible until one of them is modified
				mergeOps.markAsFailed(src, dst);
				continue;
			}

			if (verbose != null) verbose.println("Success merging. dst.size=" + dst.listViews.size());

			// Remove both views from the counts for now
			mergeOps.toggleViewEnabled(src, scenesInEachView);
			mergeOps.toggleViewEnabled(dst, scenesInEachView);

			// Mark all the newly added views and being inside of dst
			for (int i = sizeBefore; i < dst.listViews.size(); i++) {
				SceneWorkingGraph.View wview = dst.listViews.get(i);
				scenesInEachView.getView(wview.pview).viewedBy.add(dst.index);
				scenesInEachView.getView(wview.pview).viewedBy.sort();// TODO use sorter
			}

			// Add the dst scene back in
			mergeOps.toggleViewEnabled(dst, scenesInEachView);
			BoofMiscOps.checkTrue(!mergeOps.enabledScenes.get(src.index), "Should be disabled now");
			BoofMiscOps.checkTrue(mergeOps.enabledScenes.get(dst.index), "Should be enabled now");

			// Uncomment to run sanity checks on the validity of each of data structures
//			sanityCheckScenesInEachView();
//			mergeOps.sanityCheckTable(scenes.toList());
		}
	}

	/**
	 * Checks to see if 'src' is entirely contained inside of 'dst'
	 */
	private boolean isSubset( SceneWorkingGraph src, SceneWorkingGraph dst ) {
		boolean subset = true;
		for (int i = 0; i < src.listViews.size(); i++) {
			if (!dst.views.containsKey(src.listViews.get(i).pview.id)) {
				subset = false;
				break;
			}
		}
		return subset;
	}

	/**
	 * If a scene has been merged into another one remove it from the list of scenes
	 */
	private void removeMergedScenes() {
		// remove scenes that got merged into others. This is output to the user
		for (int i = scenes.size - 1; i >= 0; i--) {
			if (mergeOps.enabledScenes.get(scenes.get(i).index))
				continue;
			scenes.removeSwap(i);
		}

		if (verbose != null) {
			verbose.println("scenes.size=" + scenes.size);
			for (int i = 0; i < scenes.size; i++) {
				verbose.println("_ scene[" + i + "].size = " + scenes.get(i).listViews.size());
			}
		}
	}

	/**
	 * Expands the scene to include the specified view
	 *
	 * @return true if it could expand into the view and updated the scene. False if it failed
	 */
	boolean expandIntoView( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCam,
							SceneWorkingGraph scene, PairwiseImageGraph.View selected ) {
		// TODO if it fails to expand into a view put it into a list to consider again later if another view is
		//      updated to metric and connected to this view.
		if (!expandMetric.process(dbSimilar, dbCam, scene, selected)) {
			if (verbose != null)
				verbose.println("FAILED: Expand/add scene=" + scene.index + " view='" + selected.id + "'. Discarding.");
			return false;
		}

		// Compute the score for view's inliers
		SceneWorkingGraph.View wview = scene.lookupView(selected.id);
		SceneWorkingGraph.InlierInfo inlier = wview.inliers.getTail();
		inlier.scoreGeometric = computeGeometricScore(scene, inlier);

		// TODO consider local refinement while expanding to help mitigate the unbounded growth in errors

		int openSizePrior = scene.open.size;

		// Examine other scenes which contains this view when deciding if we should continue to expand from here
		if (canSpawnFromView(scene, wview.pview))
			addOpenForView(scene, wview.pview);

		if (verbose != null) {
			verbose.println("_ Expanded scene=" + scene.index + " view='" + selected.id + "'  inliers=" +
					utils.inliersThreeView.size() + "/" + utils.matchesTriple.size + " Open added.size=" +
					(scene.open.size - openSizePrior));
		}

		// Add this view to the list
		scenesInEachView.getView(selected).viewedBy.add(scene.index);

		return true;
	}

	/**
	 * Makes sure the number of scenes that have each view is correctly counted. Used for debugging.
	 */
	protected void sanityCheckScenesInEachView() {
		List<SceneWorkingGraph> expected = new ArrayList<>();
		for (int viewIdx = 0; viewIdx < scenesInEachView.views.size; viewIdx++) {
			int _viewIdx = viewIdx;

			// Find all scenes with this view
			expected.clear();
			scenes.forEach(scene -> {
				for (int i = 0; i < scene.listViews.size(); i++) {
					if (scene.listViews.get(i).pview.index == _viewIdx) {
						expected.add(scene);
						break;
					}
				}
			});

			ViewScenes found = scenesInEachView.views.get(viewIdx);
			BoofMiscOps.checkEq(expected.size(), found.viewedBy.size, "Number of scenes do not match. view=" +
					viewIdx);

			for (int i = 0; i < expected.size(); i++) {
				if (!found.viewedBy.contains(expected.get(i).index)) {
					throw new RuntimeException("Scene is missing from nodeViews. scene.index=" + expected.get(i).index);
				}
			}
		}
	}

	/**
	 * Estimates the quality of the geometry information contained in the inlier set. Higher values are better.
	 */
	public double computeGeometricScore( SceneWorkingGraph scene, SceneWorkingGraph.InlierInfo inlier ) {
		return inlier.getInlierCount();
	}

	/**
	 * Returns the largest scene. Throws an exception if there is no valid scene
	 */
	public SceneWorkingGraph getLargestScene() {
		if (scenes.isEmpty())
			throw new IllegalArgumentException("There are no valid scenes");

		SceneWorkingGraph best = scenes.get(0);
		for (int i = 1; i < scenes.size; i++) {
			SceneWorkingGraph scene = scenes.get(i);
			if (scene.listViews.size() > best.listViews.size()) {
				best = scene;
			}
		}

		return best;
	}

	/**
	 * Contains information about which scenes contain this specific view
	 */
	@SuppressWarnings("NullAway.Init")
	public static class ViewScenes {
		/** String ID if the view in the pairwise graph */
		public String id;
		/** Indexes of scenes that contain this view */
		public DogArray_I32 viewedBy = new DogArray_I32();

		@SuppressWarnings("NullAway")
		public void reset() {
			id = null;
			viewedBy.reset();
		}
	}

	/**
	 * Records which scenes have grown to include which views. There is a one-to-one correspondence between
	 * elements here and in the pairwise graph.
	 *
	 * The main reason this class was created was to ensure the correct array index was used to access the view
	 * information.
	 */
	public static class PairwiseViewScenes {
		/** Which scenes are include which views */
		public final DogArray<ViewScenes> views = new DogArray<>(ViewScenes::new, ViewScenes::reset);

		public void initialize( PairwiseImageGraph pairwise ) {
			views.reset();
			views.resize(pairwise.nodes.size, ( idx, o ) -> o.id = pairwise.nodes.get(idx).id);
		}

		public ViewScenes getView( PairwiseImageGraph.View view ) {
			return views.get(view.index);
		}
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(verbose, configuration,
				spawnScene, expandMetric, refineWorking, mergeOps, mergeScenes);
	}
}
