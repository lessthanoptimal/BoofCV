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

package boofcv.alg.sfm.structure;

import boofcv.abst.tracker.PointTrack;
import boofcv.struct.calib.CameraPinhole;
import boofcv.testing.BoofStandardJUnit;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestPointTrackerPerfectCloud extends BoofStandardJUnit {
	/** Test basic functionality */
	@Test void basics() {
		var tracker = new PointTrackerPerfectCloud<>();
		tracker.setCamera(new CameraPinhole(200,200,0,200,200,400,400));
		tracker.cloud.add(new Point3D_F64(0,0,2));
		tracker.cloud.add(new Point3D_F64(0.5,0,3));
		tracker.cloud.add(new Point3D_F64(0,0.5,-2));

		tracker.process(null);
		assertEquals(0, tracker.getFrameID());
		assertEquals(0, tracker.getTotalActive());

		// Only two tracks should be created since the 3rd is behind
		tracker.spawnTracks();
		assertEquals(2, tracker.getTotalActive());
		// sanity check the pixel coordinates
		List<PointTrack> active = tracker.getActiveTracks(null);
		assertEquals(2, active.size());
		assertEquals(200, active.get(0).pixel.x, UtilEjml.TEST_F64);
		assertEquals(200, active.get(0).pixel.y, UtilEjml.TEST_F64);
		assertTrue(active.get(1).pixel.x>0);
		assertEquals(200, active.get(1).pixel.y, UtilEjml.TEST_F64);

		// See what happens when we flip it around and it can only see the 3rd point
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ, 0, Math.PI, 0, tracker.world_to_view.R);
		tracker.process(null);
		// it should drop the two active tracks
		assertEquals(0, tracker.getTotalActive());
		assertEquals(2, tracker.getDroppedTracks(null).size());
		// it should just spawn the point with a negative coordinate
		tracker.spawnTracks();
		assertEquals(1, tracker.getTotalActive());
	}
}