/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.struct.geo;

import org.ddogleg.struct.FastQueue;
import org.ejml.data.DenseMatrix64F;

/**
 * {@link FastQueue} which will internally declare {@link DenseMatrix64F} of a specific shape.
 *
 * @author Peter Abeles
 */
public class QueueMatrix extends FastQueue<DenseMatrix64F> {

	// matrix shape
	private int numRows,numCols;

	/**
	 * Specifies the matrix shape and the number of elements in the internal array initially.
	 *
	 * @param numRows Number of rows in each matrix.
	 * @param numCols Number of columns in each matrix.
	 * @param initialMaxSize Initial number of matrices in storage.
	 */
	public QueueMatrix(int numRows, int numCols, int initialMaxSize) {
		this.numRows = numRows;
		this.numCols = numCols;

		init(initialMaxSize,DenseMatrix64F.class,true);
	}

	/**
	 * Specifies the matrix shape.
	 *
	 * @param numRows Number of rows in each matrix.
	 * @param numCols Number of columns in each matrix.
	 */
	public QueueMatrix( int numRows , int numCols ) {
		this.numRows = numRows;
		this.numCols = numCols;

		init(10,DenseMatrix64F.class,true);
	}

	@Override
	protected DenseMatrix64F createInstance() {
		return new DenseMatrix64F(numRows,numCols);
	}
}
