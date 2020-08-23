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

import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import lombok.Getter;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.data.DMatrixRMaj;

import java.util.List;
import java.util.Map;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * Given a {@link PairwiseImageGraph2} that describes how a set of images are related to each other based on point
 * features, compute a projective reconstruction of the camera matrices for each view. The reconstructed location
 * of scene points are not saved.
 *
 * Summary of approach:
 * <ol>
 *     <li>Input: {@link PairwiseImageGraph2}</li>
 *     <li>Select images/views to act as seeds</li>
 *     <li>Pick the first seed and perform initial reconstruction from its neighbors and common features</li>
 *     <li>For each remaining unknown view with a 3D relationship to a known view, find its camera matrix</li>
 *     <li>Stop when no more valid views can be found</li>
 * </ol>
 *
 * In the future multiple seeds will be used to reduce the amount of error which accumulates as the scene spreads out
 * from its initial location
 *
 * <p>Output: {@link #getWorkGraph()}</p>
 *
 * <p>WARNING: There are serious issues with N-view projective scenes. See {@link ProjectiveToMetricReconstruction}
 * for a brief summary of the problems.</p>
 * <p>NOTE: One possible way (not tested) to mitigate those issues would be to scale pixels using a 3x3 matrix
 * that essentially resembles the inverse of an intrinsic matrix. At that point you might as well do a metric
 * reconstruction.</p>
 *
 * @see ProjectiveInitializeAllCommon
 * @see ProjectiveExpandByOneView
 * @see PairwiseGraphUtils
 *
 * @author Peter Abeles
 */
public class ProjectiveReconstructionFromPairwiseGraph extends ReconstructionFromPairwiseGraph {

	/** Computes the initial scene from the seed and some of it's neighbors */
	private final @Getter ProjectiveInitializeAllCommon initProjective;
	/** Adds a new view to an existing projective scene */
	private final @Getter ProjectiveExpandByOneView expandProjective;

	public ProjectiveReconstructionFromPairwiseGraph(PairwiseGraphUtils utils) {
		super(utils);
		initProjective = new ProjectiveInitializeAllCommon();
		initProjective.utils = utils;
		expandProjective = new ProjectiveExpandByOneView();
		expandProjective.utils = utils;
	}

	public ProjectiveReconstructionFromPairwiseGraph(ConfigProjectiveReconstruction config) {
		this(new PairwiseGraphUtils(config));
	}

	public ProjectiveReconstructionFromPairwiseGraph() {
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

		if( verbose != null ) verbose.println("Selected "+seeds.size()+" seeds out of "+graph.nodes.size+" nodes");

		// For now we are keeping this very simple. Only a single seed is considered
		SeedInfo info = seeds.get(0);

		// TODO redo every component to use shifted pixels
		// TODO redo every component to use scaled pixels

		// Find the common features
		GrowQueue_I32 common = utils.findCommonFeatures(info.seed,info.motions);
		if( common.size < 6 ) // if less than the minimum it will fail
			return false;

		if( verbose != null ) verbose.println("Selected seed.id="+info.seed.id+" common="+common.size);

		// TODO build up a scene so that SBA can be run on the whole thing
		if (!estimateInitialSceneFromSeed(db, info, common))
			return false;

		// NOTE: Computing H to scale camera matrices didn't prevent them from vanishing

		expandScene(db);

		// TODO compute features across all views for SBA
		// NOTE: Could do one last bundle adjustment on the entire scene. not doing that here since it would
		//       be a pain to code up since features need to be tracked across all the images and triangulated
		// TODO Note that the scene should be properly scale first if this is done.

		if( verbose != null ) verbose.println("Done");
		return true;
	}

	/**
	 * Initializes the scene at the seed view
	 */
	private boolean estimateInitialSceneFromSeed(LookupSimilarImages db, SeedInfo info, GrowQueue_I32 common) {
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
			DMatrixRMaj cameraMatrix = initProjective.utils.structure.views.get(structViewIdx).worldToView;
			workGraph.addView(view).projective.set(cameraMatrix);
			exploredViews.add(view.id);
		}

		// save which features were used for later use in metric reconstruction
		utils.saveRansacInliers(workGraph.lookupView(utils.seed.id));

		return true;
	}

	/**
	 * Adds all the remaining views to the scene
	 */
	private void expandScene(LookupSimilarImages db) {
		if( verbose != null ) verbose.println("ENTER Expanding Scene:");
		// Create a list of views that can be added the work graph
		FastArray<View> open = findAllOpenViews();

		// Grow the projective scene until there are no more views to process
		DMatrixRMaj cameraMatrix = new DMatrixRMaj(3,4);
		while( open.size > 0 ) {
			View selected = selectNextToProcess(open);
			if( selected == null ) {
				if( verbose != null ) verbose.println("  No valid views left. open.size="+open.size);
				break;
			}

			if(!expandProjective.process(db,workGraph,selected,cameraMatrix)) {
				if( verbose != null ) verbose.println("  Failed to expand/add view="+selected.id+". Discarding.");
				continue;
			}
			if( verbose != null ) {
				verbose.println("  Success Expanding: view=" + selected.id + "  inliers="
						+ utils.inliersThreeView.size() + " / " + utils.matchesTriple.size);
			}

			// save the results
			SceneWorkingGraph.View wview = workGraph.addView(selected);
			wview.projective.set(cameraMatrix);

			// save which features were used for later use in metric reconstruction
			assertBoof(utils.seed==wview.pview);// just being paranoid
			utils.saveRansacInliers(wview);

			// Add views which are neighbors
			addOpenForView(wview.pview, open);
		}
		if( verbose != null ) verbose.println("EXIT Expanding Scene");
	}
}
