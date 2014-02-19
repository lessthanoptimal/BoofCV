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

package boofcv.alg.segmentation.watershed;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.struct.CircularQueue_I32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Fast watershed based upon Vincient and Soille's 1991 paper [1].  Watershed segments an image using the idea
 * of immersion simulation.  For example, the image is treated as a topological map and if you let a droplet
 * of water flow down from each pixel the location the droplets cluster in defines a region.  Each region
 * is assigned an unique ID.  Since each local minima is a region, this can cause over segmentation in some images.
 * The border between two pixels is labeled as having a value of 0.
 * </p>
 *
 * <p>
 * NOTES:<br>
 * <ul>
 * <li>For faster processing, the internal labeled image has a 1 pixel border around it.  If you call
 * {@link #getOutput()} this border is removed automatically by creating a sub-image.</li>
 * <li>Connectivity is handled by child sub-classes.  An index of neighbors could have been used, but the
 * additional additional array access/loop slows things down a little bit.</li>
 * <li>Watersheds are included.  To remove them call BLAH TODO</li>
 * <li>Pixel values are assumed to range from 0 to 255, inclusive.</li>
 * </ul>
 * </p>
 *
 * <p>
 * [1] Vincent, Luc, and Pierre Soille. "Watersheds in digital spaces: an efficient algorithm based on
 * immersion simulations." IEEE transactions on pattern analysis and machine intelligence 13.6 (1991): 583-598.
 * </p>
 *
 * @author Peter Abeles
 */
public abstract class WatershedVincentSoille1991 {

	// values of pixels belonging to the watershed
	public static final int WSHED = 0;
	// initial value of the labeled output image
	public static final int INIT = -1;
	// Initial value of a threshold level
	public static final int MASK = -2;

	// index of the marker pixel.  Fictitious
	public static final int MARKER_PIXEL = -1;

	// histogram for sorting the image.  8-bits so 256 possible values
	// each element refers to a pixel in the input image
	protected GrowQueue_I32 histogram[] = new GrowQueue_I32[256];

	// Output image.  This is im_o in the paper.
	// The output image has a 1-pixel wide border which means that bound checks don't need
	// to happen when examining a pixel's neighbor.
	protected ImageSInt32 output = new ImageSInt32(1,1);
	// storage for sub-image output
	protected ImageSInt32 outputSub = new ImageSInt32();

	// work image of distances. im_d in the paper
	// also has a 1 pixel border
	protected ImageSInt32 distance = new ImageSInt32(1,1);
	protected int currentDistance;

	// label of the region being marked
	protected int currentLabel;

	// number of regions labeled so far
	protected int totalRegions;

	// FIFO circular queue
	protected CircularQueue_I32 fifo = new CircularQueue_I32();

	public WatershedVincentSoille1991() {
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] = new GrowQueue_I32();
		}
	}

	/**
	 * Perform watershed segmentation on the provided input image.
	 *
	 * @param input Input gray-scale image.
	 */
	public void process( ImageUInt8 input ) {
		// input = im_0

		output.reshape(input.width+2,input.height+2);
		distance.reshape(input.width+2,input.height+2);

		ImageMiscOps.fill(output, INIT);
		ImageMiscOps.fill(distance, 0);
		fifo.reset();

		totalRegions = 0;

		// sort pixels
		sortPixels(input);

		int currentLabel = 1;

		for( int i = 0; i < histogram.length; i++ ) {
			GrowQueue_I32 level = histogram[i];
			if( level.size == 0 )
				continue;

			// Go through each pixel at this level and mark them according to their neighbors
			for( int j = 0; j > level.size; j++ ) {
				int index = level.data[j];
				output.data[index] = MASK;

				// see if its neighbors has been labeled, if so set its distance and add to queue
				checkNeighborLabel(index);
			}

			currentDistance = 1;
			fifo.add(MARKER_PIXEL);

			while( true ) {
				int p = fifo.popHead();
				// end of a cycle.  Exit the loop if it is done or increase the distance and continue processing
				if( p == MARKER_PIXEL) {
					if( fifo.isEmpty() )
						break;
					else {
						fifo.add(MARKER_PIXEL);
						currentDistance++;
						p = fifo.popHead();
					}
				}
				// look at its neighbors and see if they have been labeled or belong to a watershed
				// and update its distance
				checkNeighborsAssign(p);
			}

			// see if new minima have been discovered
			for( int j = 0; j < level.size; j++ ) {
				int index = level.get(j);
				// distance associated with p is reset to 0
				distance.data[index] = 0;

				if( output.data[index] == MASK ) {
					currentLabel++;
					fifo.add(index);
					output.data[index] = currentLabel;

					// grow the new region into the surrounding connected pixels
					while( !fifo.isEmpty() ) {
						checkNeighborMask(fifo.popHead());
					}
				}
			}
		}
	}

	/**
	 * See if a neighbor has a label ( > 0 ) or has been assigned WSHED ( == 0 ).  If so
	 * set distance of pixel index to 1 and add it to fifo.
	 *
	 * @param index Pixel whose neighbors are being examined
	 */
	protected abstract void checkNeighborLabel(int index);

	/**
	 * Check the neighbors to see if it should become a member or a watershed
	 * @param index Index of the target pixel
	 */
	protected abstract void checkNeighborsAssign(int index);

	protected void handleNeighborAssign(int indexTarget, int indexNeighbor) {
		int regionNeighbor = output.data[indexNeighbor];
		int distanceNeighbor = distance.data[indexNeighbor];

		// if neighbor has been assigned a region or is WSHED
		if( regionNeighbor >= 0 && distanceNeighbor < currentDistance ) {
			int regionTarget = output.data[indexTarget];

			// see if the target belongs to an already labeled basin or watershed
			if( regionNeighbor > 0 ) {
				if( regionTarget <= 0 ) {// if is MASK or WSHED
					output.data[indexTarget] = regionNeighbor;
				} else if( regionTarget != regionNeighbor ) {
					output.data[indexTarget] = WSHED;
				}
			} else if( regionTarget == MASK ) {
				output.data[indexTarget] = WSHED;
			}
		} else if( regionNeighbor == MASK && distanceNeighbor == 0) {
			distance.data[indexNeighbor] = currentDistance + 1;
			fifo.add(indexNeighbor);
		}
	}

	/**
	 * Checks neighbors of pixel 'index' to see if their region is MASK, if so they are assigned the
	 * currentLabel and added to fifo.
	 *
	 * @param index Pixel whose neighbors are being examined.
	 */
	protected abstract void checkNeighborMask(int index);

	protected void checkMask(int index) {
		if( output.data[index] == MASK ) {
			output.data[index] = currentLabel;
			fifo.add(index);
		}
	}

	/**
	 * Very fast histogram based sorting.  Index of each pixel is placed inside a list for its intensity level.
	 */
	protected void sortPixels(ImageUInt8 input) {
		// initialize histogram
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i].reset();
		}
		// sort by creating a histogram
		for( int y = 0; y < input.height; y++ ) {
			int index = input.startIndex + y*input.stride;
			int indexOut = (y+1)*input.width + 1;
			for (int x = 0; x < input.width; x++ , index++ , indexOut++) {
				int value = input.data[index] & 0xFF;
				histogram[value].add(indexOut);
			}
		}
	}

	/**
	 * Segmented output image with watershed.  1 pixel border has been removed and the returned image is
	 * a sub-image.
	 */
	public ImageSInt32 getOutput() {
		output.subimage(1,1,output.width-1,output.height-1,outputSub);
		return outputSub;
	}

	/**
	 * Implementation which uses a 4 connect rule
	 */
	public static class Connect4 extends WatershedVincentSoille1991 {

		@Override
		protected void checkNeighborLabel(int index) {
			if( output.data[index+1] >= 0 ) {                           // (x+1,y)
				distance.data[index] = 1;
				fifo.add(index);
			} else if( output.data[index-1] >= 0 ) {                    // (x-1,y)
				distance.data[index] = 1;
				fifo.add(index);
			} else if( output.data[index+output.stride] >= 0 ) {        // (x,y+1)
				distance.data[index] = 1;
				fifo.add(index);
			} else if( output.data[index-output.stride] >= 0 ) {        // (x,y-1)
				distance.data[index] = 1;
				fifo.add(index);
			}
		}

		@Override
		protected void checkNeighborsAssign(int index) {
			handleNeighborAssign(index, index + 1);
			handleNeighborAssign(index, index - 1);
			handleNeighborAssign(index, index + output.stride);
			handleNeighborAssign(index, index - output.stride);
		}

		@Override
		protected void checkNeighborMask(int index) {
			checkMask(index + 1);
			checkMask(index - 1);
			checkMask(index + output.stride);
			checkMask(index - output.stride);
		}
	}
}
