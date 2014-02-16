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

package boofcv.factory.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.alg.tracker.meanshift.LikelihoodHistCoupled_U8;
import boofcv.alg.tracker.meanshift.LikelihoodHueSatHistCoupled_U8;
import boofcv.alg.tracker.meanshift.LikelihoodHueSatHistInd_U8;
import boofcv.alg.tracker.meanshift.PixelLikelihood;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.alg.tracker.tld.TldParameters;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageDataType;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * Factory for creating low level implementations of object tracking algorithms.  These algorithms allow
 * the user to specify an object in a video stream and then track it.  For a high level and user to use
 * common interface see {@kink FactoryTrackerObjectQuad}
 *
 * @author Peter Abeles
 */
public class FactoryTrackerObjectAlgs {

	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TldTracker<T,D> createTLD( TldParameters config ,
							   InterpolatePixelS<T> interpolate , ImageGradient<T,D> gradient ,
							   Class<T> imageType , Class<D> derivType ) {
		return new TldTracker<T,D>(config,interpolate,gradient,imageType,derivType);
	}

	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	SparseFlowObjectTracker<T,D> createSparseFlow( SfotConfig config ,
												   Class<T> imageType , Class<D> derivType ,
												   ImageGradient<T, D> gradient) {
		return new SparseFlowObjectTracker<T,D>(config,imageType,derivType,gradient);
	}

	public static <T extends ImageMultiBand>
	PixelLikelihood<T> likelihoodHueSatHistIndependent(
			double maxPixelValue , int numHistogramBins , ImageType<T> imageType )
	{
		if( imageType.getFamily() != ImageType.Family.MULTI_SPECTRAL )
			throw new IllegalArgumentException("Only MultiSpectral images supported currently");
		if( imageType.getNumBands() != 3 )
			throw new IllegalArgumentException("Input image type must have 3 bands.");

		if( imageType.getDataType() == ImageDataType.U8 ) {
			return (PixelLikelihood)new LikelihoodHueSatHistInd_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+imageType.getDataType());
		}
	}

	public static <T extends ImageMultiBand>
	PixelLikelihood<T> likelihoodHueSatHistCoupled(
			double maxPixelValue , int numHistogramBins , ImageType<T> imageType )
	{
		if( imageType.getFamily() != ImageType.Family.MULTI_SPECTRAL )
			throw new IllegalArgumentException("Only MultiSpectral images supported currently");
		if( imageType.getNumBands() != 3 )
			throw new IllegalArgumentException("Input image type must have 3 bands.");

		if( imageType.getDataType() == ImageDataType.U8 ) {
			return (PixelLikelihood)new LikelihoodHueSatHistCoupled_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+imageType.getDataType());
		}
	}

	public static <T extends ImageMultiBand>
	PixelLikelihood<T> likelihoodHistogramCoupled( double maxPixelValue , int numHistogramBins , ImageType<T> imageType )
	{
		if( imageType.getFamily() != ImageType.Family.MULTI_SPECTRAL )
			throw new IllegalArgumentException("Only MultiSpectral images supported currently");

		if( imageType.getDataType() == ImageDataType.U8 ) {
			return (PixelLikelihood)new LikelihoodHistCoupled_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+imageType.getDataType());
		}
	}

	public static <T extends ImageSingleBand>
	CirculantTracker<T> circulant( ConfigCirculantTracker config , Class<T> imageType) {
		if( config == null )
			config = new ConfigCirculantTracker();

		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType);

		return new CirculantTracker(
				config.output_sigma_factor,config.sigma,config.lambda,config.interp_factor,
				config.padding,
				config.workSpace,
				config.maxPixelValue,interp);
	}
}
