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

import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;

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
public abstract class HornSchunckPyramid<I extends ImageSingleBand, D extends ImageSingleBand> {

	// used to weight the error of image brightness and smoothness of velocity flow
	protected float alpha2;

	// relaxation parameter for SOR  0 < w < 2.  Recommended default is 1.9
	float w;

	// maximum number of iterations
	int maxIterations;
	// convergence tolerance
	float convergeTolerance;

	// found flow for the most recently processed layer.  Final output is stored here
	ImageFloat32 flowX = new ImageFloat32(1,1);
	ImageFloat32 flowY = new ImageFloat32(1,1);

	// flow estimation at the start of the iteration
	ImageFloat32 initFlowX = new ImageFloat32(1,1);
	ImageFloat32 initFlowY = new ImageFloat32(1,1);

	// interpolation used for passing flow estimates between layers in the image
	InterpolatePixelS<ImageFloat32> interp;

	/**
	 * Configures flow estimation
	 *
	 * @param alpha  Weights importance of image brightness error and velocity smoothness.  Try 15
	 * @param w  relaxation parameter for SOR  0 < w < 2.  Recommended default is 1.9
	 * @param maxIterations Maximum number of Taylor series iterations in each layer.
	 * @param convergeTolerance When the change drops below this value it will stop iterating.  Per pixel error.  Try 1e-4f
	 * @param interp Interpolation for image flow between image layers.
	 */
	public HornSchunckPyramid(float alpha, float w, int maxIterations, float convergeTolerance, InterpolatePixelS<ImageFloat32> interp) {
		this.alpha2 = alpha*alpha;
		this.w = w;
		this.maxIterations = maxIterations;
		this.interp = interp;
		this.convergeTolerance = convergeTolerance;
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
	public void process( ImagePyramid<I> image1 ,
						 ImagePyramid<I> image2 ,
						 D[] derivX2 , D[] derivY2 ) {


		// Process the pyramid from low resolution to high resolution
		boolean first = true;
		for( int i = image1.getNumLayers()-1; i >= 0; i-- ) {
			I layer1 = image1.getLayer(i);
			I layer2 = image2.getLayer(i);
			D layerDX1 = derivX2[i];
			D layerDY1 = derivY2[i];

			if( !first ) {
				// interpolate initial flow from previous layer
				interpolateFlow(layer1.width,layer1.height);
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
	protected void interpolateFlow( int widthNew , int heightNew ) {
		initFlowX.reshape(widthNew,heightNew);
		initFlowY.reshape(widthNew,heightNew);

		interpolateFlow(flowX,initFlowX);
		interpolateFlow(flowY,initFlowY);

		flowX.reshape(widthNew,heightNew);
		flowY.reshape(widthNew,heightNew);

		// init flow contains the initial estimate of the flow vector (if available)
		// flow contains the estimate for each iteration below
		flowX.setTo(initFlowX);
		flowY.setTo(initFlowY);
	}

	protected void interpolateFlow( ImageFloat32 prev , ImageFloat32 curr ) {
		interp.setImage(prev);

		float scaleX = (float)(prev.width-1)/(float)(curr.width-1)*0.999f;
		float scaleY = (float)(prev.height-1)/(float)(curr.height-1)*0.999f;

		float scale = (float)curr.width/(float)prev.width;

		int indexCurr = 0;
		for( int y = 0; y < curr.height; y++ ) {
			for( int x = 0; x < curr.width; x++ ) {
				curr.data[indexCurr++] = interp.get(x*scaleX,y*scaleY)*scale;
			}
		}
	}

	/**
	 * Computes the flow for a layer using Taylor series expansion and Successive Over-Relaxation linear solver.
	 * Flow estimates from previous layers are feed into this by setting initFlow and flow to their values.
	 */
	protected abstract void processLayer( I image1 , I image2 ,  D derivX2 , D derivY2);

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
