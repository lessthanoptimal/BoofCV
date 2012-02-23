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
import boofcv.alg.geo.d3.epipolar.FundamentalLinear7;
import boofcv.alg.geo.d3.epipolar.FundamentalLinear8;
import org.ejml.data.DenseMatrix64F;

import java.util.List;

/**
 * Wrapper around either {@link FundamentalLinear8} or {@link FundamentalLinear7}
 * for {@link EpipolarMatrixEstimator}.
 * 
 * @author Peter Abeles
 */
public class WrapFundamentalLinear implements EpipolarMatrixEstimator {
	FundamentalLinear8 alg;
	int minPoints;

	public WrapFundamentalLinear( boolean fundamental , int minPoints ) {
		if( minPoints == 8 ) {
			alg = new FundamentalLinear8(fundamental);
		} else if( minPoints == 7 ) {
			alg = new FundamentalLinear7(fundamental);
		} else {
			throw new IllegalArgumentException("minPoints must be 7 or 8");
		}

		this.minPoints = minPoints;
	}

	@Override
	public boolean process(List<AssociatedPair> points) {
		return alg.process(points);
	}

	@Override
	public DenseMatrix64F getEpipolarMatrix() {
		return alg.getF();
	}

	@Override
	public int getMinPoints() {
		return minPoints;
	}
}
