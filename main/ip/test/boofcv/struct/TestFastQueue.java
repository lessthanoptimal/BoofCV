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

package boofcv.struct;

import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;


/**
 * @author Peter Abeles
 */
public class TestFastQueue {

	@Test
	public void checkDeclareInstance() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(10,Point2D_I32.class,true);

		assertTrue(alg.getMaxSize()>0);
		assertTrue(alg.data[0] != null);

		alg = new FastQueue<Point2D_I32>(10,Point2D_I32.class,false);

		assertTrue(alg.getMaxSize()>0);
		assertTrue(alg.data[0] == null);
	}

	@Test
	public void toList() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(10,Point2D_I32.class,true);

		List<Point2D_I32> l = alg.toList();
		assertEquals(0,l.size());
		
		alg.grow().set(1,2);
		alg.grow().set(1,2);
		alg.grow().set(2,3);
		alg.removeTail();

		l = alg.toList();
		assertEquals(2,l.size());
		assertEquals(1,l.get(0).x);
		assertEquals(1,l.get(1).x);
	}
	
	@Test
	public void removeTail() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(10,Point2D_I32.class,true);

		alg.grow();
		assertEquals(1,alg.size);
		alg.removeTail();
		assertEquals(0,alg.size);
	}

	@Test
	public void getTail() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(10,Point2D_I32.class,true);

		alg.grow();alg.grow();

		assertTrue(alg.data[1] == alg.getTail());
	}

	@Test
	public void get_pop() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

		// test a failure case
		try {
			alg.get(0);
			fail("Didn't fail");
		} catch( IllegalArgumentException e ) {}

		alg.grow();
		alg.get(0);
	}

	@Test
	public void size() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(Point2D_I32.class,true);
		assertEquals(0,alg.size);
		alg.grow();
		assertEquals(1,alg.size);
	}

	@Test
	public void add() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(Point2D_I32.class,false);

		Point2D_I32 a = new Point2D_I32();
		alg.add(a);
		assertTrue(a==alg.data[0]);
	}

	@Test
	public void addAll() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(Point2D_I32.class,true);
		alg.grow();
		alg.grow();

		FastQueue<Point2D_I32> alg2 = new FastQueue<Point2D_I32>(Point2D_I32.class,false);

		alg2.addAll(alg);

		assertTrue(alg.get(0) == alg2.get(0));
		assertTrue(alg.get(1) == alg2.get(1));
	}

	/**
	 * Checks to see if pop automatically grows correctly
	 */
	@Test
	public void pop_grow() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(1,Point2D_I32.class,true);

		int before = alg.getMaxSize();
		for( int i = 0; i < 20; i++ ) {
			alg.grow();
		}
		alg.get(19);
		int after = alg.getMaxSize();
		assertTrue(after>before);
	}

	@Test
	public void growArray() {
		FastQueue<Point2D_I32> alg = new FastQueue<Point2D_I32>(1,Point2D_I32.class,true);

		alg.grow().set(10,12);
		int before = alg.getMaxSize();
		alg.growArray(before+5);
		assertEquals(10,alg.get(0).getX());
	}
}
