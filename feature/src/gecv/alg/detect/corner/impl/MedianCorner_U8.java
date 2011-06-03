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

package gecv.alg.detect.corner.impl;

import gecv.alg.InputSanityCheck;
import gecv.alg.detect.corner.MedianCornerIntensity;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageUInt8;

/**
 * Implementation of {@link MedianCornerIntensity} for {@link gecv.struct.image.ImageUInt8} images.
 *
 * @author Peter Abeles
 */
public class MedianCorner_U8 implements MedianCornerIntensity<ImageUInt8> {

	// the intensity of the found features in the image
	private ImageFloat32 featureIntensity;

	public MedianCorner_U8( int imgWidth , int imgHeight ) {
		featureIntensity = new ImageFloat32(imgWidth,imgHeight);
	}

	@Override
	public void process(ImageUInt8 originalImage, ImageUInt8 medianImage) {
		InputSanityCheck.checkSameShape(featureIntensity,originalImage, medianImage);

		final int width = originalImage.width;
		final int height = originalImage.height;

		for( int y = 0; y < height; y++ ) {

			int indexOrig = originalImage.startIndex + originalImage.stride*y;
			int indexMed = medianImage.startIndex + medianImage.stride*y;
			int indexInten = featureIntensity.startIndex + featureIntensity.stride*y;

			for( int x = 0; x < width; x++ ) {
				int val = (originalImage.data[indexOrig++] & 0xFF) - (medianImage.data[indexMed++] & 0xFF);

				featureIntensity.data[indexInten++] = val < 0 ? -val : val;
			}
		}
	}

	@Override
	public int getRadius() {
		return 0;
	}

	@Override
	public ImageFloat32 getIntensity() {
		return featureIntensity;
	}
}
