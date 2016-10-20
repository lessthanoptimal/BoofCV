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

package boofcv.abst.tracker;

import boofcv.abst.distort.FDistort;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.shapes.Quadrilateral_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public abstract class GenericTrackerObjectRectangleTests<T extends ImageBase> {

	Random rand = new Random(234);

	int width = 320;
	int height = 240;

	ImageType<T> imageType;
	protected T input;
	Quadrilateral_F64 where = new Quadrilateral_F64();

	// tolerances for different tests
	protected double tolTranslateSmall = 0.02;
	// tolerance for scale changes
	protected double tolScale = 0.1;
	// tolerance for stationary test
	protected double tolStationary = 1e-8;

	// the initial location of the target in the image
	protected Quadrilateral_F64 initRegion = rect(20,25,120,160);

	protected GenericTrackerObjectRectangleTests(ImageType<T> imageType) {
		this.imageType = imageType;
	}

	public abstract TrackerObjectQuad<T> create( ImageType<T> imageType );

	@Test
	public void changeInputImageSize() {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);

		T smaller = (T)input.createNew(width/2,height/2);

		new FDistort(input,smaller).scaleExt().apply();

		assertTrue(tracker.initialize(smaller, rect(20, 25, 70, 100)));
		assertTrue(tracker.process(smaller, where));

		assertTrue(tracker.initialize(input, initRegion));
		assertTrue(tracker.process(input, where));
	}

	@Test
	public void stationary() {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));
		assertTrue(tracker.process(input, where));

		assertEquals(initRegion.a.x, where.a.x, tolStationary);
		assertEquals(initRegion.a.y, where.a.y, tolStationary);
		assertEquals(initRegion.c.x, where.c.x, tolStationary);
		assertEquals(initRegion.c.y, where.c.y, tolStationary);
	}

	@Test
	public void translation_small() {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));

		for( int i = 0; i < 10; i++ ) {
			int tranX =  2*i;
			int tranY = -2*i;

			render(1,tranX,tranY);
			assertTrue(tracker.process(input, where));

			checkSolution(20+tranX,25+tranY,120+tranX,160+tranY,tolTranslateSmall);
		}

		double totalX = (where.a.x+where.b.x+where.c.x+where.d.x)/4 -
				(initRegion.a.x+initRegion.b.x+initRegion.c.x+initRegion.d.x)/4;
		double totalY = (where.a.y+where.b.y+where.c.y+where.d.y)/4 -
				(initRegion.a.y+initRegion.b.y+initRegion.c.y+initRegion.d.y)/4;

		assertEquals(2*9,totalX,4);
		assertEquals(-2*9,totalY,4);

	}

	@Test
	public void translation_large() {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));

		int tranX =  20;
		int tranY =  30;

		render(1,tranX,tranY);
		assertTrue(tracker.process(input, where));

		checkSolution(20+tranX,25+tranY,120+tranX,160+tranY,0.05);
	}

	@Test
	public void zooming_in() {
		zoom(-1);
	}

	@Test
	public void zooming_out() {
		zoom(1);
	}

	/**
	 * Zoom in and out without any visual translation of the object.  e.g. the center is constant
	 * @param dir
	 */
	protected void zoom( double dir ) {
		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));

		double centerX = 20+50;
		double centerY = 25+(160-25)/2.0;

		for( int i = 0; i < 20; i++ ) {
			double scale = 1 + dir*0.2*(i/9.0);
//			System.out.println("scale "+scale);

			double w2 = 100*scale/2.0;
			double h2 = (160-25)*scale/2.0;

			double tranX = centerX - centerX*scale;
			double tranY = centerY - centerY*scale;

			render(scale,tranX,tranY);
			assertTrue(tracker.process(input, where));

			checkSolution(centerX-w2,centerY-h2,centerX+w2,centerY+h2,tolScale);
		}
	}

	/**
	 * See if it correctly reinitializes.  Should produce identical results when given the same inputs after
	 * being reinitialized.
	 */
	@Test
	public void reinitialize() {
		Quadrilateral_F64 where1 = new Quadrilateral_F64();

		TrackerObjectQuad<T> tracker = create(imageType);
		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));
		render(1,3,-3);
		assertTrue(tracker.process(input, where));
		render(1,6,-6);
		assertTrue(tracker.process(input, where));

		render(1,0,0);
		assertTrue(tracker.initialize(input, initRegion));
		render(1,3,-3);
		assertTrue(tracker.process(input, where1));
		render(1,6,-6);
		assertTrue(tracker.process(input, where1));

		// Might not be a perfect match due to robust algorithm not being reset to their initial state
		checkSolution(where1.a.x,where1.a.y,where1.c.x,where1.c.y,0.02);
	}

	private void checkSolution( double x0 , double y0 , double x1 , double y1 , double fractionError ) {
//		System.out.println("Expected "+x0+" "+y0+" "+x1+" "+y1);
//		System.out.println("Actual "+where.a.x+" "+where.a.y+" "+where.c.x+" "+where.c.y);

		double tolX = (x1-x0)*fractionError;
		double tolY = (y1-y0)*fractionError;
		double tol = Math.max(tolX,tolY);

		assertTrue(Math.abs(where.a.x - x0) <= tol);
		assertTrue(Math.abs(where.a.y - y0) <= tol);
		assertTrue(Math.abs(where.c.x - x1) <= tol);
		assertTrue(Math.abs(where.c.y - y1) <= tol);
	}

	protected abstract void render( double scale , double tranX , double tranY );

	private static Quadrilateral_F64 rect( int x0 , int y0 , int x1 , int y1 ) {
		return new Quadrilateral_F64(x0,y0,x1,y0,x1,y1,x0,y1);
	}

}
