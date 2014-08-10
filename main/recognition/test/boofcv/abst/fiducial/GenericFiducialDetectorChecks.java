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

package boofcv.abst.fiducial;

import boofcv.struct.calib.IntrinsicParameters;
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

	public abstract IntrinsicParameters loadIntrinsic();

	public abstract FiducialDetector createDetector( ImageType imageType );

	/**
	 * See if it blows if if there are no distortion parameters
	 */
	@Test
	public void checkNoDistortion() {
		IntrinsicParameters param = loadIntrinsic();

		if( param.radial == null )
			fail("radial must not be null");

		// normal case.  should work
		for( ImageType type : types ) {
			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);
			detector.setIntrinsic(param);
			detector.detect(image);
		}

		// null.  might blow up
		param.radial = null;
		for( ImageType type : types ) {
			ImageBase image = loadImage(type);
			FiducialDetector detector = createDetector(type);
			detector.setIntrinsic(param);
			detector.detect(image);
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

			int foundID[] = new int[ detector.totalFound() ];
			List<Se3_F64> foundPose = new ArrayList<Se3_F64>();

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToWorld(i,pose);
				foundPose.add(pose);
			}

			// run it again
			detector.detect(image);

			// see if it produced exactly the same results
			assertEquals(foundID.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(foundID[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToWorld(i,pose);
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

			int foundID[] = new int[ detector.totalFound() ];
			List<Se3_F64> foundPose = new ArrayList<Se3_F64>();

			for (int i = 0; i < detector.totalFound(); i++) {
				foundID[i] = detector.getId(i);
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToWorld(i,pose);
				foundPose.add(pose);
			}

			// run it again with a sub-image
			detector.detect(BoofTesting.createSubImageOf(image));

			// see if it produced exactly the same results
			assertEquals(foundID.length,detector.totalFound());
			for (int i = 0; i < detector.totalFound(); i++) {
				assertEquals(foundID[i],detector.getId(i));
				Se3_F64 pose = new Se3_F64();
				detector.getFiducialToWorld(i,pose);
				assertEquals(0,pose.getT().distance(foundPose.get(i).T),1e-8);
				assertTrue(MatrixFeatures.isIdentical(pose.getR(),foundPose.get(i).R,1e-8));
			}
		}
	}
}
