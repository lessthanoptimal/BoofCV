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

package boofcv.numerics.solver.impl;

import boofcv.numerics.solver.GeneralPolynomialRootReal;
import boofcv.numerics.solver.Polynomial;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class TestFindRealRootsSturm extends GeneralPolynomialRootReal {


	@Override
	public List<Double> computeRealRoots(Polynomial poly) {
		FindRealRootsSturm alg = new FindRealRootsSturm(poly.size,Double.POSITIVE_INFINITY,1e-16,500);

		alg.process(poly);

		int N = alg.getNumberOfRoots();
		double[] roots = alg.getRoots();

		List<Double> ret = new ArrayList<Double>();
		for( int i = 0; i < N; i++ )
			ret.add(roots[i]);

		return ret;
	}
}
