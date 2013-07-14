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
import boofcv.struct.feature.NccFeature;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestScoreAssociateNccFeature extends StandardScoreAssociationChecks<NccFeature> {

	public TestScoreAssociateNccFeature() {
		super(MatchScoreType.CORRELATION);
	}

	@Override
	public ScoreAssociation<NccFeature> createScore() {
		return new ScoreAssociateNccFeature();
	}

	@Override
	public NccFeature createDescription() {
		NccFeature a = new NccFeature(5);
		a.mean = 10;
		a.sigma = 5;
		for( int i = 0; i < a.size(); i++ )
			a.value[i] = rand.nextGaussian()*2;

		return a;
	}

	@Test
	public void compareToExpected() {
		ScoreAssociateNccFeature scorer = new ScoreAssociateNccFeature();

		NccFeature a = new NccFeature(5);
		NccFeature b = new NccFeature(5);

		a.sigma =12;
		b.sigma =7;
		a.value=new double[]{1,2,3,4,5};
		b.value=new double[]{2,-1,7,-8,10};

		assertEquals(-0.46429/5.0,scorer.score(a,b),1e-2);
	}
}
