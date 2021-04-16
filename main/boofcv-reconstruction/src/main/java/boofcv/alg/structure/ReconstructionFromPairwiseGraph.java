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

import boofcv.alg.structure.PairwiseImageGraph.View;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.ScoreIndex;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;
import org.ddogleg.struct.VerbosePrint;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.*;

/**
 * Contains common functions useful for perform a full scene reconstruction from a {@link PairwiseImageGraph}.
 * This includes selecting the seed views, selecting next views to expand to, and boiler plate.
 *
 * @author Peter Abeles
 * @see MetricFromUncalibratedPairwiseGraph
 * @see ProjectiveReconstructionFromPairwiseGraph
 */
public abstract class ReconstructionFromPairwiseGraph implements VerbosePrint {

	// Common functions used in projective reconstruction
	protected PairwiseGraphUtils utils;

	// If not null then verbose debugging information is printed
	protected @Nullable PrintStream verbose;

	//--------------------------------------------- Internal workspace
	// scores of individual motions for a View
	public DogArray<ScoreIndex> scoresMotions = new DogArray<>(ScoreIndex::new);

	// information related to each view being a potential seed
	DogArray<SeedInfo> seedScores = new DogArray<>(SeedInfo::new, SeedInfo::reset);

	private final List<View> valid = new ArrayList<>();

	protected ReconstructionFromPairwiseGraph( PairwiseGraphUtils utils ) {
		this.utils = utils;
	}

	/**
	 * Searches all connections to known views and creates a list of connected views which have a 3D relationship
	 */
	protected void findAllOpenViews( SceneWorkingGraph scene ) {
		for (SceneWorkingGraph.View wview : scene.getAllViews()) {
			addOpenForView(scene, wview.pview);
		}
	}

	/**
	 * Adds connections to the passed in view to the list of views to explore. Care is taken to not add the same
	 * view more than once
	 *
	 * @param view (Input) Inspects connected views to add to found
	 */
	protected void addOpenForView( SceneWorkingGraph scene, View view ) {
		for (PairwiseImageGraph.Motion c : view.connections.toList()) {
			if (!c.is3D)
				continue;

			View o = c.other(view);

			if (scene.exploredViews.contains(o.id))
				continue;

			if (scene.open.contains(o))
				continue;

			if (verbose != null) verbose.println("  scene[" + scene.index + "] adding to open view.id='" + o.id + "'");
			scene.open.add(o);
			scene.exploredViews.add(o.id);
		}
	}

	/**
	 * Selects next View to process based on the score of it's known connections. Two connections which both
	 * connect to each other is required.
	 *
	 * @param selection The results
	 * @return true if a valid choice was found. Otherwise false.
	 */
	protected boolean selectNextToProcess( SceneWorkingGraph scene, Expansion selection ) {
		selection.reset();
		selection.scene = scene;

		int bestIdx = -1;
		double bestScore = 0.0;
		int bestValidCount = 0;

		valid.clear();

		for (int openIdx = 0; openIdx < scene.open.size; openIdx++) {
			final View pview = scene.open.get(openIdx);

			// Create a list of valid views pview can connect too
			valid.clear();
			for (int connIdx = 0; connIdx < pview.connections.size; connIdx++) {
				PairwiseImageGraph.Motion m = pview.connections.get(connIdx);
				View dst = m.other(pview);
				if (!m.is3D || !scene.isKnown(dst))
					continue;
				valid.add(dst);
			}
			double bestLocalScore = 0.0;
			for (int idx0 = 0; idx0 < valid.size(); idx0++) {
				View dst = valid.get(idx0);

				for (int idx1 = idx0 + 1; idx1 < valid.size(); idx1++) {
					if (null == dst.findMotion(valid.get(idx1)))
						continue;

					PairwiseImageGraph.Motion m0 = pview.findMotion(dst);
					PairwiseImageGraph.Motion m1 = pview.findMotion(valid.get(idx1));
					PairwiseImageGraph.Motion m2 = dst.findMotion(valid.get(idx1));

					double s = BoofMiscOps.min(m0.score3D, m1.score3D, m2.score3D);

					bestLocalScore = Math.max(s, bestLocalScore);
				}
			}

			// strongly prefer 3 or more. Technically the above test won't check for this but in the future it will
			// so this test serves as a reminder
			if (Math.min(3, valid.size()) >= bestValidCount && bestLocalScore > bestScore) {
				bestValidCount = Math.min(3, valid.size());
				bestScore = bestLocalScore;
				bestIdx = openIdx;
			}
		}

		if (bestIdx < 0) {
			if (verbose != null) {
				verbose.println("  Failed to find a valid view to connect. open.size=" + scene.open.size);
				for (int i = 0; i < scene.open.size; i++) {
					View v = scene.open.get(i);
					verbose.print("    id='" + v.id + "' conn={ ");
					for (int j = 0; j < v.connections.size; j++) {
						verbose.print("'" + v.connections.get(j).other(v).id + "' ");
					}
					verbose.println("}");
				}
			}

			return false;
		}

		if (verbose != null)
			verbose.println("  scene[" + scene.index + "].open.size=" + scene.open.size + " score=" +
					bestScore + " conn=" + bestValidCount);

		selection.score = bestScore;
		selection.openIdx = bestIdx;

		return true;
	}

	/**
	 * Considers every view in the graph as a potential seed and computes their scores
	 */
	protected Map<String, SeedInfo> scoreNodesAsSeeds( PairwiseImageGraph graph ) {
		seedScores.reset();
		Map<String, SeedInfo> mapScores = new HashMap<>();
		for (int idxView = 0; idxView < graph.nodes.size; idxView++) {
			View v = graph.nodes.get(idxView);
			SeedInfo info = seedScores.grow();
			scoreAsSeed(v, info);
			mapScores.put(v.id, info);
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
	protected List<SeedInfo> selectSeeds( DogArray<SeedInfo> candidates, Map<String, SeedInfo> lookupInfo ) {
		// Storage for selected seeds
		List<SeedInfo> seeds = new ArrayList<>();
		// sort it so best scores are last
		Collections.sort(candidates.toList());

		// Should geometry should be used to select the minimum possible score not just a relative score?

		// ignore nodes with too low of a score
		double minScore = candidates.get(candidates.size() - 1).score*0.2;

		if (verbose != null) {
			verbose.printf("SelectSeeds: candidates.size=%d minScore=%.2f\n", candidates.size, minScore);
		}

		// Collect summary information on rejections
		int rejectedNeighbor = 0;
		int rejectedScore = 0;
		int rejectedClose = 0;

		// Start iterating from the best scores
		for (int i = candidates.size() - 1; i >= 0; i--) {
			SeedInfo s = candidates.get(i);

			// skip if it's a neighbor to an already selected seed
			if (s.neighbor) {
				rejectedNeighbor++;
				continue;
			}

			// All scores for now on will be below the minimum since they are sorted
			if (s.score <= minScore) {
				rejectedScore = i + 1;
				break;
			}

			// If any of the connected seeds are zero it's too close to another seed and you should pass over it
			boolean skip = false;
			for (int j = 0; j < s.seed.connections.size; j++) {
				if (lookupInfo.get(s.seed.connections.get(j).other(s.seed).id).neighbor) {
					rejectedClose++;
					skip = true;
					break;
				}
			}
			if (skip)
				continue;

			// This is a valid seed so add it to the list
			seeds.add(s);

			// zero the score of children so that they can't be a seed. This acts as a sort of non-maximum suppression
			for (int j = 0; j < s.seed.connections.size; j++) {
				lookupInfo.get(s.seed.connections.get(j).other(s.seed).id).neighbor = true;
			}
		}

		if (verbose != null)
			verbose.printf("Seed Rejections: neighbor=%3d score=%3d close=%3d\n",
					rejectedNeighbor, rejectedScore, rejectedClose);


		return seeds;
	}

	/**
	 * Score a view for how well it could be a seed based on the the 3 best 3D motions associated with it
	 */
	protected SeedInfo scoreAsSeed( View target, SeedInfo output ) {
		output.seed = target;
		scoresMotions.reset();

		// score all edges
		for (int i = 0; i < target.connections.size; i++) {
			PairwiseImageGraph.Motion m = target.connections.get(i);
			if (!m.is3D)
				continue;

			scoresMotions.grow().set(m.score3D, i);
		}

		// only score the 3 best. This is to avoid biasing it for
		Collections.sort(scoresMotions.toList());

		for (int i = Math.min(3, scoresMotions.size) - 1; i >= 0; i--) {
			output.motions.add(scoresMotions.get(i).index);
			output.score += scoresMotions.get(i).score;
		}

		return output;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
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
		DogArray_I32 motions = new DogArray_I32();
		// if it's a neighbor of a seed
		boolean neighbor = false;

		public void reset() {
			seed = null;
			score = 0;
			motions.reset();
			neighbor = false;
		}

		@Override public int compareTo( SeedInfo o ) { return Double.compare(score, o.score); }
	}

	/**
	 * Contains information on a potential expansion
	 */
	protected static class Expansion {
		public SceneWorkingGraph scene;
		public int openIdx;

		public double score;

		public void reset() {
			scene = null;
			openIdx = -1;
			score = -1;
		}
	}
}
