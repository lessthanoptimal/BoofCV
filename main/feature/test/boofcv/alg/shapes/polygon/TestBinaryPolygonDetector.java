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

package boofcv.alg.shapes.polygon;

import boofcv.abst.distort.FDistort;
import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.ConfigRefinePolygonCornersToImage;
import boofcv.factory.shape.ConfigRefinePolygonLineToImage;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.affine.UtilAffine;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import georegression.transform.affine.AffinePointOps_F64;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;
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
public class TestBinaryPolygonDetector {

	int width = 400,height=450;
	boolean showRendered = false;

	GrayU8 binary = new GrayU8(1,1);
	ImageGray orig;
	ImageGray dist;

	Class imageTypes[] = new Class[]{GrayU8.class, GrayF32.class};

	List<Rectangle2D_I32> rectangles = new ArrayList<>();
	List<Polygon2D_F64> distorted = new ArrayList<>();

	Affine2D_F64 transform = new Affine2D_F64();

	InputToBinary<GrayU8> inputToBinary_U8 = FactoryThresholdBinary.globalFixed(100, true, GrayU8.class);

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

		transform.set(0.8, 0, 0, 0.8, 1, 2);
		transform = transform.invert(null);

		for( Class imageType : imageTypes ) {
			checkDetected_LensDistortion(imageType, true, 0.5);
			checkDetected_LensDistortion(imageType, false, 0.5);
		}
	}

	private void checkDetected_LensDistortion(Class imageType, boolean useLines , double tol) {
		renderDistortedRectangle(imageType);

		Affine2D_F32 a = new Affine2D_F32();
		UtilAffine.convert(transform,a);
		PixelTransform2_F32 tranFrom = new PixelTransformAffine_F32(a);
		PixelTransform2_F32 tranTo = new PixelTransformAffine_F32(a.invert(null));

		int numberOfSides = 4;
		BinaryPolygonDetector alg = createDetector(imageType, useLines, numberOfSides,numberOfSides);
		alg.setLensDistortion(dist.width, dist.height, tranTo, tranFrom);
		alg.process(dist, binary);

		FastQueue<Polygon2D_F64> found = alg.getFoundPolygons();

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
			checkDetected(imageType,true,1e-8);
			checkDetected(imageType,false,1e-8);
		}
	}

	@Test
	public void someAffineDistortion() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		rectangles.add(new Rectangle2D_I32(90,30,120,60));
		rectangles.add(new Rectangle2D_I32(30,90,60,120));
		rectangles.add(new Rectangle2D_I32(90,90,120,120));

		transform.set(1.1, 0.2, 0.12, 1.3, 10.2, 20.3);

		for( Class imageType : imageTypes ) {
			checkDetected(imageType,true,0.3);
			checkDetected(imageType,false,0.5);
		}
	}

	private void checkDetected(Class imageType, boolean useLines,  double tol ) {
		renderDistortedRectangle(imageType);

		int numberOfSides = 4;
		BinaryPolygonDetector alg = createDetector(imageType, useLines, numberOfSides,numberOfSides);
		alg.process(dist, binary);

		FastQueue<Polygon2D_F64> found = alg.getFoundPolygons();

		assertEquals(rectangles.size(), found.size);

		for (int i = 0; i < found.size; i++) {
			assertEquals(1,findMatches(found.get(i),tol));
		}
	}

	@Test
	public void easyTestMultipleShapes() {

		List<Polygon2D_F64> polygons = new ArrayList<>();
		polygons.add(new Polygon2D_F64(20, 20, 40, 50, 80, 20));
		polygons.add(new Polygon2D_F64(20, 60, 20, 90, 40, 90,40, 60));

		for( Class imageType : imageTypes ) {
			checkDetectedMulti(imageType, polygons, true,1.5);
			checkDetectedMulti(imageType, polygons, false,2);
		}
	}

	private void checkDetectedMulti(Class imageType,List<Polygon2D_F64> polygons, boolean useLines,  double tol ) {
		renderPolygons(polygons,imageType);

		BinaryPolygonDetector alg = createDetector(imageType, useLines, 3,4);
		alg.process(dist, binary);

		FastQueue<Polygon2D_F64> found = alg.getFoundPolygons();

		assertEquals(polygons.size(), found.size);

		for (int i = 0; i < found.size; i++) {
			assertEquals(1,findMatches(found.get(i),tol));
		}
	}

	private <T extends ImageGray> BinaryPolygonDetector<T> createDetector(Class<T> imageType, boolean useLines, int minSides, int maxSides) {
		ConfigPolygonDetector config = new ConfigPolygonDetector(minSides,maxSides);

		if( useLines ) {
			config.refine = new ConfigRefinePolygonLineToImage();
		} else {
			config.refine = new ConfigRefinePolygonCornersToImage();
		}

		return FactoryShapeDetector.polygon(config,imageType);
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


	public void renderPolygons( List<Polygon2D_F64> polygons, Class imageType ) {
		InputToBinary inputToBinary = FactoryThresholdBinary.globalFixed(100, true, imageType);

		BufferedImage work = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		Graphics2D g2 = work.createGraphics();

		g2.setColor(Color.WHITE);
		g2.fillRect(0, 0, width, height);
		g2.setColor(Color.BLACK);

		distorted.clear();
		for (int i = 0; i < polygons.size(); i++) {
			Polygon2D_F64 orig = polygons.get(i);

			int x[] = new int[ orig.size() ];
			int y[] = new int[ orig.size() ];

			for (int j = 0; j < orig.size(); j++) {
				x[j] = (int)orig.get(j).x;
				y[j] = (int)orig.get(j).y;
			}

			g2.fillPolygon(x,y,orig.size());

			distorted.add( orig );
		}

		dist = GeneralizedImageOps.createSingleBand(imageType, width, height);
		binary = new GrayU8(width,height);

		ConvertBufferedImage.convertFrom(work,dist,true);

		inputToBinary.process(dist,binary);

		if( showRendered ) {
			ShowImages.showWindow(work, "Rendered");
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void renderDistortedRectangle( Class imageType ) {
		InputToBinary inputToBinary = FactoryThresholdBinary.globalFixed(100, true, imageType);

		orig = GeneralizedImageOps.createSingleBand(imageType,width,height);
		dist = GeneralizedImageOps.createSingleBand(imageType,width,height);
		binary.reshape(width,height);

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

		inputToBinary.process(dist,binary);

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
		g2.fillOval(30, 30, 90, 100);

		GrayU8 gray = ConvertBufferedImage.convertFrom(work,(GrayU8)null);
		binary.reshape(gray.width,gray.height);
		inputToBinary_U8.process(gray,binary);

		for (int i = 3; i <= 6; i++) {
			BinaryPolygonDetector alg = createDetector(GrayU8.class, true, i,i);

			alg.process(gray,binary);
			assertEquals("num sides = "+i,0,alg.getFoundPolygons().size());
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

		GrayU8 gray = ConvertBufferedImage.convertFrom(work,(GrayU8)null);
		binary.reshape(gray.width,gray.height);
		inputToBinary_U8.process(gray,binary);

		for (int i = 3; i <= 6; i++) {
			BinaryPolygonDetector alg = createDetector(GrayU8.class, true, i, i);

			alg.process(gray,binary);
			if( i == 3 ) {
				double tol = 0.5;
				assertEquals(1, alg.getFoundPolygons().size());
				Polygon2D_F64 found = (Polygon2D_F64)alg.getFoundPolygons().get(0);
				checkPolygon(new double[]{10, 10, 30, 40, 50, 10}, found);
			} else
				assertEquals(0,alg.getFoundPolygons().size());
		}
	}

	public static boolean checkPolygon( double[] expected , Polygon2D_F64 found  ) {
		for (int i = 0; i < found.size(); i++) {


			boolean matched = true;
			for (int j = 0; j < found.size(); j++) {
				double x = expected[j*2];
				double y = expected[j*2+1];

				Point2D_F64 p = found.get((i+j)%found.size());

				if( Math.abs(p.x-x) > 1e-5 || Math.abs(p.y-y) > 1e-5 ) {
					matched = false;
					break;
				}
			}
			if( matched )
				return true;
		}
		return false;
	}

	/**
	 * Configure the detector to reject concave shapes
	 */
	@Test
	public void rejectShapes_concave() {
		List<Polygon2D_F64> polygons = new ArrayList<>();
		polygons.add(new Polygon2D_F64(20,20, 80,20, 80,80, 40,40, 20,80));

		for( Class imageType : imageTypes ) {
			renderPolygons(polygons, imageType);

			BinaryPolygonDetector alg = createDetector(imageType, true, 5,5);

			alg.process(dist,binary);

			assertEquals(0,alg.getFoundPolygons().size());
		}
	}

	/**
	 * Give it an easy to detect concave shape
	 */
	@Test
	public void detect_concave() {
		List<Polygon2D_F64> polygons = new ArrayList<>();
		Polygon2D_F64 expected = new Polygon2D_F64(20, 20, 20, 80, 40, 40, 80, 80, 80, 20);
		polygons.add(expected);

		for( Class imageType : imageTypes ) {
			renderPolygons(polygons,imageType );

			BinaryPolygonDetector alg = createDetector(imageType, true, 5,5);
			alg.setConvex(false);

			alg.process(dist, binary);

			assertEquals(1, alg.getFoundPolygons().size());

			Polygon2D_F64 found = (Polygon2D_F64)alg.getFoundPolygons().get(0);
			assertEquals(1, findMatches(found, 1));
		}
	}

	@Test
	public void touchesBorder_false() {
		List<Point2D_I32> contour = new ArrayList<>();

		BinaryPolygonDetector alg = createDetector(GrayU8.class, true, 4,4);
		alg.getLabeled().reshape(20,30);
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

	/**
	 * When an adaptive threshold is used, the area around a bright light gets marked as "dark" then when
	 * the polygon is fit to it it can snap around the white object
	 */
	@Test
	public void snapToBrightObject() {
		GrayU8 gray = new GrayU8(200,200);
		GrayU8 binary = new GrayU8(200,200);

		ImageMiscOps.fillRectangle(gray,200,40,40,40,40);
		ImageMiscOps.fillRectangle(binary,1,38,38,44,44);
		ImageMiscOps.fillRectangle(binary,0,40,40,40,40);

		BinaryPolygonDetector<GrayU8> alg = createDetector(GrayU8.class, true, 4,4);

		// edge threshold test will only fail now if the sign is reversed
		alg.edgeThreshold = 0;

		alg.process(gray,binary);

		assertEquals(0,alg.getFoundPolygons().size);
	}

	@Test
	public void determineCornersOnBorder() {
		BinaryPolygonDetector alg = createDetector(GrayU8.class, true, 4,4);
		alg.getLabeled().reshape(width,height);

		Polygon2D_F64 poly = new Polygon2D_F64(0,0, 10,0, 10,10, 0,10);

		GrowQueue_B corners = new GrowQueue_B();
		alg.determineCornersOnBorder(poly,corners,0.5f);

		assertEquals(4,corners.size());

		assertEquals(true,corners.get(0));
		assertEquals(true,corners.get(1));
		assertEquals(false,corners.get(2));
		assertEquals(true,corners.get(3));
	}

	@Test
	public void isUndistortedOnBorder() {
		Affine2D_F32 a = new Affine2D_F32();
		transform.set(1.2,0,0,1.2,0,0);
		UtilAffine.convert(transform,a);

		PixelTransform2_F32 tranFrom = new PixelTransformAffine_F32(a);
		PixelTransform2_F32 tranTo = new PixelTransformAffine_F32(a.invert(null));

		BinaryPolygonDetector alg = createDetector(GrayU8.class, true, 4,4);
		alg.undistToDist = tranFrom;
		alg.distToUndist = tranTo;
		alg.getLabeled().reshape(width,height);

		List<Point2D_I32> positive = new ArrayList<>();
		positive.add( new Point2D_I32(20,0));
		positive.add( new Point2D_I32(width-1,30));
		positive.add( new Point2D_I32(0,30));
		positive.add( new Point2D_I32(20,height-1));

		for( Point2D_I32 p : positive ) {
			alg.distToUndist.compute(p.x,p.y);
			float x = alg.distToUndist.distX;
			float y = alg.distToUndist.distY;
			assertTrue(alg.isUndistortedOnBorder(new Point2D_F64(x,y),0.7f));
		}

		List<Point2D_I32> negative = new ArrayList<>();
		negative.add( new Point2D_I32(20,5));
		negative.add( new Point2D_I32(width-3,30));
		negative.add( new Point2D_I32(7,30));
		negative.add( new Point2D_I32(20,height-4));
		for( Point2D_I32 p : negative ) {
			alg.distToUndist.compute(p.x,p.y);
			float x = alg.distToUndist.distX;
			float y = alg.distToUndist.distY;
			assertFalse(alg.isUndistortedOnBorder(new Point2D_F64(x,y),0.7f));
		}
	}
}
