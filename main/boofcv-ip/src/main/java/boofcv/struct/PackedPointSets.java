/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

/**
 * Compact storage for a set of points. Designed to minimize memory usage. New points can only be added to a set
 * when the set is the tail/last one in the list.
 *
 * <p>Internally, the value of each point is stored in one or more int[] arrays. The maximum size of each array
 * is specified in the constructor. These arrays are known as blocks. A set of points specifies which block
 * the first element belongs in, the index and how many points are in the set.</p>
 *
 * @author Peter Abeles
 */
public class PackedPointSets {
	// maximum number of elements that can be in a block
	final int blockLength;
	// arrays which store the points
	final FastQueue<int[]> blocks;
	// describes where there data for a set is stored
	final FastQueue<BlockIndexLength> sets = new FastQueue<>(BlockIndexLength.class,true);

	// the length/size of the last block
	int tailBlockSize;
	// the set which is on the tail and can have points added to
	BlockIndexLength tailSet;

	/**
	 * Configures the storage
	 *
	 * @param blockLength Number of elements in the block's array. Try 2000
	 */
	public PackedPointSets(final int blockLength ) {
		this.blockLength = blockLength;
		blocks = new FastQueue(int[].class,true) {
			@Override
			protected Object createInstance() {
				return new int[ blockLength ];
			}
		};
	}

	/**
	 * Discards all previously detected points but does not free its memory. This allows it to be recycled
	 */
	public void reset() {
		tailBlockSize = 0;
		blocks.reset();
		blocks.grow();
		sets.reset();
	}

	/**
	 * Adds a new point set to the end.
	 */
	public void grow() {
		if( tailBlockSize >= blockLength ) {
			tailBlockSize = 0;
			blocks.grow();
		}

		BlockIndexLength s = sets.grow();
		s.block = blocks.size-1;
		s.start = tailBlockSize;
		s.length = 0;

		tailSet = s;
	}

	/**
	 * Removes the current point set from the end
	 */
	public void removeTail() {
		while( sets.size-1 != tailSet.block )
			sets.removeTail();
		tailBlockSize = tailSet.start;
		tailSet = null;
	}

	/**
	 * Adds a point to the tail point set
	 * @param x coordinate
	 * @param y coordinate
	 */
	public void addPointToTail( int x , int y ) {
		int index = tailSet.start + tailSet.length*2;

		int block[];
		int blockIndex = tailSet.block + index/blockLength;
		if( blockIndex == blocks.size ) {
			tailBlockSize = 0;
			block = blocks.grow();
		} else {
			tailBlockSize += 2;
			block = blocks.get( blockIndex );
		}

		index %= blockLength;

		block[tailSet.start + index ] = x;
		block[tailSet.start + index+1 ] = y;
		tailSet.length += 1;
	}

	/**
	 * Total number of points
	 * @return
	 */
	public int totalPoints() {
		return blockLength*(blocks.size-1) + tailBlockSize;
	}

	/**
	 * Number of point sets
	 * @return number of point sets
	 */
	public int size() {
		return sets.size;
	}

	/**
	 * Returns the size/length of a point set
	 * @param which index of point set
	 * @return the size
	 */
	public int sizeOfSet( int which ) {
		return sets.get(which).length;
	}

	/**
	 * Copies all the points in the set into the specified list
	 * @param which (Input) which point set
	 * @param list (Output) Storage for points
	 */
	public void setToList(int which , FastQueue<Point2D_I32> list ) {
		list.reset();

		BlockIndexLength set = sets.get(which);

		for (int i = 0; i < set.length; i++) {
			int index = tailSet.start + i*2;
			int blockIndex = tailSet.block + index/blockLength;
			index %= blockLength;

			int block[] = blocks.get( blockIndex );
			list.grow().set( block[index] , block[index+1] );
		}
	}
	public SetIterator createIterator() {
		return new SetIterator();
	}

	/**
	 * Used to access all the points in a set without making a copy.
	 */
	public class SetIterator {
		BlockIndexLength set;
		int pointIndex;
		Point2D_I32 p = new PointIndex_I32();

		/**
		 * Specifies which set the iterator should process
		 * @param whichSet index of the set
		 */
		public void setup( int whichSet ) {
			set = sets.get(whichSet);
			pointIndex = 0;
		}

		public void setToStart() {
			pointIndex = 0;
		}

		public boolean hasNext() {
			return pointIndex < set.length;
		}

		public Point2D_I32 next() {
			int index = tailSet.start + pointIndex*2;
			int blockIndex = tailSet.block + index/blockLength;
			index %= blockLength;

			int block[] = blocks.get( blockIndex );
			p.set( block[index] , block[index+1] );

			pointIndex++;
			return p;
		}
	}
}
