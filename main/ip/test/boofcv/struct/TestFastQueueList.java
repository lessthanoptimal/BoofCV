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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestFastQueueList {

	@Test
	public void size() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);

		List<Double> list = queue.toList();

		assertTrue(0==list.size());

		queue.add( 1.0 );

		assertTrue(1==list.size());
	}

	@Test
	public void isEmpty() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);

		List<Double> list = queue.toList();

		assertTrue( list.isEmpty() );

		queue.add( 1.0 );

		assertFalse(list.isEmpty());
	}

	@Test
	public void contains() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);
		Double d = 1.0;

		List<Double> list = queue.toList();

		assertFalse(list.contains(d));

		queue.add( d );

		assertTrue(!list.isEmpty());
	}

	@Test
	public void iterator() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);
		queue.add(1.0);
		queue.add(2.0);
		queue.add(3.0);

		Iterator<Double> iterator = queue.toList().iterator();
		assertTrue(iterator.hasNext());
		assertTrue(1.0 == iterator.next());
		assertTrue(iterator.hasNext());
		assertTrue(2.0 == iterator.next());
		assertTrue(iterator.hasNext());
		assertTrue(3.0 == iterator.next());
		assertFalse(iterator.hasNext());

	}

	@Test
	public void toArray() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);
		queue.add(1.0);
		queue.add(2.0);
		queue.add(3.0);

		Object[] array = queue.toList().toArray();
		assertEquals(3, array.length);
		assertTrue(1.0 == (Double)array[0]);
		assertTrue(2.0 == (Double)array[1]);
		assertTrue(3.0 == (Double)array[2]);

		// remove an element from the queue to make sure it isn't using array length
		queue.removeTail();
		array = queue.toList().toArray();
		assertEquals(2, array.length);
	}

	@Test
	public void containsAll() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);
		queue.add(1.0);
		queue.add(2.0);
		queue.add(3.0);

		List<Double> list = new ArrayList<Double>();
		list.add(1.0);
		list.add(2.0);

		assertTrue(queue.toList().containsAll(list));

		list.add(5.0);
		assertFalse(queue.toList().containsAll(list));
	}

	@Test
	public void get() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);
		queue.add(1.0);
		queue.add(2.0);

		assertTrue(2.0==queue.toList().get(1));
	}

	@Test
	public void set() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);
		queue.add(1.0);
		queue.add(2.0);

		List<Double> list = queue.toList();
		list.set(0,3.0);

		assertTrue(3.0==list.get(0));
	}

	@Test
	public void indexOf_lastIndexOf() {
		FastQueue<Double> queue = new FastQueue<Double>(100,Double.class,false);
		queue.add(1.0);
		queue.add(2.0);
		queue.add(2.0);
		queue.add(3.0);

		assertTrue(1==queue.toList().indexOf(2.0));
		assertTrue(2==queue.toList().lastIndexOf(2.0));
	}
}
