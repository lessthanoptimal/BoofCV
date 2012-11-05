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
import java.util.Iterator;
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


	InterestPointDetector<I> detector;
	DescribeRegionPoint<I,D> describe;
	GeneralAssociation<D> associate;

	// location of interest points
	private FastQueue<Point2D_F64> locDst = new FastQueue<Point2D_F64>(10,Point2D_F64.class,true);
	// description of interest points
	private FastQueue<D> featSrc;
	private FastQueue<D> featDst;

	private boolean keyFrameSet = false;

	// all tracks
	private List<PointTrack> tracksAll = new ArrayList<PointTrack>();
	// recently associated tracks
	private List<PointTrack> tracksActive = new ArrayList<PointTrack>();
	// tracks dropped by the tracker
	private List<PointTrack> tracksDropped = new ArrayList<PointTrack>();
	// tracks recently spawned
	private List<PointTrack> tracksNew = new ArrayList<PointTrack>();

	// previously declared tracks which are being recycled
	protected List<PointTrack> unused = new ArrayList<PointTrack>();

	private FastQueue<AssociatedIndex> matches;

	long featureID = 0;

	// if a track goes unassociated for this long it is pruned
	int pruneThreshold = 2;

	// should it update the feature description after each association?
	boolean updateState = true;

	// how many frames have been processed
	long tick;

	public DetectAssociateTracker(InterestPointDetector<I> detector,
								  DescribeRegionPoint<I, D> describe,
								  GeneralAssociation<D> associate ) {
		this.detector = detector;
		this.describe = describe;
		this.associate = associate;

		featSrc = new TupleDescQueue<D>(describe,false);
		featDst = new TupleDescQueue<D>(describe,true);
	}

	public boolean isUpdateState() {
		return updateState;
	}

	/**
	 * If a feature is associated should the description be updated with the latest observation?
	 */
	public void setUpdateState(boolean updateState) {
		this.updateState = updateState;
	}

	public int getPruneThreshold() {
		return pruneThreshold;
	}

	public void setPruneThreshold(int pruneThreshold) {
		this.pruneThreshold = pruneThreshold;
	}

	@Override
	public void reset() {
		dropAllTracks();
		featureID = 0;
		tick = 0;
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

		int N = detector.getNumberOfFeatures();
		for( int i = 0; i < N; i++ ) {
			Point2D_F64 p = locDst.grow();
			p.set(detector.getLocation(i));

			double yaw = detector.getOrientation(i);
			double scale = detector.getScale(i);

			D desc = featDst.grow();
			describe.process(p.x,p.y,yaw,scale,desc);
		}

		// if the keyframe has been set associate
		if( keyFrameSet ) {
			// create a list of previously detected features
			if( featSrc.size != 0 )
				throw new RuntimeException("BUG");

			for( int i = 0; i < tracksAll.size(); i++ ) {
				TrackInfo info = tracksAll.get(i).getDescription();
				featSrc.add(info.desc);
			}

			// pair of old and newly detected features
			associate.associate(featSrc,featDst);

			// update tracks
			matches = associate.getMatches();
			for( int i = 0; i < matches.size; i++ ) {
				AssociatedIndex indexes = matches.data[i];
				PointTrack track = tracksAll.get(indexes.src);
				Point2D_F64 loc = locDst.data[indexes.dst];
				track.set(loc.x, loc.y);
				tracksActive.add(track);
				TrackInfo info = track.getDescription();
				info.lastAssociated = tick;

				// update the description
				if( updateState ) {
					info.desc.setTo(featDst.get(indexes.dst));
				}
//				System.out.println("i = "+i+"  x = "+loc.x+" y = "+loc.y);
			}

//			System.out.println("----------------- matched "+matches.size()+"  tracked "+tracksAll.size());

			// clean up
			featSrc.reset();
		}

		pruneTracks();
		tick++;
	}

	private void pruneTracks() {
		Iterator<PointTrack> iter = tracksAll.iterator();
		while( iter.hasNext() ) {
			PointTrack p = iter.next();
			TrackInfo info = p.getDescription();
			if( tick - info.lastAssociated > pruneThreshold ) {
				tracksDropped.add(p);
				unused.add(p);
				iter.remove();
			}
		}
	}

	@Override
	public boolean addTrack(double x, double y) {
		throw new IllegalArgumentException("Not supported.  Need to know orientation and/or scale.");
	}

	/**
	 * Takes the current crop of detected features and makes them the keyframe
	 */
	@Override
	public void spawnTracks() {
		// create new tracks from latest detected features
		for( int i = 0; i < featDst.size; i++ ) {
			// if a keyframe has already been set then that means associate has been called
			if( keyFrameSet ) {
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
			((TrackInfo)p.getDescription()).desc.setTo(featDst.get(i));
			p.featureId = featureID++;

			tracksNew.add(p);
			tracksActive.add(p);
			tracksAll.add(p);
		}

		keyFrameSet = true;
	}

	private PointTrack getUnused() {
		PointTrack p;
		if( unused.size() > 0 ) {
			p = unused.remove( unused.size()-1 );
			((TrackInfo)p.getDescription()).reset();
		} else {
			p = new PointTrack();
			TrackInfo info = new TrackInfo();
			info.desc = describe.createDescription();
			p.setDescription(info);
		}
		return p;
	}

	@Override
	public void dropAllTracks() {
		unused.addAll(tracksAll);
		tracksDropped.clear();
		tracksDropped.addAll(tracksActive);
		tracksActive.clear();
		tracksAll.clear();
		tracksNew.clear();
		keyFrameSet = false;
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
	public List<PointTrack> getActiveTracks() {
		return tracksActive;
	}

	@Override
	public List<PointTrack> getDroppedTracks() {
		return tracksDropped;
	}

	@Override
	public List<PointTrack> getNewTracks() {
		return tracksNew;
	}

	@Override
	public List<PointTrack> getAllTracks() {
		return tracksAll;
	}

	private class TrackInfo
	{
		// which tick was it last associated at.  Used for dropping tracks
		long lastAssociated;
		// description of the feature
		D desc;

		public void reset() {
			lastAssociated = tick;
		}
	}
}
