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

import boofcv.abst.geo.selfcalib.ProjectiveToMetricCameras;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.geo.bundle.BundleAdjustmentOps;
import boofcv.alg.structure.SceneMergingOperations.SelectedViews;
import boofcv.factory.geo.ConfigSelfCalibDualQuadratic;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedTupleDN;
import boofcv.struct.image.ImageDimension;
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
import java.util.Map;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkEq;

/**
 * Fully computes views (intrinsics + SE3) for each view and saves which observations were inliers. This should
 * be considered a first pass and all optimization is done at a local level.
 *
 * <ol>
 * <li>Input: {@link PairwiseImageGraph} and {@link LookUpSimilarImages image information}</li>
 * <li>Selected a set of views to estimate a projective scene based on having good geometry.
 * {@link ProjectiveInitializeAllCommon}</li>
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

	/** Computes the initial scene from the seed and some of it's neighbors */
	private final @Getter ProjectiveInitializeAllCommon initProjective;

	/** Used elevate the projective scene into a metric scene */
	private @Getter @Setter ProjectiveToMetricCameras projectiveToMetric =
			FactoryMultiView.projectiveToMetric((ConfigSelfCalibDualQuadratic)null);

	// Uses known metric views to expand the metric reconstruction by one view
	private final @Getter MetricExpandByOneView expandMetric = new MetricExpandByOneView();

	private final @Getter RefineMetricWorkingGraph refineWorking = new RefineMetricWorkingGraph();

	/** If true it will apply sanity checks on results for debugging. This could be expensive */
	public boolean sanityChecks = false;

	/** List of all the scenes. There can be multiple at the end if not everything is connected */
	final @Getter DogArray<SceneWorkingGraph> scenes =
			new DogArray<>(SceneWorkingGraph::new, SceneWorkingGraph::reset);

	SceneMergingOperations mergeOps = new SceneMergingOperations();

	/** Which scenes are include which views */
	PairwiseViewScenes nodeViews = new PairwiseViewScenes();

	MetricSanityChecks metricChecks = new MetricSanityChecks();

	// Storage for selected views to estimate the transform between the two scenes
	SelectedViews selectedViews = new SelectedViews();

	public MetricFromUncalibratedPairwiseGraph( PairwiseGraphUtils utils ) {
		super(utils);
		initProjective = new ProjectiveInitializeAllCommon();
		initProjective.utils = utils;
		expandMetric.utils = utils;
	}

	public MetricFromUncalibratedPairwiseGraph( ConfigProjectiveReconstruction config ) {
		this(new PairwiseGraphUtils(config));
	}

	public MetricFromUncalibratedPairwiseGraph() {
		this(new ConfigProjectiveReconstruction());
	}

	{
		// prune outlier observations and run SBA a second time
		refineWorking.bundleAdjustment.keepFraction = 0.95;
	}

	/**
	 * Performs a projective reconstruction of the scene from the views contained in the graph
	 *
	 * @param db (input) Contains information on each image
	 * @param pairwise (input) Relationship between the images
	 * @return true if successful or false if it failed and results can't be used
	 */
	public boolean process( LookUpSimilarImages db, PairwiseImageGraph pairwise ) {
		scenes.reset();

		// Score nodes for their ability to be seeds
		Map<String, SeedInfo> mapScores = scoreNodesAsSeeds(pairwise);
		List<SeedInfo> seeds = selectSeeds(seedScores, mapScores);
		// TODO also take in account distance from other seeds when selecting

		if (seeds.isEmpty()) {
			if (verbose != null) verbose.println("No valid seeds found.");
			return false;
		}

		if (verbose != null)
			verbose.println("Selected seeds.size=" + seeds.size() + " seeds out of " + pairwise.nodes.size + " nodes");

		// Declare storage for book keeping at each view
		nodeViews.initialize(pairwise);

		// Spawn as many seeds as possible
		spawnSeeds(db, pairwise, seeds);

		if (scenes.isEmpty()) {
			if (verbose != null) verbose.println("Failed to upgrade any of the seeds to a metric scene.");
			return false;
		}

		// Expand all the scenes until they can't any more
		expandScenes(db);
		// TODO while expanding perform local SBA

		// Merge scenes together until there are no more scenes which can be merged
		mergeScenes(db);
		// TODO local SBA with fixed parameters in master when merging

		// There can be multiple scenes at the end that are disconnected and share no views in common

		if (verbose != null) verbose.println("Done.");
		return true;
	}

	/**
	 * Create a new scene for every seed that it can successfully create a metric estimate from. By using multiple
	 * seeds there is less change for a single point of failure.
	 */
	private void spawnSeeds( LookUpSimilarImages db, PairwiseImageGraph pairwise, List<SeedInfo> seeds ) {
		if (verbose != null) verbose.println("ENTER spawn seeds.size=" + seeds.size());

		var commonPairwise = new DogArray_I32();

		for (int seedIdx = 0; seedIdx < seeds.size(); seedIdx++) {
			SeedInfo info = seeds.get(seedIdx);
			if (verbose != null)
				verbose.println("Spawn index=" + seedIdx + "  count " + (seedIdx + 1) + " / " + seeds.size());

			// TODO reject a seed if it's too similar to other seeds? Should that be done earlier?

			// Find the common features
			utils.findAllConnectedSeed(info.seed, info.motions, commonPairwise);
			if (commonPairwise.size < 6) {// if less than the minimum it will fail
				if (verbose != null) verbose.println("  FAILED: Too few common features. seed.id=" + info.seed.id);
				continue;
			}

			if (verbose != null)
				verbose.println("  Selected seed.id='" + info.seed.id + "' common=" + commonPairwise.size);

			if (!estimateProjectiveSceneFromSeed(db, info, commonPairwise)) {
				if (verbose != null) verbose.println("  FAILED: Projective estimate. seed.id='" + info.seed.id + "'");
				continue;
			}

			// Create a new scene
			SceneWorkingGraph scene = scenes.grow();
			scene.index = scenes.size - 1;

			// Elevate initial seed to metric
			if (!projectiveSeedToMetric(pairwise, scene)) {
				if (verbose != null) verbose.println("  FAILED: Projective to metric. seed.id='" + info.seed.id + "'");
				// reclaim the failed graph
				scenes.removeTail();
				continue;
			}

			// Refine initial estimate
			refineWorking.process(db, scene);

			if (sanityChecks)
				metricChecks.inlierTriangulatePositiveDepth(0.1, db, scene, info.seed.id);

			if (verbose != null)
				verbose.println("  scene.index=" + scene.index + " views.size=" + scene.listViews.size());

			// Add this scene to each node so that we know they are connected
			for (int i = 0; i < scene.listViews.size(); i++) {
				SceneWorkingGraph.View wview = scene.listViews.get(i);
				PairwiseImageGraph.View pview = wview.pview;
				nodeViews.getView(pview).viewedBy.add(scene.index);

				if (verbose != null)
					verbose.println("  view['" + pview.id + "']  intrinsic.f=" + wview.intrinsic.f + "  view.index=" + pview.index);
			}
		}
		if (verbose != null) verbose.println("EXIT spawn seeds: scenes.size=" + scenes.size());
	}

	/**
	 * Expand the scenes until there are no more views they can be expanded into. A scene can expand into
	 * a view if it's connected to a view which already belongs to the scene and at least one of those
	 * connected views does not belong to any other scenes.
	 */
	void expandScenes( LookUpSimilarImages db ) {
		if (verbose != null) verbose.println("Expand Scenes: Finding open views in each scene");

		// Initialize the expansion by finding all the views each scene could expand into
		for (int sceneIdx = 0; sceneIdx < scenes.size; sceneIdx++) {
			if (verbose != null) verbose.println("scene.index=" + sceneIdx);
			SceneWorkingGraph scene = scenes.get(sceneIdx);

			// Mark views which were learned in the spawn as known
			scene.listViews.forEach(wv -> scene.exploredViews.add(wv.pview.id));

			// Add views which can be expanded into
			findAllOpenViews(scene);

			if (verbose != null) verbose.println("scene[" + sceneIdx + "].open.size=" + scene.open.size);
		}

		// Workspace for selecting which scene and view to expand into
		Expansion best = new Expansion();
		Expansion candidate = new Expansion();

		// Loop until it can't expand any more
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
					if (verbose != null) verbose.println("  No valid views left. open.size=" + scene.open.size);
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
				verbose.println("  Expanding scene=" + best.scene.index + " view='" + view.id + "' score=" + best.score);

			expandIntoView(db, best.scene, view);
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
		if (nodeViews.getView(pview).viewedBy.size == 0)
			return true;

		// If another scene also occupies the view then we can only expand from it if it is part of the seed set
		// The idea is that the seed set could have produced a bad reconstruction that needs to be jumped over
		ViewScenes views = nodeViews.getView(pview);
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

	/**
	 * Merge the different scenes together if they share common views
	 */
	void mergeScenes( LookUpSimilarImages db ) {
		if (verbose != null) verbose.println("Merging Scenes. scenes.size=" + scenes.size);

		SceneMergingOperations.SelectedScenes selected = new SceneMergingOperations.SelectedScenes();
		ScaleSe3_F64 src_to_dst = new ScaleSe3_F64();

		// Compute the number of views which are in common between all the scenes
		mergeOps.countCommonViews(nodeViews, scenes.size);

		// Merge views until views can no longer be merged
		while (mergeOps.selectViewsToMerge(selected)) {
			SceneWorkingGraph src = scenes.get(selected.sceneA);
			SceneWorkingGraph dst = scenes.get(selected.sceneB);

			// See if it needs to swap src and dst for merging
			if (!mergeOps.decideFirstIntoSecond(src, dst)) {
				SceneWorkingGraph tmp = dst;
				dst = src;
				src = tmp;
			}

			// TODO break this up into multiple functions
			// See if the src id contained entirely in the dst
			boolean subset = true;
			for (int i = 0; i < src.listViews.size(); i++) {
				if (!dst.views.containsKey(src.listViews.get(i).pview.id)) {
					subset = false;
					break;
				}
			}

			// Don't merge in this situation
			if (subset) {
				if (verbose != null)
					verbose.println("scenes: src=" + src.index + " dst=" + dst.index + " Removing: src is a subset.");
				mergeOps.adjustSceneCounts(src, nodeViews, false);
				mergeOps.adjustSceneCounts(dst, nodeViews, false);
				SceneMergingOperations.removeScene(src, nodeViews);
				mergeOps.adjustSceneCounts(dst, nodeViews, true);
				continue;
			}

			// Remove both views from the counts for now
			mergeOps.adjustSceneCounts(src, nodeViews, false);
			mergeOps.adjustSceneCounts(dst, nodeViews, false);

			// Select which view pair to determine the relationship between the scenes from
			if (!mergeOps.selectViewsToEstimateTransform(src, dst, selectedViews))
				throw new RuntimeException("Merge failed. Unable to selected a view pair");

			// Estimate the transform from the pair
			if (!mergeOps.computeSceneTransform(db, src, dst, selectedViews.src, selectedViews.dst, src_to_dst))
				throw new RuntimeException("Merge failed. Unable to determine transform");

			int dstViewCountBefore = dst.listViews.size();

			// Merge the views
			mergeOps.mergeViews(src, dst, src_to_dst);

			// Check the views which were modified for geometric consistency to catch bugs in the code
			if (sanityChecks) {
				for (int i = 0; i < mergeOps.modifiedViews.size(); i++) {
					SceneWorkingGraph.View wview = mergeOps.modifiedViews.get(i);
					metricChecks.inlierTriangulatePositiveDepth(0.1, db, dst, wview.pview.id);
				}
			}

			if (verbose != null)
				verbose.println("scenes: src=" + src.index + " dst=" + dst.index +
						" views: (" + src.listViews.size() + " , " + dstViewCountBefore +
						") -> " + dst.listViews.size() + ", scale=" + src_to_dst.scale);

			// Remove the one that's no longer needed
			SceneMergingOperations.removeScene(src, nodeViews);

			// Update the scene counts with the new combined scene
			mergeOps.adjustSceneCounts(dst, nodeViews, true);
		}

		// remove scenes that got merged into others. This is output to the user
		for (int i = scenes.size - 1; i >= 0; i--) {
			if (!scenes.get(i).listViews.isEmpty())
				continue;
			scenes.removeSwap(i);
		}

		if (verbose != null) {
			verbose.println("scenes.size=" + scenes.size);
			for (int i = 0; i < scenes.size; i++) {
				verbose.println("  scene[" + i + "].size = " + scenes.get(i).listViews.size());
			}
		}
	}

	/**
	 * Expands the scene to include the specified view
	 */
	void expandIntoView( LookUpSimilarImages db, SceneWorkingGraph scene, PairwiseImageGraph.View selected ) {
		if (!expandMetric.process(db, scene, selected)) {
			if (verbose != null)
				verbose.println("Failed to expand/add scene=" + scene.index + " view='" + selected.id + "'. Discarding.");
			return; // TODO handle this failure somehow. mark the scene as dead? Revisit it later on?
		}

		// Saves the set of inliers used to estimate this views metric view for later use
		SceneWorkingGraph.View wview = scene.lookupView(selected.id);
		SceneWorkingGraph.InlierInfo inlier = utils.saveRansacInliers(wview);
		inlier.scoreGeometric = computeGeometricScore(scene, inlier);

		// Check results for geometric consistency
		if (sanityChecks)
			metricChecks.inlierTriangulatePositiveDepth(0.1, db, scene, selected.id);

		// TODO Refining at this point is essential for long term stability but optimizing everything is not scalable
		// maybe identify views with large residuals and optimizing up to N views surrounding them to fix the issue?
//			refineWorking.process(db, workGraph);

		int openSizePrior = scene.open.size;

		// Examine other scenes which contains this view when deciding if we should continue to expand from here
		if (canSpawnFromView(scene, wview.pview))
			addOpenForView(scene, wview.pview);

		if (verbose != null) {
			verbose.println("  Expanded  scene=" + scene.index + " view='" + selected.id + "'  inliers=" +
					utils.inliersThreeView.size() + "/" + utils.matchesTriple.size + " Open added.size=" +
					(scene.open.size - openSizePrior));
		}

		// Add this view to the list
		nodeViews.getView(selected).viewedBy.add(scene.index);
	}

	/**
	 * Initializes the scene at the seed view
	 */
	private boolean estimateProjectiveSceneFromSeed( LookUpSimilarImages db, SeedInfo info, DogArray_I32 common ) {
		// initialize projective scene using common tracks
		if (!initProjective.projectiveSceneN(db, info.seed, common, info.motions)) {
			if (verbose != null) verbose.println("Failed initialize seed");
			return false;
		}

		return true;
	}

	/**
	 * Elevate the initial projective scene into a metric scene.
	 */
	private boolean projectiveSeedToMetric( PairwiseImageGraph pairwise, SceneWorkingGraph scene ) {
		// Declare storage for projective scene in a format that 'projectiveToMetric' understands
		List<String> viewIds = new ArrayList<>();
		DogArray<ImageDimension> dimensions = new DogArray<>(ImageDimension::new);
		DogArray<DMatrixRMaj> views = new DogArray<>(() -> new DMatrixRMaj(3, 4));
		DogArray<AssociatedTupleDN> observations = new DogArray<>(AssociatedTupleDN::new);

		initProjective.lookupInfoForMetricElevation(viewIds, dimensions, views, observations);

		// Pass the projective scene and elevate into a metric scene
		MetricCameras results = new MetricCameras();
		if (!projectiveToMetric.process(dimensions.toList(), views.toList(), (List)observations.toList(), results)) {
			if (verbose != null) verbose.println("  views=" + BoofMiscOps.toStringLine(viewIds));
			return false;
		}

		// Save the results to the working graph
		saveMetricSeed(pairwise, viewIds, dimensions.toList(), initProjective.getInlierIndexes(), results, scene);

		return true;
	}

	/**
	 * Saves the elevated metric results to the scene. Each view is given a copy of the inlier that has been
	 * adjusted so that it is view zero.
	 *
	 * @param viewInlierIndexes Which observations in each view are part of the inlier set
	 */
	void saveMetricSeed( PairwiseImageGraph graph, List<String> viewIds, List<ImageDimension> dimensions,
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
			SceneWorkingGraph.View wview = scene.addView(pview);
			if (i > 0)
				wview.world_to_view.setTo(results.motion_1_to_k.get(i - 1));
			BundleAdjustmentOps.convert(results.intrinsics.get(i), wview.intrinsic);
			wview.imageDimension.setTo(dimensions.get(i));
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

		// The geometric score should be the same for all views
		double scoreGeometric = computeGeometricScore(scene, scene.listViews.get(0).inliers.get(0));
		for (int viewIdx = 0; viewIdx < scene.listViews.size(); viewIdx++) {
			scene.listViews.get(viewIdx).inliers.get(0).scoreGeometric = scoreGeometric;
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
	public static class ViewScenes {
		/** String ID if the view in the pairwise graph */
		public String id;
		/** Indexes of scenes that contain this view */
		public DogArray_I32 viewedBy = new DogArray_I32();

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
				initProjective, expandMetric, refineWorking, mergeOps, metricChecks);

		if (projectiveToMetric instanceof VerbosePrint) {
			BoofMiscOps.verboseChildren(verbose, configuration, (VerbosePrint)projectiveToMetric);
		}
	}
}
