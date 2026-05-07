/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.tracker.klt;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.concurrency.BoofConcurrency;
import boofcv.core.image.PyramidGradient;
import boofcv.factory.tracker.FactoryTrackerAlg;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.geometry.UtilPoint2D_F32;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.struct.DogArray;
import pabeles.concurrency.GrowArray;

/// High level interface for using [PyramidKltTracker] that lets you tweak its behavior in ways the more abstract
/// [boofcv.abst.tracker.PointTracker] interface will not let you, but without all the drudger of using
/// [PyramidKltTracker]. It performs forward-backwards validation and allows the user to easily and remove
/// tracks. All images in the sequence must be the same size.
///
/// Typical Usage:
///
/// 1) When a new image arrives: [#startFrame]
/// 2) Update location of existing tracks: [#track()]
/// 3) Clear failed tracks [#removeFailed()]
/// 4) Add new features from an external detector [#addTrack(double, double)]
/// 5) Update descriptor of all tracks [#describe()]
/// 6) Prepare for the next image: [#finishFrame()]
/// 7) Look at all the tracks: [#forEachTrack]
///
/// If you want to perform tracking from the same image to multiple others you can do the following:
///
/// 1) Save location of all tracks [#forEachTrack]
/// 2) Pass in image 1: [#startFrame]
/// 3) Update location in image 1: [#track()]
/// 4) Undo changes in location to tracks then revert their failed status: [#markAllSuccess()]
/// 5) Repeat stop 2 for N images.
///
/// Note that you only call [#finishFrame()] when you plan on using that image as the seed for the frame
@SuppressWarnings({"NullAway.Init"})
public class EasyPyramidKlt<Image extends ImageGray<Image>, Derivative extends ImageGray<Derivative>> {

	/// If true it will check feature's center has tracked outside the image and mark it as failed
	@Getter @Setter protected boolean filterOutOfBounds = true;

	/// Pyramidal tracker that this is a wrapper around
	@Getter @Setter protected PyramidKltTracker<Image, Derivative> tracker;

	/// Gradient pyramids
	@Getter protected PyramidGradient<Image, Derivative> currPyr;
	@Getter protected PyramidGradient<Image, Derivative> prevPyr;

	/// size of the template/feature description
	@Getter @Setter protected int templateRadius = 7;

	/// tolerance for forwards-backwards validation in pixels at level 0. disabled if < 0
	@Getter @Setter protected double toleranceFB = 2;

	/// Minimum number of tracks for it to use a concurrent approach
	@Getter @Setter protected int concurrentMinimumTracks = 20;

	/// List of all tracks
	protected DogArray<PyramidKltFeature> tracks;
	/// List of metadata associated with the tracks
	protected DogArray<TrackMeta> metadata = new DogArray<>(TrackMeta::new, TrackMeta::reset);

	/// Internal track list. Do not modify directly; use [#addTrack], [#remove], [#removeSwap] instead.
	/// Kept parallel with [#_getMetadata] to avoid wrapping every track-level call into the lower-level KLT.
	public DogArray<PyramidKltFeature> _getTracks() {return tracks;}

	/// Internal metadata list. Do not modify directly; use [#addTrack], [#remove], [#removeSwap] instead.
	/// Kept parallel with [#_getTracks] to avoid wrapping every track-level call into the lower-level KLT.
	public DogArray<TrackMeta> _getMetadata() {return metadata;}

	/// Workspace for concurrency. Each thread needs to have its own data
	protected GrowArray<PyramidKltTracker<Image, Derivative>> workspace;

	/// The next ID which will be assigned to a track also the total number of tracks created
	@Getter private long nextTrackID = 0;

	/// Unique ID assigned to a frame. This is incremented when [#finishFrame] is called.
	@Getter private long frameID = 0;

	/// Input image shape
	int imageWidth, imageHeight;

	/// Type of input image
	@Getter ImageType<Image> imageType;
	/// Type of derivative image
	@Getter ImageType<Derivative> derivativeType;

	public EasyPyramidKlt( PyramidDiscrete<Image> pyramid,
	                       ImageGradient<Image, Derivative> gradient,
	                       ConfigKlt config ) {
		this.currPyr = new PyramidGradient<>(pyramid, gradient);
		this.prevPyr = new PyramidGradient<>(pyramid, gradient);

		imageType = gradient.getInputType();
		derivativeType = gradient.getDerivativeType();

		Class<Image> imageClass = gradient.getInputType().getImageClass();
		Class<Derivative> derivClass = gradient.getDerivativeType().getImageClass();
		KltTracker<Image, Derivative> klt = FactoryTrackerAlg.klt(config, imageClass, derivClass);

		tracker = new PyramidKltTracker<>(klt);
	}

	/// Clears all tracks
	public void reset() {
		nextTrackID = 0;
		if (tracks != null)
			tracks.reset();
		metadata.reset();
	}

	/// Call first when a new image has arrived. New features will use this feature to create their descriptor from
	/// and existing features will be tracked into this image.
	public void startFrame( Image image ) {
		if (tracks != null) {
			if (imageWidth != image.width || imageHeight != image.height)
				throw new IllegalArgumentException("Once the image size has been set it can't change");
		} else {
			// Lazy initialization now that the iamge size is known.
			currPyr.basePyramid.initialize(image.width, image.height);
			prevPyr.basePyramid.initialize(image.width, image.height);

			tracks = new DogArray<>(this::createNewTrack, ( t ) -> {
				t.x = Float.NaN;
				t.y = Float.NaN;
			});
			workspace = new GrowArray<>(() -> EasyPyramidKlt.this.tracker.copyConcurrent());

			this.imageWidth = image.width;
			this.imageHeight = image.height;
		}
		currPyr.update(image);
	}

	/// Swaps the internal image pyramids so that what's the current one becomes the previous one.
	public void swapFrames() {
		PyramidGradient<Image, Derivative> tmp = currPyr;
		currPyr = prevPyr;
		prevPyr = tmp;
	}

	/// Call when you're no longer processing an image
	public void finishFrame() {
		frameID++;
		swapFrames();
	}

	/// Adds a new track at he specified location. After you are done adding new tracks call [#describe()]
	/// to compute the description of all the new tracks. Location must be inside the image.
	public void addTrack( double x, double y ) {
		if (!BoofMiscOps.isInside(imageWidth, imageHeight, x, y))
			throw new IllegalArgumentException("Can't create a feature outside the image");

		PyramidKltFeature t = tracks.grow();
		t.x = (float)x;
		t.y = (float)y;

		TrackMeta meta = metadata.grow();
		meta.frameSpawned = frameID;
		meta.id = nextTrackID++;
		meta.status = KltTrackFault.SUCCESS;
	}

	/// For each look that passes in each track and matching metadata. The index is valid only during
	/// the callback; subsequent removeSwap calls invalidate it.
	public void forEachTrack( ProcessTrack op ) {
		for (int i = 0; i < tracks.size; i++) {
			PyramidKltFeature track = tracks.get(i);
			TrackMeta meta = metadata.get(i);
			op.process(i, meta, track);
		}
	}

	/// Returns number of tracks
	public int getTrackCount() {
		return tracks.size;
	}

	/// Update the track location of image features. Already-failed tracks are skipped.
	public void track() {
		track(0, tracks.size);
	}

	/// Update the track location of image features within the specified range. Already-failed tracks are skipped.
	///
	/// @param idx0 Track index lower extent, inclusive.
	/// @param idx1 Track index upper extent, exclusive.
	public void track( int idx0, int idx1 ) {
		if (isUseConcurrent(idx1 - idx0)) {
			BoofConcurrency.loopBlocks(idx0, idx1, workspace, ( helper, block0, block1 ) -> {
				helper.setImage(prevPyr.basePyramid, prevPyr.derivX, prevPyr.derivY);
				track(block0, block1, tracker);
			});
		} else {
			tracker.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);
			track(idx0, idx1, tracker);
		}
	}

	protected void track( int idx0, int idx1, PyramidKltTracker<Image, Derivative> tracker ) {
		for (int i = idx0; i < idx1; i++) {
			PyramidKltFeature t = tracks.get(i);
			TrackMeta meta = metadata.get(i);

			// Skip over tracks which have already failed
			if (meta.status != KltTrackFault.SUCCESS)
				continue;

			meta.status = tracker.track(t);

			// Because of the pyramid structure and features are allowed to be partially outside it's possible
			// for the center to go all the way out, which is often a bad thing.
			if (filterOutOfBounds && !BoofMiscOps.isInside(imageWidth, imageHeight, t.x, t.y)) {
				meta.status = KltTrackFault.OUT_OF_BOUNDS;
			}
		}
	}

	/// Performs tracking on just an individual feature
	public KltTrackFault track( int index ) {
		if (index < 0 || index >= tracks.size)
			throw new IndexOutOfBoundsException("index=" + index + " size=" + tracks.size);

		tracker.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);
		PyramidKltFeature t = tracks.get(index);
		TrackMeta meta = metadata.get(index);

		// Skip over tracks which have already failed
		if (meta.status == KltTrackFault.SUCCESS)
			meta.status = tracker.track(t);

		return meta.status;
	}

	/// Validate tracks by tracking from the previous to current frame. If the error is too large mark the
	/// track as failing. Already-failed tracks are skipped.
	public void backwardsValidation() {
		backwardsValidation(0, tracks.size);
	}

	/// Validate specified tracks by tracking from the previous to current frame. If the error is too large mark the
	/// track as failing. Already-failed tracks are skipped.
	///
	/// @param idx0 Track index lower extent, inclusive.
	/// @param idx1 Track index upper extent, exclusive.
	public void backwardsValidation( int idx0, int idx1 ) {
		if (isUseConcurrent(idx1 - idx0)) {
			BoofConcurrency.loopBlocks(idx0, idx1, workspace, ( helper, block0, block1 ) -> {
				helper.setImage(prevPyr.basePyramid, prevPyr.derivX, prevPyr.derivY);
				backwardsValidation(block0, block1, tracker);
			});
		} else {
			tracker.setImage(prevPyr.basePyramid, prevPyr.derivX, prevPyr.derivY);
			backwardsValidation(idx0, idx1, tracker);
		}
	}

	protected void backwardsValidation( int idx0, int idx1, PyramidKltTracker<Image, Derivative> tracker ) {
		float thresholdSq = (float)(toleranceFB*toleranceFB);

		for (int i = idx0; i < idx1; i++) {
			PyramidKltFeature t = tracks.get(i);
			TrackMeta meta = metadata.get(i);

			// Skip over tracks which have already failed
			if (meta.status != KltTrackFault.SUCCESS)
				continue;

			float locX = t.x;
			float locY = t.y;
			if (tracker.track(t) != KltTrackFault.SUCCESS) {
				meta.status = KltTrackFault.BACKWARDS;
			} else if (UtilPoint2D_F32.distanceSq(locX, locY, t.x, t.y) > thresholdSq) {
				meta.status = KltTrackFault.BACKWARDS;
			}

			// Undo the motion from this test
			t.x = locX;
			t.y = locY;
		}
	}

	/// Recompute the description for all tracks which are being actively tracked using the current image.
	/// Already-failed tracks are skipped.
	public void describe() {
		describe(0, tracks.size);
	}

	/// Recompute the description for tracks within the specified range which are being actively tracked using the current image.
	/// Already-failed tracks are skipped.
	///
	/// @param idx0 Track index lower extent, inclusive.
	/// @param idx1 Track index upper extent, exclusive.
	public void describe( int idx0, int idx1 ) {
		if (isUseConcurrent(idx1 - idx0)) {
			BoofConcurrency.loopBlocks(idx0, idx1, workspace, ( helper, block0, block1 ) -> {
				helper.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);
				describe(block0, block1, tracker);
			});
		} else {
			tracker.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);
			describe(idx0, idx1, tracker);
		}
	}

	/// Returns true if it should use a concurrent algorithm
	private boolean isUseConcurrent( int count ) {
		return BoofConcurrency.isUseConcurrent() && count >= concurrentMinimumTracks;
	}

	protected void describe( int idx0, int idx1, PyramidKltTracker<Image, Derivative> tracker ) {
		for (int i = idx0; i < idx1; i++) {
			PyramidKltFeature t = tracks.get(i);
			TrackMeta meta = metadata.get(i);

			// Skip over tracks which have already failed
			if (meta.status != KltTrackFault.SUCCESS)
				continue;

			// Recompute its description
			if (!tracker.setDescription(t)) {
				meta.status = KltTrackFault.DESCRIBE;
			}
		}
	}

	/// Removes all tracks which are not being tracked due to a failure. Location of tracks in the internal
	/// array can be swapped. Runtime O(N)
	///
	/// @return Number of tracks it removed
	public int removeFailed() {
		int sizeBefore = tracks.size;
		for (int i = tracks.size() - 1; i >= 0; i--) {
			TrackMeta meta = metadata.get(i);
			if (meta.status == KltTrackFault.SUCCESS)
				continue;

			removeSwap(i);
		}
		return sizeBefore - tracks.size;
	}

	/// Reactivates all tracks by setting their status to [KltTrackFault#SUCCESS].
	public void markAllSuccess() {
		for (int i = tracks.size() - 1; i >= 0; i--) {
			metadata.get(i).status = KltTrackFault.SUCCESS;
		}
	}

	/// Counts how many tracks pass the test
	///
	/// @param op The test
	public int count( TestTrack op ) {
		int total = 0;
		for (int i = 0; i < tracks.size; i++) {
			PyramidKltFeature t = tracks.get(i);
			TrackMeta meta = metadata.get(i);
			if (op.process(meta, t))
				total++;
		}
		return total;
	}

	/// Removes features using an O(1) operation that does not preserve order
	public void removeSwap( int index ) {
		tracks.removeSwap(index);
		metadata.removeSwap(index);
	}

	/// Removes features using an O(N) operations that preserves order
	public void remove( int index ) {
		tracks.remove(index);
		metadata.remove(index);
	}

	/// Declares a new track and puts it into the unused list
	protected PyramidKltFeature createNewTrack() {
		int numLayers = currPyr.basePyramid.getNumLayers();
		return new PyramidKltFeature(numLayers, templateRadius);
	}

	/// Metadata associated with a track
	public static class TrackMeta {
		/// Frame this track was originally created inside of
		public long frameSpawned = -1;
		/// Unique ID assigned to this track
		public long id = -1;
		/// If the track is still active and if not why
		public KltTrackFault status = KltTrackFault.UNKNOWN;

		public void reset() {
			frameSpawned = -1;
			id = -1;
			status = KltTrackFault.UNKNOWN;
		}
	}

	@FunctionalInterface public interface ProcessTrack {
		void process( int index, TrackMeta meta, PyramidKltFeature feature );
	}

	@FunctionalInterface public interface TestTrack {
		boolean process( TrackMeta meta, PyramidKltFeature feature );
	}
}