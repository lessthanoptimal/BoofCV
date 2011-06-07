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

import gecv.struct.distort.PixelDistort;
import pja.geometry.struct.affine.Affine2D_F32;
import pja.geometry.struct.point.Point2D_F32;
import pja.geometry.transform.affine.AffinePointOps;


/**
 * @author Peter Abeles
 */
public class PixelDistortAffine extends PixelDistort {

	Affine2D_F32 affine = new Affine2D_F32();
	Point2D_F32 tran = new Point2D_F32();

	public void set( Affine2D_F32 affine ) {
		this.affine.set(affine);
	}


	@Override
	public void distort(int x, int y) {
		AffinePointOps.transform(affine,x,y,tran);
		distX = tran.x;
		distY = tran.y;
	}
}
