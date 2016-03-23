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
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

/**
 * Wrapper for applying image gradients to {@link Planar} images.
 *
 * @author Peter Abeles
 */
public class ImageGradient_PL<T extends ImageGray, D extends ImageGray>
		implements ImageGradient<Planar<T>,Planar<D>>
{
	ImageGradient<T,D> bandGradient;

	ImageType<Planar<D>> derivType;

	public ImageGradient_PL(ImageGradient<T, D> bandGradient , int numBands ) {
		this.bandGradient = bandGradient;

		derivType = ImageType.pl(numBands,bandGradient.getDerivativeType().getImageClass());
	}

	@Override
	public void process(Planar<T> inputImage, Planar<D> derivX, Planar<D> derivY) {
		for (int i = 0; i < inputImage.getNumBands(); i++) {
			bandGradient.process(inputImage.getBand(i), derivX.getBand(i), derivY.getBand(i));
		}
	}

	@Override
	public void setBorderType(BorderType type) {
		bandGradient.setBorderType(type);
	}

	@Override
	public BorderType getBorderType() {
		return bandGradient.getBorderType();
	}

	@Override
	public int getBorder() {
		return bandGradient.getBorder();
	}

	@Override
	public ImageType<Planar<D>> getDerivativeType() {
		return derivType;
	}
}
