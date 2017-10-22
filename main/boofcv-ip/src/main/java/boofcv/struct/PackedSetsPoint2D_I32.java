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

import java.util.ArrayList;
import java.util.List;

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
public class PackedSetsPoint2D_I32 {
	// maximum number of elements that can be in a block
	final int blockLength;
	// arrays which store the points
	final FastQueue<int[]> blocks;
	// describes where there data for a set is stored
	final FastQueue<BlockIndexLength> sets = new FastQueue<>(BlockIndexLength.class,true);

	// the length/size of the last block
	int tailBlockSize;
	// the set which is on the tail and can have points added to
	BlockIndexLength tail;

	/**
	 * Configures the storage
	 *
	 * @param blockLength Number of elements in the block's array. Try 2000
	 */
	public PackedSetsPoint2D_I32(final int blockLength ) {
		if( blockLength < 2 )
			throw new IllegalArgumentException("Block length must be more than 2");
		// ensure that the block length is divisible by two
		this.blockLength = blockLength + (blockLength%2);
		blocks = new FastQueue(int[].class,true) {
			@Override
			protected Object createInstance() {
				return new int[ blockLength ];
			}
		};
		blocks.grow();
	}

	public PackedSetsPoint2D_I32() {
		this(2000);
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

		tail = s;
	}

	/**
	 * Removes the current point set from the end
	 */
	public void removeTail() {
		while( blocks.size-1 != tail.block )
			blocks.removeTail();
		tailBlockSize = tail.start;
		sets.removeTail();
		tail = sets.size > 0 ? sets.get( sets.size-1 ) : null;
	}

	/**
	 * Adds a point to the tail point set
	 * @param x coordinate
	 * @param y coordinate
	 */
	public void addPointToTail( int x , int y ) {
		int index = tail.start + tail.length*2;

		int block[];
		int blockIndex = tail.block + index/blockLength;
		if( blockIndex == blocks.size ) {
			tailBlockSize = 0;
			block = blocks.grow();
		} else {
			block = blocks.get( blockIndex );
		}
		tailBlockSize += 2;
		index %= blockLength;

		block[index ] = x;
		block[index+1 ] = y;
		tail.length += 1;
	}

	/**
	 * Total number of points
	 * @return
	 */
	public int totalPoints() {
		return (blockLength*(blocks.size-1) + tailBlockSize)/2;
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
	public void getSet(int which , FastQueue<Point2D_I32> list ) {
		list.reset();

		BlockIndexLength set = sets.get(which);

		for (int i = 0; i < set.length; i++) {
			int index = set.start + i*2;
			int blockIndex = set.block + index/blockLength;
			index %= blockLength;

			int block[] = blocks.get( blockIndex );
			list.grow().set( block[index] , block[index+1] );
		}
	}

	public List<Point2D_I32> getSet(int which) {
		FastQueue<Point2D_I32> tmp = new FastQueue<>(Point2D_I32.class,true);
		getSet(which,tmp);
		List<Point2D_I32> output = new ArrayList<>();
		output.addAll( tmp.toList() );
		return output;
	}

	public SetIterator createIterator() {
		return new SetIterator();
	}

	/**
	 * Returns the size of the set at the tail. If there is no tail an exception will be thrown.
	 */
	public int sizeOfTail() {
		return tail.length;
	}

	/**
	 * Overwrites the points in the set with the list of points.
	 *
	 * @param points Points which are to be written into the set. Must be the same size as the set.
	 */
	public void writeOverSet(int which, List<Point2D_I32> points) {
		BlockIndexLength set = sets.get(which);
		if( set.length != points.size() )
			throw new IllegalArgumentException("points and set don't have the same length");

		for (int i = 0; i < set.length; i++) {
			int index = set.start + i*2;
			int blockIndex = set.block + index/blockLength;
			index %= blockLength;

			Point2D_I32 p = points.get(i);
			int block[] = blocks.get( blockIndex );
			block[index] = p.x;
			block[index+1] = p.y;
		}
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
			int index = set.start + pointIndex*2;
			int blockIndex = set.block + index/blockLength;
			index %= blockLength;

			int block[] = blocks.get( blockIndex );
			p.set( block[index] , block[index+1] );

			pointIndex++;
			return p;
		}
	}
}
