/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.distort.brown.LensDistortionBrown;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.jupiter.api.Test;

import static georegression.struct.se.SpecialEuclideanOps_F64.eulerXyz;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class GenericFiducialTrackerChecks extends GenericFiducialDetectorChecks {
	@Override
	protected void detect(FiducialDetector detector, ImageBase image) {
		((FiducialTracker)detector).reset();
		detector.detect(image);
	}

	/**
	 * Send it through a sequence of image and see if it blows up.
	 *
	 * Could improve the tracking test by resetting the tracker and seeing if the output changes.
	 */
	@Test
	void checkTracking() {
		boolean distorted = false;
		LensDistortionBrown lensDistorted = new LensDistortionBrown(loadDistortion(distorted));

		for (ImageType type : types) {
			FiducialDetector detector = createDetector(type);
			Se3_F64 pose = new Se3_F64();

			for (int timeStep = 0; timeStep < 10; timeStep++) {
				Se3_F64 markerToWorld = eulerXyz(-0.05*timeStep,0,1.5,0.1,Math.PI,0.15*timeStep,null);

				ImageBase imageD = renderImage(loadDistortion(distorted),markerToWorld, type);
				detector.setLensDistortion(lensDistorted, imageD.width, imageD.height);
				detector.detect(imageD);

//				ShowImages.showBlocking(imageD,"Distorted", 1_000);
				assertEquals(1, detector.totalFound());

				detector.getFiducialToCamera(0, pose);

				pose.T.scale(markerToWorld.T.norm()/pose.T.norm());
				Se3_F64 diff = markerToWorld.concat(pose.invert(null),null);
				double theta = ConvertRotation3D_F64.matrixToRodrigues(diff.R, null).theta;

				assertEquals(0,diff.T.norm(), tolAccuracyT);
				assertEquals(Math.PI,theta, tolAccuracyTheta);
			}
		}
	}
}
