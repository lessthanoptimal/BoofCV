/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.filter.derivative.ImageHessian;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Detects features using {@link GeneralFeatureDetector} but Handles all the derivative computations automatically.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class EasyGeneralFeatureDetector<T extends ImageGray<T>, D extends ImageGray<D>> {

	// Feature detector
	protected GeneralFeatureDetector<T, D> detector;
	// Computes image gradient
	protected @Nullable ImageGradient<T, D> gradient;
	// computes hessian
	protected @Nullable ImageHessian<D> hessian;

	// storage for image derivatives
	protected D derivX; // first derivative x-axis
	protected D derivY; // first derivative y-axis
	protected D derivXX; // second derivative x-x
	protected D derivYY; // second derivative y-y
	protected D derivXY; // second derivative x-y

	/**
	 * Configures detector and uses default image derivatives.
	 *
	 * @param detector Feature detector.
	 * @param imageType Type of input image
	 * @param derivType If null then the derivative will be selected using the image type.
	 */
	public EasyGeneralFeatureDetector( GeneralFeatureDetector<T, D> detector,
									   @Nullable Class<T> imageType, @Nullable Class<D> derivType ) {
		this.detector = detector;

		if (imageType == null) {
			imageType = detector.getImageType();
		}

		if (derivType == null) {
			derivType = detector.getDerivType();
		}

		if (derivType == null) {
			Class<T> im = Objects.requireNonNull(imageType, "Must specify image type since detector doesn't");
			derivType = GImageDerivativeOps.getDerivativeType(im);
		}

		if (detector.getRequiresGradient() || detector.getRequiresHessian()) {
			Class<T> im = Objects.requireNonNull(imageType, "Must specify image type since detector doesn't");
			gradient = FactoryDerivative.sobel(im, derivType);
		}
		if (detector.getRequiresHessian()) {
			hessian = FactoryDerivative.hessianSobel(derivType);
		}
		declareDerivativeImages(gradient, hessian, derivType);
	}

	/**
	 * Constructor which allows the user to specify how derivatives are computed
	 */
	public EasyGeneralFeatureDetector( GeneralFeatureDetector<T, D> detector,
									   @Nullable ImageGradient<T, D> gradient,
									   @Nullable ImageHessian<D> hessian,
									   Class<D> derivType ) {
		this.detector = detector;
		this.gradient = gradient;
		this.hessian = hessian;

		declareDerivativeImages(gradient, hessian, derivType);
	}

	/**
	 * Declare storage for image derivatives as needed
	 */
	private void declareDerivativeImages( @Nullable ImageGradient<T, D> gradient,
										  @Nullable ImageHessian<D> hessian, Class<D> derivType ) {
		if (gradient != null || hessian != null) {
			derivX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
			derivY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		}
		if (hessian != null) {
			derivXX = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
			derivYY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
			derivXY = GeneralizedImageOps.createSingleBand(derivType, 1, 1);
		}
	}

	/**
	 * Detect features inside the image. Excluding points in the exclude list.
	 *
	 * @param input Image being processed.
	 * @param exclude List of points that should not be returned. Null if there are no excluded points.
	 */
	public void detect( T input, @Nullable QueueCorner exclude ) {

		initializeDerivatives(input);

		if (detector.getRequiresGradient() || detector.getRequiresHessian())
			Objects.requireNonNull(gradient).process(input, derivX, derivY);
		if (detector.getRequiresHessian())
			Objects.requireNonNull(hessian).process(derivX, derivY, derivXX, derivYY, derivXY);

		detector.setExclude(exclude);
		detector.process(input, derivX, derivY, derivXX, derivYY, derivXY);
	}

	/**
	 * Reshape derivative images to match the input image
	 */
	private void initializeDerivatives( T input ) {
		// reshape derivatives if the input image has changed size
		if (detector.getRequiresGradient() || detector.getRequiresHessian()) {
			derivX.reshape(input.width, input.height);
			derivY.reshape(input.width, input.height);
		}
		if (detector.getRequiresHessian()) {
			derivXX.reshape(input.width, input.height);
			derivYY.reshape(input.width, input.height);
			derivXY.reshape(input.width, input.height);
		}
	}

	public GeneralFeatureDetector<T, D> getDetector() {
		return detector;
	}

	public QueueCorner getMaximums() {
		return detector.getMaximums();
	}

	public QueueCorner getMinimums() {
		return detector.getMinimums();
	}

	public ImageType<T> getInputType() {
		if (detector.getRequiresGradient() || detector.getRequiresHessian())
			return Objects.requireNonNull(gradient).getInputType();
		return ImageType.single(Objects.requireNonNull(detector.getImageType()));
	}
}
