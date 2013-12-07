/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.triangulate;

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.optimization.functions.FunctionNtoM;

import java.util.List;

/**
 * Basic error function for triangulation which only computes the residual between predicted and
 * actual observed point location.  Does not take in account the epipolar constraints.
 * 
 * @author Peter Abeles
 */
public class ResidualsTriangulateSimple implements FunctionNtoM  {

	// observations of the same feature in normalized coordinates
	private List<Point2D_F64> observations;
	// Known camera motion
	private List<Se3_F64> motionGtoC;

	private Point3D_F64 point = new Point3D_F64();
	private Point3D_F64 transformed = new Point3D_F64();

	/**
	 * Configures inputs.
	 *
	 * @param observations Observations of the feature at different locations. Normalized image coordinates.
	 * @param motionGtoC Camera motion from global to camera frame..
	 */
	public void setObservations( List<Point2D_F64> observations , List<Se3_F64> motionGtoC ) {
		if( observations.size() != motionGtoC.size() )
			throw new IllegalArgumentException("Different size lists");

		this.observations = observations;
		this.motionGtoC = motionGtoC;
	}
	
	@Override
	public int getNumOfInputsN() {
		return 3;
	}

	@Override
	public int getNumOfOutputsM() {
		return observations.size();
	}

	@Override
	public void process(double[] input, double[] output) {

		point.x = input[0];
		point.y = input[1];
		point.z = input[2];

		for( int i = 0; i < observations.size(); i++ ) {
			Point2D_F64 p = observations.get(i);
			Se3_F64 m = motionGtoC.get(i);

			SePointOps_F64.transform(m,point,transformed);
			
			double dx = p.x-transformed.x/transformed.z;
			double dy = p.y-transformed.y/transformed.z;

			output[i] = dx*dx + dy*dy;
		}
	}
}
