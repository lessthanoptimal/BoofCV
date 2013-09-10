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

import boofcv.alg.tracker.meanshift.LikelihoodHistCoupled_U8;
import boofcv.alg.tracker.meanshift.LikelihoodHueSatHistCoupled_U8;
import boofcv.alg.tracker.meanshift.LikelihoodHueSatHistInd_U8;
import boofcv.alg.tracker.meanshift.PixelLikelihood;
import boofcv.alg.tracker.sfot.SfotConfig;
import boofcv.alg.tracker.sfot.SparseFlowObjectTracker;
import boofcv.alg.tracker.tld.TldConfig;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import boofcv.struct.image.MultiSpectral;

/**
 * Factory for creating low level implementations of object tracking algorithms.  These algorithms allow
 * the user to specify an object in a video stream and then track it.  For a high level and user to use
 * common interface see {@kink FactoryTrackerObjectQuad}
 *
 * @author Peter Abeles
 */
public class FactoryTrackerObjectAlgs {

	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	TldTracker<T,D> createTLD( TldConfig<T,D> config ) {
		return new TldTracker<T,D>(config);
	}

	public static <T extends ImageSingleBand,D extends ImageSingleBand>
	SparseFlowObjectTracker<T,D> createSparseFlow( SfotConfig<T,D> config ) {
		return new SparseFlowObjectTracker<T,D>(config);
	}

	public static <T extends ImageSingleBand>
	PixelLikelihood<MultiSpectral<T>> likelihoodHueSatHistIndependent( double maxPixelValue , int numHistogramBins ,
																	   Class<T> bandType )
	{
		if( bandType == ImageUInt8.class ) {
			return (PixelLikelihood)new LikelihoodHueSatHistInd_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+bandType.getSimpleName());
		}
	}

	public static <T extends ImageSingleBand>
	PixelLikelihood<MultiSpectral<T>> likelihoodHueSatHistCoupled( double maxPixelValue , int numHistogramBins ,
																	   Class<T> bandType )
	{
		if( bandType == ImageUInt8.class ) {
			return (PixelLikelihood)new LikelihoodHueSatHistCoupled_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+bandType.getSimpleName());
		}
	}

	public static <T extends ImageSingleBand>
	PixelLikelihood<MultiSpectral<T>> likelihoodHistogramCoupled( double maxPixelValue , int numHistogramBins ,
																	   Class<T> bandType )
	{
		if( bandType == ImageUInt8.class ) {
			return (PixelLikelihood)new LikelihoodHistCoupled_U8((int)maxPixelValue,numHistogramBins);
		} else {
			throw new RuntimeException("Band type not yet supported "+bandType.getSimpleName());
		}
	}
}
