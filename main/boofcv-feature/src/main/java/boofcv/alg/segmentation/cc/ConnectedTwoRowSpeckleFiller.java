/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.image.ImageGray;
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
@SuppressWarnings({"NullAway.Init"})
public abstract class ConnectedTwoRowSpeckleFiller<T extends ImageGray<T>> implements ConnectedSpeckleFiller<T> {

	/** Number of clusters that were filed in */
	protected int totalFilled;

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

	// Copy parameters into class fields to make function arguments less verbose
	T image;

	/**
	 * Finds non-smooth regions and fills them in with the fill value. Uses 4-connect rule.
	 */
	@Override
	public void process( final T image, int maximumArea, double _similarTol, double _fillValue ) {
		// Initialize data structures and save internal references
		initTypeSpecific(_similarTol, _fillValue);
		init(image);

		// Find connectivity in the first row A
		countsA.size = labelRow(image.startIndex, labelsA.data, countsA.data, pixXinA.data);

		// Go through the remaining rows
		for (int y = 1; y < image.height; y++) {
			int startRowB = image.startIndex + y*image.stride;
			int startRowA = startRowB - image.stride;

			// Find connectivity in the second row B
			countsB.size = labelRow(startRowB, labelsB.data, countsB.data, pixXinB.data);

			// Finds regions which are connected between the two rows
			findConnectionsBetweenRows(startRowA, startRowB);
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
				fillCluster(pixXinA.get(labelA), y - 1, count);
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
			fillCluster(pixXinA.get(labelA), image.height - 1, count);
		}
	}

	protected abstract void initTypeSpecific( double similarTol, double fillValue );

	private void init( T image ) {
		this.image = image;
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
	 * @param labels Array that stores labels
	 * @param labelCount Array that stores label counts
	 * @return number of clusters
	 */
	protected abstract int labelRow( int idx0, final int[] labels, int[] labelCount, int[] locationX );

	/**
	 * Compres pxiel values between the two rows to find the mapping between the regions. This is also where
	 * "finished" regions in A are identified.
	 *
	 * @param startRowA Index of row in image array
	 * @param startRowB Index of row in image array
	 */
	protected abstract void findConnectionsBetweenRows( int startRowA, int startRowB );

	/**
	 * Traverses the graph until it hits an end point then and returns the end point/root.
	 */
	protected final int traverseToEnd( int labelB ) {
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
	protected abstract void fillCluster( int seedX, int seedY, int clusterSize );

	@Override public int getTotalFilled() {
		return totalFilled;
	}
}
