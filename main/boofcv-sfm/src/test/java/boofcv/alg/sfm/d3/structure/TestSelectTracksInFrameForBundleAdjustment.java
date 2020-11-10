/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm.d3.structure;

import boofcv.alg.sfm.d3.structure.SelectTracksInFrameForBundleAdjustment.Info;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment.BTrack;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.sfm.d3.structure.TestMaxGeoKeyFrameManager.connectFrames;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @author Peter Abeles
 */
class TestSelectTracksInFrameForBundleAdjustment extends BoofStandardJUnit {
	final int width = 100;
	final int height = 200;

	/**
	 * Basic test. Each track is visible in every frame at the same location.
	 */
	@Test
	void minimal_all() {
		var scene = new VisOdomBundleAdjustment<>(BTrack::new);
		var alg = new SelectTracksInFrameForBundleAdjustment(0xBEEF);
		alg.configUniform.regionScaleFactor = 1.0; // makes the math easier
		alg.maxFeaturesPerFrame = 200;
		var selected = new ArrayList<BTrack>();

		scene.addCamera(new CameraPinholeBrown(0, 0, 0, 0, 0, width, height));
		for (int i = 0; i < 5; i++) {
			scene.addFrame(i);
		}
		for (int i = 0; i < 200; i++) {
			scene.tracks.grow().id = i;
		}
		for (int i = 0; i < scene.frames.size; i++) {
			BFrame frame = scene.frames.get(i);
			for (int j = 0; j < 200; j++) {
				BTrack track = scene.tracks.get(j);
				frame.tracks.add(track);
				// pixel coordinates
				int x = (i*10)%width;
				int y = 10*((i*10)/width);
				VisOdomBundleAdjustment.BObservation o = track.observations.grow();
				o.frame = frame;
				o.pixel.setTo(x, y);
			}
		}
		alg.selectTracks(scene, selected);
		assertEquals(200, selected.size());
	}

	@Test
	void initializeGrid() {
		var scene = new VisOdomBundleAdjustment<>(BTrack::new);
		var alg = new SelectTracksInFrameForBundleAdjustment(0xBEEF);
		alg.minTrackObservations = 1;

		scene.addCamera(new CameraPinholeBrown(0, 0, 0, 0, 0, width, height));
		for (int i = 0; i < 3; i++) {
			scene.addFrame(i);
		}
		BFrame targetFrame = scene.frames.get(2);
		// create enough tracks for there to be one in each cell
		connectFrames(1, 2, 200, scene);
		for (int i = 0; i < scene.tracks.size; i++) {
			BTrack track = scene.tracks.get(i);
			// pixel coordinates
			int x = (i*10)%width;
			int y = 10*((i*10)/width);
			// make sure it's false. should be already
			track.selected = false;
			track.findObservationBy(targetFrame).pixel.setTo(x, y);
		}

		// mark this one as active so that it isn't added to a cell. There should only be one empty cell
		scene.tracks.get(2).selected = true;

		// run it
		alg.initializeGrid(targetFrame, width, height, 10);

		// There should be one track in all but one cell
		for (int i = 0; i < alg.grid.cells.size; i++) {
			Info cell = alg.grid.cells.get(i);
			if (i != 2) {
				assertEquals(0, cell.alreadySelected);
				assertEquals(1, cell.unselected.size());
				assertSame(scene.tracks.get(i), cell.unselected.get(0));
			} else {
				assertEquals(1, cell.alreadySelected);
				assertEquals(0, cell.unselected.size());
			}
		}
	}

	/**
	 * Simple scenario. One track in each cell and one cell has an already selected track. See if the expected number
	 * of tracks is returned
	 */
	@Test
	void selectNewTracks() {
		var scene = new VisOdomBundleAdjustment<>(BTrack::new);
		var alg = new SelectTracksInFrameForBundleAdjustment(0xBEEF);
		alg.maxFeaturesPerFrame = 200; // one less than the total number of tracks
		alg.grid.initialize(10, width, height);
		// populate the grid with one track each
		for (int row = 0; row < 20; row++) {
			for (int col = 0; col < 10; col++) {
				alg.grid.get(row, col).unselected.add(scene.tracks.grow());
			}
		}
		alg.grid.get(1, 1).alreadySelected = 1;

		List<BTrack> selected = new ArrayList<>();
		alg.selectNewTracks(selected);
		assertEquals(199, selected.size());
		for (int row = 0; row < 20; row++) {
			for (int col = 0; col < 10; col++) {
				assertEquals(0, alg.grid.get(row, col).alreadySelected);
				if (row == 1 && col == 1)
					assertEquals(1, alg.grid.get(row, col).unselected.size());
				else
					assertEquals(0, alg.grid.get(row, col).unselected.size());
			}
		}
	}
}
