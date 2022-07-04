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

import boofcv.struct.geo.PointIndex2D_F64;
import org.ddogleg.struct.DogArray;

/**
 * Set of observed calibration targets in a single frame from a multi camera calibration system.
 *
 * @author Peter Abeles
 */
public class ObservedCalTargets {
	/** When it was observed. All observations with the same frame will be assuemed to be simultaneous */
	public int frame;

	/** Which camera did the observation */
	public int cameraID;

	/** All observed calibration target landmarks in this frame */
	public final DogArray<Observation> targets = new DogArray<>(Observation::new, Observation::reset);

	public ObservedCalTargets setTo( ObservedCalTargets src ) {
		this.frame = src.frame;
		this.cameraID = src.cameraID;
		this.targets.reset();
		for (int i = 0; i < src.targets.size; i++) {
			this.targets.grow().setTo(src.targets.get(i));
		}
		return this;
	}

	public void reset() {
		frame = -1;
		cameraID = -1;
		targets.reset();
	}

	/**
	 * Specifies observation for a single target
	 */
	public static class Observation {
		/** Which target was observed */
		public int target;
		/** Landmarks that were observed in pixel coordinates */
		public final DogArray<PointIndex2D_F64> landmarks = new DogArray<>(PointIndex2D_F64::new);

		public Observation setTo( Observation src ) {
			this.target = src.target;
			landmarks.reset();
			for (int i = 0; i < src.landmarks.size; i++) {
				landmarks.grow().setTo(src.landmarks.get(i));
			}
			return this;
		}

		public void reset() {
			target = -1;
			landmarks.reset();
		}
	}
}
