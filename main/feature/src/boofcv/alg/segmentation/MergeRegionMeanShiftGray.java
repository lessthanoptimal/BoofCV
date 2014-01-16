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

	// the new ID of the root nodes (segments)
	protected GrowQueue_I32 rootID = new GrowQueue_I32();

	// Local copy of these lists after elements which have been merged are removed
	GrowQueue_I32 tmpMemberCount = new GrowQueue_I32();
	GrowQueue_F32 tmpColor = new GrowQueue_F32();

	/**
	 * Configures merging.
	 *
	 * @param tolerance How similar in color two adjacent segments need to be to be considered the same segment.
	 *                  For 8-bit images try 5.
	 */
	public MergeRegionMeanShiftGray(float tolerance) {
		this.tolerance = tolerance;
	}

	/**
	 * Merges equivalent segments together and updates all the data structures by removing the redundant segments.
	 */
	public void merge( ImageSInt32 pixelToRegion , GrowQueue_I32 regionMemberCount , GrowQueue_F32 regionColor )
	{
		// see which ones need to be merged into which ones
		createMergeList(pixelToRegion, regionColor);

		// update member counts
		flowIntoRootNode(regionMemberCount);

		// re-assign the number of the root node and trim excessive nodes from the lists
		setToRootNodeNewID(regionMemberCount,regionColor);

		// change the labels in the pixelToRegion image
		BinaryImageOps.relabel(pixelToRegion, mergeList.data);
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
	 * For each region in the merge list which is not a root node, find its root node and add to the root node
	 * its member count and set the index  in mergeList to the root node.  If a node is a root node just note
	 * what its new ID will be after all the other segments are removed.
	 */
	protected void flowIntoRootNode(GrowQueue_I32 regionMemberCount) {
		rootID.resize(regionMemberCount.size);
		int count = 0;

		for( int i = 0; i < mergeList.size; i++ ) {
			int p = mergeList.data[i];

			// see if it is a root note
			if( p == -1 ) {
				// mark the root nodes new ID
				rootID.data[i] = count++;
				continue;
			}

			// traverse down until it finds the root note
			int gp = mergeList.data[p];
			while( gp != -1 ) {
				p = gp;
				gp = mergeList.data[p];
			}

			// update the count and change this node into the root node
			regionMemberCount.data[p] += regionMemberCount.data[i];
			mergeList.data[i] = p;
		}
	}

	/**
	 * Does much of the work needed to remove the redundant segments that are being merged into their root node.
	 * The list of member count and colors is updated.  mergeList is updated with the new segment IDs.
	 */
	protected void setToRootNodeNewID( GrowQueue_I32 regionMemberCount, GrowQueue_F32 regionColor ) {

		tmpMemberCount.reset();
		tmpColor.reset();

		for( int i = 0; i < mergeList.size; i++ ) {
			int p = mergeList.data[i];

			if( p == -1 ) {
				mergeList.data[i] = rootID.data[i];
				tmpMemberCount.add( regionMemberCount.data[i] );
				tmpColor.add( regionColor.data[i] );
			} else {
				mergeList.data[i] = rootID.data[mergeList.data[i]];
			}
		}

		regionMemberCount.reset();
		regionColor.reset();

		regionMemberCount.addAll(tmpMemberCount);
		regionColor.addAll(tmpColor);
	}


	/**
	 * If the two regions are not really the same region regionB will become a member of regionA.  A quick
	 * check is done to see if they are really the same region.  If that fails it will traverse down the
	 * inheritance path for each region until it gets to their roots.  If the roots are not the same then
	 * they are merged.  Either way the path is updated such that the quick check will pass.
	 */
	protected void checkMerge( int regionA , int regionB ) {

		int dA = mergeList.data[regionA];
		int dB = mergeList.data[regionB];

		// see if they link to the same thing doing the quick check
		if( dA != -1 && dB != -1 ) {
			if( dA == dB )
				return;
		} else if( dA != -1 ) {
			if( dA == regionB )
				return;
		} else if( dB != -1 ) {
			if( dB == regionA )
				return;
		}

		// search down to the root node
		int rootA = regionA;
		while( dA != -1 ) {
			rootA = dA;
			dA = mergeList.data[rootA];
		}

		int rootB = regionB;
		while( dB != -1 ) {
			rootB = dB;
			dB = mergeList.data[rootB];
		}

		// if they are not the same link merge one into the other
		if( rootA != rootB ) {
			mergeList.data[rootB] = rootA;
		}

		// make it so that the quick check will work the next time        '
		if( regionB != rootA ) {
			mergeList.data[regionB] = rootA;
		}
		if( mergeList.data[regionA] != -1 ) {
			mergeList.data[regionA] = rootA;
		}


	}
}
