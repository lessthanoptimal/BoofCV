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

import boofcv.numerics.optimization.functions.FunctionNtoM;
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
public class Zhang98OptimizationFunction implements FunctionNtoM {

	int N,M;
	
	// description of the calibration grid
	List<Point3D_F64> grid = new ArrayList<Point3D_F64>();
	// optimization parameters
	ParametersZhang98 param;
	
	// should it assume the skew parameter is zero?
	private boolean assumeZeroSkew;

	// variables for storing intermediate results
	Se3_F64 se = new Se3_F64();

	Point3D_F64 cameraPt = new Point3D_F64();
	Point2D_F64 calibratedPt = new Point2D_F64();

	// observations
	List<List<Point2D_F64>> observations;

	/**
	 * Configurations the optimization function.
	 *
	 * @param param Storage for calibration parameters. Effectively specifies the number of target views
	 * and radial terms
	 * @param grid Location of points on the calibration grid.  z=0
	 */
	public Zhang98OptimizationFunction( ParametersZhang98 param ,
										boolean assumeZeroSkew,
										List<Point2D_F64> grid ,
										List<List<Point2D_F64>> observations ) {
		if( param.views.length != observations.size() )
			throw new IllegalArgumentException("For each view there should be one observation");

		this.param = param;
		this.observations = observations;
		this.assumeZeroSkew = assumeZeroSkew;

		for( Point2D_F64 p : grid ) {
			this.grid.add( new Point3D_F64(p.x,p.y,0) );
		}

		N = assumeZeroSkew ? param.size() -1 : param.size();
		M = observations.size()*grid.size()*2;
	}

	@Override
	public int getN() {
		return N;
	}

	@Override
	public int getM() {
		return M;
	}

	@Override
	public void process(double[] input, double[] output) {
		param.setFromParam(assumeZeroSkew,input);
		
		int index = 0;
		for( int indexView = 0; indexView < param.views.length; indexView++ ) {
			
			ParametersZhang98.View v = param.views[indexView];
			
			RotationMatrixGenerator.rodriguesToMatrix(v.rotation,se.getR());
			se.T = v.T;

			List<Point2D_F64> obs = observations.get(indexView);
			
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

				Point2D_F64 p = obs.get(i);
				
				output[index++] = p.x-x;
				output[index++] = p.y-y;
			}
		}
	}


}
