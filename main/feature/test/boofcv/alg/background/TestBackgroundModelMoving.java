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

package boofcv.alg.background;

import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import georegression.struct.homography.Homography2D_F32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBackgroundModelMoving
{
	@Test
	public void updateBackground() {
		Helper helper = new Helper();

		helper.initialize(400,600,new Homography2D_F32(1,0,100,0,1,150,0,0,1));
		helper.updateBackground(new Homography2D_F32(1,0,-10,0,1,-15,0,0,1),new GrayU8(100,120));
		// tests are contained in helper
	}

	@Test
	public void segment() {
		Helper helper = new Helper();

		helper.initialize(400, 600, new Homography2D_F32(1, 0, 100, 0, 1, 150, 0, 0, 1));
		helper.segment(new Homography2D_F32(1,0,-10,0,1,-15,0,0,1),
				new GrayU8(100,120),new GrayU8(100,120));
	}


	public class Helper extends BackgroundModelMoving<GrayU8,Homography2D_F32> {

		GrayU8 background = new GrayU8(1,1);

		public Helper() {
			super(new PointTransformHomography_F32(), ImageType.single(GrayU8.class));
		}

		@Override
		public void initialize(int backgroundWidth, int backgroundHeight, Homography2D_F32 homeToWorld) {
			this.background.reshape(backgroundWidth,backgroundHeight);
			this.homeToWorld.set(homeToWorld);
			this.homeToWorld.invert(worldToHome);

			this.backgroundWidth = backgroundWidth;
			this.backgroundHeight = backgroundHeight;
		}

		@Override public void reset() {}

		@Override
		protected void updateBackground(int x0, int y0, int x1, int y1, GrayU8 frame) {
			assertEquals(110,x0);
			assertEquals(165,y0);
			assertEquals(210,x1);
			assertEquals(285,y1);

			Homography2D_F32 expected = new Homography2D_F32(1,0,110,0,1,165,0,0,1);
			checkEquals(expected,currentToWorld,1e-8);

			assertTrue(frame != null );
		}

		@Override
		protected void _segment(Homography2D_F32 currentToWorld, GrayU8 frame, GrayU8 segmented) {

			Homography2D_F32 expected = new Homography2D_F32(1,0,110,0,1,165,0,0,1);
			checkEquals(expected,currentToWorld,1e-8);

			assertTrue(frame != null );
			assertTrue(segmented != null );
		}
	}

	public void checkEquals( Homography2D_F32 a , Homography2D_F32 b , double tol ) {
		assertEquals(a.a11,b.a11,tol);
		assertEquals(a.a12,b.a12,tol);
		assertEquals(a.a13,b.a13,tol);
		assertEquals(a.a21,b.a21,tol);
		assertEquals(a.a22,b.a22,tol);
		assertEquals(a.a23,b.a23,tol);
		assertEquals(a.a31,b.a31,tol);
		assertEquals(a.a32,b.a32,tol);
		assertEquals(a.a33,b.a33,tol);
	}
}
