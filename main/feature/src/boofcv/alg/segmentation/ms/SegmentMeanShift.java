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

import boofcv.alg.InputSanityCheck;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Performs mean-shift segmentation on an image.  Primary based upon the description provided in [1], it first
 * uses mean-shift to find the mode of each pixel in the image.  The mode is the location mean-shift converges to
 * when initialized at a particular pixel.  All the pixels which have the same mode, to within tolerance, are combined
 * into one region.  Since mean-shift does not guarantee that pixels which have the same label are connected
 * to each other the labeled image is now segmented to ensure connectivity between labels.  If a minimum size is
 * specified for a segment then small regions are pruned and their pixels combined into the adjacent region which has
 * the most similar color.
 * </p>
 *
 * <p>
 * Mean-shift segmentation can be slow but it has the advantage of there being relatively few tuning parameters.
 * Few tuning parameters make it easier to work with and to some extent more robust.
 * </p>
 *
 * <p>
 * NOTE: Connectivity rule of 4 tends to produce more tightly compact regions while 8 produces fewer regions but
 * with a more complex surface.
 * </p>
 *
 * <p>
 * [1] Comaniciu, Dorin, and Peter Meer. "Mean shift analysis and applications." Computer Vision, 1999.
 * The Proceedings of the Seventh IEEE International Conference on. Vol. 2. IEEE, 1999.
 * </p>
 * @author Peter Abeles
 */
public class SegmentMeanShift<T extends ImageBase> {
	// finds mean shift modes
	SegmentMeanShiftSearch<T> search;
	// Combines similar regions together
	MergeRegionMeanShift merge;
	// ensures that all pixels in segment are connected
	ClusterLabeledImage segment;
	// Prunes and merges smaller regions together
	MergeSmallRegions<T> prune;

	// contains resegmented image after enforcing all points be connected
//	GrayS32 pixelToRegion2 = new GrayS32(1,1);

	/**
	 * Specifies internal classes used by mean-shift.
	 *
	 * @param search mean-shift search
	 * @param merge Used to merge regions
	 * @param prune Prunes small regions and merges them  If null then prune step will be skipped.
	 * @param connectRule Specify if a 4 or 8 connect rule should be used when segmenting disconnected regions. Try 4
	 */
	public SegmentMeanShift(SegmentMeanShiftSearch<T> search,
							MergeRegionMeanShift merge,
							MergeSmallRegions<T> prune,
							ConnectRule connectRule )
	{
		this.search = search;
		this.merge = merge;
		this.prune = prune;
		this.segment = new ClusterLabeledImage(connectRule);
	}

	/**
	 * Performs mean-shift segmentation on the input image.   The
	 * total number of regions can be found by calling {@link #getNumberOfRegions()}.
	 *
	 * @param image Image
	 * @param output Storage for output image.  Each pixel is set to the region it belongs to.
	 */
	public void process( T image , GrayS32 output ) {
		InputSanityCheck.checkSameShape(image,output);

//		long time0 = System.currentTimeMillis();

		search.process(image);

//		long time1 = System.currentTimeMillis();

		FastQueue<float[]> regionColor = search.getModeColor();
		GrayS32 pixelToRegion = search.getPixelToRegion();
		GrowQueue_I32 regionPixelCount = search.getRegionMemberCount();
		FastQueue<Point2D_I32> modeLocation = search.getModeLocation();

		merge.process(pixelToRegion,regionPixelCount,regionColor,modeLocation);

//		long time2 = System.currentTimeMillis();

		segment.process(pixelToRegion, output, regionPixelCount);

//		long time3 = System.currentTimeMillis();

		if( prune != null)
			prune.process(image,output,regionPixelCount,regionColor);

//		long time4 = System.currentTimeMillis();

//		System.out.println("Search: "+(time1-time0)+" Merge: "+(time2-time1)+
//				" segment: "+(time3-time2)+" Prune: "+(time4-time3));
	}

	/**
	 * The number of regions which it found in the image.
	 * @return Total regions
	 */
	public int getNumberOfRegions() {
		return search.getRegionMemberCount().size;
	}

	/**
	 * Average color of each region
	 */
	public FastQueue<float[]> getRegionColor() {
		return search.getModeColor();
	}

	/**
	 * Number of pixels in each region
	 */
	public GrowQueue_I32 getRegionSize() {
		return search.getRegionMemberCount();
	}

	public ImageType<T> getImageType() {
		return search.getImageType();
	}
}
