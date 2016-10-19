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

import boofcv.alg.distort.PixelTransformAffine_F32;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.image.GrayU8;
import georegression.struct.affine.Affine2D_F32;
import georegression.struct.shapes.EllipseRotated_F64;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestBinaryEllipseDetector {

	// edge intensity theshold for a valid ellipse
	public static int THRESHOLD = 20;

	/**
	 * Simple test case with unambiguous detections
	 */
	@Test
	public void simpleCase() {
		List<EllipseRotated_F64> expected = new ArrayList<>();

		expected.add( new EllipseRotated_F64(50,65,20,10,0.5));
		expected.add( new EllipseRotated_F64(90,100,25,25,0));

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, expected, 0);
		GrayU8 binary = image.createSameShape();
		ThresholdImageOps.threshold(image,binary,30,true);

		BinaryEllipseDetector<GrayU8> alg = create();

		alg.process(image, binary);

		List<EllipseRotated_F64> found = alg.getFoundEllipses().toList();

		TestBinaryEllipseDetectorPixel.checkEquals(expected,found, 1.0, 0.1);
	}

	/**
	 * Handle a situation where a shape should be filtered out based on its edge intensity
	 */
	@Test
	public void filterByEdge() {
		List<EllipseRotated_F64> expected = new ArrayList<>();

		expected.add( new EllipseRotated_F64(50,65,20,10,0.5));

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, expected, 0);
		GrayU8 binary = image.createSameShape();
		ThresholdImageOps.threshold(image,binary,30,true);

		BinaryEllipseDetector<GrayU8> alg = create();

		// pass once with it being a clear edge
		alg.process(image, binary);
		List<EllipseRotated_F64> found = alg.getFoundEllipses().toList();
		TestBinaryEllipseDetectorPixel.checkEquals(expected,found, 1.0, 0.1);

		// now make the ellipse more dim so it shouldn't pass
		image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, expected, 255-THRESHOLD+5);
		alg.process(image, binary);
		assertEquals(0,alg.getFoundEllipses().size());
	}

	/**
	 * Input image is distorted
	 */
	@Test
	public void distortedImage() {
		List<EllipseRotated_F64> original = new ArrayList<>();

		original.add( new EllipseRotated_F64(50,65,20,10,0.5));
		original.add( new EllipseRotated_F64(90,100,25,25,0));


		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, original, 0);
		GrayU8 binary = image.createSameShape();
		ThresholdImageOps.threshold(image,binary,30,true);

		BinaryEllipseDetector<GrayU8> alg = create();
		PixelTransform2_F32 distToUndist = new PixelTransformAffine_F32(new Affine2D_F32(1,0,0,1,5,8));
		PixelTransform2_F32 undistToDist = new PixelTransformAffine_F32(new Affine2D_F32(1,0,0,1,-5,-8));
		alg.setLensDistortion(distToUndist, undistToDist);
		alg.process(image, binary);

		// adjust the ellipses using the transform
		List<EllipseRotated_F64> expected = new ArrayList<>();
		for( EllipseRotated_F64 o : original ) {
			EllipseRotated_F64 e = new EllipseRotated_F64(o);
			e.center.x += 5;
			e.center.y += 8;
			expected.add( e );
		}

		List<EllipseRotated_F64> found = alg.getFoundEllipses().toList();
		TestBinaryEllipseDetectorPixel.checkEquals(expected,found, 1.0, 0.1);
	}

	/**
	 * Turn off refinement and manually invoke it
	 */
	@Test
	public void autoRefineToggle() {
		List<EllipseRotated_F64> expected = new ArrayList<>();

		expected.add( new EllipseRotated_F64(50,65,20,10,0.5));
		expected.add( new EllipseRotated_F64(90,100,25,25,0));

		GrayU8 image = TestBinaryEllipseDetectorPixel.renderEllipses(200,210, expected, 0);
		GrayU8 binary = image.createSameShape();
		ThresholdImageOps.threshold(image,binary,30,true);

		BinaryEllipseDetector<GrayU8> alg = create();
		alg.setAutoRefine(false);

		alg.process(image, binary);

		List<EllipseRotated_F64> found = alg.getFoundEllipses().toList();
		List<EllipseRotated_F64> refined = new ArrayList<>();

		for( EllipseRotated_F64 f : found ) {
			EllipseRotated_F64 r = new EllipseRotated_F64(f);
			assertTrue(alg.refine(r));

			assertTrue( f.a != r.a );
			assertTrue( f.b != r.b );
			assertTrue( f.phi != r.phi );

			refined.add( r );
		}

		TestBinaryEllipseDetectorPixel.checkEquals(refined,found, 1.0, 0.1);
	}


	private static BinaryEllipseDetector<GrayU8> create() {
		BinaryEllipseDetectorPixel ellipseDetector = new BinaryEllipseDetectorPixel();
		SnapToEllipseEdge<GrayU8> ellipseRefiner = new SnapToEllipseEdge<>(20,2,GrayU8.class);
		EdgeIntensityEllipse<GrayU8> intensityCheck = new EdgeIntensityEllipse<>(2.0,20,THRESHOLD,GrayU8.class);

		return new BinaryEllipseDetector<>(ellipseDetector,ellipseRefiner,intensityCheck,GrayU8.class);
	}
}
