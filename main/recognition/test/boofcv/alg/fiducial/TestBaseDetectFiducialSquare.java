/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.fiducial;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.distort.*;
import boofcv.alg.feature.shapes.SplitMergeLineFitLoop;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ejml.data.DenseMatrix64F;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public class TestBaseDetectFiducialSquare {
	/**
	 * Basic test where a distorted pattern is places in the image and the found coordinates
	 * are compared against ground truth
	 */
	@Test
	public void findPatternEasy() {
		checkFindKnown(new IntrinsicParameters(500,500,0,320,240,640,480,false,null),1);
	}

	private void checkFindKnown(IntrinsicParameters intrinsic, double tol ) {
		ImageUInt8 pattern = createPattern(100);
		Quadrilateral_F64 where = new Quadrilateral_F64(50,50,  130,60,  140,150,  40,140);

		ImageUInt8 image = new ImageUInt8(640,480);
		ImageUInt8 distorted = new ImageUInt8(640,480);
		ImageMiscOps.fill(image, 255);
		render(pattern, where, image);

		RemoveRadialPtoP_F32 distort = new RemoveRadialPtoP_F32();
		distort.set(intrinsic.fx,intrinsic.fy,intrinsic.skew,intrinsic.cx,intrinsic.cy,intrinsic.radial);

		ImageDistort<ImageUInt8,ImageUInt8> distorter
				= DistortImageOps.createImageDistort(distort,TypeInterpolate.BILINEAR,ImageUInt8.class,ImageUInt8.class);

		distorter.apply(image,distorted);

		Dummy dummy = new Dummy();
		dummy.configure(0.1, intrinsic);
		dummy.process(distorted);

		assertEquals(1,dummy.detected.size());

		Quadrilateral_F64 found = dummy.getFound().get(0).location;
//		System.out.println("found "+found);
//		System.out.println("where "+where);
		checkMatch(where, found.a, tol);
		checkMatch(where,found.b,tol);
		checkMatch(where,found.c,tol);
		checkMatch(where, found.d, tol);

		// see if the undistorted image is as expected
		checkPattern( dummy.detected.get(0) );
	}

	private void checkMatch( Quadrilateral_F64 q , Point2D_F64 p , double tol ) {

		if( q.a.distance(p) <= tol)
			return;
		if( q.b.distance(p) <= tol)
			return;
		if( q.c.distance(p) <= tol)
			return;
		if( q.d.distance(p) <= tol)
			return;
		fail("no match "+p+"    "+q);
	}

	/**
	 * Makes sure all the remove lens and perspective distortion code works correctly.  Add a square target to the
	 * image. Distort it badly, and see if it reproduces the original
	 */
	@Test
	public void removeDistortion() {
		checkFindKnown(new IntrinsicParameters(500,500,0,320,240,640,480,false,new double[]{0.1,0.3}),1.5);
	}

	@Test
	public void computeTargetToWorld() {

		IntrinsicParameters intrinsic = new IntrinsicParameters(400,400,0,320,240,640,380,false,null);
		DenseMatrix64F K = PerspectiveOps.calibrationMatrix(intrinsic,null);

		Dummy alg = new Dummy();
		alg.configure(0.5, intrinsic);

		Se3_F64 targetToWorld = new Se3_F64();
		targetToWorld.getT().set(0.1,-0.07,1.5);
		RotationMatrixGenerator.eulerXYZ(0.03,0.1,0,targetToWorld.getR());

		Quadrilateral_F64 quad = new Quadrilateral_F64();

		quad.a = PerspectiveOps.renderPixel(targetToWorld,K,c(alg.pairsPose.get(0).p1));
		quad.b = PerspectiveOps.renderPixel(targetToWorld,K,c(alg.pairsPose.get(1).p1));
		quad.c = PerspectiveOps.renderPixel(targetToWorld,K,c(alg.pairsPose.get(2).p1));
		quad.d = PerspectiveOps.renderPixel(targetToWorld,K,c(alg.pairsPose.get(3).p1));

		Se3_F64 found = new Se3_F64();
		alg.computeTargetToWorld(quad, found);

		assertTrue(MatrixFeatures.isIdentical(targetToWorld.getR(), found.getR(), 1e-6));
		assertEquals(0,targetToWorld.getT().distance(found.getT()),1e-6);
	}

	private static Point3D_F64 c( Point2D_F64 a ) {
		return new Point3D_F64(a.x,a.y,0);
	}

	/**
	 * Creates a square pattern image of the specified size
	 */
	public static ImageUInt8 createPattern( int length ) {
		ImageUInt8 pattern = new ImageUInt8( length , length );

		int b = length/8;

		for (int y = 0; y < length; y++) {
			for (int x = 0; x < length; x++) {
				int color = (x < b || y < b || x >= length-b || y >= length-b ) ? 0 : 255;
				pattern.set(x,y,color);
			}
		}

		return pattern;
	}

	public static void checkPattern( ImageFloat32 image ) {

		int x0 = image.width/8;
		int y0 = image.height/8;
		int x1 = image.width - x0;
		int y1 = image.height - y0;

		double totalBorder = 0;
		int countBorder = 0;
		double totalInner = 0;
		int countInner = 0;

		// the border regions can be ambiguous so sum up around them
		for (int y = 0; y < image.height; y++) {
			for (int x = 0; x < image.width; x++) {
				if( x < (x0-1) || x >= (x1+1) || y < (y0-1) || y >= (y1+1) ) {
					totalBorder += image.get(x,y);
					countBorder++;
				} else if( x >= (x0+1) && x < (x1-1) && y >= (y0+1) && y < (y1-1) ) {
					totalInner += image.get(x,y);
					countInner++;
				}
			}
		}

		totalBorder /= countBorder;
		totalInner /= countInner;

		assertTrue( totalBorder < 10 );
		assertTrue( totalInner > 245 );
	}

	/**
	 * Draws a distorted pattern onto the output
	 */
	public static void render( ImageUInt8 pattern , Quadrilateral_F64 where , ImageUInt8 output ) {

		int w = pattern.width-1;
		int h = pattern.height-1;

		ArrayList<AssociatedPair> associatedPairs = new ArrayList<AssociatedPair>();
		associatedPairs.add(new AssociatedPair(where.a,new Point2D_F64(0,0)));
		associatedPairs.add(new AssociatedPair(where.b,new Point2D_F64(w,0)));
		associatedPairs.add(new AssociatedPair(where.c,new Point2D_F64(w,h)));
		associatedPairs.add(new AssociatedPair(where.d,new Point2D_F64(0,h)));

		Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomography(true);

		DenseMatrix64F H = new DenseMatrix64F(3,3);
		computeHomography.process(associatedPairs, H);

		// Create the transform for distorting the image
		PointTransformHomography_F32 homography = new PointTransformHomography_F32(H);
		PixelTransform_F32 pixelTransform = new PointToPixelTransform_F32(homography);

		// Apply distortion and show the results
		DistortImageOps.distortSingle(pattern, output, pixelTransform, true, TypeInterpolate.BILINEAR);
	}

	public static class Dummy extends BaseDetectFiducialSquare<ImageUInt8> {

		public List<ImageFloat32> detected = new ArrayList<ImageFloat32>();

		protected Dummy() {
			super(FactoryThresholdBinary.globalFixed(50,true,ImageUInt8.class),
					new SplitMergeLineFitLoop(2.0,0.05,200), 100, 0.23, ImageUInt8.class);
		}

		@Override
		public boolean processSquare(ImageFloat32 square, Result result) {
			detected.add(square.clone());
			return true;
		}
	}
}