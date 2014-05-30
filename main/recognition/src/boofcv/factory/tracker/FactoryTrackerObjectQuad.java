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
import boofcv.abst.tracker.*;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.alg.tracker.meanshift.LocalWeightedHistogramRotRect;
import boofcv.alg.tracker.meanshift.PixelLikelihood;
import boofcv.alg.tracker.meanshift.TrackerMeanShiftComaniciu2003;
import boofcv.alg.tracker.meanshift.TrackerMeanShiftLikelihood;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageType;

/**
 * Factory for implementations of {@link TrackerObjectQuad}, a high level interface for tracking user specified
 * objects inside video sequences.  As usual, the high level interface makes it easier to use these algorithms
 * at the expensive of algorithm specific features.
 *
 * @author Peter Abeles
 */
public class FactoryTrackerObjectQuad {

	/**
	 * Create an instance of {@link TldTracker  Tracking-Learning-Detection (TLD)} tracker for the
	 * {@link TrackerObjectQuad} interface.
	 * @param config Configuration for the tracker
	 * @param <T> Image input type
	 * @param <D> Image derivative type
	 * @return TrackerObjectQuad
	 */
	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TrackerObjectQuad<T> tld(ConfigTld config , Class<T> imageType ) {
		if( config == null )
			config = new ConfigTld();

		Class<D> derivType = GImageDerivativeOps.getDerivativeType(imageType);

		InterpolatePixelS<T> interpolate = FactoryInterpolation.bilinearPixelS(imageType);
		ImageGradient<T,D> gradient =  FactoryDerivative.sobel(imageType, derivType);

		TldTracker<T,D> tracker = new TldTracker<T,D>(config.parameters,interpolate,gradient,imageType,derivType);

		return new Tld_to_TrackerObjectQuad<T,D>(tracker,imageType);
	}

	/**
	 * Create an instance of {@link SparseFlowObjectTracker  Sparse Flow Object Tracker} for the
	 * {@link TrackerObjectQuad} interface.
	 * @param config Configuration for the tracker,  Null for default.
	 * @param <T> Image input type
	 * @param <D> Image derivative type.  Null for default.
	 * @return TrackerObjectQuad
	 */
	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TrackerObjectQuad<T> sparseFlow(SfotConfig config, Class<T> imageType , Class<D> derivType ) {

		if( derivType == null )
			derivType = GImageDerivativeOps.getDerivativeType(imageType);

		if( config == null )
			config = new SfotConfig();

		ImageGradient<T, D> gradient = FactoryDerivative.sobel(imageType,derivType);

		SparseFlowObjectTracker<T,D> tracker = new SparseFlowObjectTracker<T,D>(config,imageType,derivType,gradient);

		return new Sfot_to_TrackObjectQuad<T,D>(tracker,imageType);
	}

	/**
	 * Very basic and very fast implementation of mean-shift which uses a fixed sized rectangle for its region.
	 * Works best when the target is composed of a single color.
	 *
	 * @see TrackerMeanShiftLikelihood
	 *
	 * @param maxIterations Maximum number of mean-shift iterations.  Try 30.
	 * @param numBins Number of bins in the histogram color model.  Try 5.
	 * @param maxPixelValue Maximum number of pixel values.  For 8-bit images this will be 256
	 * @param modelType Type of color model used.
	 * @param imageType Type of image
	 * @return TrackerObjectQuad based on {@link TrackerMeanShiftLikelihood}.
	 */
	public static <T extends ImageMultiBand>
	TrackerObjectQuad<T> meanShiftLikelihood(int maxIterations,
											 int numBins,
											 double maxPixelValue,
											 MeanShiftLikelihoodType modelType,
											 ImageType<T> imageType) {
		PixelLikelihood<T> likelihood;

		switch( modelType ) {
			case HISTOGRAM:
				likelihood = FactoryTrackerObjectAlgs.likelihoodHistogramCoupled(maxPixelValue,numBins,imageType);
				break;

			case HISTOGRAM_INDEPENDENT_RGB_to_HSV:
				likelihood = FactoryTrackerObjectAlgs.likelihoodHueSatHistIndependent(maxPixelValue,numBins,imageType);
				break;

			case HISTOGRAM_RGB_to_HSV:
				likelihood = FactoryTrackerObjectAlgs.likelihoodHueSatHistCoupled(maxPixelValue,numBins,imageType);
				break;

			default:
				throw new IllegalArgumentException("Unknown likelihood model "+modelType);
		}

		TrackerMeanShiftLikelihood<T> alg =
				new TrackerMeanShiftLikelihood<T>(likelihood,maxIterations,0.1f);

		return new Msl_to_TrackerObjectQuad<T>(alg,likelihood,imageType);
	}

	/**
	 * Implementation of mean-shift which matches the histogram and can handle targets composed of multiple colors.
	 * The tracker can also be configured to estimate gradual changes in scale.  The track region is
	 * composed of a rotated rectangle.
	 *
	 * @see TrackerMeanShiftComaniciu2003
	 *
	 * @param config Tracker configuration
	 * @param <T> Image type
	 * @return TrackerObjectQuad based on Comaniciu2003
	 */
	public static <T extends ImageMultiBand>
	TrackerObjectQuad<T> meanShiftComaniciu2003(ConfigComaniciu2003 config, ImageType<T> imageType ) {

		if( config == null )
			config = new ConfigComaniciu2003();

		InterpolatePixelMB<T> interp = FactoryInterpolation.createPixelMB(0,config.maxPixelValue,
				config.interpolation,imageType);

		LocalWeightedHistogramRotRect<T> hist =
				new LocalWeightedHistogramRotRect<T>(config.numSamples,config.numSigmas,config.numHistogramBins,
						imageType.getNumBands(),config.maxPixelValue,interp);
		TrackerMeanShiftComaniciu2003<T> alg = new TrackerMeanShiftComaniciu2003<T>(
				config.updateHistogram,config.meanShiftMaxIterations,config.meanShiftMinimumChange,
				config.scaleWeight,config.minimumSizeRatio,config.scaleChange,hist);

		return new Comaniciu2003_to_TrackerObjectQuad<T>(alg,imageType);
	}

	/**
	 * Creates the Circulant feature tracker.  Texture based tracker which uses the theory of circulant matrices,
	 * Discrete Fourier Transform (DCF), and linear classifiers to track a target.  Fixed sized rectangular target
	 * and only estimates translation.  Can't detect when it loses track or re-aquire track.
	 *
	 * @see {@link CirculantTracker}
	 *
	 * @param config Configuration
	 * @return CirculantTracker
	 */
	public static <T extends ImageSingleBand>
	TrackerObjectQuad<T> circulant( ConfigCirculantTracker config , Class<T> imageType ) {

		CirculantTracker<T> alg = FactoryTrackerObjectAlgs.circulant(config,imageType);

		return new Circulant_to_TrackerObjectQuad<T>(alg,ImageType.single(imageType));
	}
}
