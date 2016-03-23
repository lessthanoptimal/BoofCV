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

package boofcv.abst.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.flow.DenseOpticalFlowKlt;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ImagePyramid;

import java.lang.reflect.Array;

/**
 * Wrapper around {@link DenseOpticalFlowKlt} for {@link DenseOpticalFlow}.
 *
 * @author Peter Abeles
 */
public class FlowKlt_to_DenseOpticalFlow<I extends ImageGray, D extends ImageGray>
	implements DenseOpticalFlow<I>
{
	DenseOpticalFlowKlt<I,D> flowKlt;
	ImageGradient<I,D> gradient;

	ImagePyramid<I> pyramidSrc;
	ImagePyramid<I> pyramidDst;

	D[] srcDerivX;
	D[] srcDerivY;

	ImageType<I> imageType;

	public FlowKlt_to_DenseOpticalFlow(DenseOpticalFlowKlt<I, D> flowKlt,
									   ImageGradient<I, D> gradient,
									   ImagePyramid<I> pyramidSrc,
									   ImagePyramid<I> pyramidDst,
									   Class<I> inputType , Class<D> derivType ) {
		if( pyramidSrc.getNumLayers() != pyramidDst.getNumLayers() )
			throw new IllegalArgumentException("Pyramids do not have the same number of layers!");

		this.flowKlt = flowKlt;
		this.gradient = gradient;
		this.pyramidSrc = pyramidSrc;
		this.pyramidDst = pyramidDst;

		srcDerivX = (D[])Array.newInstance(derivType,pyramidSrc.getNumLayers());
		srcDerivY = (D[])Array.newInstance(derivType,pyramidSrc.getNumLayers());

		for( int i = 0; i < srcDerivX.length; i++ ) {
			srcDerivX[i] = GeneralizedImageOps.createSingleBand(derivType,1,1);
			srcDerivY[i] = GeneralizedImageOps.createSingleBand(derivType,1,1);
		}

		imageType = ImageType.single(inputType);
	}

	@Override
	public void process(I source, I destination, ImageFlow flow) {
		pyramidSrc.process(source);
		pyramidDst.process(destination);

		PyramidOps.reshapeOutput(pyramidSrc,srcDerivX);
		PyramidOps.reshapeOutput(pyramidSrc,srcDerivY);

		PyramidOps.gradient(pyramidSrc, gradient, srcDerivX,srcDerivY);

		flowKlt.process(pyramidSrc,srcDerivX,srcDerivY,pyramidDst,flow);
	}

	@Override
	public ImageType<I> getInputType() {
		return imageType;
	}
}
