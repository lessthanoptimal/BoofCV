/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.feature.tracker;

import boofcv.abst.feature.associate.AssociateDescription2D;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageGray;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;


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

	protected SetTrackInfo sets[];

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

	// number of features created.  Used to assign unique IDs
	protected long featureID = 0;

	// should it update the feature description after each association?
	boolean updateDescription;

	/**
	 * Configures tracker
	 *
	 * @param associate Association
	 * @param updateDescription If true then the feature description will be updated after each image.
	 *                          Typically this should be false.
	 */
	public DetectDescribeAssociate(DdaFeatureManager<I, Desc> manager,
								   final AssociateDescription2D<Desc> associate,
								   final boolean updateDescription ) {
		this.manager = manager;
		this.associate = associate;
		this.updateDescription = updateDescription;

		sets = new SetTrackInfo[manager.getNumberOfSets()];

		for (int i = 0; i < sets.length; i++) {
			sets[i] = new SetTrackInfo();
			sets[i].featSrc = new FastQueue<>(10, manager.getDescriptionType(), false);
			sets[i].featDst = new FastQueue<>(10, manager.getDescriptionType(), false);
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
		for (int i = 0; i < sets.length; i++) {
			sets[i].featDst.reset();
			sets[i].locDst.reset();
			sets[i].matches.reset();
		}
	}

	@Override
	public void process( I input ) {

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
				for( int j = 0; j < info.tracks.size(); j++ ) {
					if( !info.isAssociated[j] )
						tracksInactive.add(info.tracks.get(j));
				}

				// clean up
				for (int j = 0; j < sets.length; j++) {
					sets[j].featSrc.reset();
					sets[j].locSrc.reset();
				}
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
		FastQueue<AssociatedIndex> matches = associate.getMatches();
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
			info.locSrc.add(t);
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
			track.set(loc.x, loc.y);
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
		p.set(x, y);
		((Desc)p.getDescription()).setTo(desc);
		if( checkValidSpawn(setIndex,p) ) {
			p.setId = setIndex;
			p.featureId = featureID++;

			sets[setIndex].tracks.add(p);
			tracksNew.add(p);
			tracksActive.add(p);
			tracksAll.add(p);
			return p;
		} else {
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
		} else {
			p = new PointTrack();
			p.setDescription(manager.createDescription());
		}
		return p;
	}

	@Override
	public void dropAllTracks() {
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

		if( !sets[track.setId].tracks.remove(track) ) {
			return false;
		}
		// the track may or may not be in the active list
		tracksActive.remove(track);
		tracksInactive.remove(track);
		// it must be in the all list
		// recycle the data
		unused.add(track);
		return true;
	}

	@Override
	public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();

		list.addAll(tracksActive);
		return list;
	}

	@Override
	public List<PointTrack> getDroppedTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();

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

		list.addAll(tracksAll);
		return list;
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list == null )
			list = new ArrayList<>();

		list.addAll(tracksInactive);
		return list;
	}

	protected static class SetTrackInfo<Desc> {
		// location of interest points
		protected FastQueue<Point2D_F64> locDst = new FastQueue<>(10, Point2D_F64.class, false);
		protected FastQueue<Point2D_F64> locSrc = new FastQueue<>(10, Point2D_F64.class, true);
		// description of interest points
		protected FastQueue<Desc> featSrc;
		protected FastQueue<Desc> featDst;

		// indicates if a feature was associated or not
		protected boolean isAssociated[] = new boolean[1];

		protected List<PointTrack> tracks = new ArrayList<>();

		// Data returned by associate
		protected FastQueue<AssociatedIndex> matches = new FastQueue<>(AssociatedIndex.class,true);
	}
}
