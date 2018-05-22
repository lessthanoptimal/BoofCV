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

package boofcv.alg.distort;

import boofcv.abst.distort.FDistort;
import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.misc.PixelMath;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.FMatrixRMaj;
import org.ejml.ops.ConvertMatrixData;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestRemovePerspectiveDistortion {


	/**
	 * The transform should not scale and produce a simple transform from input to output
	 */
	@Test
	public void identity() {
		RemovePerspectiveDistortion<GrayF32> alg =
				new RemovePerspectiveDistortion<>(30,40, ImageType.single(GrayF32.class));

		alg.createTransform(
				new Point2D_F64(20,30),new Point2D_F64(50,30),
				new Point2D_F64(50,70),new Point2D_F64(20,70));

		Point2D_F32 p = new Point2D_F32();
		PointTransformHomography_F32 transform = alg.getTransform();

		transform.compute(0,0,p);
		assertTrue(p.distance(20,30)< UtilEjml.TEST_F64);

		transform.compute(30,40,p);
		assertTrue(p.distance(50,70)< UtilEjml.TEST_F64);
	}

	@Test
	public void undoDistortion() {
		GrayF32 expected = new GrayF32(30,40);
		GrayF32 input = new GrayF32(200,150);

		Point2D_F64 topLeft = new Point2D_F64(30,20);
		Point2D_F64 topRight = new Point2D_F64(80,30);
		Point2D_F64 bottomRight = new Point2D_F64(70,90);
		Point2D_F64 bottomLeft = new Point2D_F64(25,80);

		GImageMiscOps.fill(expected,255);
		GImageMiscOps.fillRectangle(expected, 100, 10, 10, 15, 25);

		// apply homography distortion to expected
		applyForwardTransform(expected, input, topLeft, topRight, bottomRight, bottomLeft);

		// now reverse it with the class
		RemovePerspectiveDistortion<GrayF32> alg =
				new RemovePerspectiveDistortion<>(30,40, ImageType.single(GrayF32.class));

		assertTrue(alg.apply(input, topLeft, topRight, bottomRight, bottomLeft));

		GrayF32 found = alg.getOutput();
		GrayF32 difference = found.createSameShape();

		PixelMath.diffAbs(expected, found, difference);
		double error = ImageStatistics.sum(difference)/(difference.width*difference.height);

		assertTrue(error < 10);

	}

	private void applyForwardTransform(GrayF32 expected, GrayF32 input, Point2D_F64 topLeft, Point2D_F64 topRight, Point2D_F64 bottomRight, Point2D_F64 bottomLeft) {
		Estimate1ofEpipolar computeHomography = FactoryMultiView.computeHomographyDLT(true);

		ArrayList<AssociatedPair> associatedPairs = new ArrayList<>();
		associatedPairs.add( new AssociatedPair(topLeft,new Point2D_F64(0,0)));
		associatedPairs.add( new AssociatedPair(topRight,new Point2D_F64(expected.width-1,0)));
		associatedPairs.add( new AssociatedPair(bottomRight,new Point2D_F64(expected.width-1,expected.height-1)));
		associatedPairs.add( new AssociatedPair(bottomLeft,new Point2D_F64(0,expected.height-1)));

		DMatrixRMaj H = new DMatrixRMaj(3,3);
		computeHomography.process(associatedPairs,H);

		FMatrixRMaj H32 = new FMatrixRMaj(3,3);
		ConvertMatrixData.convert(H,H32);

		new FDistort(expected,input).transform(new PointTransformHomography_F32(H32)).apply();
	}
}
