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
import boofcv.struct.ConfigLength;
import boofcv.struct.ScoreIndex;
import lombok.Getter;
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

	/**
	 * It will stop spawning more seeds when it fails this many times. If relative, then its relative to the
	 * maximum number of candidate views for seeds.
	 */
	public final @Getter ConfigLength maximumSeedFailures = ConfigLength.relative(0.1,10);

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
		int openSizeBefore = scene.open.size;

		for (int connIdx = 0; connIdx < view.connections.size; connIdx++) {
			PairwiseImageGraph.Motion c = view.connections.get(connIdx);
			// If there isn't 3D information skip it
			if (!c.is3D)
				continue;

			View o = c.other(view);

			// Make sure it hasn't been added or considered already
			if (scene.exploredViews.contains(o.id))
				continue;

			scene.open.add(o);
			scene.exploredViews.add(o.id);
		}

		if (verbose == null)
			return;

		verbose.print("_ scene[" + scene.index + "].view='" + view.id + "' adding: size=" +
				(scene.open.size - openSizeBefore) + " views={ ");
		for (int i = openSizeBefore; i < scene.open.size; i++) {
			verbose.print("'" + scene.open.get(i).id + "' ");
		}
		verbose.println("}");
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
				View viewB = valid.get(idx0);
				PairwiseImageGraph.Motion m0 = Objects.requireNonNull(pview.findMotion(viewB));

				for (int idx1 = idx0 + 1; idx1 < valid.size(); idx1++) {
					View viewC = valid.get(idx1);
					PairwiseImageGraph.Motion m2 = viewB.findMotion(viewC);

					if (m2 == null || !m2.is3D)
						continue;

					PairwiseImageGraph.Motion m1 = Objects.requireNonNull(pview.findMotion(viewC));

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
				verbose.println("_ Failed to find a valid view to connect. open.size=" + scene.open.size);
				for (int i = 0; i < scene.open.size; i++) {
					View v = scene.open.get(i);
					verbose.print("___ id='" + v.id + "' conn={ ");
					for (int j = 0; j < v.connections.size; j++) {
						verbose.print("'" + v.connections.get(j).other(v).id + "' ");
					}
					verbose.println("}");
				}
			}

			return false;
		}

		if (verbose != null)
			verbose.printf("_ scene[%d].open.size=%d score=%.2f conn=%d\n",
					scene.index, scene.open.size, bestScore, bestValidCount);

		selection.score = bestScore;
		selection.openIdx = bestIdx;

		return true;
	}

	/**
	 * Considers every view in the graph as a potential seed and computes their scores
	 */
	protected Map<String, SeedInfo> scoreNodesAsSeeds( PairwiseImageGraph graph, int maxMotions ) {
		seedScores.reset();
		Map<String, SeedInfo> mapScores = new HashMap<>();
		for (int idxView = 0; idxView < graph.nodes.size; idxView++) {
			View v = graph.nodes.get(idxView);
			SeedInfo info = seedScores.grow();
			scoreSeedAndSelectSet(v, maxMotions, info);
			mapScores.put(v.id, info);
		}
		return mapScores;
	}

	/**
	 * Goes through every candidate for a seed and greedily selects the ones with the best score. It then
	 * attempts a metric upgrade. If successful it then uses that as a seed and marks all of its neighbors
	 * so that they are not selected as seeds themselves.
	 *
	 * @param candidates (input) All the candidate views for seeds
	 * @param lookupInfo (input) Used to lookup SeedInfo by View ID
	 */
	protected void selectAndSpawnSeeds( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams,
										PairwiseImageGraph pairwise,
										DogArray<SeedInfo> candidates, Map<String, SeedInfo> lookupInfo ) {

		// sort it so best scores are last
		Collections.sort(candidates.toList());

		if (verbose != null) {
			double maxScore = candidates.get(candidates.size - 1).score;
			double minScore = candidates.get(0).score;
			verbose.printf("Select Seeds: candidates.size=%d scores=(%.2f - %.2f)\n",
					candidates.size, minScore, maxScore);
		}

		// When it reaches the maximum number of failed seeds/spawns it will stop trying
		int maxFailures = maximumSeedFailures.computeI(candidates.size);

		// Collect summary information on rejections
		int rejectedNeighbor = 0;
		int rejectedClose = 0;
		int rejectedSpawn = 0;
		int successes = 0;

		// Start iterating from the best scores
		for (int i = candidates.size() - 1; i >= 0 && rejectedSpawn < maxFailures; i--) {
			SeedInfo s = candidates.get(i);

			// skip if it's a neighbor to an already selected seed. Also if it has no connections.
			if (s.neighbor || s.motions.isEmpty()) {
				rejectedNeighbor++;
				continue;
			}

			// If any of the connected seeds are zero it's too close to another seed and you should pass over it
			boolean skip = false;
			for (int j = 0; j < s.seed.connections.size; j++) {
				if (Objects.requireNonNull(lookupInfo.get(s.seed.connections.get(j).other(s.seed).id)).neighbor) {
					rejectedClose++;
					skip = true;
					break;
				}
			}
			if (skip)
				continue;

			// Attempt to create a new scene here.
			if (!spawnSceneFromSeed(dbSimilar, dbCams, pairwise, s)) {
				if (verbose != null) verbose.println("FAILED: Spawn view.id='" + s.seed.id + "', remaining=" + i);
				rejectedSpawn++;
				continue;
			}

			if (verbose != null) verbose.println("Successfully spawned view.id='" + s.seed.id + "', remaining=" + i);

			// zero the score of children so that they can't be a seed. This acts as a sort of non-maximum suppression
			for (int j = 0; j < s.seed.connections.size; j++) {
				Objects.requireNonNull(lookupInfo.get(s.seed.connections.get(j).other(s.seed).id)).neighbor = true;
			}

			successes++;
		}

		if (verbose != null) {
			verbose.printf("Seed Summary: candidates=%d, success=%d, failures: neighbor=%d close=%d spawn=%d\n",
					candidates.size, successes, rejectedNeighbor, rejectedClose, rejectedSpawn);
		}
	}

	/**
	 * Attempts to create a scene from the passed in seed. If successful this function needs
	 * to add it to the list of scenes. If it fails then there should be no change.
	 *
	 * @param info Seed for a new scene
	 * @return true if successful or false if it failed
	 */
	protected abstract boolean spawnSceneFromSeed( LookUpSimilarImages dbSimilar, LookUpCameraInfo dbCams,
												   PairwiseImageGraph pairwise, SeedInfo info );

	/**
	 * Scores how the target as a seed and selects the initial set of views it should spawn from.
	 */
	protected SeedInfo scoreSeedAndSelectSet( View target, int maxMotions, SeedInfo output ) {
		output.seed = target;
		scoresMotions.reset();

		// score all edges
		for (int i = 0; i < target.connections.size; i++) {
			PairwiseImageGraph.Motion m = target.connections.get(i);
			if (!m.is3D)
				continue;

			scoresMotions.grow().set(m.score3D, i);
		}

		// After a view has been selected, the others are selected based on their connection to the already
		// selected ones. This avoids selecting two nearly identical views
		while (output.motions.size < maxMotions && !scoresMotions.isEmpty()) {
			double bestScore = 0;
			int bestMotion = -1;
			for (int i = 0; i < scoresMotions.size; i++) {
				// set the score initially to the score with the target
				double score = scoresMotions.get(i).score;
				PairwiseImageGraph.View va = target.connections.get(scoresMotions.get(i).index).other(target);

				// Go through all already selected motions. Reduce the score is similar to another view.
				// Disqualify if so similar it doesn't have a 3D connection
				boolean foundValid = true;
				for (int outIdx = 0; outIdx < output.motions.size; outIdx++) {
					int connIdx = output.motions.get(outIdx);
					PairwiseImageGraph.View vb = target.connections.get(connIdx).other(target);
					PairwiseImageGraph.Motion m = va.findMotion(vb);

					// All seed views must be connected with each other with a 3D relationship
					if (m == null || !m.is3D) {
						foundValid = false;
						break;
					}
					score = Math.min(score, m.score3D);
				}
				if (!foundValid || score <= bestScore)
					continue;
				bestScore = score;
				bestMotion = i;
			}

			// stop if it can't find another valid view
			if (bestScore == 0.0)
				break;

			output.motions.add(scoresMotions.removeSwap(bestMotion).index);
			output.score += bestScore;
		}

		return output;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		this.verbose = BoofMiscOps.addPrefix(this, out);
	}

	/**
	 * Information related to a view acting as a seed to spawn a projective graph
	 */
	@SuppressWarnings("NullAway.Init")
	protected static class SeedInfo implements Comparable<SeedInfo> {
		// The potential initial seed
		View seed;
		// score for how good of a seed this node would make. higher is better
		double score;
		// edges in seed that were used to generate the score
		DogArray_I32 motions = new DogArray_I32();
		// if it's a neighbor of a seed
		boolean neighbor = false;

		@SuppressWarnings("NullAway")
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
	@SuppressWarnings({"NullAway.Init"})
	protected static class Expansion {
		public SceneWorkingGraph scene;
		public int openIdx;

		public double score;

		@SuppressWarnings({"NullAway"})
		public void reset() {
			scene = null;
			openIdx = -1;
			score = -1;
		}
	}
}
