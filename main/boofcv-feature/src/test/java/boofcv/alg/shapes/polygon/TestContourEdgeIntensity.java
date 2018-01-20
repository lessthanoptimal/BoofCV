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

package boofcv.alg.shapes.polygon;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.RectangleLength2D_I32;
import org.ddogleg.struct.GrowQueue_I32;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestContourEdgeIntensity {
	@Test
	public void simpleCase() {
		GrayU8 image = new GrayU8(200,150);

		RectangleLength2D_I32 r = new RectangleLength2D_I32(20,25,30,35);

		ImageMiscOps.fillRectangle(image,200,r.x0,r.y0,r.width,r.height);

		List<Point2D_I32> contour = rectToContour(r);

		ContourEdgeIntensity<GrayU8> alg =
				new ContourEdgeIntensity<>(20,2,1.0,GrayU8.class);
		alg.setImage(image);

		alg.process(contour,true);
		assertTrue( alg.getOutsideAverage() < 8 );
		assertTrue( alg.getInsideAverage() > 195 );

		// test the CCW flag AND multiple calls
		alg.process(contour,false);
		assertTrue( alg.getOutsideAverage() > 195 );
		assertTrue( alg.getInsideAverage() < 8 );

		// change the number of contour samples
		alg = new ContourEdgeIntensity<>(10,2,1.0,GrayU8.class);
		alg.setImage(image);

		alg.process(contour,true);
		assertTrue( alg.getOutsideAverage() < 8 );
		assertTrue( alg.getInsideAverage() > 195 );

		// change the number of tangent samples
		alg = new ContourEdgeIntensity<>(20,1,1.0,GrayU8.class);
		alg.setImage(image);

		alg.process(contour,true);
		assertTrue( alg.getOutsideAverage() < 8 );
		assertTrue( alg.getInsideAverage() > 195 );
	}

	@Test
	public void smallContours() {
		GrayU8 image = new GrayU8(200,150);

		RectangleLength2D_I32 r = new RectangleLength2D_I32(20,25,5,4);

		ImageMiscOps.fillRectangle(image,200,r.x0,r.y0,r.width,r.height);

		List<Point2D_I32> contour = rectToContour(r);

		ContourEdgeIntensity<GrayU8> alg =
				new ContourEdgeIntensity<>(30,2,1.0,GrayU8.class);
		alg.setImage(image);

		alg.process(contour,true);
		assertTrue( alg.getOutsideAverage() < 8 );
		assertTrue( alg.getInsideAverage() > 195 );
	}

	public static List<Point2D_I32> rectToContour( RectangleLength2D_I32 r ) {
		List<Point2D_I32> contour = new ArrayList<>();

		int x1 = r.x0+r.width-1;
		int y1 = r.y0+r.height-1;

		for (int i = 1; i < r.width; i++) {
			contour.add( new Point2D_I32(r.x0+i, r.y0) );
		}
		for (int i = 1; i < r.height; i++) {
			contour.add( new Point2D_I32(x1, r.y0+i) );
		}
		for (int i = 1; i < r.width; i++) {
			contour.add( new Point2D_I32(x1-i, y1) );
		}
		for (int i = 1; i < r.height; i++) {
			contour.add( new Point2D_I32(r.x0, y1-i) );
		}
		return contour;
	}

	public static GrowQueue_I32 computeContourVertexes(RectangleLength2D_I32 r ) {
		GrowQueue_I32 out = new GrowQueue_I32();
		out.add(r.width-2);
		out.add(r.width+r.height-3);
		out.add(2*r.width+r.height-4);
		out.add(2*r.width+2*r.height-5);
		return out;
	}
}