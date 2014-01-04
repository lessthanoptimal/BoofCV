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

package boofcv.abst.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.flow.DenseOpticalFlowKlt;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.flow.FlowImage;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * Wrapper around {@link DenseOpticalFlowKlt} for {@link DenseOpticalFlow}.
 *
 * @author Peter Abeles
 */
public class FlowKlt_to_DenseOpticalFlow<I extends ImageSingleBand, D extends ImageSingleBand>
	implements DenseOpticalFlow<I>
{
	DenseOpticalFlowKlt<I,D> flowKlt;
	ImageGradient<I,D> gradient;

	D derivX,derivY;

	ImageType<I> imageType;

	public FlowKlt_to_DenseOpticalFlow(DenseOpticalFlowKlt<I, D> flowKlt,
									   ImageGradient<I, D> gradient,
									   Class<I> inputType , Class<D> derivType ) {
		this.flowKlt = flowKlt;
		this.gradient = gradient;

		derivX = GeneralizedImageOps.createSingleBand(derivType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(derivType,1,1);

		imageType = ImageType.single(inputType);
	}

	@Override
	public void process(I source, I destination, FlowImage flow) {
		derivX.reshape(source.width,source.height);
		derivY.reshape(source.width,source.height);

		gradient.process(source,derivX,derivY);
		flowKlt.process(source,derivX,derivY,destination,flow);
	}

	@Override
	public ImageType<I> getInputType() {
		return imageType;
	}
}
