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
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.FastArray;
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
 * Output is contained in {@link SceneWorkingGraph} and accessible from {@link #getWorkGraph()}. 3D point features
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
	 * @param graph (input) Relationship between the images
	 * @return true if successful or false if it failed and results can't be used
	 */
	public boolean process( LookUpSimilarImages db, PairwiseImageGraph graph ) {
		exploredViews.clear();
		workGraph.reset();

		// Score nodes for their ability to be seeds
		Map<String, SeedInfo> mapScores = scoreNodesAsSeeds(graph);
		List<SeedInfo> seeds = selectSeeds(seedScores, mapScores);

		// Multiple Seed Approach
		// - Each seed will have it's own work graph
		// - Expand the seeds until two views in two graphs are in common. Those two views must also be neighbors
		// - Compute the common transform and find scale factor
		// - Merge the two graphs
		// Allow for multiple outputs of different graphs

		if (seeds.isEmpty()) {
			if (verbose != null) verbose.println("No valid seeds found.");
			return false;
		}

		if (verbose != null)
			verbose.println("Selected seeds.size=" + seeds.size() + " seeds out of " + graph.nodes.size + " nodes");

		// For now we are keeping this very simple. Only a single seed is considered
		SeedInfo info = seeds.get(0);

		// Find the common features
		DogArray_I32 common = utils.findCommonFeatures(info.seed, info.motions);
		if (common.size < 6) {// if less than the minimum it will fail
			if (verbose != null) verbose.println("Too few common features in seed.");
			return false;
		}

		if (verbose != null) verbose.println("Selected seed.id='" + info.seed.id + "' common=" + common.size);

		if (!estimateProjectiveSceneFromSeed(db, info, common))
			return false;

		// Elevate initial seed to metric
		if (!projectiveSeedToMetric(graph))
			return false;

		// Refine initial estimate
		refineWorking.process(db, workGraph);

		// Expand to the remaining views one at a time
		expandMetricScene(db);

		if (verbose != null) verbose.println("Done");
		return true;
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

		// Save found camera matrices for each view it was estimated in
		if (verbose != null) verbose.println("Saving initial seed camera matrices");
		for (int structViewIdx = 0; structViewIdx < initProjective.utils.structure.views.size; structViewIdx++) {
			PairwiseImageGraph.View view = initProjective.getPairwiseGraphViewByStructureIndex(structViewIdx);
			if (verbose != null) verbose.println("view.id=`" + view.id + "`");
		}

		return true;
	}

	/**
	 * Elevate the initial projective scene into a metric scene.
	 */
	private boolean projectiveSeedToMetric( PairwiseImageGraph graph ) {
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
		saveMetricSeed(graph, viewIds, dimensions.toList(),
				initProjective.getInlierToSeed(), initProjective.getInlierIndexes(), results);

		return true;
	}

	/**
	 * Saves the elevated metric results to the {@link #workGraph}.
	 *
	 * @param inlierToSeed Indexes of observations which are part of the inlier set
	 * @param inlierToOther Indexes of observations for all the other views which are part of the inlier set.
	 */
	void saveMetricSeed( PairwiseImageGraph graph, List<String> viewIds, List<ImageDimension> dimensions,
						 DogArray_I32 inlierToSeed,
						 DogArray<DogArray_I32> inlierToOther,
						 MetricCameras results ) {
		checkEq(viewIds.size(), results.motion_1_to_k.size + 1, "Implicit view[0] no included");

		// Save the metric views
		for (int i = 0; i < viewIds.size(); i++) {
			PairwiseImageGraph.View pview = graph.lookupNode(viewIds.get(i));
			SceneWorkingGraph.View wview = workGraph.addView(pview);
			if (i > 0)
				wview.world_to_view.setTo(results.motion_1_to_k.get(i - 1));
			BundleAdjustmentOps.convert(results.intrinsics.get(i), wview.intrinsic);
			wview.imageDimension.setTo(dimensions.get(i));
		}

		// Save the inliers used to construct the metric scene
		SceneWorkingGraph.View wtarget = workGraph.lookupView(viewIds.get(0));
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
	 * Adds all the remaining views to the scene one at a time. Known metric views are used to seed an upgrade
	 * of each new view. If a metric upgrade fails that view is discarded from the metric scene.
	 */
	private void expandMetricScene( LookUpSimilarImages db ) {
		if (verbose != null) verbose.println("ENTER expandMetricScene()");
		// Mark known views as well known
		workGraph.viewList.forEach(v -> exploredViews.add(v.pview.id));

		// Create a list of views that can be added the work graph
		FastArray<PairwiseImageGraph.View> open = findAllOpenViews();

		while (open.size > 0) {
			PairwiseImageGraph.View selected = selectNextToProcess(open);
			if (selected == null) {
				if (verbose != null) verbose.println("No valid views left. open.size=" + open.size);
				break;
			}

			if (!expandMetric.process(db, workGraph, selected)) {
				if (verbose != null) verbose.println("Failed to expand/add view='" + selected.id + "'. Discarding.");
				continue;
			}
			if (verbose != null) {
				verbose.println("Expanded view='" + selected.id + "'  inliers="
						+ utils.inliersThreeView.size() + " / " + utils.matchesTriple.size);
			}

			// Saves the set of inliers used to estimate this views metric view for later use
			SceneWorkingGraph.View wview = workGraph.lookupView(selected.id);
			utils.saveRansacInliers(wview);

			// TODO Refining at this point is essential for long term stability but optimizing everything is not scalable
			// maybe identify views with large residuals and optimizing up to N views surrounding them to fix the issue?
//			refineWorking.process(db, workGraph);

			// Add neighboring views which have yet to be added to the open list
			addOpenForView(wview.pview, open);
		}
		if (verbose != null) verbose.println("EXIT expandMetricScene()");
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
		BoofMiscOps.verboseChildren(out, configuration, initProjective, expandMetric, refineWorking);
	}
}
