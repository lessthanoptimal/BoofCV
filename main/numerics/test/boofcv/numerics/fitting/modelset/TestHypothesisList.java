/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.numerics.fitting.modelset;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestHypothesisList {

	ModelGenerator<Object,Double> model = new ModelGenerator<Object, Double>() {
		@Override
		public Object createModelInstance() {
			return new Object();
		}

		@Override
		public void generate(List<Double> dataSet, HypothesisList<Object> models) {}

		@Override
		public int getMinimumPoints() {
			return 1;
		}
	};

	@Test
	public void swap_get() {
		HypothesisList<Object> alg = new HypothesisList<Object>(model);

		Object a = 1234.0;
		alg.pop();
		Object inside = alg.get(0);
		Object b = alg.swap(0,a);
		
		assertTrue(inside == b);
		assertTrue(a==alg.get(0));
	}

	@Test
	public void pop() {
		HypothesisList<Object> alg = new HypothesisList<Object>(model);

		assertEquals(1,alg.getMaxSize());
		assertEquals(0,alg.size());
		Object v = alg.pop();
		assertEquals(1,alg.size());
		
		// should trigger a grow event
		Object w = alg.pop();
		assertEquals(2,alg.getMaxSize());
		assertEquals(2,alg.size());
		assertTrue(v != w);
	}

	@Test
	public void reset() {
		HypothesisList<Object> alg = new HypothesisList<Object>(model);
		
		alg.pop();
		alg.pop();
		assertEquals(2,alg.size());
		alg.reset();
		assertEquals(0,alg.size()); 
	}

	@Test
	public void resize() {
		HypothesisList<Object> alg = new HypothesisList<Object>(model);

		// test initial conditions
		assertEquals(0,alg.size());
		assertEquals(1,alg.getMaxSize());
		Object a = alg.pop();
		assertEquals(1,alg.size());
		
		// test grow behavior
		alg.resize(2);
		assertEquals(1,alg.size());
		assertTrue(a==alg.get(0));
		assertEquals(2,alg.getMaxSize());
		
		
		// test shrink behavior
		alg.resize(1);
		assertEquals(1,alg.size());
		assertEquals(1,alg.getMaxSize());
		
		
		// test shrink when N is more than resize
		alg.pop();
		alg.pop();
		assertEquals(3,alg.size());
		alg.resize(1);
		assertEquals(1,alg.size());
		assertEquals(1,alg.getMaxSize());
	}
}
