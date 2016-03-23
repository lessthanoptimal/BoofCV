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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Given a labeled image in which pixels that contains the same label may or may not be connected to each other,
 * create a new labeled image in which only connected pixels have the same label.
 * A two pass algorithm is used.  In the first pass pixels are examined from top to bottom, left to right.  For
 * each pixel (the target), the input image and output image labels of its adjacent pixels are examined.  If an adjacent
 * pixel has the same input label as the target then it is either assigned the same output label or marked for being
 * merged.  Depending if it is not labeled or has an output label already, respectively. After all the pixels are process
 * the merge requests are examined and a new set of output labels is created.  A pass across the output image is
 * done to relabel the inputs.
 * </p>
 *
 * <p>
 * Clustering can be done using 4 or 8 connect, which defines what an adjacent pixel is. 4-connect just considers
 * pixels which are (+1,0) (0,1) (-1,0) (0,-1). 8-connect considers (+1,0) (0,1) (-1,0) (0,-1) and
 * (1,1) (-1,1) (-1,-1) (1,-1).
 * </p>
 *
 * @author Peter Abeles
 */
public class ClusterLabeledImage extends RegionMergeTree {

	// which connectivity rule is used.  4 or 8.
	protected ConnectRule connectRule;

	// offset in pixel indices for adjacent pixels
	protected int edgesIn[];
	protected int edgesOut[];

	// relative coordinates of adjacent pixels
	protected Point2D_I32 edges[];

	// contains the number of pixels in each output label
	protected GrowQueue_I32 regionMemberCount;

	/**
	 * Configures labeling
	 *
	 * @param connectRule Which connectivity rule to use.  4 or 8
	 */
	public ClusterLabeledImage(ConnectRule connectRule) {
		this.connectRule = connectRule;

		if( connectRule == ConnectRule.EIGHT ) {
			edgesIn = new int[4];
			edgesOut = new int[4];
			edges = new Point2D_I32[4];
		} else if( connectRule == ConnectRule.FOUR ) {
			edgesIn = new int[2];
			edgesOut = new int[2];
			edges = new Point2D_I32[2];
		} else {
			throw new IllegalArgumentException("connectRule must be 4 or 8");
		}
		for( int i = 0; i < edges.length; i++ )
			edges[i] = new Point2D_I32();
	}

	/**
	 * Declares lookup tables for neighbors
	 */
	protected void setUpEdges(GrayS32 input , GrayS32 output  ) {
		if( connectRule == ConnectRule.EIGHT ) {
			setUpEdges8(input,edgesIn);
			setUpEdges8(output,edgesOut);

			edges[0].set( 1, 0);
			edges[1].set( 1, 1);
			edges[2].set( 0, 1);
			edges[3].set(-1, 0);
		} else {
			setUpEdges4(input,edgesIn);
			setUpEdges4(output,edgesOut);

			edges[0].set( 1,0);
			edges[1].set( 0, 1);
		}
	}

	protected void setUpEdges8(GrayS32 image , int edges[] )  {
		edges[0] =  1;
		edges[1] =  1 + image.stride;
		edges[2] =    + image.stride;
		edges[3] = -1 + image.stride;
	}

	protected void setUpEdges4(GrayS32 image , int edges[] )  {
		edges[0] =  1;
		edges[1] =    + image.stride;
	}

	/**
	 * Relabels the image such that all pixels with the same label are a member of the same graph.
	 *
	 * @param input Labeled input image.
	 * @param output Labeled output image.
	 * @param regionMemberCount (Input/Output) Number of pixels which belong to each group.
	 */
	public void process(GrayS32 input , GrayS32 output , GrowQueue_I32 regionMemberCount ) {
		// initialize data structures
		this.regionMemberCount = regionMemberCount;
		regionMemberCount.reset();

		setUpEdges(input,output);
		ImageMiscOps.fill(output,-1);

		// this is a bit of a hack here.  Normally you call the parent's init function.
		// since the number of regions is not initially known this will grow
		mergeList.reset();

		connectInner(input, output);
		connectLeftRight(input, output);
		connectBottom(input, output);

		// Merge together all the regions that are connected in the output image
		performMerge(output, regionMemberCount);
	}

	/**
	 * Examines pixels inside the image without the need for bounds checking
	 */
	protected void connectInner(GrayS32 input, GrayS32 output) {

		int startX = connectRule == ConnectRule.EIGHT ? 1 : 0;

		for( int y = 0; y < input.height-1; y++ ) {
			int indexIn = input.startIndex + y*input.stride + startX;
			int indexOut = output.startIndex + y*output.stride + startX;

			for( int x = startX; x < input.width-1; x++ , indexIn++, indexOut++) {
				int inputLabel = input.data[indexIn];
				int outputLabel = output.data[indexOut];
				if( outputLabel == -1 ) { // see if it needs to create a new output segment
					output.data[indexOut] = outputLabel = regionMemberCount.size;
					regionMemberCount.add(1);
					mergeList.add(outputLabel);
				}

				for( int i = 0; i < edgesIn.length; i++ ) {
					if( inputLabel == input.data[indexIn+edgesIn[i]] ) {
						int outputAdj = output.data[indexOut+edgesOut[i]];
						if( outputAdj == -1 ) {  // see if not assigned
							regionMemberCount.data[outputLabel]++;
							output.data[indexOut+edgesOut[i]] = outputLabel;
						} else if( outputLabel != outputAdj ) { // see if assigned to different regions
							markMerge(outputLabel,outputAdj);
						} // do nothing, same input and output labels
					}
				}
			}
		}
	}

	/**
	 * Examines pixels along the left and right border
	 */
	protected void connectLeftRight(GrayS32 input, GrayS32 output) {
		for( int y = 0; y < input.height; y++ ) {
			int x = input.width-1;

			int inputLabel = input.unsafe_get(x, y);
			int outputLabel = output.unsafe_get(x, y);

			if( outputLabel == -1 ) { // see if it needs to create a new output segment
				outputLabel = regionMemberCount.size;
				output.unsafe_set(x,y,outputLabel);
				regionMemberCount.add(1);
				mergeList.add(outputLabel);
			}

			// check right first
			for( int i = 0; i < edges.length; i++ ) {

				Point2D_I32 offset = edges[i];

				// make sure it is inside the image
				if( !input.isInBounds(x+offset.x,y+offset.y))
					continue;

				if( inputLabel == input.unsafe_get(x+offset.x,y+offset.y) ) {
					int outputAdj = output.unsafe_get(x+offset.x,y+offset.y);
					if( outputAdj == -1 ) {  // see if not assigned
						regionMemberCount.data[outputLabel]++;
						output.unsafe_set(x+offset.x,y+offset.y, outputLabel);
					} else if( outputLabel != outputAdj ) { // see if assigned to different regions
						markMerge(outputLabel,outputAdj);
					} // do nothing, same input and output labels
				}
			}

			// skip check of left of 4-connect
			if( connectRule != ConnectRule.EIGHT )
				continue;

			x = 0;

			inputLabel = input.unsafe_get(x, y);
			outputLabel = output.unsafe_get(x, y);

			if( outputLabel == -1 ) { // see if it needs to create a new output segment
				outputLabel = regionMemberCount.size;
				output.unsafe_set(x,y,outputLabel);
				regionMemberCount.add(1);
				mergeList.add(outputLabel);
			}

			for( int i = 0; i < edges.length; i++ ) {
				Point2D_I32 offset = edges[i];

				// make sure it is inside the image
				if( !input.isInBounds(x+offset.x,y+offset.y))
					continue;

				if( inputLabel == input.unsafe_get(x+offset.x,y+offset.y) ) {
					int outputAdj = output.unsafe_get(x+offset.x,y+offset.y);
					if( outputAdj == -1 ) {  // see if not assigned
						regionMemberCount.data[outputLabel]++;
						output.unsafe_set(x+offset.x,y+offset.y, outputLabel);
					} else if( outputLabel != outputAdj ) { // see if assigned to different regions
						markMerge(outputLabel,outputAdj);
					} // do nothing, same input and output labels
				}
			}
		}
	}

	/**
	 * Examines pixels along the bottom border
	 */
	protected void connectBottom(GrayS32 input, GrayS32 output) {
		for( int x = 0; x < input.width-1; x++ ) {
			int y = input.height-1;

			int inputLabel = input.unsafe_get(x,y);
			int outputLabel = output.unsafe_get(x,y);

			if( outputLabel == -1 ) { // see if it needs to create a new output segment
				outputLabel = regionMemberCount.size;
				output.unsafe_set(x,y,outputLabel);
				regionMemberCount.add(1);
				mergeList.add(outputLabel);
			}

			// for 4 and 8 connect the check is only +1 x and 0 y
			if( inputLabel == input.unsafe_get(x+1,y) ) {
				int outputAdj = output.unsafe_get(x+1,y);
				if( outputAdj == -1 ) {  // see if not assigned
					regionMemberCount.data[outputLabel]++;
					output.unsafe_set(x+1,y, outputLabel);
				} else if( outputLabel != outputAdj ) { // see if assigned to different regions
					markMerge(outputLabel,outputAdj);
				} // do nothing, same input and output labels
			}
		}
	}
}
