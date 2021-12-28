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

package boofcv.abst.tracker;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.tracker.hybrid.HybridTrack;
import boofcv.alg.tracker.hybrid.HybridTrackerScalePoint;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.factory.transform.pyramid.FactoryPyramid;
import boofcv.struct.ConfigLength;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.ConfigDiscreteLevels;
import boofcv.struct.pyramid.PyramidDiscrete;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static boofcv.abst.tracker.PointTrackerUtils.addAllTracksInList;

/**
 * Wrapper around {@link HybridTrackerScalePoint} for {@link PointTracker}. Features are respawned when the
 * number of active tracks drops below a threshold automatically. This threshold is realtive to the number
 * of tracks spawned previously and is adjusted when the user requests that tracks are dropped.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked", "rawtypes", "NullAway.Init"})
public class PointTrackerHybrid<I extends ImageGray<I>, D extends ImageGray<D>, Desc extends TupleDesc<Desc>>
		implements PointTracker<I> {

	HybridTrackerScalePoint<I, D, Desc> tracker;

	// Threshold which decides enough tracks have been dropped that it should try to respawn some of the ones it
	// dropped
	public final ConfigLength thresholdRespawn = ConfigLength.relative(0.4, 50);

	// Number of active tracks after respawning
	private int countAfterSpawn;

	// Reference to image passed to process()
	I image;

	// Image pyramid data structures
	PyramidDiscrete<I> pyramid;
	D[] derivX;
	D[] derivY;

	// Image type used to store gradient
	ImageType<D> derivType;

	// Computes the image gradient
	ImageGradient<I, D> gradient;

	// Type of input image
	ImageType<I> inputType;

	// If true that means feature detection has already been called once and new features can be spawned
	boolean detectCalled;

	public PointTrackerHybrid( HybridTrackerScalePoint<I, D, Desc> tracker,
							   ConfigDiscreteLevels configLevels,
							   Class<I> imageType, Class<D> derivType ) {
		this.tracker = tracker;
		this.derivType = ImageType.single(derivType);
		this.inputType = ImageType.single(imageType);

		pyramid = FactoryPyramid.discreteGaussian(configLevels, -1, 2, true, ImageType.single(imageType));
		gradient = FactoryDerivative.sobel(imageType, derivType);

		reset();
	}

	@Override
	public void process( I image ) {
		this.image = image;
		detectCalled = false;

		// update the image pyramid
		pyramid.process(image);
		if (derivX == null) {
			derivX = PyramidOps.declareOutput(pyramid, derivType);
			derivY = PyramidOps.declareOutput(pyramid, derivType);
		}
		PyramidOps.gradient(pyramid, gradient, derivX, derivY);

		// Perform KLT tracking
		tracker.updateTracks(pyramid, derivX, derivY);
		// Perform DDA tracking when the number of pure KLT has dropped significantly from the previous attempt
		if (tracker.getTracksActive().size < thresholdRespawn.computeI(countAfterSpawn)) {
			detectCalled = true;
			tracker.associateInactiveTracks(image);
			countAfterSpawn = tracker.getTracksActive().size;
		}
		// Drop KLT tracks which are too close to each other
		tracker.pruneActiveTracksWhichAreTooClose();
		// Perform track maintenance
		tracker.dropExcessiveInactiveTracks();
	}

	@Override
	public void dropTracks( Dropper dropper ) {
		DogArray<HybridTrack<Desc>> all = tracker.getTracksAll();

		for (int i = all.size - 1; i >= 0; i--) {
			if (!dropper.shouldDropTrack(all.get(i)))
				continue;

			tracker.dropTrackByAllIndex(i);
			countAfterSpawn--;
		}
	}

	@Override public void spawnTracks() {
		if (!detectCalled) {
			tracker.associateInactiveTracks(image);
		}
		tracker.spawnNewTracks();
		countAfterSpawn = tracker.getTracksActive().size;
	}

	@Override public ImageType<I> getImageType() {
		return inputType;
	}

	@Override public void reset() {
		countAfterSpawn = 0;
		tracker.reset();
	}

	@Override public long getFrameID() {return tracker.getFrameID();}

	@Override public int getTotalActive() {return tracker.getTracksActive().size();}

	@Override public int getTotalInactive() {return tracker.getTracksInactive().size();}

	@Override public void dropAllTracks() {tracker.dropAllTracks();}

	@Override public int getMaxSpawn() {return 0;} // returning zero here since there is no good answer.

	@Override public boolean dropTrack( PointTrack track ) {
		if (tracker.dropTrack((HybridTrack<Desc>)track)) {
			countAfterSpawn--;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public List<PointTrack> getAllTracks( @Nullable List<PointTrack> list ) {
		return addAllTracksInList((List)tracker.getTracksAll().toList(), list);
	}

	@Override
	public List<PointTrack> getActiveTracks( @Nullable List<PointTrack> list ) {
		return addAllTracksInList((List)tracker.getTracksActive().toList(), list);
	}

	@Override
	public List<PointTrack> getInactiveTracks( @Nullable List<PointTrack> list ) {
		return addAllTracksInList((List)tracker.getTracksInactive().toList(), list);
	}

	@Override
	public List<PointTrack> getDroppedTracks( @Nullable List<PointTrack> list ) {
		return addAllTracksInList((List)tracker.getTracksDropped(), list);
	}

	@Override
	public List<PointTrack> getNewTracks( @Nullable List<PointTrack> list ) {
		return addAllTracksInList((List)tracker.getTracksSpawned(), list);
	}
}
