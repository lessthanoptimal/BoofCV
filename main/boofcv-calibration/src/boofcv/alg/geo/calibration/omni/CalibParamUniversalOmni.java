
/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration.omni;

import boofcv.alg.geo.calibration.Zhang99IntrinsicParam;
import boofcv.struct.calib.CameraUniversalOmni;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;

/**
 * Parameters for batch optimization of extrinsic and intrinsic calibration parameters.
 *
 * @author Peter Abeles
 */
public class CalibParamUniversalOmni implements Zhang99IntrinsicParam
{
	public CameraUniversalOmni intrinsic;

	// does it assume skew = 0?
	public boolean assumeZeroSkew;
	// should it estimate the tangential terms?
	public boolean includeTangential;

	public CalibParamUniversalOmni(boolean assumeZeroSkew,
								   int numRadial, boolean includeTangential)
	{
		this.intrinsic = new CameraUniversalOmni(numRadial);
		this.assumeZeroSkew = assumeZeroSkew;
		this.includeTangential = includeTangential;
	}

	@Override
	public CalibParamUniversalOmni createLike() {
		return new CalibParamUniversalOmni(assumeZeroSkew,
				intrinsic.radial.length,
				includeTangential);
	}

	@Override
	public void setTo( Zhang99IntrinsicParam orig ) {
		CalibParamUniversalOmni o = (CalibParamUniversalOmni)orig;
		intrinsic.set(o.intrinsic);
		includeTangential = o.includeTangential;
		assumeZeroSkew = o.assumeZeroSkew;
	}

	@Override
	public void project(Point3D_F64 cameraPt, Point2D_F64 pixel) {

	}

	@Override
	public void initialize(DMatrixRMaj K , double[] radial) {
		intrinsic.fx = K.get(0,0);
		intrinsic.fy = K.get(1,1);
		intrinsic.skew = assumeZeroSkew ? 0 : K.get(0,1);
		intrinsic.cx = K.get(0,2);
		intrinsic.cy = K.get(1,2);

		if( radial.length != intrinsic.radial.length )
			throw new RuntimeException("BUG!");
		System.arraycopy(radial,0,intrinsic.radial,0,intrinsic.radial.length);
		intrinsic.t1 = intrinsic.t2 = 0;
		intrinsic.mirrorOffset = 0;
	}

	@Override
	public int numParameters() {
		int totalIntrinsic = 5 + intrinsic.radial.length;
		if( includeTangential )
			totalIntrinsic += 2;
		if( !assumeZeroSkew )
			totalIntrinsic += 1;

		return totalIntrinsic;
	}

	@Override
	public int setFromParam( double param[] ) {
		int index = 0;
		intrinsic.fx = param[index++];
		intrinsic.fy = param[index++];
		intrinsic.cx = param[index++];
		intrinsic.cy = param[index++];
		intrinsic.mirrorOffset = param[index++];
		if( !assumeZeroSkew )
			intrinsic.skew = param[index++];
		for (int i = 0; i < intrinsic.radial.length; i++) {
			intrinsic.radial[i]=param[index++];
		}
		if( includeTangential ) {
			intrinsic.t1 = param[index++];
			intrinsic.t2 = param[index++];
		}

		return index;
	}

	@Override
	public int convertToParam( double param[] ) {
		int index = 0;
		param[index++] = intrinsic.fx;
		param[index++] = intrinsic.fy;
		param[index++] = intrinsic.cx;
		param[index++] = intrinsic.cy;
		param[index++] = intrinsic.mirrorOffset;
		if( !assumeZeroSkew )
			param[index++] = intrinsic.skew;
		for (int i = 0; i < intrinsic.radial.length; i++) {
			param[index++] = intrinsic.radial[i];
		}
		if( includeTangential ) {
			param[index++] = intrinsic.t1;
			param[index++] = intrinsic.t2;
		}

		return index;
	}

	@Override
	public CameraUniversalOmni getCameraModel() {
		return intrinsic;
	}
}
