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

package boofcv.alg.segmentation.watershed;

import boofcv.alg.InputSanityCheck;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import org.ddogleg.struct.CircularQueue_I32;
import org.ddogleg.struct.GrowQueue_I32;

/**
 * <p>
 * Fast watershed based upon Vincient and Soille's 1991 paper [1].  Watershed segments an image using the idea
 * of immersion simulation.  For example, the image is treated as a topological map and if you let a droplet
 * of water flow down from each pixel the location the droplets cluster in defines a region.  Two different
 * methods are provided for processing the image, a new region is created at each local minima or the user
 * provides an initial seed for each region for it to grow from.  The output will be a segmented image
 * with watersheds being assign a value of 0 and each region a value &gt; 0.  Watersheds are assigned to pixels
 * which are exactly the same distance from multiple regions, thus it is ambiguous which one it is a member of.
 * </p>
 *
 * <p>
 * If the image is processed with {@link #process(GrayU8)} then a new region is
 * created at each local minima and assigned a unique ID &gt; 0. The total number of regions found is returned
 * by {@link #getTotalRegions()}.  This technique will lead to over segmentation on many images.
 * </p>
 *
 * <p>
 * Initial seeds are provided with a call to {@link #process(GrayU8, GrayS32)}.
 * No new regions will be created.  By providing an initial set of seeds over segmentation can be avoided, but
 * prior knowledge of the image is typically needed to create the seeds.
 * </p>
 *
 * <p>
 * NOTES:<br>
 * <ul>
 * <li>For faster processing, the internal labeled image has a 1 pixel border around it.  If you call
 * {@link #getOutput()} this border is removed automatically by creating a sub-image.</li>
 * <li>Connectivity is handled by child sub-classes.  An index of neighbors could have been used, but the
 * additional additional array access/loop slows things down a little bit.</li>
 * <li>Watersheds are included.  To remove them using {@link RemoveWatersheds}</li>
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
	protected GrayS32 output = new GrayS32(1,1);
	// storage for sub-image output
	protected GrayS32 outputSub = new GrayS32();

	// work image of distances. im_d in the paper
	// also has a 1 pixel border
	protected GrayS32 distance = new GrayS32(1,1);
	protected int currentDistance;

	// label of the region being marked
	protected int currentLabel;

	// FIFO circular queue
	protected CircularQueue_I32 fifo = new CircularQueue_I32();

	// used to remove watersheds
	protected RemoveWatersheds removeWatersheds = new RemoveWatersheds();
	boolean removedWatersheds;

	public WatershedVincentSoille1991() {
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i] = new GrowQueue_I32();
		}
	}

	/**
	 * Perform watershed segmentation on the provided input image.  New basins are created at each local minima.
	 *
	 * @param input Input gray-scale image.
	 */
	public void process( GrayU8 input ) {
		// input = im_0

		removedWatersheds = false;
		output.reshape(input.width+2,input.height+2);
		distance.reshape(input.width+2,input.height+2);

		ImageMiscOps.fill(output, INIT);
		ImageMiscOps.fill(distance, 0);
		fifo.reset();

		// sort pixels
		sortPixels(input);

		currentLabel = 0;

		for( int i = 0; i < histogram.length; i++ ) {
			GrowQueue_I32 level = histogram[i];
			if( level.size == 0 )
				continue;

			// Go through each pixel at this level and mark them according to their neighbors
			for( int j = 0; j < level.size; j++ ) {
				int index = level.data[j];
				output.data[index] = MASK;

				// see if its neighbors has been labeled, if so set its distance and add to queue
				assignNewToNeighbors(index);
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
						checkNeighborsMasks(fifo.popHead());
					}
				}
			}
		}
	}

	/**
	 * <p>
	 * Segments the image using initial seeds for each region.  This is often done to avoid
	 * over segmentation but requires additional preprocessing and/or knowledge on the image structure.  Initial
	 * seeds are specified in the input image 'seeds'.  A seed is any pixel with a value &gt; 0.  New new regions
	 * will be created beyond those seeds.  The final segmented image is provided by {@link #getOutput()}.
	 * </p>
	 *
	 * <p>
	 * NOTE: If seeds are used then {@link #getTotalRegions()} will not return a correct solution.
	 * </p>
	 *
	 * @param input (Input) Input image
	 * @param seeds (Output) Segmented image containing seeds.  Note that all seeds should have a value &gt; 0 and have a
	 *              value &le; numRegions.
	 */
	public void process(GrayU8 input , GrayS32 seeds ) {
		InputSanityCheck.checkSameShape(input,seeds);

		removedWatersheds = false;
		output.reshape(input.width+2,input.height+2);
		distance.reshape(input.width+2,input.height+2);

		ImageMiscOps.fill(output, INIT);
		ImageMiscOps.fill(distance, 0);
		fifo.reset();

		// copy the seeds into the output directory
		for( int y = 0; y < seeds.height; y++ ) {
			int indexSeeds = seeds.startIndex + y*seeds.stride;
			int indexOut = (y+1)*output.stride + 1;
			for( int x = 0; x < seeds.width; x++ , indexSeeds++, indexOut++ ) {
				int v = seeds.data[indexSeeds];
				if( v > 0 ) {
					output.data[indexOut] = v;
				}
			}
		}

		// sort pixels
		sortPixels(input);

		// perform watershed
		for( int i = 0; i < histogram.length; i++ ) {
			GrowQueue_I32 level = histogram[i];
			if( level.size == 0 )
				continue;

			// Go through each pixel at this level and mark them according to their neighbors
			for( int j = 0; j < level.size; j++ ) {
				int index = level.data[j];

				// If not has not already been labeled by a seed then try assigning it values
				// from its neighbors
				if( output.data[index] == INIT ) {
					output.data[index] = MASK;
					assignNewToNeighbors(index);
				}
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

			// Ensure that all pixels have a distance of zero
			// Could probably do this a bit more intelligently...
			ImageMiscOps.fill(distance, 0);
		}
	}

	/**
	 * See if a neighbor has a label ( &gt; 0 ) or has been assigned WSHED ( == 0 ).  If so
	 * set distance of pixel index to 1 and add it to fifo.
	 *
	 * @param index Pixel whose neighbors are being examined
	 */
	protected abstract void assignNewToNeighbors(int index);

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
				if( regionTarget < 0 ) {// if is MASK
					output.data[indexTarget] = regionNeighbor;
				} else if( regionTarget == 0 ) {
					// if it is a watershed only assign to the neighbor value if it would be closer
					// this is a deviation from what's in the paper.  There might be a type-o there or I miss read it
					if( distanceNeighbor+1 < currentDistance  ) {
						output.data[indexTarget] = regionNeighbor;
					}
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
	protected abstract void checkNeighborsMasks(int index);

	protected void checkMask(int index) {
		if( output.data[index] == MASK ) {
			output.data[index] = currentLabel;
			fifo.add(index);
		}
	}

	/**
	 * Very fast histogram based sorting.  Index of each pixel is placed inside a list for its intensity level.
	 */
	protected void sortPixels(GrayU8 input) {
		// initialize histogram
		for( int i = 0; i < histogram.length; i++ ) {
			histogram[i].reset();
		}
		// sort by creating a histogram
		for( int y = 0; y < input.height; y++ ) {
			int index = input.startIndex + y*input.stride;
			int indexOut = (y+1)*output.stride + 1;
			for (int x = 0; x < input.width; x++ , index++ , indexOut++) {
				int value = input.data[index] & 0xFF;
				histogram[value].add(indexOut);
			}
		}
	}

	/**
	 * Segmented output image with watersheds.  This is a sub-image of {@link #getOutputBorder()} to remove
	 * the outside border of -1 valued pixels.
	 */
	public GrayS32 getOutput() {
		output.subimage(1,1,output.width-1,output.height-1,outputSub);
		return outputSub;
	}

	/**
	 * The entire segmented image used internally.  This contains a 1-pixel border around the entire
	 * image filled with pixels of value -1.
	 */
	public GrayS32 getOutputBorder() {
		return output;
	}

	/**
	 * Removes watershed pixels from the output image by merging them into an arbitrary neighbor.
	 */
	public void removeWatersheds() {
		removedWatersheds = true;
		removeWatersheds.remove(output);
	}

	/**
	 * Returns the total number of regions labeled.  If watersheds have not
	 * been removed then this will including the watershed.
	 *
	 * <p>THIS IS NOT VALID IF SEEDS ARE USED!!!</p>
	 *
	 * @return number of regions.
	 */
	public int getTotalRegions() {
		return removedWatersheds ? currentLabel : currentLabel + 1;
	}

	/**
	 * Implementation which uses a 4-connect rule
	 */
	public static class Connect4 extends WatershedVincentSoille1991 {

		@Override
		protected void assignNewToNeighbors(int index) {
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
		protected void checkNeighborsMasks(int index) {
			checkMask(index + 1);
			checkMask(index - 1);
			checkMask(index + output.stride);
			checkMask(index - output.stride);
		}
	}

	/**
	 * Implementation which uses a 8-connect rule
	 */
	public static class Connect8 extends WatershedVincentSoille1991 {

		@Override
		protected void assignNewToNeighbors(int index) {
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
			} else if( output.data[index+1+output.stride] >= 0 ) {      // (x+1,y+1)
				distance.data[index] = 1;
				fifo.add(index);
			} else if( output.data[index-1+output.stride] >= 0 ) {      // (x-1,y+1)
				distance.data[index] = 1;
				fifo.add(index);
			} else if( output.data[index+1-output.stride] >= 0 ) {      // (x+1,y-1)
				distance.data[index] = 1;
				fifo.add(index);
			} else if( output.data[index-1-output.stride] >= 0 ) {      // (x-1,y-1)
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

			handleNeighborAssign(index, index + 1 + output.stride);
			handleNeighborAssign(index, index - 1 + output.stride);
			handleNeighborAssign(index, index + 1 - output.stride);
			handleNeighborAssign(index, index - 1 - output.stride);
		}

		@Override
		protected void checkNeighborsMasks(int index) {
			checkMask(index + 1);
			checkMask(index - 1);
			checkMask(index + output.stride);
			checkMask(index - output.stride);

			checkMask(index + 1 + output.stride);
			checkMask(index - 1 + output.stride);
			checkMask(index + 1 - output.stride);
			checkMask(index - 1 - output.stride);
		}
	}
}
