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

package boofcv.abst.feature.associate;

import boofcv.struct.feature.MatchScoreType;
import boofcv.struct.feature.TupleDesc_F64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestScoreAssociateCorrelation extends StandardScoreAssociationChecks<TupleDesc_F64> {

	public TestScoreAssociateCorrelation() {
		super(MatchScoreType.CORRELATION);
	}

	@Override
	public ScoreAssociation<TupleDesc_F64> createScore() {
		return new ScoreAssociateCorrelation();
	}

	@Override
	public TupleDesc_F64 createDescription() {
		TupleDesc_F64 a = new TupleDesc_F64(5);
		for( int i = 0; i < a.size(); i++ )
			a.data[i] = rand.nextGaussian()*2;

		return a;
	}

	@Test void compareToExpected() {
		ScoreAssociateCorrelation score = new ScoreAssociateCorrelation();

		TupleDesc_F64 a = new TupleDesc_F64(5);
		TupleDesc_F64 b = new TupleDesc_F64(5);

		a.data =new double[]{1,2,3,4,5};
		b.data =new double[]{2,-1,7,-8,10};

		assertEquals(-39,score.score(a,b),1e-2);
	}
}
