/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

import boofcv.alg.geo.AssociatedPair;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageBase;
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
public abstract class DetectAssociateTracker<I extends ImageBase, D >
		implements ImagePointTracker<I> {

	// location of interest points
	private FastQueue<Point2D_F64> locDst = new FastQueue<Point2D_F64>(10,Point2D_F64.class,true);
	// description of interest points
	private FastQueue<D> featSrc;
	private FastQueue<D> featDst;

	private boolean keyFrameSet = false;

	private List<AssociatedPair> tracksAll = new ArrayList<AssociatedPair>();
	private List<AssociatedPair> tracksActive = new ArrayList<AssociatedPair>();
	private List<AssociatedPair> tracksDropped = new ArrayList<AssociatedPair>();
	private List<AssociatedPair> tracksNew = new ArrayList<AssociatedPair>();

	private List<AssociatedPair> unused = new ArrayList<AssociatedPair>();

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

//		System.out.println("process");
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
				AssociatedPair pair = tracksAll.get(indexes.src);
				Point2D_F64 loc = locDst.data[indexes.dst];
				pair.currLoc.set(loc.x,loc.y);
				tracksActive.add(pair);
				TrackInfo info = (TrackInfo)pair.getDescription();
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
		Iterator<AssociatedPair> iter = tracksAll.iterator();
		while( iter.hasNext() ) {
			AssociatedPair p = iter.next();
			TrackInfo info = (TrackInfo)p.description;
			if( tick - info.lastAssociated > pruneThreshold ) {
//				System.out.println("Dropping track");
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
//		System.out.println("Spawning Tracks");
		tracksNew.clear();
		tracksActive.clear();
		tracksDropped.clear();

		if( keyFrameSet ) {
			for( int i = 0; i < featDst.size; i++ ) {
				Point2D_F64 loc = locDst.get(i);
				// see if the track had been associated with an older one
				boolean matched = false;
				for( int j = 0; j < matches.size; j++ ) {
					AssociatedIndex indexes = matches.data[j];

					if( indexes.dst == i ) {
						matched = true;
						AssociatedPair p = tracksAll.get(indexes.src);
						tracksActive.add(p);
						break;
					}
				}

				if( !matched ) {
					AssociatedPair p = getUnused();
					p.currLoc.set(loc.x,loc.y);
					p.keyLoc.set(p.currLoc);
					p.featureId = featureID++;
					setDescription(((TrackInfo)p.getDescription()).desc, featDst.get(i));
					tracksActive.add(p);
					tracksNew.add(p);
				}
			}
		} else {
			// create new tracks from latest detected features
			for( int i = 0; i < featDst.size; i++ ) {
				AssociatedPair p = getUnused();
				Point2D_F64 loc = locDst.get(i);
				p.currLoc.set(loc.x,loc.y);
				p.keyLoc.set(p.currLoc);
				setDescription(((TrackInfo)p.getDescription()).desc, featDst.get(i));
				p.featureId = featureID++;

				tracksNew.add(p);
				tracksActive.add(p);
			}
		}

		// add unused to the lists
		for(AssociatedPair p : tracksAll ) {
			if( !tracksActive.contains(p) ) {
				tracksDropped.add(p);
				unused.add(p);
			}
		}
		tracksAll.clear();
		tracksAll.addAll(tracksActive);

		featSrc.reset();
		for( AssociatedPair p : tracksAll ) {
			featSrc.add(p.<TrackInfo>getDescription().desc);
		}

		keyFrameSet = true;
	}

	private AssociatedPair getUnused() {
		AssociatedPair p;
		if( unused.size() > 0 ) {
			p = unused.remove( unused.size()-1 );
			((TrackInfo)p.getDescription()).reset();
		} else {
			p = new AssociatedPair();
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

		keyFrameSet = false;
		if( featSrc != null ) {
			featSrc.reset();
			featDst.reset();
		}
	}

	@Override
	public void setCurrentToKeyFrame() {
		for( AssociatedPair a : tracksAll ) {
			a.keyLoc.set(a.currLoc);
		}
	}

	@Override
	public void dropTrack(AssociatedPair track) {
		throw new IllegalArgumentException("Not supported yet");
	}

	@Override
	public List<AssociatedPair> getActiveTracks() {
		return tracksActive;
	}

	@Override
	public List<AssociatedPair> getDroppedTracks() {
		return tracksDropped;
	}

	@Override
	public List<AssociatedPair> getNewTracks() {
		return tracksNew;
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
