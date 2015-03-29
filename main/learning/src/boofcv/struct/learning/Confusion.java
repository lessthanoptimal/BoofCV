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

import org.ejml.data.DenseMatrix64F;

/**
 * Storage for a confusion matrix.  Rows represent the actual type and the columns the predicted type.  All the rows
 * sum up to 1.
 *
 * @author Peter Abeles
 */
public class Confusion {
	DenseMatrix64F matrix;
	int actualCounts[];

	public Confusion( int numTypes) {
		matrix = new DenseMatrix64F(numTypes,numTypes);
		actualCounts = new int[numTypes];
	}

	public DenseMatrix64F getMatrix() {
		return matrix;
	}

	/**
	 * Computes accuracy from the confusion matrix.  This is the sum of the fraction correct divide by total number
	 * of types.  The number of each sample for each type is not taken in account.
	 *
	 * @return overall accuracy
	 */
	public double computeAccuracy() {
		double totalCorrect = 0;
		double totalIncorrect = 0;

		for (int i = 0; i < actualCounts.length; i++) {
			for (int j = 0; j < actualCounts.length; j++) {
				if( i == j ) {
					totalCorrect += matrix.get(i,j);
				} else {
					totalIncorrect += matrix.get(i,j);
				}
			}
		}

		return totalCorrect/(totalCorrect+totalIncorrect);
	}
}
