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

package boofcv.alg.shapes.edge;

import boofcv.abst.distort.FDistort;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import boofcv.testing.BoofTesting;
import georegression.geometry.UtilLine2D_F64;
import georegression.metric.ClosestPoint2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.affine.Affine2D_F64;
import georegression.struct.line.LineGeneral2D_F64;
import georegression.struct.line.LineSegment2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Vector2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.transform.ConvertTransform_F64;
import georegression.transform.affine.AffinePointOps_F64;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSnapToLineEdge {

	Class imageTypes[] = new Class[]{GrayU8.class, GrayF32.class};

	Random rand = new Random(234);

	int width = 400, height = 500;
	ImageGray work; // original image before affine has been applied
	ImageGray image; // image after affine applied

	int x0 = 200, y0 = 160;
	int x1 = 260, y1 = 400; // that's exclusive

	int white = 200;


	/**
	 * Edges which are aligned with the x and y axises
	 */
	@Test
	public void easy_aligned() {
		for (Class imageType : imageTypes) {
			easy_aligned(imageType);
		}
	}

	public void easy_aligned(Class imageType) {
		setup(null, imageType);

		SnapToLineEdge alg = new SnapToLineEdge(10,2, imageType);
		alg.setImage(image);

		int r = 2;
		LineSegment2D_F64 bottom    = new LineSegment2D_F64(x1-r,y0,x0+r,y0);
		LineSegment2D_F64 left   = new LineSegment2D_F64(x0,y0+r,x0,y1-r);
		LineSegment2D_F64 top = new LineSegment2D_F64(x0+r,y1,x1-r,y1);
		LineSegment2D_F64 right  = new LineSegment2D_F64(x1,y1-r,x1,y0+r);

		differentInitial(alg,bottom);
		differentInitial(alg,left);
		differentInitial(alg,top);
		differentInitial(alg,right);
	}

	private void differentInitial(SnapToLineEdge alg , LineSegment2D_F64 segment ) {
		Vector2D_F64 v = new Vector2D_F64();
		v.x = -segment.slopeY();
		v.y = segment.slopeX();
		v.normalize();

		LineGeneral2D_F64 found = new LineGeneral2D_F64();

		// give it an offset from truth.  still simple enough that it should nail the correct solution on the first
		// try
		for (int i = -1; i <= 1; i++) {
			LineSegment2D_F64 work = segment.copy();
			work.a.x += i*v.x; work.a.y += i*v.y;
			work.b.x += i*v.x; work.b.y += i*v.y;

			assertTrue(alg.refine(work.a, work.b, found));
			checkIdentical(segment,found);

			// do it in the other direction. shouldn't matter
			assertTrue(alg.refine(work.b, work.a, found));
			checkIdentical(segment,found);
		}
	}

	private void checkIdentical( LineSegment2D_F64 expectedLS , LineGeneral2D_F64 found ) {
		LineGeneral2D_F64 expected = new LineGeneral2D_F64();
		UtilLine2D_F64.convert(expectedLS,expected);

		expected.normalize();
		found.normalize();

		if( Math.signum(expected.C) != Math.signum(found.C) ) {
			expected.A *= -1;
			expected.B *= -1;
			expected.C *= -1;
		}

		assertEquals(expected.A,found.A,1e-8);
		assertEquals(expected.B,found.B,1e-8);
		assertEquals(expected.C,found.C,1e-8);
	}

	/**
	 * Make sure it can process a sub-image just fine
	 */
	@Test
	public void subimage() {
		for (Class imageType : imageTypes) {
			subimage(imageType);
		}
	}

	public void subimage(Class imageType) {
		setup(null, imageType);

		SnapToLineEdge alg = new SnapToLineEdge(10,2, imageType);
		alg.setImage(BoofTesting.createSubImageOf_S(image));

		int r = 2;
		LineSegment2D_F64 bottom    = new LineSegment2D_F64(x1-r,y0,x0+r,y0);
		LineSegment2D_F64 left   = new LineSegment2D_F64(x0,y0+r,x0,y1-r);
		LineSegment2D_F64 top = new LineSegment2D_F64(x0+r,y1,x1-r,y1);
		LineSegment2D_F64 right  = new LineSegment2D_F64(x1,y1-r,x1,y0+r);

		differentInitial(alg,bottom);
		differentInitial(alg,left);
		differentInitial(alg,top);
		differentInitial(alg,right);
	}

	/**
	 * Fit the quad with a noisy initial guess
	 */
	@Test
	public void fit_noisy_affine() {
		// distorted and undistorted views
		Affine2D_F64 affines[] = new Affine2D_F64[2];
		affines[0] = new Affine2D_F64();
		affines[1] = new Affine2D_F64(1.3,0.05,-0.15,0.87,0.1,0.6);
		ConvertTransform_F64.convert(new Se2_F64(0, 0, 0.2), affines[0]);

		for (Class imageType : imageTypes) {
			for (Affine2D_F64 a : affines) {
				fit_noisy_affine(a, imageType);
			}
		}
	}

	public void fit_noisy_affine(Affine2D_F64 affine, Class imageType) {
		setup(affine, imageType);

		double tol = 0.5;

		SnapToLineEdge alg = new SnapToLineEdge(10,2, imageType);

		Polygon2D_F64 input = new Polygon2D_F64(4);
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y0), input.get(0));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x0,y1),input.get(1));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y1),input.get(2));
		AffinePointOps_F64.transform(affine,new Point2D_F64(x1,y0),input.get(3));


		LineGeneral2D_F64 found[] = new LineGeneral2D_F64[input.size()];
		for (int i = 0; i < found.length; i++) {
			found[i] = new LineGeneral2D_F64();
		}

		for (int i = 0; i < 10; i++) {
			// add some noise
			Polygon2D_F64 noisy = input.copy();
			addNoise(noisy, 2);

			alg.setImage(image);
			for (int j = 0; j < noisy.size(); j++) {
				LineSegment2D_F64 edge = noisy.getLine(j,null);

				double slopeX = edge.slopeX();
				double slopeY = edge.slopeY();
				double r = Math.sqrt(slopeX*slopeX + slopeY*slopeY);
				slopeX /= r;
				slopeY /= r;

				// shrink it slightly to avoid weirdness along the corner
				edge.a.x += 2*slopeX;
				edge.a.y += 2*slopeY;
				edge.b.x -= 2*slopeX;
				edge.b.y -= 2*slopeY;

				// optimize it a few times to get a good solution
				for (int k = 0; k < 5; k++) {
					assertTrue(alg.refine(edge.a,edge.b, found[j]));
					edge.a.set(ClosestPoint2D_F64.closestPoint(found[j],edge.a,null));
					edge.b.set(ClosestPoint2D_F64.closestPoint(found[j],edge.b,null));
				}

			}

			// compute the corners and see how it did
			for (int j = 0; j < input.size(); j++) {
				int k = (j+1)%input.size();
				Point2D_F64 c = new Point2D_F64();
				Intersection2D_F64.intersection(found[j],found[k],c);
				double d = c.distance(input.get(k));
				assertTrue(d <= tol);
			}
		}
	}

	private void addNoise(Polygon2D_F64 input, double spread) {
		for (int i = 0; i < input.size(); i++) {
			Point2D_F64 v = input.get(i);
			v.x += rand.nextDouble()*spread - spread/2.0;
			v.y += rand.nextDouble()*spread - spread/2.0;
		}
	}


	/**
	 * Simple case where it samples along a line on a perfect rectangle
	 */
	@Test
	public void computePointsAndWeights() {
		for (Class imageType : imageTypes) {
			computePointsAndWeights(imageType);
		}
	}

	public void computePointsAndWeights( Class imageType ) {
		setup(null,imageType);

		SnapToLineEdge alg = new SnapToLineEdge(10,2, imageType);

		float H = y1-y0-10;

		alg.setImage(image);
		alg.center.set(10, 12);
		alg.localScale = 2;
		alg.computePointsAndWeights(0, H, x0, y0 + 5, 1, 0);

		// sample points on the outside of the line will be zero since the image has no gradient
		// there.  Thus the weight is zero and the point skipped
		assertEquals(alg.lineSamples,alg.samplePts.size());

		for (int i = 0; i < alg.lineSamples; i++) {
			// only points along the line were saved, the outer ones discarded
			assertEquals(white,alg.weights.get(i),1e-8);

			double x = x0 - 10;
			double y = y0 + 5 + H*i/(alg.lineSamples -1) - 12;
			x /= alg.localScale;
			y /= alg.localScale;
			Point2D_F64 p = (Point2D_F64)alg.samplePts.get(i);
			assertEquals(x,p.x,1e-4);
			assertEquals(y,p.y,1e-4);
		}
	}

	/**
	 * Checks to see if it blows up along the image border
	 */
	@Test
	public void computePointsAndWeights_border() {
		for (Class imageType : imageTypes) {
			computePointsAndWeights_border(imageType);
		}
	}

	public void computePointsAndWeights_border( Class imageType ) {
		setup(null,imageType);

		SnapToLineEdge alg = new SnapToLineEdge(10,2, imageType);

		alg.setImage(image);
		alg.computePointsAndWeights(0, image.height-2, 0, 2, 1, 0);
		assertEquals(0,alg.samplePts.size());
		alg.computePointsAndWeights(0, image.height-2, image.width-1, 2, 1, 0);
		assertEquals(0,alg.samplePts.size());
		alg.computePointsAndWeights(image.width-2,0, 1, 0, 0, 1);
		assertEquals(0,alg.samplePts.size());
		alg.computePointsAndWeights(image.width-2,0, 1, image.height-1, 0, 1);
		assertEquals(0, alg.samplePts.size());
	}

	@Test
	public void localToGlobal() {

		LineSegment2D_F64 segment = new LineSegment2D_F64(10,20,50,-10);

		SnapToLineEdge alg = new SnapToLineEdge(10,2, GrayU8.class);
		alg.center.set(20,23);
		alg.localScale = 10;

		LineSegment2D_F64 local = segment.copy();
		toLocal(local.a,alg);
		toLocal(local.b,alg);

		LineGeneral2D_F64 expected = new LineGeneral2D_F64();
		LineGeneral2D_F64 found = new LineGeneral2D_F64();
		UtilLine2D_F64.convert(segment,expected);
		UtilLine2D_F64.convert(local,found);

		alg.localToGlobal(found);

		expected.normalize();
		found.normalize();

		assertEquals(expected.A,found.A,1e-8);
		assertEquals(expected.B,found.B,1e-8);
		assertEquals(expected.C,found.C,1e-8);
	}

	private void toLocal( Point2D_F64 p , SnapToLineEdge alg ) {
		p.x = (p.x-alg.center.x)/alg.localScale;
		p.y = (p.y-alg.center.y)/alg.localScale;
	}

	private void setup( Affine2D_F64 affine, Class imageType ) {
		work = GeneralizedImageOps.createSingleBand(imageType, width, height);
		image = GeneralizedImageOps.createSingleBand(imageType,width,height);

		int bg = white;
		int fg = 0;
		GImageMiscOps.fill(work, bg);
		GImageMiscOps.fillRectangle(work, fg, x0, y0, x1 - x0, y1 - y0);

		if( affine != null ) {
			new FDistort(work, image).border(bg).affine(affine).apply();
		} else {
			image.setTo(work);
		}

//		BufferedImage out = ConvertBufferedImage.convertTo(image, null, true);
//		ShowImages.showWindow(out, "Rendered");
//		try {
//			Thread.sleep(3000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
	}
}
