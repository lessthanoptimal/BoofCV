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
import boofcv.struct.ScoreIndex;
import lombok.Getter;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

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
 * @see ProjectiveInitializeAllCommon
 * @see ProjectiveExpandByOneView
 * @see PairwiseGraphUtils
 *
 * @author Peter Abeles
 */
public class ProjectiveReconstructionFromPairwiseGraph implements VerbosePrint {

	/**
	 * Contains the found projective scene
	 */
	public final @Getter SceneWorkingGraph workGraph = new SceneWorkingGraph();

	// Computes the initial scene from the seed and some of it's neighbors
	private final @Getter ProjectiveInitializeAllCommon initProjective;
	// Adds a new view to an existing projective scene
	private final @Getter ProjectiveExpandByOneView expandProjective;
	// Common functions used in projective reconstruction
	final PairwiseGraphUtils utils;

	// If not null then verbose debugging information is printed
	PrintStream verbose;

	//--------------------------------------------- Internal workspace
	// scores of individual motions for a View
	FastQueue<ScoreIndex> scoresMotions = new FastQueue<>(ScoreIndex::new);
	// list of views that have already been explored
	HashSet<String> exploredViews = new HashSet<>();
	// information related to each view being a potential seed
	FastQueue<SeedInfo> seedScores = new FastQueue<>(SeedInfo::new,SeedInfo::reset);

	public ProjectiveReconstructionFromPairwiseGraph(PairwiseGraphUtils utils) {
		this.utils = utils;
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

		// Score nodes for their ability to be seeds
		Map<String, SeedInfo> mapScores = scoreNodesAsSeeds(graph);
		List<SeedInfo> seeds = selectSeeds(seedScores,mapScores);

		if( seeds.size() == 0 )
			return false;

		if( verbose != null ) verbose.println("Selected "+seeds.size()+" seeds out of "+graph.nodes.size);

		// For now we are keeping this very simple. Only a single seed is considered
		SeedInfo info = seeds.get(0);

		// Find the common features
		GrowQueue_I32 common = utils.findCommonFeatures(info.seed,info.motions);
		if( common.size < 6 ) // if less than the minimum it will fail
			return false;

		if( verbose != null ) verbose.println("Seed.id="+info.seed.id+" common="+common.size);

		if (!estimateInitialSceneFromSeed(db, info, common))
			return false;

		expandScene(db);

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
		for (int structViewIdx = 0; structViewIdx < initProjective.utils.structure.views.size; structViewIdx++) {
			View view = initProjective.getPairwiseGraphViewByStructureIndex(structViewIdx);
			if( verbose != null ) verbose.println("  init view.id="+view.id);
			DMatrixRMaj cameraMatrix = initProjective.utils.structure.views.get(structViewIdx).worldToView;
			workGraph.addView(view).camera.set(cameraMatrix);
			exploredViews.add(view.id);
		}

		return true;
	}

	/**
	 * Adds all the remaining views to the scene
	 */
	private void expandScene(LookupSimilarImages db) {
		// Create a list of views that can be added the work graph
		FastArray<View> open = findAllOpenViews();

		// Grow the projective scene until there are no more views to process
		if( verbose != null ) verbose.println("Expanding Scene:");
		DMatrixRMaj cameraMatrix = new DMatrixRMaj(3,4);
		while( open.size > 0 ) {
			View selected = selectNextToProcess(open);
			if( selected == null ) {
				if( verbose != null ) verbose.println("  No valid views left. open.size="+open.size);
				break;
			}

			if(!expandProjective.process(db,workGraph,selected,cameraMatrix)) {
				if( verbose != null )
					verbose.println("Failed to expand/add view="+selected.id+". Discarding.");
				continue;
			}

			// save the results
			SceneWorkingGraph.View wview = workGraph.addView(selected);
			wview.camera.set(cameraMatrix);

			// Add views which are neighbors
			addOpenForView(wview.pview, open);
		}
	}

	/**
	 * Searches all connections to known views and creates a list of connected views which have a 3D relationship
	 */
	FastArray<View> findAllOpenViews() {
		FastArray<View> found = new FastArray<>(View.class);

		for( SceneWorkingGraph.View wview : workGraph.getAllViews() ) {
			addOpenForView(wview.pview, found);
		}

		return found;
	}

	/**
	 * Adds connections to the passed in view to the list of views to explore. Care is taken to not add the same
	 * view more than once
	 * @param view (Input) Inspects connected views to add to found
	 * @param found (Output) Storage for selected views
	 */
	void addOpenForView(View view, FastArray<View> found) {
		for( PairwiseImageGraph2.Motion c :  view.connections.toList() ) {
			if( !c.is3D )
				continue;

			View o = c.other(view);

			if( exploredViews.contains(o.id) )
				continue;

			if( found.contains(o) )
				continue;

			found.add(o);
			exploredViews.add(o.id);
		}
	}

	/**
	 * Selects next View to process based on the score of it's known connections. At least two connections is preferred
	 * and the score is based on the worst score of the two connections.
	 */
	View selectNextToProcess( FastArray<View> open ) {
		int bestIdx = -1;
		double bestScore = 0.0;
		int bestConnections = 0;
		for (int openIdx = 0; openIdx < open.size; openIdx++) {
			View pview = open.get(openIdx);

			int totalConnections = 0;
			double score = Double.MAX_VALUE;
			for (int connIdx = 0; connIdx < pview.connections.size; connIdx++) {
				PairwiseImageGraph2.Motion m = pview.connections.get(connIdx);
				View dst = m.other(pview);

				// View must already be known for connection to be considered
				if( !workGraph.isKnown(dst) )
					continue;

				// Only save the worst score
				score = Math.min(score,utils.scoreMotion.score(m));
				totalConnections++;
			}

			// Prefer two connections
			if( bestConnections < totalConnections && bestConnections < 2 ) {
				bestConnections = totalConnections;
				bestScore = score;
				bestIdx = openIdx;
			} else if( bestConnections == totalConnections && score != Double.MAX_VALUE && bestScore < score ) {
				bestScore = score;
				bestIdx = openIdx;
			}
		}

		if( bestIdx < 0 )
			return null;

		View selected = open.removeSwap(bestIdx);
		if( verbose != null ) verbose.println("  open.size="+open.size+" selected.id="+selected.id+" score="+bestScore+" conn="+bestConnections);

		return selected;
	}

	/**
	 * Considers every view in the graph as a potential seed and computes their scores
	 */
	Map<String, SeedInfo> scoreNodesAsSeeds(PairwiseImageGraph2 graph) {
		seedScores.reset();
		Map<String,SeedInfo> mapScores = new HashMap<>();
		for (int idxView = 0; idxView < graph.nodes.size; idxView++) {
			View v = graph.nodes.get(idxView);
			SeedInfo info = seedScores.grow();
			score(v,info);
			mapScores.put(v.id,info);
		}
		return mapScores;
	}

	/**
	 * Get a list of all nodes which can be seeds. From the list of candidate seeds it picks the seeds with the
	 * highest score first. Then all of their children are marked as having a score of zero, which means they
	 * will be skipped over later on.
	 *
	 * @param candidates (input) All the candidate views for seeds
	 * @param lookupInfo (input) Used to lookup SeedInfo by View ID
	 * @return List of seeds with the best seeds first.
	 */
	List<SeedInfo> selectSeeds(FastQueue<SeedInfo> candidates, Map<String, SeedInfo> lookupInfo) {
		// Storage for selected seeds
		List<SeedInfo> seeds = new ArrayList<>();
		// sort it so best scores are last
		Collections.sort(candidates.toList());

		// ignore nodes with too low of a score
		double minScore = candidates.get(candidates.size()-1).score*0.2;

		// Start iterating from the best scores
		for (int i = candidates.size()-1; i >= 0; i--) {
			SeedInfo s = candidates.get(i);

			// skip if it's a neighbor to an already selected seed
			if( s.neighbor )
				continue;

			// All scores for now on will be below the minimum
			if( s.score <= minScore )
				break;

			// If any of the connected seeds are zero it's too close to another seed and you should pass over it
			boolean skip = false;
			for (int j = 0; j < s.seed.connections.size; j++) {
				if( lookupInfo.get(s.seed.connections.get(j).other(s.seed).id).neighbor ) {
					skip = true;
					break;
				}
			}
			if( skip )
				continue;

			// This is a valid seed so add it to the list
			seeds.add(s);

			// zero the score of children so that they can't be a seed. This acts as a sort of non-maximum suppression
			for (int j = 0; j < s.seed.connections.size; j++) {
				lookupInfo.get(s.seed.connections.get(j).other(s.seed).id).neighbor = true;
			}
		}
		return seeds;
	}

	/**
	 * Score a view for how well it could be a seed based on the the 3 best 3D motions associated with it
	 */
	SeedInfo score( View target , SeedInfo output ) {
		output.seed = target;
		scoresMotions.reset();

		// score all edges
		for (int i = 0; i < target.connections.size; i++) {
			PairwiseImageGraph2.Motion m = target.connections.get(i);
			if( !m.is3D )
				continue;

			scoresMotions.grow().set(utils.scoreMotion.score(m),i);
		}

		// only score the 3 best. This is to avoid biasing it for
		Collections.sort(scoresMotions.toList());

		for (int i = Math.min(3, scoresMotions.size)-1; i >= 0; i--) {
			output.motions.add(scoresMotions.get(i).index);
			output.score += scoresMotions.get(i).score;
		}

		return output;
	}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		this.verbose = out;
	}

	/**
	 * Information related to a view acting as a seed to spawn a projective graph
	 */
	static class SeedInfo implements Comparable<SeedInfo> {
		// The potential initial seed
		View seed;
		// score for how good of a seed this node would make. higher is better
		double score;
		// edges in seed that were used to generate the score
		GrowQueue_I32 motions = new GrowQueue_I32();
		// if it's a neighbor of a seed
		boolean neighbor = false;

		public void reset() {
			seed = null;
			score = 0;
			motions.reset();
			neighbor = false;
		}

		@Override
		public int compareTo(SeedInfo o) {
			return Double.compare(score, o.score);
		}
	}
}
