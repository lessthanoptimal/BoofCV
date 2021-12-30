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

package boofcv.struct.geo;

import org.ddogleg.struct.DogArray;
import org.ejml.data.DMatrixRMaj;

/**
 * {@link DogArray} which will internally declare {@link DMatrixRMaj} of a specific shape.
 *
 * @author Peter Abeles
 */
public class QueueMatrix extends DogArray<DMatrixRMaj> {

	/**
	 * Specifies the matrix shape.
	 *
	 * @param numRows Number of rows in each matrix.
	 * @param numCols Number of columns in each matrix.
	 */
	public QueueMatrix( int numRows, int numCols ) {
		super(DMatrixRMaj.class, () -> new DMatrixRMaj(numRows, numCols));
	}
}
