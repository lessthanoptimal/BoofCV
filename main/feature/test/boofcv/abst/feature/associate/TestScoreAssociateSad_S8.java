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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_S8;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateSad_S8 extends StandardScoreAssociationChecks<TupleDesc_S8> {

	public TestScoreAssociateSad_S8() {
		super(MatchScoreType.NORM_ERROR);
	}

	@Override
	public ScoreAssociation<TupleDesc_S8> createScore() {
		return new ScoreAssociateSad_S8();
	}

	@Override
	public TupleDesc_S8 createDescription() {
		TupleDesc_S8 a = new TupleDesc_S8(5);
		for( int i = 0; i < a.size(); i++ )
			a.value[i] = (byte)(rand.nextInt(200)-100);

		return a;
	}

	@Test
	public void basic() {
		ScoreAssociateSad_S8 scorer = new ScoreAssociateSad_S8();

		TupleDesc_S8 a = new TupleDesc_S8(3);
		TupleDesc_S8 b = new TupleDesc_S8(3);

		a.value=new byte[]{-5,2,120};
		b.value=new byte[]{56,2,-30};

		assertEquals(211,scorer.score(a,b),1e-2);
	}
}
