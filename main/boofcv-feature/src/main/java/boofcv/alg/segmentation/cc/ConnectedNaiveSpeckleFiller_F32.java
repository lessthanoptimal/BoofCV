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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.ImageType;
import org.ddogleg.struct.DogArray;

/**
 * Naive implementation of connected-component based speckle filler. A labeled image is created for each blob. When
 * a new region is encountered it is filled by growing a region. The grow region algorithm works by having an open
 * list of unexplored pixels. The top of the list is selected and its neighbors checked to see they are also members,
 * if so they are added to the open list.
 *
 * @author Peter Abeles
 */
public class ConnectedNaiveSpeckleFiller_F32 implements ConnectedSpeckleFiller<GrayF32> {
	GrayS32 labels = new GrayS32(1, 1);
	DogArray<Pixel> open = new DogArray<>(Pixel::new);

	int totalFilled;
	int totalRegions;
	float similarTolerance;
	float fillValue;

	@Override
	public void process( final GrayF32 image, int maximumArea, double similarTol, double fillValue ) {
		this.similarTolerance = (float)similarTol;
		this.fillValue = (float)fillValue;
		this.totalFilled = 0;

		labels.reshape(image);
		ImageMiscOps.fill(labels, -1);
		totalRegions = 0;

		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				// See if this location has been explored already
				int label = labels.unsafe_get(x, y);
				if (label != -1)
					continue;

				// Don't try to connect regions which have the fill value
				float value = image.unsafe_get(x, y);
				if (value == fillValue)
					continue;

				// See if the region should be filled in
				if (applyLabel(image, x, y, value) > maximumArea)
					continue;

				// Fill in the region
				for (int i = 0; i < open.size; i++) {
					Pixel p = open.get(i);
					image.unsafe_set(p.x, p.y, this.fillValue);
				}
				totalFilled++;
			}
		}
	}

	@Override public int getTotalFilled() {
		return totalFilled;
	}

	@Override public ImageType<GrayF32> getImageType() {
		return ImageType.SB_F32;
	}

	/**
	 * Applies the label to all connected pixels using the specified seed
	 *
	 * @return total number of pixels in this region
	 */
	int applyLabel( GrayF32 image, int seedX, int seedY, float seedValue ) {
		int label = totalRegions++;
		open.reset();
		open.grow().setTo(seedX, seedY, seedValue);
		labels.unsafe_set(seedX, seedY, label);

		int location = 0;
		while (location < open.size()) {
			Pixel p = open.get(location);
			int x = p.x;
			int y = p.y;
			float value = p.value;

			// connect-4 rule for neighborhood
			checkAdd(image, x + 1, y, label, value);
			checkAdd(image, x, y + 1, label, value);
			checkAdd(image, x - 1, y, label, value);
			checkAdd(image, x, y - 1, label, value);

			location++;
		}

		return location;
	}

	private void checkAdd( GrayF32 image, int x, int y, int label, float targetValue ) {
		if (!labels.isInBounds(x, y))
			return;

		if (labels.unsafe_get(x, y) != -1)
			return;

		float value = image.unsafe_get(x, y);
		if (value == fillValue || Math.abs(targetValue - value) > similarTolerance)
			return;

		labels.unsafe_set(x, y, label);
		open.grow().setTo(x, y, value);
	}

	public static class Pixel {
		int x, y;
		float value;

		public void setTo( int x, int y, float value ) {
			this.x = x;
			this.y = y;
			this.value = value;
		}
	}
}
