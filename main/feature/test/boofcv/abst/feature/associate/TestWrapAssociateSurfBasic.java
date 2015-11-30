/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.factory.feature.associate.FactoryAssociation;
import boofcv.struct.feature.BrightFeature;
import boofcv.struct.feature.TupleDesc_F64;

import java.util.Random;

/**
 * Standard tests
 *
 * @author Peter Abeles
 */
public class TestWrapAssociateSurfBasic extends StandardAssociateDescriptionChecks<BrightFeature> {

	Random rand = new Random(234);

	public TestWrapAssociateSurfBasic() {
		super(BrightFeature.class);
	}

	@Override
	public AssociateDescription<BrightFeature> createAlg() {
		ScoreAssociation<TupleDesc_F64> score = new ScoreAssociateEuclidean_F64();
		AssociateDescription<TupleDesc_F64> assoc = FactoryAssociation.greedy(score, Double.MAX_VALUE, false);

		AssociateSurfBasic basic = new AssociateSurfBasic(assoc);

		return new WrapAssociateSurfBasic(basic);
	}

	@Override
	protected BrightFeature c(double value) {
		BrightFeature s = new BrightFeature(1);
		s.value[0] = value;
		return s;
	}
}
