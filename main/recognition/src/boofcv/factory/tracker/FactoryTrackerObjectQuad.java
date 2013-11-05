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

package boofcv.factory.tracker;

import boofcv.abst.tracker.*;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.tracker.meanshift.LocalWeightedHistogramRotRect;
import boofcv.alg.tracker.meanshift.PixelLikelihood;
import boofcv.alg.tracker.meanshift.TrackerMeanShiftComaniciu2003;
import boofcv.alg.tracker.meanshift.TrackerMeanShiftLikelihood;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageMultiBand;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.MultiSpectral;

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
	TrackerObjectQuad<T> createTLD( TldConfig<T,D> config ) {
		TldTracker<T,D> tracker = new TldTracker<T,D>(config);

		return new Tld_to_TrackerObjectQuad<T,D>(tracker);
	}

	/**
	 * Create an instance of {@link SparseFlowObjectTracker  Sparse Flow Object Tracker} for the
	 * {@link TrackerObjectQuad} interface.
	 * @param config Configuration for the tracker
	 * @param <T> Image input type
	 * @param <D> Image derivative type
	 * @return TrackerObjectQuad
	 */
	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TrackerObjectQuad<T> createSparseFlow( SfotConfig<T,D> config ) {
		SparseFlowObjectTracker<T,D> tracker = new SparseFlowObjectTracker<T,D>(config);

		return new Sfot_to_TrackObjectQuad<T,D>(tracker);
	}

	/**
	 * Very basic and very fast implementation of mean-shift which uses a fixed sized rectangle for its region.
	 * The type of color model it uses is specified by modelType.
	 *
	 * @see TrackerMeanShiftLikelihood
	 *
	 * @param maxIterations Maximum number of mean-shift iteraions.  Try 30.
	 * @param numBins Number of bins in the histogram color model.  Try 5.
	 * @param maxPixelValue Maximum pixel value.  For 8-bit images this will be 255
	 * @param modelType Type of color model used.
	 * @param bandType Type of band in the color image
	 * @return TrackerObjectQuad based on {@link TrackerMeanShiftLikelihood}.
	 */
	public static <T extends ImageSingleBand>
	TrackerObjectQuad<MultiSpectral<T>> createMeanShiftLikelihood( int maxIterations ,
																   int numBins ,
																   double maxPixelValue ,
																   MeanShiftLikelihoodType modelType,
																   Class<T> bandType ) {
		PixelLikelihood<MultiSpectral<T>> likelihood;

		switch( modelType ) {
			case HISTOGRAM:
				likelihood = FactoryTrackerObjectAlgs.likelihoodHistogramCoupled(maxPixelValue,numBins,bandType);
				break;

			case HISTOGRAM_INDEPENDENT_RGB_to_HSV:
				likelihood = FactoryTrackerObjectAlgs.likelihoodHueSatHistIndependent(maxPixelValue,numBins,bandType);
				break;

			case HISTOGRAM_RGB_to_HSV:
				likelihood = FactoryTrackerObjectAlgs.likelihoodHueSatHistCoupled(maxPixelValue,numBins,bandType);
				break;

			default:
				throw new IllegalArgumentException("Unknown likelihood model "+modelType);
		}

		TrackerMeanShiftLikelihood<MultiSpectral<T>> alg =
				new TrackerMeanShiftLikelihood<MultiSpectral<T>>(likelihood,maxIterations,0.1f);

		return new Msl_to_TrackerObjectQuad<T>(alg,bandType);
	}

	/**
	 * Implementation of mean-shift which can handle gradual changes in scale of the object being tracked.
	 * The region is specified using a rotated rectangle and its orientation is assumed to be constant.
	 *
	 * @see TrackerMeanShiftComaniciu2003
	 * @param config Configuration
	 * @param <T> Image type
	 * @return TrackerObjectQuad based on Comaniciu2003
	 */
	public static <T extends ImageMultiBand>
	TrackerObjectQuad<T> createMeanShiftComaniciu2003( ConfigComaniciu2003<T> config ) {

		InterpolatePixelMB<T> interp = FactoryInterpolation.createPixelMB(0,config.maxPixelValue,
				config.interpolation,config.imageType);

		LocalWeightedHistogramRotRect<T> hist =
				new LocalWeightedHistogramRotRect<T>(config.numSamples,config.numSigmas,config.numHistogramBins,
						config.imageType.getNumBands(),config.maxPixelValue,interp);
		TrackerMeanShiftComaniciu2003<T> alg = new TrackerMeanShiftComaniciu2003<T>(
				config.updateHistogram,config.meanShiftMaxIterations,config.meanShiftMinimumChange,
				config.scaleWeight,config.constantScale,hist);

		return new Comaniciu2003_to_TrackObjectQuad<T>(alg,config.imageType);
	}
}
