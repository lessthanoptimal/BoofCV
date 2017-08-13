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

package boofcv.alg.sfm.d3.direct;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.sfm.ImagePixelTo3D;
import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolationType;
import boofcv.core.image.FactoryGImageMultiBand;
import boofcv.core.image.GImageMultiBand;
import boofcv.core.image.border.BorderType;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point3D_F32;
import georegression.struct.se.Se3_F32;
import georegression.transform.se.SePointOps_F32;
import georegression.transform.twist.TwistCoordinate_F32;
import georegression.transform.twist.TwistOps_F32;
import org.ddogleg.struct.FastQueue;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;

/**
 * TODO Fill in
 *
 * @author Peter Abeles
 */
// TODO Handle pathological situations that will basically never happen in real life
	// anything that makes the A matrix singular.  dx or dy being zero will do the trick
@SuppressWarnings("unchecked")
public class VisOdomDirectColorDepth<I extends ImageGray<I>, D extends ImageGray<D>>
{
	// Type of input images
	private ImageType<Planar<I>> imageType;
	private ImageType<Planar<D>> derivType;

	private LinearSolverDense<DMatrixRMaj> solver;
	private DMatrixRMaj A = new DMatrixRMaj(1,6);
	private DMatrixRMaj y = new DMatrixRMaj(1,1);
	private DMatrixRMaj twistMatrix = new DMatrixRMaj(6,1);

	private ImageGradient<Planar<I>,Planar<D>> computeD;

	private InterpolatePixelS<I> interpI;
	private InterpolatePixelS<D> interpDX;
	private InterpolatePixelS<D> interpDY;

	private GImageMultiBand wrapI;

	// gradient of the current frame
	Planar<D> derivX, derivY;

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
	private int inboundsPixels = 0;

	// work space
	Point3D_F32 S = new Point3D_F32();

	private TwistCoordinate_F32 twist = new TwistCoordinate_F32();

	// used to compute spatial diveresity of tracked features
	FeatureSpatialDiversity_F32 diversity = new FeatureSpatialDiversity_F32();

	/**
	 * Declares internal data structures and specifies the type of input images to expect
	 * @param imageType Input image type
	 * @param derivType Type of image to store the derivative in
	 */
	public VisOdomDirectColorDepth(final int numBands , Class<I> imageType , Class<D> derivType ) {

		this.imageType = ImageType.pl(numBands,imageType);
		this.derivType = ImageType.pl(numBands,derivType);

		wrapI = FactoryGImageMultiBand.create(this.imageType);
		// min/max doesn't matter for bilinear interpolation
		setInterpolation(0,0,0,0, InterpolationType.BILINEAR);

		derivX = this.derivType.createImage(1,1);
		derivY = this.derivType.createImage(1,1);

		keypixels = new FastQueue<Pixel>(Pixel.class,true) {
			@Override
			protected Pixel createInstance() {
				return new Pixel(numBands);
			}
		};
		computeD = FactoryDerivative.gradient(DerivativeType.THREE, this.imageType, this.derivType);
	}

	/**
	 * Specifies intrinsic camera parameters.  Must be called.
	 * @param fx focal length x (pixels)
	 * @param fy focal length y (pixels)
	 * @param cx principle point x (pixels)
	 * @param cy principle point y (pixels)
	 * @param width Width of the image
	 * @param height Height of the image
	 */
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

	/**
	 * Used to change interpolation method.  Probably don't want to do this.
	 * @param inputMin min value for input pixels. 0 is typical
	 * @param inputMax max value for input pixels. 255 is typical
	 * @param derivMin min value for the derivative of input pixels
	 * @param derivMax max value for the derivative of input pixels
	 * @param type Type of interpolation method to use
	 */
	public void setInterpolation( double inputMin , double inputMax, double derivMin , double derivMax ,
								  InterpolationType type) {
		interpI = FactoryInterpolation.createPixelS(inputMin,inputMax,type, BorderType.EXTENDED, imageType.getImageClass());
		interpDX = FactoryInterpolation.createPixelS(derivMin,derivMax,type, BorderType.EXTENDED, derivType.getImageClass());
		interpDY = FactoryInterpolation.createPixelS(derivMin,derivMax,type, BorderType.EXTENDED, derivType.getImageClass());
	}

	/**
	 * Specifies convergence parameters
	 *
	 * @param convergenceTol When change in error is less than this fraction stop.  Try 1e-6
	 * @param maxIterations When this number of iterations has been exceeded stop.  Try 10
	 */
	public void setConvergence( float convergenceTol , int maxIterations ) {
		this.convergeTol = convergenceTol;
		this.maxIterations = maxIterations;
	}

	/**
	 * Set's the keyframe.  This is the image which motion is estimated relative to.  The 3D location of points in
	 * the keyframe must be known.
	 *
	 * @param input Image which is to be used as the key frame
	 * @param pixelTo3D Used to compute 3D points from pixels in key frame
	 */
	void setKeyFrame(Planar<I> input, ImagePixelTo3D pixelTo3D) {
		InputSanityCheck.checkSameShape(derivX,input);
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
				p.valid = true;
				wrapI.get(x,y,p.bands);

				p.x = x;
				p.y = y;
				p.p3.set(P_x/P_w,P_y/P_w,P_z/P_w);
			}
		}
	}

	/**
	 * Computes the diversity of valid pixels in keyframe to the location in the current frame.
	 * @return Angular spread along the smallest axis in radians
	 */
	public double computeFeatureDiversity(Se3_F32 keyToCurrent ) {

		diversity.reset();
		for (int i = 0; i < keypixels.size(); i++) {
			Pixel p = keypixels.data[i];

			if( !p.valid )
				continue;

			SePointOps_F32.transform(keyToCurrent, p.p3, S);
			diversity.addPoint(S.x, S.y, S.z);
		}

		diversity.process();
		return diversity.getSpread();
	}

	/**
	 * Estimates the motion relative to the key frame.
	 * @param input Next image in the sequence
	 * @param hintKeyToInput estimated transform from keyframe to the current input image
	 * @return true if it was successful at estimating the motion or false if it failed for some reason
	 */
	public boolean estimateMotion(Planar<I> input , Se3_F32 hintKeyToInput ) {
		InputSanityCheck.checkSameShape(derivX,input);
		initMotion(input);

		keyToCurrent.set(hintKeyToInput);

		boolean foundSolution = false;
		float previousError = Float.MAX_VALUE;
		for (int i = 0; i < maxIterations; i++) {
			constructLinearSystem(input, keyToCurrent);
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
	 */
	void initMotion(Planar<I> input) {
		if( solver == null ) {
			solver = LinearSolverFactory_DDRM.qr(input.width*input.height*input.getNumBands(),6);
		}

		// compute image derivative and setup interpolation functions
		computeD.process(input,derivX,derivY);
	}

	/**
	 * Given the set of points in the key frame and their current observations
	 * @param g initial transform applied to pixel locations.  keyframe to current frame
	 */
	void constructLinearSystem(Planar<I> input , Se3_F32 g ) {
		int numBands = imageType.getNumBands();

		// first precompute everything that does not depend on pixel values
		inboundsPixels = 0;
		for (int i = 0; i < keypixels.size(); i++) {
			Pixel p = keypixels.data[i];

			// Apply the known warp
			SePointOps_F32.transform(g, p.p3, S);

			if( S.z <= 0 ) {
				p.valid = false;
				continue;
			}

			// Compute projected warped pixel coordinate on image I_1
			p.proj.x = (S.x / S.z) * fx + cx;
			p.proj.y = (S.y / S.z) * fy + cy;

			// make sure it's in the bounds
			if (p.proj.x < 0 || p.proj.x > input.width - 1 || p.proj.y < 0 || p.proj.y > input.height - 1) {
				p.valid = false;
				continue;
			} else {
				p.valid = true;
			}
			inboundsPixels++;

			// pi matrix derivative relative to t at S
			float ZZ = S.z * S.z;

			p.dP11 = fx / S.z;
			p.dP13 = -S.x * fx / ZZ;
			p.dP22 = fy / S.z;
			p.dP23 = -S.y * fy / ZZ;
		}

		// how compute the components which require
		errorOptical = 0;
		int row = 0;
		for (int band = 0; band < numBands; band++) {
			interpDX.setImage(derivX.getBand(band));
			interpDY.setImage(derivY.getBand(band));
			interpI.setImage(input.getBand(band));

			for (int i = 0; i < keypixels.size(); i++) {
				Pixel p = keypixels.data[i];

				if( !p.valid )
					continue;

				// Apply the known warp
				// TODO precompute?
				SePointOps_F32.transform(g, p.p3, S);

				// sample pixel values at warped location in I_1
				// NOTE: This could be highly optimized.  Compute and save interpolation weights once per input
				//       instead of for each band and image (current,dx,dy)
				// TODO create a special bilinear class for this?
				float current = interpI.get( p.proj.x, p.proj.y);
				float dx      = interpDX.get(p.proj.x, p.proj.y);
				float dy      = interpDY.get(p.proj.x, p.proj.y);

				// B = grad^T * dPI/dt = shape(1,3)
				float b1 = dx*p.dP11;
				float b2 = dy*p.dP22;
				float b3 = dx*p.dP13 + dy*p.dP23;

				// C * A(S'(x)) = shape(1,6)
				int indexA = row * 6;
				A.data[indexA++] = -b2*S.z + b3*S.y;
				A.data[indexA++] =  b1*S.z - b3*S.x;
				A.data[indexA++] = -b1*S.y + b2*S.x;
				A.data[indexA++] = b1;
				A.data[indexA++] = b2;
				A.data[indexA]   = b3;

				float error = -(current - p.bands[band]);
				y.data[row] = error;

				errorOptical += Math.abs(error);
				row += 1;
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

		// theta is 1 because of how this solution was formulated.  See derivation
		TwistOps_F32.exponential(twist,1.0f, motionTwist );

		return true;
	}

	public float getErrorOptical() {
		return errorOptical;
	}

	public int getInboundsPixels() {
		return inboundsPixels;
	}

	public int getKeyframePixels() {
		return keypixels.size;
	}

	static class Pixel {
		float bands[]; // pixel intensity in each band
		int x,y; // pixel coordinate
		Point3D_F32 p3 = new Point3D_F32(); // world coordinate
		Point2D_F32 proj = new Point2D_F32(); // projected location of point
		boolean valid; // if this is visible after apply the estimated warp

		// the pi matrix
		float dP11,dP13,dP22,dP23;

		public Pixel( int numBands ) {
			bands = new float[numBands];
		}
	}

	public Se3_F32 getKeyToCurrent() {
		return keyToCurrent;
	}

	public ImageType<Planar<I>> getImageType() {
		return imageType;
	}

	public ImageType<Planar<D>> getDerivType() {
		return derivType;
	}
}
