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

package boofcv.alg.feature.detect.template;

import boofcv.abst.transform.fft.DiscreteFourierTransform;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.alg.transform.fft.DiscreteFourierTransformOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.InterleavedF32;

/**
 * Correlation based template matching which uses FFT
 *
 * @author Peter Abeles
 */
public class TemplateCorrelationFFT
		implements TemplateMatchingIntensity<GrayF32>
{
	DiscreteFourierTransform<GrayF32,InterleavedF32> dft =
			DiscreteFourierTransformOps.createTransformF32();

	// border which should be ignored
	int borderX0,borderY0,borderX1,borderY1;

	// storage for intermediate states
	GrayF32 normalizedImage = new GrayF32(1,1);
	GrayF32 normalizedTemplate = new GrayF32(1,1);


	GrayF32 enlargedTemplate = new GrayF32(1,1);

	InterleavedF32 fftImage = new InterleavedF32(1,1,2);
	InterleavedF32 fftTemplate = new InterleavedF32(1,1,2);
	InterleavedF32 fftMult = new InterleavedF32(1,1,2);

	// the final correlation/intensity image
	GrayF32 correlation = new GrayF32(1,1);

	// image statistics used to normalize images
	float maxValue,mean;

	@Override
	public void setInputImage(GrayF32 image) {
		enlargedTemplate.reshape(image.width,image.height);
		fftImage.reshape(image.width,image.height);
		fftTemplate.reshape(image.width,image.height);
		fftMult.reshape(image.width,image.height);
		correlation.reshape(image.width,image.height);

		normalizedImage.reshape(image.width,image.height);

		maxValue = ImageStatistics.max(image)+0.0001f;// avoid divide by zero errors
		mean = ImageStatistics.mean(image);

		PixelMath.divide(image,maxValue,normalizedImage);
		PixelMath.minus(normalizedImage,mean/maxValue,normalizedImage);

		dft.forward(normalizedImage, fftImage);
	}

	@Override
	public void process(GrayF32 template) {
		process(template,null);
	}

	@Override
	public void process(GrayF32 template, GrayF32 mask) {
		if( template.width >= fftImage.width || template.height >= fftImage.height )
			throw new IllegalArgumentException("Template must be smaller than the image");

		// normalize the input image to reduce buffer overflow
		normalizedTemplate.reshape(template.width,template.height);

		PixelMath.divide(template,maxValue,normalizedTemplate);
		PixelMath.minus(normalizedTemplate,mean/maxValue,normalizedTemplate);

		if( mask != null ) {
			for (int y = 0; y < mask.height; y++) {
				for (int x = 0; x < mask.width; x++) {
					if( mask.unsafe_get(x,y) == 0 ) {
						normalizedTemplate.unsafe_set(x,y,0);
					}
				}
			}
		}

		// the image border is zero padded due to how the FFT is compute.  So avoid that
		borderX0 = template.width/2;
		borderX1 = template.width - borderX0;
		borderY0 = template.height/2;
		borderY1 = template.height - borderY0;

		// insert the template into the enlarged image
		// want it to be at (0,0) coordinate.  This requires wrapping it around the corners
		GImageMiscOps.fill(enlargedTemplate,0);
//		int x0 = 0;//enlargedTemplate.width-template.width;
//		int y0 = 0;//enlargedTemplate.height-template.height;
//		int x1 = x0 + template.width;
//		int y1 = y0 + template.height;

//		enlargedTemplate.subimage(x0,y0,x1,y1).setTo(normalizedTemplate);
		for (int y = 0; y < template.height; y++) {
			int yy = y-borderY0+(1-template.height%2);
			if( yy < 0 )
				yy = enlargedTemplate.height+yy;
			for (int x = 0;  x < template.width; x++) {
				int xx = x-borderX0+(1-template.width%2);
				if( xx < 0 )
					xx = enlargedTemplate.width+xx;
				enlargedTemplate.unsafe_set(xx,yy, normalizedTemplate.unsafe_get(x,y));
			}
		}

		dft.forward(enlargedTemplate, fftTemplate);

		// compute the correlation
		DiscreteFourierTransformOps.multiplyComplex(fftImage,fftTemplate,fftMult);
		dft.inverse(fftMult,correlation);
	}

	@Override
	public GrayF32 getIntensity() {
		return correlation;
	}

	@Override
	public boolean isBorderProcessed() {
		return false;
	}

	@Override
	public int getBorderX0() {
		return borderX0;
	}

	@Override
	public int getBorderX1() {
		return borderX1;
	}

	@Override
	public int getBorderY0() {
		return borderY0;
	}

	@Override
	public int getBorderY1() {
		return borderY1;
	}
}
