/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.flow.HornSchunckPyramid;
import boofcv.alg.flow.UtilDenseOpticalFlow;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.PyramidFloat;

/**
 * Implementation of {@link boofcv.abst.flow.DenseOpticalFlow} for {@link boofcv.alg.flow.HornSchunck}.
 *
 * @author Peter Abeles
 */
// TODO normalize histogram
public class HornSchunckPyramid_to_DenseOpticalFlow<T extends ImageSingleBand,D extends ImageSingleBand>
	implements DenseOpticalFlow<T>
{
	HornSchunckPyramid<T,D> hornSchunck;

	// parameters used to create pyramid
	double scale;
	double sigma;

	// image pyramid and its derivative
	PyramidFloat<T> pyr1;
	PyramidFloat<T> pyr2;

	D derivX[];
	D derivY[];

	// computes the gradient
	ImageGradient<T, D> gradient;

	// image type information
	ImageType<T> imageType;
	ImageType<D> derivType;

	/**
	 * TODO fill out
	 * @param hornSchunck
	 * @param scale Try 0.7
	 * @param sigma Try 1
	 * @param imageType
	 * @param derivType
	 */
	public HornSchunckPyramid_to_DenseOpticalFlow(HornSchunckPyramid<T, D> hornSchunck,
												  double scale, double sigma,
												  ImageType<T> imageType,
												  ImageType<D> derivType) {
		this.hornSchunck = hornSchunck;
		this.scale = scale;
		this.sigma = sigma;
		this.imageType = imageType;
		this.derivType = derivType;

		gradient = FactoryDerivative.two(imageType.getImageClass(), derivType.getImageClass());
	}

	@Override
	public void process(T source, T destination, ImageFlow flow) {

		if( pyr1 == null || pyr1.getInputWidth() != source.width || pyr1.getInputHeight() != source.height ) {
			pyr1 = UtilDenseOpticalFlow.standardPyramid(source.width, source.height, scale, sigma, 5, 12, imageType.getImageClass());
			pyr2 = UtilDenseOpticalFlow.standardPyramid(source.width, source.height, scale, sigma, 5, 12, imageType.getImageClass());

			pyr1.initialize(source.width,source.height);
			pyr2.initialize(source.width,source.height);

			derivX = (D[])PyramidOps.declareOutput(pyr2,derivType.getImageClass());
			derivY = (D[])PyramidOps.declareOutput(pyr2,derivType.getImageClass());
		}

		pyr1.process(source);
		pyr2.process(destination);

		PyramidOps.gradient(pyr2,gradient,derivX,derivY);

		hornSchunck.process(pyr1,pyr2, derivX,derivY);

		ImageFloat32 flowX = hornSchunck.getFlowX();
		ImageFloat32 flowY = hornSchunck.getFlowY();

		int index = 0;
		for( int y = 0; y < flow.height; y++){
			for( int x = 0; x < flow.width; x++, index++ ){
				ImageFlow.D d = flow.unsafe_get(x,y);
				d.x = flowX.data[index];
				d.y = flowY.data[index];
			}
		}
	}

	@Override
	public ImageType<T> getInputType() {
		return imageType;
	}
}
