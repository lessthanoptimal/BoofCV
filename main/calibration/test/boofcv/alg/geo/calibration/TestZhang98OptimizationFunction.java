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

package boofcv.alg.geo.calibration;

import georegression.struct.point.Point2D_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestZhang98OptimizationFunction {

	Random rand = new Random(234);

	@Test
	public void estimate() {
		CalibrationGridConfig config = GenericCalibrationGrid.createStandardConfig();
		ParametersZhang98 param = GenericCalibrationGrid.createStandardParam(false, 2, 3, rand);

		double array[] = new double[ param.size() ];
		param.convertToParam(array);

		Zhang98OptimizationFunction alg =
				new Zhang98OptimizationFunction( new ParametersZhang98(2,3),config.computeGridPoints() );

		alg.setModel(array);
		double estimates[] = new double[ alg.getNumberOfFunctions() ];

		for( int i = 0; i < param.views.length; i++ ) {
			alg.estimate(i,estimates);

			// just a really crude test to see if it blows up or done nothing
			for( double d : estimates ) {
				assertFalse(Double.isInfinite(d) || Double.isNaN(d));
				assertTrue(d!=0);
			}
		}
	}

	@Test
	public void computeResiduals() {
		CalibrationGridConfig config = GenericCalibrationGrid.createStandardConfig();
		ParametersZhang98 param = GenericCalibrationGrid.createStandardParam(false, 2, 3, rand);

		double array[] = new double[ param.size() ];
		param.convertToParam(array);

		Zhang98OptimizationFunction alg =
				new Zhang98OptimizationFunction( new ParametersZhang98(2,3),config.computeGridPoints() );

		alg.setModel(array);
		double estimates[] = new double[ alg.getNumberOfFunctions() ];
		double residual[] = new double[ alg.getNumberOfFunctions() ];

		for( int i = 0; i < param.views.length; i++ ) {
			// compute perfect observations then added some error
			alg.estimate(i,estimates);
			List<Point2D_F64> obs = new ArrayList<Point2D_F64>();
			for( int j = 0; j < estimates.length; j += 2 ) {
				Point2D_F64 p = new Point2D_F64();
				p.x = estimates[j]+1;
				p.y = estimates[j+1]+2;
				obs.add(p);
			}

			// see if each element has the expected error
			alg.computeResiduals(obs,i,residual);

			for( int j = 0; j < residual.length; j += 2 ) {
				assertEquals(1,residual[j],1e-8);
				assertEquals(2,residual[j+1],1e-8);
			}
		}
	}

}
