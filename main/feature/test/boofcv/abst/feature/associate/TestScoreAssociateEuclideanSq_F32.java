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
import boofcv.struct.feature.TupleDesc_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestScoreAssociateEuclideanSq_F32 extends StandardScoreAssociationChecks<TupleDesc_F32> {

	public TestScoreAssociateEuclideanSq_F32() {
		super(MatchScoreType.NORM_ERROR);
	}

	@Override
	public ScoreAssociation<TupleDesc_F32> createScore() {
		return new ScoreAssociateEuclideanSq_F32();
	}

	@Override
	public TupleDesc_F32 createDescription() {
		TupleDesc_F32 a = new TupleDesc_F32(5);
		for( int i = 0; i < a.size(); i++ )
			a.value[i] = rand.nextFloat()*2;

		return a;
	}

	@Test
	public void compareToExpected() {
		ScoreAssociateEuclideanSq_F32 score = new ScoreAssociateEuclideanSq_F32();

		TupleDesc_F32 a = new TupleDesc_F32(5);
		TupleDesc_F32 b = new TupleDesc_F32(5);

		a.value=new float[]{1,2,3,4,5};
		b.value=new float[]{2,-1,7,-8,10};

		assertEquals(195,score.score(a,b),1e-4);
	}
}
