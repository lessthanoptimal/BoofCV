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

package boofcv.abst.feature.dense;

import boofcv.abst.distort.FDistort;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.dense.DescribeDenseSiftAlg;
import boofcv.alg.filter.misc.AverageDownSampleOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;

import java.util.List;

/**
 * High level wrapper around {@link DescribeDenseSiftAlg} for {@link DescribeImageDense}
 *
 * @author Peter Abeles
 */
public class DescribeImageDenseSift<T extends ImageSingleBand, D extends ImageSingleBand>
		implements DescribeImageDense<T,TupleDesc_F64>
{

	DescribeDenseSiftAlg<D> alg;

	ImageGradient<T,D> gradient;

	// used to upscale the image
	FDistort scaleUp;

	// relative size of descriptor region
	double scale;
	// period in pixels at scale of 1
	double periodX;
	double periodY;

	// storage for the rescaled input image and its derivatives
	T imageScaled;
	D derivX;
	D derivY;

	public DescribeImageDenseSift(DescribeDenseSiftAlg<D> alg, Class<T> inputType ) {
		this.alg = alg;
		Class<D> gradientType = alg.getDerivType();
		gradient = FactoryDerivative.three(inputType,gradientType);

		imageScaled = GeneralizedImageOps.createSingleBand(inputType,1,1);
		derivX = GeneralizedImageOps.createSingleBand(gradientType,1,1);
		derivY = GeneralizedImageOps.createSingleBand(gradientType,1,1);

		scaleUp = new FDistort(ImageType.single(inputType));
		scaleUp.interp(TypeInterpolate.BILINEAR);
	}

	@Override
	public void configure( double scale , double periodX, double periodY) {
		this.periodX = periodX;
		this.periodY = periodY;
		this.scale = scale;
	}

	@Override
	public void process(T input) {
		if( scale == 1.0 ) {
			alg.setPeriodColumns(periodX);
			alg.setPeriodRows(periodY);
		} else {
			// scale the image up or down to effectively change the descriptor size
			// also adjust the sample period
			int width = (int)(input.width/scale+0.5);
			int height = (int)(input.width/scale+0.5);

			imageScaled.reshape(width,height);
			derivX.reshape(width,height);
			derivY.reshape(width,height);

			if( width < input.width ) {
				// average down sample is slower but doesn't introduce nearly as many artifacts as other
				// methods
				AverageDownSampleOps.down(input,imageScaled);
			} else {
				scaleUp.setRefs(input,imageScaled).scaleExt().apply();
			}
			input = imageScaled;

			alg.setPeriodColumns(periodX/scale);
			alg.setPeriodRows(periodY/scale);
		}

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
		return imageScaled.getImageType();
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
