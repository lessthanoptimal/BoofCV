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

package boofcv.alg.sfm.d3;

import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BFrame;
import boofcv.alg.sfm.d3.VisOdomBundleAdjustment.BTrack;
import boofcv.struct.ConfigGridUniform;
import boofcv.struct.ImageGrid;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Attempts to ensure spatial diversity within an image by forcing a more uniform distribution of features per-area.
 *
 * @author Peter Abeles
 */
public class VisOdomSelectFrameTracks {
	private final Random rand;

	/** Configuration for uniformally selecting a grid */
	public final ConfigGridUniform configUniform = new ConfigGridUniform();

	/** maximum number of features per frame that can be used */
	public int maxFeaturesPerFrame;

	/** The minimum number of observations to process */
	public int minTrackObservations = 3;

	// grid cells. Stored in row major format
	ImageGrid<Info> grid = new ImageGrid<>(Info::new,Info::reset);

	public VisOdomSelectFrameTracks( long randSeed ) {
		rand = new Random(randSeed);
	}

	public void selectTracks( VisOdomBundleAdjustment<?> sba, int imageWidth , int imageHeight, List<BTrack> selected )
	{
		// Initialize data structures
		selected.clear();

		// Mark all tracks as not selected
		for (int i = 0; i < sba.tracks.size; i++) {
			sba.tracks.get(i).selected = false;
		}

		// skip degenerate situation
		FastQueue<BFrame> frames = sba.frames;
		if( frames.size < 1 )
			return;

		// Start with older frames since we want to be biased to select tracks that have been seen by more frames
		for (int frameIdx = 0; frameIdx < frames.size; frameIdx++) {
			selectTracksInFrame(frames.get(frameIdx),imageWidth,imageHeight,selected);
		}
	}

	/**
	 * Select tracks inside a single frame. All tracks which have previously been selected are automatically selected
	 * again and count towards the max per frame
	 */
	protected void selectTracksInFrame(BFrame frame, int imageWidth , int imageHeight, List<BTrack> selected)
	{
		// This is the length of a side in the square grid that's to be selected
		// designed to avoid divide by zero error and have larger cells when fewer features are requested
		int targetSize = configUniform.selectTargetCellSize(maxFeaturesPerFrame,imageWidth, imageHeight);

		// Fill each grid cell with tracks that are inside of it
		initializeGrid(frame, imageWidth, imageHeight, targetSize);

		selectNewTracks(selected);
	}


	/**
	 * Initializes the grid data structure. Counts number of already selected tracks and adds unselected
	 * tracks to the list. A track is only considered for selection if it has the minimum number of observations.
	 * Otherwise it's likely to be a false positive.
	 */
	private void initializeGrid(BFrame frame, int imageWidth, int imageHeight, int targetLength) {
		grid.initialize(targetLength,imageWidth, imageHeight);
		final FastArray<BTrack> tracks = frame.tracks;
		for (int trackIdx = 0; trackIdx < tracks.size; trackIdx++) {
			BTrack t = tracks.get(trackIdx);
			VisOdomBundleAdjustment.BObservation o = t.findObservationBy(frame);
			if( o == null )
				throw new RuntimeException("BUG! track in frame not observed by frame");
			Info cell = grid.getCellAtPixel((int)o.pixel.x, (int)o.pixel.y);
			if( t.selected )
				cell.alreadySelected++;
			else if( t.observations.size >= minTrackObservations )
				cell.unselected.add(t);
		}
	}

	/**
	 * Selects new tracks such that it is uniform across the image. This takes in account the location of already
	 * selected tracks.
	 */
	private void selectNewTracks(List<BTrack> selected) {
		int total = 0;

		// Go through each grid cell one at a time and add a feature if there are any remaining
		// this will result in a more even distribution.
		while( true ) {
			int before = total;
			for (int i = 0; i < grid.cells.size && total < maxFeaturesPerFrame; i++) {
				Info cell = grid.cells.data[i];

				// See if there are remaining points that have already been selected. If so count that as a selection
				// and move on
				if( cell.alreadySelected > 0 ) {
					cell.alreadySelected--;
					total++;
					continue;
				}
				// nothing to select
				if( cell.unselected.isEmpty() )
					continue;

				// Randomly select one of the available. Could probably do better but this is reasonable and "unbiased"
				int chosen = rand.nextInt(cell.unselected.size());
				BTrack bt = cell.unselected.remove(chosen);
				bt.selected = true;
				selected.add(bt);
				total++;
			}
			// See if exit condition has been meet
			if( before == total || total >= maxFeaturesPerFrame)
				break;
		}
	}

	/**
	 * Info for each cell
	 */
	private static class Info
	{
		// counter for tracks which were selected in a previous frame
		public int alreadySelected = 0;
		// list of tracks which have not been selected yet
		public final List<BTrack> unselected = new ArrayList<>();

		public void reset() {
			alreadySelected = 0;
			unselected.clear();
		}
	}
}
