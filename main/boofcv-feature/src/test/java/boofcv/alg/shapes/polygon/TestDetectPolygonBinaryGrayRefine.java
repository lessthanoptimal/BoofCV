/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.ConfigPolygonDetector;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Rectangle2D_I32;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

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
			checkDetected(type,1.5,0.1);
		}
	}

	@Test
	public void simpleDetection_with_distortion() {
		fail("implement");
	}

	@Test
	public void clear_distortion(){
		fail("implement");
	}

	@Test
	public void refineContour() {
		fail("implement");
	}

	@Test
	public void refineGray() {
		fail("implement");
	}

	@Test
	public void refineAll() {
		fail("implement");
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

	private void checkDetected(Class imageType, double tolPixel , double tolRefined ) {
		renderDistortedRectangles(true,imageType);

		int numberOfSides = 4;
		DetectPolygonBinaryGrayRefine alg = createAlg(imageType, numberOfSides,numberOfSides);
		alg.process(image, binary);

		List<DetectPolygonFromContour.Info> found = alg.getPolygonInfo();

		assertEquals(rectangles.size(), found.size());

		checkSolutions(tolPixel, found);
		double errorContour = matchError;

		alg.refineAll();
		checkSolutions(tolRefined, found);
		double errorRefined = matchError;

		// see if refining the solution made a big difference
		assertTrue( errorRefined*5 < errorContour);
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

}