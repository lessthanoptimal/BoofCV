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

package boofcv.alg.tracker.circulant;

import boofcv.abst.feature.detect.peak.SearchLocalPeak;
import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.transform.fft.DiscreteFourierTransformOps;
import boofcv.factory.feature.detect.peak.FactorySearchLocalPeak;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayF64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.InterleavedF64;
import georegression.struct.shapes.RectangleLength2D_F32;

import java.util.Random;

/**
 * <p>
 * Tracker that uses the theory of Circulant matrices, Discrete Fourier Transform (DCF), and linear classifiers to track
 * a target and learn its changes in appearance [1].  The target is assumed to be rectangular and has fixed size and
 * location.  A dense local search is performed around the most recent target location.  The search is done quickly
 * using the DCF.
 * </p>
 *
 * <p>
 * Tracking is performed using texture information.  Since only one description of the target is saved, tracks can
 * drift over time.  Tracking performance seems to improve if the object has distinctive edges.
 * </p>
 *
 * <p>
 * CHANGES FROM PAPER:<br>
 * <ul>
 * <li>Input image is sampled into a square work region of constant size to improve runtime speed of FFT.</li>
 * <li>Peak of response is found using mean-shift.  Provides sub-pixel precision.</li>
 * <li>Pixels outside the image are assigned random values to avoid the tracker from fitting to them. Ideally they
 * wouldn't be processed, but that is complex to implement </li>
 * </ul>
 * </p>
 *
 * <p>
 * [1] Henriques, Joao F., et al. "Exploiting the circulant structure of tracking-by-detection with kernels."
 * Computer Vision–ECCV 2012. Springer Berlin Heidelberg, 2012. 702-715.
 * </p>
 *
 * @author Peter Abeles
 */
public class CirculantTracker<T extends ImageGray> {

	// --- Tuning parameters
	// spatial bandwidth (proportional to target)
	private double output_sigma_factor;

	// gaussian kernel bandwidth
	private double sigma;

	// regularization term
	private double lambda;
	// linear interpolation term.  Adjusts how fast it can learn
	private double interp_factor;

	// the maximum pixel value
	private double maxPixelValue;

	// extra padding around the selected region
	private double padding;

	//----- Internal variables
	// computes the FFT
	private DiscreteFourierTransform<GrayF64,InterleavedF64> fft = DiscreteFourierTransformOps.createTransformF64();

	// storage for subimage of input image
	protected GrayF64 templateNew = new GrayF64(1,1);
	// storage for the subimage of the previous frame
	protected GrayF64 template = new GrayF64(1,1);

	// cosine window used to reduce artifacts from FFT
	protected GrayF64 cosine = new GrayF64(1,1);

	// Storage for the kernel's response
	private GrayF64 k = new GrayF64(1,1);
	private InterleavedF64 kf = new InterleavedF64(1,1,2);

	// Learn values.  used to compute weight in linear classifier
	private InterleavedF64 alphaf = new InterleavedF64(1,1,2);
	private InterleavedF64 newAlphaf = new InterleavedF64(1,1,2);

	// location of target
	protected RectangleLength2D_F32 regionTrack = new RectangleLength2D_F32();
	protected RectangleLength2D_F32 regionOut = new RectangleLength2D_F32();

	// Used for computing the gaussian kernel
	protected GrayF64 gaussianWeight = new GrayF64(1,1);
	protected InterleavedF64 gaussianWeightDFT = new InterleavedF64(1,1,2);

	// detector response
	private GrayF64 response = new GrayF64(1,1);

	// storage for storing temporary results
	private GrayF64 tmpReal0 = new GrayF64(1,1);
	private GrayF64 tmpReal1 = new GrayF64(1,1);

	private InterleavedF64 tmpFourier0 = new InterleavedF64(1,1,2);
	private InterleavedF64 tmpFourier1 = new InterleavedF64(1,1,2);
	private InterleavedF64 tmpFourier2 = new InterleavedF64(1,1,2);

	// interpolation used when sampling input image into work space
	private InterpolatePixelS<T> interp;

	// used to compute sub-pixel location
	private SearchLocalPeak<GrayF64> localPeak =
			FactorySearchLocalPeak.meanShiftUniform(5, 1e-4f, GrayF64.class);

	// adjustment from sub-pixel
	protected float offX,offY;

	// size of the work space in pixels
	private int workRegionSize;
	// conversion from workspace to image pixels
	private float stepX,stepY;

	// used to fill the area outside of the image with unstructured data.
	private Random rand = new Random(234);

	/**
	 * Configure tracker
	 *
	 * @param output_sigma_factor  spatial bandwidth (proportional to target) Try 1.0/16.0
	 * @param sigma Sigma for Gaussian kernel in linear classifier.  Try 0.2
	 * @param lambda Try 1e-2
	 * @param interp_factor Try 0.075
	 * @param padding Padding added around the selected target.  Try 1
	 * @param workRegionSize Size of work region. Best if power of 2.  Try 64
	 * @param maxPixelValue Maximum pixel value.  Typically 255
	 */
	public CirculantTracker(double output_sigma_factor, double sigma, double lambda, double interp_factor,
							double padding ,
							int workRegionSize ,
							double maxPixelValue,
							InterpolatePixelS<T> interp ) {
		if( workRegionSize < 3 )
			throw new IllegalArgumentException("Minimum size of work region is 3 pixels.");

		this.output_sigma_factor = output_sigma_factor;
		this.sigma = sigma;
		this.lambda = lambda;
		this.interp_factor = interp_factor;
		this.maxPixelValue = maxPixelValue;
		this.interp = interp;

		this.padding = padding;
		this.workRegionSize = workRegionSize;

		resizeImages(workRegionSize);
		computeCosineWindow(cosine);
		computeGaussianWeights(workRegionSize);

		localPeak.setImage(response);
	}

	/**
	 * Initializes tracking around the specified rectangle region
	 * @param image Image to start tracking from
	 * @param x0 top-left corner of region
	 * @param y0 top-left corner of region
	 * @param regionWidth region's width
	 * @param regionHeight region's height
	 */
	public void initialize( T image , int x0 , int y0 , int regionWidth , int regionHeight ) {

		if( image.width < regionWidth || image.height < regionHeight)
			throw new IllegalArgumentException("Track region is larger than input image: "+regionWidth+" "+regionHeight);

		regionOut.width = regionWidth;
		regionOut.height = regionHeight;

		// adjust for padding
		int w = (int)(regionWidth*(1+padding));
		int h = (int)(regionHeight*(1+padding));
		int cx = x0 + regionWidth/2;
		int cy = y0 + regionHeight/2;

		// save the track location
		this.regionTrack.width = w;
		this.regionTrack.height = h;
		this.regionTrack.x0 = cx-w/2;
		this.regionTrack.y0 = cy-h/2;

		stepX = (w-1)/(float)(workRegionSize-1);
		stepY = (h-1)/(float)(workRegionSize-1);

		updateRegionOut();

		initialLearning(image);
	}


	/**
	 * Learn the target's appearance.
	 */
	protected void initialLearning( T image ) {
		// get subwindow at current estimated target position, to train classifier
		get_subwindow(image, template);

		// Kernel Regularized Least-Squares, calculate alphas (in Fourier domain)
		//	k = dense_gauss_kernel(sigma, x);
		dense_gauss_kernel(sigma, template, template,k);
		fft.forward(k, kf);

		// new_alphaf = yf ./ (fft2(k) + lambda);   %(Eq. 7)
		computeAlphas(gaussianWeightDFT, kf, lambda, alphaf);
	}

	/**
	 * Computes the cosine window
	 */
	protected static void computeCosineWindow( GrayF64 cosine ) {
		double cosX[] = new double[ cosine.width ];
		for( int x = 0; x < cosine.width; x++ ) {
			cosX[x] = 0.5*(1 - Math.cos( 2.0*Math.PI*x/(cosine.width-1) ));
		}
		for( int y = 0; y < cosine.height; y++ ) {
			int index = cosine.startIndex + y*cosine.stride;
			double cosY = 0.5*(1 - Math.cos( 2.0*Math.PI*y/(cosine.height-1) ));
			for( int x = 0; x < cosine.width; x++ ) {
				cosine.data[index++] = cosX[x]*cosY;
			}
		}
	}

	/**
	 * Computes the weights used in the gaussian kernel
	 *
	 * This isn't actually symmetric for even widths.  These weights are used has label in the learning phase.  Closer
	 * to one the more likely it is the true target.  It should be a peak in the image center.  If it is not then
	 * it will learn an incorrect model.
	 */
	protected void computeGaussianWeights( int width ) {
		// desired output (gaussian shaped), bandwidth proportional to target size
		double output_sigma = Math.sqrt(width*width) * output_sigma_factor;

		double left = -0.5/(output_sigma*output_sigma);

		int radius = width/2;

		for( int y = 0; y < gaussianWeight.height; y++ ) {
			int index = gaussianWeight.startIndex + y*gaussianWeight.stride;

			double ry = y-radius;

			for( int x = 0; x < width; x++ ) {
				double rx = x-radius;

				gaussianWeight.data[index++] = Math.exp(left * (ry * ry + rx * rx));
			}
		}

		fft.forward(gaussianWeight,gaussianWeightDFT);
	}


	protected void resizeImages( int workRegionSize ) {
		templateNew.reshape(workRegionSize, workRegionSize);
		template.reshape(workRegionSize, workRegionSize);
		cosine.reshape(workRegionSize,workRegionSize);
		k.reshape(workRegionSize,workRegionSize);
		kf.reshape(workRegionSize,workRegionSize);
		alphaf.reshape(workRegionSize,workRegionSize);
		newAlphaf.reshape(workRegionSize,workRegionSize);
		response.reshape(workRegionSize,workRegionSize);
		tmpReal0.reshape(workRegionSize,workRegionSize);
		tmpReal1.reshape(workRegionSize,workRegionSize);
		tmpFourier0.reshape(workRegionSize,workRegionSize);
		tmpFourier1.reshape(workRegionSize,workRegionSize);
		tmpFourier2.reshape(workRegionSize,workRegionSize);
		gaussianWeight.reshape(workRegionSize,workRegionSize);
		gaussianWeightDFT.reshape(workRegionSize,workRegionSize);
	}

	/**
	 * Search for the track in the image and
	 *
	 * @param image Next image in the sequence
	 */
	public void performTracking( T image ) {
		updateTrackLocation(image);
		if( interp_factor != 0 )
			performLearning(image);
	}

	/**
	 * Find the target inside the current image by searching around its last known location
	 */
	protected void updateTrackLocation(T image) {
		get_subwindow(image, templateNew);

		// calculate response of the classifier at all locations
		// matlab: k = dense_gauss_kernel(sigma, x, z);
		dense_gauss_kernel(sigma, templateNew, template,k);

		fft.forward(k,kf);

		// response = real(ifft2(alphaf .* fft2(k)));   %(Eq. 9)
		DiscreteFourierTransformOps.multiplyComplex(alphaf, kf, tmpFourier0);
		fft.inverse(tmpFourier0, response);

		// find the pixel with the largest response
		int N = response.width*response.height;
		int indexBest = -1;
		double valueBest = -1;
		for( int i = 0; i < N; i++ ) {
			double v = response.data[i];
			if( v > valueBest ) {
				valueBest = v;
				indexBest = i;
			}
		}

		int peakX = indexBest % response.width;
		int peakY = indexBest / response.width;

		// sub-pixel peak estimation
		subpixelPeak(peakX, peakY);

		// peak in region's coordinate system
		float deltaX = (peakX+offX) - templateNew.width/2;
		float deltaY = (peakY+offY) - templateNew.height/2;

		// convert peak location into image coordinate system
		regionTrack.x0 = regionTrack.x0 + deltaX*stepX;
		regionTrack.y0 = regionTrack.y0 + deltaY*stepY;

		updateRegionOut();
	}

	/**
	 * Refine the local-peak using a search algorithm for sub-pixel accuracy.
	 */
	protected void subpixelPeak(int peakX, int peakY) {
		// this function for r was determined empirically by using work regions of 32,64,128
		int r = Math.min(2,response.width/25);
		if( r < 0 )
			return;

		localPeak.setSearchRadius(r);
		localPeak.search(peakX,peakY);

		offX = localPeak.getPeakX() - peakX;
		offY = localPeak.getPeakY() - peakY;
	}

	private void updateRegionOut() {
		regionOut.x0 = (regionTrack.x0+((int)regionTrack.width)/2)-((int)regionOut.width)/2;
		regionOut.y0 = (regionTrack.y0+((int)regionTrack.height)/2)-((int)regionOut.height)/2;
	}

	/**
	 * Update the alphas and the track's appearance
	 */
	public void performLearning(T image) {
		// use the update track location
		get_subwindow(image, templateNew);

		// Kernel Regularized Least-Squares, calculate alphas (in Fourier domain)
		//	k = dense_gauss_kernel(sigma, x);
		dense_gauss_kernel(sigma, templateNew, templateNew, k);
		fft.forward(k,kf);

		// new_alphaf = yf ./ (fft2(k) + lambda);   %(Eq. 7)
		computeAlphas(gaussianWeightDFT, kf, lambda, newAlphaf);

		// subsequent frames, interpolate model
		// alphaf = (1 - interp_factor) * alphaf + interp_factor * new_alphaf;
		int N = alphaf.width*alphaf.height*2;
		for( int i = 0; i < N; i++ ) {
			alphaf.data[i] = (1-interp_factor)*alphaf.data[i] + interp_factor*newAlphaf.data[i];
		}

		// Set the previous image to be an interpolated version
		//		z = (1 - interp_factor) * z + interp_factor * new_z;
		N = templateNew.width* templateNew.height;
		for( int i = 0; i < N; i++ ) {
			template.data[i] = (1-interp_factor)* template.data[i] + interp_factor*templateNew.data[i];
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
	 * @param k Output containing Gaussian kernel for each element in target region
	 */
	public void dense_gauss_kernel(double sigma , GrayF64 x , GrayF64 y , GrayF64 k ) {

		InterleavedF64 xf=tmpFourier0,yf,xyf=tmpFourier2;
		GrayF64 xy = tmpReal0;
		double yy;

		// find x in Fourier domain
		fft.forward(x, xf);
		double xx = imageDotProduct(x);

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

		//----   xy = invF[ F(x)*F(y) ]
		// cross-correlation term in Fourier domain
		elementMultConjB(xf,yf,xyf);
		// convert to spatial domain
		fft.inverse(xyf,xy);
		circshift(xy,tmpReal1);

		// calculate gaussian response for all positions
		gaussianKernel(xx, yy, tmpReal1, sigma, k);
	}

	public static void circshift(GrayF64 a, GrayF64 b ) {
		int w2 = a.width/2;
		int h2 = b.height/2;

		for( int y = 0; y < a.height; y++ ) {
			int yy = (y+h2)%a.height;

			for( int x = 0; x < a.width; x++ ) {
				int xx = (x+w2)%a.width;

				b.set( xx , yy , a.get(x,y));
			}
		}

	}

	/**
	 * Computes the dot product of the image with itself
	 */
	public static double imageDotProduct(GrayF64 a) {

		double total = 0;

		int N = a.width*a.height;
		for( int index = 0; index < N; index++ ) {
			double value = a.data[index];
			total += value*value;
		}

		return total;
	}

	/**
	 * Element-wise multiplication of 'a' and the complex conjugate of 'b'
	 */
	public static void elementMultConjB( InterleavedF64 a , InterleavedF64 b , InterleavedF64 output ) {
		for( int y = 0; y < a.height; y++ ) {

			int index = a.startIndex + y*a.stride;

			for( int x = 0; x < a.width; x++, index += 2 ) {

				double realA = a.data[index];
				double imgA = a.data[index+1];
				double realB = b.data[index];
				double imgB = b.data[index+1];

				output.data[index] = realA*realB + imgA*imgB;
				output.data[index+1] = -realA*imgB + imgA*realB;
			}
		}
	}

	/**
	 * new_alphaf = yf ./ (fft2(k) + lambda);   %(Eq. 7)
	 */
	protected static void computeAlphas( InterleavedF64 yf , InterleavedF64 kf , double lambda ,
										 InterleavedF64 alphaf ) {

		for( int y = 0; y < kf.height; y++ ) {

			int index = yf.startIndex + y*yf.stride;

			for( int x = 0; x < kf.width; x++, index += 2 ) {
				double a = yf.data[index];
				double b = yf.data[index+1];

				double c = kf.data[index] + lambda;
				double d = kf.data[index+1];

				double bottom = c*c + d*d;

				alphaf.data[index] = (a*c + b*d)/bottom;
				alphaf.data[index+1] = (b*c - a*d)/bottom;
			}
		}
	}

	/**
	 * Computes the output of the Gaussian kernel for each element in the target region
	 *
	 * k = exp(-1 / sigma^2 * max(0, (xx + yy - 2 * xy) / numel(x)));
	 *
	 * @param xx ||x||^2
	 * @param yy ||y||^2
	 */
	protected static void gaussianKernel(double xx , double yy , GrayF64 xy , double sigma  , GrayF64 output ) {
		double sigma2 = sigma*sigma;
		double N = xy.width*xy.height;

		for( int y = 0; y < xy.height; y++ ) {
			int index = xy.startIndex + y*xy.stride;

			for( int x = 0; x < xy.width; x++ , index++ ) {

				// (xx + yy - 2 * xy) / numel(x)
				double value = (xx + yy - 2*xy.data[index])/N;

				double v = Math.exp(-Math.max(0, value) / sigma2);

				output.data[index] = v;
			}
		}
	}

	/**
	 * Copies the target into the output image and applies the cosine window to it.
	 */
	protected void get_subwindow( T image , GrayF64 output ) {

		// copy the target region

		interp.setImage(image);
		int index = 0;
		for( int y = 0; y < workRegionSize; y++ ) {
			float yy = regionTrack.y0 + y*stepY;

			for( int x = 0; x < workRegionSize; x++ ) {
				float xx = regionTrack.x0 + x*stepX;

				if( interp.isInFastBounds(xx,yy))
					output.data[index++] = interp.get_fast(xx,yy);
				else if( BoofMiscOps.checkInside(image, xx, yy))
					output.data[index++] = interp.get(xx, yy);
				else {
					// randomize to make pixels outside the image poorly correlate.  It will then focus on matching
					// what's inside the image since it has structure
					output.data[index++] = rand.nextFloat()*maxPixelValue;
				}
			}
		}

		// normalize values to be from -0.5 to 0.5
		PixelMath.divide(output, maxPixelValue, output);
		PixelMath.plus(output, -0.5f, output);
		// apply the cosine window to it
		PixelMath.multiply(output,cosine,output);
	}

	/**
	 * The location of the target in the image
	 */
	public RectangleLength2D_F32 getTargetLocation() {
		return regionOut;
	}

	/**
	 * Visual appearance of the target
	 */
	public GrayF64 getTargetTemplate() {
		return template;
	}

	public GrayF64 getResponse() {
		return response;
	}
}
