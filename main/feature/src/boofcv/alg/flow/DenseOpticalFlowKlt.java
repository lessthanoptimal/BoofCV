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

package boofcv.alg.flow;

import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;

/**
 * Computes the dense optical flow using {@link KltTracker}.  A feature is computed from each pixel in the prev
 * image and it is tracked into the curr image.   If the region around a pixel lacks texture or no good match
 * can be found the pixel will be marked as invalid.
 *
 * @author Peter Abeles
 */
public class DenseOpticalFlowKlt<I extends ImageSingleBand, D extends ImageSingleBand> {

	PyramidKltTracker<I,D> tracker;
	PyramidKltFeature feature;

	public DenseOpticalFlowKlt(PyramidKltTracker<I, D> tracker , int numLayers , int radius ) {
		this.tracker = tracker;
		feature = new PyramidKltFeature(numLayers,radius);
	}

	public void process( ImagePyramid<I> prev, D[] prevDerivX, D[] prevDerivY,
						 ImagePyramid<I> curr , ImageFlow output ) {

		int indexOut = 0;
		for( int y = 0; y < output.height; y++ ) {
			for( int x = 0; x < output.width; x++ , indexOut++ ) {
				ImageFlow.D flow = output.data[indexOut];
				flow.markInvalid();

				tracker.setImage(prev,prevDerivX,prevDerivY);
				feature.setPosition(x,y);

				if( tracker.setDescription(feature) ) {
					// derivX and derivY are not used, but can't be null for setImage()
					tracker.setImage(curr);
					KltTrackFault fault = tracker.track(feature);
					if( fault == KltTrackFault.SUCCESS ) {
						flow.x = feature.x-x;
						flow.y = feature.y-y;
					}
				}
			}
		}
	}
}
