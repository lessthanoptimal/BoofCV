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
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.affine.UtilAffine;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.transform.affine.AffinePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

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

	/**
	 * See if it uses the provided lens distortion transforms correctly.  The distortion applied
	 * is actually the affine transform instead of lens distortion.  It should find the original
	 * rectangles.
	 */
	@Test
	public void usingSetLensDistortion() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		rectangles.add(new Rectangle2D_I32(90,30,120,60));
		rectangles.add(new Rectangle2D_I32(30,90,60,120));
		rectangles.add(new Rectangle2D_I32(90,90,120,120));

		transform.set(0.8,0,0,0.8,1,2);

		for( Class imageType : imageTypes ) {
			checkDetected_LensDistortion(imageType, 0.5);
		}
	}

	private void checkDetected_LensDistortion(Class imageType, double tol) {
		renderDistortedRectangle(imageType);

		Affine2D_F32 a = new Affine2D_F32();
		UtilAffine.convert(transform,a);
		PixelTransform_F32 tranFrom = new PixelTransformAffine_F32(a);
		PixelTransform_F32 tranTo = new PixelTransformAffine_F32(a.invert(null));

		int numberOfSides = 4;
		BinaryPolygonConvexDetector alg = createDetector(imageType, numberOfSides);
		alg.setLensDistortion(dist.width,dist.height,tranTo,tranFrom);
		alg.process(dist);

		FastQueue<Polygon2D_F64> found = alg.getFound();

		assertEquals(rectangles.size(),found.size);

		for (int i = 0; i < found.size; i++) {
			assertEquals(1, findMatchesOriginal(found.get(i), tol));
		}
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
		return FactoryShapeDetector.polygon(inputToBinary,
				new ConfigPolygonDetector(numberOfSides),imageType);
	}

	/**
	 * Compare found rectangle against rectangles in the original undistorted image
	 */
	private int findMatchesOriginal(Polygon2D_F64 found, double tol) {
		int match = 0;
		for (int i = 0; i < rectangles.size(); i++) {
			Rectangle2D_I32 ri = rectangles.get(i);
			Rectangle2D_F64 r = new Rectangle2D_F64(ri.x0,ri.y0,ri.x1,ri.y1);
			Polygon2D_F64 p = new Polygon2D_F64(4);
			UtilPolygons2D_F64.convert(r,p);
			if( p.isCCW() )
				p.flip();

			if(UtilPolygons2D_F64.isEquivalent(found,p,tol))
				match++;
		}
		return match;
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
		BufferedImage work = new BufferedImage(200,220,BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = work.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,200,220);
		g2.setColor(Color.BLACK);
		g2.fillOval(30, 30, 40, 50);

		ImageUInt8 gray = ConvertBufferedImage.convertFrom(work,(ImageUInt8)null);

		for (int i = 3; i <= 6; i++) {
			BinaryPolygonConvexDetector alg = createDetector(ImageUInt8.class, i);

			alg.process(gray);
			assertEquals("num sides = "+i,0,alg.getFound().size());
		}
	}

	@Test
	public void rejectShapes_triangle() {
		BufferedImage work = new BufferedImage(200,220,BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = work.createGraphics();
		g2.setColor(Color.WHITE);
		g2.fillRect(0,0,200,220);
		g2.setColor(Color.BLACK);
		g2.fillPolygon(new int[]{10, 50, 30}, new int[]{10, 10, 40}, 3);

		ImageUInt8 gray = ConvertBufferedImage.convertFrom(work,(ImageUInt8)null);

		for (int i = 3; i <= 6; i++) {
			BinaryPolygonConvexDetector alg = createDetector(ImageUInt8.class, i);

			alg.process(gray);
			if( i == 3 ) {
				double tol = 0.5;
				assertEquals(1, alg.getFound().size());
				Polygon2D_F64 found = (Polygon2D_F64)alg.getFound().get(0);
				assertEquals(10,found.get(0).x,tol);
				assertEquals(10,found.get(0).y,tol);
				assertEquals(30,found.get(1).x,tol);
				assertEquals(40,found.get(1).y,tol);
				assertEquals(50,found.get(2).x,tol);
				assertEquals(10,found.get(2).y,tol);
			} else
				assertEquals(0,alg.getFound().size());
		}
	}


	@Test
	public void touchesBorder_false() {
		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();

		BinaryPolygonConvexDetector alg = createDetector(ImageUInt8.class, 4);
		alg.getBinary().reshape(20,30);
		assertFalse(alg.touchesBorder(contour));

		contour.add(new Point2D_I32(10,1));
		assertFalse(alg.touchesBorder(contour));
		contour.add(new Point2D_I32(10,28));
		assertFalse(alg.touchesBorder(contour));
		contour.add(new Point2D_I32(1,15));
		assertFalse(alg.touchesBorder(contour));
		contour.add(new Point2D_I32(18,15));
		assertFalse(alg.touchesBorder(contour));
	}

	@Test
	public void touchesBorder_true() {
		List<Point2D_I32> contour = new ArrayList<Point2D_I32>();

		BinaryPolygonConvexDetector alg = createDetector(ImageUInt8.class, 4);
		alg.getBinary().reshape(20,30);

		contour.add(new Point2D_I32(10,0));
		assertTrue(alg.touchesBorder(contour));
		contour.clear();contour.add(new Point2D_I32(10, 29));
		assertTrue(alg.touchesBorder(contour));
		contour.clear();contour.add(new Point2D_I32(0,15));
		assertTrue(alg.touchesBorder(contour));
		contour.clear();contour.add(new Point2D_I32(19,15));
		assertTrue(alg.touchesBorder(contour));
	}
}
