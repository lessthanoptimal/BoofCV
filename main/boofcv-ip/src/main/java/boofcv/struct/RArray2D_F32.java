/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.struct;

import java.util.Arrays;

/**
 * 2D-Array where each row is it's own primitive array.
 *
 * @author Peter Abeles
 */
public class RArray2D_F32 {
	public float[][] data = new float[0][0];

	public int cols,rows;

	public RArray2D_F32( int rows, int cols ) {
		reshape(rows,cols);
	}

	public void reshape( int rows, int cols ) {
		// this could be done much more intelligently and recycling data when possible
		if( data.length < rows || (data.length > 0 && data[0].length < cols) ) {
			data = new float[rows][cols];
		}
		this.rows = rows;
		this.cols = cols;
	}

	public void zero() {
		for (int i = 0; i < rows; i++) {
			Arrays.fill(data[i],0,cols,0);
		}
	}

	public float get( int row , int col ) {
		if( row < 0 || row >= rows || col < 0 || col >= cols )
			throw new IllegalArgumentException("Out of bounds exception");

		return data[row][col];
	}

	public float unsafe_get( int row, int col ) {
		return data[row][col];
	}

	public int getCols() {
		return cols;
	}

	public int getRows() {
		return rows;
	}
}
