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

package boofcv.alg.geo.epipolar.pose;

import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import georegression.struct.point.Point3D_F64;

import java.util.List;

/**
 * <p>
 * Jacobian used to optimize EPnP.
 * <p>
 * <p>
 * Seems to produce slightly slower results in EPnP benchmarks without increasing the quality.
 * More rigorous testing is needed to see if this or numerical Jacobian should be used.
 * </p>
 *
 * @author Peter Abeles
 */
public class JacobianEPnP implements FunctionNtoMxN {

	double max;
	int m;
	int numControl;
	protected List<Point3D_F64> cameraPts;
	protected List<Point3D_F64> nullPts[];

	/**
	 * Make sure residuals have been initialized first before calling this
	 */
	public void setUp( ResidualsEPnP residuals ) {
		max = residuals.max;
		numControl = residuals.numControl;
		cameraPts = residuals.cameraPts;
		nullPts = residuals.nullPts;
		m = residuals.worldDistances.length;
	}

	@Override
	public int getN() {
		return 4;
	}

	@Override
	public int getM() {
		return m;
	}

	@Override
	public void process(double[] input, double[] output) {
		UtilLepetitEPnP.computeCameraControl(input,nullPts, cameraPts);

		double max2=max/2.0;
		
		int row = 0;
		for( int i = 0; i < numControl; i++ ) {
			Point3D_F64 ci = cameraPts.get(i);
			for( int j = i+1; j < numControl; j++ , row++) {
				int index = row*numControl;
				Point3D_F64 cj = cameraPts.get(j);

				double dcx = ci.x-cj.x;
				double dcy = ci.y-cj.y;
				double dcz = ci.z-cj.z;

				for( int k = 0; k < 4; k++ ) {
					Point3D_F64 ni = nullPts[k].get(i);
					Point3D_F64 nj = nullPts[k].get(j);

					double ncx = ni.x-nj.x;
					double ncy = ni.y-nj.y;
					double ncz = ni.z-nj.z;

					output[index++] = (dcx*ncx + dcy*ncy + dcz*ncz)/max2;
				}
			}
		}
	}
}
