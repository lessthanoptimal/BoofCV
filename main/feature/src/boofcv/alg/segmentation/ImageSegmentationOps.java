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

import boofcv.struct.image.ImageSInt32;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Arrays;

/**
 * Usefullfunctions related to image segmentation
 *
 * @author Peter Abeles
 */
public class ImageSegmentationOps {


	/**
	 * Counts the number of instances of 'which' inside the labeled image.
	 *
	 * @param labeled Image which has been labeled
	 * @param which The label being searched for
	 * @return Number of instances of 'which' in 'labeled'
	 */
	public static int countRegionPixels( ImageSInt32 labeled , int which ) {
		int total = 0;
		for( int y = 0; y < labeled.height; y++ ) {
			int index = labeled.startIndex + y*labeled.stride;
			for( int x = 0; x < labeled.width; x++ ) {
				if( labeled.data[index++] == which ) {
					total++;
				}
			}
		}
		return total;
	}

	public static void countRegionPixels( ImageSInt32 labeled , int totalRegions , int counts[] ) {

		Arrays.fill(counts,0,totalRegions,0);

		for( int y = 0; y < labeled.height; y++ ) {
			int index = labeled.startIndex + y*labeled.stride;
			for( int x = 0; x < labeled.width; x++ ) {
				counts[labeled.data[index++]]++;
			}
		}
	}


	/**
	 * Compacts the region labels such that they are consecutive numbers starting from 0.
	 * The ID of a root node must the index of a pixel in the region.
	 *
	 * @param graph Input segmented image where the ID's are not compacted
	 * @param segmentId List of segment ID's.  See comment above about what ID's are acceptable.
	 * @param output The new image after it has been compacted
	 */
	public static void regionPixelId_to_Compact(ImageSInt32 graph, GrowQueue_I32 segmentId, ImageSInt32 output) {

		// Change the label of root nodes to be the new compacted labels
		for( int i = 0; i < segmentId.size; i++ ) {
			graph.data[segmentId.data[i]] = i;
		}

		// In the second pass assign all the children to the new compacted labels
		for( int y = 0; y < output.height; y++ ) {
			int indexGraph = graph.startIndex + y*graph.stride;
			int indexOut = output.startIndex + y*output.stride;
			for( int x = 0; x < output.width; x++ , indexGraph++,indexOut++) {
				output.data[indexOut] = graph.data[graph.data[indexGraph]];
			}
		}
		// need to do some clean up since the above approach doesn't work for the roots
		for( int i = 0; i < segmentId.size; i++ ) {
			output.data[segmentId.data[i]] = i;
		}
	}
}
