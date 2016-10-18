/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
public class DetectDescribeAssociate<I extends ImageGray, Desc extends TupleDesc>
		implements PointTracker<I> {

	// associates features between two images together
	protected AssociateDescription2D<Desc> associate;

	// Detects features and manages descriptions
	protected DdaFeatureManager<I,Desc> manager;

	// location of interest points
	protected FastQueue<Point2D_F64> locDst = new FastQueue<>(10, Point2D_F64.class, false);
	protected FastQueue<Point2D_F64> locSrc = new FastQueue<>(10, Point2D_F64.class, true);
	// description of interest points
	protected FastQueue<Desc> featSrc;
	protected FastQueue<Desc> featDst;

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

	// Data returned by associate
	protected FastQueue<AssociatedIndex> matches;

	// number of features created.  Used to assign unique IDs
	protected long featureID = 0;

	// should it update the feature description after each association?
	boolean updateDescription;

	// indicates if a feature was associated or not
	protected boolean isAssociated[] = new boolean[1];

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

		featSrc = new FastQueue<>(10, manager.getDescriptionType(), false);
		featDst = new FastQueue<>(10, manager.getDescriptionType(), false);
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
		featDst.reset();
		locDst.reset();
		matches = null;
	}

	@Override
	public void process( I input ) {

		tracksActive.clear();
		tracksInactive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		featDst.reset();
		locDst.reset();

		manager.detectFeatures(input, locDst, featDst);

		// skip if there are no features
		if( !tracksAll.isEmpty() ) {

			performTracking();

			// add unassociated to the list
			for( int i = 0; i < tracksAll.size(); i++ ) {
				if( !isAssociated[i] )
					tracksInactive.add(tracksAll.get(i));
			}

			// clean up
			featSrc.reset();
			locSrc.reset();
		}
	}

	protected void performTracking() {
		// create source list
		putIntoSrcList();

		// associate features together
		associate.setSource(locSrc, featSrc);
		associate.setDestination(locDst, featDst);
		associate.associate();

		// used in spawn tracks.  if null then no tracking data is assumed
		matches = associate.getMatches();

		// Update the track state using association information
		updateTrackState(matches);
	}

	/**
	 * Put existing tracks into source list for association
	 */
	protected void putIntoSrcList() {
		// make sure isAssociated is large enough
		if( isAssociated.length < tracksAll.size() ) {
			isAssociated = new boolean[ tracksAll.size() ];
		}

		featSrc.reset();
		locSrc.reset();

		for( int i = 0; i < tracksAll.size(); i++ ) {
			PointTrack t = tracksAll.get(i);
			Desc desc = t.getDescription();
			featSrc.add(desc);
			locSrc.add(t);
			isAssociated[i] = false;
		}
	}

	/**
	 * Update each track's location and description (if configured to do so) mark tracks as being associated.
	 */
	protected void updateTrackState( FastQueue<AssociatedIndex> matches ) {
		// update tracks
		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex indexes = matches.data[i];
			PointTrack track = tracksAll.get(indexes.src);
			Point2D_F64 loc = locDst.data[indexes.dst];
			track.set(loc.x, loc.y);
			tracksActive.add(track);

			// update the description
			if(updateDescription) {
				((Desc)track.getDescription()).setTo(featDst.get(indexes.dst));
			}

			isAssociated[indexes.src] = true;
		}
	}

	/**
	 * Takes the current crop of detected features and makes them the keyframe
	 */
	@Override
	public void spawnTracks() {
		// setup data structures
		if( isAssociated.length < featDst.size ) {
			isAssociated = new boolean[ featDst.size ];
		}

		// see which features are associated in the dst list
		for( int i = 0; i < featDst.size; i++ ) {
			isAssociated[i] = false;
		}

		if( matches != null ) {
			for( int i = 0; i < matches.size; i++ ) {
				isAssociated[matches.data[i].dst] = true;
			}
		}

		// create new tracks from latest unassociated detected features
		for( int i = 0; i < featDst.size; i++ ) {
			if( isAssociated[i] )
				continue;

			Point2D_F64 loc = locDst.get(i);
			addNewTrack(loc.x,loc.y,featDst.get(i));
		}
	}

	/**
	 * Adds a new track given its location and description
	 */
	protected PointTrack addNewTrack( double x , double y , Desc desc ) {
		PointTrack p = getUnused();
		p.set(x, y);
		((Desc)p.getDescription()).setTo(desc);
		if( checkValidSpawn(p) ) {
			p.featureId = featureID++;

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
	protected boolean checkValidSpawn( PointTrack p ) {
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
}
