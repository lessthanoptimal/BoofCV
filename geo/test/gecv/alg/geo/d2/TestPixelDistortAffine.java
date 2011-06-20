/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.geo.d2;

import jgrl.struct.affine.Affine2D_F32;
import jgrl.struct.affine.Affine2D_F64;
import jgrl.struct.point.Point2D_F32;
import jgrl.struct.point.Point2D_F64;
import jgrl.transform.affine.AffinePointOps;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * @author Peter Abeles
 */
public class TestPixelDistortAffine {
	@Test
	public void constructor_32() {
		Affine2D_F32 a = new Affine2D_F32(1,2,3,4,5,6);

		PixelDistortAffine alg = new PixelDistortAffine();
		alg.set(a);

		alg.distort(2,3);
		Point2D_F32 p = new Point2D_F32(2,3);
		Point2D_F32 expected = new Point2D_F32();
		AffinePointOps.transform(a,p,expected);

		assertEquals(expected.x,alg.distX,1e-4);
		assertEquals(expected.y,alg.distY,1e-4);
	}

	@Test
	public void constructor_64() {
		Affine2D_F64 a = new Affine2D_F64(1,2,3,4,5,6);

		PixelDistortAffine alg = new PixelDistortAffine();
		alg.set(a);

		alg.distort(2,3);
		Point2D_F64 p = new Point2D_F64(2,3);
		Point2D_F64 expected = new Point2D_F64();
		AffinePointOps.transform(a,p,expected);

		assertEquals(expected.x,alg.distX,1e-4);
		assertEquals(expected.y,alg.distY,1e-4);
	}
}
