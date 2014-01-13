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

		// update the pixelToregion
		for( int i = 0; i < mergeList.size; i++ ) {
			if( mergeList.data[i] == -1 )
				mergeList.data[i] = i;
		}
		BinaryImageOps.relabel(pixelToRegion, mergeList.data);

	}

	/**
	 * After merging regions there is likely to be many unused regions.  This removes those unused regions
	 * and updates all references.
	 *
	 * mergeList is changed to use the new compacted IDs.  the two input lists are compacted.
	 */
	protected void compactRegionId(GrowQueue_I32 regionMemberCount, GrowQueue_F32 regionColor) {
		int offset = 0;
		for( int i = 0; i < mergeList.size; i++ ) {
			int w = mergeList.data[i];
			if( w == -1 ){
				// see if there is a need to change anything
				if( offset == 0 )
					continue;

				// find all references to the original region and change it to the new index
				int dst = i-offset;
				for( int j = 0; j < mergeList.size; j++ ) {
					if( mergeList.data[j] == i ) {
						mergeList.data[j] = dst;
					}
				}

				// shift over data which describes the region
				regionMemberCount.data[dst] = regionMemberCount.data[i];
				regionColor.data[dst] = regionColor.data[i];
			} else {
				// skip over these since it will be over written later
				offset++;
			}
		}

		regionMemberCount.size -= offset;
		regionColor.size -= offset;
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
	private void createMergeList(ImageSInt32 pixelToregion, GrowQueue_F32 regionColor) {
		// merge the merge list as initial all no merge
		mergeList.resize(regionColor.getSize());
		for( int i = 0; i < mergeList.size; i++ ) {
			mergeList.data[i] = -1;
		}

		// TODO handle border case
		for( int y = 0; y < pixelToregion.height-1; y++ ) {
			int pixelIndex = y*pixelToregion.width;
			for( int x = 0; x < pixelToregion.width-1; x++ , pixelIndex++) {
				int a = pixelToregion.data[pixelIndex];
				int b = pixelToregion.data[pixelIndex+1]; // pixel +1 x
				int c = pixelToregion.data[pixelIndex+pixelToregion.width]; // pixel +1 y

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
	}

	/**
	 * Two pixels have been found to have regions of similar color and are not the same region.  This will mark
	 * one region as being merged into the other.  If one or both have already been previously merged then
	 * they will point to one of their end destinations.
	 *
	 * This procedure ensures that any merge reference will be at most one deep.  E.g. nothing like A -> B -> C
	 */
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
			}

		} else if( alreadyA ) {
			// have B link to the same one as A
			mergeList.data[regionB] = mergeList.data[regionA];
		} else if( alreadyB ) {
			// have A link to the same one as B
			mergeList.data[regionA] = mergeList.data[regionB];
		} else {
			// have B point to A, arbitrary choice
			mergeList.data[regionB] = regionA;
		}
	}
}
