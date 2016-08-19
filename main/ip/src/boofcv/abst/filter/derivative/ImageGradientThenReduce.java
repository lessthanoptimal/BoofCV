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

package boofcv.abst.filter.derivative;

import boofcv.core.image.border.BorderType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageType;

/**
 * First computes a multi-band image gradient then reduces the number of bands in the gradient
 * to one.
 *
 * @author Peter Abeles
 */
public class ImageGradientThenReduce<Input extends ImageMultiBand,
		Middle extends ImageMultiBand,
		Output extends ImageGray>
		implements ImageGradient<Input,Output>
{
	// used to compute multi-band image gradient
	ImageGradient<Input,Middle> gradient;
	// converts multi-band image gradient into single band gradient
	GradientMultiToSingleBand<Middle,Output> reduce;

	// storage for intermediate results
	Middle middleX,middleY;

	public ImageGradientThenReduce(ImageGradient<Input, Middle> gradient,
								   GradientMultiToSingleBand<Middle, Output> reduce) {
		this.gradient = gradient;
		this.reduce = reduce;

		middleX = gradient.getDerivativeType().createImage(1,1);
		middleY = gradient.getDerivativeType().createImage(1,1);
	}

	@Override
	public void setBorderType(BorderType type) {
		gradient.setBorderType(type);
	}

	@Override
	public BorderType getBorderType() {
		return gradient.getBorderType();
	}

	@Override
	public int getBorder() {
		return gradient.getBorder();
	}

	@Override
	public ImageType<Output> getDerivativeType() {
		return ImageType.single(reduce.getOutputType());
	}

	@Override
	public void process(Input inputImage, Output derivX, Output derivY) {
		middleX.reshape(inputImage.width,inputImage.height);
		middleY.reshape(inputImage.width,inputImage.height);

		gradient.process(inputImage, middleX, middleY);
		reduce.process(middleX,middleY, derivX,derivY);
	}
}
