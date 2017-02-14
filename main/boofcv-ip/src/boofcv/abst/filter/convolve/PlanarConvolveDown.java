/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.convolve;

import boofcv.core.image.border.BorderType;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

/**
 * Implementation of {@link ConvolveDown} for {@link Planar} images.  Uses an implementation for {@link ImageGray}
 * to apply the down convolution.
 *
 * @author Peter Abeles
 */
public class PlanarConvolveDown<In extends ImageGray<In>, Out extends ImageGray<Out>> 
		implements ConvolveDown<Planar<In>,Planar<Out>>
{
	private ConvolveDown<In,Out> down;
	private ImageType<Planar<In>> inputType;
	private ImageType<Planar<Out>> outputType;

	public PlanarConvolveDown(ConvolveDown<In, Out> down, int numBands ) {
		this.down = down;

		inputType = ImageType.pl(numBands, down.getInputType().getDataType());
		outputType = ImageType.pl(numBands, down.getOutputType().getDataType());
	}

	@Override
	public void process(Planar<In> in, Planar<Out> out) {
		for (int i = 0; i < in.getNumBands(); i++) {
			down.process(in.getBand(i), out.getBand(i));
		}
	}

	@Override
	public int getHorizontalBorder() {
		return down.getHorizontalBorder();
	}

	@Override
	public int getVerticalBorder() {
		return down.getVerticalBorder();
	}

	@Override
	public ImageType<Planar<In>> getInputType() {
		return inputType;
	}

	@Override
	public ImageType<Planar<Out>> getOutputType() {
		return outputType;
	}

	@Override
	public BorderType getBorderType() {
		return down.getBorderType();
	}

	@Override
	public int getSkip() {
		return down.getSkip();
	}

	@Override
	public void setSkip(int skip) {
		down.setSkip(skip);
	}
}
