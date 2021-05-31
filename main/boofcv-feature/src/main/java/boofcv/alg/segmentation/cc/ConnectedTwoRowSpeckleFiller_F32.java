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

import boofcv.errors.BoofCheckFailure;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;

import static boofcv.misc.BoofMiscOps.checkTrue;

/**
 * Implementation of {@link ConnectedTwoRowSpeckleFiller} for {@link GrayU8}.
 *
 * @author Peter Abeles
 */
public class ConnectedTwoRowSpeckleFiller_F32 extends ConnectedTwoRowSpeckleFiller<GrayF32> {

	// List of pixels which have yet to be examined in the search. Encoded by y*width + x
	final DogArray<OpenPixel> open = new DogArray<>(OpenPixel::new);

	// Copy parameters into class fields to make function arguments less verbose
	float fillValue;
	float similarTol;

	@Override protected void initTypeSpecific( double similarTol, double fillValue ) {
		this.similarTol = (float)similarTol;
		this.fillValue = (float)fillValue;
	}

	@Override protected int labelRow( int idx0, int[] labels, int[] labelCount, int[] locationX ) {
		return labelRow(image.data, idx0, image.width, labels, labelCount, locationX, fillValue, similarTol);
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
	 */
	@Override
	protected void findConnectionsBetweenRows( final int startRowA, final int startRowB ) {
		final float[] pixels = image.data;
		final int width = image.width;
		int idxRowA = startRowA;
		int idxRowB = startRowB;

		// Initially nothing is merged together or connected
		merge.resetResize(countsB.size, -1);
		connectAtoB.resetResize(countsA.size, -1);

		// Check for connectivity one column at a time
		for (int col = 0; col < width; col++, idxRowA++, idxRowB++) {
			float valueA = pixels[idxRowA];
			float valueB = pixels[idxRowB];
			// don't connect if one of them is equal to the fill value
			if (valueA == fillValue || valueB == fillValue)
				continue;

			// See if these two pixels are connected. image[y,x] and image[y+1,x]
			if (Math.abs(valueA - valueB) > similarTol)
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
	 * Fill cluster by performing a search of connected pixels. This step can be slow and a memory hog
	 * if the regions are large. It's also effectively the naive algorithm
	 */
	@Override
	protected void fillCluster( int seedX, int seedY, int clusterSize ) {
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
			checkAndConnect(x + 1, y, value, similarTol);
			checkAndConnect(x, y + 1, value, similarTol);
			checkAndConnect(x - 1, y, value, similarTol);
			checkAndConnect(x, y - 1, value, similarTol);
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
			throw new RuntimeException("Bad value: " + value);

		if (value == fillValue || Math.abs(value - targetValue) > tol)
			return;

		// Add it to the open list so that it's neighbors will be searched
		open.grow().setTo(x, y, value);

		// Fill it in now. This also prevents it from being added twice
		image.unsafe_set(x, y, fillValue);
	}

	@Override public ImageType<GrayF32> getImageType() {
		return ImageType.SB_F32;
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
