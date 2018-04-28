/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonFromContour;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.UtilAffine;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_B;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Peter Abeles
 */
public class TestDetectPolygonFromContour extends CommonFitPolygonChecks {
	
	GrayU8 binary = new GrayU8(1,1);

	InputToBinary<GrayU8> inputToBinary_U8 = FactoryThresholdBinary.globalFixed(100, true, GrayU8.class);

	public TestDetectPolygonFromContour() {
		this.fittingToBinaryImage = true;
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
			checkDetected_LensDistortion(imageType, 0.5);
		}
	}

	private void checkDetected_LensDistortion(Class imageType, double tol) {
		renderDistortedRectangles(true,imageType);

		Affine2D_F32 a = new Affine2D_F32();
		UtilAffine.convert(transform,a);
		PixelTransform2_F32 tranFrom = new PixelTransformAffine_F32(a);
		PixelTransform2_F32 tranTo = new PixelTransformAffine_F32(a.invert(null));

		int numberOfSides = 4;
		DetectPolygonFromContour alg = createDetector(imageType, numberOfSides,numberOfSides);
		alg.setLensDistortion(image.width, image.height, tranTo, tranFrom);
		alg.process(image, binary);

		FastQueue<DetectPolygonFromContour.Info> found = alg.getFound();

		assertEquals(rectangles.size(),found.size);

		for (int i = 0; i < found.size; i++) {
			Polygon2D_F64 p = found.get(i).polygon;
			assertEquals(1, findMatchesOriginal(p, tol));
			assertEquals(black,found.get(i).edgeInside,3);
			assertEquals(white,found.get(i).edgeOutside,white*0.05);
		}
	}

	@Test
	public void easyTestNoDistortion() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		rectangles.add(new Rectangle2D_I32(90,30,120,60));
		rectangles.add(new Rectangle2D_I32(30,90,60,120));
		rectangles.add(new Rectangle2D_I32(90,90,120,120));

		for( Class imageType : imageTypes ) {
			checkDetected(imageType,0.01); // the match should be perfect since the size of the
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
			checkDetected(imageType,1.0);
		}
	}

	private void checkDetected(Class imageType, double tol ) {
		renderDistortedRectangles(true,imageType);

		int numberOfSides = 4;
		DetectPolygonFromContour alg = createDetector(imageType, numberOfSides,numberOfSides);
		alg.process(image, binary);

		FastQueue<DetectPolygonFromContour.Info> found = alg.getFound();

		assertEquals(rectangles.size(), found.size);

		for (int i = 0; i < found.size; i++) {
			Polygon2D_F64 p = found.get(i).polygon;
			assertEquals(1,findMatches(p,tol));

			assertEquals(black,found.get(i).edgeInside,4);
			assertEquals(white,found.get(i).edgeOutside,white*0.1);
		}
	}

	@Test
	public void easyTestMultipleShapes() {

		List<Polygon2D_F64> polygons = new ArrayList<>();
		polygons.add(new Polygon2D_F64(20, 20, 40, 50, 80, 20));
		polygons.add(new Polygon2D_F64(20, 60, 20, 90, 40, 90,40, 60));

		for( Class imageType : imageTypes ) {
			checkDetectedMulti(imageType, polygons,2.5);
		}
	}

	private void checkDetectedMulti(Class imageType,List<Polygon2D_F64> polygons,  double tol ) {
		renderPolygons(polygons,imageType);

		DetectPolygonFromContour alg = createDetector(imageType, 3,4);
		alg.process(image, binary);

		FastQueue<DetectPolygonFromContour.Info> found = alg.getFound();

		assertEquals(polygons.size(), found.size);

		for (int i = 0; i < found.size; i++) {
			Polygon2D_F64 p = found.get(i).polygon;
			assertEquals(1,findMatches(p,tol));
		}
	}

	private <T extends ImageGray<T>> DetectPolygonFromContour<T> createDetector(Class<T> imageType, int minSides, int maxSides) {
		ConfigPolygonFromContour config = new ConfigPolygonFromContour(minSides,maxSides);

		return FactoryShapeDetector.polygonContour(config,imageType);
	}


	@Override
	public void renderPolygons( List<Polygon2D_F64> polygons, Class imageType ) {
		super.renderPolygons(polygons,imageType);
		InputToBinary inputToBinary = FactoryThresholdBinary.globalFixed(100, true, imageType);

		binary.reshape(width,height);
		inputToBinary.process(image,binary);
	}

	@Override
	public void renderDistortedRectangles( boolean black, Class imageType ) {
		super.renderDistortedRectangles(black,imageType);
		InputToBinary inputToBinary = FactoryThresholdBinary.globalFixed(100, true, imageType);

		binary.reshape(width,height);

		inputToBinary.process(image,binary);
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
			DetectPolygonFromContour alg = createDetector(GrayU8.class, i,i);

			alg.process(gray,binary);
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

		GrayU8 gray = ConvertBufferedImage.convertFrom(work,(GrayU8)null);
		binary.reshape(gray.width,gray.height);
		inputToBinary_U8.process(gray,binary);

		for (int i = 3; i <= 6; i++) {
			DetectPolygonFromContour<GrayU8> alg = createDetector(GrayU8.class, i, i);

			alg.process(gray,binary);
			if( i == 3 ) {
				assertEquals(1, alg.getFound().size());
				Polygon2D_F64 found = alg.getFound().get(0).polygon;
				checkPolygon(new double[]{10, 10, 30, 40, 50, 10}, found);
			} else
				assertEquals(0,alg.getFound().size());
		}
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

			DetectPolygonFromContour alg = createDetector(imageType, 5,5);

			alg.process(image,binary);

			assertEquals(0,alg.getFound().size());
		}
	}

	/**
	 * Make sure it rejects shapes with low contract
	 */
	@Test
	public void rejectLowContract() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		black = white-2;

		for( Class imageType : imageTypes ) {
			renderDistortedRectangles(true,imageType);

			DetectPolygonFromContour alg = createDetector(imageType, 3, 5);
			alg.process(image, binary);

			assertEquals(0, alg.getFound().size);
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

			DetectPolygonFromContour alg = createDetector(imageType, 5,5);
			alg.setConvex(false);

			alg.process(image, binary);

			assertEquals(1, alg.getFound().size());

			Polygon2D_F64 found = ((DetectPolygonFromContour.Info)alg.getFound().get(0)).polygon;
			assertEquals(1, findMatches(found,3));
		}
	}

	@Test
	public void touchesBorder_false() {
		List<Point2D_I32> contour = new ArrayList<>();

		DetectPolygonFromContour alg = createDetector(GrayU8.class, 4,4);
		alg.imageWidth = 20;
		alg.imageHeight = 30;
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
	public void doNotSnapToBrightObject() {
		GrayU8 gray = new GrayU8(200,200);
		GrayU8 binary = new GrayU8(200,200);

		ImageMiscOps.fillRectangle(gray,white,40,40,40,40);
		ImageMiscOps.fillRectangle(binary,1,38,38,44,44);
		ImageMiscOps.fillRectangle(binary,0,40,40,40,40);

		DetectPolygonFromContour<GrayU8> alg = createDetector(GrayU8.class, 4,4);

		// edge threshold test will only fail now if the sign is reversed
		alg.contourEdgeThreshold = 0;

		alg.process(gray,binary);

		assertEquals(0,alg.getFound().size);
	}

	@Test
	public void determineCornersOnBorder() {
		DetectPolygonFromContour alg = createDetector(GrayU8.class, 4,4);
		alg.imageWidth = width;
		alg.imageHeight = height;

		Polygon2D_F64 poly = new Polygon2D_F64(0,0, 10,0, 10,10, 0,10);

		GrowQueue_B corners = new GrowQueue_B();
		alg.determineCornersOnBorder(poly,corners);

		assertEquals(4,corners.size());

		assertEquals(true,corners.get(0));
		assertEquals(true,corners.get(1));
		assertEquals(false,corners.get(2));
		assertEquals(true,corners.get(3));
	}
}
