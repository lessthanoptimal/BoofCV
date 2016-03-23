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

package boofcv.alg.flow;

import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.ImagePyramid;

import java.util.Arrays;

/**
 * Computes the dense optical flow using {@link KltTracker}.  A feature is computed from each pixel in the prev
 * image and it is tracked into the curr image. The flow assigned to a pixel is the template with the lowest error
 * which overlaps it.  In other words, a pixel is assigned the flow with the lowest error with in 'radius' pixels
 * of it.  A pixel is marked as invalid if all tracks around the pixel fail.
 *
 * @author Peter Abeles
 */
public class DenseOpticalFlowKlt<I extends ImageGray, D extends ImageGray> {

	// Amount it adjusts the score for the center of a region.
	// Visually this looks better, but only makes a small difference in benchmark performance
	private static float MAGIC_ADJUSTMENT = 0.7f;

	private PyramidKltTracker<I,D> tracker;
	private PyramidKltFeature feature;

	// goodness of fit for each template
	float scores[] = new float[1];

	// size of template
	private int regionRadius;
	// image shape
	private int width,height;

	public DenseOpticalFlowKlt(PyramidKltTracker<I, D> tracker , int numLayers , int radius ) {
		this.tracker = tracker;
		feature = new PyramidKltFeature(numLayers,radius);
		this.regionRadius = radius;
	}

	public void process( ImagePyramid<I> prev, D[] prevDerivX, D[] prevDerivY,
						 ImagePyramid<I> curr , ImageFlow output ) {

		this.width = output.width;
		this.height = output.height;

		// initialize and set the score for each pixel to be very high
		int N = width*height;
		if( scores.length < N)
			scores = new float[N];
		Arrays.fill(scores,0,N,Float.MAX_VALUE);

		for (int i = 0; i < N; i++) {
			output.data[i].markInvalid();
		}

		for( int y = 0; y < output.height; y++ ) {
			for( int x = 0; x < output.width; x++ ) {

				tracker.setImage(prev,prevDerivX,prevDerivY);
				feature.setPosition(x,y);

				if( tracker.setDescription(feature) ) {
					// derivX and derivY are not used, but can't be null for setImage()
					tracker.setImage(curr);
					KltTrackFault fault = tracker.track(feature);
					if( fault == KltTrackFault.SUCCESS ) {
						float score = tracker.getError();
						// bias the result to prefer the central template
						scores[y*output.width+x] = score*MAGIC_ADJUSTMENT;
						output.get(x,y).set(feature.x-x,feature.y-y);
						// see if this flow should be assigned to any of its neighbors
						checkNeighbors(x, y, score, feature.x-x,feature.y-y, output);
					}
				}
			}
		}
	}

	/**
	 * Examines every pixel inside the region centered at (cx,cy) to see if their optical flow has a worse
	 * score the one specified in 'flow'
	 */
	protected void checkNeighbors( int cx , int cy , float score , float flowX , float flowY , ImageFlow output ) {

		int x0 = Math.max(0,cx-regionRadius);
		int x1 = Math.min(output.width, cx + regionRadius + 1);
		int y0 = Math.max(0,cy-regionRadius);
		int y1 = Math.min(output.height, cy + regionRadius + 1);

		for( int i = y0; i < y1; i++ ) {
			int index = width*i + x0;
			for( int j = x0; j < x1; j++ , index++ ) {
				float s = scores[ index ];
				ImageFlow.D f = output.data[index];
				if( s > score ) {
					f.set(flowX,flowY);
					scores[index] = score;
				} else if( s == score ) {
					// Pick solution with the least motion when ambiguous
					float m0 = f.x*f.x + f.y*f.y;
					float m1 = flowX*flowX + flowY*flowY;
					if( m1 < m0 ) {
						f.set(flowX,flowY);
						scores[index] = score;
					}
				}
			}
		}
	}
}
