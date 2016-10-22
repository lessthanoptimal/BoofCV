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
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.ArrayList;
import java.util.List;

/**
 * Function for performing non-linear optimization on the Zhang99 calibration parameters.
 *
 * @author Peter Abeles
 */
public class Zhang99OptimizationFunction implements FunctionNtoM {

	private int N,M;
	
	// description of the calibration grid
	private List<Point3D_F64> grid = new ArrayList<>();
	// optimization parameters
	private Zhang99ParamAll param;

	// variables for storing intermediate results
	private Se3_F64 se = new Se3_F64();

	private Point3D_F64 cameraPt = new Point3D_F64();
	private Point2D_F64 normPt = new Point2D_F64();

	// observations
	private List<CalibrationObservation> observations;

	/**
	 * Configurations the optimization function.
	 *
	 * @param param Storage for calibration parameters. Effectively specifies the number of target views
	 * and radial terms
	 * @param grid Location of points on the calibration grid.  z=0
	 * @param observations calibration point observation pixel coordinates
	 */
	public Zhang99OptimizationFunction(Zhang99ParamAll param,
									   List<Point2D_F64> grid,
									   List<CalibrationObservation> observations) {
		if( param.views.length != observations.size() )
			throw new IllegalArgumentException("For each view there should be one observation");

		this.param = param;
		this.observations = observations;

		for( Point2D_F64 p : grid ) {
			this.grid.add( new Point3D_F64(p.x,p.y,0) );
		}

		N = param.numParameters();
		M = CalibrationPlanarGridZhang99.totalPoints(observations)*2;
	}

	@Override
	public int getNumOfInputsN() {
		return N;
	}

	@Override
	public int getNumOfOutputsM() {
		return M;
	}

	@Override
	public void process(double[] input, double[] output) {
		param.setFromParam(input);

		process(param,output);
	}

	public void process( Zhang99ParamAll param , double []residuals ) {
		int index = 0;
		for( int indexView = 0; indexView < param.views.length; indexView++ ) {

			Zhang99ParamAll.View v = param.views[indexView];

			ConvertRotation3D_F64.rodriguesToMatrix(v.rotation,se.getR());
			se.T = v.T;

			CalibrationObservation viewSet = observations.get(indexView);

			for( int i = 0; i < viewSet.size(); i++ ) {

				int gridIndex = viewSet.get(i).index;
				Point2D_F64 obs = viewSet.get(i);

				// Put the point in the camera's reference frame
				SePointOps_F64.transform(se,grid.get(gridIndex), cameraPt);

				// normalized image coordinates
				normPt.x = cameraPt.x/ cameraPt.z;
				normPt.y = cameraPt.y/ cameraPt.z;

				// apply distortion
				CalibrationPlanarGridZhang99.applyDistortion(normPt, param.radial, param.t1, param.t2);

				// convert to pixel coordinates
				double x = param.a * normPt.x + param.c * normPt.y + param.x0;
				double y = param.b * normPt.y + param.y0;

				residuals[index++] = x-obs.x;
				residuals[index++] = y-obs.y;
			}
		}
	}
}
