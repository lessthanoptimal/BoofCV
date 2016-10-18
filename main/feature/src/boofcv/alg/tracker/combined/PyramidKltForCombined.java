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

package boofcv.alg.tracker.combined;

import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.*;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * Pyramidal KLT tracker designed for {@link CombinedTrackerScalePoint}.
 *
 * @author Peter Abeles
 */
public class PyramidKltForCombined<I extends ImageGray, D extends ImageGray> {
	/** configuration for low level KLT tracker */
	public KltConfig config;

	/** The radius of each feature. 3 is a reasonable number. */
	public int featureRadius;

	/** Scale factor for each layer in the pyramid */
	public int pyramidScaling[];

	// the tracker
	protected PyramidKltTracker<I, D> tracker;

	public PyramidKltForCombined(KltConfig config,
								 int featureRadius,
								 int[] pyramidScaling,
								 Class<I> inputType, Class<D> derivType) {
		this.config = config;
		this.featureRadius = featureRadius;
		this.pyramidScaling = pyramidScaling;

		InterpolateRectangle<I> interpInput = FactoryInterpolation.<I>bilinearRectangle(inputType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.<D>bilinearRectangle(derivType);

		KltTracker<I, D> klt = new KltTracker<>(interpInput, interpDeriv, config);
		tracker = new PyramidKltTracker<>(klt);
	}

	protected PyramidKltForCombined() {
	}

	public void setDescription( float x , float y , PyramidKltFeature ret ) {
		ret.setPosition(x,y);
		tracker.setDescription(ret);
	}

	public void setInputs( ImagePyramid<I> image , D[] derivX , D[] derivY ) {
		tracker.setImage(image,derivX,derivY);
	}

	/**
	 * Updates the track using the latest inputs.  If tracking fails then the feature description
	 * in each layer is unchanged and its global position.
	 *
	 * @param feature Feature being updated
	 * @return true if tracking was successful, false otherwise
	 */
	public boolean performTracking(  PyramidKltFeature feature ) {

		KltTrackFault result = tracker.track(feature);

		if( result != KltTrackFault.SUCCESS ) {
			return false;
		} else {
			tracker.setDescription(feature);
			return true;
		}
	}

	public PyramidKltFeature createNewTrack() {
		return new PyramidKltFeature(pyramidScaling.length, featureRadius);
	}
}
