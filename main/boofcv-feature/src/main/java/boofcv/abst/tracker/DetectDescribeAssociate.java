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

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastAccess;
import org.ddogleg.struct.FastArray;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * <p>
 * Base class for detect-describe-associate type trackers. Tracker works by detecting features in each image,
 * computing a descriptor for each feature, then associating the features together.
 * </p>
 *
 * @author Peter Abeles
 */
public class DetectDescribeAssociate<I extends ImageGray<I>, Desc extends TupleDesc>
		implements PointTracker<I> {

	// associates features between two images together
	protected AssociateDescription2D<Desc> associate;

	// Detects features and manages descriptions
	protected DdaFeatureManager<I,Desc> manager;

	protected SetTrackInfo<Desc>[] sets;

	// all tracks
	protected List<PointTrack> tracksAll = new ArrayList<>();
	// recently associated tracks
	protected List<PointTrack> tracksActive = new ArrayList<>();
	// tracks not matched to any recent features
	protected List<PointTrack> tracksInactive = new ArrayList<>();
	// tracks dropped by the tracker
	protected List<PointTrack> tracksDropped = new ArrayList<>();
	// tracks recently spawned
	protected List<PointTrack> tracksNew = new ArrayList<>();

	// previously declared tracks which are being recycled
	protected List<PointTrack> unused = new ArrayList<>();

	// ID of the most recently processed frame
	protected long frameID=-1;

	// number of features created.  Used to assign unique IDs
	protected long featureID = 0;

	// should it update the feature description after each association?
	boolean updateDescription;

	// maximum number of tracks it will keep track of that were not associated before it starts discarding
	protected int maxInactiveTracks;
	protected GrowQueue_I32 unassociatedIdx = new GrowQueue_I32();

	// Random number generator
	protected Random rand;

	/**
	 * Configures tracker
	 *
	 * @param associate Association
	 * @param config Configures behavior.
	 */
	public DetectDescribeAssociate(DdaFeatureManager<I, Desc> manager,
								   final AssociateDescription2D<Desc> associate,
								   ConfigTrackerDda config ) {
		this.manager = manager;
		this.associate = associate;
		this.updateDescription = config.updateDescription;
		this.maxInactiveTracks = config.maxUnusedTracks;
		this.rand = new Random(config.seed);

		sets = new SetTrackInfo[manager.getNumberOfSets()];

		for (int i = 0; i < sets.length; i++) {
			sets[i] = new SetTrackInfo<>();
			sets[i].featSrc = new FastArray<>(manager.getDescriptionType());
			sets[i].featDst = new FastArray<>(manager.getDescriptionType());
		}

	}

	protected DetectDescribeAssociate() {
	}

	public boolean isUpdateDescription() {
		return updateDescription;
	}

	/**
	 * If a feature is associated should the description be updated with the latest observation?
	 */
	public void setUpdateDescription(boolean updateDescription) {
		this.updateDescription = updateDescription;
	}

	@Override
	public void reset() {
		dropAllTracks();
		featureID = 0;
		frameID = -1;
		for (int i = 0; i < sets.length; i++) {
			sets[i].featDst.reset();
			sets[i].locDst.reset();
			sets[i].matches.reset();
		}
	}

	@Override
	public long getFrameID() {
		return frameID;
	}

	@Override
	public void process( I input ) {
		frameID++;
		tracksActive.clear();
		tracksInactive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		manager.detectFeatures(input);
		for (int setIndex = 0; setIndex < sets.length; setIndex++) {
			SetTrackInfo<Desc> info = sets[setIndex];

			info.featDst.reset();
			info.locDst.reset();
			manager.getFeatures(setIndex,info.locDst,info.featDst);

			// skip if there are no features
			if( !info.tracks.isEmpty() ) {

				performTracking(info);

				// add unassociated to the list
				unassociatedIdx.reset();
				for( int j = 0; j < info.tracks.size(); j++ ) {
					if( !info.isAssociated[j] ) {
						unassociatedIdx.add(j);
						tracksInactive.add(info.tracks.get(j));
					}
				}

				pruneTracks(info, unassociatedIdx);

				// clean up
				for (int j = 0; j < sets.length; j++) {
					sets[j].featSrc.reset();
					sets[j].locSrc.reset();
				}
			}
		}
	}

	/**
	 * If there are too many unassociated tracks, randomly select some of those tracks and drop them
	 */
	private void pruneTracks(SetTrackInfo<Desc> info, GrowQueue_I32 unassociated) {
		if( unassociated.size > maxInactiveTracks ) {
			// make the first N elements the ones which will be dropped
			int numDrop = unassociated.size-maxInactiveTracks;
			for (int i = 0; i < numDrop; i++) {
				int selected = rand.nextInt(unassociated.size-i)+i;
				int a = unassociated.get(i);
				unassociated.data[i] = unassociated.data[selected];
				unassociated.data[selected] = a;
			}
			List<PointTrack> dropList = new ArrayList<>();
			for (int i = 0; i < numDrop; i++) {
				dropList.add( info.tracks.get(unassociated.get(i)) );
			}
			for (int i = 0; i < dropList.size(); i++) {
				dropTrack(dropList.get(i));
			}
		}
	}

	protected void performTracking( SetTrackInfo<Desc> info ) {
		// create source list
		putIntoSrcList(info);

		// associate features together
		associate.setSource(info.locSrc, info.featSrc);
		associate.setDestination(info.locDst, info.featDst);
		associate.associate();

		// used in spawn tracks.  if null then no tracking data is assumed
		FastAccess<AssociatedIndex> matches = associate.getMatches();
		// create a copy since the data will be recycled if there are multiple sets of points
		info.matches.resize(matches.size);
		for (int i = 0; i < matches.size; i++) {
			info.matches.get(i).set(matches.get(i));
		}

		// Update the track state using association information
		updateTrackState(info);
	}

	/**
	 * Put existing tracks into source list for association
	 */
	protected void putIntoSrcList( SetTrackInfo<Desc> info ) {
		// make sure isAssociated is large enough
		if( info.isAssociated.length < info.tracks.size() ) {
			info.isAssociated = new boolean[ info.tracks.size() ];
		}

		info.featSrc.reset();
		info.locSrc.reset();

		for( int i = 0; i < info.tracks.size(); i++ ) {
			PointTrack t = info.tracks.get(i);
			Desc desc = t.getDescription();
			info.featSrc.add(desc);
			info.locSrc.add(t.pixel);
			info.isAssociated[i] = false;
		}
	}

	/**
	 * Update each track's location and description (if configured to do so) mark tracks as being associated.
	 */
	protected void updateTrackState( SetTrackInfo<Desc> info ) {
		// update tracks
		for( int i = 0; i < info.matches.size; i++ ) {
			AssociatedIndex indexes = info.matches.data[i];
			PointTrack track = info.tracks.get(indexes.src);
			Point2D_F64 loc = info.locDst.data[indexes.dst];
			track.pixel.set(loc.x, loc.y);
			tracksActive.add(track);

			// update the description
			if(updateDescription) {
				((Desc)track.getDescription()).setTo(info.featDst.get(indexes.dst));
			}

			info.isAssociated[indexes.src] = true;
		}
	}

	/**
	 * Takes the current crop of detected features and makes them the keyframe
	 */
	@Override
	public void spawnTracks() {
		for (int setIndex = 0; setIndex < sets.length; setIndex++) {
			SetTrackInfo<Desc> info = sets[setIndex];

			// setup data structures
			if( info.isAssociated.length < info.featDst.size ) {
				info.isAssociated = new boolean[ info.featDst.size ];
			}

			// see which features are associated in the dst list
			for( int i = 0; i < info.featDst.size; i++ ) {
				info.isAssociated[i] = false;
			}

			for( int i = 0; i < info.matches.size; i++ ) {
				info.isAssociated[info.matches.data[i].dst] = true;
			}

			// create new tracks from latest unassociated detected features
			for( int i = 0; i < info.featDst.size; i++ ) {
				if( info.isAssociated[i] )
					continue;

				Point2D_F64 loc = info.locDst.get(i);
				addNewTrack(setIndex, loc.x,loc.y,info.featDst.get(i));
			}
		}

	}

	/**
	 * Adds a new track given its location and description
	 */
	protected PointTrack addNewTrack( int setIndex, double x , double y , Desc desc ) {
		PointTrack p = getUnused();
		p.pixel.set(x, y);
		((Desc)p.getDescription()).setTo(desc);
		if( checkValidSpawn(setIndex,p) ) {
			if( tracksAll.contains(p))
				throw new RuntimeException("Contained twice all! p.id="+p.featureId);
			if( tracksActive.contains(p))
				throw new RuntimeException("Contained twice active! p.id="+p.featureId);
			p.spawnFrameID = frameID;
			p.setId = setIndex;
			p.featureId = featureID++;

			sets[setIndex].tracks.add(p);
			tracksNew.add(p);
			tracksActive.add(p);
			tracksAll.add(p);
			return p;
		} else {
			if( tracksAll.contains(p))
				throw new RuntimeException("Contained??? ! p.id="+p.featureId);
			if( unused.contains(p))
				throw new RuntimeException("Already in unused!");
			unused.add(p);
			return null;
		}
	}

	/**
	 * Returns true if a new track can be spawned here.  Intended to be overloaded
	 */
	protected boolean checkValidSpawn( int setIndex, PointTrack p ) {
		return true;
	}

	/**
	 * Returns an unused track.  If there are no unused tracks then it creates a ne one.
	 */
	protected PointTrack getUnused() {
		PointTrack p;
		if( unused.size() > 0 ) {
			p = unused.remove( unused.size()-1 );
			if( unused.contains(p))
				throw new RuntimeException("BUG!");
		} else {
			p = new PointTrack();
			p.setDescription(manager.createDescription());
		}
		return p;
	}

	@Override
	public void dropAllTracks() {
		for( var p : tracksAll ) {
			if( unused.contains(p) ) {
				throw new RuntimeException("Already in unused!");
			}
		}
		unused.addAll(tracksAll);
		tracksActive.clear();
		tracksInactive.clear();
		tracksAll.clear();
		tracksNew.clear();

		for (int setIndex = 0; setIndex < sets.length; setIndex++) {
			SetTrackInfo<Desc> info = sets[setIndex];
			info.tracks.clear();
		}
	}

	/**
	 * Remove from active list and mark so that it is dropped in the next cycle
	 *
	 * @param track The track which is to be dropped
	 */
	@Override
	public boolean dropTrack(PointTrack track) {

		if( !tracksAll.remove(track) )
			return false;
		if( tracksAll.contains(track))
			throw new RuntimeException("Contained twice all! pt.id="+track.featureId);
		if( unused.contains(track))
			throw new RuntimeException("Already in unused!");

		if( !sets[track.setId].tracks.remove(track) ) {
			throw new RuntimeException("Not in set!?!");
//			return false;
		}
		// the track may or may not be in the active list
		tracksActive.remove(track);
		tracksInactive.remove(track);
		if( tracksActive.contains(track))
			throw new RuntimeException("Contained twice active! pt.id="+track.featureId);

		// it must be in the all list
		// recycle the data
		unused.add(track);
		track.setId = -1;
		track.featureId = -1;
		return true;
	}

	@Override
	public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();
		else
			list.clear();

		list.addAll(tracksActive);
		return list;
	}

	@Override
	public List<PointTrack> getDroppedTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();
		else
			list.clear();

		list.addAll(tracksDropped);
		return list;
	}

	@Override
	public List<PointTrack> getNewTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();

		list.addAll(tracksNew);
		return list;
	}

	@Override
	public List<PointTrack> getAllTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();
		else
			list.clear();

		list.addAll(tracksAll);
		return list;
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list == null )
			list = new ArrayList<>();
		else
			list.clear();

		list.addAll(tracksInactive);
		return list;
	}

	protected static class SetTrackInfo<Desc> {
		// location of interest points
		protected FastArray<Point2D_F64> locDst = new FastArray<>(Point2D_F64.class);
		protected FastQueue<Point2D_F64> locSrc = new FastQueue<>(Point2D_F64::new,o->o.set(0,0));
		// description of interest points
		protected FastArray<Desc> featSrc;
		protected FastArray<Desc> featDst;

		// indicates if a feature was associated or not
		protected boolean isAssociated[] = new boolean[1];

		protected List<PointTrack> tracks = new ArrayList<>();

		// Data returned by associate
		protected FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class,true);
	}
}
