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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.feature.detect.extract.NonMaxLimiter;
import boofcv.abst.filter.convolve.ImageConvolveSparse;
import boofcv.alg.filter.kernel.KernelMath;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.core.image.border.ImageBorder;
import boofcv.factory.filter.convolve.FactoryConvolveSparse;
import boofcv.struct.convolve.Kernel1D_F32;
import boofcv.struct.convolve.Kernel2D_F32;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.image.ImageFloat32;
import org.ddogleg.struct.FastQueue;

import static boofcv.alg.feature.detect.interest.FastHessianFeatureDetector.polyPeak;

/**
 *
 * TODO describe algorithm
 *
 * TODO highlight differences
 *
 * Math Notes:
 * Convolving an image twice with a Guassian kernel of sigma is the same as convolving it once with a kernel
 * of sqrt(2)*sigma.
 *
 * @author Peter Abeles
 */
public class SiftDetector2 {

	// image pyramid that it processes
	protected SiftScaleSpace2 scaleSpace;

	// conversion factor to go from pixel coordinate in current octave to input image
	protected double pixelScaleToInput;

	// edge detector threshold
	// In the paper this is (r+1)**2/r
	double edgeThreshold;

	// all the found detections in a single octave
	protected FastQueue<ScalePoint> detections = new FastQueue<ScalePoint>(ScalePoint.class,true);

	// Computes image derivatives. used in edge rejection
	private ImageConvolveSparse<ImageFloat32,?> derivXX;
	private ImageConvolveSparse<ImageFloat32,?> derivXY;
	private ImageConvolveSparse<ImageFloat32,?> derivYY;

	// local scale space around the current scale image being processed
	ImageFloat32 dogLower;  // DoG image in lower scale
	ImageFloat32 dogTarget; // DoG image in target scale
	ImageFloat32 dogUpper;  // DoG image in upper scale
	double sigmaLower, sigmaTarget, sigmaUpper;

	// finds features from 2D intensity image
	private NonMaxLimiter extractor;

	/**
	 *
	 * @param scaleSpace
	 * @param edgeR Threshold used to remove edge responses.  Try 10
	 * @param extractor
	 */
	public SiftDetector2( SiftScaleSpace2 scaleSpace ,
						  double edgeR ,
						  NonMaxLimiter extractor ) {
		if( !extractor.getNonmax().canDetectMaximums() || !extractor.getNonmax().canDetectMinimums() )
			throw new IllegalArgumentException("The extractor must be able to detect maximums and minimums");
		if( edgeR < 1 ) {
			throw new IllegalArgumentException("R must be >= 1");
		}

		if( extractor.getNonmax().getIgnoreBorder() != 1 ) {
			throw new RuntimeException("Non-max should have an ignore border of 1");
		}

		this.scaleSpace = scaleSpace;
		this.extractor = extractor;

		this.edgeThreshold = (edgeR+1)*(edgeR+1)/edgeR;

		createSparseDerivatives();
	}

	/**
	 * Define sparse image derivative operators.
	 */
	private void createSparseDerivatives() {
		Kernel1D_F32 kernelD = new Kernel1D_F32(new float[]{-1,0,1},3);

		Kernel1D_F32 kernelDD = KernelMath.convolve1D(kernelD, kernelD);
		Kernel2D_F32 kernelXY = KernelMath.convolve2D(kernelD, kernelD);

		derivXX = FactoryConvolveSparse.horizontal1D(ImageFloat32.class, kernelDD);
		derivXY = FactoryConvolveSparse.convolve2D(ImageFloat32.class, kernelXY);
		derivYY = FactoryConvolveSparse.vertical1D(ImageFloat32.class, kernelDD);

		ImageBorder<ImageFloat32> border = FactoryImageBorder.single(ImageFloat32.class, BorderType.EXTENDED);

		derivXX.setImageBorder(border);
		derivXY.setImageBorder(border);
		derivYY.setImageBorder(border);
	}

	public void process( ImageFloat32 input ) {

		scaleSpace.initialize(input);
		detections.reset();

		do {
			// scale from octave to input image
			pixelScaleToInput = scaleSpace.pixelScaleCurrentToInput();

			// detect features in the image
			for (int j = 1; j < scaleSpace.getNumScales()+1; j++) {

				// not really sure how to compute the scale for features found at a particular DoG
				// image.  Maybe the average of the two scales it was computed from is reasonable?
				double scaleImage0 = scaleSpace.computeSigmaScale( j - 1);
				double scaleImage1 = scaleSpace.computeSigmaScale( j    );
				double scaleImage2 = scaleSpace.computeSigmaScale( j + 1);
				double scaleImage3 = scaleSpace.computeSigmaScale( j + 2);

				sigmaLower = (scaleImage0+scaleImage1)/2.0;
				sigmaTarget = (scaleImage1+scaleImage2)/2.0;
				sigmaUpper = (scaleImage2+scaleImage3)/2.0;

				sigmaLower = scaleImage0;
				sigmaTarget = scaleImage1;
				sigmaUpper = scaleImage2;

				// grab the local DoG scale space images
				dogLower  = scaleSpace.getDifferenceOfGaussian(j-1);
				dogTarget = scaleSpace.getDifferenceOfGaussian(j  );
				dogUpper  = scaleSpace.getDifferenceOfGaussian(j+1);

				detectFeatures(j);
			}
		} while( scaleSpace.computeNextOctave() );
	}

	/**
	 * Detect features inside the Difference-of-Gaussian image at the current scale
	 *
	 * @param scaleIndex Which scale in the octave is it detecting features inside up.
	 *              Primarily provided here for use in child classes.
	 */
	protected void detectFeatures( int scaleIndex ) {
		extractor.process(dogTarget);
		FastQueue<NonMaxLimiter.LocalExtreme> found = extractor.getLocalExtreme();

		derivXX.setImage(dogTarget);
		derivXY.setImage(dogTarget);
		derivYY.setImage(dogTarget);

//		octaveDetection.reset();

		for (int i = 0; i < found.size; i++) {
			NonMaxLimiter.LocalExtreme e = found.get(i);

			if( e.max ) {
				if( isScaleSpaceExtremum(e.location.x, e.location.y, e.getValue(), 1f)) {
					processFeatureCandidate(e.location.x,e.location.y,e.getValue(),e.max);
				}
			} else if( isScaleSpaceExtremum(e.location.x, e.location.y, e.getValue(), -1f)) {
				processFeatureCandidate(e.location.x,e.location.y,e.getValue(),e.max);
			}
		}
	}

	/**
	 * See if the point is a local extremum in scale-space above and below.
	 *
	 * @param c_x x-coordinate of extremum
	 * @param c_y y-coordinate of extremum
	 * @param value The maximum value it is checking
	 * @param signAdj Adjust the sign so that it can check for maximums
	 * @return true if its a local extremum
	 */
	private boolean isScaleSpaceExtremum(int c_x, int c_y, float value, float signAdj) {
		if( c_x <= 1 || c_y <= 1 || c_x >= dogLower.width-1 || c_y >= dogLower.height-1)
			return false;

		float v;

		value *= signAdj;

		for( int y = -1; y <= 1; y++ ) {
			for( int x = -1; x <= 1; x++ ) {
			    v = dogLower.unsafe_get(c_x+x,c_y+y);
				if( v*signAdj >= value )
					return false;
				v = dogUpper.unsafe_get(c_x+x,c_y+y);
				if( v*signAdj >= value )
					return false;
			}
		}

		return true;
	}

	protected void processFeatureCandidate( int x , int y , float value ,boolean white ) {
		// suppress response along edges
		if( isEdge(x,y) )
			return;

		// Estimate the scale and 2D point by fitting 2nd order polynomials
		// This is different from the original paper
		float signAdj = white ? 1 : -1;

		float x0 =  dogTarget.unsafe_get(x - 1, y)*signAdj;
		float x2 =  dogTarget.unsafe_get(x + 1, y)*signAdj;
		float y0 =  dogTarget.unsafe_get(x , y - 1)*signAdj;
		float y2 =  dogTarget.unsafe_get(x , y + 1)*signAdj;

		float s0 =  dogLower.unsafe_get(x , y )*signAdj;
		float s2 =  dogUpper.unsafe_get(x , y )*signAdj;

		ScalePoint p = detections.grow();

		// Compute the interpolated coordinate of the point in the original image coordinates
		p.x = pixelScaleToInput*(x + polyPeak(x0, value, x2));
		p.y = pixelScaleToInput*(y + polyPeak(y0, value, y2));

		// find the peak then do bilinear interpolate between the two appropriate sigmas
		double sigmaInterp = polyPeak(s0, value, s2); // scaled from -1 to 1
		if( sigmaInterp < 0 ) {
			p.scale = sigmaLower*(-sigmaInterp) + (1+sigmaInterp)*sigmaTarget;
		} else {
			p.scale = sigmaUpper*sigmaInterp + (1-sigmaInterp)*sigmaTarget;
		}
		p.white = white;

		handleDetection(p);
	}

	/**
	 * Function for handling a detected point.  Does nothing here, but can be used by a child class
	 * to process detections
	 * @param p Detected point in scale-space.
	 */
	protected void handleDetection( ScalePoint p ){}

	/**
	 * Performs an edge test to remove false positives.  See 4.1 in [1].
	 */
	private boolean isEdge( int x , int y ) {
		if( edgeThreshold <= 0 )
			return false;

		double xx = derivXX.compute(x,y);
		double xy = derivXY.compute(x,y);
		double yy = derivYY.compute(x,y);

		double Tr = xx + yy;
		double det = xx*yy - xy*xy;

		// The SIFT paper does not show absolute value here nor have I put enough thought into it
		// to determine if this makes any sense.  However, it does seem to improve performance
		// quite a bit.

		// In paper this is:
		// Tr**2/Det < (r+1)**2/r
		return( Math.abs(Tr*Tr) >= Math.abs(edgeThreshold*det));
	}

	public FastQueue<ScalePoint> getDetections() {
		return detections;
	}
}
