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

package boofcv.alg.flow;

import boofcv.alg.tracker.klt.KltFeature;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.struct.flow.FlowImage;
import boofcv.struct.image.ImageSingleBand;

/**
 * Computes the dense optical flow using {@link KltTracker}.  A feature is computed from each pixel in the prev
 * image and it is tracked into the curr image.
 *
 * @author Peter Abeles
 */
// TODO Could speed up by setting description of feature without interpolation
public class DenseOpticalFlowKlt<I extends ImageSingleBand, D extends ImageSingleBand> {

	KltTracker<I,D> tracker;
	KltFeature feature;

	public DenseOpticalFlowKlt(KltTracker<I, D> tracker , int radius ) {
		this.tracker = tracker;
		feature = new KltFeature(radius);
	}

	public void process( I prev , D prevDerivX , D prevDerivY , I curr , FlowImage output ) {

		int indexOut = 0;
		for( int y = 0; y < prev.height; y++ ) {
			for( int x = 0; x < prev.width; x++ , indexOut++ ) {
				FlowImage.D flow = output.data[indexOut];
				flow.valid = false;

				tracker.unsafe_setImage(prev,prevDerivX,prevDerivY);
				feature.setPosition(x,y);

				if( tracker.setDescription(feature) ) {
					// derivX and derivY are not used, but can't be null for setImage()
					tracker.unsafe_setImage(curr,prevDerivX,prevDerivY);
					KltTrackFault fault = tracker.track(feature);
					if( fault == KltTrackFault.SUCCESS ) {
						flow.x = feature.x-x;
						flow.y = feature.y-y;
						flow.valid = true;
					}
				}
			}
		}
	}
}
