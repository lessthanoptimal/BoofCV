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

package boofcv.alg.shapes.edge;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.struct.image.GrayU8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestEdgeIntensityPolygon {
	@Test
	public void computeEdge() {
		GrayU8 image = new GrayU8(400,500);

		int value = 200;
		ImageMiscOps.fillRectangle(image,value,20,30,40,40);

		EdgeIntensityPolygon<GrayU8> alg = new EdgeIntensityPolygon<>(2,2,10,GrayU8.class);

		Polygon2D_F64 polygon = new Polygon2D_F64(4);

		UtilPolygons2D_F64.convert(new Rectangle2D_F64(20,30,60,70),polygon);

		alg.setImage(image);
		assertTrue(alg.computeEdge(polygon,polygon.isCCW()));
		// should be average pixel intensity inside and outside
		assertEquals(200,alg.getAverageInside(),1e-8);
		assertEquals(0,alg.getAverageOutside(),1e-8);

		// see what happens if the incorrect orientation is passed in
		assertTrue(alg.computeEdge(polygon,!polygon.isCCW()));
		assertEquals(0,alg.getAverageInside(),1e-8);
		assertEquals(200,alg.getAverageOutside(),1e-8);
	}

	@Test
	public void checkIntensity() {
		GrayU8 image = new GrayU8(400,500);

		int value = 200;
		ImageMiscOps.fillRectangle(image,value,20,30,40,40);

		EdgeIntensityPolygon<GrayU8> alg = new EdgeIntensityPolygon<>(2,2,10,GrayU8.class);

		Polygon2D_F64 polygon = new Polygon2D_F64(4);
		UtilPolygons2D_F64.convert(new Rectangle2D_F64(20,30,60,70),polygon);

		alg.setImage(image);
		assertTrue(alg.computeEdge(polygon,polygon.isCCW()));

		assertTrue(alg.checkIntensity(false,50));
		assertFalse(alg.checkIntensity(true,50));
	}
}
