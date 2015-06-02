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
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.shapes.SplitMergeLineFitLoop;
import boofcv.alg.shapes.corner.SubpixelSparseCornerFit;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.transform.affine.AffinePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestBinaryPolygonConvexDetector {

	int width = 400,height=450;
	boolean showRendered = false;

	ImageSingleBand orig;
	ImageSingleBand dist;

	Class imageTypes[] = new Class[]{ImageUInt8.class, ImageFloat32.class};

	List<Rectangle2D_I32> rectangles = new ArrayList<Rectangle2D_I32>();
	List<Polygon2D_F64> distorted = new ArrayList<Polygon2D_F64>();

	Affine2D_F64 transform = new Affine2D_F64();

	@Before
	public void before() {
		rectangles.clear();
		transform.reset();
	}

	@Test
	public void easyTestNoDistortion() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		rectangles.add(new Rectangle2D_I32(90,30,120,60));
		rectangles.add(new Rectangle2D_I32(30,90,60,120));
		rectangles.add(new Rectangle2D_I32(90,90,120,120));

		for( Class imageType : imageTypes ) {
			checkDetected(imageType,1e-8);
		}
	}

	@Test
	public void someAffineDistortion() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		rectangles.add(new Rectangle2D_I32(90,30,120,60));
		rectangles.add(new Rectangle2D_I32(30,90,60,120));
		rectangles.add(new Rectangle2D_I32(90,90,120,120));

		transform.set(1.1,0.2,0.12,1.3,10.2,20.3);

		for( Class imageType : imageTypes ) {
			checkDetected(imageType,0.3);
		}
	}

	private void checkDetected(Class imageType, double tol ) {
		renderDistortedRectangle(imageType);

		int numberOfSides = 4;
		BinaryPolygonConvexDetector alg = createDetector(imageType, numberOfSides);

		alg.process(dist);

		FastQueue<Polygon2D_F64> found = alg.getFound();

		assertEquals(rectangles.size(),found.size);

		for (int i = 0; i < found.size; i++) {
			assertEquals(1,findMatches(found.get(i),tol));
		}
	}

	private BinaryPolygonConvexDetector createDetector(Class imageType, int numberOfSides) {
		InputToBinary inputToBinary = FactoryThresholdBinary.globalFixed(100, true, imageType);
		SplitMergeLineFitLoop contourToPolygon = new SplitMergeLineFitLoop(0,0.1,40);
		RefinePolygonLineToImage refineLine = new RefinePolygonLineToImage(numberOfSides,true,imageType);
		SubpixelSparseCornerFit refineCorner = new SubpixelSparseCornerFit(imageType);
		return new BinaryPolygonConvexDetector(numberOfSides,inputToBinary,contourToPolygon,
				refineLine,null,0.23,0.03,true,imageType);
	}

	private int findMatches( Polygon2D_F64 found , double tol ) {
		int match = 0;
		for (int i = 0; i < distorted.size(); i++) {
			if(UtilPolygons2D_F64.isEquivalent(found, distorted.get(i),tol))
				match++;
		}
		return match;
	}

	public void renderDistortedRectangle( Class imageType ) {
		orig = GeneralizedImageOps.createSingleBand(imageType,width,height);
		dist = GeneralizedImageOps.createSingleBand(imageType,width,height);

		GImageMiscOps.fill(orig, 200);
		GImageMiscOps.fill(dist, 200);

		distorted.clear();
		for (Rectangle2D_I32 q : rectangles) {

			GImageMiscOps.fillRectangle(orig,10,q.x0,q.y0,q.x1-q.x0,q.y1-q.y0);

			Polygon2D_F64 tran = new Polygon2D_F64(4);

			AffinePointOps_F64.transform(transform,q.x0,q.y0,tran.get(0));
			AffinePointOps_F64.transform(transform,q.x0,q.y1,tran.get(1));
			AffinePointOps_F64.transform(transform,q.x1,q.y1,tran.get(2));
			AffinePointOps_F64.transform(transform,q.x1,q.y0,tran.get(3));

			distorted.add(tran);
		}

		new FDistort(orig,dist).border(200).affine(transform).apply();

		if( showRendered ) {
			BufferedImage out = ConvertBufferedImage.convertTo(dist, null, true);
			ShowImages.showWindow(out, "Rendered");
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
