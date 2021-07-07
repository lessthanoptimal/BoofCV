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

package boofcv.alg.interpolate;

import boofcv.struct.border.ImageBorder;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F32;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Peter Abeles
 */
public class TestInterpolatePixelDistortS extends BoofStandardJUnit {
	@Test void simple() {
		MockTransform transform = new MockTransform(1,2);
		MockInterp interp = new MockInterp();

		InterpolatePixelDistortS alg = new InterpolatePixelDistortS(interp,transform);

		assertEquals(73,alg.get(2,5), UtilEjml.TEST_F32);
		assertEquals(74,alg.get_fast(2,5), UtilEjml.TEST_F32);
		assertNotNull(alg.getImageType());

	}

	public class MockTransform implements Point2Transform2_F32 {

		float tx,ty;

		public MockTransform(float tx, float ty) {
			this.tx = tx;
			this.ty = ty;
		}

		@Override
		public void compute(float x, float y, Point2D_F32 out) {
			out.x = x + tx;
			out.y = y + ty;
		}

		@Override
		public Point2Transform2_F32 copyConcurrent() {
			return null;
		}
	}

	public class MockInterp implements InterpolatePixelS {

		@Override
		public float get(float x, float y) {
			return y*10+x;
		}

		@Override
		public float get_fast(float x, float y) {
			return get(x,y)+1;
		}

		@Override
		public InterpolatePixelS copy() {
			return null;
		}

		@Override
		public void setBorder(ImageBorder border) {}

		@Override
		public ImageBorder getBorder() {return null;}

		@Override
		public void setImage(ImageBase image) {}

		@Override
		public ImageBase getImage() {return null;}

		@Override
		public boolean isInFastBounds(float x, float y) {return false;}

		@Override
		public int getFastBorderX() {return 0;}

		@Override
		public int getFastBorderY() {return 0;}

		@Override
		public ImageType getImageType() {return ImageType.SB_U8;}
	}
}
