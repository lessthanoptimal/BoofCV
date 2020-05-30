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

package boofcv.abst.tracker;

import boofcv.alg.tracker.dda.DetectDescribeAssociateTracker;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.tracker.dda.DetectDescribeAssociateTracker} for {@link PointTracker}
 *
 * @author Peter Abeles
 */
public class PointTrackerDda<I extends ImageGray<I>, TD extends TupleDesc>
	implements PointTracker<I>
{
	DetectDescribeAssociateTracker<I,TD> tracker;

	public PointTrackerDda(DetectDescribeAssociateTracker<I, TD> tracker) {
		this.tracker = tracker;
	}

	@Override public void process(I image) {tracker.process(image);}
	@Override public void reset() {tracker.reset();}
	@Override public long getFrameID() {return tracker.getFrameID();}
	@Override public int getTotalActive() {return tracker.getTracksActive().size();}
	@Override public int getTotalInactive() {return tracker.getTracksInactive().size();}
	@Override public void dropAllTracks() {tracker.dropAllTracks();}
	@Override public int getMaxSpawn() { return 0; } // not supported by this tracker
	@Override public boolean dropTrack(PointTrack track) {return tracker.dropTrack(track);}
	@Override public void dropTracks(Dropper dropper) {tracker.dropTracks(dropper);}
	@Override public void spawnTracks() { tracker.spawnTracks(); }

	@Override
	public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
		return addAllTracksInList(list, tracker.getTracksActive());
	}

	@Override
	public List<PointTrack> getDroppedTracks( List<PointTrack> list ) {
		return addAllTracksInList(list, tracker.getTracksDropped());
	}

	@Override
	public List<PointTrack> getNewTracks( List<PointTrack> list ) {
		return addAllTracksInList(list, tracker.getTracksNew());
	}

	@Override
	public List<PointTrack> getAllTracks( List<PointTrack> list ) {
		return addAllTracksInList(list, tracker.getTracksAll().toList());
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		return addAllTracksInList(list, tracker.getTracksInactive());
	}

	private static List<PointTrack> addAllTracksInList(List<PointTrack> list, List<PointTrack> tracksActive) {
		if (list == null)
			list = new ArrayList<>();
		else
			list.clear();

		list.addAll(tracksActive);
		return list;
	}
}
