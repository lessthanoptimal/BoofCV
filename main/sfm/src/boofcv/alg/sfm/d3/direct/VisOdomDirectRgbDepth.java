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

package boofcv.alg.sfm.d3.direct;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point3D_F32;
import georegression.struct.se.Se3_F32;
import georegression.transform.se.SePointOps_F32;
import georegression.transform.twist.TwistCoordinate_F32;
import georegression.transform.twist.TwistOps_F32;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.LinearSolverFactory;
import org.ejml.interfaces.linsol.LinearSolver;

/**
 * TODO Fill in
 *
 * @author Peter Abeles
 */
public class VisOdomDirectRgbDepth<I extends ImageBase<I>, D extends ImageBase<D>>
{
	// Type of input images
	private ImageType<I> imageType;
	private ImageType<D> derivType;

	private LinearSolver<DenseMatrix64F> solver;
	private DenseMatrix64F A = new DenseMatrix64F(1,6);
	private DenseMatrix64F y = new DenseMatrix64F(1,1);
	private DenseMatrix64F twistMatrix = new DenseMatrix64F(6,1);

	private ImageGradient<I,D> computeD;

	private InterpolatePixelMB<I> interpI;
	private InterpolatePixelMB<D> interpDX;
	private InterpolatePixelMB<D> interpDY;

	private GImageMultiBand wrapI;

	// storage for pixel value at current location and it's gradient
	private float current[];
	private float dx[];
	private float dy[];

	// gradient of the current frame
	D derivX, derivY;

	// storage for pixel information in the key frame
	FastQueue<Pixel> keypixels;

	// Estimated motion from key frame to current frame
	private Se3_F32 keyToCurrent = new Se3_F32();

	// estimated motion from twist parameters for current iterations
	Se3_F32 motionTwist = new Se3_F32();
	private Se3_F32 tmp = new Se3_F32(); // work space

	/** focal length along x and y axis (units: pixels) */
	private float fx,fy;
	/** image center (units: pixels) */
	private float cx,cy;

	private float convergeTol = 1e-6f;
	private int maxIterations = 10;

	// average optical error per pixel and band
	private float errorOptical;
	// number of valid pixels used to compute error
	private int validPixels = 0;

	private TwistCoordinate_F32 twist = new TwistCoordinate_F32();

	public VisOdomDirectRgbDepth(ImageType<I> imageType , ImageType<D> derivType ) {

		this.imageType = imageType;
		this.derivType = derivType;

		wrapI = FactoryGImageMultiBand.create(imageType);
		// min/max doesn't matter for bilinear interpolation
		setInterpolation(0,0,0,0, InterpolationType.BILINEAR);

		final int numBands = imageType.getNumBands();

		current = new float[ numBands ];
		dx = new float[ numBands ];
		dy = new float[ numBands ];

		derivX = derivType.createImage(1,1);
		derivY = derivType.createImage(1,1);

		keypixels = new FastQueue<Pixel>(Pixel.class,true) {
			@Override
			protected Pixel createInstance() {
				return new Pixel(numBands);
			}
		};
		computeD = FactoryDerivative.gradient(DerivativeType.THREE, imageType, derivType);
	}

	public void setCameraParameters( float fx , float fy , float cx , float cy ,
									 int width , int height ) {
		this.fx = fx;
		this.fy = fy;
		this.cx = cx;
		this.cy = cy;

		derivX.reshape(width, height);
		derivY.reshape(width, height);

		// set these to the maximum possible size
		int N = width*height*imageType.getNumBands();
		A.reshape(N,6);
		y.reshape(N,1);
	}

	public void setInterpolation( double inputMin , double inputMax, double derivMin , double derivMax ,
								  InterpolationType type) {
		interpI = FactoryInterpolation.createPixelMB(inputMin,inputMax,type, BorderType.EXTENDED, imageType);
		interpDX = FactoryInterpolation.createPixelMB(derivMin,derivMax,type, BorderType.EXTENDED, derivType);
		interpDY = FactoryInterpolation.createPixelMB(derivMin,derivMax,type, BorderType.EXTENDED, derivType);
	}

	public void setConvergence( float convergenceTol , int maxIterations ) {
		this.convergeTol = convergenceTol;
		this.maxIterations = maxIterations;
	}

	void setKeyFrame(I input, ImagePixelTo3D pixelTo3D) {
		wrapI.wrap(input);
		keypixels.reset();

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				// See if there's a valid 3D point at this location
				if( !pixelTo3D.process(x,y) ) {
					continue;
				}

				float P_x = (float)pixelTo3D.getX();
				float P_y = (float)pixelTo3D.getY();
				float P_z = (float)pixelTo3D.getZ();
				float P_w = (float)pixelTo3D.getW();

				// skip point if it's at infinity or has a negative value
				if( P_w <= 0 )
					continue;

				// save the results
				Pixel p = keypixels.grow();
				wrapI.get(x,y,p.bands);

				p.x = x;
				p.y = y;
				p.p3.set(P_x/P_w,P_y/P_w,P_z/P_w);
			}
		}
	}

	public boolean estimateMotion(I input , Se3_F32 hintKeyToInput ) {
		initMotion(input);

		keyToCurrent.set(hintKeyToInput);

		boolean foundSolution = false;
		float previousError = Float.MAX_VALUE;
		for (int i = 0; i < maxIterations; i++) {
			constructLinearSystem(input.width, input.height, keyToCurrent);
			if (!solveSystem())
				break;

			if( Math.abs(previousError-errorOptical)/previousError < convergeTol )
				break;
			else {
				// update the estimated motion from the computed twist
				previousError = errorOptical;
				keyToCurrent.concat(motionTwist, tmp);
				keyToCurrent.set(tmp);
				foundSolution = true;
			}
		}
		return foundSolution;
	}

	/**
	 * Initialize motion related data structures
	 * @param input
	 */
	void initMotion(I input) {
		if( solver == null ) {
			solver = LinearSolverFactory.qr(input.width*input.height,6);
		}

		// compute image derivative and setup interpolation functions
		computeD.process(input,derivX,derivY);
		interpDX.setImage(derivX);
		interpDY.setImage(derivY);
		interpI.setImage(input);
	}

	/**
	 * Given the set of points in the key frame and their current observations
	 * @param width image width
	 * @param height image height
	 * @param g initial transform applied to pixel locations
	 */
	void constructLinearSystem(int width , int height , Se3_F32 g ) {

		int numBands = imageType.getNumBands();

		Point3D_F32 S = new Point3D_F32();

		errorOptical = 0;
		validPixels = 0;
		int row = 0;
		for (int i = 0; i < keypixels.size(); i++) {
			Pixel p = keypixels.data[i];

			// Apply the known warp
			SePointOps_F32.transform(g,p.p3,S);

			// Compute projected warped pixel coordinate on image I_1
			float x1 = (S.x*fx)/S.z + cx;
			float y1 = (S.y*fy)/S.z + cy;

			// make sure it's in the bounds
			if( x1 < 0 || x1 > width-1 || y1 < 0 || y1 > height-1 )
				continue;
			validPixels++;

			// pi matrix derivative relative to S
			float ZZ   = S.z*S.z;
			float dP11 = fx/S.z;
			float dP13 = -S.x*fx/ZZ;
			float dP22 = fy/S.z;
			float dP23 = -S.y*fy/ZZ;

			// sample pixel values at warped location in I_1
			interpI.get(x1,y1, current);
			interpDX.get(x1,y1, dx);
			interpDY.get(x1,y1, dy);

			for (int band = 0; band < numBands; band++, row++) {
				float bandDx = dx[band];
				float bandDy = dy[band];

				// B = grad^T * dPI/dt = shape(1,3)
				float b1 = bandDx*dP11;
				float b2 = bandDy*dP22;
				float b3 = bandDx*dP13 + bandDy*dP23;

				// C * A(S'(x)) = shape(1,6)
				int indexA = row*6;
				A.data[indexA++] = -b2*S.z + b2*S.y;
				A.data[indexA++] =  b1*S.z - b3*S.x;
				A.data[indexA++] = -b1*S.y + b2*S.x;
				A.data[indexA++] = b1;
				A.data[indexA++] = b2;
				A.data[indexA  ] = b3;

				float error = -(current[band] - p.bands[band]);
				y.data[row] = error;

				errorOptical += Math.abs(error);
			}
		}
		errorOptical /= row;


		A.numRows = row;
		y.numRows = row;
	}

	boolean solveSystem() {
		if( !solver.setA(A))
			return false;

		solver.solve(y,twistMatrix);

		twist.set((float)twistMatrix.data[0], (float)twistMatrix.data[1], (float)twistMatrix.data[2],
				(float)twistMatrix.data[3], (float)twistMatrix.data[4], (float)twistMatrix.data[5]);

		// TODO see how close to norm of 1 it is
		twist.normalize();

		// theta is 1 because of how this solution was formulated.  See derivation
		TwistOps_F32.exponential(twist,1.0f, motionTwist );

		return true;
	}

	static class Pixel {
		float bands[]; // pixel intensity in each band
		int x,y; // pixel coordinate
		Point3D_F32 p3 = new Point3D_F32(); // world coordinate

		public Pixel( int numBands ) {
			bands = new float[numBands];
		}
	}

	public Se3_F32 getKeyToCurrent() {
		return keyToCurrent;
	}

	public ImageType<I> getImageType() {
		return imageType;
	}

	public ImageType<D> getDerivType() {
		return derivType;
	}
}
