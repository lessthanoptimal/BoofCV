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
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.PointToPixelTransform_F32;
import boofcv.alg.distort.PointTransformHomography_F32;
import boofcv.alg.feature.shapes.SplitMergeLineFitLoop;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ejml.data.DenseMatrix64F;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestDetectFiducialSquarePattern {
	@Test
	public void easyTest() {
		ImageUInt8 pattern = createPattern(100);
		Quadrilateral_F64 where = new Quadrilateral_F64(50,50,  130,60,  140,150,  40,140);

		ImageUInt8 image = new ImageUInt8(640,480);
		ImageMiscOps.fill(image, 255);
		render(pattern, where, image);

		IntrinsicParameters intrinsic = new IntrinsicParameters(500,500,0,320,240,640,480,false,null);

		Dummy dummy = new Dummy();
		dummy.setIntrinsic(intrinsic);
		dummy.process(image);

		assertEquals(1,dummy.detected.size());

		System.out.println(dummy.quads.get(0));
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

	/**
	 * Draws a distorted pattern onto the output
	 */
	public static void render( ImageUInt8 pattern , Quadrilateral_F64 where , ImageUInt8 output ) {

		int w = pattern.width;
		int h = pattern.height;

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

	public static class Dummy extends DetectFiducialSquarePattern {

		public List<ImageFloat32> detected = new ArrayList<ImageFloat32>();
		public List<Quadrilateral_F64> quads = new ArrayList<Quadrilateral_F64>();

		protected Dummy() {
			super(new SplitMergeLineFitLoop(2.0,0.05,200), 100);
		}

		@Override
		public void processSquare(ImageFloat32 square, Quadrilateral_F64 where) {
			detected.add(square.clone());
			quads.add( where.copy() );
		}
	}
}