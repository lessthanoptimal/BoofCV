/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidFloat;

/**
 * Pyramidal implementation of Horn-Schunck based on the discussion in [1].  The problem formulation has been
 * modified from the original found in [2] to account for larger displacements.
 *
 *
 * The image derivative should have a thickness of 1 for a straight edge.  If it is thicker you run into a situation
 * where the weight given to image difference is much greater than smoothing, but there is no image difference
 * at that point, causing the flow to get stuck.
 *
 * TODO finish commenting
 *
 * @author Peter Abeles
 */
public class HornSchunckPyramid {

	// used to weight the error of image brightness and smoothness of velocity flow
	protected float alpha2;

	// relaxation parameter for SOR  0 < w < 2.  Recommended default is 1.9
	float SOR_RELAXATION;

	// number of warps for outer loop
	int numWarps;
	// maximum number of iterations in inner loop
	int maxInnerIterations;
	// convergence tolerance
	float convergeTolerance;

	// parameters used to create pyramid
	double scale;
	double sigma;
	int maxLayers;

	// storage for normalized image
	ImageFloat32 norm1 = new ImageFloat32(1,1);
	ImageFloat32 norm2 = new ImageFloat32(1,1);

	// image pyramid and its derivative
	PyramidFloat<ImageFloat32> pyr1;
	PyramidFloat<ImageFloat32> pyr2;

	ImageFloat32 deriv2X[];
	ImageFloat32 deriv2Y[];

	// computes the gradient
	ImageGradient<ImageFloat32, ImageFloat32> gradient = FactoryDerivative.two(ImageFloat32.class, ImageFloat32.class);

	// found flow for the most recently processed layer.  Final output is stored here
	ImageFloat32 flowX = new ImageFloat32(1,1);
	ImageFloat32 flowY = new ImageFloat32(1,1);

	// flow estimation at the start of the iteration
	ImageFloat32 initFlowX = new ImageFloat32(1,1);
	ImageFloat32 initFlowY = new ImageFloat32(1,1);

	// storage for the warped flow
	ImageFloat32 warpImage2 = new ImageFloat32(1,1);
	ImageFloat32 warpDeriv2X = new ImageFloat32(1,1);
	ImageFloat32 warpDeriv2Y = new ImageFloat32(1,1);

	// Used to interpolate values between pixels
	InterpolatePixelS<ImageFloat32> interp;

	/**
	 * Configures flow estimation
	 *
	 * @param alpha  Weights importance of image brightness error and velocity smoothness.  Try 15
	 * @param SOR_RELAXATION  relaxation parameter for SOR  0 < w < 2.  Recommended default is 1.9
	 * @param numWarps Number of Taylor series iterations in each layer. Try 10
	 * @param maxInnerIterations Maximum number inner iterations.  Try 100
	 * @param convergeTolerance When the change drops below this value it will stop iterating.  Per pixel error.  Try 1e-4f
	 * @param pyrScale
	 * @param pyrSigma
	 * @param maxLayers
	 * @param interp Interpolation for image flow between image layers and warping.
	 */
	public HornSchunckPyramid(float alpha, float SOR_RELAXATION, int numWarps, int maxInnerIterations, float convergeTolerance,
							  double pyrScale, double pyrSigma , int maxLayers,
							  InterpolatePixelS<ImageFloat32> interp) {
		this.alpha2 = alpha*alpha;
		this.SOR_RELAXATION = SOR_RELAXATION;
		this.numWarps = numWarps;
		this.maxInnerIterations = maxInnerIterations;
		this.interp = interp;
		this.convergeTolerance = convergeTolerance;
		this.scale = pyrScale;
		this.sigma = pyrSigma;
		this.maxLayers = maxLayers;
	}

	/**
	 * Processes the raw input images.  Normalizes them and creates image pyramids from them.
	 */
	public void process( ImageFloat32 image1 , ImageFloat32 image2 ) {
		// declare image data structures
		if( pyr1 == null || pyr1.getInputWidth() != image1.width || pyr1.getInputHeight() != image1.height ) {
			pyr1 = UtilDenseOpticalFlow.standardPyramid(image1.width, image1.height, scale, sigma, 5, maxLayers, ImageFloat32.class);
			pyr2 = UtilDenseOpticalFlow.standardPyramid(image1.width, image1.height, scale, sigma, 5, maxLayers, ImageFloat32.class);

			pyr1.initialize(image1.width,image1.height);
			pyr2.initialize(image1.width,image1.height);

			deriv2X = PyramidOps.declareOutput(pyr2, ImageFloat32.class);
			deriv2Y = PyramidOps.declareOutput(pyr2,ImageFloat32.class);
		}

		norm1.reshape(image1.width,image1.height);
		norm2.reshape(image1.width,image1.height);

		// normalize input image to make sure alpha is image independent
		imageNormalization(image1, image2, norm1, norm2);

		// create image pyramid
		pyr1.process(norm1);
		pyr2.process(norm2);

		PyramidOps.gradient(pyr2,gradient,deriv2X,deriv2Y);

		// compute flow from pyramid
		process(pyr1,pyr2,deriv2X,deriv2Y);
	}

	/**
	 * Function to normalize the images between 0 and 255.
	 **/
	private static void imageNormalization( ImageFloat32 image1, ImageFloat32 image2,
											ImageFloat32 normalized1, ImageFloat32 normalized2 )
	{
		// find the max and min of both images
		final float max1 = ImageStatistics.max(image1);
		final float max2 = ImageStatistics.max(image2);
		final float min1 = ImageStatistics.min(image1);
		final float min2 = ImageStatistics.min(image2);

		// obtain the absolute max and min
		final float max = max1 > max2 ? max1 : max2;
		final float min = min1 < min2 ? min1 : min2;
		final float den = max - min;

		final int size = image1.width*image1.height;

		if(den > 0) {
			// normalize both images
			for(int i = 0; i < size; i++)
			{
				normalized1.data[i] = (image1.data[i] - min) / den;
				normalized2.data[i] = (image2.data[i] - min) / den;
			}
		} else {
			normalized1.setTo(image1);
			normalized2.setTo(image2);
		}
	}

	/**
	 * Computes dense optical flow from the provided image pyramid.  Image gradient for each layer should be
	 * computed directly from the layer images.
	 *
	 * @param image1 Pyramid of first image
	 * @param image2 Pyramid of second image
	 * @param derivX2 Pyramid of image derive-x computed from second image pyramid
	 * @param derivY2 Pyramid of image derive-y computed from second image pyramid
	 */
	public void process( ImagePyramid<ImageFloat32> image1 ,
						 ImagePyramid<ImageFloat32> image2 ,
						 ImageFloat32[] derivX2 , ImageFloat32[] derivY2 ) {


		// Process the pyramid from low resolution to high resolution
		boolean first = true;
		for( int i = image1.getNumLayers()-1; i >= 0; i-- ) {
			ImageFloat32 layer1 = image1.getLayer(i);
			ImageFloat32 layer2 = image2.getLayer(i);
			ImageFloat32 layerDX1 = derivX2[i];
			ImageFloat32 layerDY1 = derivY2[i];

			warpDeriv2X.reshape(layer1.width,layer1.height);
			warpDeriv2Y.reshape(layer1.width,layer1.height);
			warpImage2.reshape(layer1.width,layer1.height);

			if( !first ) {
				// interpolate initial flow from previous layer
				interpolateFlowScale(layer1.width, layer1.height);
			} else {
				// for the very first layer there is no information on flow so set everything to 0
				first = false;

				initFlowX.reshape(layer1.width,layer1.height);
				initFlowY.reshape(layer1.width,layer1.height);
				flowX.reshape(layer1.width,layer1.height);
				flowY.reshape(layer1.width,layer1.height);

				ImageMiscOps.fill(flowX,0);
				ImageMiscOps.fill(flowY,0);
				ImageMiscOps.fill(initFlowX,0);
				ImageMiscOps.fill(initFlowY,0);
			}

			// compute flow for this layer
			processLayer(layer1,layer2,layerDX1,layerDY1);
		}
	}

	/**
	 * Provides an initial estimate for the flow by interpolating values from the previous layer.
	 */
	protected void interpolateFlowScale(int widthNew, int heightNew) {
		initFlowX.reshape(widthNew,heightNew);
		initFlowY.reshape(widthNew,heightNew);

		interpolateFlowScale(flowX, initFlowX);
		interpolateFlowScale(flowY, initFlowY);

		flowX.reshape(widthNew,heightNew);
		flowY.reshape(widthNew,heightNew);

		// init flow contains the initial estimate of the flow vector (if available)
		// flow contains the estimate for each iteration below
		flowX.setTo(initFlowX);
		flowY.setTo(initFlowY);
	}

	/**
	 * Takes the flow from the previous lower resolution layer and uses it to initialize the flow
	 * in the current layer.  Adjusts for change in image scale.
	 */
	protected void interpolateFlowScale(ImageFloat32 prev, ImageFloat32 curr) {
		interp.setImage(prev);

		float scaleX = (float)(prev.width-1)/(float)(curr.width-1)*0.999f;
		float scaleY = (float)(prev.height-1)/(float)(curr.height-1)*0.999f;

		float scale = (float)prev.width/(float)curr.width;

		int indexCurr = 0;
		for( int y = 0; y < curr.height; y++ ) {
			for( int x = 0; x < curr.width; x++ ) {
				curr.data[indexCurr++] = interp.get(x*scaleX,y*scaleY)/scale;
			}
		}
	}

	/**
	 * Takes the flow from the previous lower resolution layer and uses it to initialize the flow
	 * in the current layer.  Adjusts for change in image scale.
	 */
	protected void warpImageTaylor(ImageFloat32 before, ImageFloat32 flowX , ImageFloat32 flowY , ImageFloat32 after) {
		interp.setImage(before);

		for( int y = 0; y < before.height; y++ ) {
			int pixelIndex = y*before.width;
			for (int x = 0; x < before.width; x++, pixelIndex++ ) {
				float u = flowX.data[pixelIndex];
				float v = flowY.data[pixelIndex];

				float wx = x + u;
				float wy = y + v;

				if( wx < 0) wx = 0;
				else if( wx > before.width-1 )
					wx = before.width-1;
				if( wy < 0) wy = 0;
				else if( wy > before.height-1 )
					wy = before.height-1;

				after.data[pixelIndex] = interp.get(wx, wy);
			}
		}
	}

	/**
	 * Computes the flow for a layer using Taylor series expansion and Successive Over-Relaxation linear solver.
	 * Flow estimates from previous layers are feed into this by setting initFlow and flow to their values.
	 */
	protected void processLayer( ImageFloat32 image1 , ImageFloat32 image2 , ImageFloat32 derivX2 , ImageFloat32 derivY2) {

		float w = SOR_RELAXATION;
		float uf,vf;

		// outer Taylor expansion iterations
		for( int warp = 0; warp < numWarps; warp++ ) {
			System.out.println("WARP: "+warp);

			initFlowX.setTo(flowX);
			initFlowY.setTo(flowY);

			warpImageTaylor(derivX2, initFlowX, initFlowY, warpDeriv2X);
			warpImageTaylor(derivY2, initFlowX, initFlowY, warpDeriv2Y);
			warpImageTaylor(image2, initFlowX, initFlowY, warpImage2);

			float error;
			int iter = 0;

			do {
				// inner SOR iteration.
				error = 0;

				for( int y = 0; y < image1.height; y++ ) {
					int pixelIndex = y*image1.width;
					for (int x = 0; x < image1.width; x++, pixelIndex++ ) {
						float ui = initFlowX.data[pixelIndex];
						float vi = initFlowY.data[pixelIndex];

						float u = flowX.data[pixelIndex];
						float v = flowY.data[pixelIndex];

						float I1 = image1.data[pixelIndex];
						float I2 = warpImage2.data[pixelIndex];

						float I2x = warpDeriv2X.data[pixelIndex];
						float I2y = warpDeriv2Y.data[pixelIndex];

						float AU = A(x,y,flowX);
						float AV = A(x,y,flowY);

						flowX.data[pixelIndex] = uf = (1-w)*u + w*((I1-I2+I2x*ui - I2y*(v-vi))*I2x + alpha2*AU)/(I2x*I2x + alpha2);
						flowY.data[pixelIndex] = vf = (1-w)*v + w*((I1-I2+I2y*vi - I2x*(uf-ui))*I2y + alpha2*AV)/(I2y*I2y + alpha2);

						error += (uf - u)*(uf - u) + (vf - v)*(vf - v);
					}
				}
				iter++;
			} while( error > convergeTolerance*image1.width*image1.height && iter < maxInnerIterations);
		}
	}

	/**
	 * See equation 25.
	 */
	// TODO optimize by having and inner and border version
	protected static float A( int x , int y , ImageFloat32 flow ) {
		float u0 = safe(x-1,y  ,flow);
		float u1 = safe(x+1,y  ,flow);
		float u2 = safe(x  ,y-1,flow);
		float u3 = safe(x  ,y+1,flow);

		float u4 = safe(x-1,y-1,flow);
		float u5 = safe(x+1,y-1,flow);
		float u6 = safe(x-1,y+1,flow);
		float u7 = safe(x+1,y+1,flow);

		return (1.0f/6.0f)*(u0 + u1 + u2 + u3) + (1.0f/12.0f)*(u4 + u5 + u6 + u7);
	}

	/**
	 * Ensures pixel values are inside the image.  If output it is assigned to the nearest pixel inside the image
	 */
	protected static float safe( int x , int y , ImageFloat32 image ) {
		if( x < 0 ) x = 0;
		else if( x >= image.width ) x = image.width-1;
		if( y < 0 ) y = 0;
		else if( y >= image.height ) y = image.height-1;

		return image.unsafe_get(x,y);
	}

	public ImageFloat32 getFlowX() {
		return flowX;
	}

	public ImageFloat32 getFlowY() {
		return flowY;
	}
}
