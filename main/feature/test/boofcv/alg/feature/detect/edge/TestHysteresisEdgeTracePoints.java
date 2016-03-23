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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS8;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestHysteresisEdgeTracePoints extends CommonHysteresisEdgeTrace {

	@Test
	public void test0() {
		standardTest(0);
	}

	@Test
	public void test1() {
		standardTest(1);
	}

	@Test
	public void test2() {
		GrayS8 dir = direction(2);

		HysteresisEdgeTracePoints alg = new HysteresisEdgeTracePoints();

		alg.process(intensity(2),dir,3,5);
		assertEquals(3, ImageStatistics.sum(convert(alg.getContours(),dir.width,dir.height)));

		alg.process(intensity(2),dir,2,5);
		assertEquals(4, ImageStatistics.sum(convert(alg.getContours(),dir.width,dir.height)));
	}

	@Test
	public void test3() {
		standardTest(3);
	}

	@Test
	public void test4() {
		standardTest(4);
	}

	private void standardTest( int which ) {
		GrayF32 inten = intensity(which);
		GrayS8 dir = direction(which);

		HysteresisEdgeTracePoints alg = new HysteresisEdgeTracePoints();

		alg.process(inten,dir,2,5);
		GrayU8 out = convert(alg.getContours(),inten.width,inten.height);

		BoofTesting.assertEquals(expected(which), out, 0);
	}

	private GrayU8 convert(List<EdgeContour> contour , int w , int h ) {
		GrayU8 out = new GrayU8(w,h);
		for( EdgeContour e : contour ) {
			for( EdgeSegment s : e.segments ) {
				for(Point2D_I32 p : s.points ) {
					out.set(p.x,p.y,1);
				}
			}
		}
		return out;
	}

}
