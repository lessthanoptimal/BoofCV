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

package boofcv.factory.flow;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.flow.DenseOpticalFlow;
import boofcv.abst.flow.FlowBlock_to_DenseOpticalFlow;
import boofcv.abst.flow.FlowKlt_to_DenseOpticalFlow;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.flow.DenseOpticalFlowBlock;
import boofcv.alg.flow.DenseOpticalFlowKlt;
import boofcv.alg.tracker.klt.KltConfig;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

/**
 * Creates implementations of {@link DenseOpticalFlow}.
 *
 * @author Peter Abeles
 */
public class FactoryDenseOpticalFlow {

	/**
	 *
	 * @see DenseOpticalFlowKlt
	 *
	 * @param configKlt Configuration for KLT.  If null then default values are used.
	 * @param radius Radius of square region.
	 * @param inputType Type of input image.
	 * @param derivType Type of derivative image.  If null then default is used.
	 * @param <I> Input image type.
	 * @param <D> Derivative image type.
	 * @return DenseOpticalFlow
	 */
	public static <I extends ImageSingleBand, D extends ImageSingleBand>
	DenseOpticalFlow<I> flowKlt( KltConfig configKlt, int radius , Class<I> inputType , Class<D> derivType ) {

		if( derivType == null ) {
			derivType = GImageDerivativeOps.getDerivativeType(inputType);
		}

		KltTracker<I, D> tracker = FactoryTrackerAlg.klt(configKlt,inputType,derivType);
		DenseOpticalFlowKlt<I, D> flowKlt = new DenseOpticalFlowKlt<I, D>(tracker,radius);
		ImageGradient<I, D> gradient = FactoryDerivative.sobel(inputType,derivType);

		return new FlowKlt_to_DenseOpticalFlow<I,D>(flowKlt,gradient,inputType,derivType);
	}

	/**
	 * @see boofcv.alg.flow.DenseOpticalFlowBlock
	 *
	 * @param searchRadius
	 * @param regionRadius
	 * @param maxPerPixelError
	 * @param imageType
	 * @param <T>
	 * @return
	 */
	public static <T extends ImageSingleBand>
	DenseOpticalFlow<T> region( int searchRadius, int regionRadius , int maxPerPixelError ,
								Class<T> imageType ) {

		DenseOpticalFlowBlock<T> alg;
		if( imageType == ImageUInt8.class )
			alg = (DenseOpticalFlowBlock)new DenseOpticalFlowBlock.U8(searchRadius,regionRadius,maxPerPixelError);
		else if( imageType == ImageFloat32.class )
			alg = (DenseOpticalFlowBlock)new DenseOpticalFlowBlock.F32(searchRadius,regionRadius,maxPerPixelError);
		else
			throw new IllegalArgumentException("Unsupported image type "+imageType);

		return new FlowBlock_to_DenseOpticalFlow<T>(alg,imageType);
	}
}
