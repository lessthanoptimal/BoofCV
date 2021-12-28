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

package boofcv.abst.feature.detect.line;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.line.LineParametric2D_F32;

import java.util.List;

/**
 * Wrapper around {@link DetectEdgeLines} that allows it to be used by {@link DetectLine} interface
 *
 * @author Peter Abeles
 */
public class DetectEdgeLinesToLines<T extends ImageGray<T>, D extends ImageGray<D>>
		implements DetectLine<T> {
	// line detector
	DetectEdgeLines<D> detector;

	// computes image gradient
	ImageGradient<T, D> gradient;

	// storage for image gradient
	D derivX, derivY;

	public DetectEdgeLinesToLines( DetectEdgeLines<D> detector,
								   ImageGradient<T, D> gradient ) {
		this.detector = detector;
		this.gradient = gradient;

		derivX = gradient.getDerivativeType().createImage(1, 1);
		derivY = gradient.getDerivativeType().createImage(1, 1);
	}

	/**
	 * Constructor with default gradient technique
	 */
	public DetectEdgeLinesToLines( DetectEdgeLines<D> detector,
								   Class<T> imageType, Class<D> derivType ) {
		this(detector, FactoryDerivative.sobel(imageType, derivType));
	}

	@Override
	public List<LineParametric2D_F32> detect( T input ) {
		derivX.reshape(input.width, input.height);
		derivY.reshape(input.width, input.height);

		gradient.process(input, derivX, derivY);

		detector.detect(derivX, derivY);

		return detector.getFoundLines();
	}

	@Override
	public ImageType<T> getInputType() {
		return gradient.getInputType();
	}
}
