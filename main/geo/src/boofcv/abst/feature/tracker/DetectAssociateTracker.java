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

import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
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
public abstract class DetectAssociateTracker<I extends ImageSingleBand, D >
		implements ImagePointTracker<I> {

	// location of interest points
	private FastQueue<Point2D_F64> locDst = new FastQueue<Point2D_F64>(10,Point2D_F64.class,true);
	// description of interest points
	private FastQueue<D> featSrc;
	private FastQueue<D> featDst;

	private boolean keyFrameSet = false;

	private List<PointTrack> tracksAll = new ArrayList<PointTrack>();
	private List<PointTrack> tracksActive = new ArrayList<PointTrack>();
	private List<PointTrack> tracksDropped = new ArrayList<PointTrack>();
	private List<PointTrack> tracksNew = new ArrayList<PointTrack>();

	private List<PointTrack> unused = new ArrayList<PointTrack>();

	private FastQueue<AssociatedIndex> matches;

	long featureID = 0;

	// if a track goes unassociated for this long it is pruned
	int pruneThreshold = 2;

	// should it update the feature description after each association?
	boolean updateState = true;

	// how many frames have been processed
	long tick;

	public abstract void setInputImage( I input );

	public abstract FastQueue<D> createFeatureDescQueue( boolean declareData );

	public abstract D createDescription();

	public abstract void detectFeatures( FastQueue<Point2D_F64> location ,
										 FastQueue<D> description );

	public abstract FastQueue<AssociatedIndex> associate( FastQueue<D> featSrc , FastQueue<D> featDst );

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
	public void process( I input ) {
		if( featSrc == null ) {
			featSrc = createFeatureDescQueue(false);
			featDst = createFeatureDescQueue(true);
		}

		setInputImage(input);
		tick++;

		tracksActive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		featDst.reset();
		locDst.reset();

		detectFeatures(locDst,featDst);

		pruneTracks();

		// if the keyframe has been set associate
		if( keyFrameSet ) {
			matches = associate(featSrc,featDst);

			for( int i = 0; i < matches.size; i++ ) {
				AssociatedIndex indexes = matches.data[i];
				PointTrack track = tracksAll.get(indexes.src);
				Point2D_F64 loc = locDst.data[indexes.dst];
				track.set(loc.x, loc.y);
				tracksActive.add(track);
				TrackInfo info = (TrackInfo)track.getDescription();
				info.lastAssociated = tick;

				// update the description
				if( updateState ) {
					setDescription(info.desc, featDst.get(indexes.dst));
				}
//				System.out.println("i = "+i+"  x = "+loc.x+" y = "+loc.y);
			}
//			System.out.println("----------------- matched "+matches.size()+"  tracked "+locDst.size());
		}
	}

	private void pruneTracks() {
		featSrc.reset();
		Iterator<PointTrack> iter = tracksAll.iterator();
		while( iter.hasNext() ) {
			PointTrack p = iter.next();
			TrackInfo info = p.getDescription();
			if( tick - info.lastAssociated > pruneThreshold ) {
				tracksDropped.add(p);
				unused.add(p);
				iter.remove();
			} else {
				featSrc.add(info.desc);
			}
		}
	}

	/**
	 * Sets the 'src' description equal to 'dst'
	 */
	protected abstract void setDescription(D src, D dst);

	@Override
	public boolean addTrack(double x, double y) {
		throw new IllegalArgumentException("Not supported.  SURF features need to know the scale.");
	}

	/**
	 * Takes the current crop of detected features and makes them the keyframe
	 */
	@Override
	public void spawnTracks() {
		tracksNew.clear();
		tracksDropped.clear();
		tracksActive.clear();
		unused.addAll(tracksAll);
		tracksAll.clear();

		// create new tracks from latest detected features
		for( int i = 0; i < featDst.size; i++ ) {
			PointTrack p = getUnused();
			Point2D_F64 loc = locDst.get(i);
			p.set(loc.x,loc.y);
			setDescription(((TrackInfo)p.getDescription()).desc, featDst.get(i));
			p.featureId = featureID++;

			tracksNew.add(p);
			tracksActive.add(p);
			tracksAll.add(p);
		}

		featSrc.reset();
		for( PointTrack p : tracksAll ) {
			featSrc.add(p.<TrackInfo>getDescription().desc);
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
			info.desc = createDescription();
			p.setDescription(info);
		}
		return p;
	}

	@Override
	public void dropTracks() {
		unused.addAll(tracksAll);
		tracksDropped.clear();
		tracksDropped.addAll(tracksActive);
		tracksActive.clear();
		tracksAll.clear();
		tracksNew.clear();
		matches = null;

		keyFrameSet = false;
//		if( featSrc != null ) {
//			featSrc.reset();
//			featDst.reset();
//		}
	}

	/**
	 * Remove from active list and mark so that it is dropped in the next cycle
	 *
	 * @param track The track which is to be dropped
	 */
	@Override
	public void dropTrack(PointTrack track) {
		// first mark the track as being old so that it will get dropped on the next cycle
		TrackInfo info = track.getDescription();
		info.lastAssociated = -pruneThreshold;

		// remove it from the active list
		tracksActive.remove(track);
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

	public List<PointTrack> getTracksAll() {
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
