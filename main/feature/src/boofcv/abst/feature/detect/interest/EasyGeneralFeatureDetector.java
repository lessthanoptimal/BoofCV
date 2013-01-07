/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.detect.interest;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageSingleBand;

/**
 * Detects features using {@link GeneralFeatureDetector} but Handles all the derivative computations automatically.
 *
 * @author Peter Abeles
 */
// TODO move to alg? with general feature detector
public class EasyGeneralFeatureDetector<T extends ImageSingleBand, D extends ImageSingleBand> {

	// Feature detector
	GeneralFeatureDetector<T, D> detector;
	// Computes iamge gradient
	ImageGradient<T, D> gradient;
	// computes hessian
	ImageHessian<D> hessian;

	// storage for image derivatives
	D derivX; // first derivative x-axis
	D derivY; // first derivative y-axis
	D derivXX; // second derivative x-x
	D derivYY; // second derivative y-y
	D derivXY; // second derivative x-y

	/**
	 * Uses default image derivatives.
	 *
	 * @param detector Feature detector.
	 * @param imageType Type of input image
	 * @param derivType If null then the derivative will be selected using the image type.
	 */
	public EasyGeneralFeatureDetector(GeneralFeatureDetector<T, D> detector ,
									  Class<T> imageType, Class<D> derivType ) {
		this.detector = detector;

		if( derivType == null ) {
			derivType = GImageDerivativeOps.getDerivativeType(imageType);
		}

		if( detector.getRequiresGradient() ) {
			gradient = FactoryDerivative.sobel(imageType, derivType);
		}
		if( detector.getRequiresHessian() ) {
			hessian = FactoryDerivative.hessianSobel(derivType);
		}
		declareDerivativeImages(gradient, hessian, derivType);
	}

	public EasyGeneralFeatureDetector(GeneralFeatureDetector<T, D> detector,
									  ImageGradient<T, D> gradient,
									  ImageHessian<D> hessian,
									  Class<D> derivType ) {
		this.detector = detector;
		this.gradient = gradient;
		this.hessian = hessian;

		declareDerivativeImages(gradient, hessian, derivType);
	}

	private void declareDerivativeImages(ImageGradient<T, D> gradient, ImageHessian<D> hessian, Class<D> derivType) {
		if( gradient != null ) {
			derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
			derivY = GeneralizedImageOps.createSingleBand(derivType,1,1);
		}
		if( hessian != null ) {
			derivXX = GeneralizedImageOps.createSingleBand(derivType,1,1);
			derivYY = GeneralizedImageOps.createSingleBand(derivType,1,1);
			derivXY = GeneralizedImageOps.createSingleBand(derivType,1,1);
		}
	}


	public void detect(T input, QueueCorner excludedCorners ) {

		initializeDerivatives(input);

		if (detector.getRequiresGradient() || detector.getRequiresHessian())
			gradient.process(input, derivX, derivY);
		if (detector.getRequiresHessian())
			hessian.process(derivX, derivY, derivXX, derivYY, derivXY);

		detector.setExcludedCorners(excludedCorners);
		detector.process(input, derivX, derivY, derivXX, derivYY, derivXY);
	}

	private void initializeDerivatives(T input) {
		// reshape derivatives if the input image has changed size
		derivX.reshape(input.width, input.height);
		derivY.reshape(input.width, input.height);
		if (detector.getRequiresHessian()) {
			derivXX.reshape(input.width, input.height);
			derivYY.reshape(input.width, input.height);
			derivXY.reshape(input.width, input.height);
		}
	}

	public QueueCorner getFeatures() {
		return detector.getFeatures();
	}
}
