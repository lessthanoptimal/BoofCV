/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.calibration;

import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

/**
 * Set of observed calibration targets in a single frame from a single camera
 *
 * @author Peter Abeles
 */
public class CalibrationObservationSet {
	/** Which camera did the observation */
	public int cameraID;

	/** All observed calibration target landmarks in this frame */
	public final DogArray<CalibrationObservation> targets = new DogArray<>(CalibrationObservation::new, CalibrationObservation::reset);

	public CalibrationObservationSet setTo( CalibrationObservationSet src ) {
		this.cameraID = src.cameraID;
		this.targets.reset();
		for (int i = 0; i < src.targets.size; i++) {
			this.targets.grow().setTo(src.targets.get(i));
		}
		return this;
	}

	public void reset() {
		cameraID = -1;
		targets.reset();
	}

	/**
	 * Exhaustively searches for a target with the specified ID. Returns null if no match is found.
	 */
	public @Nullable CalibrationObservation findTarget( int targetID ) {
		for (int i = 0; i < targets.size; i++) {
			if (targets.get(i).target == targetID) {
				return targets.get(i);
			}
		}
		return null;
	}
}
