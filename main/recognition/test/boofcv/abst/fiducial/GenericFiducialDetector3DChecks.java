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

package boofcv.abst.fiducial;

import boofcv.abst.distort.FDistort;
import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericFiducialDetector3DChecks extends GenericFiducialDetectorChecks{

	public abstract LensDistortionNarrowFOV loadDistortion( boolean distorted );

	public abstract FiducialDetector3D createDetector3D( ImageType imageType );

	public FiducialDetector createDetector( ImageType imageType ) {
		return createDetector3D(imageType);
	}

	/**
	 * Tests several items:
	 *
	 * 1) Does it properly handle setIntrinsic() being called multiple times
	 * 2) Can it handle no distortion
	 */
	@Test
	public void checkHandleNewIntrinsic() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector3D detector = createDetector3D(type);


			detector.setLensDistortion(loadDistortion(true));

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			Results results = extractResults(detector);

			// run it again with a changed intrinsic that's incorrect
			detector.setLensDistortion(loadDistortion(false));
			detector.detect(image);

			// results should have changed
			if( results.id.length == detector.totalFound()) {
				assertEquals(results.id.length, detector.totalFound());
				for (int i = 0; i < detector.totalFound(); i++) {
					assertEquals(results.id[i],detector.getId(i));
					Se3_F64 pose = new Se3_F64();
					Point2D_F64 pixel = new Point2D_F64();
					detector.getFiducialToCamera(i, pose);
					detector.getImageLocation(i, pixel);
					assertTrue(pose.getT().distance(results.pose.get(i).T) > 1e-4);
					assertFalse(MatrixFeatures.isIdentical(pose.getR(), results.pose.get(i).R, 1e-4));
					// pixel location is based on the observed location, thus changing the intrinsics should not
					// affect it
					assertTrue(results.pixel.get(i).distance(pixel) <= 2.0 );
				}
			} else {
				// clearly changed
			}

			// then reproduce original
			detector.setLensDistortion(loadDistortion(true));
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(results.id.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(results.id[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(results.pose.get(i).T),1e-8);
				assertTrue(MatrixFeatures.isIdentical(pose.getR(),results.pose.get(i).R,1e-8));
			}
		}
	}

	@Test
	public void checkMultipleRuns_3D() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector3D detector = createDetector3D(type);

			detector.setLensDistortion(loadDistortion(true));

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			Results results = extractResults(detector);

			// run it again
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(results.id.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(results.id[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(results.pose.get(i).T),1e-8);
				assertTrue(MatrixFeatures.isIdentical(pose.getR(),results.pose.get(i).R,1e-8));
			}
		}
	}

	/**
	 * See if the stability estimation is reasonable.  First detect targets in the full sized image.  Then shrink it
	 * by 15% and see if the instability increases.  The instability should always increase for smaller objects with
	 * the same orientation since the geometry is worse.
	 */
	@Test
	public void checkStability() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector3D detector = createDetector3D(type);

			detector.setLensDistortion(loadDistortion(true));

			detector.detect(image);
			assertTrue(detector.totalFound() >= 1);

			long foundIds[] = new long[ detector.totalFound() ];
			double location[] = new double[ detector.totalFound() ];
			double orientation[] = new double[ detector.totalFound() ];

			FiducialStability results = new FiducialStability();
			for (int i = 0; i < detector.totalFound(); i++) {
				detector.computeStability(i,0.2,results);

				foundIds[i] = detector.getId(i);
				location[i] = results.location;
				orientation[i] = results.orientation;
			}

			ImageBase shrunk = image.createSameShape();
			new FDistort(image,shrunk).affine(0.8,0,0,0.8,0,0).apply();

			detector.detect(shrunk);

			assertTrue(detector.totalFound() == foundIds.length);

			for (int i = 0; i < detector.totalFound(); i++) {
				detector.computeStability(i,0.2,results);

				long id = detector.getId(i);

				boolean matched = false;
				for (int j = 0; j < foundIds.length; j++) {
					if( foundIds[j] == id ) {
						matched = true;
						assertTrue(location[j] < results.location);
						assertTrue(orientation[j] < results.orientation);
						break;
					}
				}
				assertTrue(matched);
			}
		}
	}

	/**
	 * Check image location against 3D location
	 */
	@Test
	public void checkImageLocation() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector3D detector = createDetector3D(type);

			LensDistortionNarrowFOV factory = loadDistortion(true);
			detector.setLensDistortion(factory);

			detector.detect(image);

			assertTrue(detector.totalFound() >= 1);

			for (int i = 0; i < detector.totalFound(); i++) {
				Se3_F64 fidToCam = new Se3_F64();
				Point2D_F64 pixel = new Point2D_F64();
				detector.getFiducialToCamera(i, fidToCam);
				detector.getImageLocation(i, pixel);

//				Point3D
//				GeometryMath_F64.mult(fidToCam,);
//				Point2D_F64 rendered = new Point2D_F64();
//
//				WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(intrinsic, fidToCam);
//				worldToPixel.transform(new Point3D_F64(0,0,0),rendered);

				// see if the reprojected is near the pixel location
//				assertTrue( rendered.distance(pixel) <= pixelAndProjectedTol);
				fail("Implement");
			}
		}
	}

	private Results extractResults( FiducialDetector3D detector ) {
		Results out = new Results(detector.totalFound());

		for (int i = 0; i < detector.totalFound(); i++) {
			Se3_F64 pose = new Se3_F64();
			Point2D_F64 pixel = new Point2D_F64();
			detector.getFiducialToCamera(i, pose);
			detector.getImageLocation(i, pixel);

			out.id[i] = detector.getId(i);
			out.pose.add(pose);
			out.pixel.add(pixel);
		}

		return out;
	}

	private static class Results {
		public long id[];
		public List<Se3_F64> pose = new ArrayList<>();
		public List<Point2D_F64> pixel = new ArrayList<>();

		public Results( int N ) {
			id = new long[ N ];
			pose = new ArrayList<>();
			pixel = new ArrayList<>();
		}
	}
}
