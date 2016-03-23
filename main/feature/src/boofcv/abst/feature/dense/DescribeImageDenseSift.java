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

package boofcv.abst.feature.dense;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.dense.DescribeDenseSiftAlg;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * High level wrapper around {@link DescribeDenseSiftAlg} for {@link DescribeImageDense}
 *
 * @author Peter Abeles
 */
public class DescribeImageDenseSift<T extends ImageGray, D extends ImageGray>
		implements DescribeImageDense<T,TupleDesc_F64>
{
	// dense SIFT implementation
	DescribeDenseSiftAlg<D> alg;

	// computes the image gradient
	ImageGradient<T,D> gradient;

	// type of input image
	ImageType<T> inputType;

	// period in pixels at scale of 1
	double periodX;
	double periodY;

	// storage for the rescaled input image and its derivatives
	D derivX;
	D derivY;

	/**
	 *
	 * @param alg Reference to the algorithm that is wrapped
	 * @param periodX How often the image is samples in pixels. X-axis
	 * @param periodY How often the image is samples in pixels. Y-axis
	 * @param inputType Type of input image
	 */
	public DescribeImageDenseSift(DescribeDenseSiftAlg<D> alg,
								  double periodX , double periodY ,
								  Class<T> inputType ) {
		this.alg = alg;
		this.periodX = periodX;
		this.periodY = periodY;
		this.inputType = ImageType.single(inputType);

		Class<D> gradientType = alg.getDerivType();
		gradient = FactoryDerivative.three(inputType,gradientType);

		derivX = GeneralizedImageOps.createSingleBand(gradientType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(gradientType,1,1);
	}

	@Override
	public void process(T input) {
		alg.setPeriodColumns(periodX);
		alg.setPeriodRows(periodY);

		derivX.reshape(input.width,input.height);
		derivY.reshape(input.width,input.height);
		gradient.process(input,derivX,derivY);

		alg.setImageGradient(derivX,derivY);
		alg.process();
	}

	@Override
	public List<TupleDesc_F64> getDescriptions() {
		return alg.getDescriptors().toList();
	}

	@Override
	public List<Point2D_I32> getLocations() {
		return alg.getLocations().toList();
	}

	@Override
	public ImageType<T> getImageType() {
		return inputType;
	}

	@Override
	public TupleDesc_F64 createDescription() {
		return new TupleDesc_F64(alg.getDescriptorLength());
	}

	@Override
	public Class<TupleDesc_F64> getDescriptionType() {
		return TupleDesc_F64.class;
	}

	public DescribeDenseSiftAlg<D> getAlg() {
		return alg;
	}
}
