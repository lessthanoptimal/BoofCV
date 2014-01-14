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

package boofcv.alg.segmentation;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageSInt32;
import org.ddogleg.struct.GrowQueue_F32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * In a region with uniform color, mean-shift segmentation will produce lots of regions with identical colors since they
 * are all local maximums.  This will find all such neighbors and merge them into one group.  For each
 * pixel it checks its 4-connect neighbors to see if they are in the same region or not.  If not in the same
 * region it checks to see if their peaks have the same color to within tolerance.  If so a mark will be
 * made in an integer list of regions that one should be merged into another.  A check is made to see
 * if the region it is merged into doesn't merge into another one.  If it does a link will be made directly to
 * the last one it gets merged into.
 *
 * @author Peter Abeles
 */
public class MergeRegionMeanShiftGray {

	// list used to convert the original region ID's into their new compacted ones
	protected GrowQueue_I32 mergeList = new GrowQueue_I32();

	// How similar two region's pixel intensity values need to be for them to be merged
	protected float tolerance;

	public MergeRegionMeanShiftGray(float tolerance) {
		this.tolerance = tolerance;
	}

	public void merge( ImageSInt32 pixelToRegion , GrowQueue_I32 regionMemberCount , GrowQueue_F32 regionColor )
	{
		// see which ones need to be merged into which ones
		createMergeList(pixelToRegion, regionColor);

		// update member counts
		updateMemberCount(regionMemberCount);

		// compact the region id's
		compactRegionId(regionMemberCount, regionColor);

		// update the pixelToRegion
		int count = 0;
		for( int i = 0; i < mergeList.size; i++ ) {
			if( mergeList.data[i] == -1 )
				mergeList.data[i] = count++;
		}

		BinaryImageOps.relabel(pixelToRegion, mergeList.data);

	}

	/**
	 * After merging regions there is likely to be many unused regions.  This removes those unused regions
	 * and updates all references.
	 *
	 * mergeList is changed to use the new compacted IDs.  the two input lists are compacted.
	 */
	// TODO speed this up also
	protected void compactRegionId(GrowQueue_I32 regionMemberCount, GrowQueue_F32 regionColor) {

		GrowQueue_I32 rmcNew = new GrowQueue_I32();
		GrowQueue_F32 colorNew = new GrowQueue_F32();

		int count = 0;
		for( int i = 0; i < mergeList.size; i++ ) {
			int w = mergeList.data[i];
			if( w == -1 ){
				for( int j = 0; j < mergeList.size; j++ ) {
					if( mergeList.data[j] == i ) {
						mergeList.data[j] = count;
					}
				}

				// shift over data which describes the region
				rmcNew.add(regionMemberCount.data[i]);
				colorNew.add(regionColor.data[i]);

				count++;
			}
		}

		regionMemberCount.reset();
		regionColor.reset();

		regionMemberCount.addAll(rmcNew);
		regionColor.addAll(colorNew);
	}

	/**
	 * Adds the counts of all the regions that refer to others.
	 */
	protected void updateMemberCount(GrowQueue_I32 regionMemberCount) {
		for( int i = 0; i < mergeList.size; i++ ) {
			int w = mergeList.data[i];
			if( w != -1 ) {
				regionMemberCount.data[w] += regionMemberCount.data[i];
			}
		}
	}

	/**
	 * Checks the 4-connect of each pixel to see if it references a different region.  If it does it checks to
	 * see if their pixel intensity values are within tolerance of each other.  If so they are then marked for
	 * merging.
	 */
	protected void createMergeList(ImageSInt32 pixelToRegion, GrowQueue_F32 regionColor) {
		// merge the merge list as initial all no merge
		mergeList.resize(regionColor.getSize());
		for( int i = 0; i < mergeList.size; i++ ) {
			mergeList.data[i] = -1;
		}

		// the inner image, excluding the right and bottom borders
		for( int y = 0; y < pixelToRegion.height-1; y++ ) {
			int pixelIndex = y*pixelToRegion.width;
			for( int x = 0; x < pixelToRegion.width-1; x++ , pixelIndex++) {
				int a = pixelToRegion.data[pixelIndex];
				int b = pixelToRegion.data[pixelIndex+1]; // pixel +1 x
				int c = pixelToRegion.data[pixelIndex+pixelToRegion.width]; // pixel +1 y

				float colorA = regionColor.get(a);

				if( a != b ) {
					float colorB = regionColor.get(b);
					boolean merge = Math.abs(colorA-colorB) <= tolerance;
					if( merge ) {
						checkMerge(a,b);
					}
				}

				if( a != c ) {
					float colorC = regionColor.get(c);
					boolean merge = Math.abs(colorA-colorC) <= tolerance;
					if( merge ) {
						checkMerge(a,c);
					}
				}
			}
		}

		// right side of the image
		for( int y = 0; y < pixelToRegion.height-1; y++ ) {
			// location (w-1,y)
			int pixelIndex = y*pixelToRegion.width+pixelToRegion.width-1;

			int a = pixelToRegion.data[pixelIndex];
			int c = pixelToRegion.data[pixelIndex+pixelToRegion.width]; // pixel +1 y

			float colorA = regionColor.get(a);

			if( a != c ) {
				float colorC = regionColor.get(c);
				boolean merge = Math.abs(colorA-colorC) <= tolerance;
				if( merge ) {
					checkMerge(a,c);
				}
			}
		}

		// bottom of the image
		for( int x = 0; x < pixelToRegion.width-1; x++ ) {
			// location (x,h-1)
			int pixelIndex = (pixelToRegion.height-1)*pixelToRegion.width + x;

			int a = pixelToRegion.data[pixelIndex];
			int b = pixelToRegion.data[pixelIndex+1]; // pixel +1 x

			float colorA = regionColor.get(a);

			if( a != b ) {
				float colorB = regionColor.get(b);
				boolean merge = Math.abs(colorA-colorB) <= tolerance;
				if( merge ) {
					checkMerge(a,b);
				}
			}
		}
	}

	/**
	 * Two pixels have been found to have regions of similar color and are not the same region.  This will mark
	 * one region as being merged into the other.  If one or both have already been previously merged then
	 * they will point to one of their end destinations.
	 *
	 * This procedure ensures that any merge reference will be at most one deep.  E.g. nothing like A -> B -> C
	 *
	 * DESIGN NOTE: Could speed this function up a lot by saving which nodes references others instead of traversing
	 * the entire mergeList each time there is a modification
	 */
	// TODO update unit test for changes
	// TODO must speed up
	protected void checkMerge( int regionA , int regionB ) {
		boolean alreadyA = mergeList.data[regionA] != -1;
		boolean alreadyB = mergeList.data[regionB] != -1;

		if( alreadyA && alreadyB ) {
			// if they already point to the same one, do nothing
			if( mergeList.data[regionB] != mergeList.data[regionA] ) {
				// put B into A, arbitrary choice
				// don't point to A instead point to A's destination
				int dstA = mergeList.data[regionA];
				int dstB = mergeList.data[regionB];
				// look for all reference to B and change to A
				for( int i = 0; i < mergeList.size; i++ ) {
					if( mergeList.data[i] == dstB )
						mergeList.data[i] = dstA;
				}
				mergeList.data[dstB] = dstA;
			}

		} else if( alreadyA ) {
			// make sure A is not already pointing to B
			if( mergeList.data[regionA] != regionB ) {
				int dstA = mergeList.data[regionA];

				// have B link to the same one as A
				mergeList.data[regionB] = dstA;

				for( int i = 0; i < mergeList.size; i++ ) {
					if( mergeList.data[i] == regionB )
						mergeList.data[i] = dstA;
				}
			}
		} else if( alreadyB ) {
			// make sure B is not already pointing to A
			if( mergeList.data[regionB] != regionA ) {
				int dstB = mergeList.data[regionB];

				// have A link to the same one as B
				mergeList.data[regionA] = dstB;

				for( int i = 0; i < mergeList.size; i++ ) {
					if( mergeList.data[i] == regionA )
						mergeList.data[i] = dstB;
				}
			}
		} else {
			// have B point to A, arbitrary choice
			mergeList.data[regionB] = regionA;

			for( int i = 0; i < mergeList.size; i++ ) {
				if( mergeList.data[i] == regionB )
					mergeList.data[i] = regionA;
			}
		}
	}
}
