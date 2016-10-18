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

package boofcv.alg.shapes.ellipse;

import boofcv.alg.misc.ImageMiscOps;
import boofcv.alg.misc.PixelMath;
import boofcv.struct.image.GrayU8;
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestSnapToEllipseEdge {

	Random rand = new Random(24234);

	/**
	 * Simple test case involving a fully rendered image and known result
	 */
	@Test
	public void simpleNoChange() {
		EllipseRotated_F64 target = new EllipseRotated_F64(80,85,50,40,0);
		EllipseRotated_F64 found = new EllipseRotated_F64();

		List<EllipseRotated_F64> ellipses = new ArrayList<>();
		ellipses.add( target);

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,300,ellipses, 0);

		SnapToEllipseEdge<GrayU8> alg = new SnapToEllipseEdge<>(30,1,GrayU8.class);

		alg.setImage(image);
		assertTrue(alg.process(target,found));

		TestBinaryEllipseDetectorPixel.checkEquals(target,found,1.0,0.01);
	}

	/**
	 * The ellipse touches the image border
	 */
	@Test
	public void simpleNoChange_border() {
		EllipseRotated_F64 target = new EllipseRotated_F64(35,85,50,40,0);
		EllipseRotated_F64 found = new EllipseRotated_F64();

		List<EllipseRotated_F64> ellipses = new ArrayList<>();
		ellipses.add( target);

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,300,ellipses, 0);

		SnapToEllipseEdge<GrayU8> alg = new SnapToEllipseEdge<>(30,1,GrayU8.class);

		alg.setImage(image);
		assertTrue(alg.process(target,found));

		TestBinaryEllipseDetectorPixel.checkEquals(target,found,1.0,0.01);
	}

	/**
	 * The initial estimate is slightly off
	 */
	@Test
	public void simpleIncorrectEstimate() {
		EllipseRotated_F64 target = new EllipseRotated_F64(80,85,50,40,0);
		EllipseRotated_F64 input = new EllipseRotated_F64(target);
		EllipseRotated_F64 found = new EllipseRotated_F64();

		// make the import imprecise
		input.center.x += 0.5;
		input.a *= 0.97;
		input.b *= 1.05;
		input.phi = 0.04;

		List<EllipseRotated_F64> ellipses = new ArrayList<>();
		ellipses.add( target);

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,300,ellipses, 0);

		SnapToEllipseEdge<GrayU8> alg = new SnapToEllipseEdge<>(30,1,GrayU8.class);

		alg.setImage(image);
		assertTrue(alg.process(input,found));

		TestBinaryEllipseDetectorPixel.checkEquals(target,found,1.0,0.01);
	}

	@Test
	public void change() {


		assertEquals(0,SnapToEllipseEdge.
				change(new EllipseRotated_F64(1,2,3,4,-0.2), new EllipseRotated_F64(1,2,3,4,-0.2)),1e-8);

		assertEquals(0,SnapToEllipseEdge.
				change(new EllipseRotated_F64(1,2,3,4,Math.PI/2.0), new EllipseRotated_F64(1,2,3,4,-Math.PI/2.0)),1e-8);

		assertNotEquals(0,SnapToEllipseEdge.
				change(new EllipseRotated_F64(1.5,2,3,4,-0.2), new EllipseRotated_F64(1,2,3,4,-0.2)),1e-8);

		assertNotEquals(0,SnapToEllipseEdge.
				change(new EllipseRotated_F64(1,2.5,3,4,-0.2), new EllipseRotated_F64(1,2,3,4,-0.2)),1e-8);

		assertNotEquals(0,SnapToEllipseEdge.
				change(new EllipseRotated_F64(1,2,3.5,4,-0.2), new EllipseRotated_F64(1,2,3,4,-0.2)),1e-8);;

		assertNotEquals(0,SnapToEllipseEdge.
				change(new EllipseRotated_F64(1,2,3,4.5,-0.25), new EllipseRotated_F64(1,2,3,4,-0.2)),1e-8);;

	}

	@Test
	public void computePointsAndWeights() {
		EllipseRotated_F64 target = new EllipseRotated_F64(80,85,50,40,0);

		List<EllipseRotated_F64> ellipses = new ArrayList<>();
		ellipses.add( target);

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,300,ellipses, 0);
		// add a little bit of noise to prevent perfect zeros from appearing in weights
		PixelMath.plus(image,10,0,255,image);
		PixelMath.multiply(image,0.95,0,255,image);
		ImageMiscOps.addUniform(image,rand,-5,5);

		int numContour = 20;
		SnapToEllipseEdge<GrayU8> alg = new SnapToEllipseEdge<>(numContour,1,GrayU8.class);

		alg.setImage(image);
		alg.computePointsAndWeights(target);

		// all sampling was done inside the image and low change of a perfect zero
		assertEquals(3*numContour,alg.samplePts.size);
		assertEquals(3*numContour,alg.weights.size);

		// if the image wasn't discretized the number of zero weight would be 2 times larger than the max values
		// see if the results approximate that
		int numLow = 0, numHigh = 0;

		for (int i = 0; i < alg.weights.size(); i++) {
			if( alg.weights.data[i] < 90 )
				numLow++;
			else if( alg.weights.data[i] > 120 )
				numHigh++;
		}

		assertTrue(numLow > numHigh*1.5);
	}
}
