/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.GrowQueue_I32;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.MatchScoreType;

import java.util.ArrayList;
import java.util.List;

/**
 * If multiple solutions are returned then the best the ambiguity is resolved by selecting the association
 * with the best score.  If multiple scores are identical then the one is arbitrarily selected.
 *
 * @author Peter Abeles
 */
public class AssociateUniqueByScore {

	// type of match score being processed
	MatchScoreType type;

	// should it check for ambiguity
	boolean checkSource;
	boolean checkDestination;

	// storage for the index of the best match found so far
	GrowQueue_I32 solutions = new GrowQueue_I32();

	// storage for found solutions
	List<AssociatedIndex> firstPass = new ArrayList<AssociatedIndex>();
	List<AssociatedIndex> pruned = new ArrayList<AssociatedIndex>();

	public void process( List<AssociatedIndex> matches , int numSource , int numDestination ) {

		if( checkSource ) {
			processSource(matches, numSource, firstPass);
			if( checkDestination ) {
				processDestination(firstPass,numDestination,pruned);
			}
		} else if( checkDestination ) {
			processDestination(matches,numDestination,pruned);
		}
	}

	private void processSource(List<AssociatedIndex> matches, int numSource,
							   List<AssociatedIndex> output ) {
		//set up data structures
		solutions.resize(numSource);
		for( int i =0; i < numSource; i++ ) {
			solutions.data[i] = -1;
		}

		// select best matches
		for( int i = 0; i < matches.size(); i++ ) {
			AssociatedIndex a = matches.get(i);
			int found = solutions.data[a.src];
			if( found != -1 ) {
				AssociatedIndex currentBest = matches.get( found );
				if( type.compareTo(currentBest.fitScore,a.fitScore) < 0 ) {
					solutions.data[a.src] = i;
				}
			} else {
				solutions.data[a.src] = i;
			}
		}

		output.clear();
		for( int i =0; i < numSource; i++ ) {
			int index = solutions.data[i];
			if( index != -1 ) {
				output.add( matches.get(index) );
			}
		}
	}

	private void processDestination(List<AssociatedIndex> matches, int numDestination,
									List<AssociatedIndex> output ) {
		//set up data structures
		solutions.resize(numDestination);
		for( int i =0; i < numDestination; i++ ) {
			solutions.data[i] = -1;
		}

		// select best matches
		for( int i = 0; i < matches.size(); i++ ) {
			AssociatedIndex a = matches.get(i);
			int found = solutions.data[a.dst];
			if( found != -1 ) {
				AssociatedIndex currentBest = matches.get( found );
				if( type.compareTo(currentBest.fitScore,a.fitScore) < 0 ) {
					solutions.data[a.dst] = i;
				}
			} else {
				solutions.data[a.dst] = i;
			}
		}

		output.clear();
		for( int i =0; i < numDestination; i++ ) {
			int index = solutions.data[i];
			if( index != -1 ) {
				output.add( matches.get(index) );
			}
		}
	}

	public List<AssociatedIndex> getMatches() {
		return pruned;
	}

}
