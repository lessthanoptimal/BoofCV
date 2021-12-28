/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.tracker.PruneCloseTracks;
import boofcv.alg.tracker.klt.*;
import boofcv.alg.transform.pyramid.PyramidOps;
import boofcv.struct.ConfigLength;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I16;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static boofcv.abst.tracker.PointTrackerUtils.declareTrackStorage;

/**
 * Wrapper around {@link boofcv.alg.tracker.klt.PyramidKltTracker} for {@link PointTracker}. Every track
 * will have the same size and shaped descriptor. If any fault is encountered the track will be dropped.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class PointTrackerKltPyramid<I extends ImageGray<I>, D extends ImageGray<D>>
		implements PointTracker<I> {
	// If this is a positive number it specifies the maximum number of allowed tracks
	public @Getter @Setter ConfigLength configMaxTracks = ConfigLength.fixed(0);
	// The actual maximum after considering the number of pixels
	int actualMaxTracks;

	// reference to input image
	protected I input;

	// ID of the most recently processed frame
	protected long frameID = -1;

	// Updates the image pyramid's gradient.
	protected ImageGradient<I, D> gradient;

	// tolerance for forwards-backwards validation in pixels at level 0. disabled if < 0
	protected double toleranceFB;

	// storage for image pyramid
	protected ImageStruct currPyr;
	protected ImageStruct prevPyr;
	protected ImageType<D> derivType;

	// configuration for the KLT tracker
	protected ConfigKlt config;
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
	private final QueueCorner excludeList = new QueueCorner(10);

	// number of features tracked so far
	private long totalFeatures = 0;

	// Used to prune points close by
	PruneCloseTracks<PyramidKltFeature> pruneClose;
	List<PyramidKltFeature> closeDropped = new ArrayList<>();

	/**
	 * Constructor which specified the KLT track manager and how the image pyramids are computed.
	 *
	 * @param config KLT tracker configuration
	 * @param toleranceFB Tolerance in pixels for right to left validation. Disable with a value less than 0.
	 * @param templateRadius Radius of square templates that are tracked
	 * @param performPruneClose If true it will prune tracks that are within the detection radius
	 * @param pyramid The image pyramid which KLT is tracking inside of
	 * @param detector Feature detector. If null then no feature detector will be available and spawn won't work.
	 * @param gradient Computes gradient image pyramid.
	 * @param interpInput Interpolation used on input image
	 * @param interpDeriv Interpolation used on gradient images
	 * @param derivType Type of image the gradient is
	 */
	public PointTrackerKltPyramid( ConfigKlt config,
								   double toleranceFB,
								   int templateRadius,
								   boolean performPruneClose,
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
		if (toleranceFB >= 0) {
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

		if (detector != null) {
			if (detector.getRequiresHessian())
				throw new IllegalArgumentException("Hessian based feature detectors not yet supported");

			this.detector = detector;

			if (performPruneClose) {
				pruneClose = new PruneCloseTracks<>(detector.getSearchRadius(), new PruneCloseTracks.TrackInfo<>() {
					@Override
					public void getLocation( PyramidKltFeature track, Point2D_F64 location ) {
						location.x = track.x;
						location.y = track.y;
					}

					@Override
					public long getID( PyramidKltFeature track ) {
						return ((PointTrackMod)track.cookie).featureId;
					}
				});
			}
		}
	}

	/**
	 * Declares a new track and puts it into the unused list
	 */
	private PyramidKltFeature createNewTrack() {
		int numLayers = currPyr.basePyramid.getNumLayers();
		var t = new PyramidKltFeature(numLayers, templateRadius);
		var p = new PointTrackMod();
		p.setDescription(t);
		t.cookie = p;
		return t;
	}

	/**
	 * Creates a new feature track at the specified location. Must only be called after
	 * {@link #process(ImageGray)} has been called. It can fail if there
	 * is insufficient texture
	 *
	 * @param x x-coordinate
	 * @param y y-coordinate
	 * @return the new track if successful or null if no new track could be created
	 */
	public @Nullable PointTrack addTrack( double x, double y ) {
		if (!input.isInBounds((int)x, (int)y))
			return null;

		PyramidKltFeature t = getUnusedTrack();
		t.setPosition((float)x, (float)y);
		tracker.setDescription(t);

		PointTrackMod p = t.getCookie();
		p.pixel.setTo(x, y);
		p.prev.setTo(x, y);

		if (checkValidSpawn(p)) {
			p.featureId = totalFeatures++;
			p.spawnFrameID = frameID;
			p.lastSeenFrameID = frameID;
			active.add(t);
			return p;
		}

		return null;
	}

	/**
	 * Checks to see if there's an unused track that can be recycled. if not it will create a new one
	 */
	protected PyramidKltFeature getUnusedTrack() {
		if (unused.isEmpty())
			return createNewTrack();
		PyramidKltFeature t = unused.remove(unused.size() - 1);
		t.checkUpdateLayers(currPyr.derivX.length);
		return t;
	}

	@Override
	public void spawnTracks() {
		spawned.clear();

		tracker.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);

		// used to convert it from the scale of the bottom layer into the original image
		float scaleBottom = (float)currPyr.basePyramid.getScale(0);

		// exclude active tracks
		excludeList.resize(active.size());
		for (int i = 0; i < active.size(); i++) {
			PyramidKltFeature f = active.get(i);
			excludeList.get(i).setTo((int)(f.x/scaleBottom), (int)(f.y/scaleBottom));
		}

		// Don't want to detect features again which are already being tracked
		detector.setExclude(excludeList);
		// Don't exceed the maximum tracking limit
		I baseLayer = currPyr.basePyramid.getLayer(0);
		actualMaxTracks = configMaxTracks.computeI(baseLayer.totalPixels());
		if (actualMaxTracks > 0) {
			int limit = actualMaxTracks - excludeList.size;
			if (limit <= 0)
				return;
			detector.setFeatureLimit(actualMaxTracks - excludeList.size);
		} else
			detector.setFeatureLimit(-1);
		detector.process(baseLayer, currPyr.derivX[0], currPyr.derivY[0], null, null, null);

		// Create new tracks from the detected features
		addToTracks(scaleBottom, detector.getMinimums());
		addToTracks(scaleBottom, detector.getMaximums());
	}

	@Override public ImageType<I> getImageType() {
		return gradient.getInputType();
	}

	private void addToTracks( float scaleBottom, QueueCorner found ) {
		for (int i = 0; i < found.size(); i++) {
			Point2D_I16 pt = found.get(i);

			// set up pyramid description
			PyramidKltFeature t = getUnusedTrack();
			t.x = pt.x*scaleBottom;
			t.y = pt.y*scaleBottom;

			tracker.setDescription(t);

			// set up point description
			PointTrackMod p = t.getCookie();
			p.pixel.setTo(t.x, t.y);

			if (checkValidSpawn(p)) {
				p.featureId = totalFeatures++;
				p.spawnFrameID = frameID;
				p.lastSeenFrameID = frameID;
				p.prev.setTo(t.x, t.y);

				// add to appropriate lists
				active.add(t);
				spawned.add(t);
			} else {
				unused.add(t);
			}
		}
	}

	/**
	 * Returns true if a new track can be spawned here. Intended to be overloaded
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
	public int getMaxSpawn() {
		return actualMaxTracks;
	}

	@Override
	public void process( I image ) {
		this.input = image;
		this.frameID++;

		// swap currPyr to prevPyr so that the previous is now the previous
		if (toleranceFB >= 0) {
			ImageStruct tmp = currPyr;
			currPyr = prevPyr;
			prevPyr = tmp;
		}

		boolean activeTracks = active.size() > 0;
		spawned.clear();
		dropped.clear();

		// update image pyramids
		currPyr.update(image);

		// track features
		tracker.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);
		for (int i = active.size() - 1; i >= 0; i--) {
			PyramidKltFeature t = active.get(i);
			KltTrackFault ret = tracker.track(t);

			boolean success = false;

			if (ret == KltTrackFault.SUCCESS) {
				// discard a track if its center drifts outside the image.
				if (image.isInBounds((int)t.x, (int)t.y) && tracker.setDescription(t)) {
					PointTrack p = t.getCookie();
					p.pixel.setTo(t.x, t.y);
					p.lastSeenFrameID = frameID;
					success = true;
				}
			}

			if (!success) {
				active.remove(i);
				dropped.add(t);
				unused.add(t);
			}
		}

		if (toleranceFB >= 0) {
			// If there are no tracks it must have been reset or this is the first frame
			if (activeTracks) {
				backwardsTrackValidate();
			} else {
				this.prevPyr.update(image);
			}
		}

		// If configured to, drop features which are close by each other
		if (pruneClose != null) {
			pruneCloseTracks();
		}
	}

	/**
	 * Prune tracks which are too close and adds them to the dropped list
	 */
	protected void pruneCloseTracks() {
		pruneClose.init(input.width, input.height);
		pruneClose.process(active, closeDropped);
		active.removeAll(closeDropped);
		dropped.addAll(closeDropped);
	}

	/**
	 * Track back to the previous frame and see if the original coordinate is found again. This assumes that all
	 * tracks in active list existed in the previous frame and were not spawned.
	 */
	protected void backwardsTrackValidate() {
		double tol2 = toleranceFB*toleranceFB;

		tracker.setImage(prevPyr.basePyramid, prevPyr.derivX, prevPyr.derivY);
		for (int i = active.size() - 1; i >= 0; i--) {
			PyramidKltFeature t = active.get(i);
			PointTrackMod p = t.getCookie();

			KltTrackFault ret = tracker.track(t);

			if (ret != KltTrackFault.SUCCESS || p.prev.distance2(t.x, t.y) > tol2) {
				active.remove(i);
				dropped.add(t);
				unused.add(t);
			} else {
				// the new previous will be the current location
				p.prev.setTo(p.pixel);
				// Revert the update by KLT
				t.x = (float)p.pixel.x;
				t.y = (float)p.pixel.y;
			}
		}
	}

	@Override
	public boolean dropTrack( PointTrack track ) {
		if (active.remove((PyramidKltFeature)track.getDescription())) {
			// only recycle the description if it is in the active list. This avoids the problem of adding the
			// same description multiple times
			unused.add(track.getDescription());
			return true;
		}
		return false;
	}

	@Override
	public void dropTracks( Dropper dropper ) {
		for (int i = active.size() - 1; i >= 0; i--) {
			PointTrack t = (PointTrack)active.get(i).cookie;
			if (dropper.shouldDropTrack(t)) {
				PyramidKltFeature klt = active.remove(i);
				unused.add(klt);
			}
		}
	}

	@Override
	public List<PointTrack> getActiveTracks( @Nullable List<PointTrack> list ) {
		list = declareTrackStorage(list);

		addToList(active, list);

		return list;
	}

	/**
	 * KLT does not have inactive tracks since all tracks are dropped if a problem occurs.
	 */
	@Override
	public List<PointTrack> getInactiveTracks( @Nullable List<PointTrack> list ) {
		return declareTrackStorage(list);
	}

	@Override
	public List<PointTrack> getDroppedTracks( @Nullable List<PointTrack> list ) {
		list = declareTrackStorage(list);

		addToList(dropped, list);

		return list;
	}

	@Override
	public List<PointTrack> getNewTracks( @Nullable List<PointTrack> list ) {
		list = declareTrackStorage(list);

		addToList(spawned, list);

		return list;
	}

	@Override
	public List<PointTrack> getAllTracks( @Nullable List<PointTrack> list ) {
		return getActiveTracks(list);
	}

	protected void addToList( List<PyramidKltFeature> in, List<PointTrack> out ) {
		for (int featIdx = 0; featIdx < in.size(); featIdx++) {
			out.add((PointTrack)in.get(featIdx).cookie);
		}
	}

	@Override
	public void reset() {
		dropAllTracks();
		totalFeatures = 0;
		frameID = -1;
	}

	@Override
	public long getFrameID() {
		return frameID;
	}

	@Override
	public int getTotalActive() {
		return active.size();
	}

	@Override
	public int getTotalInactive() {
		// there are no inactive tracks with KLT. If a match isn't found it is immediately dropped
		return 0;
	}

	static class PointTrackMod extends PointTrack {
		// previous location of the track
		public final Point2D_F64 prev = new Point2D_F64();
	}

	/**
	 * Contains the image pyramid
	 */
	@SuppressWarnings({"NullAway.Init"})
	class ImageStruct {
		public PyramidDiscrete<I> basePyramid;
		public D[] derivX;
		public D[] derivY;

		public ImageStruct( PyramidDiscrete<I> o ) {
			basePyramid = o.copyStructure();
		}

		public void update( I image ) {
			basePyramid.process(image);
			if (derivX == null || derivX.length != basePyramid.layers.length) {
				derivX = PyramidOps.declareOutput(basePyramid, derivType);
				derivY = PyramidOps.declareOutput(basePyramid, derivType);
			}

			if (derivX[0].width != basePyramid.getLayer(0).width ||
					derivX[0].height != basePyramid.getLayer(0).height) {
				PyramidOps.reshapeOutput(basePyramid, derivX);
				PyramidOps.reshapeOutput(basePyramid, derivY);
			}
			PyramidOps.gradient(basePyramid, gradient, derivX, derivY);
		}
	}
}
