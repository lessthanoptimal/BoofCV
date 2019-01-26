/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.bundle.cameras;

import boofcv.abst.geo.bundle.BundleAdjustmentCamera;
import boofcv.alg.distort.universal.UniOmniStoP_F64;
import boofcv.struct.calib.CameraUniversalOmni;
import georegression.struct.point.Point2D_F64;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of {@link boofcv.struct.calib.CameraUniversalOmni} for bundle adjustment
 *
 * @author Peter Abeles
 */
public class BundleUniversalOmni implements BundleAdjustmentCamera {
	public CameraUniversalOmni intrinsic;

	// forces skew to be zero
	public boolean assumeZeroSkew;
	// should it estimate the tangential terms?
	public boolean includeTangential;
	// the mirror parameter will not be changed during optimization
	public boolean fixedMirror;

	UniOmniStoP_F64 sphereToPixel = new UniOmniStoP_F64();

	public BundleUniversalOmni(boolean assumeZeroSkew,
							   int numRadial, boolean includeTangential, boolean fixedMirror)
	{
		this.intrinsic = new CameraUniversalOmni(numRadial);
		this.assumeZeroSkew = assumeZeroSkew;
		this.includeTangential = includeTangential;
		this.fixedMirror = fixedMirror;
	}

	public BundleUniversalOmni(boolean assumeZeroSkew,
							   int numRadial, boolean includeTangential, double mirrorOffset)
	{
		this(assumeZeroSkew,numRadial,includeTangential,true);
		this.intrinsic.mirrorOffset = mirrorOffset;
	}

	@Override
	public void setIntrinsic(double[] parameters, int offset) {
		intrinsic.fx = parameters[offset++];
		intrinsic.fy = parameters[offset++];
		if( !assumeZeroSkew )
			intrinsic.skew = parameters[offset++];
		else
			intrinsic.skew = 0;
		intrinsic.cx = parameters[offset++];
		intrinsic.cy = parameters[offset++];
		for (int i = 0; i < intrinsic.radial.length; i++) {
			intrinsic.radial[i]=parameters[offset++];
		}
		if( includeTangential ) {
			intrinsic.t1 = parameters[offset++];
			intrinsic.t2 = parameters[offset++];
		} else {
			intrinsic.t1 = 0;
			intrinsic.t2 = 0;
		}
		if( !fixedMirror )
			intrinsic.mirrorOffset = parameters[offset++];

		sphereToPixel.setModel(intrinsic);
	}

	@Override
	public void getIntrinsic(double[] parameters, int offset) {
		parameters[offset++] = intrinsic.fx;
		parameters[offset++] = intrinsic.fy;
		if( !assumeZeroSkew )
			parameters[offset++] = intrinsic.skew;
		parameters[offset++] = intrinsic.cx;
		parameters[offset++] = intrinsic.cy;
		for (int i = 0; i < intrinsic.radial.length; i++) {
			parameters[offset++] = intrinsic.radial[i];
		}
		if( includeTangential ) {
			parameters[offset++] = intrinsic.t1;
			parameters[offset++] = intrinsic.t2;
		}
		if( !fixedMirror )
			parameters[offset++] = intrinsic.mirrorOffset;
	}

	@Override
	public void project(double x, double y, double z, Point2D_F64 output) {
		double[] radial = intrinsic.radial;
		double t1 = intrinsic.t1;
		double t2 = intrinsic.t2;

		// apply mirror offset
		z += intrinsic.mirrorOffset;

		// compute normalized image coordinates
		x /= z;
		y /= z;

		double r2 = x*x + y*y;
		double ri2 = r2;

		double sum = 0;
		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*ri2;
			ri2 *= r2;
		}

		// compute distorted normalized image coordinates
		x = x*( 1.0 + sum) + 2.0*t1*x*y + t2*(r2 + 2.0*x*x);
		y = y*( 1.0 + sum) + t1*(r2 + 2.0*y*y) + 2.0*t2*x*y;

		// project into pixels
		output.x = intrinsic.fx * x + intrinsic.skew * y + intrinsic.cx;
		output.y = intrinsic.fy * y + intrinsic.cy;
	}

	@Override
	public void jacobian(double camX, double camY, double camZ,
						 @Nonnull double[] pointX, @Nonnull double[] pointY,
						 boolean computeIntrinsic,
						 @Nullable double[] calibX, @Nullable double[] calibY)
	{

	}

	@Override
	public int getIntrinsicCount() {
		int totalIntrinsic = 5 + intrinsic.radial.length;
		if( includeTangential )
			totalIntrinsic += 2;
		if( !assumeZeroSkew )
			totalIntrinsic += 1;
		if( fixedMirror )
			totalIntrinsic -= 1;

		return totalIntrinsic;
	}
}
