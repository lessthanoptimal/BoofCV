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
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Contains common functions useful for perform a full scene reconstruction from a {@link PairwiseImageGraph2}.
 * This includes selecting the seed views, selecting next views to expand to, and boiler plate.
 *
 * @see MetricFromUncalibratedPairwiseGraph
 * @see ProjectiveReconstructionFromPairwiseGraph
 *
 * @author Peter Abeles
 */
public abstract class ReconstructionFromPairwiseGraph implements VerbosePrint {

	/** Contains the found projective scene */
	public final @Getter SceneWorkingGraph workGraph = new SceneWorkingGraph();

	// Common functions used in projective reconstruction
	protected PairwiseGraphUtils utils;

	// If not null then verbose debugging information is printed
	protected @Nullable PrintStream verbose;

	//--------------------------------------------- Internal workspace
	// scores of individual motions for a View
	FastQueue<ScoreIndex> scoresMotions = new FastQueue<>(ScoreIndex::new);
	// list of views that have already been explored
	HashSet<String> exploredViews = new HashSet<>();
	// information related to each view being a potential seed
	FastQueue<SeedInfo> seedScores = new FastQueue<>(SeedInfo::new, SeedInfo::reset);

	public ReconstructionFromPairwiseGraph(PairwiseGraphUtils utils) {
		this.utils = utils;
	}

	/**
	 * Searches all connections to known views and creates a list of connected views which have a 3D relationship
	 */
	protected FastArray<View> findAllOpenViews() {
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
	protected void addOpenForView(View view, FastArray<View> found) {
		for( PairwiseImageGraph2.Motion c :  view.connections.toList() ) {
			if( !c.is3D )
				continue;

			View o = c.other(view);

			if( exploredViews.contains(o.id) )
				continue;

			if( found.contains(o) )
				continue;

			if( verbose != null ) verbose.println("  Adding to open list view.id='"+o.id+"'");
			found.add(o);
			exploredViews.add(o.id);
		}
	}

	/**
	 * Selects next View to process based on the score of it's known connections. Two connections which both
	 * connect to each other is required.
	 */
	protected View selectNextToProcess( FastArray<View> open ) {
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

			// strongly prefer 3 or more. Technically the above test won't check for this but in the future it will
			// so this test serves as a reminder
			if( Math.min(3,valid.size()) >= bestValidCount && bestLocalScore > bestScore ) {
				bestValidCount = Math.min(3,valid.size());
				bestScore = bestLocalScore;
				bestIdx = openIdx;
			}
		}

		if( bestIdx < 0 ) {
			if( verbose != null ) {
				verbose.println("  Failed to find a valid view to connect. open.size=" + open.size);
				for (int i = 0; i < open.size; i++) {
					View v = open.get(i);
					verbose.print("    id='"+v.id+"' conn={ ");
					for (int j = 0; j < v.connections.size; j++) {
						verbose.print("'"+v.connections.get(j).other(v).id+"' ");
					}
					verbose.println("}");
				}
			}

			return null;
		}

		View selected = open.removeSwap(bestIdx);
		if( verbose != null ) verbose.println("  open.size="+open.size+" selected.id='"+selected.id+"' score="+bestScore+" conn="+bestValidCount);

		return selected;
	}

	/**
	 * Considers every view in the graph as a potential seed and computes their scores
	 */
	protected Map<String, SeedInfo> scoreNodesAsSeeds(PairwiseImageGraph2 graph) {
		seedScores.reset();
		Map<String,SeedInfo> mapScores = new HashMap<>();
		for (int idxView = 0; idxView < graph.nodes.size; idxView++) {
			View v = graph.nodes.get(idxView);
			SeedInfo info = seedScores.grow();
			scoreAsSeed(v,info);
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
	protected List<SeedInfo> selectSeeds(FastQueue<SeedInfo> candidates, Map<String, SeedInfo> lookupInfo) {
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
	protected SeedInfo scoreAsSeed(View target , SeedInfo output ) {
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
	protected static class SeedInfo implements Comparable<SeedInfo> {
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
