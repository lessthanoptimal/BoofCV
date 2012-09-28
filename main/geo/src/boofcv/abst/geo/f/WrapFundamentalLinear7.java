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

package boofcv.abst.geo.f;

import boofcv.abst.geo.EpipolarMatrixEstimatorN;
import boofcv.alg.geo.f.FundamentalLinear7;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Wrapper around either {@link boofcv.alg.geo.f.FundamentalLinear7} for {@link boofcv.abst.geo.EpipolarMatrixEstimator}.
 *
 * @author Peter Abeles
 */
public class WrapFundamentalLinear7 implements EpipolarMatrixEstimatorN {
	FundamentalLinear7 alg;

	public WrapFundamentalLinear7(boolean fundamental) {
		alg = new FundamentalLinear7(fundamental);
	}

	@Override
	public boolean process(List<AssociatedPair> points) {
		return alg.process(points);
	}

	@Override
	public List<DenseMatrix64F> getSolutions() {
		return alg.getSolutions();
	}

	@Override
	public int getMinimumPoints() {
		return 7;
	}

	public FundamentalLinear7 getAlgorithm() {
		return alg;
	}
}
