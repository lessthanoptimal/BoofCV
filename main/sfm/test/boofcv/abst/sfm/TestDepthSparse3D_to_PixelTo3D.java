/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.sfm;

import boofcv.alg.sfm.DepthSparse3D;
import georegression.struct.point.Point3D_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDepthSparse3D_to_PixelTo3D {
	@Test
	public void checkInputs() {
		Dummy depthSparse = new Dummy();

		DepthSparse3D_to_PixelTo3D alg = new DepthSparse3D_to_PixelTo3D(depthSparse);

		assertTrue(alg.process(10, 11));

		assertEquals(10,depthSparse.requestX);
		assertEquals(11,depthSparse.requestY);

		assertEquals(1,alg.getX(),1e-8);
		assertEquals(2,alg.getY(),1e-8);
		assertEquals(3,alg.getZ(),1e-8);
		assertEquals(1,alg.getW(),1e-8);
	}

	public class Dummy extends DepthSparse3D {

		public int requestX, requestY;

		public Dummy() {
			super(1);
		}

		@Override
		public boolean process(int x, int y) {
			this.requestX = x;
			this.requestY = y;

			return true;
		}

		@Override
		public Point3D_F64 getWorldPt() {
			return new Point3D_F64(1,2,3);
		}

		@Override
		protected double lookupDepth(int depthX, int depthY) {
			return 0;
		}
	}
}
