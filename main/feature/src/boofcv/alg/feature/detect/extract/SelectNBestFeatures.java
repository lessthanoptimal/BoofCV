/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageFloat32;
import georegression.struct.point.Point2D_I16;
import pja.sorting.QuickSelectArray;


/**
 * This extractor selects N best results from the extractor that it is wrapped around.
 *
 * @author Peter Abeles
 */
public class SelectNBestFeatures {

	// list of the found best corners
	QueueCorner bestCorners;
	int indexes[] = new int[1];
	float inten[] = new float[1];

	int N;

	public SelectNBestFeatures(int maxCorners) {
		bestCorners = new QueueCorner(maxCorners);
		setN(maxCorners);
	}

	public void setN( int N ) {
		if( N > indexes.length ) {
			indexes = new int[N];
			inten = new float[N];
		}
		this.N = N;

	}

	public void process(ImageFloat32 intensityImage, QueueCorner origCorners) {
		final int numFoundFeatures = origCorners.size;
		bestCorners.reset();

		if (numFoundFeatures <= N) {
			// make a copy of the results with no pruning since it already
			// has the desired number, or less
			for (int i = 0; i < numFoundFeatures; i++) {
				Point2D_I16 pt = origCorners.data[i];
				bestCorners.add(pt.x, pt.y);
			}
		} else {
			// prune the list after finding the N best using quick select

			// grow internal data structures
			if (numFoundFeatures > inten.length) {
				inten = new float[numFoundFeatures];
				indexes = new int[numFoundFeatures];
			}

			// extract the intensities for each corner
			Point2D_I16[] points = origCorners.data;
			int size = origCorners.size();

			for (int i = 0; i < size; i++) {
				Point2D_I16 pt = points[i];
				// quick select selects the k smallest
				// I want the k-biggest so the negative is used
				inten[i] = -intensityImage.get(pt.getX(), pt.getY());

			}

			QuickSelectArray.selectIndex(inten, N, size, indexes);

			for (int i = 0; i < N; i++) {
				Point2D_I16 pt = origCorners.data[indexes[i]];
				bestCorners.add(pt.x, pt.y);
			}
		}
	}

	public QueueCorner getBestCorners() {
		return bestCorners;
	}
}
