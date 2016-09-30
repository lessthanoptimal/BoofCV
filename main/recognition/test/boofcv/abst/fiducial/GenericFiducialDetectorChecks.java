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
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.testing.BoofTesting;
import georegression.struct.se.Se3_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Peter Abeles
 */
public abstract class GenericFiducialDetectorChecks {

	protected List<ImageType> types = new ArrayList<ImageType>();

	public abstract ImageBase loadImage(ImageType imageType);

	public abstract CameraPinholeRadial loadIntrinsic();

	public abstract FiducialDetector createDetector( ImageType imageType );

	/**
	 * See if it blows if if there are no distortion parameters
	 */
	@Test
	public void checkNoDistortion() {
		CameraPinholeRadial param = loadIntrinsic();

		if( !param.isDistorted() )
			fail("intrinsic must be distorted");

		// normal case.  should work
		for( ImageType type : types ) {
			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);
			detector.setIntrinsic(param);
			detector.detect(image);
		}

		// null.  might blow up
		param.radial = null;
		param.t1 = param.t2 = 0;
		for( ImageType type : types ) {
			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);
			detector.setIntrinsic(param);
			detector.detect(image);
		}
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
			FiducialDetector detector = createDetector(type);

			CameraPinholeRadial intrinsic = loadIntrinsic();
			assertTrue(intrinsic.isDistorted());

			detector.setIntrinsic(intrinsic);

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			long foundID[] = new long[ detector.totalFound() ];
			List<Se3_F64> foundPose = new ArrayList<Se3_F64>();

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				foundPose.add(pose);
			}

			// run it again with a changed intrinsic that's incorrect
			intrinsic.radial = null;
			intrinsic.t1 = 0; intrinsic.t2 = 0;
			detector.setIntrinsic(intrinsic);
			detector.detect(image);

			// results should have changed
			if( foundID.length == detector.totalFound()) {
				assertEquals(foundID.length, detector.totalFound());
				for (int i = 0; i < detector.totalFound(); i++) {
//				assertEquals(foundID[i],detector.getId(i));
					Se3_F64 pose = new Se3_F64();
					detector.getFiducialToCamera(i, pose);
					assertTrue(pose.getT().distance(foundPose.get(i).T) > 1e-4);
					assertFalse(MatrixFeatures.isIdentical(pose.getR(), foundPose.get(i).R, 1e-4));
				}
			} else {
				// clearly changed
			}

			// then reproduce original
			detector.setIntrinsic(loadIntrinsic());
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(foundID.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(foundID[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(foundPose.get(i).T),1e-8);
				assertTrue(MatrixFeatures.isIdentical(pose.getR(),foundPose.get(i).R,1e-8));
			}
		}
	}

	/**
	 * Makes sure the input is not modified
	 */
	@Test
	public void modifyInput() {
		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			ImageBase orig = image.clone();
			FiducialDetector detector = createDetector(type);

			detector.setIntrinsic(loadIntrinsic());

			detector.detect(image);

			BoofTesting.assertEquals(image,orig,0);
		}
	}

	@Test
	public void checkMultipleRuns() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.setIntrinsic(loadIntrinsic());

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			long foundID[] = new long[ detector.totalFound() ];
			List<Se3_F64> foundPose = new ArrayList<Se3_F64>();

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				foundPose.add(pose);
			}

			// run it again
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(foundID.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(foundID[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(foundPose.get(i).T),1e-8);
				assertTrue(MatrixFeatures.isIdentical(pose.getR(),foundPose.get(i).R,1e-8));
			}
		}
	}

	@Test
	public void checkSubImage() {

		for( ImageType type : types ) {

			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);

			detector.setIntrinsic(loadIntrinsic());

			detector.detect(image);

			assertTrue(detector.totalFound()>= 1);

			long foundID[] = new long[ detector.totalFound() ];
			List<Se3_F64> foundPose = new ArrayList<Se3_F64>();

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				foundPose.add(pose);
			}

			// run it again with a sub-image
			detector.detect(BoofTesting.createSubImageOf(image));

			// see if it produced exactly the same results
			assertEquals(foundID.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(foundID[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToCamera(i, pose);
				assertEquals(0,pose.getT().distance(foundPose.get(i).T),1e-8);
				assertTrue(MatrixFeatures.isIdentical(pose.getR(),foundPose.get(i).R,1e-8));
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
			FiducialDetector detector = createDetector(type);

			detector.setIntrinsic(loadIntrinsic());

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
}
