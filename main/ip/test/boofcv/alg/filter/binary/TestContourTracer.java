/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestContourTracer {

	FastQueue<Point2D_I32> queue = new FastQueue<>(Point2D_I32.class, true);
	List<Point2D_I32> found = new ArrayList<>();

	@Before
	public void init() {
		queue.reset();
		found.clear();
	}


	@Test
	public void single() {
		GrayU8 pattern = new GrayU8(1,1);
		ImageMiscOps.fill(pattern,1);

		shiftContourCheck(pattern,1,ConnectRule.FOUR);
		shiftContourCheck(pattern,1,ConnectRule.EIGHT);
	}

	@Test
	public void two_horizontal() {
		GrayU8 pattern = new GrayU8(2,1);
		ImageMiscOps.fill(pattern,1);

		shiftContourCheck(pattern,2,ConnectRule.FOUR);
		shiftContourCheck(pattern,2,ConnectRule.EIGHT);
	}

	@Test
	public void two_vertical() {
		GrayU8 pattern = new GrayU8(1,2);
		ImageMiscOps.fill(pattern,1);

		shiftContourCheck(pattern,2,ConnectRule.FOUR);
		shiftContourCheck(pattern,2,ConnectRule.EIGHT);
	}

	@Test
	public void square() {
		GrayU8 pattern = new GrayU8(2,2);
		ImageMiscOps.fill(pattern,1);

		shiftContourCheck(pattern,4,ConnectRule.FOUR);
		shiftContourCheck(pattern,4,ConnectRule.EIGHT);
	}

	@Test
	public void funky1() {
		String s =
				"10\n"+
				"01\n"+
				"10\n";

		GrayU8 pattern = stringToImage(s);

		shiftContourCheck(pattern,4,ConnectRule.EIGHT);
	}

	@Test
	public void funky2() {
		String s =
				"1000\n"+
				"0110\n"+
				"1001\n";

		GrayU8 pattern = stringToImage(s);

		shiftContourCheck(pattern,8,ConnectRule.EIGHT);
	}

	@Test
	public void funky3() {
		String s =
				"0100\n"+
				"0110\n"+
				"1101\n";

		GrayU8 input = stringToImage(s);
		GrayS32 label = new GrayS32(input.width,input.height);

		ContourTracer alg = new ContourTracer(ConnectRule.EIGHT);

		// process the image
		alg.setInputs(addBorder(input),label,queue);
		alg.trace(2,1+1,0+1,true,found);

		assertEquals(7,queue.size);
		assertEquals(7, found.size());
	}

	@Test
	public void funk4() {
		String s =
				"101\n"+
				"111\n"+
				"101\n";

		GrayU8 pattern = stringToImage(s);

		shiftContourCheck(pattern,12,ConnectRule.FOUR);
	}

	@Test
	public void interior1() {
		String s =
				"01110\n"+
				"01101\n"+
				"11110\n";

		GrayU8 input = stringToImage(s);
		GrayS32 label = new GrayS32(input.width,input.height);

		ContourTracer alg = new ContourTracer(ConnectRule.EIGHT);

		// process the image
		alg.setInputs(addBorder(input),label,queue);
		alg.trace(2,3+1,0+1,false,found);

		assertEquals(4, queue.size);
		assertEquals(4,found.size());
	}

	@Test
	public void interior2() {
		String s =
				"01111\n"+
				"01101\n"+
				"11111\n";

		GrayU8 input = stringToImage(s);
		GrayS32 label = new GrayS32(input.width,input.height);

		ContourTracer alg = new ContourTracer(ConnectRule.FOUR);

		// process the image
		alg.setInputs(addBorder(input),label,queue);
		alg.trace(2,3+1,0+1,false,found);

		assertEquals(8, queue.size);
		assertEquals(8,found.size());
	}

	/**
	 * Make sure it is marking surrounding white pixels
	 */
	@Test
	public void checkMarkWhite() {
		String b =
				"000000\n"+
				"001100\n"+
				"000100\n"+
				"000000\n";
		String a =
				"022220\n"+
				"021120\n"+
				"022120\n"+
				"002220\n";

		GrayU8 before = stringToImage(b);
		GrayU8 after = stringToImage(a);
		GrayS32 label = new GrayS32(before.width,before.height);

		ContourTracer alg = new ContourTracer(ConnectRule.EIGHT);

		// process the image
		alg.setInputs(before,label,queue);
		alg.trace(2,2,1,true,found);

		for( int i = 0; i < before.height; i++ ) {
			for( int j = 0; j < before.width; j++ ) {
				if( after.get(j,i) == 2 )
					assertEquals(255,before.get(j,i));
				else
					assertEquals(after.get(j,i),before.get(j,i));
			}
		}
	}

	/**
	 * Given a pattern that is only a contour, it sees if it has the expected results when the pattern
	 * is shifted to every possible location in the image
	 */
	public void shiftContourCheck(GrayU8 pattern , int expectedSize , ConnectRule rule ) {
		ContourTracer alg = new ContourTracer(rule);
		GrayU8 input = new GrayU8(4,5);
		GrayS32 label = new GrayS32(input.width,input.height);

		// exhaustively try all initial locations
		for( int y = 0; y < input.height-pattern.height+1; y++ ) {
			for( int x = 0; x < input.width-pattern.width+1; x++ ) {
				// paste the pattern in to the larger image
				ImageMiscOps.fill(input,0);
				GrayU8 sub = input.subimage(x,y,x+pattern.width,y+pattern.height, null);
				sub.setTo(pattern);

				// reset other data structures
				ImageMiscOps.fill(label,0);
				queue.reset();
				found.clear();

				// process the image
				alg.setInputs(addBorder(input),label,queue);
				alg.trace(2,x+1,y+1,true,found);

				// forward then back
				assertEquals(expectedSize,queue.size);
				assertEquals(expectedSize,found.size());

				// see if the image has been correctly labeled
				for( int yy = 0; yy < input.height; yy++ ) {
					for( int xx = 0; xx < input.width; xx++ ) {
						boolean isOne = false;
						if( pattern.isInBounds(xx-x,yy-y) ) {
							isOne = pattern.get(xx-x,yy-y) == 1;
						}
						if( isOne )
							assertEquals(2,label.get(xx,yy));
						else
							assertEquals(0,label.get(xx,yy));
					}
				}
			}
		}
	}

	private GrayU8 addBorder(GrayU8 original ) {
		GrayU8 border = new GrayU8(original.width+2,original.height+2);
		border.subimage(1,1,border.width-1,border.height-1, null).setTo(original);
		ImageMiscOps.fillBorder(border,0,1);
		return border;
	}

	private GrayU8 stringToImage(String s ) {
		int numCols = s.indexOf('\n');
		int numRows = s.length()/(numCols+1);

		GrayU8 out = new GrayU8(numCols,numRows);
		for( int y = 0; y < numRows; y++ ) {
			for( int x = 0; x < numCols; x++ ) {
				out.set(x,y, Integer.parseInt(""+s.charAt(y*(numCols+1)+x)));
			}
		}

		return out;
	}
}
