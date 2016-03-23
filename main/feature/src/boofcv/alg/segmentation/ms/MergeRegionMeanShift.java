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

package boofcv.alg.segmentation.ms;

import boofcv.struct.image.GrayS32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Merges together regions which have modes close to each other and have a similar color.
 *
 * @author Peter Abeles
 */
public class MergeRegionMeanShift extends RegionMergeTree {

	// maximum distance squared in pixels that two nodes can be apart to be merged
	private int maxSpacialDistanceSq;
	// Maximum Euclidean distance squared two colors can be for them to be considered similar
	private float maxColorDistanceSq;

	// Search radius in pixels when looking for regions to merge with
	private int searchRadius;

	/**
	 * Configures MergeRegionMeanShift
	 *
	 * @param maxSpacialDistance The maximum spacial distance (pixels) at which two modes can be for their
	 *                           regions to be merged together.
	 * @param maxColorDistance The maximum Euclidean distance two colors can be from each other for them to be merged.
	 */
	public MergeRegionMeanShift(int maxSpacialDistance, float maxColorDistance ) {

		this.searchRadius = maxSpacialDistance;
		this.maxSpacialDistanceSq = maxSpacialDistance*maxSpacialDistance;
		this.maxColorDistanceSq = maxColorDistance*maxColorDistance;
	}

	/**
	 * Merges together similar regions which are in close proximity to each other.  After merging
	 * most of the input data structures are modified to take in account the  changes.
	 *
	 * @param pixelToRegion (Input/output) Image that specifies the segmentation.  Modified.
	 * @param regionMemberCount (Input/output) Number of pixels in each region. Modified.
	 * @param regionColor (Input/output) Color of each region. Modified.
	 * @param modeLocation (Input) Location of each region's mode. Not modified.
	 */
	public void process( GrayS32 pixelToRegion ,
						 GrowQueue_I32 regionMemberCount,
						 FastQueue<float[]> regionColor ,
						 FastQueue<Point2D_I32> modeLocation ) {
		initializeMerge(regionMemberCount.size);

		markMergeRegions(regionColor,modeLocation,pixelToRegion);

		performMerge(pixelToRegion, regionMemberCount);
	}

	/**
	 * Takes the mode of a region and searches the local area around it for other regions.  If the region's mode
	 * is also within the local area its color is checked to see if it's similar enough.  If the color is similar
	 * enough then the two regions are marked for merger.
	 */
	protected void markMergeRegions(FastQueue<float[]> regionColor,
									FastQueue<Point2D_I32> modeLocation,
									GrayS32 pixelToRegion  ) {
		for( int targetId = 0; targetId < modeLocation.size; targetId++ ) {

			float[] color = regionColor.get(targetId);
			Point2D_I32 location = modeLocation.get(targetId);

			int x0 = location.x-searchRadius;
			int x1 = location.x+searchRadius+1;
			int y0 = location.y-searchRadius;
			int y1 = location.y+searchRadius+1;

			// ensure that all pixels it examines are inside the image
			if( x0 < 0 ) x0 = 0;
			if( x1 > pixelToRegion.width ) x1 = pixelToRegion.width;
			if( y0 < 0 ) y0 = 0;
			if( y1 > pixelToRegion.height ) y1 = pixelToRegion.height;

			// look at the local neighborhood
			for( int y = y0; y < y1; y++ ) {
				for( int x = x0; x < x1; x++ ) {
					int candidateId = pixelToRegion.unsafe_get(x,y);

					// see if it is the same region
					if( candidateId == targetId )
						continue;

					// see if the mode is near by
					Point2D_I32 p = modeLocation.get(candidateId);
					if( p.distance2(location) <= maxSpacialDistanceSq ) {

						// see if the color is similar
						float[] candidateColor = regionColor.get(candidateId);
						float colorDistance = SegmentMeanShiftSearch.distanceSq(color,candidateColor);

						if( colorDistance <= maxColorDistanceSq ) {
							// mark the two regions as merged
							markMerge(targetId, candidateId);
						}
					}
				}
			}

		}
	}
}
