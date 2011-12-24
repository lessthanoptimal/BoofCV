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

package boofcv.alg.geo.calibration;

import georegression.struct.point.Vector3D_F64;
import georegression.struct.so.Rodrigues;

/**
 * Parameters for batch optimization.
 *
 * @author Peter Abeles
 */
public class ParametersZhang98 {
	// camera calibration matrix
	public double a,b,c,x0,y0;
	// radial distortion
	public double distortion[];

	// position of each view of the target
	public View[] views;

	public ParametersZhang98( int numDistort , int numViews ) {
		distortion = new double[numDistort];
		setNumberOfViews(numViews);
	}

	public ParametersZhang98( int numDistort ) {
		distortion = new double[numDistort];
	}

	public ParametersZhang98() {
	}

	public void setNumberOfViews( int numViews ) {
			views = new View[numViews];
		for( int i = 0; i < numViews; i++ ) {
			views[i] = new View();
		}
	}

	public ParametersZhang98 createNew() {
		return new ParametersZhang98(distortion.length,views.length);
	}

	public ParametersZhang98 copy() {
		ParametersZhang98 ret = createNew();
		ret.a = a;
		ret.b = b;
		ret.c = c;
		ret.x0 = x0;
		ret.y0 = y0;

		for( int i = 0; i < distortion.length; i++ ) {
			ret.distortion[i] = distortion[i];
		}

		for( int i = 0; i < views.length; i++ ) {
			View a = views[i];
			View b = ret.views[i];

			b.rotation.unitAxisRotation.set(a.rotation.unitAxisRotation);
			b.rotation.theta = a.rotation.theta;
			b.T.set(a.T);
		}

		return ret;
	}

	public static class View
	{
		// description of rotation
		public Rodrigues rotation = new Rodrigues();
		// translation
		public Vector3D_F64 T = new Vector3D_F64();
	}

	public int size() {
		return 5+distortion.length+(4+3)*views.length;
	}

	public void setFromParam( double param[] ) {
		a = param[0];
		b = param[1];
		c = param[2];
		x0 = param[3];
		y0 = param[4];

		int index = 5;
		for( int i = 0; i < distortion.length; i++ ) {
			distortion[i] = param[index++];
		}

		for( View v : views ) {
			v.rotation.theta = param[index++];
			v.rotation.unitAxisRotation.x = param[index++];
			v.rotation.unitAxisRotation.y = param[index++];
			v.rotation.unitAxisRotation.z = param[index++];
			v.T.x = param[index++];
			v.T.y = param[index++];
			v.T.z = param[index++];
		}
	}

	public void convertToParam( double param[] ) {
		param[0] = a;
		param[1] = b;
		param[2] = c;
		param[3] = x0;
		param[4] = y0;

		int index = 5;
		for( int i = 0; i < distortion.length; i++ ) {
			param[index++] = distortion[i];
		}

		for( View v : views ) {
			param[index++] = v.rotation.theta;
			param[index++] = v.rotation.unitAxisRotation.x;
			param[index++] = v.rotation.unitAxisRotation.y;
			param[index++] = v.rotation.unitAxisRotation.z;
			param[index++] = v.T.x;
			param[index++] = v.T.y;
			param[index++] = v.T.z;
		}
	}
}
