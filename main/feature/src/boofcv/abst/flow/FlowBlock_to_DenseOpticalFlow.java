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

import boofcv.alg.flow.DenseOpticalFlowBlockPyramid;
import boofcv.alg.flow.UtilDenseOpticalFlow;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * Wrapper around {@link boofcv.alg.flow.DenseOpticalFlowBlockPyramid} for {@link boofcv.abst.flow.DenseOpticalFlow}.
 *
 * @author Peter Abeles
 */
public class FlowBlock_to_DenseOpticalFlow<T extends ImageGray>
	implements DenseOpticalFlow<T>
{
	DenseOpticalFlowBlockPyramid<T> flowAlg;

	// width and height of input image.  used to see if anything changes
	int width = -1;
	int height = -1;

	// relative change in scale between pyramid layers
	double scale;
	// maximum number of layers in the pyramid
	int maxLayers;

	ImagePyramid<T> pyramidSrc;
	ImagePyramid<T> pyramidDst;

	ImageType<T> imageType;

	public FlowBlock_to_DenseOpticalFlow(DenseOpticalFlowBlockPyramid<T> flowAlg,
										 double scale,
										 int maxLayers,
										 Class<T> imageType) {
		this.flowAlg = flowAlg;
		this.scale = scale;
		this.maxLayers = maxLayers;

		this.imageType = ImageType.single(imageType);
	}

	@Override
	public void process(T source, T destination, ImageFlow flow) {

		if( width != source.width || height != source.height ) {
			width = source.width;
			height = source.height;

			int minSize = (2*(flowAlg.getRegionRadius() + flowAlg.getRegionRadius()+1) + 1);

			// apply no blur to the layers.  If the user wants a blurred image they can blur it themselves
			pyramidSrc = UtilDenseOpticalFlow.standardPyramid(source.width,source.height,scale,0,
					minSize,maxLayers,source.getImageType().getImageClass());
			pyramidDst = UtilDenseOpticalFlow.standardPyramid(source.width,source.height,scale,0,
					minSize,maxLayers,source.getImageType().getImageClass());
		}

		pyramidSrc.process(source);
		pyramidDst.process(destination);

		flowAlg.process(pyramidSrc,pyramidDst);

		flow.setTo(flowAlg.getOpticalFlow());
	}

	@Override
	public ImageType<T> getInputType() {
		return imageType;
	}
}
