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

package boofcv.alg.feature.detect.edge;

import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofTesting;
import georegression.struct.point.Point2D_I32;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestCannyEdge {

	int width = 150;
	int height = 200;

	Random rand = new Random(234);

	/**
	 * Image has no texture and the sadistic user and specified a threshold of zero.  Everything should
	 * be an edge.
	 */
	@Test
	public void canHandleNoTexture_and_zeroThresh() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 output = new GrayU8(width,height);

		CannyEdge<GrayU8,GrayS16> alg = createCanny(true);

		alg.process(input,0,0,output);

		List<EdgeContour> contour = alg.getContours();
		assertTrue(contour.size()>0);

		int numEdgePixels = 0;
		for( EdgeContour e : contour ) {
			for( EdgeSegment s : e.segments ) {
				numEdgePixels += s.points.size();
			}
		}
		assertEquals(numEdgePixels,input.width*input.height);

		for( int i = 0; i < output.data.length; i++ )
			assertEquals(1,output.data[i]);
	}

	/**
	 * Test a pathological case. The input image has a constant gradient
	 */
	@Test
	public void constantGradient() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 output = new GrayU8(width,height);

		// the whole image has a constant gradient
		for( int i = 0; i < input.width; i++ ) {
			for( int j = 0; j < input.height; j++ ) {
				input.unsafe_set(i,j,i*2);
			}
		}

		CannyEdge<GrayU8,GrayS16> alg = createCanny(true);

		alg.process(input,1,2,output);

		// just see if it blows up or freezes
	}

	@Test
	public void basicTestPoints() {

		GrayU8 input = new GrayU8(width,height);

		ImageMiscOps.fillRectangle(input,50,20,30,40,50);

		CannyEdge<GrayU8,GrayS16> alg = createCanny(true);

		alg.process(input,10,50,null);

		List<EdgeContour> contour = alg.getContours();
		assertEquals(1,contour.size());

		// check for sequential order and location
		// the exact edge location is ambiguous
		for( EdgeContour e : contour ) {
			for( EdgeSegment s : e.segments ) {
				checkNeighbor(s.points);
				checkRectangle(s.points,20,30,20+40,30+50,1);
			}
		}
	}

	@Test
	public void basicTestMarks() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 binary = new GrayU8(width,height);

		ImageMiscOps.fillRectangle(input,50,20,30,40,50);

		CannyEdge<GrayU8,GrayS16> alg = createCanny(false);

		alg.process(input,10,50,binary);

		GrayU8 expected = new GrayU8(width,height);
		// set pixels to 1 if there are where the edge could lie
		ImageMiscOps.fillRectangle(expected,1,19,29,42,52);
		ImageMiscOps.fillRectangle(expected,0,21,31,38,48);

		int totalHits = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				if( expected.get(x,y) == 0 ) {
					assertEquals(0,binary.get(x,y));
				} else if( binary.get(x,y) == 1 ) {
					totalHits++;
				}
			}
		}
		assertTrue( totalHits >= 2*50+2*38 );
	}

	@Test
	public void checkThresholds() {
		GrayU8 input = new GrayU8(15,20);

		input.set(5,0,50);
		input.set(5,1,50);
		input.set(5,2,50);
		input.set(5,3,5);
		input.set(5,4,5);
		input.set(5,5,5);

		// manually inspecting the image shows that the intensity image has a max value of 34 and a
		// smallest value of 2
		CannyEdge<GrayU8,GrayS16> alg = createCanny(true);
		alg.process(input,1,28,null);
		assertEquals(1, alg.getContours().size());

		// the high threshold should be too high
		alg.process(input,1,1000,null);
		assertEquals(0, alg.getContours().size());

		// the low threshold is too low now for everything to be connected
		alg.process(input,30,31,null);
		assertEquals(2, alg.getContours().size());
	}

	/**
	 * Makes sure the two output modes are equivalent
	 */
	@Test
	public void checkEquivalentOutput() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 output0 = new GrayU8(width,height);
		GrayU8 output1 = new GrayU8(width,height);

		for( int i = 0; i < 10; i++ ) {
			ImageMiscOps.fillUniform(input,rand,0,200);
			CannyEdge<GrayU8,GrayS16> algTrace = createCanny(true);
			CannyEdge<GrayU8,GrayS16> algMark = createCanny(false);

			algTrace.process(input,20,100,output0);
			algMark.process(input,20,100,output1);

			BoofTesting.assertEquals(output0,output1,0);
		}
	}

	/**
	 * Make sure it can handle sub-images
	 */
	@Test
	public void checkSubImage() {
		GrayU8 input = new GrayU8(width,height);
		GrayU8 output = new GrayU8(width,height);
		ImageMiscOps.fillUniform(input,rand,0,200);

		BoofTesting.checkSubImage(this,"checkSubImage",true,input,output);
	}

	public void checkSubImage(GrayU8 input , GrayU8 output ) {
		CannyEdge<GrayU8,GrayS16> alg = createCanny(true);
		alg.process(input,1,100,output);
	}

	private CannyEdge<GrayU8,GrayS16> createCanny(boolean saveTrace ) {
		BlurFilter<GrayU8> blur = FactoryBlurFilter.gaussian(GrayU8.class, -1, 1);
		ImageGradient<GrayU8,GrayS16> gradient = FactoryDerivative.three(GrayU8.class, GrayS16.class);

		return new CannyEdge<>(blur, gradient, saveTrace);
	}

	private void checkNeighbor( List<Point2D_I32> list ) {
		for( int i = 1; i < list.size(); i++ ) {
			Point2D_I32 a = list.get(i-1);
			Point2D_I32 b = list.get(i);

			int dx = Math.abs(a.x-b.x);
			int dy = Math.abs(a.y-b.y);
			assertTrue( dx <= 1 && dy <= 1);
		}
	}

	private void checkRectangle( List<Point2D_I32> list , int x0 , int y0 , int x1 , int y1 , int tol ) {
		for( Point2D_I32 p : list ) {
			// left side
			if( isClose(p.x,x0,tol) ) {
				assertTrue( p.y >= y0-tol && p.y < y1 + tol );
			} else if( isClose(p.x,x1,tol) ) {
				// right side
				assertTrue( p.y >= y0-tol && p.y < y1 + tol );
			} else if( isClose(p.y,y0,tol) ) {
				// top
				assertTrue( p.x >= x0-tol && p.x < x1 + tol );
			} else if( isClose(p.y,y1,tol) ) {
				// bottom
				assertTrue( p.x >= x0-tol && p.x < x1 + tol );
			} else {
				fail("Not near rectangle");
			}
		}
	}

	private boolean isClose( int x0 , int x1 , int tol )  {
		return Math.abs(x0-x1) <= tol;
	}
}
