/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestPolygonEdgeScore {
	@Test
	public void basic() {
		ImageUInt8 image = new ImageUInt8(400,500);

		ImageMiscOps.fillRectangle(image,200,20,30,40,40);

		PolygonEdgeScore<ImageUInt8> alg = new PolygonEdgeScore<ImageUInt8>(2,2,10,49,ImageUInt8.class);

		Polygon2D_F64 polygon = new Polygon2D_F64(4);

		UtilPolygons2D_F64.convert(new Rectangle2D_F64(20,30,60,70),polygon);

		alg.setImage(image);
		assertTrue(alg.validate(polygon));
		assertEquals(50, alg.getAverageEdgeIntensity(), 1e-8);

		UtilPolygons2D_F64.convert(new Rectangle2D_F64(24, 30, 60, 70), polygon);

		// test a negative case
		assertFalse(alg.validate(polygon));
		assertEquals(37.5, alg.getAverageEdgeIntensity(), 1e-8);
	}
}
