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

import boofcv.alg.tracker.dda.DetectDescribeAssociateTracker;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.tracker.dda.DetectDescribeAssociateTracker} for {@link PointTracker}
 *
 * @author Peter Abeles
 */
public class PointTrackerDda<I extends ImageGray<I>, TD extends TupleDesc<TD>>
		implements PointTracker<I> {
	DetectDescribeAssociateTracker<I, TD> tracker;

	public PointTrackerDda( DetectDescribeAssociateTracker<I, TD> tracker ) {
		this.tracker = tracker;
	}

	// @formatter:off
	@Override public void process( I image ) {tracker.process(image);}
	@Override public void reset() {tracker.reset();}
	@Override public long getFrameID() {return tracker.getFrameID();}
	@Override public int getTotalActive() {return tracker.getTracksActive().size();}
	@Override public int getTotalInactive() {return tracker.getTracksInactive().size();}
	@Override public void dropAllTracks() {tracker.dropAllTracks();}
	@Override public int getMaxSpawn() { return 0; } // not supported by this tracker
	@Override public boolean dropTrack( PointTrack track ) {return tracker.dropTrack(track);}
	@Override public void dropTracks( Dropper dropper ) {tracker.dropTracks(dropper);}
	@Override public void spawnTracks() {tracker.spawnTracks();}
	@Override public ImageType<I> getImageType() {return tracker.getImageType();}
	// @formatter:on

	@Override
	public List<PointTrack> getActiveTracks( @Nullable List<PointTrack> list ) {
		return PointTrackerUtils.addAllTracksInList(tracker.getTracksActive(), list);
	}

	@Override
	public List<PointTrack> getDroppedTracks( @Nullable List<PointTrack> list ) {
		return PointTrackerUtils.addAllTracksInList(tracker.getTracksDropped(), list);
	}

	@Override
	public List<PointTrack> getNewTracks( @Nullable List<PointTrack> list ) {
		return PointTrackerUtils.addAllTracksInList(tracker.getTracksNew(), list);
	}

	@Override
	public List<PointTrack> getAllTracks( @Nullable List<PointTrack> list ) {
		return PointTrackerUtils.addAllTracksInList(tracker.getTracksAll().toList(), list);
	}

	@Override
	public List<PointTrack> getInactiveTracks( @Nullable List<PointTrack> list ) {
		return PointTrackerUtils.addAllTracksInList(tracker.getTracksInactive(), list);
	}
}
