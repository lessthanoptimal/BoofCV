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

package boofcv.alg.shapes.polygon;

import boofcv.abst.distort.FDistort;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestBinaryPolygonConvexDetector {

	int width = 400,height=450;

	ImageSingleBand orig;
	ImageSingleBand dist;

	Class imageTypes[] = new Class[]{ImageUInt8.class, ImageFloat32.class};

	List<Quadrilateral_F64> quadsBase = new ArrayList<Quadrilateral_F64>();
	List<Quadrilateral_F64> quadsTrans = new ArrayList<Quadrilateral_F64>();

	Affine2D_F64 transform = new Affine2D_F64();

	@Test
	public void foo() {
		fail("implement");
	}

	public void renderQuads( Class imageType ) {
		orig = GeneralizedImageOps.createSingleBand(imageType,width,height);
		dist = GeneralizedImageOps.createSingleBand(imageType,width,height);

		GImageMiscOps.fill(orig, 200);
		GImageMiscOps.fill(dist, 200);

		for (Quadrilateral_F64 q : quadsBase ) {
			int x0 = (int)(q.a.x+0.5);
			int y0 = (int)(q.a.y+0.5);
			int x1 = (int)(q.c.x+0.5)+1;
			int y1 = (int)(q.c.y+0.5)+1;

			GImageMiscOps.fillRectangle(orig,10,x0,y0,x1-x0,y1-y0);

			Quadrilateral_F64 tran = new Quadrilateral_F64();

			AffinePointOps_F64.transform(transform,q.a,tran.a);
			AffinePointOps_F64.transform(transform,q.b,tran.b);
			AffinePointOps_F64.transform(transform,q.c,tran.c);
			AffinePointOps_F64.transform(transform,q.d,tran.d);
		}

		new FDistort(orig,dist).border(200).affine(transform).apply();

		BufferedImage out = ConvertBufferedImage.convertTo(dist, null, true);
		ShowImages.showWindow(out, "Rendered");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void rejectShapes_circle() {
		fail("Implement");
	}

	@Test
	public void rejectShapes_triangle() {
		fail("Implement");
	}

	@Test
	public void rejectShapes_pentagon() {
		fail("Implement");
	}

	@Test
	public void touchesBorder() {
		fail("Implement");
	}
}
