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
import boofcv.struct.calib.CameraUniversalOmni;
import georegression.struct.point.Point2D_F64;
import org.ejml.data.DMatrixRMaj;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of {@link boofcv.struct.calib.CameraUniversalOmni} for bundle adjustment
 *
 * @author Peter Abeles
 */
public class BundleUniversalOmni implements BundleAdjustmentCamera {

	/** focal length along x and y axis (units: pixels) */
	public double fx,fy;
	/** skew parameter, typically 0 (units: pixels)*/
	public double skew;
	/** image center (units: pixels) */
	public double cx,cy;
	/** Mirror offset distance. &xi; */
	public double mirrorOffset;
	/** radial distortion parameters: k<sub>1</sub>,...,k<sub>n</sub> */
	public double radial[];
	/** tangential distortion parameters */
	public double t1, t2;

	// forces skew to be zero
	public boolean zeroSkew;
	// should it estimate the tangential terms?
	public boolean tangential;
	// the mirror parameter will not be changed during optimization
	public boolean fixedMirror;

	public BundleUniversalOmni(boolean zeroSkew,
							   int numRadial, boolean includeTangential, boolean fixedMirror)
	{
		this.radial = new double[numRadial];
		this.zeroSkew = zeroSkew;
		this.tangential = includeTangential;
		this.fixedMirror = fixedMirror;
	}

	public BundleUniversalOmni(boolean zeroSkew,
							   int numRadial, boolean includeTangential, double mirrorOffset)
	{
		this(zeroSkew,numRadial,includeTangential,true);
		this.mirrorOffset = mirrorOffset;
	}

	public BundleUniversalOmni(CameraUniversalOmni intrinsic ) {
		if( intrinsic.radial == null )
			radial = new double[0];
		else
			radial = intrinsic.radial.clone();

		zeroSkew = intrinsic.skew == 0;
		fx = intrinsic.fx;
		fy = intrinsic.fy;
		cx = intrinsic.cx;
		cy = intrinsic.cy;
		if( intrinsic.t1 != 0 || intrinsic.t2 != 0 ) {
			t1 = intrinsic.t1;
			t2 = intrinsic.t2;
		} else {
			tangential = false;
		}
		skew = intrinsic.skew;
		mirrorOffset = intrinsic.mirrorOffset;
	}

	public void convert( CameraUniversalOmni out ) {
		out.fx = fx;
		out.fy = fy;
		out.cx = cx;
		out.cy = cy;
		if( zeroSkew )
			out.skew = 0;
		else
			out.skew = skew;
		out.radial = radial.clone();
		if( tangential ) {
			out.t1 = t1;
			out.t2 = t2;
		} else {
			out.t1 = out.t2 = 0;
		}
		out.mirrorOffset = mirrorOffset;
	}

	public void setK(DMatrixRMaj K ) {
		fx = K.get(0,0);
		fy = K.get(1,1);
		cx = K.get(0,2);
		cy = K.get(1,2);
		if( zeroSkew )
			skew = 0;
		else
			skew = K.get(0,1);
	}

	@Override
	public void setIntrinsic(double[] parameters, int offset) {
		fx = parameters[offset++];
		fy = parameters[offset++];
		cx = parameters[offset++];
		cy = parameters[offset++];
		for (int i = 0; i < radial.length; i++) {
			radial[i]=parameters[offset++];
		}
		if(tangential) {
			t1 = parameters[offset++];
			t2 = parameters[offset++];
		} else {
			t1 = 0;
			t2 = 0;
		}
		if( !zeroSkew)
			skew = parameters[offset++];
		else
			skew = 0;
		if( !fixedMirror )
			mirrorOffset = parameters[offset];
	}

	@Override
	public void getIntrinsic(double[] parameters, int offset) {
		parameters[offset++] = fx;
		parameters[offset++] = fy;
		parameters[offset++] = cx;
		parameters[offset++] = cy;
		for (int i = 0; i < radial.length; i++) {
			parameters[offset++] = radial[i];
		}
		if(tangential) {
			parameters[offset++] = t1;
			parameters[offset++] = t2;
		}
		if( !zeroSkew)
			parameters[offset++] = skew;
		if( !fixedMirror )
			parameters[offset] = mirrorOffset;
	}

	@Override
	public void project(double x, double y, double z, Point2D_F64 output) {

		// apply mirror offset
		z += mirrorOffset;

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
		double dx = x*( 1.0 + sum) + 2.0*t1*x*y + t2*(r2 + 2.0*x*x);
		double dy = y*( 1.0 + sum) + t1*(r2 + 2.0*y*y) + 2.0*t2*x*y;

		// project into pixels
		output.x = fx * dx + skew * dy + cx;
		output.y = fy * dy + cy;
	}

	@Override
	public void jacobian(double camX, double camY, double camZ,
						 @Nonnull double[] inputX, @Nonnull double[] inputY,
						 boolean computeIntrinsic,
						 @Nullable double[] calibX, @Nullable double[] calibY)
	{
		double Z = camZ + mirrorOffset;

		double nx = camX/Z;
		double ny = camY/Z;

		// Apply radial distortion
		double sum = 0;
		double sumdot = 0;

		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		double rdev = 1;

		for( int i = 0; i < radial.length; i++ ) {
			sum += radial[i]*r2i;
			sumdot += radial[i]*(i+1)*rdev;

			r2i *= r2;
			rdev *= r2;
		}

		// X
		double xdot = sumdot*2*nx*nx/Z + (1+sum)/Z;
		double ydot = sumdot*2*nx*ny/Z;
		if( tangential ) {
			xdot += (2*t1*ny + t2*6*nx) / Z;
			ydot += (2*t1*nx + 2*ny*t2) / Z;
		}
		inputX[0] = fx*xdot + skew*ydot;
		inputY[0] = fy*ydot;

		// Y
		xdot = sumdot*2*ny*nx/Z;
		ydot = sumdot*2*ny*ny/Z + (1 + sum)/Z;
		if( tangential ) {
			xdot += (2*t1*nx + t2*2*ny) / Z;
			ydot += (6*t1*ny + 2*nx*t2) / Z;
		}
		inputX[1] = fx*xdot + skew*ydot;
		inputY[1] = fy*ydot;

		// Z
		xdot = -sumdot*2*r2*nx/Z;
		ydot = -sumdot*2*r2*ny/Z;

		xdot += -(1 + sum)*nx/Z;
		ydot += -(1 + sum)*ny/Z;

		if( tangential ) {
			xdot += -(4*t1*nx*ny + 6*t2*nx*nx + 2*t2*ny*ny)/Z;
			ydot += -(2*t1*nx*nx + 6*t1*ny*ny + 4*nx*ny*t2)/Z;
		}
		inputX[2] = fx*xdot + skew*ydot;
		inputY[2] = fy*ydot;

		if( !computeIntrinsic )
			return;

		// compute distorted normalized image coordinates
		double dx = nx + nx*sum + (tangential? 2*t1*nx*ny + t2*(r2 + 2*nx*nx) : 0);
		double dy = ny + ny*sum + (tangential? t1*(r2 + 2*ny*ny) + 2*t2*ny*ny : 0);

		jacobianIntrinsic(calibX,calibY,Z,nx,ny,dx,dy);
	}

	/**
	 *
	 * @param calibX storage for calibration jacobian
	 * @param calibY storage for calibration jacobian
	 * @param nx undistorted normalized image coordinates
	 * @param ny undistorted normalized image coordinates
	 * @param dnx distorted normalized image coordinates
	 * @param dny distorted normalized image coordinates
	 */
	private void jacobianIntrinsic(double[] calibX, double[] calibY,
								   double Z ,
								   double nx, double ny,
								   double dnx, double dny) {
		// Intrinsic parameters
		int index = 0;
		calibX[index  ] = dnx; calibY[index++] = 0;   // fx
		calibX[index  ] = 0;   calibY[index++] = dny; // fy
		calibX[index  ] = 1;   calibY[index++] = 0;   // cx
		calibX[index  ] = 0;   calibY[index++] = 1;   // cy

		// Radial
		double r2 = nx*nx + ny*ny;
		double r2i = r2;
		for( int i = 0; i < radial.length; i++ ) {
			double xdot = nx*r2i;
			double ydot = ny*r2i;

			calibX[index  ] = fx*xdot + skew*ydot;
			calibY[index++] = fy*ydot;
			r2i *= r2;
		}

		// Tangential
		if( tangential ) {
			double xy2 = 2.0*nx*ny;
			double r2yy = r2 + 2*ny*ny;
			double r2xx = r2 + 2*nx*nx;

			calibX[index  ] = fx*xy2  + skew*r2yy;
			calibY[index++] = fy*r2yy;

			calibX[index  ] = fx*r2xx + skew*xy2;
			calibY[index++] = fy*xy2;
		}

		if( !zeroSkew) {
			calibX[index] = dny; calibY[index++] = 0;
		}

		if( !fixedMirror ) {
			double ri2 = -2.0*r2/Z;

			double sum = 0;
			for( int i = 0; i < radial.length; i++ ) {
				sum += radial[i]*ri2;
				ri2 *= 2.0*r2;
			}

			double dx = -nx/Z + nx*sum;
			double dy = -ny/Z + ny*sum;

			if( tangential ) {
				dx += -2*(2.0*t1*nx*ny + t2*(r2 + 2.0*nx*nx))/Z;
				dy += -2*(t1*(r2 + 2.0*ny*ny) + 2.0*t2*nx*ny)/Z;
			}

			calibX[index] = fx * dx + skew * dy;
			calibY[index] = fy * dy;
		}
	}

	@Override
	public int getIntrinsicCount() {
		int totalIntrinsic = 4 + radial.length;
		if( tangential )
			totalIntrinsic += 2;
		if( !zeroSkew )
			totalIntrinsic += 1;
		if( !fixedMirror )
			totalIntrinsic += 1;

		return totalIntrinsic;
	}
}
