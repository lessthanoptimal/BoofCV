/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.structure2;

import boofcv.abst.geo.selfcalib.ProjectiveToMetricCameras;
import boofcv.alg.geo.MetricCameras;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.factory.geo.ConfigSelfCalibDualQuadratic;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTupleDN;
import boofcv.struct.image.ImageDimension;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static boofcv.misc.BoofMiscOps.assertBoof;
import static boofcv.misc.BoofMiscOps.assertEq;

/**
 * Fully computes views (intrinsics + SE3) for each view and saves which observations were inliers. This should
 * be considered a first pass and all optimization is done at a local level.
 *
 * <ol>
 * <li>Input: {@link PairwiseImageGraph2} and {@link LookupSimilarImages image information}</li>
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
	private @Getter @Setter	ProjectiveToMetricCameras projectiveToMetric =
			FactoryMultiView.projectiveToMetric((ConfigSelfCalibDualQuadratic)null);

	// Uses known metric views to expand the metric reconstruction by one view
	private final MetricExpandByOneView expandMetric = new MetricExpandByOneView();

	public MetricFromUncalibratedPairwiseGraph(PairwiseGraphUtils utils) {
		super(utils);
		initProjective = new ProjectiveInitializeAllCommon();
		initProjective.utils = utils;
		expandMetric.utils = utils;
	}

	public MetricFromUncalibratedPairwiseGraph(ConfigProjectiveReconstruction config) {
		this(new PairwiseGraphUtils(config));
	}

	public MetricFromUncalibratedPairwiseGraph() {
		this(new ConfigProjectiveReconstruction());
	}

	/**
	 * Performs a projective reconstruction of the scene from the views contained in the graph
	 * @param db (input) Contains information on each image
	 * @param graph (input) Relationship between the images
	 * @return true if successful or false if it failed and results can't be used
	 */
	public boolean process( LookupSimilarImages db , PairwiseImageGraph2 graph ) {
		exploredViews.clear();
		workGraph.reset();

		// Score nodes for their ability to be seeds
		Map<String, SeedInfo> mapScores = scoreNodesAsSeeds(graph);
		List<SeedInfo> seeds = selectSeeds(seedScores,mapScores);

		if( seeds.size() == 0 )
			return false;

		if( verbose != null ) verbose.println("Selected seeds.size="+seeds.size()+" seeds out of "+graph.nodes.size+" nodes");

		// For now we are keeping this very simple. Only a single seed is considered
		SeedInfo info = seeds.get(0);

		// Find the common features
		GrowQueue_I32 common = utils.findCommonFeatures(info.seed,info.motions);
		if( common.size < 6 ) // if less than the minimum it will fail
			return false;

		if( verbose != null ) verbose.println("Selected seed.id='"+info.seed.id+"' common="+common.size);

		if (!estimateProjectiveSceneFromSeed(db, info, common))
			return false;

		// Elevate initial seed to metric
		if (!projectiveSeedToMetric(graph))
			return false;

		// Expand to the remaining views one at a time
		expandMetricScene(db);

		if( verbose != null ) verbose.println("Done");
		return true;
	}

	/**
	 * Initializes the scene at the seed view
	 */
	private boolean estimateProjectiveSceneFromSeed(LookupSimilarImages db, SeedInfo info, GrowQueue_I32 common) {
		// initialize projective scene using common tracks
		if( !initProjective.projectiveSceneN(db,info.seed,common,info.motions) ) {
			if( verbose != null ) verbose.println("Failed initialize seed");
			return false;
		}

		// Save found camera matrices for each view it was estimated in
		if( verbose != null ) verbose.println("Saving initial seed camera matrices");
		for (int structViewIdx = 0; structViewIdx < initProjective.utils.structure.views.size; structViewIdx++) {
			View view = initProjective.getPairwiseGraphViewByStructureIndex(structViewIdx);
			if( verbose != null ) verbose.println("  view.id=`"+view.id+"`");
		}

		return true;
	}

	/**
	 * Elevate the initial projective scene into a metric scene.
	 */
	private boolean projectiveSeedToMetric(PairwiseImageGraph2 graph) {
		// Declare storage for projective scene in a format that 'projectiveToMetric' understands
		List<String> viewIds = new ArrayList<>();
		FastQueue<ImageDimension> dimensions = new FastQueue<>(ImageDimension::new);
		FastQueue<DMatrixRMaj> views = new FastQueue<>(()->new DMatrixRMaj(3,4));
		FastQueue<AssociatedTupleDN> observations = new FastQueue<>(AssociatedTupleDN::new);

		initProjective.lookupInfoForMetricElevation(viewIds,dimensions,views,observations);

		// Pass the projective scene and elevate into a metric scene
		MetricCameras results = new MetricCameras();
		if( !projectiveToMetric.process(dimensions.toList(),views.toList(),(List)observations.toList(),results) ) {
			if( verbose != null ) verbose.println("Failed to elevate initial seed to metric");
			return false;
		}

		// Save the results to the working grpah
		saveMetricSeed(graph,viewIds,initProjective.getInlierToSeed(), initProjective.getInlierIndexes(), results);

		return true;
	}

	/**
	 * Saves the elevated metric results to the {@link #workGraph}.
	 *
	 * @param inlierToSeed Indexes of observations which are part of the inlier set
	 * @param inlierToOther Indexes of observations for all the other views which are part of the inlier set.
	 */
	void saveMetricSeed(PairwiseImageGraph2 graph, List<String> viewIds,
						GrowQueue_I32 inlierToSeed,
						FastQueue<GrowQueue_I32> inlierToOther,
						MetricCameras results) {
		assertEq(viewIds.size(),results.motion_1_to_k.size+1,"Implicit view[0] no included");

		// Save the metric views
		for (int i = 0; i < viewIds.size(); i++) {
			PairwiseImageGraph2.View pview = graph.lookupNode(viewIds.get(i));
			SceneWorkingGraph.View wview = workGraph.addView(pview);
			if( i > 0 )
				wview.world_to_view.set(results.motion_1_to_k.get(i-1));
			wview.pinhole.set(results.intrinsics.get(i));
		}

		// Save the inliers used to construct the metric scene
		SceneWorkingGraph.View wtarget = workGraph.lookupView(viewIds.get(0));
		assertEq(wtarget.inliers.views.size,0,"There should be at most one set of inliers per view");
		SceneWorkingGraph.InlierInfo inliers = wtarget.inliers;
		inliers.views.resize(viewIds.size());
		inliers.observations.resize(viewIds.size());
		for (int viewIdx = 0; viewIdx < viewIds.size(); viewIdx++) {
			inliers.views.set(viewIdx,graph.lookupNode(viewIds.get(viewIdx)));
			if( viewIdx == 0 ) {
				inliers.observations.get(0).setTo(inlierToSeed);
				assertBoof(inliers.observations.get(0).size>0,"There should be observations");
				continue;
			}
			inliers.observations.get(viewIdx).setTo(inlierToOther.get(viewIdx-1));
			assertEq(inliers.observations.get(viewIdx).size,inliers.observations.get(0).size,
					"Each view should have the same number of observations");
		}
	}

	/**
	 * Adds all the remaining views to the scene one at a time. Known metric views are used to seed an upgrade
	 * of each new view. If a metric upgrade fails that view is discarded from the metric scene.
	 */
	private void expandMetricScene(LookupSimilarImages db) {
		if( verbose != null ) verbose.println("ENTER expandMetricScene()");
		// Mark known views as well known
		workGraph.viewList.forEach(v->exploredViews.add(v.pview.id));

		// Create a list of views that can be added the work graph
		FastArray<View> open = findAllOpenViews();

		while( open.size > 0 ) {
			View selected = selectNextToProcess(open);
			if( selected == null ) {
				if( verbose != null ) verbose.println("  No valid views left. open.size="+open.size);
				break;
			}

			if(!expandMetric.process(db,workGraph,selected)) {
				if( verbose != null ) verbose.println("  Failed to expand/add view='"+selected.id+"'. Discarding.");
				continue;
			}
			if( verbose != null ) {
				verbose.println("    Expanded view='" + selected.id + "'  inliers="
						+ utils.inliersThreeView.size() + " / " + utils.matchesTriple.size);
			}

			// Saves the set of inliers used to estimate this views metric view for later use
			SceneWorkingGraph.View wview = workGraph.lookupView(selected.id);
			utils.saveRansacInliers(wview);

			// These fundamental assumptions are being made
			// TODO move to expandMetric and include in esetimation of calibrating homography for better results
			wview.pinhole.cx = wview.pinhole.cy = wview.pinhole.skew = 0.0;

			// Add neighboring views which have yet to be added to the open list
			addOpenForView(wview.pview, open);
		}
		if( verbose != null ) verbose.println("EXIT expandMetricScene()");
	}
}
