/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.shapes.polyline.keypoint;

import boofcv.alg.shapes.polyline.splitmerge.TestSplitMergeLineFitLoop;
import boofcv.struct.ConfigLength;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestContourInterestPointDetector {

	int period = 5;
	ConfigLength cperiod = ConfigLength.fixed(period);

	@Test
	public void simpleLoop() {
		ContourInterestPointDetector alg = new ContourInterestPointDetector(true, cperiod,1);

		List<Point2D_I32> contour = rect(0,0,10,15);

		List<Point2D_I32> expected = new ArrayList<>();
		expected.add( new Point2D_I32(0,0));
		expected.add( new Point2D_I32(10,0));
		expected.add( new Point2D_I32(10,15));
		expected.add( new Point2D_I32(0,15));

		List<Point2D_I32> found = new ArrayList<>();

		for (int i = 0; i < 15; i++) {
			alg.process(contour);
			alg.getInterestPoints(contour,found);
			int matches = 0;

			assertEquals(expected.size(),found.size());
			for (int j = 0; j < found.size(); j++) {
				for (int k = 0; k < expected.size(); k++) {
					if( expected.get(k).distance(found.get(j)) == 0 ) {
						matches++;
						break;
					}
				}
			}

			assertEquals(expected.size(),matches);

			// shift the contour indexes. This should not change the results
			contour = TestSplitMergeLineFitLoop.shiftContour(contour,1);
		}
	}

	@Test
	public void computeCornerIntensityLoop() {
		ContourInterestPointDetector alg = new ContourInterestPointDetector(true,cperiod,1);

		List<Point2D_I32> contour = rect(0,0,10,15);

		alg.computeCornerIntensityLoop(contour,period);

		GrowQueue_F64 intensity = alg.intensity;
		assertEquals(contour.size(),intensity.size);

		// should be zero where there's no features/a line
		assertEquals(0,intensity.get(5),1e-6);

		// peak should be at the corners
		double peak = intensity.get(0);
		assertTrue(peak>intensity.get(intensity.size-1) && peak>intensity.get(1));

		assertEquals(peak,intensity.get(10),1e-6);
		assertEquals(peak,intensity.get(25),1e-6);
		assertEquals(peak,intensity.get(35),1e-6);
	}

	@Test
	public void nonMaximumSupressionLoop() {
		ContourInterestPointDetector alg = new ContourInterestPointDetector(true,cperiod,1);

		GrowQueue_F64 intensity = GrowQueue_F64.zeros(20);
		intensity.set(19,20.0); // test wrapping
		intensity.set(0,5.0);

		intensity.set(6,2);
		intensity.set(8,5.0); // test the range
		intensity.set(10,2);
		intensity.set(11,3);

		intensity.set(15,3); // none of these should be a max
		intensity.set(16,3);
		intensity.set(17,3);

		alg.intensity = intensity;
		alg.nonMaximumSupressionLoop(period);

		assertEquals(3,alg.indexes.size);
		assertEquals(8,alg.indexes.get(0));
		assertEquals(11,alg.indexes.get(1));
		assertEquals(19,alg.indexes.get(2));
	}

	@Test
	public void simpleSegment() {
		fail("Implement");
	}

	@Test
	public void computeCornerIntensitySegment() {
		fail("Implement");
	}

	@Test
	public void nonMaximumSupressionSegment() {
		fail("Implement");
	}

	public static List<Point2D_I32> rect( int x0 , int y0 , int x1 , int y1 ) {
		List<Point2D_I32> out = new ArrayList<>();

		out.addAll( line(x0,y0,x1,y0));
		out.addAll( line(x1,y0,x1,y1));
		out.addAll( line(x1,y1,x0,y1));
		out.addAll( line(x0,y1,x0,y0));

		return out;
	}

	private static List<Point2D_I32> line( int x0 , int y0 , int x1 , int y1 ) {
		List<Point2D_I32> out = new ArrayList<>();

		int lengthY = Math.abs(y1-y0);
		int lengthX = Math.abs(x1-x0);

		int x,y;
		if( lengthY > lengthX ) {
			for (int i = 0; i < lengthY; i++) {
				x = x0 + (x1-x0)*lengthX*i/lengthY;
				y = y0 + (y1-y0)*i/lengthY;
				out.add( new Point2D_I32(x,y));
			}
		} else {
			for (int i = 0; i < lengthX; i++) {
				x = x0 + (x1-x0)*i/lengthX;
				y = y0 + (y1-y0)*lengthY*i/lengthX;
				out.add( new Point2D_I32(x,y));
			}
		}
		return out;
	}

}
