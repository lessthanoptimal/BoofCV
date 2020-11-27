/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.BoofTesting;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.PackedSetsPoint2D_I32;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.DogArray;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestLinearExternalContours extends BoofStandardJUnit {
	Random rand = BoofTesting.createRandom(0);

	public static GrayU8 TEST1 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			 {0,0,0,0,0,0,0,0,1,0,0,0,1,1,0},
			 {0,0,0,0,0,0,0,0,1,0,0,0,1,1,0},
			 {0,0,0,0,0,0,0,0,1,0,0,1,1,0,0},
			 {0,0,0,0,0,0,0,0,0,1,1,1,1,0,0},
			 {0,0,0,1,0,0,0,0,0,1,1,1,0,0,0},
			 {0,0,0,1,0,0,0,1,1,1,1,1,0,0,0},
			 {0,1,1,1,1,1,1,1,1,1,1,0,0,0,0},
			 {0,0,0,0,1,1,1,1,1,0,0,0,0,0,0},
			 {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}});


	public static GrayU8 TEST2 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,0,0,0,0,0,0,0},
			 {0,0,0,1,0,0,0,0,1,0,0,0,0,0},
			 {0,0,1,0,1,0,0,1,0,0,1,0,0,0},
			 {0,0,0,1,0,0,1,0,1,0,1,1,1,0},
			 {0,0,0,0,0,1,0,0,0,1,1,1,1,0},
			 {0,0,0,1,0,1,0,0,0,1,0,0,0,0},
			 {0,0,0,0,0,1,0,1,1,1,0,1,1,0},
			 {0,1,1,1,0,0,1,0,0,1,0,0,1,0},
			 {0,0,0,0,1,1,1,1,1,0,0,0,0,0},
			 {0,0,0,0,0,0,0,0,0,0,0,0,0,0}});

	public static GrayU8 TEST3 = new GrayU8(new byte[][]
			{{0,0,0,0,0},
			 {0,1,1,1,0},
			 {0,1,1,1,0},
			 {0,1,0,1,0},
			 {0,1,1,1,0},
			 {0,0,0,0,0}});

	public static GrayU8 TEST4 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,0},
			 {0,0,1,1,1,1,1,0},
			 {0,1,0,1,1,1,1,0},
			 {0,1,1,1,0,1,1,0},
			 {0,1,1,1,1,1,1,0},
			 {0,1,1,1,1,1,1,0},
			 {0,1,1,1,1,1,1,0},
			 {0,0,0,0,0,0,0,0}});

	public static GrayU8 TEST5 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,0},
			 {0,1,1,1,1,1,1,0},
			 {0,1,0,0,0,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,1,1,1,1,1,0},
			 {0,0,0,0,0,0,0,0}});

	public static GrayU8 TEST6 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,0},
		 	 {0,1,1,1,1,1,1,0},
			 {0,1,0,0,1,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,1,1,1,1,1,0},
			 {0,0,0,0,0,0,0,0}});

	public static GrayU8 TEST7 = new GrayU8(new byte[][]
			{{0,0,0,0,0,0,0,0},
			 {0,1,1,1,1,1,1,0},
			 {0,1,0,0,0,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,0,1,1,0,1,0},
			 {0,1,0,0,0,1,1,0},
			 {0,1,1,1,1,1,1,0},
			 {0,0,0,0,0,0,0,0}});

	@Test
	void compareToChang2004() {
		var binary = new GrayU8(200,190);
		var labeled = new GrayS32(200,190);
		GImageMiscOps.fillUniform(binary,rand,0,1);

		var binaryExpanded = binary.createNew(binary.width+2, binary.height+2);
		GImageMiscOps.copy(0,0, 1,1,binary.width,binary.height,binary,binaryExpanded);

		for( var rule : ConnectRule.values() ) {
			LinearContourLabelChang2004 chang = new LinearContourLabelChang2004(rule);
			LinearExternalContours alg = new LinearExternalContours(rule);

			chang.setSaveInternalContours(false);
			chang.process(binary.clone(),labeled);

			alg.process(binaryExpanded.clone(),1,1);

			DogArray<ContourPacked> expected = chang.getContours();
			PackedSetsPoint2D_I32 found = alg.getExternalContours();

			assertEquals(expected.size, found.size());

			// individual contours should be tested, but this seems to catch major problems
		}
	}

	@Test
	void test1_4() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.FOUR);

		GrayU8 binary = TEST1.clone();

		alg.process(binary,1,1);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(2,contours.size());
	}

	@Test
	void test1_8() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.EIGHT);

		GrayU8 binary = TEST1.clone();

		alg.process(binary,1,1);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(1,contours.size());
	}

	@Test
	void test2_4() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.FOUR);

		GrayU8 binary = TEST2.clone();

		alg.process(binary,1,1);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(14,contours.size());
	}

	@Test
	void test2_8() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.EIGHT);

		GrayU8 binary = TEST2.clone();

		alg.process(binary,1,1);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(4,contours.size());
	}

	@Test
	void test3_4() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.FOUR);

		GrayU8 binary = TEST3.clone();

		alg.process(binary,1,1);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(1,contours.size());
	}

	@Test
	void test3_8() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.EIGHT);

		GrayU8 binary = TEST3.clone();

		alg.process(binary,1,1);

		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(1,contours.size());
	}

	@Test
	void test4() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.FOUR);
		alg.process(TEST4.clone(),1,1);
		checkExpectedExternal(new int[]{24},alg);

		alg = new LinearExternalContours(ConnectRule.EIGHT);
		alg.process(TEST4.clone(),1,1);
		checkExpectedExternal(new int[]{19},alg);
	}

	@Test
	void test5() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.FOUR);
		alg.process(TEST5.clone(),1,1);
		checkExpectedExternal(new int[]{20},alg);

		alg = new LinearExternalContours(ConnectRule.EIGHT);
		alg.process(TEST5.clone(),1,1);
		checkExpectedExternal(new int[]{20},alg);
	}

	@Test
	void test6() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.FOUR);
		alg.process(TEST6.clone(),1,1);
		checkExpectedExternal(new int[]{20},alg);

		alg = new LinearExternalContours(ConnectRule.EIGHT);
		alg.process(TEST6.clone(),1,1);
		checkExpectedExternal(new int[]{20},alg);
	}

	@Test
	void test7() {
		LinearExternalContours alg = new LinearExternalContours(ConnectRule.FOUR);
		alg.process(TEST7.clone(),1,1);
		checkExpectedExternal(new int[]{4,20},alg);

		alg = new LinearExternalContours(ConnectRule.EIGHT);
		alg.process(TEST7.clone(),1,1);
		checkExpectedExternal(new int[]{20},alg);
	}

	public static void checkExpectedExternal(int expected[] , LinearExternalContours alg ) {
		PackedSetsPoint2D_I32 contours = alg.getExternalContours();
		assertEquals(expected.length, contours.size());

		DogArray<Point2D_I32> points = new DogArray<>(Point2D_I32::new);

		boolean matched[] = new boolean[ expected.length ];

		for (int i = 0; i < expected.length; i++) {
			int e = expected[i];
			boolean foo = false;
			for (int j = 0; j < contours.size(); j++) {
				if( matched[j])
					continue;
				contours.getSet(j,points);
				if( points.size == e ) {
					matched[j] = true;
					foo = true;
					break;
				}
			}
			assertTrue(foo);
		}
	}
}
