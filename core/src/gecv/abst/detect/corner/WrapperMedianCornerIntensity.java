/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.abst.detect.corner;

import gecv.alg.detect.corner.MedianCornerIntensity;
import gecv.alg.detect.corner.impl.MedianCorner_F32;
import gecv.alg.detect.corner.impl.MedianCorner_I8;
import gecv.alg.filter.blur.MedianFilterFactory;
import gecv.alg.filter.blur.MedianImageFilter;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;

/**
 * Wrapper around children of {@link gecv.alg.detect.corner.MedianCornerIntensity}.  This is a bit of a hack since
 * the median image is not provided as a standard input so it has to compute it internally
 * 
 * @author Peter Abeles
 */
public class WrapperMedianCornerIntensity<I extends ImageBase, D extends ImageBase> implements GeneralCornerIntensity<I,D> {

	MedianCornerIntensity<I> alg;
	MedianImageFilter<I> medianFilter;
	I medianImage;

	@SuppressWarnings({"unchecked"})
	public static <I extends ImageBase, D extends ImageBase>
	WrapperMedianCornerIntensity<I,D> create( Class<I> imageType , int imgWidth , int imgHeight , int medianRadius ) {
		if( imageType == ImageUInt8.class ) {
			ImageUInt8 medianImage = new ImageUInt8(imgWidth,imgHeight);
			MedianImageFilter<ImageUInt8> medianFilter = MedianFilterFactory.create_I8(medianRadius);
			MedianCornerIntensity<ImageUInt8> alg = new MedianCorner_I8(imgWidth,imgHeight);
			return new WrapperMedianCornerIntensity(alg,medianFilter,medianImage);
		} else if( imageType == ImageFloat32.class ) {
			ImageFloat32 medianImage = new ImageFloat32(imgWidth,imgHeight);
			MedianImageFilter<ImageFloat32> medianFilter = MedianFilterFactory.create_F32(medianRadius);
			MedianCornerIntensity<ImageFloat32> alg = new MedianCorner_F32(imgWidth,imgHeight);
			return new WrapperMedianCornerIntensity(alg,medianFilter,medianImage);
		} else {
			throw new IllegalArgumentException("Unknown image type");
		}
	}

	protected WrapperMedianCornerIntensity(MedianCornerIntensity<I> alg ,
										   MedianImageFilter<I> medianFilter ,
										   I medianImage ) {
		this.alg = alg;
		this.medianFilter = medianFilter;
		this.medianImage = medianImage;
	}

	@Override
	public void process(I input, D derivX , D derivY , D derivXX , D derivYY , D derivXY ) {
		medianFilter.process(input,medianImage);
		alg.process(input,medianImage);
	}

	@Override
	public ImageFloat32 getIntensity() {
		return alg.getIntensity();
	}

	@Override
	public QueueCorner getCandidates() {
		return null;
	}

	@Override
	public boolean getRequiresGradient() {
		return false;
	}

	@Override
	public boolean getRequiresHessian() {
		return false;
	}

	@Override
	public boolean hasCandidates() {
		return false;
	}
}
