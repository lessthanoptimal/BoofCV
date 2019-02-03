/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.sfm.structure2.PairwiseImageGraph2.Motion;
import boofcv.alg.sfm.structure2.PairwiseImageGraph2.View;
import boofcv.factory.geo.ConfigRansac;
import boofcv.factory.geo.ConfigTrifocal;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.AssociatedTripleIndex;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Given a set of views all of which view all the same features, estimate their structure up to a
 * projective transform
 *
 * @author Peter Abeles
 */
public class ProjectiveInitializeAllCommon {

	public ConfigRansac configRansac = new ConfigRansac();
	public ConfigTrifocal configTriRansac = new ConfigTrifocal();


	List<View> views = new ArrayList<>();


	GrowQueue_I32 common;

	// Internal workspace variables
	int selectedTriple[] = new int[2];
	FastQueue<AssociatedTripleIndex> matchesTriple = new FastQueue<>(AssociatedTripleIndex.class,true);

	public ProjectiveInitializeAllCommon() {
		configRansac.maxIterations = 500;
		configRansac.inlierThreshold = 1;
	}

	/**
	 *
	 * @param seed The seed view that will act as the origin
	 * @param common Indexes of common features. Index in seed view
	 * @param motions Index of motions in the seed view to use when initializing
	 * @return
	 */
	public boolean process(View seed , GrowQueue_I32 common , GrowQueue_I32 motions ) {
		if( motions.size == 1 ) {
			// TODO stereo is a special case
			return false;
		}
		// find the 3 view combination with the best score
		if( !selectInitialTriplet(seed,motions,selectedTriple))
			return false;

		// Find tracks between all 3 views
		Motion seedB = seed.connections.get(selectedTriple[0]);
		Motion seedC = seed.connections.get(selectedTriple[1]);

		findTripleMatches(seed,seedB,seedC,matchesTriple);
		if( matchesTriple.size == 0 )
			return false;

		// Use trifocal tensor to prune tracks from that set
		estimateTrifocal();

		// TODO remove features not in the inlier set

		// TODO compute projective transform

		// Estimate projective for each view not in the original triplet
		for (int motionIdx = 0; motionIdx < motions.size; motionIdx++) {
			// TODO compute projective transform relative to the seed
		}


		// TODO refine results with projective bundle adjustment

		return true;
	}

	/**
	 * Exhaustively look at all triplets that connect with the seed view
	 */
	boolean selectInitialTriplet( View seed , GrowQueue_I32 motions , int selected[] ) {
		double bestScore = 0;
		for (int i = 0; i < motions.size; i++) {
			View viewB = seed.connections.get(i).other(seed);

			for (int j = i+1; j < motions.size; j++) {
				View viewC = seed.connections.get(j).other(seed);

				double s = scoreTripleView(seed,viewB,viewC);
				if( s > bestScore ) {
					bestScore = s;
					selected[0] = i;
					selected[1] = j;
				}
			}
		}
		return bestScore != 0;
	}

	double scoreTripleView(View seedA, View viewB , View viewC ) {
		Motion motionAB = seedA.findMotion(viewB);
		Motion motionAC = seedA.findMotion(viewC);
		Motion motionBC = viewB.findMotion(viewC);
		if( motionBC == null )
			return 0;

		double score = 0;
		score += DoStuffFromPairwiseGraph.score(motionAB);
		score += DoStuffFromPairwiseGraph.score(motionAC);
		score += DoStuffFromPairwiseGraph.score(motionBC);

		return score;
	}

	void findTripleMatches(View seedA, Motion edgeAB , Motion edgeAC ,
						   FastQueue<AssociatedTripleIndex> matches ) {
		matches.reset();

		boolean srcAB = edgeAB.src == seedA;
		boolean srcAC = edgeAC.src == seedA;

		View viewB = srcAB ? edgeAB.dst : edgeAB.src;
		View viewC = srcAC ? edgeAC.dst : edgeAC.src;

		// see if there's an edge from viewA to viewB. There should be...
		Motion edgeBC = viewB.findMotion(viewC);
		if( edgeBC == null ) {
			return;
		}

		int[] table_B_to_A = createFeatureLookup(edgeAB, srcAB, viewB);
		int[] table_C_to_A = createFeatureLookup(edgeAC, srcAC, viewC);

		// Go through all the matches from B to C and see if the path is consistent between all the views
		boolean srcIsB = edgeBC.src == viewB;
		for (int i = 0; i < edgeBC.associated.size; i++) {
			AssociatedIndex assoc = edgeBC.associated.get(i);
			if( srcIsB ) {
				if( table_B_to_A[assoc.src] != -1 ) {
					AssociatedTripleIndex tri = matches.grow();
					tri.a = table_B_to_A[assoc.src];
					if( table_C_to_A[assoc.dst] == tri.a ) {
						tri.b = assoc.src;
						tri.c = assoc.dst;
					} else {
						matches.removeTail();
					}
				}
			}
		}
	}

	private int[] createFeatureLookup(Motion edgeAC, boolean srcAC, View viewC) {
		int[] table_C_to_A = new int[viewC.totalFeatures];
		Arrays.fill(table_C_to_A, 0, viewC.totalFeatures, -1);
		for (int i = 0; i < edgeAC.associated.size; i++) {
			AssociatedIndex assoc = edgeAC.associated.get(i);
			if (srcAC) {
				table_C_to_A[assoc.dst] = assoc.src;
			} else {
				table_C_to_A[assoc.src] = assoc.dst;
			}
		}
		return table_C_to_A;
	}

	private void estimateTrifocal() {

	}
}
