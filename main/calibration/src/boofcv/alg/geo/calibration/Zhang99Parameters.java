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

package boofcv.alg.geo.calibration;

import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.so.Rodrigues_F64;

/**
 * <p>
 * Parameters for batch optimization.<br>
 * <br>
 * Calibration matrix = [ a c x0 ; 0 b y0; 0 0 1];
 * </p>
 * Same as in the paper, but with the addition of tangental distortion terms.
 *
 * @author Peter Abeles
 */
public class Zhang99Parameters {
	// camera calibration matrix
	public double a,b,c,x0,y0;
	// radial distortion
	public double radial[];
	// tangential distortion
	public double t1,t2;

	// does it assume c = 0?
	public boolean assumeZeroSkew;
	// should it estimate the tangetial terms?
	public boolean includeTangential;

	// position of each view of the target
	// target to camera transform
	public View[] views;

	public Zhang99Parameters(boolean assumeZeroSkew ,
							 int numRadial, boolean includeTangential,
							 int numViews)
	{
		this.assumeZeroSkew = assumeZeroSkew;
		radial = new double[numRadial];
		this.includeTangential = includeTangential;
		setNumberOfViews(numViews);
	}

	public Zhang99Parameters(boolean assumeZeroSkew , int numRadial, boolean includeTangential) {
		this.assumeZeroSkew = assumeZeroSkew;
		radial = new double[numRadial];
		this.includeTangential = includeTangential;
	}

	public Zhang99Parameters() {
	}

	public void setNumberOfViews( int numViews ) {
		views = new View[numViews];
		for( int i = 0; i < numViews; i++ ) {
			views[i] = new View();
		}
	}

	public Zhang99Parameters createNew() {
		return new Zhang99Parameters(assumeZeroSkew, radial.length,includeTangential,views.length);
	}

	public Zhang99Parameters copy() {
		Zhang99Parameters ret = createNew();
		ret.a = a;
		ret.b = b;
		ret.c = c;
		ret.x0 = x0;
		ret.y0 = y0;

		for( int i = 0; i < radial.length; i++ ) {
			ret.radial[i] = radial[i];
		}

		ret.t1 = t1;
		ret.t2 = t2;
		ret.includeTangential = includeTangential;

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
		public Rodrigues_F64 rotation = new Rodrigues_F64();
		// translation
		public Vector3D_F64 T = new Vector3D_F64();
	}

	public int size() {
		int numTangential = includeTangential ? 2 : 0;
		int skew = assumeZeroSkew ? 0 : 1;

		return 4 + skew + radial.length + numTangential+(3+3)*views.length;
	}

	public void setFromParam( double param[] ) {
		int index = 0;

		a = param[index++];
		b = param[index++];
		if( !assumeZeroSkew )
			c = param[index++];
		x0 = param[index++];
		y0 = param[index++];

		for( int i = 0; i < radial.length; i++ ) {
			radial[i] = param[index++];
		}

		if( includeTangential ) {
			t1 = param[index++];
			t2 = param[index++];
		}

		for( View v : views ) {
			v.rotation.setParamVector(param[index++],param[index++],param[index++]);
			v.T.x = param[index++];
			v.T.y = param[index++];
			v.T.z = param[index++];
		}
	}

	public void convertToParam( double param[] ) {
		int index = 0;

		param[index++] = a;
		param[index++] = b;
		if( !assumeZeroSkew )
			param[index++] = c;
		param[index++] = x0;
		param[index++] = y0;

		for( int i = 0; i < radial.length; i++ ) {
			param[index++] = radial[i];
		}

		if( includeTangential ) {
			param[index++] = t1;
			param[index++] = t2;
		}

		for( View v : views ) {
			param[index++] = v.rotation.unitAxisRotation.x*v.rotation.theta;
			param[index++] = v.rotation.unitAxisRotation.y*v.rotation.theta;
			param[index++] = v.rotation.unitAxisRotation.z*v.rotation.theta;
			param[index++] = v.T.x;
			param[index++] = v.T.y;
			param[index++] = v.T.z;
		}
	}

	/**
	 * Converts to a generalized class that specifies camera intrinsic parameters
	 *
	 * @return Intrinsic parameters
	 */
	public IntrinsicParameters convertToIntrinsic() {
		IntrinsicParameters ret = new IntrinsicParameters();

		ret.fx = a;
		ret.fy = b;
		if( assumeZeroSkew )
			ret.skew = 0;
		else
			ret.skew = c;
		ret.cx = x0;
		ret.cy = y0;
		ret.radial = new double[ radial.length ];
		System.arraycopy(radial,0,ret.radial,0, radial.length);
		if( includeTangential ) {
			ret.t1 = t1;
			ret.t2 = t2;
		} else {
			ret.t1 = ret.t2 = 0;
		}

		return ret;
	}
}
