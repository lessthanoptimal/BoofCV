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

package boofcv.factory.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.abst.tracker.ConfigCirculantTracker;
import boofcv.abst.tracker.ConfigComaniciu2003;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.alg.tracker.meanshift.*;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.alg.tracker.tld.TldParameters;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.core.image.border.BorderType;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.*;

/**
 * Factory for creating low level implementations of object tracking algorithms.  These algorithms allow
 * the user to specify an object in a video stream and then track it.  For a high level and user to use
 * common interface see {@link FactoryTrackerObjectQuad}
 *
 * @author Peter Abeles
 */
public class FactoryTrackerObjectAlgs {

	public static <T extends ImageGray,D extends ImageGray>
	TldTracker<T,D> createTLD( TldParameters config ,
							   InterpolatePixelS<T> interpolate , ImageGradient<T,D> gradient ,
							   Class<T> imageType , Class<D> derivType ) {
		return new TldTracker<>(config, interpolate, gradient, imageType, derivType);
	}

	public static <T extends ImageGray,D extends ImageGray>
	SparseFlowObjectTracker<T,D> createSparseFlow( SfotConfig config ,
												   Class<T> imageType , Class<D> derivType ,
												   ImageGradient<T, D> gradient) {
		return new SparseFlowObjectTracker<>(config, imageType, derivType, gradient);
	}

	public static <T extends ImageMultiBand>
	PixelLikelihood<T> likelihoodHueSatHistIndependent(
			double maxPixelValue , int numHistogramBins , ImageType<T> imageType )
	{
		if( imageType.getFamily() != ImageType.Family.PLANAR)
			throw new IllegalArgumentException("Only Planar images supported currently");
		if( imageType.getNumBands() != 3 )
			throw new IllegalArgumentException("Input image type must have 3 bands.");

		if( imageType.getDataType() == ImageDataType.U8 ) {
			return (PixelLikelihood)new LikelihoodHueSatHistInd_PL_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+imageType.getDataType());
		}
	}

	public static <T extends ImageMultiBand>
	PixelLikelihood<T> likelihoodHueSatHistCoupled(
			double maxPixelValue , int numHistogramBins , ImageType<T> imageType )
	{
		if( imageType.getFamily() != ImageType.Family.PLANAR)
			throw new IllegalArgumentException("Only Planar images supported currently");
		if( imageType.getNumBands() != 3 )
			throw new IllegalArgumentException("Input image type must have 3 bands.");

		if( imageType.getDataType() == ImageDataType.U8 ) {
			return (PixelLikelihood)new LikelihoodHueSatHistCoupled_PL_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+imageType.getDataType());
		}
	}

	public static <T extends ImageBase>
	PixelLikelihood<T> likelihoodHistogramCoupled( double maxPixelValue , int numHistogramBins , ImageType<T> imageType )
	{
		switch( imageType.getFamily() ) {
			case GRAY:
				if( imageType.getDataType() != ImageDataType.U8 )
					throw new IllegalArgumentException("Only U8 currently supported");
				return (PixelLikelihood)new LikelihoodHistCoupled_SB_U8((int)maxPixelValue,numHistogramBins);

			case PLANAR:
				if( imageType.getDataType() == ImageDataType.U8 ) {
					return (PixelLikelihood)new LikelihoodHistCoupled_PL_U8((int)maxPixelValue,numHistogramBins);
				} else {
					throw new RuntimeException("Band type not yet supported "+imageType.getDataType());
				}

			default:
				throw new IllegalArgumentException("Image family not yet supported.  Try Planar");
		}
	}

	public static <T extends ImageGray>
	CirculantTracker<T> circulant( ConfigCirculantTracker config , Class<T> imageType) {
		if( config == null )
			config = new ConfigCirculantTracker();

		InterpolatePixelS<T> interp = FactoryInterpolation.bilinearPixelS(imageType, BorderType.EXTENDED);

		return new CirculantTracker(
				config.output_sigma_factor,config.sigma,config.lambda,config.interp_factor,
				config.padding,
				config.workSpace,
				config.maxPixelValue,interp);
	}

	public static <T extends ImageBase>
	TrackerMeanShiftComaniciu2003<T> meanShiftComaniciu2003(ConfigComaniciu2003 config, ImageType<T> imageType ) {

		if( config == null )
			config = new ConfigComaniciu2003();

		InterpolatePixelMB<T> interp = FactoryInterpolation.createPixelMB(0,config.maxPixelValue,
				config.interpolation, BorderType.EXTENDED,imageType);

		LocalWeightedHistogramRotRect<T> hist =
				new LocalWeightedHistogramRotRect<>(config.numSamples, config.numSigmas, config.numHistogramBins,
						imageType.getNumBands(), config.maxPixelValue, interp);

		return new TrackerMeanShiftComaniciu2003<>(
				config.updateHistogram, config.meanShiftMaxIterations, config.meanShiftMinimumChange,
				config.scaleWeight, config.minimumSizeRatio, config.scaleChange, hist);
	}
}
