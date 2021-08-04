/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.geo.calibration;

import boofcv.alg.distort.LensDistortionNarrowFOV;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_I32;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestMultiToSingleFiducialCalibration extends BoofStandardJUnit {
	/**
	 * simple sanity check
	 */
	@Test void simple() {
		var mock = new MockMulti();

		mock.numMarkers = 3;
		mock.detectedMarkers.add(1);

		var alg = new MultiToSingleFiducialCalibration(mock);
		alg.targetMarker = 2;
		assertFalse(alg.process(new GrayF32(1,1)));
		assertTrue(mock.processCalled);

		alg.targetMarker = 1;
		assertTrue(alg.process(new GrayF32(1,1)));
		assertNotNull((alg.getDetectedPoints()));
		assertNotNull((alg.getLayout()));
		assertEquals(1, mock.getLayoutMarker);
	}

	private static class MockMulti implements DetectMultiFiducialCalibration {
		int numMarkers;
		DogArray_I32 detectedMarkers = new DogArray_I32();

		List<Point2D_F64> layout = new ArrayList<>();
		boolean processCalled = false;
		boolean setLens = false;
		int getLayoutMarker = -1;

		{
			for (int i = 0; i < 10; i++) {
				layout.add( new Point2D_F64(1,2));
			}
		}

		@Override public void process( GrayF32 input ) { processCalled = true; }

		@Override public int getDetectionCount() { return detectedMarkers.size; }

		@Override public int getMarkerID( int detectionID ) { return detectedMarkers.get(detectionID); }

		@Override public int getTotalUniqueMarkers() { return numMarkers; }

		@Override public CalibrationObservation getDetectedPoints( int detectionID ) {
			var det = new CalibrationObservation();
			for (int i = 0; i < 3; i++) {
				det.add(1.0, 2.0, i+2);
			}
			return det;
		}

		@Override public List<Point2D_F64> getLayout( int markerID ) {
			getLayoutMarker = markerID;
			return layout;
		}

		@Override public void setLensDistortion( LensDistortionNarrowFOV distortion, int width, int height ) {
			setLens = true;
		}
	}
}
