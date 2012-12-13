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

import boofcv.abst.feature.detect.interest.GeneralFeatureDetector;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.KltTracker;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.pyramid.ImagePyramid;
import boofcv.struct.pyramid.PyramidDiscrete;
import boofcv.struct.pyramid.PyramidUpdaterDiscrete;
import georegression.struct.point.Point2D_I16;

import java.util.ArrayList;
import java.util.List;


/**
 * Wrapper around {@link boofcv.alg.tracker.klt.PyramidKltTracker} for {@link ImagePointTracker}.  Every track
 * will have the same size and shaped descriptor.  If any fault is encountered the track will be dropped.
 *
 * @author Peter Abeles
 */
public class PointTrackerKltPyramid<I extends ImageSingleBand,D extends ImageSingleBand>
		implements ImagePointTracker<I>
{
	// update the image pyramid
	private PyramidUpdaterDiscrete<I> inputPyramidUpdater;
	// Updates the image pyramid's gradient.
	private ImageGradient<I,D> gradient;

	// storage for image pyramid
	private PyramidDiscrete<I> basePyramid;
	private ImagePyramid<D> derivX;
	private ImagePyramid<D> derivY;

	// configuration for the track manager
	protected PkltConfig<I, D> config;

	// list of features which are actively being tracked
	protected List<PyramidKltFeature> active = new ArrayList<PyramidKltFeature>();
	// list of features which were just spawned
	protected List<PyramidKltFeature> spawned = new ArrayList<PyramidKltFeature>();
	// list of features which were just dropped
	protected List<PyramidKltFeature> dropped = new ArrayList<PyramidKltFeature>();
	// feature data available for future tracking
	protected List<PyramidKltFeature> unused = new ArrayList<PyramidKltFeature>();

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
	 * @param inputPyramidUpdater Computes the main image pyramid.
	 * @param gradient Computes gradient image pyramid.
	 */
	public PointTrackerKltPyramid(PkltConfig<I, D> config,
								  PyramidUpdaterDiscrete<I> inputPyramidUpdater,
								  GeneralFeatureDetector<I, D> detector,
								  ImageGradient<I, D> gradient,
								  InterpolateRectangle<I> interpInput,
								  InterpolateRectangle<D> interpDeriv) {
		if( detector.getRequiresHessian() )
			throw new IllegalArgumentException("Hessian based feature detectors not yet supported");

		this.config = config;
		this.gradient = gradient;
		this.detector = detector;
		this.inputPyramidUpdater = inputPyramidUpdater;

		KltTracker<I, D> klt = new KltTracker<I, D>(interpInput, interpDeriv, config.config);
		tracker = new PyramidKltTracker<I, D>(klt);

		// pre-declare image features
		int numLayers = config.pyramidScaling.length;
		for (int i = 0; i < config.maxFeatures; i++) {
			addTrackToUnused();
		}

		// declare the image pyramid
		basePyramid = new PyramidDiscrete<I>(config.typeInput,true,config.pyramidScaling);
		derivX = new PyramidDiscrete<D>(config.typeDeriv,false,config.pyramidScaling);
		derivY = new PyramidDiscrete<D>(config.typeDeriv,false,config.pyramidScaling);
	}

	private void addTrackToUnused() {
		int numLayers = config.pyramidScaling.length;
		PyramidKltFeature t = new PyramidKltFeature(numLayers, config.featureRadius);

		PointTrack p = new PointTrack();
		p.setDescription(t);
		t.cookie = p;

		unused.add(t);
	}

	@Override
	public void spawnTracks() {
		spawned.clear();

		// used to convert it from the scale if the bottom layer into the original image
		float scaleBottom = (float) basePyramid.getScale(0);

		// exclude active tracks
		excludeList.reset();
		for (int i = 0; i < active.size(); i++) {
			PyramidKltFeature f = active.get(i);
			excludeList.add((int) (f.x / scaleBottom), (int) (f.y / scaleBottom));
		}

		// find new tracks, but no more than the max
		detector.setExcludedCorners(excludeList);
		detector.setMaxFeatures(excludeList.size() + unused.size());
		detector.process(basePyramid.getLayer(0), derivX.getLayer(0), derivY.getLayer(0), null, null, null);

		// extract the features
		QueueCorner found = detector.getFeatures();

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
			PointTrack p = t.getCookie();
			p.featureId = totalFeatures++;
			p.set(t.x,t.y);

			// add to appropriate lists
			active.add(t);
			spawned.add(t);
		}
	}

	@Override
	public void dropAllTracks() {
		unused.addAll(active);
		active.clear();
		dropped.clear();
	}

	@Override
	public void process(I image) {
		spawned.clear();
		dropped.clear();
		
		// update image pyramids
		inputPyramidUpdater.update(image,basePyramid);
		PyramidOps.gradient(basePyramid, gradient, derivX,derivY);

		// track features
		tracker.setImage(basePyramid,derivX,derivY);
		for( int i = 0; i < active.size(); ) {
			PyramidKltFeature t = active.get(i);
			KltTrackFault ret = tracker.track(t);

			if( ret == KltTrackFault.SUCCESS ) {
				tracker.setDescription(t);
				PointTrack p = t.getCookie();
				p.set(t.x,t.y);
				i++;
			} else {
				active.remove(i);
				dropped.add( t );
				unused.add( t );
			}
		}
	}

	@Override
	public void dropTrack(PointTrack track) {
		unused.add((PyramidKltFeature)track.getDescription());
		if( !active.remove((PyramidKltFeature)track.getDescription()) ) {
			throw new RuntimeException("Not in active list!");
		}
	}

	@Override
	public List<PointTrack> getActiveTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		addToList(active,list);

		return list;
	}

	/**
	 * KLT does not have inactive tracks since all tracks are dropped if a problem occurs.
	 */
	@Override
	public List<PointTrack> getInactiveTracks(List<PointTrack> list) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		return list;
	}

	@Override
	public List<PointTrack> getDroppedTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		addToList(dropped,list);

		return list;
	}

	@Override
	public List<PointTrack> getNewTracks( List<PointTrack> list ) {
		if( list == null )
			list = new ArrayList<PointTrack>();

		addToList(spawned,list);

		return list;
	}

	@Override
	public List<PointTrack> getAllTracks( List<PointTrack> list ) {
		return getActiveTracks(list);
	}

	private void addToList( List<PyramidKltFeature> in , List<PointTrack> out ) {
		for( PyramidKltFeature t : in ) {
			out.add( (PointTrack)t.cookie );
		}
	}

	@Override
	public void reset() {
		dropAllTracks();
		totalFeatures = 0;
	}
}
