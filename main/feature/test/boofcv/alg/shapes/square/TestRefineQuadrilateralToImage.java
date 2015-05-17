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

package boofcv.alg.shapes.square;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.ImageSingleBand;
import georegression.geometry.UtilLine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestRefineQuadrilateralToImage {

	int width = 400, height = 500;
	ImageSingleBand work; // original image before homography has been applied
	ImageSingleBand image; // image after homography applied

	int x0 = 200, y0 = 160;
	int x1 = 260, y1 = 700;

	@Test
	public void all() {
		fail("implement");
	}

	/**
	 * Give it a shape which is too small and see if it fails
	 */
	@Test
	public void all_tooSmall() {
		fail("implement");
	}

	@Test
	public void localToImage() {
		fail("implement");
	}

	@Test
	public void optimize() {
		fail("implement");
	}

	@Test
	public void computePointsAndWeights() {
		// white and dark
		fail("implement");
	}

	@Test
	public void convert() {
		Quadrilateral_F64 orig = new Quadrilateral_F64(10,20,30,21,19.5,-10,8,-8);

		LineGeneral2D_F64[] lines = new LineGeneral2D_F64[4];
		lines[0] = UtilLine2D_F64.convert(new LineSegment2D_F64(orig.a,orig.b),(LineGeneral2D_F64)null);
		lines[1] = UtilLine2D_F64.convert(new LineSegment2D_F64(orig.b,orig.c),(LineGeneral2D_F64)null);
		lines[2] = UtilLine2D_F64.convert(new LineSegment2D_F64(orig.c,orig.d),(LineGeneral2D_F64)null);
		lines[3] = UtilLine2D_F64.convert(new LineSegment2D_F64(orig.d,orig.a),(LineGeneral2D_F64)null);

		Quadrilateral_F64 found = new Quadrilateral_F64();
		RefineQuadrilateralToImage.convert(lines,found);

		assertTrue(orig.a.distance(found.a) <= 1e-8);
		assertTrue(orig.b.distance(found.b)<=1e-8);
		assertTrue(orig.c.distance(found.c)<=1e-8);
		assertTrue(orig.d.distance(found.d) <= 1e-8);
	}

	private void setup( DenseMatrix64F H , Class imageType ) {
		work = GeneralizedImageOps.createSingleBand(imageType,width,height);
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);

		GImageMiscOps.fill(work,0);
		GImageMiscOps.fillRectangle(work, 200, x0, y0, x1 - x0, y1 - y0);

		GImageMiscOps.fill(image, 0);
//		DistortImageOps.affine(work,image, TypeInterpolate.BILINEAR,);
	}

}
