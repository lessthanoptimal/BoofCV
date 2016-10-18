/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestZhang99OptimizationFunction {

	Random rand = new Random(234);

	/**
	 * Give it perfect observations and see if the residuals are all zero
	 */
	@Test
	public void computeResidualsPerfect() {
		Zhang99ParamAll param = GenericCalibrationGrid.createStandardParam(false, 2, true, 3, rand);

		double array[] = new double[ param.numParameters() ];
		param.convertToParam(array);
		
		List<Point2D_F64> gridPts = GenericCalibrationGrid.standardLayout();

		List<CalibrationObservation> observations = new ArrayList<>();

		for( int i = 0; i < param.views.length; i++ ) {
			observations.add( estimate(param,param.views[i],gridPts));
		}

		Zhang99OptimizationFunction alg =
				new Zhang99OptimizationFunction( new Zhang99ParamAll(false,2,true,3),gridPts,observations );

		double residuals[] = new double[ alg.getNumOfOutputsM()];
		for( int i = 0; i < residuals.length; i++ )
			residuals[i] = 1;
		
		alg.process(array,residuals);
		
		for( double r : residuals ) {
			assertEquals(0,r,1e-8);
		}
	}
	
	protected static CalibrationObservation estimate( Zhang99ParamAll param ,
													  Zhang99ParamAll.View v ,
													  List<Point2D_F64> grid ) {

		CalibrationObservation ret = new CalibrationObservation();
		
		Se3_F64 se = new Se3_F64();
		Point3D_F64 cameraPt = new Point3D_F64();
		Point2D_F64 calibratedPt = new Point2D_F64();

		ConvertRotation3D_F64.rodriguesToMatrix(v.rotation, se.getR());
		se.T = v.T;

		for( int i = 0; i < grid.size(); i++ ) {
			Point2D_F64 gridPt = grid.get(i);
			
			// Put the point in the camera's reference frame
			SePointOps_F64.transform(se, new Point3D_F64(gridPt.x,gridPt.y,0), cameraPt);

			// calibrated pixel coordinates
			calibratedPt.x = cameraPt.x/ cameraPt.z;
			calibratedPt.y = cameraPt.y/ cameraPt.z;

			// apply radial distortion
			CalibrationPlanarGridZhang99.applyDistortion(calibratedPt, param.radial,param.t1,param.t2);

			// convert to pixel coordinates
			double x = param.a*calibratedPt.x + param.c*calibratedPt.y + param.x0;
			double y = param.b*calibratedPt.y + param.y0;
			
			ret.add( new Point2D_F64(x,y), i);
		}

		return ret;
	}

	/**
	 * Have there only be partial observations of the fiducial
	 */
	@Test
	public void computeResidualsPerfect_partial() {
		Zhang99ParamAll param = GenericCalibrationGrid.createStandardParam(false, 2, true, 3, rand);

		double array[] = new double[ param.numParameters() ];
		param.convertToParam(array);

		List<Point2D_F64> gridPts = GenericCalibrationGrid.standardLayout();

		List<CalibrationObservation> observations = new ArrayList<>();

		for( int i = 0; i < param.views.length; i++ ) {
			CalibrationObservation set = estimate(param, param.views[i], gridPts);
			observations.add( set );

			for (int j = 0; j < 5; j++) {
				set.points.remove(i * 2 + j);
			}
		}

		int expectedM = 0;
		for (int i = 0; i < observations.size(); i++) {
			expectedM += observations.get(i).size()*2;
		}

		Zhang99OptimizationFunction alg =
				new Zhang99OptimizationFunction( new Zhang99ParamAll(false,2,true,3),gridPts,observations );

		assertEquals(expectedM,alg.getNumOfOutputsM());
		double residuals[] = new double[ alg.getNumOfOutputsM()];
		for( int i = 0; i < residuals.length; i++ )
			residuals[i] = 1;

		alg.process(array, residuals);

		for( double r : residuals ) {
			assertEquals(0,r,1e-8);
		}
	}

}
