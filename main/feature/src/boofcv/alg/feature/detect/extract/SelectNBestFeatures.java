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

package boofcv.alg.feature.detect.extract;

import boofcv.struct.QueueCorner;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.sorting.QuickSelect;


/**
 * Selects and sorts up to the N best features based on their intensity.
 *
 * @author Peter Abeles
 */
public class SelectNBestFeatures {

	// list of the found best corners
	QueueCorner bestCorners;
	int indexes[] = new int[1];
	float inten[] = new float[1];

	// number of features it should return
	int target;

	public SelectNBestFeatures(int N) {
		bestCorners = new QueueCorner(N);
		setN(N);
	}

	public void setN( int N ) {
		target = N;
	}

	public void process(GrayF32 intensityImage, QueueCorner origCorners, boolean positive ) {
		bestCorners.reset();

		if (origCorners.size <= target) {
			// make a copy of the results with no pruning since it already
			// has the desired number, or less
			for (int i = 0; i < origCorners.size; i++) {
				Point2D_I16 pt = origCorners.data[i];
				bestCorners.add(pt.x, pt.y);
			}
		} else {

			// grow internal data structures
			if( origCorners.size > indexes.length ) {
				indexes = new int[origCorners.size];
				inten = new float[origCorners.size];
			}

			// extract the intensities for each corner
			Point2D_I16[] points = origCorners.data;

			if( positive ) {
				for (int i = 0; i < origCorners.size; i++) {
					Point2D_I16 pt = points[i];
					// quick select selects the k smallest
					// I want the k-biggest so the negative is used
					inten[i] = -intensityImage.get(pt.getX(), pt.getY());
				}
			} else {
				for (int i = 0; i < origCorners.size; i++) {
					Point2D_I16 pt = points[i];
					inten[i] = intensityImage.get(pt.getX(), pt.getY());
				}
			}

			QuickSelect.selectIndex(inten,target,origCorners.size,indexes);

			for (int i = 0; i < target; i++) {
				Point2D_I16 pt = origCorners.data[indexes[i]];
				bestCorners.add(pt.x, pt.y);
			}
		}
	}

	public QueueCorner getBestCorners() {
		return bestCorners;
	}
}
