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

package boofcv.alg.feature.detect.peak;

import boofcv.struct.image.ImageSingleBand;

/**
 *  Implementation of {@link MeanShiftPeak} which uses a flat kernel.
 *
 * @author Peter Abeles
 */
public class MeanShiftUniformPeak<T extends ImageSingleBand> extends MeanShiftPeak<T> {

	/**
	 * Configures search.
	 *
	 * @param maxIterations  Maximum number of iterations.  Try 10
	 * @param convergenceTol Convergence tolerance.  Try 1e-3
	 * @param radius         Search radius.  Application dependent.
	 */
	public MeanShiftUniformPeak(int maxIterations, float convergenceTol, int radius, Class<T> imageType) {
		super(maxIterations, convergenceTol, radius, imageType);
	}

	/**
	 * Performs a mean-shift search center at the specified coordinates
	 */
	@Override
	public void search( float cx , float cy ) {

		peakX = cx; peakY = cy;
		setRegion(cx, cy);

		for( int i = 0; i < maxIterations; i++ ) {
			float total = 0;
			float sumX = 0, sumY = 0;

			// see if it can use fast interpolation otherwise use the safer technique
			if( interpolate.isInFastBounds(x0, y0) &&
					interpolate.isInFastBounds(x0 + width - 1, y0 + width - 1)) {
				for( int yy = 0; yy < width; yy++ ) {
					for( int xx = 0; xx < width; xx++ ) {
						float weight = interpolate.get_fast(x0 + xx, y0 + yy);
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
					}
				}
			} else {
				for( int yy = 0; yy < width; yy++ ) {
					for( int xx = 0; xx < width; xx++ ) {
						float weight = interpolate.get(x0 + xx, y0 + yy);
						total += weight;
						sumX += weight*(xx+x0);
						sumY += weight*(yy+y0);
					}
				}
			}

			cx = sumX/total;
			cy = sumY/total;

			setRegion(cx, cy);

			float dx = cx-peakX;
			float dy = cy-peakY;

			peakX = cx; peakY = cy;

			if( Math.abs(dx) < convergenceTol && Math.abs(dy) < convergenceTol ) {
				break;
			}
		}
	}
}
