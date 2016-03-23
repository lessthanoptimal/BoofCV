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

package boofcv.alg.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.flow.ConfigHornSchunckPyramid;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * <p>
 * Pyramidal implementation of Horn-Schunck [2] based on the discussion in [1].  The problem formulation has been
 * modified from the original found in [2] to account for larger displacements.  The Euler-Lagrange equations
 * are solved using Successive Over-Relaxation (SOR).
 * </p>
 *
 * <ol>
 * <li>Meinhardt-Llopis, Enric and Sánchez Pérez, Javier and Kondermann, Daniel,
 * "Horn-Schunck Optical Flow with a Multi-Scale Strategy" vol 3, 2013, Image Processing On Line</li>
 * <li>Horn, Berthold K., and Brian G. Schunck. "Determining optical flow."
 * 1981 Technical Symposium East. International Society for Optics and Photonics, 1981.</li>
 * </ol>
 *
 *
 * @author Peter Abeles
 */
public class HornSchunckPyramid< T extends ImageGray>
		extends DenseFlowPyramidBase<T>
{
	// used to weight the error of image brightness and smoothness of velocity flow
	private float alpha2;

	// relaxation parameter for SOR  0 < w < 2.  Recommended default is 1.9
	private float SOR_RELAXATION;

	// number of warps for outer loop
	private int numWarps;
	// maximum number of iterations in inner loop
	private int maxInnerIterations;
	// convergence tolerance
	private float convergeTolerance;

	// computes the image gradient
	private ImageGradient<GrayF32, GrayF32> gradient = FactoryDerivative.three(GrayF32.class, GrayF32.class);

	// image gradient second image
	private GrayF32 deriv2X = new GrayF32(1,1);
	private GrayF32 deriv2Y = new GrayF32(1,1);

	// found flow for the most recently processed layer.  Final output is stored here
	protected GrayF32 flowX = new GrayF32(1,1);
	protected GrayF32 flowY = new GrayF32(1,1);

	// flow estimation at the start of the iteration
	protected GrayF32 initFlowX = new GrayF32(1,1);
	protected GrayF32 initFlowY = new GrayF32(1,1);

	// storage for the warped flow
	protected GrayF32 warpImage2 = new GrayF32(1,1);
	protected GrayF32 warpDeriv2X = new GrayF32(1,1);
	protected GrayF32 warpDeriv2Y = new GrayF32(1,1);

	/**
	 * Configures flow estimation
	 *
	 * @param config Configuration parameters
	 * @param interp Interpolation for image flow between image layers and warping.  Overrides selection in config.
	 */
	public HornSchunckPyramid(ConfigHornSchunckPyramid config , InterpolatePixelS<GrayF32> interp)
	{
		super(config.pyrScale,config.pyrSigma,config.pyrMaxLayers,interp);

		this.alpha2 = config.alpha*config.alpha;
		this.SOR_RELAXATION = config.SOR_RELAXATION;
		this.numWarps = config.numWarps;
		this.maxInnerIterations = config.maxInnerIterations;
		this.interp = interp;
		this.convergeTolerance = config.convergeTolerance;
	}

	/**
	 * Computes dense optical flow from the provided image pyramid.  Image gradient for each layer should be
	 * computed directly from the layer images.
	 *
	 * @param image1 Pyramid of first image
	 * @param image2 Pyramid of second image
	 */
	@Override
	public void process( ImagePyramid<GrayF32> image1 ,
						 ImagePyramid<GrayF32> image2 ) {


		// Process the pyramid from low resolution to high resolution
		boolean first = true;
		for( int i = image1.getNumLayers()-1; i >= 0; i-- ) {
			GrayF32 layer1 = image1.getLayer(i);
			GrayF32 layer2 = image2.getLayer(i);

			// declare memory for this layer
			deriv2X.reshape(layer1.width,layer1.height);
			deriv2Y.reshape(layer1.width,layer1.height);
			warpDeriv2X.reshape(layer1.width,layer1.height);
			warpDeriv2Y.reshape(layer1.width,layer1.height);
			warpImage2.reshape(layer1.width,layer1.height);

			// compute the gradient for the second image
			gradient.process(layer2,deriv2X,deriv2Y);

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
			processLayer(layer1,layer2,deriv2X,deriv2Y);
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
	protected void interpolateFlowScale(GrayF32 prev, GrayF32 curr) {
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
	protected void warpImageTaylor(GrayF32 before, GrayF32 flowX , GrayF32 flowY , GrayF32 after) {
		interp.setImage(before);

		for( int y = 0; y < before.height; y++ ) {
			int pixelIndex = y*before.width;
			for (int x = 0; x < before.width; x++, pixelIndex++ ) {
				float u = flowX.data[pixelIndex];
				float v = flowY.data[pixelIndex];

				float wx = x + u;
				float wy = y + v;

				if( wx < 0 || wx > before.width-1 || wy < 0 || wy > before.height-1 ) {
					// setting outside pixels to zero seems to produce smoother results than extending the image
					after.data[pixelIndex] = 0;
				} else {
					after.data[pixelIndex] = interp.get(wx, wy);
				}
			}
		}
	}

	/**
	 * Computes the flow for a layer using Taylor series expansion and Successive Over-Relaxation linear solver.
	 * Flow estimates from previous layers are feed into this by setting initFlow and flow to their values.
	 */
	protected void processLayer(GrayF32 image1 , GrayF32 image2 , GrayF32 derivX2 , GrayF32 derivY2) {

		float w = SOR_RELAXATION;
		float uf,vf;

		// outer Taylor expansion iterations
		for( int warp = 0; warp < numWarps; warp++ ) {

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

				// inner portion
				for( int y = 1; y < image1.height-1; y++ ) {
					int pixelIndex = y*image1.width+1;
					for (int x = 1; x < image1.width-1; x++, pixelIndex++ ) {
						// could speed this up a bit more by precomputing the constant portion before the do-while loop
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

				// border regions require special treatment
				int pixelIndex0 = 0;
				int pixelIndex1 = (image1.height-1)*image1.width;
				for (int x = 0; x < image1.width; x++ ) {
					error += iterationSorSafe(image1,x,0,pixelIndex0++);
					error += iterationSorSafe(image1,x,image1.height-1,pixelIndex1++);
				}

				pixelIndex0 = image1.width;
				 pixelIndex1 = image1.width + image1.width-1;
				for( int y = 1; y < image1.height-1; y++ ) {
					error += iterationSorSafe(image1,0,y,pixelIndex0);
					error += iterationSorSafe(image1,image1.width-1,y,pixelIndex1);

					pixelIndex0 += image1.width;
					pixelIndex1 += image1.width;
				}

			} while( error > convergeTolerance*image1.width*image1.height && ++iter < maxInnerIterations);
		}
	}

	/**
	 * SOR iteration for border pixels
	 */
	private float iterationSorSafe(GrayF32 image1, int x, int y, int pixelIndex) {
		float w = SOR_RELAXATION;

		float uf;
		float vf;
		float ui = initFlowX.data[pixelIndex];
		float vi = initFlowY.data[pixelIndex];

		float u = flowX.data[pixelIndex];
		float v = flowY.data[pixelIndex];

		float I1 = image1.data[pixelIndex];
		float I2 = warpImage2.data[pixelIndex];

		float I2x = warpDeriv2X.data[pixelIndex];
		float I2y = warpDeriv2Y.data[pixelIndex];

		float AU = A_safe(x,y,flowX);
		float AV = A_safe(x,y,flowY);

		flowX.data[pixelIndex] = uf = (1-w)*u + w*((I1-I2+I2x*ui - I2y*(v-vi))*I2x + alpha2*AU)/(I2x*I2x + alpha2);
		flowY.data[pixelIndex] = vf = (1-w)*v + w*((I1-I2+I2y*vi - I2x*(uf-ui))*I2y + alpha2*AV)/(I2y*I2y + alpha2);

		return (uf - u)*(uf - u) + (vf - v)*(vf - v);
	}

	/**
	 * See equation 25.  Safe version
	 */
	protected static float A_safe( int x , int y , GrayF32 flow ) {
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
	 * See equation 25.  Fast unsafe version
	 */
	protected static float A( int x , int y , GrayF32 flow ) {
		int index = flow.getIndex(x,y);

		float u0 = flow.data[index-1];
		float u1 = flow.data[index+1];
		float u2 = flow.data[index-flow.stride];
		float u3 = flow.data[index+flow.stride];

		float u4 = flow.data[index-1-flow.stride];
		float u5 = flow.data[index+1-flow.stride];
		float u6 = flow.data[index-1+flow.stride];
		float u7 = flow.data[index+1+flow.stride];

		return (1.0f/6.0f)*(u0 + u1 + u2 + u3) + (1.0f/12.0f)*(u4 + u5 + u6 + u7);
	}

	/**
	 * Ensures pixel values are inside the image.  If output it is assigned to the nearest pixel inside the image
	 */
	protected static float safe( int x , int y , GrayF32 image ) {
		if( x < 0 ) x = 0;
		else if( x >= image.width ) x = image.width-1;
		if( y < 0 ) y = 0;
		else if( y >= image.height ) y = image.height-1;

		return image.unsafe_get(x,y);
	}

	public GrayF32 getFlowX() {
		return flowX;
	}

	public GrayF32 getFlowY() {
		return flowY;
	}
}
