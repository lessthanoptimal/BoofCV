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

import boofcv.numerics.optimization.functions.FunctionNtoM;
import georegression.struct.point.Point3D_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * Function used to optimize EPnP beta values.  Minimizes the difference between world and camera
 * control point distances by adjusting the values of beta.
 *
 * @author Peter Abeles
 */
public class ResidualsEPnP implements FunctionNtoM  {
	
	// number of control points
	protected int numControl;
	
	protected List<Point3D_F64> cameraPts;
	protected List<Point3D_F64> nullPts[];

	// precomputed distances between world coordinates
	protected double worldDistances[] = new double[6];
	// max distance in world coordinates, used to normalize points an avoid overflow
	// probably not needed...
	protected double max;

	public void setParameters( List<Point3D_F64> worldPts, List<Point3D_F64> nullPts[] ) {
		numControl = worldPts.size();

		int index = 0;
		max = 0;
		for( int i = 0; i < numControl; i++ ) {
			Point3D_F64 wi = worldPts.get(i);
			for( int j = i+1; j < numControl; j++ , index++ ) {
				worldDistances[index] = wi.distance2(worldPts.get(j));
				max = Math.max(worldDistances[index],max);
			}
		}

		for( int i = 0; i < index; i++ )
			worldDistances[i] /= max;

		// declare camera points
		cameraPts = new ArrayList<Point3D_F64>(numControl);
		for( int i = 0; i < numControl; i++ )
			cameraPts.add( new Point3D_F64() );

		// save reference to null points
		this.nullPts = nullPts;
	}

	public List<Point3D_F64> getCameraPts() {
		return cameraPts;
	}

	@Override
	public int getN() {
		return 4;
	}

	@Override
	public int getM() {
		return worldDistances.length;
	}

	@Override
	public void process(double[] input, double[] output) {
		UtilLepetitEPnP.computeCameraControl(input,nullPts, cameraPts);

		int index = 0;
		for( int i = 0; i < numControl; i++ ) {
			Point3D_F64 ci = cameraPts.get(i);
			for( int j = i+1; j < numControl; j++ , index++) {
				double d = ci.distance2(cameraPts.get(j))/max;
				output[index] = d - worldDistances[index];
			}
		}
	}
}
