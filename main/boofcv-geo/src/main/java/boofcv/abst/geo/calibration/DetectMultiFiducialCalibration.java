/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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
import georegression.struct.point.Point2D_F64;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Calibration targets which can detect multiple targets at once with unique IDs
 *
 * @author Peter Abeles
 */
public interface DetectMultiFiducialCalibration {

	/**
	 * Image processing for calibration target detection
	 *
	 * @param input Gray scale image containing calibration target
	 */
	void process( GrayF32 input );

	/** Returns the number of detected markers */
	int getDetectionCount();

	/** Returns the number of unique markers that it can detect */
	int getTotalUniqueMarkers();

	/**
	 * Returns the set of detected points from the most recent call to {@link #process(GrayF32)}. Each
	 * time this function is invoked a new instance of the list and points is returned. No data reuse here.
	 *
	 * @param detectionID Which detection should it return the points for
	 * @return List of detected points in row major grid order.
	 */
	CalibrationObservation getDetectedPoints( int detectionID );

	/**
	 * Returns the layout of the calibration points on the target
	 *
	 * @param markerID Which marker should it return the layout of
	 * @return List of calibration points
	 */
	List<Point2D_F64> getLayout( int markerID );

	/**
	 * Returns the layout for all markers as a list.
	 */
	default List<List<Point2D_F64>> getLayouts() {
		var list = new ArrayList<List<Point2D_F64>>();
		for (int i = 0; i < getTotalUniqueMarkers(); i++) {
			list.add(getLayout(i));
		}
		return list;
	}

	/**
	 * Returns the observations with the most detected landmarks for each specific target
	 */
	default List<CalibrationObservation> getBestForEachTarget() {
		var markerToObservations = new HashMap<Integer, CalibrationObservation>();
		for (int i = 0; i < getDetectionCount(); i++) {
			CalibrationObservation o = getDetectedPoints(i);
			CalibrationObservation previousBest = markerToObservations.get(o.target);

			// First time it's seen this target ID or if the new observation has more points
			if (previousBest == null || previousBest.size() < o.size()) {
				markerToObservations.put(o.target, o);
			}
		}

		return new ArrayList<>(markerToObservations.values());
	}

	/**
	 * Explicitly handles lens distortion when detecting image features. If used, features will be found in
	 * undistorted pixel coordinates
	 */
	void setLensDistortion( @Nullable LensDistortionNarrowFOV distortion, int width, int height );
}
