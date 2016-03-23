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

package boofcv.alg.segmentation;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.Arrays;

/**
 * Useful functions related to image segmentation
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
	public static int countRegionPixels(GrayS32 labeled , int which ) {
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

	/**
	 * Counts the number of pixels in all regions.  Regions must be have labels from 0 to totalRegions-1.
	 *
	 * @param labeled (Input) labeled image
	 * @param totalRegions Total number of regions
	 * @param counts Storage for pixel counts
	 */
	public static void countRegionPixels(GrayS32 labeled , int totalRegions , int counts[] ) {

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
	 * The ID of a root node must the index of a pixel in the 'graph' image, taking in account the change
	 * in coordinates for sub-images.
	 *
	 * @param graph Input segmented image where the ID's are not compacted
	 * @param segmentId List of segment ID's.  See comment above about what ID's are acceptable.
	 * @param output The new image after it has been compacted
	 */
	public static void regionPixelId_to_Compact(GrayS32 graph, GrowQueue_I32 segmentId, GrayS32 output) {

		InputSanityCheck.checkSameShape(graph,output);

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
			int indexGraph = segmentId.data[i] - graph.startIndex;

			int x = indexGraph%graph.stride;
			int y = indexGraph/graph.stride;

			output.data[output.startIndex + y*output.stride + x] = i;
		}
	}

	/**
	 * Indicates border pixels between two regions.  If two adjacent pixels (4-connect) are not from the same region
	 * then both pixels are marked as true (value of 1) in output image, all other pixels are false (0).
	 *
	 * @param labeled Input segmented image.
	 * @param output Output binary image.  1 for border pixels.
	 */
	public static void markRegionBorders(GrayS32 labeled , GrayU8 output ) {

		InputSanityCheck.checkSameShape(labeled,output);

		ImageMiscOps.fill(output,0);

		for( int y = 0; y < output.height-1; y++ ) {
			int indexLabeled = labeled.startIndex + y*labeled.stride;
			int indexOutput = output.startIndex + y*output.stride;

			for( int x = 0; x < output.width-1; x++ , indexLabeled++ , indexOutput++ ) {
				int region0 = labeled.data[indexLabeled];
				int region1 = labeled.data[indexLabeled+1];
				int region2 = labeled.data[indexLabeled+labeled.stride];

				if( region0 != region1 ) {
					output.data[indexOutput] = 1;
					output.data[indexOutput+1] = 1;
				}

				if( region0 != region2 ) {
					output.data[indexOutput] = 1;
					output.data[indexOutput+output.stride] = 1;
				}
			}
		}

		for( int y = 0; y < output.height-1; y++ ) {
			int indexLabeled = labeled.startIndex + y*labeled.stride + output.width-1;
			int indexOutput = output.startIndex + y*output.stride + output.width-1;

			int region0 = labeled.data[indexLabeled];
			int region2 = labeled.data[indexLabeled+labeled.stride];

			if( region0 != region2 ) {
				output.data[indexOutput] = 1;
				output.data[indexOutput+output.stride] = 1;
			}
		}

		int y = output.height-1;
		int indexLabeled = labeled.startIndex + y*labeled.stride;
		int indexOutput = output.startIndex + y*output.stride;
		for( int x = 0; x < output.width-1; x++ , indexLabeled++ , indexOutput++ ) {
			int region0 = labeled.data[indexLabeled];
			int region1 = labeled.data[indexLabeled+1];

			if( region0 != region1 ) {
				output.data[indexOutput] = 1;
				output.data[indexOutput+1] = 1;
			}
		}
	}
}
