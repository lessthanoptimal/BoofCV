/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.circulant;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.transform.fft.DiscreteFourierTransformOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.InterleavedF32;
import georegression.struct.shapes.Rectangle2D_I32;

/**
 *
 * TODO write
 *
 * @author Peter Abeles
 */
public class CirculantTracker {

	// --- Tuning parameters
	// spatial bandwidth (proportional to target)
	private double output_sigma_factor;

	// gaussian kernel bandwidth
	private float sigma;

	// regularization term
	private float lambda;
	// linear interpolation term.  Adjusts how fast it can learn
	private float interp_factor;

	//----- Internal variables
	// computes the FFT
	private DiscreteFourierTransform<ImageFloat32,InterleavedF32> fft = DiscreteFourierTransformOps.createTransformF32();

	// storage for subimage of input image
	private ImageFloat32 subInput = new ImageFloat32();
	// storage for the subimage of the previous frame
	private ImageFloat32 subPrev = new ImageFloat32();

	// cosine window used to reduce artifacts from FFT
	private ImageFloat32 cosine = new ImageFloat32(1,1);

	// Storage for the kernel's response
	private ImageFloat32 k = new ImageFloat32(1,1);
	private InterleavedF32 kf = new InterleavedF32(1,1,2);

	// Learn values.  used to compute weight in linear classifier
	private ImageFloat32 alpha = new ImageFloat32(1,1);
	private InterleavedF32 alphaf = new InterleavedF32(1,1,2);
	private ImageFloat32 newAlpha = new ImageFloat32(1,1);
	private InterleavedF32 newAlphaf = new InterleavedF32(1,1,2);

	// location of target
	private Rectangle2D_I32 region = new Rectangle2D_I32();

	// Used for computing the gaussian kernel
	private ImageFloat32 gaussianWeight = new ImageFloat32(1,1);
	private InterleavedF32 gaussianWeightDFT = new InterleavedF32(1,1,2);

	// storage for storing temporary results
	private ImageFloat32 tmpReal0 = new ImageFloat32(1,1);

	private InterleavedF32 tmpFourier0 = new InterleavedF32(1,1,2);
	private InterleavedF32 tmpFourier1 = new InterleavedF32(1,1,2);
	private InterleavedF32 tmpFourier2 = new InterleavedF32(1,1,2);

	/**
	 *
	 * @param output_sigma_factor  Try 1.0/16.0
	 * @param sigma Try 0.2f
	 * @param lambda Try 1e-2f
	 * @param interp_factor Try 0.075f
	 */
	public CirculantTracker(double output_sigma_factor, float sigma, float lambda, float interp_factor) {
		this.output_sigma_factor = output_sigma_factor;
		this.sigma = sigma;
		this.lambda = lambda;
		this.interp_factor = interp_factor;
	}

	/**
	 * Initializes tracking around the specified rectangle region
	 * @param image Image to start tracking from
	 * @param x0 top-left corner of region
	 * @param y0 top-left corner of region
	 * @param regionWidth region's width
	 * @param regionHeight region's height
	 */
	public void initialize( ImageFloat32 image , int x0 , int y0 , int regionWidth , int regionHeight ) {

		// save the track location
		this.region.width = regionWidth;
		this.region.height = regionHeight;
		this.region.tl_x = x0;
		this.region.tl_y = y0;

		initializeData(image);
		initialLearning(image,x0,y0);
	}

	/**
	 * Declare and compute various data structures
	 */
	private void initializeData(ImageFloat32 image ) {
		boolean sizeChange = cosine.width != region.width || cosine.height != region.height;
		if( sizeChange ) {
			if( region.width >= image.width || region.height >= image.height )
				throw new IllegalArgumentException("Bad");
			resizeImages(region.width, region.height);
			computeCosineWindow(cosine);
			computeGaussianWeights();
		}
	}

	/**
	 * Learn the target's appearance.
	 */
	private void initialLearning( ImageFloat32 image , int x0 , int y0 ) {
		// get subwindow at current estimated target position, to train classifer
		get_subwindow(image, x0, y0, subInput);
		
		// Kernel Regularized Least-Squares, calculate alphas (in Fourier domain)
		//	k = dense_gauss_kernel(sigma, x);
		dense_gauss_kernel(sigma,subInput,subInput,k);
		fft.forward(k, kf);

		// new_alphaf = yf ./ (fft2(k) + lambda);   %(Eq. 7)
		computeAlphas(gaussianWeightDFT, kf, lambda, alpha, alphaf);

		ImageFloat32 tmp = subInput;
		subInput = subPrev;
		subPrev = tmp;
	}

	/**
	 * Computes the cosine window
	 */
	protected static void computeCosineWindow( ImageFloat32 cosine ) {
		double cosX[] = new double[ cosine.width ];
		for( int x = 0; x < cosine.width; x++ ) {
			cosX[x] = Math.cos( Math.PI*x/(cosine.width-1) );
		}
		for( int y = 0; y < cosine.height; y++ ) {
			int index = cosine.startIndex + y*cosine.stride;
			double cosY = Math.cos( Math.PI*y/(cosine.height-1) );
			for( int x = 0; x < cosine.width; x++ ) {
				cosine.data[index++] = (float)Math.cos(cosX[x]*cosY);
			}
		}
	}

	/**
	 * Computes the weights used in the gaussian kernel
	 */
	private void computeGaussianWeights() {
		// desired output (gaussian shaped), bandwidth proportional to target size
		double output_sigma = (float)(Math.sqrt(region.width*region.height) * output_sigma_factor);

		double left = -0.5/(output_sigma*output_sigma);

		for( int y = 0; y < gaussianWeight.height; y++ ) {
			int index = gaussianWeight.startIndex + y*gaussianWeight.stride;

			int ry = y-gaussianWeight.height/2;

			for( int x = 0; x < gaussianWeight.width; x++ ) {
				int rx = x-gaussianWeight.width/2;

				gaussianWeight.data[index++] = (float)Math.exp(left * (ry * ry + rx * rx));
			}
		}

		fft.forward(gaussianWeight,gaussianWeightDFT);
	}

	protected void resizeImages(int width, int height) {
		subInput.reshape(width,height);
		subPrev.reshape(width,height);
		cosine.reshape(width,height);
		k.reshape(width,height);
		kf.reshape(width,height);
		alpha.reshape(width,height);
		alphaf.reshape(width,height);
		newAlpha.reshape(width,height);
		newAlphaf.reshape(width,height);
		tmpReal0.reshape(width,height);
		tmpFourier0.reshape(width,height);
		tmpFourier1.reshape(width,height);
		tmpFourier2.reshape(width,height);
		gaussianWeight.reshape(width,height);
		gaussianWeightDFT.reshape(width,height);
	}

	/**
	 * Search for the track in the image and
	 *
	 * @param image Next image in the sequence
	 */
	public void performTracking( ImageFloat32 image ) {
		updateTrackLocation(image);
		performLearning(image);
	}

	/**
	 * Find the target inside the current image by searching around its last known location
	 */
	private void updateTrackLocation(ImageFloat32 image) {
		get_subwindow(image, region.tl_x, region.tl_y, subInput);

		// calculate response of the classifier at all locations
		// k = dense_gauss_kernel(sigma, x, z);
		dense_gauss_kernel(sigma, subPrev,subInput,k);
		fft.forward(k,kf);

		// response = real(ifft2(alphaf .* fft2(k)));   %(Eq. 9)
		DiscreteFourierTransformOps.multiplyComplex(alphaf, kf, tmpFourier0);
		fft.inverse(tmpFourier0, tmpReal0);

		// find the pixel with the largest response
		int N = tmpReal0.width*tmpReal0.height;
		int indexBest = -1;
		float valueBest = -1;
		for( int i = 0; i < N; i++ ) {
			float v = tmpReal0.data[i];
			if( v > valueBest ) {
				valueBest = v;
				indexBest = i;
			}
		}

		// peak in region's coordinate system
		int cx = indexBest % tmpReal0.width;
		int cy = indexBest / tmpReal0.width;

		// compute peak in original image coordinate sytem
		region.tl_x = (region.tl_x+cx) - subInput.width/2;
		region.tl_y = (region.tl_y+cy) - subInput.height/2;
	}

	/**
	 * Update the alphas and the track's appearance
	 */
	public void performLearning(ImageFloat32 image) {
		// use the update track location
		get_subwindow(image, region.tl_x, region.tl_y, subInput);

		// Kernel Regularized Least-Squares, calculate alphas (in Fourier domain)
		//	k = dense_gauss_kernel(sigma, x);
		dense_gauss_kernel(sigma, subInput, subInput, k);
		fft.forward(k,kf);

		// new_alphaf = yf ./ (fft2(k) + lambda);   %(Eq. 7)
		computeAlphas(gaussianWeightDFT, kf, lambda, newAlpha, newAlphaf);

		// subsequent frames, interpolate model
		// alphaf = (1 - interp_factor) * alphaf + interp_factor * new_alphaf;
		int N = alpha.width*alpha.height*2;
		for( int i = 0; i < N; i++ ) {
			alphaf.data[i] = (1-interp_factor)*alphaf.data[i] + interp_factor*newAlphaf.data[i];
		}

		// Set the previous image to be an interpolated version
		//		z = (1 - interp_factor) * z + interp_factor * new_z;
		N = subInput.width*subInput.height;
		for( int i = 0; i < N; i++ ) {
			subPrev.data[i] = (1-interp_factor)*subPrev.data[i] + interp_factor*subInput.data[i];
		}
	}

	/**
	 * Gaussian Kernel with dense sampling.
	 *  Evaluates a gaussian kernel with bandwidth SIGMA for all displacements
	 *  between input images X and Y, which must both be MxN. They must also
	 *  be periodic (ie., pre-processed with a cosine window). The result is
	 *  an MxN map of responses.
	 *
	 * @param sigma Gaussian kernel bandwidth
	 * @param x Input image
	 * @param y Input image
	 * @param k Output
	 */
	public void dense_gauss_kernel( float sigma , ImageFloat32 x , ImageFloat32 y , ImageFloat32 k ) {

		InterleavedF32 xf=tmpFourier0,yf,xyf=tmpFourier2;
		ImageFloat32 xy = tmpReal0;
		float yy;

		// find x in Fourier domain
		fft.forward(x, xf);
		float xx = imageDotProduct(x);

		if( x != y ) {
			// general case, x and y are different
			yf = tmpFourier1;
			fft.forward(y,yf);
			yy = imageDotProduct(y);
		} else {
			// auto-correlation of x, avoid repeating a few operations
			yf = xf;
			yy = xx;
		}

		// cross-correlation term in Fourier domain
		elementMultConjB(xf,yf,xyf);
		// convert to spatial domain
		fft.inverse(xyf,xy);

		// calculate gaussian response for all positions
		gaussianKernel(xx,yy,xy,sigma,k);
	}

	/**
	 * Computes the dot product of the image with itself
	 */
	public static float imageDotProduct(ImageFloat32 a) {

		float total = 0;

		for( int y = 0; y < a.height; y++ ) {
			int indexA = a.startIndex + y*a.stride;

			for( int x = 0; x < a.width; x++ ) {
				float value = a.data[indexA];
				total += value*value;
			}
		}

		return total;
	}

	/**
	 * Element-wise multiplication of 'a' and the complex conjugate of 'b'
	 */
	public static void elementMultConjB( InterleavedF32 a , InterleavedF32 b , InterleavedF32 output ) {
		for( int y = 0; y < a.height; y++ ) {

			int index = a.startIndex + y*a.stride;

			for( int x = 0; x < a.width; x++, index += 2 ) {

				float realA = a.data[index];
				float imgA = a.data[index+1];
				float realB = b.data[index];
				float imgB = b.data[index+1];

				output.data[index] = realA*realB + imgA*imgB;
				output.data[index+1] = realA*imgB - imgA*realB;
			}
		}
	}

	/**
	 * new_alphaf = yf ./ (fft2(k) + lambda);   %(Eq. 7)
	 */
	public void computeAlphas( InterleavedF32 yf , InterleavedF32 kf , float lambda ,
							   ImageFloat32 alpha, InterleavedF32 alphaf) {

		for( int y = 0; y < kf.height; y++ ) {

			int index = yf.startIndex + y*yf.stride;

			for( int x = 0; x < kf.width; x++, index += 2 ) {
				float a = kf.data[index] + lambda;
				float b = kf.data[index+1];

				float c = yf.data[index];
				float d = yf.data[index+1];

				float bottom = c*c + d*d;

				alphaf.data[index] = (a*c + b*d)/bottom;
				alphaf.data[index+1] = (b*c - a*d)/bottom;
			}
		}

		fft.inverse(alphaf,alpha);
	}

	/**
	 * k = exp(-1 / sigma^2 * max(0, (xx + yy - 2 * xy) / numel(x)));
	 */
	public static void gaussianKernel( float xx , float yy , ImageFloat32 xy , float sigma  , ImageFloat32 output ) {
		float sigma2 = sigma*sigma;

		for( int y = 0; y < xy.height; y++ ) {
			int indexXY = xy.startIndex + y*xy.stride;
			int indexOut = output.startIndex + y*output.stride;

			for( int x = 0; x < xy.width; x++ ) {

				float valueXY = xy.data[indexXY++];

				double v = Math.sqrt( -(1.0f/sigma2)*(xx + yy - 2.0f*Math.max(0,valueXY)) );

				output.data[indexOut++] = (float)v;
			}

		}
	}

	/**
	 * Copies the target into the output image and applies the cosine window to it.  Takes in account
	 * image bounds.
	 */
	protected void get_subwindow( ImageFloat32 image , int x0 , int y0 , ImageFloat32 output ) {
		// make sure it is in bounds
		if( x0 < 0 )
			x0 = 0;
		if( x0 > image.width-region.width )
			x0 = image.width-region.width;
		if( y0 < 0 )
			y0 = 0;
		if( y0 > image.height-region.height )
			y0 = image.height-region.height;

		// copy the target
		ImageMiscOps.copy(x0, y0, 0, 0, region.width, region.height, image, output);
		// apply the cosine window to it
		PixelMath.multiply(output,cosine,output);
	}


	/**
	 * The location of the target in the image
	 */
	public Rectangle2D_I32 getTargetLocation() {
		return region;
	}

	/**
	 * Visual appearance of the target
	 */
	public ImageFloat32 getTargetTemplate() {
		return subInput;
	}
}
