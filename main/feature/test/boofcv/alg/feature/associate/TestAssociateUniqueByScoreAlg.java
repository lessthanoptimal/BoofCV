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
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAssociateUniqueByScoreAlg {

	FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class, true);

	@Test
	public void checkSource() {
		AssociateUniqueByScoreAlg alg = new AssociateUniqueByScoreAlg(MatchScoreType.NORM_ERROR,true,false);

		// all unique matches, no change
		matches.reset();
		add(0,0,5);
		add(1,2,4);
		add(3,1,6);
		alg.process(matches,5,6);
		assertEquals(3,alg.getMatches().size);

		// one good match
		matches.reset();
		add(0, 0, 5);
		add(0, 1, 6);
		alg.process(matches,5,6);
		assertEquals(1,alg.getMatches().size);
		assertEquals(0,alg.getMatches().get(0).dst);

		// two equal matches, ignore results
		matches.reset();
		add(0, 0, 5);
		add(0, 1, 5);
		alg.process(matches,5,6);
		assertEquals(0, alg.getMatches().size);
	}

	@Test
	public void checkDestination() {
		AssociateUniqueByScoreAlg alg = new AssociateUniqueByScoreAlg(MatchScoreType.NORM_ERROR,false,true);

		// all unique matches, no change
		matches.reset();
		add(0,0,5);
		add(2,1,4);
		add(1,3,6);
		alg.process(matches,5,6);
		assertEquals(3,alg.getMatches().size);

		// one good match
		matches.reset();
		add(0, 0, 5);
		add(1, 0, 6);
		alg.process(matches,5,6);
		assertEquals(1,alg.getMatches().size);
		assertEquals(0,alg.getMatches().get(0).src);

		// two equal matches, ignore results
		matches.reset();
		add(0, 0, 5);
		add(1, 0, 5);
		alg.process(matches,5,6);
		assertEquals(0,alg.getMatches().size);
	}

	@Test
	public void checkBoth() {
		AssociateUniqueByScoreAlg alg = new AssociateUniqueByScoreAlg(MatchScoreType.NORM_ERROR,true,true);

		// all unique matches, no change
		matches.reset();
		add(0,0,5);
		add(1,2,4);
		add(3,1,6);
		alg.process(matches,5,6);
		assertEquals(3,alg.getMatches().size);

		// one good match in each direction
		matches.reset();
		add(0, 0, 5);
		add(0, 1, 6);
		add(1, 0, 7);
		alg.process(matches,5,6);
		assertEquals(1,alg.getMatches().size);
		assertEquals(0,alg.getMatches().get(0).src);
		assertEquals(0,alg.getMatches().get(0).dst);

		// two equal matches, ignore results
		matches.reset();
		add(0, 0, 5);
		add(0, 1, 5);
		add(2, 2, 7);
		add(3, 2, 7);
		alg.process(matches,5,6);
		assertEquals(0,alg.getMatches().size);
	}

	private void add( int src , int dst , double score ) {
		matches.grow().setAssociation(src,dst,score);
	}
}
