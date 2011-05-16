/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import gecv.alg.tracker.klt.KltTrackFault;
import gecv.alg.tracker.klt.KltTracker;
import gecv.struct.image.ImageBase;
import gecv.struct.pyramid.ImagePyramid;

/**
 * <p>
 * A pyramid KLT tracker that allows features to be tracker over a larger region than the basic ({@link KltTracker})
 * implementation.  A feature is tracked at multiple resolutions, large motions can be detected at low resolution and
 * are refined at higher resolutions.
 * </p>
 * <p/>
 * <p>
 * Features are tracked at the lowest layer in the pyramid which can contain the feature.  If a feature is contained
 * or not is defined by the basic tracker provided to the pyramid tracker.  In other words, if this tracker can handle
 * partial features then so can the pyramid tracker.
 * </p>
 *
 * @author Peter Abeles
 */
public class PyramidKltTracker<InputImage extends ImageBase, DerivativeImage extends ImageBase> {

	KltTracker<InputImage, DerivativeImage> tracker;
	ImagePyramid<InputImage> image;
	DerivativeImage[] derivX;
	DerivativeImage[] derivY;

	public PyramidKltTracker(KltTracker<InputImage, DerivativeImage> tracker) {
		this.tracker = tracker;
	}

	public void setDescription(PyramidKltFeature feature) {
		float x = feature.x;
		float y = feature.y;

		for (int layer = 0; layer < image.getNumLayers(); layer++) {
			x /= image.scale[layer];
			y /= image.scale[layer];

			setupKltTracker(layer);

			feature.desc[layer].setPosition(x, y);
			if( !tracker.setDescription(feature.desc[layer]) )
				break;
			feature.maxLayer = layer;
		}
	}

	public void setImage(ImagePyramid<InputImage> image, DerivativeImage[] derivX, DerivativeImage[] derivY) {
		this.image = image;
		if (derivX != null) {
			if (derivX.length != image.getNumLayers() || derivY.length != image.getNumLayers())
				throw new IllegalArgumentException("Unexpected number of images in derivX and/or derivY");
			this.derivX = derivX;
			this.derivY = derivY;
		}
	}

	public KltTrackFault track(PyramidKltFeature feature) {
		boolean worked = false;

		float x = feature.x;
		float y = feature.y;


		// estimate the position in the top most layer
		int scaleAtTop = image.getScalingAtLayer(feature.maxLayer);
		x /= scaleAtTop;
		y /= scaleAtTop;

		// this is the first level it was able to track the feature at
		int firstLevelTracked = -1;

		// track from the top of the pyramid to the bottom
		for (int layer = feature.maxLayer; layer >= 0; layer--) {
			setupKltTracker(layer);

			feature.desc[layer].setPosition(x, y);
			KltTrackFault ret = tracker.track(feature.desc[layer]);

			if (ret == KltTrackFault.OUT_OF_BOUNDS) {
				x = feature.desc[layer].x;
				y = feature.desc[layer].y;
				feature.maxLayer = layer-1;
				// if out of bounds try tracking on a lower layer
			} else if (ret == KltTrackFault.SUCCESS) {
				if( firstLevelTracked == -1 )
					firstLevelTracked = layer;
				// nothing bad happened, save this result
				x = feature.desc[layer].x;
				y = feature.desc[layer].y;
				worked = true;
			} else {
				// tracking failed
				return ret;
			}

			// put the position estimate into the next layer
			x *= image.scale[layer];
			y *= image.scale[layer];
		}



		if (worked) {
			feature.setPosition(x, y);
			return KltTrackFault.SUCCESS;
		} else {
			return KltTrackFault.OUT_OF_BOUNDS;
		}
	}

	private void setupKltTracker(int layer) {
		if (derivX != null)
			tracker.setImage(image.getLayer(layer), derivX[layer], derivY[layer]);
		else
			tracker.setImage(image.getLayer(layer), null, null);
	}
}
