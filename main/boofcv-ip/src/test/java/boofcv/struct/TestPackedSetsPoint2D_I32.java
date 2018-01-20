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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPackedSetsPoint2D_I32 {

	@Test
	public void grow() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(20);

		assertEquals(0,alg.totalPoints());
		assertEquals(0,alg.size());
		alg.grow();
		assertEquals(0,alg.totalPoints());
		assertEquals(1,alg.size());
		assertEquals(1,alg.blocks.size);
		assertEquals(0,alg.tailBlockSize);
		assertEquals(20,alg.blocks.get(0).length);
		assertEquals(0,alg.sizeOfSet(0));

		alg.grow();
		assertEquals(0,alg.totalPoints());
		assertEquals(2,alg.size());
		assertEquals(1,alg.blocks.size);
		assertEquals(0,alg.tailBlockSize);
		assertEquals(20,alg.blocks.get(0).length);
		assertEquals(0,alg.sizeOfSet(1));
	}

	@Test
	public void removeTail_zero() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(6);

		try {
			alg.removeTail();
			fail("should have thrown exception");
		} catch( RuntimeException ignore ){}
	}

	@Test
	public void removeTail_one() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(6);

		alg.grow();
		alg.addPointToTail(1,2);
		alg.addPointToTail(3,4);

		alg.removeTail();
		assertEquals(0,alg.totalPoints());
		assertEquals(0,alg.size());
	}

	@Test
	public void removeTail_two() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(6);

		alg.grow();
		alg.addPointToTail(1,2);
		alg.addPointToTail(3,4);
		alg.grow();
		alg.addPointToTail(4,3);
		alg.addPointToTail(5,2);
		alg.addPointToTail(6,1);

		alg.removeTail();
		assertEquals(2,alg.totalPoints());
		assertEquals(1,alg.size());

		alg.addPointToTail(4,3);
		assertEquals(3,alg.totalPoints());
		assertEquals(1,alg.size());
		checkPoint(0,0,1,2, alg);
		checkPoint(0,1,3,4, alg);
		checkPoint(0,2,4,3, alg);
	}

	@Test
	public void addPointToTail() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(6);

		alg.grow();
		alg.addPointToTail(1,2);
		alg.addPointToTail(3,4);
		assertEquals(2,alg.totalPoints());
		assertEquals(1,alg.size());
		assertEquals(2,alg.sizeOfSet(0));
		checkPoint(0,0,1,2, alg);
		checkPoint(0,1,3,4, alg);

		alg.grow();
		alg.addPointToTail(4,3);
		alg.addPointToTail(5,2);
		alg.addPointToTail(6,1);
		assertEquals(5,alg.totalPoints());
		assertEquals(2,alg.size());
		assertEquals(2,alg.sizeOfSet(0));
		assertEquals(3,alg.sizeOfSet(1));
		checkPoint(1,0,4,3, alg);
		checkPoint(1,1,5,2, alg);
		checkPoint(1,2,6,1, alg);

		alg.grow();
		alg.addPointToTail(-1,3);
		assertEquals(6,alg.totalPoints());
		assertEquals(3,alg.size());
		assertEquals(1,alg.sizeOfSet(2));
		checkPoint(0,1,3,4, alg);
		checkPoint(1,2,6,1, alg);
		checkPoint(2,0,-1,3, alg);
	}

	private void checkPoint( int set , int point , int x , int y,
							 PackedSetsPoint2D_I32 alg) {

		FastQueue<Point2D_I32> list = new FastQueue<>(Point2D_I32.class,true);
		alg.getSet(set,list);

		assertEquals(x,list.get(point).x);
		assertEquals(y,list.get(point).y);
	}

	@Test
	public void writeOverSet() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(6);

		alg.grow();
		for (int i = 0; i < 12; i++) {
			alg.addPointToTail(1,i);
		}
		alg.grow();
		for (int i = 0; i < 20; i++) {
			alg.addPointToTail(2,i);
		}
		// sanity check
		assertEquals(12,alg.sizeOfSet(0));
		assertEquals(20,alg.sizeOfSet(1));

		// write new values
		List<Point2D_I32> expected = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			expected.add( new Point2D_I32(3,i));
		}
		alg.writeOverSet(1,expected);
		List<Point2D_I32> found = alg.getSet(1);

		assertEquals(expected.size(),found.size());
		for (int i = 0; i < expected.size(); i++) {
			assertTrue(expected.get(i).distance(found.get(i)) <= 1e-5);
		}
	}

	@Test
	public void writeOverSet_badsize() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(6);

		alg.grow();
		for (int i = 0; i < 12; i++) {
			alg.addPointToTail(1,i);
		}

		List<Point2D_I32> expected = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			expected.add( new Point2D_I32(3,i));
		}

		try {
			alg.writeOverSet(0, expected);
			fail("exception expected");
		} catch( RuntimeException ignore){}
	}

	@Test
	public void iterator() {
		PackedSetsPoint2D_I32 alg = new PackedSetsPoint2D_I32(6);

		alg.grow();
		alg.addPointToTail(1,2);
		alg.addPointToTail(3,4);
		alg.grow();
		alg.addPointToTail(4,3);
		alg.addPointToTail(5,2);
		alg.addPointToTail(6,1);

		PackedSetsPoint2D_I32.SetIterator iter = alg.createIterator();
		iter.setup(1);

		assertTrue(iter.hasNext());
		assertTrue(iter.next().distance(4,3) <= 0 );
		assertTrue(iter.hasNext());
		assertTrue(iter.next().distance(5,2) <= 0 );
		assertTrue(iter.hasNext());
		assertTrue(iter.next().distance(6,1) <= 0 );
		assertFalse(iter.hasNext());

	}
}