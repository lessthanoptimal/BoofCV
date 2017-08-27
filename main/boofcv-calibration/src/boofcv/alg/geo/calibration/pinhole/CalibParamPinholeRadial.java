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

package boofcv.alg.geo.calibration.pinhole;

import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.alg.geo.calibration.CalibrationPlanarGridZhang99;
import boofcv.alg.geo.calibration.Zhang99IntrinsicParam;
import boofcv.alg.geo.calibration.Zhang99OptimizationJacobian;
import boofcv.struct.calib.CameraPinholeRadial;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.ejml.data.DMatrixRMaj;

import java.util.List;

/**
 * Intrinsic camera parameters for optimization in Zhang99
 *
 * @author Peter Abeles
 */
public class CalibParamPinholeRadial extends Zhang99IntrinsicParam {
	public CameraPinholeRadial intrinsic;

	private Point2D_F64 normPt = new Point2D_F64();

	// should it estimate the tangential terms?
	public boolean includeTangential;

	/**
	 *
	 * @param assumeZeroSkew Should it assumed the camera has zero skew. Typically true.
	 * @param numRadial Number of radial distortion parameters to consider.  Typically 0,1,2.
	 * @param includeTangential Should it include tangential distortion?
	 */
	public CalibParamPinholeRadial(boolean assumeZeroSkew, int numRadial, boolean includeTangential) {
		this.intrinsic = new CameraPinholeRadial(numRadial);
		this.assumeZeroSkew = assumeZeroSkew;
		this.includeTangential = includeTangential;
	}


	@Override
	public int getNumberOfRadial() {
		return intrinsic.radial.length;
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
	}

	/**
	 * Returns the total number of parameters being estimated
	 */
	@Override
	public int numParameters() {
		int total = 4 + intrinsic.radial.length;
		if( !assumeZeroSkew )
			total += 1;
		if( includeTangential ) {
			total += 2;
		}

		return total;
	}

	/**
	 * Sets the camera parameters from the passed in array
	 *
	 * @return number of parameters read.
	 */
	@Override
	public int setFromParam( double param[] ) {
		int index = 0;

		intrinsic.fx = param[index++];
		intrinsic.fy = param[index++];
		if (!assumeZeroSkew)
			intrinsic.skew = param[index++];
		else
			intrinsic.skew = 0;
		intrinsic.cx = param[index++];
		intrinsic.cy = param[index++];

		for (int i = 0; i < intrinsic.radial.length; i++) {
			intrinsic.radial[i] = param[index++];
		}

		if (includeTangential) {
			intrinsic.t1 = param[index++];
			intrinsic.t2 = param[index++];
		} else {
			intrinsic.t1 = 0;
			intrinsic.t2 = 0;
		}

		return index;
	}

	/**
	 * Writes the parameters into the provided array
	 *
	 * @return number of parameters
	 */
	@Override
	public int convertToParam( double param[] ) {
		int index = 0;

		param[index++] = intrinsic.fx;
		param[index++] = intrinsic.fy;
		if (!assumeZeroSkew)
			param[index++] = intrinsic.skew;
		param[index++] = intrinsic.cx;
		param[index++] = intrinsic.cy;

		for (int i = 0; i < intrinsic.radial.length; i++) {
			param[index++] = intrinsic.radial[i];
		}

		if (includeTangential) {
			param[index++] = intrinsic.t1;
			param[index++] = intrinsic.t2;
		}

		return index;
	}

	@Override
	public CameraPinholeRadial getCameraModel() {
		return intrinsic;
	}

	@Override
	public Zhang99IntrinsicParam createLike() {
		return new CalibParamPinholeRadial(assumeZeroSkew,intrinsic.radial.length,includeTangential);
	}

	@Override
	public Zhang99OptimizationJacobian createJacobian(List<CalibrationObservation> observations, List<Point2D_F64> grid) {
		return new Zhang99OptimizationJacobian(this,observations,grid);
	}

	@Override
	public void setTo(Zhang99IntrinsicParam orig) {
		CalibParamPinholeRadial o = (CalibParamPinholeRadial)orig;
		intrinsic.set(o.intrinsic);
		includeTangential = o.includeTangential;
		assumeZeroSkew = o.assumeZeroSkew;
	}

	@Override
	public void forceProjectionUpdate() {
	}

	@Override
	public void project(Point3D_F64 cameraPt, Point2D_F64 pixel) {
		// normalized image coordinates
		normPt.x = cameraPt.x/ cameraPt.z;
		normPt.y = cameraPt.y/ cameraPt.z;

		// apply distortion
		CalibrationPlanarGridZhang99.applyDistortion(normPt, intrinsic.radial, intrinsic.t1, intrinsic.t2);

		// convert to pixel coordinates
		pixel.x = intrinsic.fx*normPt.x + intrinsic.skew*normPt.y + intrinsic.cx;
		pixel.y = intrinsic.fy*normPt.y + intrinsic.cy;
	}
}
