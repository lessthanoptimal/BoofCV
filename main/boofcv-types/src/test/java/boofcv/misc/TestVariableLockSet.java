/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.misc;

import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestVariableLockSet extends BoofStandardJUnit {
	@Test void safe_timeout() throws InterruptedException {
		var alg = new VariableLockSet();

		// Locked by the same thread. Should run just fine and unlock it
		assertTrue(alg.safe(100L, () -> {}));
		assertFalse(alg.lock.isLocked());

		// Lock it before. It shouldn't unlock it
		alg.lock.lock();
		assertTrue(alg.safe(100L, () -> {}));
		assertTrue(alg.lock.isLocked());
		alg.lock.unlock();

		// Shouldn't time out since it's not locked
		new Thread(() -> assertTrue(alg.safe(100L, () -> {}))).start();
		Thread.sleep(400L);

		// This will time out because it's already locked
		alg.lock.lock();
		new Thread(() -> assertFalse(alg.safe(100L, () -> {}))).start();
		Thread.sleep(400L);
		alg.lock.unlock();
	}

	@Test void select_timeout() throws InterruptedException {
		var alg = new VariableLockSet();

		// Locked by the same thread. Should run just fine and unlock it
		assertTrue(alg.select(100L, () -> 1.0).success);
		assertFalse(alg.lock.isLocked());

		// Lock it before. It shouldn't unlock it
		alg.lock.lock();
		assertTrue(alg.select(100L, () -> 1.0).success);
		assertTrue(alg.lock.isLocked());
		alg.lock.unlock();

		// Shouldn't time out since it's not locked
		new Thread(() -> assertTrue(alg.select(100L, () -> 1.0).success)).start();
		Thread.sleep(400L);

		// This will time out because it's already locked
		alg.lock.lock();
		new Thread(() -> assertFalse(alg.select(100L, () -> 1.0).success)).start();
		Thread.sleep(400L);
		alg.lock.unlock();
	}

	@Test void selectNull_timeout() throws InterruptedException {
		var alg = new VariableLockSet();

		// Locked by the same thread. Should run just fine and unlock it
		assertNotNull(alg.selectNull(100L, () -> 1.0));
		assertFalse(alg.lock.isLocked());

		// Lock it before. It shouldn't unlock it
		alg.lock.lock();
		assertNotNull(alg.selectNull(100L, () -> 1.0));
		assertTrue(alg.lock.isLocked());
		alg.lock.unlock();

		// Shouldn't time out since it's not locked
		new Thread(() -> assertNotNull(alg.selectNull(100L, () -> 1.0))).start();
		Thread.sleep(400L);

		// This will time out because it's already locked
		alg.lock.lock();
		new Thread(() -> assertNull(alg.selectNull(100L, () -> 1.0))).start();
		Thread.sleep(400L);
		alg.lock.unlock();
	}
}
