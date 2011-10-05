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
import boofcv.alg.geo.SingleImageInput;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.image.ImageBase;
import georegression.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;


/**
 * Tracker for image features which are first detected and then associated using the extracted
 * feature description..
 *
 * TODO improve algorithm
 * - Put descriptions into associated pair
 * - Drop tracks if not associated for a while
 * - Drop tracks if consistent poor score
 * - configure option: change feature description after association
 *
 * @author Peter Abeles
 */
public abstract class DetectAssociateTracker<I extends ImageBase, D >
		implements PointSequentialTracker<I> , SingleImageInput<I> {

	// location of interest points
	private FastQueue<Point2D_I32> locDst = new FastQueue<Point2D_I32>(10,Point2D_I32.class,true);
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

	public abstract void setInputImage( I input );

	public abstract FastQueue<D> createFeatureDescQueue();

	public abstract void detectFeatures( FastQueue<Point2D_I32> location ,
										 FastQueue<D> description );

	public abstract FastQueue<AssociatedIndex> associate( FastQueue<D> featSrc , FastQueue<D> featDst );

	@Override
	public void process( I input ) {
		if( featSrc == null ) {
			featSrc = createFeatureDescQueue();
			featDst = createFeatureDescQueue();
		}

//		System.out.println("process");
		setInputImage(input);

		tracksActive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		featDst.reset();
		locDst.reset();

		detectFeatures(locDst,featDst);

		// if the keyframe has been set associate
		if( keyFrameSet ) {
			matches = associate(featSrc,featDst);

			for( int i = 0; i < matches.size; i++ ) {
				AssociatedIndex indexes = matches.data[i];
				AssociatedPair pair = tracksAll.get(indexes.src);
				Point2D_I32 loc = locDst.data[indexes.dst];
				pair.currLoc.set(loc.x,loc.y);
				tracksActive.add(pair);
			}
		}
	}

	@Override
	public boolean addTrack(float x, float y) {
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
				Point2D_I32 loc = locDst.get(i);
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
					tracksActive.add(p);
					tracksNew.add(p);
				}
			}
		} else {
			// create new tracks from latest detected features
			for( int i = 0; i < featDst.size; i++ ) {
				AssociatedPair p = getUnused();
				Point2D_I32 loc = locDst.get(i);
				p.currLoc.set(loc.x,loc.y);
				p.keyLoc.set(p.currLoc);
				p.featureId = featureID++;

				tracksNew.add(p);
				tracksActive.add(p);
			}
		}

		// add unused to unused
		for(AssociatedPair p : tracksAll ) {
			if( !tracksActive.contains(p) ) {
				tracksDropped.add(p);
				unused.add(p);
			}
		}
		tracksAll.clear();
		tracksAll.addAll(tracksActive);

		keyFrameSet = true;

		// swap to avoid unnecessary memory copy
		FastQueue<D> temp = featDst;
		featDst = featSrc;
		featSrc = temp;
	}

	private AssociatedPair getUnused() {
		AssociatedPair p;
		if( unused.size() > 0 ) {
			p = unused.remove( unused.size()-1 );
		} else {
			p = new AssociatedPair();
		}
		return p;
	}

	@Override
	public void dropTracks() {
		tracksDropped.addAll(tracksAll);
		keyFrameSet = false;
		featSrc.reset();
		featDst.reset();
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
}
