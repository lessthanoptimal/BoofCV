/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.filter.binary;

import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public abstract class GenericBinaryContourFinder extends GenericBinaryContourInterface {

	@Override
	protected abstract BinaryContourFinder create();

	@Test void inputNotModified() {
		GrayU8 input = TEST2.clone();

		BinaryContourFinder alg = create();

		alg.process(input);

		assertEquals(0, ImageStatistics.meanDiffSq(TEST2, input), 1e-8);
	}

	@Test void minContour() {
		GrayU8 input = TEST3.clone();

		BinaryContourFinder alg = create();

		alg.setMinContour(1000);
		alg.process(input);
		assertEquals(0, alg.getContours().size());
	}

	@Test void maxContour() {
		GrayU8 input = TEST3.clone();

		BinaryContourFinder alg = create();

		alg.setMaxContour(1);
		alg.process(input);

		assertEquals(0, alg.getContours().size());
	}

	@Test void connectRule() {
		GrayU8 input = TEST3.clone();

		BinaryContourFinder alg = create();

		alg.process(input);
		checkExternalSize(alg, 0, 10);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(input);
		checkExternalSize(alg, 0, 8);
	}

	@Test void saveInternal() {
		if (!supportsInternalContour)
			return;

		GrayU8 input = TEST3.clone();

		BinaryContourFinder alg = create();

		alg.process(input);
		checkInternalSize(alg, 0, 0, 8);

		alg.setSaveInnerContour(false);
		alg.process(input);
		checkInternalSize(alg, 0, 0, 0);
	}

	@Test void testCase1() {
		GrayU8 input = TEST1.clone();

		BinaryContourFinder alg = create();

		alg.setConnectRule(ConnectRule.FOUR);
		alg.process(input);
		checkExpectedExternal(new int[]{4, 42}, alg);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(input);
		checkExpectedExternal(new int[]{37}, alg);
	}

	@Test void testCase2() {
		GrayU8 input = TEST2.clone();

		BinaryContourFinder alg = create();

		alg.setConnectRule(ConnectRule.FOUR);
		alg.process(input);
		checkExpectedExternal(new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 4, 4, 10, 20}, alg);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(input);
		checkExpectedExternal(new int[]{1, 3, 4, 32}, alg);
	}

	@Test void testCase4() {
		GrayU8 input = TEST4.clone();
		BinaryContourFinder alg = create();

		alg.setConnectRule(ConnectRule.FOUR);
		alg.process(input);
		checkExpectedExternal(new int[]{24}, alg);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(input);
		checkExpectedExternal(new int[]{19}, alg);
	}

	@Test void testCase5() {
		BinaryContourFinder alg = create();

		alg.setConnectRule(ConnectRule.FOUR);
		alg.process(TEST5.clone());
		checkExpectedExternal(new int[]{20}, alg);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(TEST5.clone());
		checkExpectedExternal(new int[]{20}, alg);
	}

	@Test void test6() {
		BinaryContourFinder alg = create();

		alg.setConnectRule(ConnectRule.FOUR);
		alg.process(TEST6.clone());
		checkExpectedExternal(new int[]{20}, alg);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(TEST6.clone());
		checkExpectedExternal(new int[]{20}, alg);
	}

	@Test void test7() {
		BinaryContourFinder alg = create();

		alg.setConnectRule(ConnectRule.FOUR);
		alg.process(TEST7.clone());
		checkExpectedExternal(new int[]{4, 20}, alg);

		alg.setConnectRule(ConnectRule.EIGHT);
		alg.process(TEST7.clone());
		checkExpectedExternal(new int[]{20}, alg);
	}
}
