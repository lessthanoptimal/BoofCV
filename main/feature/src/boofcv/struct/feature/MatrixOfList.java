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

package boofcv.struct.feature;


import java.util.ArrayList;
import java.util.List;

/**
 * A matrix of Lists for containing items in a grid.
 *
 * @author Peter Abeles
 */
public class MatrixOfList<T> {
	public List<T> grid[];
	public int width;
	public int height;

	public MatrixOfList(int width, int height) {
		this.width = width;
		this.height = height;
		grid = new ArrayList[ width*height ];
		for( int i = 0; i < grid.length; i++ ) {
			grid[i] = new ArrayList<>();
		}
	}


	public void reshape( int width , int height ) {
		if( width*height > grid.length ) {
			grid = new ArrayList[ width*height ];
			for( int i = 0; i < grid.length; i++ ) {
				grid[i] = new ArrayList<>();
			}
		}
		this.width = width;
		this.height = height;
	}

	public void reset() {
		final int N = width*height;
		for( int i = 0; i < N; i++ ) {
			grid[i].clear();
		}
	}

	public List<T> get( int x , int y ) {
		return grid[y*width + x];
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public List<T> createSingleList() {
		List<T> ret = new ArrayList<>();

		final int N = width*height;
		for( int i = 0; i < N; i++ ) {
			ret.addAll(grid[i]);
		}

		return ret;
	}

	public boolean isInBounds(int x, int y) {
		return( x >= 0 && x < width && y >= 0 && y < height );
	}
}
