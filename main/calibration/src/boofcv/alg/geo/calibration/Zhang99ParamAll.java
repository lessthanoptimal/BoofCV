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

import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.so.Rodrigues_F64;

/**
 * <p>
 * Parameters for batch optimization.<br>
 * <br>
 * Calibration matrix = [ a c x0 ; 0 b y0; 0 0 1];
 * </p>
 * Same as in the paper, but with the addition of tangential distortion terms.
 *
 * @author Peter Abeles
 */
public class Zhang99ParamAll extends Zhang99ParamCamera {

	// position of each view of the target
	// target to camera transform
	public View[] views;

	public Zhang99ParamAll(boolean assumeZeroSkew,
						   int numRadial, boolean includeTangential,
						   int numViews)
	{
		super(assumeZeroSkew,numRadial,includeTangential);
		setNumberOfViews(numViews);
	}

	public Zhang99ParamAll(boolean assumeZeroSkew, int numRadial, boolean includeTangential) {
		super(assumeZeroSkew,numRadial,includeTangential);
	}

	public Zhang99ParamAll() {
	}

	public void setNumberOfViews( int numViews ) {
		views = new View[numViews];
		for( int i = 0; i < numViews; i++ ) {
			views[i] = new View();
		}
	}

	public Zhang99ParamAll createNew() {
		return new Zhang99ParamAll(assumeZeroSkew, radial.length,includeTangential,views.length);
	}

	public Zhang99ParamAll copy() {
		Zhang99ParamAll ret = createNew();
		ret.a = a;
		ret.b = b;
		ret.c = c;
		ret.x0 = x0;
		ret.y0 = y0;

		System.arraycopy(radial, 0, ret.radial, 0, radial.length);

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

	@Override
	public int numParameters() {
		return super.numParameters()+ (3+3)*views.length;
	}

	public int setFromParam( double param[] ) {
		int index = super.setFromParam(param);

		for( View v : views ) {
			v.rotation.setParamVector(param[index++],param[index++],param[index++]);
			v.T.x = param[index++];
			v.T.y = param[index++];
			v.T.z = param[index++];
		}

		return index;
	}

	@Override
	public int convertToParam( double param[] ) {
		int index = super.convertToParam(param);

		for( View v : views ) {
			param[index++] = v.rotation.unitAxisRotation.x*v.rotation.theta;
			param[index++] = v.rotation.unitAxisRotation.y*v.rotation.theta;
			param[index++] = v.rotation.unitAxisRotation.z*v.rotation.theta;
			param[index++] = v.T.x;
			param[index++] = v.T.y;
			param[index++] = v.T.z;
		}
		return index;
	}

	/**
	 * Converts to a generalized class that specifies camera intrinsic parameters
	 *
	 * @return Intrinsic parameters
	 */
	public CameraPinholeRadial convertToIntrinsic() {
		CameraPinholeRadial ret = new CameraPinholeRadial();

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
