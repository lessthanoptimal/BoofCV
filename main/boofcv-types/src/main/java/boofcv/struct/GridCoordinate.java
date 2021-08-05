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

package boofcv.struct;

/**
 * @author Peter Abeles
 */
public class GridCoordinate {
	public int row;
	public int col;

	public GridCoordinate( int row, int col ) {
		this.row = row;
		this.col = col;
	}

	public GridCoordinate() {}

	public void setTo( int row, int col ) {
		this.row = row;
		this.col = col;
	}

	public boolean equals( int row, int col ) {
		return this.row == row && this.col == col;
	}

	public boolean equals( GridCoordinate a ) {
		return this.row == a.row && this.col == a.col;
	}

	@Override public String toString() {
		return "{" +
				"row=" + row +
				", col=" + col +
				'}';
	}
}
