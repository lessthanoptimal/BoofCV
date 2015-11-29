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

package boofcv.alg.feature.detect.interest;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageFloat32;

import java.util.ArrayList;
import java.util.List;

/**
 * Precomputes the gradient for all scales in the scale-space and saves them in a list.
 *
 * @author Peter Abeles
 */
public class UnrollSiftScaleSpaceGradient {

	SiftScaleSpace scaleSpace;

	List<ImageScale> usedScales = new ArrayList<ImageScale>();
	List<ImageScale> allScales = new ArrayList<ImageScale>();

	ImageGradient<ImageFloat32,ImageFloat32> gradient = FactoryDerivative.three_F32();

	public UnrollSiftScaleSpaceGradient(SiftScaleSpace scaleSpace) {
		this.scaleSpace = scaleSpace;

		// create one image for each scale to minimize memory being created/destroyed
		int numScales = scaleSpace.getNumScales()*scaleSpace.getTotalOctaves();
		for (int i = 0; i < numScales; i++) {
			allScales.add( new ImageScale());
		}
	}

	public void setImage(ImageFloat32 image) {

		scaleSpace.initialize(image);

		usedScales.clear();
//		System.out.println("total scales "+allScales.size());
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

//				System.out.printf("Sigma = %6.2f c2i = % 6.2f\n",sigma,pixelCurrentToInput);
			}
		} while( scaleSpace.computeNextOctave() );
	}


	/**
	 * Looks up the image which is closest specified sigma
	 */
	public ImageScale lookup( double sigma ) {
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

	public static class ImageScale {
		public ImageFloat32 derivX = new ImageFloat32(1,1);
		public ImageFloat32 derivY = new ImageFloat32(1,1);
		public double imageToInput;
		public double sigma;
	}
}
