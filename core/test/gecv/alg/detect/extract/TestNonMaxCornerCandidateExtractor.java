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

package gecv.alg.detect.extract;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestNonMaxCornerCandidateExtractor {

	/**
	 * If a list of pre-existing corners is added they should not be added again to the found list
	 */
	@Test
	public void excludePreExisting() {
		fail("implement");
	}

/**
	 * See if it produces the correct answers after adjusting the width
	 */
	@Test
	public void testRegionWidth() {
	}

	/**
	 * Make sure it does the threshold thing correctly
	 */
	@Test
	public void testThreshold() {
		
	}
}
