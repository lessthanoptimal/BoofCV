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

package boofcv.alg.tracker.klt;

import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * <p>
 * A pyramid Kanade-Lucas-Tomasi (KLT) tracker that allows features to be tracker over a larger region than the basic
 * ({@link KltTracker}) implementation.  A feature is tracked at multiple resolutions, large motions can
 * be detected at low resolution and are refined at higher resolutions.
 * </p>
 *
 * <p>
 * Tracking is allowed along the image border.  A track is dropped if the smallest track is outside the image.
 * </p>
 *
 * @author Peter Abeles
 */
public class PyramidKltTracker<InputImage extends ImageGray, DerivativeImage extends ImageGray> {

	// basic KLT tracker which works on a single image
	protected KltTracker<InputImage, DerivativeImage> tracker;
	// image pyramid for raw input image
	protected ImagePyramid<InputImage> image;
	// image pyramid for image gradient
	protected DerivativeImage[] derivX;
	protected DerivativeImage[] derivY;

	public PyramidKltTracker(KltTracker<InputImage, DerivativeImage> tracker) {
		this.tracker = tracker;
	}

	/**
	 * Sets the feature's description up.  The feature's (x,y) must have already been set
	 * and {@link #setImage} been called.
	 *
	 * @param feature Feature's whose description is being setup.
	 * @return true if there was sufficient information to create a feature or false if not
	 */
	public boolean setDescription(PyramidKltFeature feature) {
		for (int layer = 0; layer < image.getNumLayers(); layer++) {
			float scale = (float)image.getScale(layer);
			float x = feature.x / scale;
			float y = feature.y /  scale;

			setupKltTracker(layer);

			feature.desc[layer].setPosition(x, y);
			if( !tracker.setDescription(feature.desc[layer]) )
				return false;
		}
		return true;
	}

	/**
	 * Sets the current input images for the tracker to use.
	 * @param image Original image pyramid.
	 * @param derivX Derivative along x-axis.
	 * @param derivY Derivative along y-axis.
	 */
	public void setImage(ImagePyramid<InputImage> image,
						 DerivativeImage[] derivX, DerivativeImage[] derivY) {
		if( image.getNumLayers() != derivX.length || image.getNumLayers() != derivY.length )
			throw new IllegalArgumentException("Number of layers does not match.");

		this.image = image;
		this.derivX = derivX;
		this.derivY = derivY;
	}

	/**
	 * Only sets the image pyramid.  The derivatives are set to null.  Only use this when tracking.
	 * @param image Image pyramid
	 */
	public void setImage(ImagePyramid<InputImage> image ) {
		this.image = image;
		this.derivX = null;
		this.derivY = null;
	}

	/**
	 * <p>
	 * Finds the feature's new location in the image. The feature's position can be modified even if
	 * tracking fails.
	 * </p>
	 *
	 * <p>
	 * NOTE: The feature's description is not updated and tracking over several frames can break down
	 * if its description is not updated.
	 * </p>
	 *
	 * @param feature The feature being tracked.
	 * @return If tracking failed or not.
	 */
	public KltTrackFault track(PyramidKltFeature feature) {

		// this is the first level it was able to track the feature at
		int firstLevelTracked = -1;

		float x = feature.x;
		float y = feature.y;

		// track from the top of the pyramid to the bottom
		for (int layer = image.getNumLayers()-1; layer >= 0; layer--) {
			float scale = (float)image.getScale(layer);
			x /= scale;
			y /= scale;

			// tracking never needs the derivative
			tracker.unsafe_setImage(image.getLayer(layer), null, null);

			KltFeature f = feature.desc[layer];
			f.setPosition(x, y);
			KltTrackFault ret = tracker.track(f);

			if (ret == KltTrackFault.SUCCESS) {
				if( firstLevelTracked == -1 )
					firstLevelTracked = layer;
				// nothing bad happened, save this result
				x = feature.desc[layer].x;
				y = feature.desc[layer].y;
			} else {
				// tracking failed
				return ret;
			}

			x *= scale;
			y *= scale;
		}

		feature.setPosition(x, y);
		return KltTrackFault.SUCCESS;
	}

	/**
	 * Average error between track template and the image.
	 *
	 * @see boofcv.alg.tracker.klt.KltTracker#getError()
	 *
	 * @return error
	 */
	public float getError() {
		return tracker.getError();
	}

	private void setupKltTracker(int layer) {
		if (derivX != null)
			tracker.unsafe_setImage(image.getLayer(layer), derivX[layer], derivY[layer]);
		else
			tracker.unsafe_setImage(image.getLayer(layer), null, null);
	}
}
