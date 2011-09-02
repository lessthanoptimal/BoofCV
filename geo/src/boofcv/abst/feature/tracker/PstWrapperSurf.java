/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package boofcv.abst.feature.tracker;

import boofcv.alg.feature.associate.AssociateSurfBasic;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.detect.interest.FastHessianFeatureDetector;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.geo.AssociatedPair;
import boofcv.alg.geo.SingleImageInput;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import boofcv.struct.feature.ScalePoint;
import boofcv.struct.feature.SurfFeature;
import boofcv.struct.feature.SurfFeatureQueue;
import boofcv.struct.image.ImageBase;
import jgrl.struct.point.Point2D_I32;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around SURF features for {@link PointSequentialTracker}.
 *
 * @author Peter Abeles
 */
// todo drop tracks if they haven't been seen in a while
public class PstWrapperSurf<I extends ImageBase,II extends ImageBase>
		implements PointSequentialTracker<I> , SingleImageInput<I> {

	II integralImage;

	private FastHessianFeatureDetector<II> detector;
	private OrientationIntegral<II> orientation;
	private DescribePointSurf<II> describe;
	private AssociateSurfBasic assoc;

	// location of interest points
	private FastQueue<Point2D_I32> locDst = new FastQueue<Point2D_I32>(10,Point2D_I32.class,true);
	// description of interest points
	private FastQueue<SurfFeature> featSrc = new SurfFeatureQueue(64);
	private FastQueue<SurfFeature> featDst = new SurfFeatureQueue(64);

	private boolean keyFrameSet = false;

	private List<AssociatedPair> tracksAll = new ArrayList<AssociatedPair>();
	private List<AssociatedPair> tracksActive = new ArrayList<AssociatedPair>();
	private List<AssociatedPair> tracksDropped = new ArrayList<AssociatedPair>();
	private List<AssociatedPair> tracksNew = new ArrayList<AssociatedPair>();

	private List<AssociatedPair> unused = new ArrayList<AssociatedPair>();

	long featureID = 0;

	public PstWrapperSurf(FastHessianFeatureDetector<II> detector,
						  OrientationIntegral<II> orientation,
						  DescribePointSurf<II> describe,
						  AssociateSurfBasic assoc ,
						  Class<II> integralType ) {
		this.detector = detector;
		this.orientation = orientation;
		this.describe = describe;
		this.assoc = assoc;
		this.integralImage = GeneralizedImageOps.createImage(integralType,1,1);
	}

	@Override
	public void process( I input ) {

		integralImage.reshape(input.width,input.height);
		GIntegralImageOps.transform(input,integralImage);

		tracksActive.clear();
		tracksDropped.clear();
		tracksNew.clear();

		featDst.reset();
		locDst.reset();

		// detect interest points
		detector.detect(integralImage);
		List<ScalePoint> points = detector.getFoundPoints();

		// extract feature descriptions
		orientation.setImage(integralImage);
		describe.setImage(integralImage);
		SurfFeature tmpFeat = featDst.pop();
		for( ScalePoint p : points ) {
			orientation.setScale(p.scale);
			double angle = orientation.compute(p.x,p.y);

			SurfFeature feat = describe.describe(p.x,p.y,p.scale,angle,tmpFeat);
			if( feat != null ) {
				locDst.pop().set(p.x,p.y);
				tmpFeat = featDst.pop();
			}
		}
		featDst.removeTail();

		// add to association
		assoc.setDst(featDst);

		// if the keyframe has been set associate
		if( keyFrameSet ) {
			assoc.associate();
			FastQueue<AssociatedIndex> matches = assoc.getMatches();

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
		unused.addAll(tracksAll);
		tracksAll.clear();
		tracksNew.clear();
		tracksActive.clear();
		tracksDropped.clear();

		// create new tracks from latest detected features
		for( int i = 0; i < featDst.size; i++ ) {
			AssociatedPair p;
			if( unused.size() > 0 ) {
				p = unused.remove( unused.size()-1 );
			} else {
				p = new AssociatedPair();
			}
			Point2D_I32 loc = locDst.get(i);
			p.currLoc.set(loc.x,loc.y);
			p.keyLoc.set(p.currLoc);
			p.featureId = featureID++;

			tracksAll.add(p);
			tracksNew.add(p);
			tracksActive.add(p);
		}
		keyFrameSet = true;
		// make the current dst the keyframe
		assoc.swapLists();
		// since features have been swapped in association (which has references to these features)
		// the lists also need to be swapped here
		FastQueue<SurfFeature> tmp = featSrc;
		featSrc = featDst;
		featDst = tmp;
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
		// ignore this command
		System.out.print("ignored setCurrentToKeyFrame");
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
