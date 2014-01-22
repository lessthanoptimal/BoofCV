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

package boofcv.alg.segmentation;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.struct.image.ImageSInt32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * Merges regions together quickly and efficiently using a directed tree graph.  To merge two segments together
 * first call {@Link #markMerge}. Then after all the regions which are to be merged are marked call
 * {@link #performMerge}.
 *
 *
 * Internally a directed tree graph is maintained.  When two regions are marked to be merged  they are made to
 * point to the same node.  Later on the graph is examined and all connected nodes are made to point to the same
 * root.  Then all data structures are updated.
 *
 * @author Peter Abeles
 */
public class RegionMergeTree {

	// list used to convert the original region ID's into their new compacted ones
	// The values indicate which region a region is to be merged into
	// An value of -1 indicates that the region is not to be merged with any others
	protected GrowQueue_I32 mergeList = new GrowQueue_I32();

	// Local copy of these lists after elements which have been merged are removed
	protected GrowQueue_I32 tmpMemberCount = new GrowQueue_I32();

	// the new ID of the root nodes (segments)
	protected GrowQueue_I32 rootID = new GrowQueue_I32();

	/**
	 * Must call before any other functions.
	 * @param numRegions Total number of regions.
	 */
	public void initializeMerge(int numRegions) {
		mergeList.resize(numRegions);
		mergeList.fill(-1);
	}

	/**
	 * Merges regions together and updates the provided data structures for said changes.
	 *
	 * @param pixelToRegion (Input/Output) Image used to convert pixel location in region ID.  Modified.
	 * @param regionMemberCount (Input/Output) List containing how many pixels belong to each region.  Modified.
	 */
	public void performMerge( ImageSInt32 pixelToRegion ,
							  GrowQueue_I32 regionMemberCount ) {
		// update member counts
		flowIntoRootNode(regionMemberCount);

		// re-assign the number of the root node and trim excessive nodes from the lists
		setToRootNodeNewID(regionMemberCount);

		// change the labels in the pixelToRegion image
		BinaryImageOps.relabel(pixelToRegion, mergeList.data);
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
	 * The list of member count is updated.  mergeList is updated with the new segment IDs.
	 */
	protected void setToRootNodeNewID( GrowQueue_I32 regionMemberCount ) {

		tmpMemberCount.reset();

		for( int i = 0; i < mergeList.size; i++ ) {
			int p = mergeList.data[i];

			if( p == -1 ) {
				mergeList.data[i] = rootID.data[i];
				tmpMemberCount.add( regionMemberCount.data[i] );
			} else {
				mergeList.data[i] = rootID.data[mergeList.data[i]];
			}
		}

		regionMemberCount.reset();
		regionMemberCount.addAll(tmpMemberCount);
	}

	/**
	 * <p>This function will mark two regions for merger.</p>
	 *
	 * <p>
	 * If the two regions have yet to be merged into any others then regionB will become a member of regionA.
	 * Otherwise a quick heck is done to see if they are already marked for merging.  If that fails it will
	 * traverse down the tree for each region until it gets to their roots.  If the roots are not the same then
	 * they are merged.  Either way the path is updated such that the quick check will pass.
	 * </p>
	 */
	protected void markMerge(int regionA, int regionB) {

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
