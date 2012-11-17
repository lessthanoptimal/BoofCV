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

import boofcv.abst.feature.associate.GeneralAssociation;
import boofcv.abst.feature.describe.DescribeRegionPoint;
import boofcv.abst.feature.detect.interest.InterestPointDetector;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.feature.TupleDescQueue;
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

	// detects features inside the image
	InterestPointDetector<I> detector;
	// describes features using local information
	DescribeRegionPoint<I,D> describe;
	// associates features between two images together
	GeneralAssociation<D> associate;

	// location of interest points
	private FastQueue<Point2D_F64> locDst = new FastQueue<Point2D_F64>(10,Point2D_F64.class,true);
	// description of interest points
	private FastQueue<D> featSrc;
	private FastQueue<D> featDst;

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
	private FastQueue<AssociatedIndex> matches;

	// number of features created.  Used to assign unique IDs
	long featureID = 0;

	// should it update the feature description after each association?
	boolean updateDescription;

	// indicates if a feature was associated or not
	boolean srcAssociated[] = new boolean[1];

	/**
	 * Configures tracker
	 *
	 * @param detector Feature detector
	 * @param describe Feature descriptor
	 * @param associate Association
	 * @param updateDescription If true then the feature description will be updated after each image.
	 *                          Typically this should be false.
	 */
	public DetectAssociateTracker(InterestPointDetector<I> detector,
								  DescribeRegionPoint<I, D> describe,
								  GeneralAssociation<D> associate ,
								  boolean updateDescription ) {
		this.detector = detector;
		this.describe = describe;
		this.associate = associate;
		this.updateDescription = updateDescription;

		featSrc = new TupleDescQueue<D>(describe,false);
		featDst = new TupleDescQueue<D>(describe,true);
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

		detector.detect(input);
		describe.setImage(input);

		tracksActive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		featDst.reset();
		locDst.reset();

		if( srcAssociated.length < tracksAll.size() ) {
			srcAssociated = new boolean[ tracksAll.size() ];
		}

		int N = detector.getNumberOfFeatures();
		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = locDst.grow();
			p.set(detector.getLocation(i));

			double yaw = detector.getOrientation(i);
			double scale = detector.getScale(i);

			if( describe.isInBounds(p.x,p.y,yaw,scale)) {
				D desc = featDst.grow();
				describe.process(p.x,p.y,yaw,scale,desc);
			} else {
				locDst.removeTail();
			}
		}

		// skip if there are no features to match with the current image
		if( !tracksAll.isEmpty() ) {
			// create a list of previously detected features
			if( featSrc.size != 0 )
				throw new RuntimeException("BUG");

			for( int i = 0; i < tracksAll.size(); i++ ) {
				D desc = tracksAll.get(i).getDescription();
				featSrc.add(desc);
				srcAssociated[i] = false;
			}

			// pair of old and newly detected features
			associate.setSource(featSrc);
			associate.setDestination(featDst);
			associate.associate();

			// update tracks
			matches = associate.getMatches();
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

				srcAssociated[indexes.src] = true;
//				System.out.println("i = "+i+"  x = "+loc.x+" y = "+loc.y);
			}

			// add unassociated to the list
			for( int i = 0; i < tracksAll.size(); i++ ) {
				if( !srcAssociated[i] )
					tracksInactive.add(tracksAll.get(i));
			}

			// clean up
			featSrc.reset();
		}
	}

	/**
	 * Takes the current crop of detected features and makes them the keyframe
	 */
	@Override
	public void spawnTracks() {
		// create new tracks from latest detected features
		for( int i = 0; i < featDst.size; i++ ) {

			if( matches != null ) {
				// only spawn tracks at points which have not been associated
				boolean found = false;

				// *** NOTE *** could speed up using by creating a lookup table first
				for( int j = 0; j < matches.size; j++ ) {
					AssociatedIndex indexes = matches.data[j];
					if( indexes.dst == i ) {
						found = true;
						break;
					}
				}

				if( found )
					continue;
			}

			PointTrack p = getUnused();
			Point2D_F64 loc = locDst.get(i);
			p.set(loc.x,loc.y);
			((D)p.getDescription()).setTo(featDst.get(i));
			p.featureId = featureID++;

			tracksNew.add(p);
			tracksActive.add(p);
			tracksAll.add(p);
		}
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
			p.setDescription(describe.createDescription());
		}
		return p;
	}

	@Override
	public void dropAllTracks() {
		unused.addAll(tracksAll);
		tracksActive.clear();
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
