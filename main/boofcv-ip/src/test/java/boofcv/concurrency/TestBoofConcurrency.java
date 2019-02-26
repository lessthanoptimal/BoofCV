/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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

package boofcv.concurrency;

import boofcv.misc.BoofMiscOps;
import org.ddogleg.struct.GrowQueue_I32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
class TestBoofConcurrency {

	final int numThreads = 4;

	TestBoofConcurrency() {
		// need to know the max number of threads for many of these checks
		BoofConcurrency.setMaxThreads(numThreads);
	}

	@Test
	void blocks() {
		GrowQueue_I32 found = new GrowQueue_I32();

		BoofConcurrency.loopBlocks(10,100,12,new BlockTask(found));

		assertEquals(8,found.size);
		// the timing was set up so that they should be approximately in reverse order, if run in parallel
		assertTrue(found.data[0] != 10 && found.data[2] != 32 );
		findPair(found,10,32);
		findPair(found,32,54);
		findPair(found,54,76);
		findPair(found,76,100);
	}

	private void findPair( GrowQueue_I32 found , int val0 , int val1 ) {
		for (int i = 0; i < found.size; i += 2) {
			if( found.get(i) == val0 && found.get(i+1) == val1 ) {
				return;
			}
		}
		fail("Couldn't find pair "+val0+" "+val1);
	}

	@Test
	void selectBlockSize() {
		assertEquals(10,BoofConcurrency.selectBlockSize(100,5,10));
		assertEquals(20,BoofConcurrency.selectBlockSize(100,20,10));
		assertEquals(16,BoofConcurrency.selectBlockSize(100,15,10));
		assertEquals(100,BoofConcurrency.selectBlockSize(100,80,10));

		assertEquals(22,BoofConcurrency.selectBlockSize(90,12,4));
	}

	static class BlockTask implements IntRangeConsumer {

		final GrowQueue_I32 found;

		BlockTask(GrowQueue_I32 found) {
			this.found = found;
		}

		@Override
		public void accept(int minInclusive, int maxExclusive) {
			// sleep such that they will be out of order. This is manually checked to make sure it is in parallel
			BoofMiscOps.sleep(100+(100-minInclusive));
			synchronized (found) {
				found.add(minInclusive);
				found.add(maxExclusive);
			}

		}
	}

	@Test
	void sum() {
		int foundI = BoofConcurrency.sum(5,10,int.class,i->i+2).intValue();
		assertEquals(45,foundI);

		double foundD = BoofConcurrency.sum(5,10,double.class,i->i+2.5).doubleValue();
		assertEquals(47.5,foundD, UtilEjml.TEST_F64);
	}

	@Test
	void max() {
		int foundI = BoofConcurrency.max(5,10,int.class,i->i+2).intValue();
		assertEquals(9+2,foundI);

		double foundD = BoofConcurrency.max(5,10,double.class,i->i+2.5).doubleValue();
		assertEquals(9.0+2.5,foundD, UtilEjml.TEST_F64);
	}

	@Test
	void min() {
		int foundI = BoofConcurrency.min(5,10,int.class,i->i+2).intValue();
		assertEquals(5+2,foundI);

		double foundD = BoofConcurrency.min(5,10,double.class,i->i+2.5).doubleValue();
		assertEquals(5.0+2.5,foundD, UtilEjml.TEST_F64);
	}
}