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

package boofcv.alg.shapes.corner;

import boofcv.struct.PointGradient_F64;
import org.ddogleg.optimization.DerivativeChecker;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCornerFitGradient_F64 {
	@Test
	public void compareToNumerical() {
		FastQueue<PointGradient_F64> points = new FastQueue<PointGradient_F64>(PointGradient_F64.class, true);

		points.grow().set(1, 1, -1, 1);
		points.grow().set(2, 2, -1, 1);
		points.grow().set(5, 4, -2, 0.5);

		CornerFitFunction_F64 func = new CornerFitFunction_F64();
		CornerFitGradient_F64 gradient = new CornerFitGradient_F64();

		func.setPoints(points.toList());
		gradient.setPoints(points.toList());

		assertTrue(DerivativeChecker.gradient(func,gradient,new double[]{0.1,2},1e-6));
		assertTrue(DerivativeChecker.gradient(func,gradient,new double[]{5,5},1e-6));
	}

}
