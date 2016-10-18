/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.associate;

import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_F64;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * If multiple associations are found for a single source and/or destination feature then this ambiguity is
 * removed by selecting the association with the best score.  If there are multiple best scores for a single
 * feature index then there are no associations for that feature.
 *
 * @author Peter Abeles
 */
public class AssociateUniqueByScoreAlg {

	// type of match score being processed
	private MatchScoreType type;

	// should it check for ambiguity in source and/or destination
	private boolean checkSource;
	private boolean checkDestination;

	// storage for the index of the best match found so far
	private GrowQueue_I32 solutions = new GrowQueue_I32();
	private GrowQueue_F64 scores = new GrowQueue_F64();

	// storage for found solutions
	private FastQueue<AssociatedIndex> firstPass = new FastQueue<>(AssociatedIndex.class, false);
	// final output of pruned matches
	private FastQueue<AssociatedIndex> pruned = new FastQueue<>(AssociatedIndex.class, false);

	/**
	 * Configures algorithm.
	 *
	 * @param type Used to determine which score is better
	 * @param checkSource Should it check source features for uniqueness
	 * @param checkDestination Should it check destination features for uniqueness
	 */
	public AssociateUniqueByScoreAlg(MatchScoreType type,
									 boolean checkSource,
									 boolean checkDestination) {
		this.type = type;
		this.checkSource = checkSource;
		this.checkDestination = checkDestination;
	}

	/**
	 * Given a set of matches, enforce the uniqueness rules it was configured to.
	 *
	 * @param matches Set of matching features
	 * @param numSource Number of source features
	 * @param numDestination Number of destination features
	 */
	public void process( FastQueue<AssociatedIndex> matches , int numSource , int numDestination ) {

		if( checkSource ) {
			if( checkDestination ) {
				processSource(matches, numSource, firstPass);
				processDestination(firstPass,numDestination,pruned);
			} else {
				processSource(matches, numSource, pruned);
			}
		} else if( checkDestination ) {
			processDestination(matches,numDestination,pruned);
		} else {
			// well this was pointless, just return the input set
			pruned = matches;
		}
	}

	/**
	 * Selects a subset of matches that have at most one association for each source feature.
	 */
	private void processSource(FastQueue<AssociatedIndex> matches, int numSource,
							   FastQueue<AssociatedIndex> output ) {
		//set up data structures
		scores.resize(numSource);
		solutions.resize(numSource);
		for( int i =0; i < numSource; i++ ) {
			solutions.data[i] = -1;
		}

		// select best matches
		for( int i = 0; i < matches.size(); i++ ) {
			AssociatedIndex a = matches.get(i);
			int found = solutions.data[a.src];
			if( found != -1 ) {
				if( found == -2 ) {
					// the best solution is invalid because two or more had the same score, see if this is better
					double bestScore = scores.data[a.src];
					int result = type.compareTo(bestScore,a.fitScore);
					if( result < 0 ) {
						// found a better one, use this now
						solutions.data[a.src] = i;
						scores.data[a.src] = a.fitScore;
					}
				} else {
					// see if this new score is better than the current best
					AssociatedIndex currentBest = matches.get( found );
					int result = type.compareTo(currentBest.fitScore,a.fitScore);
					if( result < 0 ) {
						// found a better one, use this now
						solutions.data[a.src] = i;
						scores.data[a.src] = a.fitScore;
					} else if( result == 0 ) {
						// two solutions have the same score
						solutions.data[a.src] = -2;
					}
				}
			} else {
				// no previous match found
				solutions.data[a.src] = i;
				scores.data[a.src] = a.fitScore;
			}
		}

		output.reset();
		for( int i =0; i < numSource; i++ ) {
			int index = solutions.data[i];
			if( index >= 0 ) {
				output.add( matches.get(index) );
			}
		}
	}

	/**
	 * Selects a subset of matches that have at most one association for each destination feature.
	 */
	private void processDestination(FastQueue<AssociatedIndex> matches, int numDestination,
									FastQueue<AssociatedIndex> output ) {
		//set up data structures
		scores.resize(numDestination);
		solutions.resize(numDestination);
		for( int i =0; i < numDestination; i++ ) {
			solutions.data[i] = -1;
		}

		// select best matches
		for( int i = 0; i < matches.size(); i++ ) {
			AssociatedIndex a = matches.get(i);
			int found = solutions.data[a.dst];
			if( found != -1 ) {
				if( found == -2 ) {
					// the best solution is invalid because two or more had the same score, see if this is better
					double bestScore = scores.data[a.dst];
					int result = type.compareTo(bestScore,a.fitScore);
					if( result < 0 ) {
						// found a better one, use this now
						solutions.data[a.dst] = i;
						scores.data[a.dst] = a.fitScore;
					}
				} else {
					// see if this new score is better than the current best
					AssociatedIndex currentBest = matches.get( found );
					int result = type.compareTo(currentBest.fitScore,a.fitScore);
					if( result < 0 ) {
						// found a better one, use this now
						solutions.data[a.dst] = i;
						scores.data[a.dst] = a.fitScore;
					} else if( result == 0 ) {
						// two solutions have the same score
						solutions.data[a.dst] = -2;
					}
				}
			} else {
				// no previous match found
				solutions.data[a.dst] = i;
				scores.data[a.dst] = i;
			}
		}

		output.reset();
		for( int i =0; i < numDestination; i++ ) {
			int index = solutions.data[i];
			if( index >= 0 ) {
				output.add( matches.get(index) );
			}
		}
	}

	public FastQueue<AssociatedIndex> getMatches() {
		return pruned;
	}

	public boolean checkSource() {
		return checkSource;
	}

	public boolean checkDestination() {
		return checkDestination;
	}
}
