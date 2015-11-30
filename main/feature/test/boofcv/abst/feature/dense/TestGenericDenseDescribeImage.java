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

package boofcv.abst.feature.dense;

import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestGenericDenseDescribeImage {

	/**
	 * Give it a known situation and see if it produces the expected results
	 */
	@Test
	public void process() {
		DummyFeature sparse = new DummyFeature();

		GenericDenseDescribeImageDense alg = new GenericDenseDescribeImageDense(sparse,1.5,7,3,4);

		FastQueue<TupleDesc_F64> descs = new FastQueue<TupleDesc_F64>(TupleDesc_F64.class,true) {
			@Override
			protected TupleDesc_F64 createInstance() {
				return new TupleDesc_F64(5);
			}
		};
		FastQueue<Point2D_I32> points = new FastQueue<Point2D_I32>(Point2D_I32.class,true);

		ImageUInt8 image = new ImageUInt8(100,110);

		alg.process(image,descs,points);

		int w = (100-3)/3;
		int h = (110-3)/4;

		// -1 since it intentionally skips feature 20
		assertEquals(w*h-1,points.size);

		int count = 0;
		for (int y = 0; y < h; y++) {
			int pixelY = 7/2 + y*y;
			for (int x = 0; x < w; x++) {
				int pixelX = 7/2 + x*3;

				Point2D_I32 p = null;
				if( count < 19 ) {
					p = points.get(count);
				} else if( count > 20 ) {
					p = points.get(count+1);
				} else {
					continue;
				}
				assertEquals("count = "+count,pixelX,p.x);
				assertEquals(pixelY,p.y);
				count++;
			}
		}
	}

	public static class DummyFeature implements DescribeRegionPoint {

		ImageType type = ImageType.single(ImageUInt8.class);
		double inputRadius;
		ImageBase image;
		FastQueue<Point2D_I32> points = new FastQueue<Point2D_I32>(Point2D_I32.class,true);
		int count = 0;

		@Override
		public void setImage(ImageBase image) {
			this.image = image;
		}

		@Override
		public boolean process(double x, double y, double orientation, double radius, TupleDesc description) {
			assertTrue(description!=null);
			if( ++count != 20 ) {
				points.grow().set((int) x, (int) y);
				inputRadius = radius;
				return true;
			} else {
				return false;
			}
		}

		@Override public boolean requiresRadius() {return false;}

		@Override public boolean requiresOrientation() {return false;}

		@Override
		public ImageType getImageType() {
			return type;
		}

		@Override
		public double getCanonicalWidth() {
			throw new RuntimeException("Foo");
		}

		@Override
		public TupleDesc createDescription() {
			return new TupleDesc_F64(10);
		}

		@Override
		public Class getDescriptionType() {
			return TupleDesc_F64.class;
		}
	}
}
