/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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
import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;

import java.util.ArrayList;
import java.util.List;


/**
 * <p>
 * Tracker for image features which are first detected and then associated using the extracted
 * feature description.  For this tracker to work well the feature descriptor must be very strong
 * and result in the correct association without any model of the model being fit.
 * </p>
 *
 * @author Peter Abeles
 */
public class DetectAssociateTracker<I extends ImageSingleBand, D extends TupleDesc>
		implements ImagePointTracker<I> {

	// Feature detector and describer
	protected DetectDescribePoint<I,D> detDesc;
	// associates features between two images together
	protected AssociateDescription2D<D> associate;

	// location of interest points
	protected FastQueue<Point2D_F64> locDst = new FastQueue<Point2D_F64>(10,Point2D_F64.class,false);
	protected FastQueue<Point2D_F64> locSrc = new FastQueue<Point2D_F64>(10,Point2D_F64.class,true);
	// description of interest points
	protected FastQueue<D> featSrc;
	protected FastQueue<D> featDst;

	// all tracks
	protected List<PointTrack> tracksAll = new ArrayList<PointTrack>();
	// recently associated tracks
	protected List<PointTrack> tracksActive = new ArrayList<PointTrack>();
	// tracks not matched to any recent features
	protected List<PointTrack> tracksInactive = new ArrayList<PointTrack>();
	// tracks dropped by the tracker
	protected List<PointTrack> tracksDropped = new ArrayList<PointTrack>();
	// tracks recently spawned
	protected List<PointTrack> tracksNew = new ArrayList<PointTrack>();

	// previously declared tracks which are being recycled
	protected List<PointTrack> unused = new ArrayList<PointTrack>();

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
	 * @param detDesc Feature detector and descriptor
	 * @param associate Association
	 * @param updateDescription If true then the feature description will be updated after each image.
	 *                          Typically this should be false.
	 */
	public DetectAssociateTracker( final DetectDescribePoint<I,D> detDesc ,
								   final AssociateDescription2D<D> associate ,
								   final boolean updateDescription ) {
		this.detDesc = detDesc;
		this.associate = associate;
		this.updateDescription = updateDescription;

		featSrc = new FastQueue<D>(10,detDesc.getDescriptorType(),false);
		featDst = new FastQueue<D>(10,detDesc.getDescriptorType(),false);
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

		detDesc.detect(input);

		tracksActive.clear();
		tracksInactive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		featDst.reset();
		locDst.reset();


		int N = detDesc.getNumberOfFeatures();
		for( int i = 0; i < N; i++ ) {
			locDst.add( detDesc.getLocation(i) );
			featDst.add( detDesc.getDescriptor(i) );
		}

		// skip if there are no features to match with the current image
		if( !tracksAll.isEmpty() ) {
			// create a list of previously detected features
			if( featSrc.size != 0 )
				throw new RuntimeException("BUG");

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
		// Match features
		associateFeatures();
		// used in spawn tracks.  if null then no tracking data is assumed
		matches = associate.getMatches();

		// Update the track state using association information
		updateTrackState(matches);
	}

	/**
	 * Associate features and update the track information
	 */
	protected void associateFeatures() {
		if( isAssociated.length < tracksAll.size() ) {
			isAssociated = new boolean[ tracksAll.size() ];
		}

		// put each track's location and description into the source list
		for( int i = 0; i < tracksAll.size(); i++ ) {
			PointTrack t = tracksAll.get(i);
			D desc = t.getDescription();
			featSrc.add(desc);
			locSrc.add(t);
			isAssociated[i] = false;
		}

		associate.setSource(locSrc, featSrc);
		associate.setDestination(locDst, featDst);
		associate.associate();
	}

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
				((D)track.getDescription()).setTo(featDst.get(indexes.dst));
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

			PointTrack p = getUnused();
			Point2D_F64 loc = locDst.get(i);
			p.set(loc.x, loc.y);
			((D)p.getDescription()).setTo(featDst.get(i));
			if( checkValidSpawn(p) ) {
				p.featureId = featureID++;

				tracksNew.add(p);
				tracksActive.add(p);
				tracksAll.add(p);
			} else {
				unused.add(p);
			}
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
			p.setDescription(detDesc.createDescription());
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
	public void dropTrack(PointTrack track) {
		// the track may or may not be in the active list
		tracksActive.remove(track);
		tracksInactive.remove(track);
		// it must be in the all list
		if( !tracksAll.remove(track) )
			throw new IllegalArgumentException("Track not found in all list");
		// recycle the data
		unused.add(track);
	}

	@Override
	public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		list.addAll(tracksActive);
		return list;
	}

	@Override
	public List<PointTrack> getDroppedTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		list.addAll(tracksDropped);
		return list;
	}

	@Override
	public List<PointTrack> getNewTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		list.addAll(tracksNew);
		return list;
	}

	@Override
	public List<PointTrack> getAllTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		list.addAll(tracksAll);
		return list;
	}

	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		list.addAll(tracksInactive);
		return list;
	}

}
