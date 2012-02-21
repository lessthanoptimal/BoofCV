/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.epipolar;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.HypothesisList;
import boofcv.numerics.fitting.modelset.ModelGenerator;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Wrapper around {@link FundamentalInterface} for {@link ModelGenerator}.  Used for robust model
 * fitting with outliers.
 * 
 * @author Peter Abeles
 */
public class FundamentalModelGenerator implements ModelGenerator<DenseMatrix64F,AssociatedPair> {

	FundamentalInterface alg;

	public FundamentalModelGenerator(FundamentalInterface alg) {
		this.alg = alg;
	}

	@Override
	public DenseMatrix64F createModelInstance() {
		return new DenseMatrix64F(3,3);
	}

	@Override
	public void generate(List<AssociatedPair> dataSet, HypothesisList<DenseMatrix64F> models) {
		if( alg.process(dataSet) ) {
			DenseMatrix64F found = alg.getF();
			models.pop().set(found);
		}
	}

	@Override
	public int getMinimumPoints() {
		return alg.getMinPoints();
	}
}
