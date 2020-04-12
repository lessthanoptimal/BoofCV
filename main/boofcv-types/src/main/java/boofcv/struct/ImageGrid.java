/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.concurrency.BoofConcurrency;
import lombok.Getter;
import org.ddogleg.struct.Factory;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.Process;

/**
 * Breaks the image up into a grid. For use when processing individual regions of the image at a time. The size
 * of a cell is designed to be approximately the target size, but adjusted to ensure even coverage.
 *
 * @author Peter Abeles
 */
public class ImageGrid<T> {
	public final FastQueue<T> cells;

	// Number of rows and columns in the grid
	@Getter public int rows, cols;

	// length of a cell along each direction
	@Getter public int lengthX, lengthY;

	public ImageGrid(Factory<T> factory , Process<T> reset) {
		cells = new FastQueue<T>(factory, reset);
	}

	/**
	 * Initializes the grid based on how many pixels long a cell should be and the image size.
	 *
	 * @param targetLength Target length of a grid cell in pixels
	 * @param imageWidth Image width in pixels
	 * @param imageHeight Image height in pixels
	 */
	public void initialize( int targetLength , int imageWidth , int imageHeight ) {
		// Select grid's shape by trying to find the one which comes closest to the target cell size
		rows = imageHeight/targetLength + (imageHeight%targetLength > 0 ? 1 : 0);
		cols = imageWidth/targetLength + (imageWidth%targetLength > 0 ? 1 : 0);
		// The actual cell size is designed to be close to the target and avoid edge cases
		lengthY = (int)(imageHeight/(double)rows+0.5);
		lengthX = (int)(imageWidth/(double)cols+0.5);

		cells.reset();
		cells.resize(rows*cols);
	}

	public T getCellAtPixel( int pixelX , int pixelY )
	{
		int row = pixelY/lengthY;
		int col = pixelX/lengthX;
		if( row >= rows )
			row = rows-1;
		if( col >= cols )
			col = cols-1;
		return cells.data[ row*cols + col ];
	}

	public T get( int row , int col )
	{
		return cells.data[ row*cols + col ];
	}

	/**
	 * Goes through every cell in the grid and passes in data to the processor
	 */
	public void processCells( ProcessCell<T> processor )
	{
		int i = 0;
		for (int row = 0; row < rows; row++) {
			for (int col = 0; col < cols; col++, i++ ) {
				processor.process(row,col, cells.data[i] );
			}
		}
	}

	/**
	 * Same as {@link #processCells} but threaded.
	 */
	public void processCellsThreads( ProcessCell<T> processor )
	{
		BoofConcurrency.loopFor(0,cells.size,cellIdx->{
			int row = cellIdx/cols;
			int col = cellIdx%cols;
			processor.process(row,col, cells.data[cellIdx] );
		});
	}

	public interface ProcessCell<T>
	{
		void process( int row , int col , T data );
	}
}
