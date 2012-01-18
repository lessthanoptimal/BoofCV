/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.numerics.optimization;

/**
 *
 * <p>
 * Wolfe condition<br>
 * &phi;(&alpha;) &le; &phi;(0) + &mu;&alpha;&phi;'(0)<br>
 * | &phi;'(&alpha;)| &le; &eta; |&phi;'(0)|<br>
 * wher
 *
 * <p>
 * [1] Jorge J. More and David J. Thuente, "Line Search Algorithms with Guaranteed Sufficient Decrease"
 * ACM Transactions of Mathematical Software, Vol 20 , No. 3, September 1994, Pages 286-307
 * </p>
 * @author Peter Abeles
 */
public class LineSearchMore94 extends CommonLineSearch {

	double ftol;
	double gtol;
	double stepMin;
	double stepMax;

	@Override
	public void init(double funcZero, double derivZero, double funcInit, double stepInit ) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean iterate() throws OptimizationException {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public double getStep() {
		return 0;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public String getWarning() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
