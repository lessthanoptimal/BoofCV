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
import boofcv.factory.geo.ConfigSelfCalibDualQuadratic;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.geo.AssociatedTupleDN;
import boofcv.struct.image.ImageDimension;
import georegression.struct.se.Se3_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static boofcv.misc.BoofMiscOps.checkEq;
import static boofcv.misc.BoofMiscOps.checkTrue;

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

	/** List of all the scenes. There can be multiple at the end if not everything is connected */
	final @Getter DogArray<SceneWorkingGraph> scenes =
			new DogArray<>(SceneWorkingGraph::new, SceneWorkingGraph::reset);

	/** Which scenes are include which views */
	PairwiseViewScenes nodeViews = new PairwiseViewScenes();

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

		// Expand all the scenes until they can't any more
		expandScenes(db);
		// TODO while expanding perform local SBA

		// Merge scenes together until there are no more scenes which can be merged
		mergeScenes();
		// TODO local SBA with fixed parameters in master when merging

		// There can be multiple scenes at the end that are disconnected and share no views in common

		if (verbose != null) verbose.println("Done. scenes.size=" + scenes.size);
		return true;
	}

	/**
	 * Create a new scene for every seed that it can successfully create a metric estimate from. By using multiple
	 * seeds there is less change for a single point of failure.
	 */
	private void spawnSeeds( LookUpSimilarImages db, PairwiseImageGraph pairwise, List<SeedInfo> seeds ) {
		if (verbose != null) verbose.println("ENTER spawn seeds.size=" + seeds.size());
		for (int seedIdx = 0; seedIdx < seeds.size(); seedIdx++) {
			SeedInfo info = seeds.get(seedIdx);
			if (verbose != null) verbose.println("Spawn index="+seedIdx+"  count " + (seedIdx + 1) + " / " + seeds.size());

			// TODO reject a seed if it's too similar to other seeds? Should that be done earlier?

			// Find the common features
			DogArray_I32 common = utils.findCommonFeatures(info.seed, info.motions);
			if (common.size < 6) {// if less than the minimum it will fail
				if (verbose != null) verbose.println("  FAILED: Too few common features seed.id=" + info.seed.id);
				continue;
			}

			if (verbose != null) verbose.println("  Selected seed.id='" + info.seed.id + "' common=" + common.size);

			if (!estimateProjectiveSceneFromSeed(db, info, common)) {
				if (verbose != null) verbose.println("    FAILED: Projective estimate seed.id='" + info.seed.id);
				continue;
			}

			// Create a new scene
			SceneWorkingGraph scene = scenes.grow();
			scene.index = scenes.size - 1;

			// Elevate initial seed to metric
			if (!projectiveSeedToMetric(pairwise, scene)) {
				if (verbose != null) verbose.println("    FAILED: Projective to metric seed.id='" + info.seed.id);
				// reclaim the failed graph
				scenes.removeTail();
				continue;
			}

			// Refine initial estimate
			refineWorking.process(db, scene);

			if (verbose != null)
				verbose.println("  scene.index="+scene.index+" views.size="+scene.workingViews.size());

			// Add this scene to each node so that we know they are connected
			for (int i = 0; i < scene.workingViews.size(); i++) {
				SceneWorkingGraph.View wview = scene.workingViews.get(i);
				nodeViews.getView(wview.pview).viewedBy.add(scene.index);

				if (verbose != null)
					verbose.println("   view['" + wview.pview.id + "']  intrinsic.f=" + wview.intrinsic.f);
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
		if (verbose != null) verbose.println("Finding open views in each scene");

		// Initialize the expansion by finding all the views each scene could expand into
		for (int sceneIdx = 0; sceneIdx < scenes.size; sceneIdx++) {
			if (verbose != null) verbose.println("scene.index=" + sceneIdx);
			SceneWorkingGraph scene = scenes.get(sceneIdx);

			// Mark views which were learned in the spawn as known
			scene.workingViews.forEach(wv -> scene.exploredViews.add(wv.pview.id));

			// Add views which can be expanded into
			findAllOpenViews(scene);

			if (verbose != null) verbose.println("scene[" + sceneIdx + "].open.size=" + scene.open.size);
		}

		// Workspace for selecting which scene and view to expand into
		Expansion best = new Expansion();
		Expansion candidate = new Expansion();

		if (verbose != null) verbose.println("Expanding scenes");

		// Loop until it can't expand any more
		while (true) {
			// Clear previous best results
			best.reset();

			// Go through each scene and select the view to expand into with the best score
			for (int sceneIdx = 0; sceneIdx < scenes.size; sceneIdx++) {
				SceneWorkingGraph scene = scenes.get(sceneIdx);
				if (scene.open.isEmpty())
					continue;

				// TODO this logic should be to remove if an open view has no neighbor that isn't just
				// 	    owned by this scene

				// The logic below doesn't do what I want it to do, but does limit growth and help debug rest of the
				// code

				// Remove views from open list which can't be expanded to
				int openSizeBefore = scene.open.size;
				for (int openIdx = scene.open.size - 1; openIdx >= 0; openIdx--) {
					final PairwiseImageGraph.View pview = scene.open.get(openIdx);
					if (nodeViews.getView(pview).viewedBy.size > 1) {
						scene.open.removeSwap(openIdx);
					}
				}
				if (verbose != null)
					verbose.println("  scene[" + sceneIdx + "] removed open " + (openSizeBefore - scene.open.size));

				// TODO this can be optimized by only searching the open list for a new best when there's a change
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
				verbose.println("  expand sceneIdx=" + best.scene.index + " view='" + view.id + "' score=" + best.score);

			expandIntoView(db, best.scene, view);
		}
	}

	/**
	 * Merge the different scenes together if they share common views
	 */
	void mergeScenes() {
		if (verbose != null) verbose.println("Merging Scenes. scenes.size=" + scenes.size);

		SceneMergingOperations mergeOps = new SceneMergingOperations();
		SceneMergingOperations.SelectedScenes selected = new SceneMergingOperations.SelectedScenes();
		Se3_F64 src_to_dst = new Se3_F64();

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

			if (verbose != null) verbose.println("  scenes: src=" + src.index + " dst=" + dst.index +
					" views: (" + src.workingViews.size() + " , " + dst.workingViews.size() + ")");

			// Remove both views from the counts for now
			mergeOps.adjustSceneCounts(src, nodeViews, false);
			mergeOps.adjustSceneCounts(dst, nodeViews, false);

			// Determine how to convert the coordinate systems
			if (!mergeOps.findTransformSe3(src, dst, src_to_dst))
				throw new RuntimeException("ACK! Don't know how to merge coordinate systems. Handle this.");

			// Merge the views
			SceneMergingOperations.mergeViews(src, dst, src_to_dst);

			// Remove the one that's no longer needed
			SceneMergingOperations.removeScene(src, nodeViews);

			// Update the scene counts with the new combined scene
			mergeOps.adjustSceneCounts(dst, nodeViews, true);
		}

		// remove scenes that got merged into others
		for (int i = scenes.size - 1; i >= 0; i--) {
			if (!scenes.get(i).workingViews.isEmpty())
				continue;
			scenes.removeSwap(i);
		}
	}

	/**
	 * Expands the scene to include the specified view
	 */
	void expandIntoView( LookUpSimilarImages db, SceneWorkingGraph scene, PairwiseImageGraph.View selected ) {
		if (!expandMetric.process(db, scene, selected)) {
			if (verbose != null) verbose.println("  Failed to expand/add view='" + selected.id + "'. Discarding.");
			return; // TODO handle this failure somehow. mark the scene as dead?
		}
		if (verbose != null) {
			verbose.println("    Expanded view='" + selected.id + "'  inliers="
					+ utils.inliersThreeView.size() + " / " + utils.matchesTriple.size);
		}

		// Saves the set of inliers used to estimate this views metric view for later use
		SceneWorkingGraph.View wview = scene.lookupView(selected.id);
		utils.saveRansacInliers(wview);

		// TODO Refining at this point is essential for long term stability but optimizing everything is not scalable
		// maybe identify views with large residuals and optimizing up to N views surrounding them to fix the issue?
//			refineWorking.process(db, workGraph);

		int openSizePrior = scene.open.size;
		// Add neighboring views which have yet to be added to the open list
		addOpenForView(scene, wview.pview);

		// If the view is already part of another scene, do not allow this scene to expand into views that already
		// already occupied. This is a way to prevent all scenes from consuming all views.
		if (nodeViews.getView(wview.pview).viewedBy.size == 0) {
			for (int openIdx = scene.open.size - 1; openIdx >= openSizePrior; openIdx--) {
				PairwiseImageGraph.View pview = scene.open.get(openIdx);
				if (!nodeViews.getView(pview).viewedBy.isEmpty())
					continue;
				scene.open.removeSwap(openIdx);
			}
		}

		// Add this view to the list
		nodeViews.getView(wview.pview).viewedBy.add(scene.index);
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
			if (verbose != null) verbose.println("Failed to elevate initial seed to metric");
			return false;
		}

		// Save the results to the working graph
		saveMetricSeed(pairwise, viewIds, dimensions.toList(),
				initProjective.getInlierToSeed(), initProjective.getInlierIndexes(), results, scene);

		return true;
	}

	/**
	 * Saves the elevated metric results to the scene.
	 *
	 * @param inlierToSeed Indexes of observations which are part of the inlier set
	 * @param inlierToOther Indexes of observations for all the other views which are part of the inlier set.
	 */
	void saveMetricSeed( PairwiseImageGraph graph, List<String> viewIds, List<ImageDimension> dimensions,
						 DogArray_I32 inlierToSeed,
						 DogArray<DogArray_I32> inlierToOther,
						 MetricCameras results,
						 SceneWorkingGraph scene ) {
		checkEq(viewIds.size(), results.motion_1_to_k.size + 1, "Implicit view[0] no included");

		// Save the metric views
		for (int i = 0; i < viewIds.size(); i++) {
			PairwiseImageGraph.View pview = graph.lookupNode(viewIds.get(i));
			SceneWorkingGraph.View wview = scene.addView(pview);
			if (i > 0)
				wview.world_to_view.setTo(results.motion_1_to_k.get(i - 1));
			BundleAdjustmentOps.convert(results.intrinsics.get(i), wview.intrinsic);
			wview.imageDimension.setTo(dimensions.get(i));
		}

		// Save the inliers used to construct the metric scene
		SceneWorkingGraph.View wtarget = scene.lookupView(viewIds.get(0));
		checkEq(wtarget.inliers.views.size, 0, "There should be at most one set of inliers per view");
		SceneWorkingGraph.InlierInfo inliers = wtarget.inliers;
		inliers.views.resize(viewIds.size());
		inliers.observations.resize(viewIds.size());
		for (int viewIdx = 0; viewIdx < viewIds.size(); viewIdx++) {
			inliers.views.set(viewIdx, graph.lookupNode(viewIds.get(viewIdx)));
			if (viewIdx == 0) {
				inliers.observations.get(0).setTo(inlierToSeed);
				checkTrue(inliers.observations.get(0).size > 0, "There should be observations");
				continue;
			}
			inliers.observations.get(viewIdx).setTo(inlierToOther.get(viewIdx - 1));
			checkEq(inliers.observations.get(viewIdx).size, inliers.observations.get(0).size,
					"Each view should have the same number of observations");
		}
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
			if (scene.workingViews.size() > best.workingViews.size()) {
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
		BoofMiscOps.verboseChildren(out, configuration, initProjective, expandMetric, refineWorking);
	}
}
