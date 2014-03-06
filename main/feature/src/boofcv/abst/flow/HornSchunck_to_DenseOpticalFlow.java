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
import boofcv.alg.flow.DenseOpticalFlowHornSchunck;
import boofcv.alg.flow.UtilDenseOpticalFlow;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Implementation of {@link DenseOpticalFlow} for {@link DenseOpticalFlowHornSchunck}.  The gradient and
 * image difference operators are different from the original work, but should be an improvement
 * since they are symmetric operators.
 *
 * @author Peter Abeles
 */
public class HornSchunck_to_DenseOpticalFlow<T extends ImageBase,D extends ImageBase>
	implements DenseOpticalFlow<T>
{

	ImageGradient<T,D> gradient;
	DenseOpticalFlowHornSchunck<D> hornSchunck;

	// storage for difference image and the gradient
	D difference;
	D derivX;
	D derivY;

	ImageType<T> imageType;

	public HornSchunck_to_DenseOpticalFlow( DenseOpticalFlowHornSchunck<D> hornSchunck,
											ImageGradient<T, D> gradient ,
											ImageType<T> imageType ) {
		this.gradient = gradient;
		this.hornSchunck = hornSchunck;
		this.imageType = imageType;

		difference = gradient.getDerivType().createImage(1,1);
		derivX = gradient.getDerivType().createImage(1,1);
		derivY = gradient.getDerivType().createImage(1,1);
	}

	@Override
	public void process(T source, T destination, ImageFlow flow) {

		derivX.reshape(source.width,source.height);
		derivY.reshape(source.width,source.height);
		difference.reshape(source.width,source.height);

		gradient.process(source,derivX,derivY);

		UtilDenseOpticalFlow.difference4(source,destination,difference);

		hornSchunck.process(derivX, derivY, difference, flow);
	}

	@Override
	public ImageType<T> getInputType() {
		return imageType;
	}
}
