/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.calibration;

import boofcv.numerics.optimization.OptimizationFunction;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Function for performing non-linear optimization on the Zhang98 calibration parameters.
 *
 * @author Peter Abeles
 */
public class Zhang98OptimizationFunction implements OptimizationFunction<List<Point2D_F64>,Integer> {

	// description of the calibration grid
	List<Point3D_F64> grid = new ArrayList<Point3D_F64>();
	// optimization parameters
	ParametersZhang98 param;

	// variables for storing intermediate results
	Se3_F64 se = new Se3_F64();

	Point3D_F64 cameraPt = new Point3D_F64();
	Point2D_F64 calibratedPt = new Point2D_F64();
	double[] estimated;

	/**
	 * Configurations the optimization function.
	 *
	 * @param param Storage for calibration parameters. Effectively specifies the number of target views
	 * and radial terms
	 * @param grid Location of points on the calibration grid.  z=0
	 */
	public Zhang98OptimizationFunction( ParametersZhang98 param , List<Point2D_F64> grid ) {
		this.param = param;

		for( Point2D_F64 p : grid ) {
			this.grid.add( new Point3D_F64(p.x,p.y,0) );
		}

		estimated = new double[grid.size()*2];
	}

	@Override
	public void setModel(double[] model) {
		param.setFromParam(model);
	}

	@Override
	public int getNumberOfFunctions() {
		return estimated.length;
	}

	@Override
	public int getModelSize() {
		return param.size();
	}

	@Override
	public boolean estimate(Integer viewIndex, double[] estimated) {
		ParametersZhang98.View v = param.views[viewIndex];

		RotationMatrixGenerator.rodriguesToMatrix(v.rotation,se.getR());
		se.T = v.T;

		int index = 0;
		for( int i = 0; i < grid.size(); i++ ) {
			// Put the point in the camera's reference frame
			SePointOps_F64.transform(se,grid.get(i), cameraPt);

			// calibrated pixel coordinates
			calibratedPt.x = cameraPt.x/ cameraPt.z;
			calibratedPt.y = cameraPt.y/ cameraPt.z;

			// apply radial distortion
			CalibrationPlanarGridZhang98.applyDistortion(calibratedPt, param.distortion);

			// convert to pixel coordinates
			double x = param.a*calibratedPt.x + param.c*calibratedPt.y + param.x0;
			double y = param.b*calibratedPt.y + param.y0;

			estimated[index++] = x;
			estimated[index++] = y;
		}

		return true;
	}

	@Override
	public boolean computeResiduals(List<Point2D_F64> obs, Integer viewIndex, double[] residuals) {
		estimate(viewIndex,estimated);

		int index = 0;
		for( int i = 0; i < obs.size(); i++ ) {
			Point2D_F64 p = obs.get(i);
			residuals[index] = p.x - estimated[index++];
			residuals[index] = p.y - estimated[index++];
		}

		return true;
	}
}
