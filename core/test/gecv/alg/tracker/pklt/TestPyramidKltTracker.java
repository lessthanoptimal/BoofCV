/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.tracker.pklt;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestPyramidKltTracker {

	/**
	 * Test set description when the image is fully inside the image for all the pyramid layers
	 */
	@Test
	public void setDescription() {
		fail("implement tests");
	}

	/**
	 * Test set description when a feature is inside the allowed region for only part of the pyramid.
	 */
	@Test
	public void setDescription_partial() {
		fail("implement tests");
	}

	/**
	 * Test positive examples of tracking when there should be no fault at any point.
	 */
	@Test
	public void track() {
		fail("implement tests");
	}

	/**
	 * Test tracking when a feature is out of bounds in the middle of the pyramid
	 */
	@Test
	public void track_outside_middle() {
		fail("implement tests");
	}

	/**
	 * The feature being tracked will have a fault.
	 */
	@Test
	public void track_fault() {
		fail("implement tests");
	}
}
