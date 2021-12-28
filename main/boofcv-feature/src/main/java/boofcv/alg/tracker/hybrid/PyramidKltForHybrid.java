/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.hybrid;

import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.*;
import boofcv.factory.interpolate.FactoryInterpolation;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * Pyramidal KLT tracker designed for {@link HybridTrackerScalePoint}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PyramidKltForHybrid<I extends ImageGray<I>, D extends ImageGray<D>> {
	/** configuration for low level KLT tracker */
	public ConfigKlt config;

	/** The radius of each feature. 3 is a reasonable number. */
	public int featureRadius;

	// Number of levels in the image pyramid
	protected int numLevels = -1;

	// the tracker
	protected PyramidKltTracker<I, D> tracker;

	public PyramidKltForHybrid( ConfigKlt config,
								int featureRadius,
								Class<I> inputType, Class<D> derivType ) {
		this.config = config;
		this.featureRadius = featureRadius;

		InterpolateRectangle<I> interpInput = FactoryInterpolation.bilinearRectangle(inputType);
		InterpolateRectangle<D> interpDeriv = FactoryInterpolation.bilinearRectangle(derivType);

		KltTracker<I, D> klt = new KltTracker<>(interpInput, interpDeriv, config);
		tracker = new PyramidKltTracker<>(klt);
	}

	protected PyramidKltForHybrid() {}

	public void setDescription( float x, float y, PyramidKltFeature ret ) {
		ret.setPosition(x, y);
		tracker.setDescription(ret);
	}

	public void setInputs( ImagePyramid<I> image, D[] derivX, D[] derivY ) {
		if (numLevels == -1)
			this.numLevels = image.getNumLayers();
		else if (numLevels != image.getNumLayers())
			throw new IllegalArgumentException("Number of levels pyramid changed!");
		tracker.setImage(image, derivX, derivY);
	}

	/**
	 * Updates the track using the latest inputs. If tracking fails then the feature description
	 * in each layer is unchanged and its global position.
	 *
	 * @param feature Feature being updated
	 * @return true if tracking was successful, false otherwise
	 */
	public boolean performTracking( PyramidKltFeature feature ) {

		KltTrackFault result = tracker.track(feature);

		if (result != KltTrackFault.SUCCESS) {
			return false;
		} else {
			tracker.setDescription(feature);
			return true;
		}
	}

	public PyramidKltFeature createNewTrack() {
		return new PyramidKltFeature(numLevels, featureRadius);
	}
}
