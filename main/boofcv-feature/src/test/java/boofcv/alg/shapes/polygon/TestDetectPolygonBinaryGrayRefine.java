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
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.affine.UtilAffine;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestDetectPolygonBinaryGrayRefine extends CommonFitPolygonChecks {

	GrayU8 binary = new GrayU8(1,1);

	@Test
	public void simpleDetection() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		rectangles.add(new Rectangle2D_I32(90,30,120,60));

		for( Class type : imageTypes ) {
			simpleDetection(type,1.5,0.1);
		}
	}

	private void simpleDetection(Class imageType, double tolPixel , double tolRefined ) {
		renderDistortedRectangles(true,imageType);

		int numberOfSides = 4;
		DetectPolygonBinaryGrayRefine alg = createAlg(imageType, numberOfSides,numberOfSides);
		alg.process(image, binary);

		List<DetectPolygonFromContour.Info> found = alg.getPolygonInfo();

		assertEquals(rectangles.size(), found.size());

		checkSolutions(tolPixel, found);

		alg.refineAll();
		checkSolutions(tolRefined, found);
	}

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
		DetectPolygonBinaryGrayRefine alg = createAlg(imageType, numberOfSides,numberOfSides);
		alg.setLensDistortion(image.width, image.height, tranTo, tranFrom);
		alg.process(image, binary);

		List<DetectPolygonFromContour.Info> found = alg.getPolygonInfo();

		assertEquals(rectangles.size(),found.size());

		for (int i = 0; i < found.size(); i++) {
			Polygon2D_F64 p = found.get(i).polygon;
			assertEquals(1, findMatchesOriginal(p, tol));
			assertEquals(black,found.get(i).edgeInside,3);
			assertEquals(white,found.get(i).edgeOutside,white*0.05);
		}

		//----------- see if distortion is cleared properly
		alg.clearLensDistortion();
		alg.process(image, binary);

		found = alg.getPolygonInfo();

		assertEquals(rectangles.size(),found.size());

		// nothing should match now
		for (int i = 0; i < found.size(); i++) {
			Polygon2D_F64 p = found.get(i).polygon;
			assertEquals(0, findMatchesOriginal(p, tol));
		}
	}

	@Test
	public void refineAll() {
		rectangles.add(new Rectangle2D_I32(30,30,60,60));
		rectangles.add(new Rectangle2D_I32(90,30,120,60));

		for( Class type : imageTypes ) {
			refineAll(type,1.5,0.1);
		}
	}

	private void refineAll(Class imageType, double tolPixel , double tolRefined ) {
		renderDistortedRectangles(true,imageType);

		int numberOfSides = 4;
		DetectPolygonBinaryGrayRefine alg = createAlg(imageType, numberOfSides,numberOfSides);
		alg.process(image, binary);

		List<DetectPolygonFromContour.Info> found = alg.getPolygonInfo();

		assertEquals(rectangles.size(), found.size());

		// let's mess up the solutions some
		for (DetectPolygonFromContour.Info info : found ) {
			info.polygon.get(0).x -= 1.5;
			info.polygon.get(1).y += 1.5;
			info.polygon.get(2).x += 1.5;
			info.polygon.get(3).x += 1.5;
		}

		checkSolutions(tolPixel, found);
		double errorContour = matchError;

		alg.refineAll();
		checkSolutions(tolRefined, found);
		double errorRefined = matchError;

		// see if refining the solution made a big difference
		assertTrue( errorRefined*5 < errorContour);
	}

	/**
	 * See if it removes a polygon when bias reduces its size below the minimum
	 */
	@Test
	public void removePolygonWhenAdjustMakesTooSmall() {
		DetectPolygonBinaryGrayRefine alg = createAlg(GrayU8.class, 4,4);
		alg.detector = new MockDetector();
		alg.adjustForBias = new MockAdjustBias();

		alg.process(new GrayU8(1,1),new GrayU8(1,1));

		assertEquals(4,alg.getPolygonInfo().size());
	}

	@Override
	public void renderPolygons(List<Polygon2D_F64> polygons, Class imageType ) {
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

	private void checkSolutions(double tolerance, List<DetectPolygonFromContour.Info> found) {
		for (int i = 0; i < found.size(); i++) {
			Polygon2D_F64 p = found.get(i).polygon;
			assertEquals(1,findMatches(p,tolerance));

			assertEquals(black,found.get(i).edgeInside,4);
			assertEquals(white,found.get(i).edgeOutside,white*0.1);
		}
	}

	DetectPolygonBinaryGrayRefine createAlg( Class imageType, int minSides, int maxSides ) {
		return FactoryShapeDetector.polygon(new ConfigPolygonDetector(minSides,maxSides),imageType);
	}

	class MockDetector<T extends ImageGray<T>> extends DetectPolygonFromContour<T> {

		@Override
		public void process(T gray, GrayU8 binary) {
			foundInfo.reset();
			for (int i = 0; i < 5; i++) {
				Info info = foundInfo.grow();
				info.polygon.vertexes.resize(4);
			}
		}

		@Override
		public int getMinimumSides() {
			return 4;
		}
	}

	class MockAdjustBias extends AdjustPolygonForThresholdBias {

		int counter = 0;

		@Override
		public void process( Polygon2D_F64 polygon, boolean clockwise) {
			if( counter++ == 2 ) {
				polygon.vertexes.resize(3);
			}
		}
	}

}