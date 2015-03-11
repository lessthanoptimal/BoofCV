/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.learning;

import java.util.Arrays;

/**
 * Used create a histogram of actual to predicted classification.  This will be a NxN matrix.  This can
 * then be converted into a confusion matrix.  Rows are actual type and columns is the predicted type.
 *
 * @author Peter Abeles
 */
public class ClassificationHistogram {
	int results[];
	int numTypes;

	public ClassificationHistogram(int numTypes) {
		results = new int[ numTypes*numTypes ];
		this.numTypes = numTypes;
	}

	public void reset() {
		Arrays.fill(results,0);
	}

	public void increment( int actual , int predicted ) {
		results[actual*numTypes + predicted]++;
	}

	public Confusion createConfusion() {
		Confusion confusion = new Confusion(numTypes);

		for (int i = 0; i < numTypes; i++) {
			int totalActual = 0;
			for (int j = 0; j < numTypes; j++) {
				totalActual += get(i,j);
			}
			confusion.actualCounts[i] = totalActual;
			for (int j = 0; j < numTypes; j++) {
				double fraction = get(i,j)/(double)totalActual;
				confusion.matrix.set(i, j, fraction);
			}
		}

		return confusion;
	}

	public int get( int actual , int predicted ) {
		return results[actual*numTypes + predicted];
	}
}
