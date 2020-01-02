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

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.detect.interest.GeneralFeatureDetector;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.*;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link boofcv.alg.tracker.klt.PyramidKltTracker} for {@link PointTracker}.  Every track
 * will have the same size and shaped descriptor.  If any fault is encountered the track will be dropped.
 *
 * @author Peter Abeles
 */
public class PointTrackerKltPyramid<I extends ImageGray<I>,D extends ImageGray<D>>
		implements PointTracker<I>
{
	// reference to input image
	protected I input;

	// Updates the image pyramid's gradient.
	protected ImageGradient<I,D> gradient;

	// tolerance for forwards-backwards validation in pixels at level 0. disabled if < 0
	protected double toleranceFB;

	// storage for image pyramid
	protected ImageStruct currPyr;
	protected ImageStruct prevPyr;
	protected ImageType<D> derivType;

	// configuration for the KLT tracker
	protected KltConfig config;
	// size of the template/feature description
	protected int templateRadius;

	// list of features which are actively being tracked
	protected List<PyramidKltFeature> active = new ArrayList<>();
	// list of features which were just spawned
	protected List<PyramidKltFeature> spawned = new ArrayList<>();
	// list of features which were just dropped
	protected List<PyramidKltFeature> dropped = new ArrayList<>();
	// feature data available for future tracking
	protected List<PyramidKltFeature> unused = new ArrayList<>();

	// the tracker
	protected PyramidKltTracker<I, D> tracker;

	// selects point features
	private GeneralFeatureDetector<I, D> detector;
	// list of corners which should be ignored by the corner detector
	private QueueCorner excludeList = new QueueCorner(10);

	// number of features tracked so far
	private long totalFeatures = 0;

	/**
	 * Constructor which specified the KLT track manager and how the image pyramids are computed.
	 *
	 * @param config KLT tracker configuration
	 * @param toleranceFB Tolerance in pixels for right to left validation. Disable with a value less than 0.
	 * @param templateRadius Radius of square templates that are tracked
	 * @param pyramid The image pyramid which KLT is tracking inside of
	 * @param detector Feature detector.   If null then no feature detector will be available and spawn won't work.
	 * @param gradient Computes gradient image pyramid.
	 * @param interpInput Interpolation used on input image
	 * @param interpDeriv Interpolation used on gradient images
	 * @param derivType Type of image the gradient is
	 */
	public PointTrackerKltPyramid(KltConfig config,
								  double toleranceFB,
								  int templateRadius ,
								  PyramidDiscrete<I> pyramid,
								  GeneralFeatureDetector<I, D> detector,
								  ImageGradient<I, D> gradient,
								  InterpolateRectangle<I> interpInput,
								  InterpolateRectangle<D> interpDeriv,
								  Class<D> derivType ) {

		this.config = config;
		this.toleranceFB = toleranceFB;
		this.templateRadius = templateRadius;
		this.gradient = gradient;
		this.derivType = ImageType.single(derivType);
		this.currPyr = new ImageStruct(pyramid);
		if( toleranceFB >= 0 ) {
			this.prevPyr = new ImageStruct(pyramid);
			// don't save the reference because the input image might be the same instance each time and change
			// between frames
			this.prevPyr.basePyramid.setSaveOriginalReference(false);
			this.currPyr.basePyramid.setSaveOriginalReference(false);
		} else {
			this.currPyr.basePyramid.setSaveOriginalReference(true);
		}

		var klt = new KltTracker<>(interpInput, interpDeriv, config);
		tracker = new PyramidKltTracker<>(klt);

		if( detector != null) {
			if (detector.getRequiresHessian())
				throw new IllegalArgumentException("Hessian based feature detectors not yet supported");

			this.detector = detector;
		}
	}

	private void addTrackToUnused() {
		int numLayers = currPyr.basePyramid.getNumLayers();
		var t = new PyramidKltFeature(numLayers, templateRadius);

		var p = new PointTrackMod();
		p.setDescription(t);
		t.cookie = p;

		unused.add(t);
	}

	/**
	 * Creates a new feature track at the specified location. Must only be called after
	 * {@link #process(ImageGray)} has been called.  It can fail if there
	 * is insufficient texture
	 *
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return the new track if successful or null if no new track could be created
	 */
	public PointTrack addTrack( double x , double y ) {
		if( !input.isInBounds((int)x,(int)y))
			return null;

		// grow the number of tracks if needed
		if( unused.isEmpty() )
			addTrackToUnused();

		PyramidKltFeature t = unused.remove(unused.size() - 1);
		t.setPosition((float)x,(float)y);
		tracker.setDescription(t);

		PointTrackMod p = t.getCookie();
		p.set(x,y);
		p.prev.set(x,y);

		if( checkValidSpawn(p) ) {
			active.add(t);
			return p;
		}

		return null;
	}

	@Override
	public void spawnTracks() {
		spawned.clear();

		// used to convert it from the scale of the bottom layer into the original image
		float scaleBottom = (float)currPyr.basePyramid.getScale(0);

		// exclude active tracks
		excludeList.reset();
		for (int i = 0; i < active.size(); i++) {
			PyramidKltFeature f = active.get(i);
			excludeList.add((int) (f.x / scaleBottom), (int) (f.y / scaleBottom));
		}

		// find new tracks, but no more than the max
		detector.setExcludeMaximum(excludeList);
		detector.process(currPyr.basePyramid.getLayer(0), currPyr.derivX[0], currPyr.derivY[0], null, null, null);

		// extract the features
		QueueCorner found = detector.getMaximums();

		// grow the number of tracks if needed
		while( unused.size() < found.size() )
			addTrackToUnused();

		for (int i = 0; i < found.size() && !unused.isEmpty(); i++) {
			Point2D_I16 pt = found.get(i);

			// set up pyramid description
			PyramidKltFeature t = unused.remove(unused.size() - 1);
			t.x = pt.x * scaleBottom;
			t.y = pt.y * scaleBottom;

			tracker.setDescription(t);

			// set up point description
			PointTrackMod p = t.getCookie();
			p.set(t.x,t.y);
			p.prev.set(t.x,t.y);

			if( checkValidSpawn(p) ) {
				p.featureId = totalFeatures++;

				// add to appropriate lists
				active.add(t);
				spawned.add(t);
			} else {
				unused.add(t);
			}
		}
	}

	/**
	 * Returns true if a new track can be spawned here.  Intended to be overloaded
	 */
	protected boolean checkValidSpawn( PointTrack p ) {
		return true;
	}

	@Override
	public void dropAllTracks() {
		unused.addAll(active);
		active.clear();
		dropped.clear();
	}

	@Override
	public void process(I image) {
		this.input = image;

		boolean activeTracks = active.size()>0;
		spawned.clear();
		dropped.clear();

		// update image pyramids
		currPyr.update(image);

		// track features
		tracker.setImage(currPyr.basePyramid,currPyr.derivX,currPyr.derivY);
		for( int i = 0; i < active.size(); ) {
			PyramidKltFeature t = active.get(i);
			KltTrackFault ret = tracker.track(t);

			boolean success = false;

			if( ret == KltTrackFault.SUCCESS ) {
				// discard a track if its center drifts outside the image.
				if( image.isInBounds((int)t.x,(int)t.y) && tracker.setDescription(t) ) {
					PointTrack p = t.getCookie();
					p.set(t.x,t.y);
					i++;
					success = true;
				}
			}

			if( !success ) {
				active.remove( i );
				dropped.add( t );
				unused.add( t );
			}
		}

		if( toleranceFB >= 0 ) {
			// If there are no tracks it must have been reset or this is the first frame
			if( activeTracks ) {
				backwardsTrackValidate();
			} else {
				this.prevPyr.update(image);
			}

			// make the current image the previous image
			ImageStruct tmp = currPyr;
			currPyr = prevPyr;
			prevPyr = tmp;
		}
	}

	/**
	 * Track back to the previous frame and see if the original coordinate is found again. This assumes that all
	 * tracks in active list existed in the previous frame and were not spawned.
	 */
	protected void backwardsTrackValidate() {
		double tol2 = toleranceFB * toleranceFB;

		tracker.setImage(prevPyr.basePyramid,prevPyr.derivX,prevPyr.derivY);
		for (int i = active.size()-1; i >= 0; i--) {
			PyramidKltFeature t = active.get(i);
			PointTrackMod p = t.getCookie();

			float origX = t.x;
			float origY = t.y;

			KltTrackFault ret = tracker.track(t);

			if( ret != KltTrackFault.SUCCESS || p.prev.distance2(t.x,t.y) > tol2 ) {
				active.remove(i);
				dropped.add( t );
				unused.add( t );
			} else {
				// the new previous will be the current location
				p.prev.set(origX,origY);
				// Revert the update by KLT
				t.x = origX;
				t.y = origY;
			}
		}
	}

	@Override
	public boolean dropTrack(PointTrack track) {
		if( active.remove((PyramidKltFeature)track.getDescription()) ) {
			// only recycle the description if it is in the active list.  This avoids the problem of adding the
			// same description multiple times
			unused.add((PyramidKltFeature)track.getDescription());
			return true;
		}
		return false;
	}

	@Override
	public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();

		addToList(active,list);

		return list;
	}

	/**
	 * KLT does not have inactive tracks since all tracks are dropped if a problem occurs.
	 */
	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list == null )
			list = new ArrayList<>();

		return list;
	}

	@Override
	public List<PointTrack> getDroppedTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();

		addToList(dropped,list);

		return list;
	}

	@Override
	public List<PointTrack> getNewTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<>();

		addToList(spawned,list);

		return list;
	}

	@Override
	public List<PointTrack> getAllTracks( List<PointTrack> list ) {
		return getActiveTracks(list);
	}

	protected void addToList( List<PyramidKltFeature> in , List<PointTrack> out ) {
		for( PyramidKltFeature t : in ) {
			out.add( (PointTrack)t.cookie );
		}
	}

	@Override
	public void reset() {
		dropAllTracks();
		totalFeatures = 0;
	}

	static class PointTrackMod extends PointTrack {
		// previous location of the track
		public final Point2D_F64 prev = new PointTrack();
	}

	/**
	 * Contains the image pyramid
	 */
	class ImageStruct {
		public PyramidDiscrete<I> basePyramid;
		public D[] derivX;
		public D[] derivY;

		public ImageStruct(PyramidDiscrete<I> o ) {
			basePyramid = o.copyStructure();
			derivX = PyramidOps.declareOutput(basePyramid, derivType);
			derivY = PyramidOps.declareOutput(basePyramid, derivType);
		}

		public void update( I image ) {
			basePyramid.process(image);

			if( derivX[0].width != basePyramid.getLayer(0).width ||
					derivX[0].height != basePyramid.getLayer(0).height )
			{
				PyramidOps.reshapeOutput(basePyramid,derivX);
				PyramidOps.reshapeOutput(basePyramid,derivY);
			}
			PyramidOps.gradient(basePyramid, gradient, derivX,derivY);
		}
	}

}
