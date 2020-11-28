/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.cc;

import boofcv.errors.BoofCheckFailure;
import boofcv.struct.image.GrayF32;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.ddogleg.struct.DogArray_I32;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Searches for small clusters (or blobs) of connected pixels and then fills them in with the specified fill color. Two
 * pixels are considered to be connected if their intensity value is within the specified tolerance of each other.
 * No pixel can be connected to a pixel with the value of a fill color. A connect-4 rule is used to
 * determine the local neighborhood of a pixel. This algorithm has been optimized for
 * speed and low memory. Memory usage is approximately O(W) where W is the width of the image and has an
 * approximate runtime of O(W*H). As long as the fill in regions are small this approximation will be accurate.
 *
 * The algorithm works by considering two rows at a time. Each row is initially processed independently and sets
 * of connected pixels are found. Then the connectivity is found between the first row to the second row. A merge
 * directed graph is built to handle potential many to one mappings from regions in the first row to the second row.
 * Once this is complete the second row is updated by merging and by adding the region size counts.
 *
 * Any region in the first row with no match in the second row is declared complete as it's impossible
 * to grow any more. When a region is complete the number of pixels in it is then considered. if it's less
 * than the threshold it will be filled in by doing a depth first search starting from a pixel in the first row.
 *
 * See BoofCV Tech Report BLAH.
 *
 * @author Peter Abeles
 */
public class ConnectedTwoRowSpeckleFiller {

	/** Number of clusters that were filed in */
	private @Getter int totalFilled;

	// Labels for each pixel in a row
	DogArray_I32 labelsA = new DogArray_I32();
	DogArray_I32 labelsB = new DogArray_I32();
	// The first time a cluster was seen in a row. x-coordinate.
	// Used when a region needs to be filled in
	DogArray_I32 pixXinA = new DogArray_I32();
	DogArray_I32 pixXinB = new DogArray_I32();
	// Number of times a cluster was seen. For A it will be in total. For B it will be for the row
	DogArray_I32 countsA = new DogArray_I32();
	DogArray_I32 countsB = new DogArray_I32();

	// Which clusters from A are connected to which clusters in B
	final DogArray_I32 connectAtoB = new DogArray_I32();
	// Specifies which clusters in B should be merged into each other. Directed tree.
	final DogArray_I32 merge = new DogArray_I32();

	// List of clusters in A which are no longer visible and can't grow
	final DogArray_I32 finished = new DogArray_I32();
	// List of pixels which have yet to be examined in the search. Encoded by y*width + x
	final DogArray<OpenPixel> open = new DogArray<>(OpenPixel::new);

	// Copy parameters into class fields to make function arguments less verbose
	GrayF32 image;
	float fillValue;

	/**
	 * Finds non-smooth regions and fills them in with the fill value. Uses 4-connect rule.
	 *
	 * @param image (Input, Output) Image which is searched for speckle noise which is then filled in
	 * @param maximumArea (Input) All regions with this number of pixels or fewer will be filled in.
	 * @param similarTol (Input) Two pixels are connected if their different in value is &le; than this.
	 * @param fillValue (Input) The value that small regions are filled in with.
	 */
	public void process( final GrayF32 image, int maximumArea, float similarTol, float fillValue ) {
		// Initialize data structures and save internal references
		init(image, fillValue);

		// Find connectivity in the first row A
		countsA.size = labelRow(image.data, image.startIndex, image.width,
				labelsA.data, countsA.data, pixXinA.data, fillValue, similarTol);

		// Go through the remaining rows
		for (int y = 1; y < image.height; y++) {
			int startRowB = image.startIndex + y*image.stride;
			int startRowA = startRowB - image.stride;

			// Find connectivity in the second row B
			countsB.size = labelRow(image.data, startRowB, image.width,
					labelsB.data, countsB.data, pixXinB.data, fillValue, similarTol);

			// Finds regions which are connected between the two rows
			findConnectionsBetweenRows(startRowA, startRowB, similarTol);
			// Merge rows in B together
			mergeClustersInB();
			// Update the region size counts in B
			addCountsRowAIntoB();

			// For any region which can't grow any more, see if it should be filled in. If it should then do it
			for (int idxClosed = 0; idxClosed < finished.size; idxClosed++) {
				int labelA = finished.get(idxClosed);
				int count = countsA.get(labelA);
				checkTrue(count > 0, "BUG! a merged cluster was added");

				// If it's too big continue
				if (count > maximumArea)
					continue;

				// Fill in the row
				fillCluster(pixXinA.get(labelA), y - 1, count, similarTol);
			}

			// swap references to avoid a copy A and B
			DogArray_I32 tmp;
			tmp = labelsA;
			labelsA = labelsB;
			labelsB = tmp;
			tmp = countsA;
			countsA = countsB;
			countsB = tmp;
			tmp = pixXinA;
			pixXinA = pixXinB;
			pixXinB = tmp;
		}

		// Last row is a special case
		for (int labelA = 0; labelA < countsA.size; labelA++) {
			int count = countsA.data[labelA];
			if (count == 0 || count > maximumArea)
				continue;
			fillCluster(pixXinA.get(labelA), image.height - 1, count, similarTol);
		}
	}

	private void init( GrayF32 image, float fillValue ) {
		this.image = image;
		this.fillValue = fillValue;
		this.totalFilled = 0;

		labelsA.resize(image.width);
		labelsB.resize(image.width);
		pixXinA.resize(image.width);
		pixXinB.resize(image.width);
		countsA.reserve(image.width);
		countsB.reserve(image.width);
		connectAtoB.reserve(image.width);
		merge.resize(image.width);
	}

	/**
	 * Applies connectivity rule along a single row in 1D
	 *
	 * @param pixels array with image pixels
	 * @param idx0 start of row (inclusive)
	 * @param width Number of elements in the row.
	 * @param labels Array that stores labels
	 * @param labelCount Array that stores label counts
	 * @param tol pixel similarity tolerance
	 * @return number of clusters
	 */
	static int labelRow( final float[] pixels, final int idx0, final int width,
						 final int[] labels, int[] labelCount, int[] locationX,
						 final float fillValue,
						 final float tol ) {
		// compute the index it will end at
		final int idx1 = idx0 + width;

		// initialize the first cluster
		int currentLabel;
		if (pixels[idx0] == fillValue) {
			// fillValue pixels can't be the start of a cluster
			currentLabel = -1;
		} else {
			currentLabel = 0;
			labelCount[currentLabel] = 1;
			locationX[currentLabel] = 0;
		}
		labels[0] = currentLabel;

		for (int i = idx0, j = idx0 + 1; j < idx1; i = j, j++) {
			int col = j - idx0;
			// See if these two pixels are connected. image[y,x] and image[y,x+1]
			float pixel_i = pixels[i];
			float pixel_j = pixels[j];
			// can't connect to any pixel with fillValue
			if (pixel_i != fillValue && pixel_j != fillValue && Math.abs(pixel_i - pixel_j) <= tol) {
				// increment the total pixels in this cluster
				labelCount[currentLabel]++;
				labels[col] = currentLabel;
			} else {
				// don't create new clusters for fillValue pixels since there can be no cluster here
				if (pixel_j == fillValue) {
					labels[col] = -1;
				} else {
					// Initialize the new cluster
					labelCount[++currentLabel] = 1;
					locationX[currentLabel] = col;
					labels[col] = currentLabel;
				}
			}
		}
		return currentLabel + 1;
	}

	/**
	 * Compres pxiel values between the two rows to find the mapping between the regions. This is also where
	 * "finished" regions in A are identified.
	 *
	 * @param startRowA Index of row in image array
	 * @param startRowB Index of row in image array
	 * @param tol Pixel similarity tolerance
	 */
	final void findConnectionsBetweenRows( final int startRowA,
										   final int startRowB,
										   final float tol ) {
		final float[] pixels = image.data;
		final int width = image.width;
		int idxRowA = startRowA;
		int idxRowB = startRowB;

		// Initially nothing is merged together or connected
		merge.resize(countsB.size, -1);
		connectAtoB.resize(countsA.size, -1);

		// Check for connectivity one column at a time
		for (int col = 0; col < width; col++, idxRowA++, idxRowB++) {
			float valueA = pixels[idxRowA];
			float valueB = pixels[idxRowB];
			// don't connect if one of them is equal to the fill value
			if (valueA == fillValue || valueB == fillValue)
				continue;

			// See if these two pixels are connected. image[y,x] and image[y+1,x]
			if (Math.abs(valueA - valueB) > tol)
				continue;

			int labelA = labelsA.get(col);
			int labelB = labelsB.get(col);

			// Look up the corresponding cluster in B
			int whatAinB = connectAtoB.data[labelA];

			if (whatAinB == -1) {
				// This label does not have a mapping already. So assign it one
				connectAtoB.data[labelA] = traverseToEnd(labelB);
				continue;
			} else if (whatAinB == labelB) {
				// It has a mapping but it's the same as what this pixel points too.
				continue;
			}

			// Traverse to the end points from both nodes. If they are not already connected, connect them
			int target1 = traverseToEnd(labelB);
			int target2 = traverseToEnd(whatAinB);
			if (target1 != target2) {
				merge.data[target1] = target2;
			}
		}
	}

	/**
	 * Traverses the graph until it hits an end point then and returns the end point/root.
	 */
	private int traverseToEnd( int labelB ) {
		int targetB = labelB;
		while (merge.data[targetB] != -1) {
			targetB = merge.data[targetB];
		}
		return targetB;
	}

	/**
	 * Examine the merging graph and merge together clusters in row B
	 */
	final void mergeClustersInB() {
		// Update the labels with merge info for the next iteration
		for (int i = 0; i < labelsB.size; i++) {
			int label = labelsB.data[i];
			if (label == -1 || merge.data[label] == -1) // See if this cluster got merged
				continue;
			labelsB.data[i] = traverseToEnd(label);
		}

		// Update the counts for each cluster
		for (int label = 0; label < countsB.size; label++) {
			if (merge.data[label] == -1) // See if this cluster got merged
				continue;
			int target = traverseToEnd(label);

			// Add the point count from the merged nodes
			countsB.data[target] += countsB.data[label];
			// zero it out so that when the next line is processed we know this has been merged
			countsB.data[label] = 0;
		}
	}

	/**
	 * Add the counts from rows in A into matching regions in B. Also identify regions in A which are complete.
	 */
	final void addCountsRowAIntoB() {
		finished.reset();
		for (int labelA = 0; labelA < countsA.size; labelA++) {
			int labelB = connectAtoB.data[labelA];

			if (labelB == -1) {
				// No match was found in row 1. If that cluster has not been merged into another then we should
				// add it to the closed list. Otherwise just ignore it
				if (countsA.data[labelA] > 0)
					finished.add(labelA);
				continue;
			}

			// Handle the case if this cluster got merged
			labelB = traverseToEnd(labelB);

			// Add all the points into the equivalent cluster
			countsB.data[labelB] += countsA.data[labelA];
		}
	}

	/**
	 * Fill cluster by performing a search of connected pixels. This step can be slow and a memory hog
	 * if the regions are large. It's also effectively the naive algorithm
	 */
	void fillCluster( int seedX, int seedY, int clusterSize, final float tol ) {
		float seedValue = image.unsafe_get(seedX, seedY);
		checkTrue(seedValue != fillValue, "BUG! Shouldn't have gotten this far");

		totalFilled++;
		image.unsafe_set(seedX, seedY, fillValue);

		open.reset();
		open.grow().setTo(seedX, seedY, seedValue);

		int foundSize = 0;

		while (!open.isEmpty()) {
			foundSize++;
			OpenPixel c = open.removeSwap(0);

			// create a copy because 'c' just got recycled and might be modified by the functions below!
			final int x = c.x;
			final int y = c.y;
			final float value = c.value;

			// check 4-connect neighborhood for pixels which are connected and add them to the open list
			// while marking them as visited
			checkAndConnect(x + 1, y, value, tol);
			checkAndConnect(x, y + 1, value, tol);
			checkAndConnect(x - 1, y, value, tol);
			checkAndConnect(x, y - 1, value, tol);
		}

		// Sanity check for bugs
		if (clusterSize != foundSize)
			throw new BoofCheckFailure("BUG! Fill does not match cluster size. Expected=" +
					clusterSize + " Found=" + foundSize);
	}

	/**
	 * Checks to see if the pixel at the specified coordinate could be connected to another pixel with the specified
	 * value. If so it's added to the open list and filled in
	 */
	void checkAndConnect( final int x, final int y, float targetValue, final float tol ) {
		if (!image.isInBounds(x, y))
			return;

		float value = image.unsafe_get(x, y);

		if (Float.isInfinite(value) || Float.isNaN(value))
			throw new RuntimeException("BAd value");

		if (value == fillValue || Math.abs(value - targetValue) > tol)
			return;

		// Add it to the open list so that it's neighbors will be searched
		open.grow().setTo(x, y, value);

		// Fill it in now. This also prevents it from being added twice
		image.unsafe_set(x, y, fillValue);
	}

	/** Information for a pixel that's in the open list when filling in a region */
	private static class OpenPixel {
		int x;
		int y;
		float value; // value of the pixel before it got filled in

		public void setTo( int x, int y, float value ) {
			this.x = x;
			this.y = y;
			this.value = value;
		}
	}
}
