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

package boofcv.abst.filter.binary;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.filter.binary.impl.ThresholdLocalPercentile;
import boofcv.core.image.GConvertImage;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

/**
 * TODO fill in
 * @author Peter Abeles
 */
public class LocalSquarePercentileFilter<T extends ImageSingleBand> implements InputToBinary<T>
{
	ThresholdLocalPercentile alg;
	Class<T> imageType;

	boolean skipConvert;
	ImageUInt8 work = new ImageUInt8(1,1);

	double minPixelValue;
	double maxPixelValue;

	public LocalSquarePercentileFilter(boolean thresholdDown ,
									   int regionWidth ,
									   double minPixelValue, double maxPixelValue ,
									   int histogramLength , int minimumSpread,
									   double lowerFrac , double upperFrac,
									   Class<T> imageType )
	{
		this.imageType = imageType;
		this.minPixelValue = minPixelValue;
		this.maxPixelValue = maxPixelValue;
		alg = new ThresholdLocalPercentile(
				thresholdDown,regionWidth,histogramLength,minimumSpread,lowerFrac,upperFrac);

		skipConvert = imageType==ImageUInt8.class&&(minPixelValue==0&&maxPixelValue==255&&histogramLength==256);
	}

	@Override
	public void process(T input, ImageUInt8 binary) {
		InputSanityCheck.checkSameShape(input,binary);

		if( skipConvert ) {
			alg.process((ImageUInt8)input,binary);
		} else {
			work.reshape(input.width, input.height);
			GConvertImage.convert(input, minPixelValue, maxPixelValue, alg.getHistogramLength(), work);
			alg.process(work,binary);
		}
	}

	@Override
	public int getHorizontalBorder() {
		return 0;
	}

	@Override
	public int getVerticalBorder() {
		return 0;
	}

	@Override
	public ImageType<T> getInputType() {
		return ImageType.single(imageType);
	}

	@Override
	public ImageType<ImageUInt8> getOutputType() {
		return ImageType.single(ImageUInt8.class);
	}
}
