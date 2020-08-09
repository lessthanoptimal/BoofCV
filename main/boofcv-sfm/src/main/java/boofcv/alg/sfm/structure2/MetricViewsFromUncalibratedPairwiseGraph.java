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
import boofcv.struct.ScoreIndex;
import boofcv.struct.geo.AssociatedTupleDN;
import boofcv.struct.image.ImageDimension;
import lombok.Getter;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;
import org.ddogleg.struct.VerbosePrint;
import org.ejml.data.DMatrixRMaj;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

import static boofcv.misc.BoofMiscOps.assertBoof;

/**
 * TODO WRITE THIS
 *
 * @author Peter Abeles
 */
public class MetricViewsFromUncalibratedPairwiseGraph implements VerbosePrint {

	/** Contains the found projective scene */
	public final @Getter SceneWorkingGraph workGraph = new SceneWorkingGraph();
	/** Computes the initial scene from the seed and some of it's neighbors */
	private final @Getter ProjectiveInitializeAllCommon initProjective;

	private final ProjectiveToMetricCameras projectiveToMetric = FactoryMultiView.projectiveToMetric((ConfigSelfCalibDualQuadratic)null);

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
	FastQueue<SeedInfo> seedScores = new FastQueue<>(SeedInfo::new, SeedInfo::reset);

	public MetricViewsFromUncalibratedPairwiseGraph(PairwiseGraphUtils utils) {
		this.utils = utils;
		initProjective = new ProjectiveInitializeAllCommon();
		initProjective.utils = utils;
	}

	public MetricViewsFromUncalibratedPairwiseGraph(ConfigProjectiveReconstruction config) {
		this(new PairwiseGraphUtils(config));
	}

	public MetricViewsFromUncalibratedPairwiseGraph() {
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

		// Find the common features
		GrowQueue_I32 common = utils.findCommonFeatures(info.seed,info.motions);
		if( common.size < 6 ) // if less than the minimum it will fail
			return false;

		if( verbose != null ) verbose.println("Selected seed.id="+info.seed.id+" common="+common.size);


		if (!estimateProjectiveSceneFromSeed(db, info, common))
			return false;

		// Save found camera matrices for each view it was estimated in
		if( verbose != null ) verbose.println("Saving initial seed camera matrices");

		// TODO from seed get metric reconstruction
		List<String> viewIds = new ArrayList<>();
		FastQueue<ImageDimension> dimensions = new FastQueue<>(ImageDimension::new);
		FastQueue<DMatrixRMaj> views = new FastQueue<>(()->new DMatrixRMaj(3,4));
		FastQueue<AssociatedTupleDN> observations = new FastQueue<>(AssociatedTupleDN::new);

		initProjective.lookupInfoForMetricElevation(viewIds,dimensions,views,observations);
		MetricCameras results = new MetricCameras();
//		projectiveToMetric.process(viewIds,dimensions,views,observations,results);

		// TODO save which features were inliers at every stage so that the 3D cloud can be computed later on

		// TODO expand the scene by finding connected triplets


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

//			if(!expandProjective.process(db,workGraph,selected,cameraMatrix)) {
//				if( verbose != null ) verbose.println("  Failed to expand/add view="+selected.id+". Discarding.");
//				continue;
//			}
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

			if( verbose != null ) verbose.println("  adding to open list view.id='"+o.id+"'");
			found.add(o);
			exploredViews.add(o.id);
		}
	}

	/**
	 * Selects next View to process based on the score of it's known connections. Two connections which both
	 * connect to each other is required.
	 */
	View selectNextToProcess( FastArray<View> open ) {
		int bestIdx = -1;
		double bestScore = 0.0;
		int bestValidCount = 0;

		List<View> valid = new ArrayList<>();

		for (int openIdx = 0; openIdx < open.size; openIdx++) {
			final View pview = open.get(openIdx);

			// Create a list of valid views pview can connect too
			valid.clear();
			for (int connIdx = 0; connIdx < pview.connections.size; connIdx++) {
				PairwiseImageGraph2.Motion m = pview.connections.get(connIdx);
				View dst = m.other(pview);
				if( !m.is3D || !workGraph.isKnown(dst) )
					continue;
				valid.add(dst);
			}
			double bestLocalScore = 0.0;
			for (int idx0 = 0; idx0 < valid.size(); idx0++) {
				View dst = valid.get(idx0);

				for (int idx1 = idx0+1; idx1 < valid.size(); idx1++) {
					if( null == dst.findMotion(valid.get(idx1)) )
						continue;

					PairwiseImageGraph2.Motion m0 = pview.findMotion(dst);
					PairwiseImageGraph2.Motion m1 = pview.findMotion(valid.get(idx1));
					PairwiseImageGraph2.Motion m2 = dst.findMotion(valid.get(idx1));

					double s = Math.min(utils.scoreMotion.score(m0),utils.scoreMotion.score(m1));
					s = Math.min(s,utils.scoreMotion.score(m2));

					bestLocalScore = Math.max(s,bestLocalScore);
				}
			}

//			System.out.println("view.id="+pview.id+"  valid.size"+valid.size()+" score="+bestLocalScore);

			// strongly prefer 3 or more. Technically the above test won't check for this but in the future it will
			// so this test serves as a reminder
			if( Math.min(3,valid.size()) >= bestValidCount && bestLocalScore > bestScore ) {
				bestValidCount = Math.min(3,valid.size());
				bestScore = bestLocalScore;
				bestIdx = openIdx;
			}
		}

		if( bestIdx < 0 )
			return null;

		View selected = open.removeSwap(bestIdx);
		if( verbose != null ) verbose.println("  open.size="+open.size+" selected.id="+selected.id+" score="+bestScore+" conn="+bestValidCount);

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
