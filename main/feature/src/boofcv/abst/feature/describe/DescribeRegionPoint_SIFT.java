/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.describe;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.describe.DescribePointSiftLowe;
import boofcv.alg.feature.detect.interest.SiftScaleSpace2;
import boofcv.core.image.GConvertImage;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.BoofDefaults;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

import java.util.ArrayList;
import java.util.List;

/**
 * Allows you to use SIFT features independent of the SIFT detector.  A SIFT scale-space is computed with all octaves
 * and most of the scales saved.  When a few feature is requested it looks up the closest scale image and uses
 * that as the input image.
 *
 * @author Peter Abeles
 */
public class DescribeRegionPoint_SIFT <T extends ImageSingleBand>
	implements DescribeRegionPoint<T,TupleDesc_F64>
{

	ImageType<T> imageType;

	SiftScaleSpace2 scaleSpace;
	DescribePointSiftLowe<ImageFloat32> describe;

	ImageFloat32 imageFloat = new ImageFloat32(1,1);

	List<ImageScale> usedScales = new ArrayList<ImageScale>();
	List<ImageScale> allScales = new ArrayList<ImageScale>();

	ImageGradient<ImageFloat32,ImageFloat32> gradient =
			FactoryDerivative.three_F32();

	public DescribeRegionPoint_SIFT(SiftScaleSpace2 scaleSpace,
									DescribePointSiftLowe<ImageFloat32> describe,
									Class<T> imageType ) {
		this.scaleSpace = scaleSpace;
		this.describe = describe;

		// create one image for each scale to minimize memory being created/destroyed
		int numScales = scaleSpace.getNumScales()*scaleSpace.getTotalOctaves();
		for (int i = 0; i < numScales; i++) {
			allScales.add( new ImageScale());
		}

		this.imageType = ImageType.single(imageType);
	}

	@Override
	public void setImage(T image) {
		ImageFloat32 input;
		if( image instanceof ImageFloat32 ) {
			input = (ImageFloat32)image;
		} else {
			imageFloat.reshape(image.width,image.height);
			GConvertImage.convert(image,imageFloat);
			input = imageFloat;
		}

		// "unroll" the scale space so that features can be looked up quickly
		scaleSpace.initialize(input);

		usedScales.clear();
		System.out.println("total scales "+allScales.size());
		do {
			for (int i = 0; i < scaleSpace.getNumScales(); i++) {
				ImageFloat32 scaleImage = scaleSpace.getImageScale(i);
				double sigma = scaleSpace.computeSigmaScale(i);
				double pixelCurrentToInput = scaleSpace.pixelScaleCurrentToInput();

				ImageScale scale = allScales.get(usedScales.size());
				scale.derivX.reshape(scaleImage.width,scaleImage.height);
				scale.derivY.reshape(scaleImage.width,scaleImage.height);

				gradient.process(scaleImage,scale.derivX,scale.derivY);
				scale.imageToInput = pixelCurrentToInput;
				scale.sigma = sigma;

				usedScales.add(scale);

				System.out.printf("Sigma = %6.2f c2i = % 6.2f\n",sigma,pixelCurrentToInput);
			}
		} while( scaleSpace.computeNextOctave() );
	}

	@Override
	public boolean process(double x, double y, double orientation, double radius, TupleDesc_F64 description) {

		// get the blur sigma for the radius
		double sigma = radius/ BoofDefaults.SIFT2_SCALE_TO_RADIUS;

		// find the image which the blur factor closest to this sigma
		ImageScale image = lookup(sigma);

		// compute the descriptor
		describe.setImageGradient(image.derivX,image.derivY);
		describe.process(x/image.imageToInput,y/image.imageToInput,sigma/image.imageToInput,
				orientation,description);

		return true;
	}

	/**
	 * Looks up the image which is closest specified sigma
	 */
	private ImageScale lookup( double sigma ) {
		ImageScale best = null;
		double bestValue = Double.MAX_VALUE;

		for (int i = 0; i < usedScales.size(); i++) {
			ImageScale image = usedScales.get(i);
			double difference = Math.abs(sigma-image.sigma);
			if( difference < bestValue ) {
				bestValue = difference;
				best = image;
			}
		}
		return best;
	}

	@Override
	public boolean requiresRadius() {
		return true;
	}

	@Override
	public boolean requiresOrientation() {
		return true;
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public double getCanonicalWidth() {
		return describe.getCanonicalRadius()*2;
	}

	@Override
	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(describe.getDescriptorLength());
	}

	@Override
	public Class<TupleDesc_F64> getDescriptionType() {
		return TupleDesc_F64.class;
	}

	private static class ImageScale {
		ImageFloat32 derivX = new ImageFloat32(1,1);
		ImageFloat32 derivY = new ImageFloat32(1,1);
		double imageToInput;
		double sigma;

	}
}
