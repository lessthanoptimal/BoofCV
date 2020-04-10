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
	/** The targeted maximum number of features it can detect */
	public int maxFeaturesPerFrame;
	/** Scales the size of a region up by the inverse of this number */
	public double inverseRegionScale = 0.25;
	/** The smallest allowed cell size */
	public int minCellLength = 5;
	/** The minimum number of observations to process */
	public int minObservations = 3;

	// grid cells. Stored in row major format
	ImageGrid<List<BTrack>> imageGrid = new ImageGrid<>(ArrayList::new, List::clear);

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
		int targetLength = selectTargetLength(imageWidth, imageHeight);

		// Fill each grid cell with tracks that are inside of it
		initializeGrid(frame, imageWidth, imageHeight, targetLength);

		int totalInFrame = handleAlreadySelected();
		selectNewTracks(selected, totalInFrame);
	}

	/**
	 * Selects the target length of a cell
	 */
	private int selectTargetLength(int imageWidth, int imageHeight) {
		int targetLength = (int)Math.ceil(Math.sqrt(imageWidth*imageHeight)/
				(0.1+Math.sqrt(maxFeaturesPerFrame *inverseRegionScale)));
		targetLength = Math.max(minCellLength,targetLength);
		return targetLength;
	}

	/**
	 * Initializes the grid data structure
	 */
	private void initializeGrid(BFrame frame, int imageWidth, int imageHeight, int targetLength) {
		imageGrid.initialize(targetLength,imageWidth, imageHeight);
		final FastArray<BTrack> tracks = frame.tracks;
		for (int trackIdx = 0; trackIdx < tracks.size; trackIdx++) {
			BTrack t = tracks.get(trackIdx);
			VisOdomBundleAdjustment.BObservation o = t.findObservationBy(frame);
			if( o == null )
				throw new RuntimeException("BUG! track in frame not observed by frame");
			imageGrid.getCellAtPixel((int)o.pixel.x, (int)o.pixel.y).add(t);
		}
	}

	/**
	 * Add all cells which have been selected previously remove them from each cell while incrementing the total
	 */
	private int handleAlreadySelected() {
		int total = 0;
		// In the first pass deal with tracks that have already been selected
		for (int i = 0; i < imageGrid.cells.size; i++) {
			List<BTrack> cell = imageGrid.cells.data[i];
			if( cell.isEmpty() )
				continue;
			// see if there are any tracks that have already been selected, add them towards the total but remove them
			// from the list since we are forced to use them
			for (int j = cell.size()-1; j >= 0; j-- ) {
				BTrack bt = cell.get(j);
				if( bt.selected ) {
					total++;
					cell.remove(j);
				} else if( !bt.active || bt.observations.size < minObservations) {
					// Remove inactive tracks since they should not be considered by bundle adjustment
					// Tracks with too few observations are more likely to be false positives
					cell.remove(j);
				}
			}
		}
		return total;
	}

	/**
	 * Go through the remaining tracks which have not been previously selected and picks them in a way which
	 * is approximately uniform across cells
	 */
	private void selectNewTracks(List<BTrack> selected, int total) {
		// Go through each grid cell one at a time and add a feature if there are any remaining
		// this will result in a more even distribution.
		while( true ) {
			int before = total;
			for (int i = 0; i < imageGrid.cells.size && total < maxFeaturesPerFrame; i++) {
				List<BTrack> cell = imageGrid.cells.data[i];
				if( cell.isEmpty() )
					continue;
				// Randomly select one of the available. Could probably do better but this is reasonable and "unbiased"
				int chosen = rand.nextInt(cell.size());
				BTrack bt = cell.remove(chosen);
				bt.selected = true;
				selected.add(bt);
				total++;
			}
			// See if exit condition has been meet
			if( before == total || total >= maxFeaturesPerFrame)
				break;
		}
	}
}
