/*
 * Copyright (c) 2022, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.tracker.klt.ConfigKlt;
import boofcv.alg.tracker.klt.KltTrackFault;
import boofcv.alg.tracker.klt.PyramidKltFeature;
import boofcv.alg.tracker.klt.PyramidKltTracker;
import boofcv.concurrency.BoofConcurrency;
import boofcv.struct.QueueCorner;
import boofcv.struct.image.ImageGray;
import boofcv.struct.pyramid.PyramidDiscrete;
import georegression.struct.point.Point2D_I16;
import org.ddogleg.struct.DogArray_I32;
import pabeles.concurrency.GrowArray;

import java.util.ArrayList;
import java.util.List;

/**
 * Concurrent extension of {@link PointTrackerKltPyramid}
 *
 * @author Peter Abeles
 */
public class PointTrackerKltPyramid_MT<I extends ImageGray<I>, D extends ImageGray<D>>
		extends PointTrackerKltPyramid<I, D> {

	/** If there are fewer than this number of tracks it will use a single thread */
	public int minimumTracksConcurrent = 20;

	// Each thread will have its own tracker
	private final GrowArray<Helper> workspace;

	// Workspace for merging results
	private final DogArray_I32 allFailed = new DogArray_I32();

	public PointTrackerKltPyramid_MT( ConfigKlt config, double toleranceFB, int templateRadius,
									  boolean performPruneClose, PyramidDiscrete<I> pyramid,
									  GeneralFeatureDetector<I, D> detector, ImageGradient<I, D> gradient,
									  InterpolateRectangle<I> interpInput,
									  InterpolateRectangle<D> interpDeriv, Class<D> derivType ) {
		super(config, toleranceFB, templateRadius, performPruneClose, pyramid, detector, gradient, interpInput,
				interpDeriv, derivType);
		workspace = new GrowArray<>(Helper::new, Helper::reset);
	}

	@Override protected void addToTracks( float scaleBottom, QueueCorner found ) {
		// threads will be slower if there aren't enough tracks
		if (found.size() < minimumTracksConcurrent) {
			super.addToTracks(scaleBottom, found);
			return;
		}

		// pre-declare memory for each track
		List<PyramidKltFeature> workTracks = new ArrayList<>(found.size);
		for (int i = 0; i < found.size; i++) {
			workTracks.add(getUnusedTrack());
		}

		// do image processing on each new track
		BoofConcurrency.loopBlocks(0, workTracks.size(), workspace, ( helper, idx0, idx1 ) -> {
			helper.tracker.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);
			for (int detIdx = idx0; detIdx < idx1; detIdx++) {
				Point2D_I16 pt = found.get(detIdx);
				PyramidKltFeature t = workTracks.get(detIdx);

				t.x = pt.x*scaleBottom;
				t.y = pt.y*scaleBottom;

				helper.tracker.setDescription(t);

				// set up point description
				PointTrackMod p = t.getCookie();
				p.pixel.setTo(t.x, t.y);

				if (checkValidSpawn(p)) {
					p.featureId = 1; // mark it as success
					p.spawnFrameID = frameID;
					p.lastSeenFrameID = frameID;
					p.prev.setTo(t.x, t.y);
				} else {
					p.featureId = -1; // mark it as failure
				}
			}
		});

		// Add the tracks to the appropriate lists
		for (int i = 0; i < workTracks.size(); i++) {
			PyramidKltFeature t = workTracks.get(i);
			PointTrackMod p = t.getCookie();

			if (p.featureId == 1) {
				p.featureId = totalFeatures++;
				active.add(t);
				spawned.add(t);
			} else {
				unused.add(t);
			}
		}
	}

	@Override protected void trackFeatures( I image ) {
		// threads will be slower if there aren't enough tracks
		if (active.size() < minimumTracksConcurrent) {
			super.trackFeatures(image);
			return;
		}

		// Perform tracking in parallel
		BoofConcurrency.loopBlocks(0, active.size(), workspace, ( helper, idx0, idx1 ) -> {
			helper.tracker.setImage(currPyr.basePyramid, currPyr.derivX, currPyr.derivY);

			for (int trackIdx = idx0; trackIdx < idx1; trackIdx++) {
				PyramidKltFeature t = active.get(trackIdx);
				KltTrackFault ret = helper.tracker.track(t);

				boolean success = false;

				if (ret == KltTrackFault.SUCCESS) {
					// discard a track if its center drifts outside the image.
					if (image.isInBounds((int)t.x, (int)t.y) && helper.tracker.setDescription(t)) {
						PointTrack p = t.getCookie();
						p.pixel.setTo(t.x, t.y);
						p.lastSeenFrameID = frameID;
						success = true;
					}
				}

				if (!success) {
					helper.failed.add(trackIdx);
				}
			}
		});

		dropFailed();
	}

	private void dropFailed() {
		// Order the tracks which are to be dropped and remove them
		allFailed.reset();
		for (int helpIdx = 0; helpIdx < workspace.size(); helpIdx++) {
			allFailed.addAll(workspace.get(helpIdx).failed);
		}
		allFailed.sort();

		// drop the tracks in reverse order to avoid changing indexes
		for (int allidx = allFailed.size - 1; allidx >= 0; allidx--) {
			int trackIdx = allFailed.get(allidx);
			PyramidKltFeature t = active.get(trackIdx);
			active.remove(trackIdx);
			dropped.add(t);
			unused.add(t);
		}
	}

	@Override protected void backwardsTrackValidate() {
		// threads will be slower if there aren't a enough tracks
		if (active.size() < minimumTracksConcurrent) {
			super.backwardsTrackValidate();
			return;
		}
		double tol2 = toleranceFB*toleranceFB;

		// Perform tracking in parallel
		BoofConcurrency.loopBlocks(0, active.size(), workspace, ( helper, idx0, idx1 ) -> {
			helper.tracker.setImage(prevPyr.basePyramid, prevPyr.derivX, prevPyr.derivY);

			for (int trackIdx = idx0; trackIdx < idx1; trackIdx++) {
				PyramidKltFeature t = active.get(trackIdx);
				PointTrackMod p = t.getCookie();

				KltTrackFault ret = helper.tracker.track(t);

				if (ret != KltTrackFault.SUCCESS || p.prev.distance2(t.x, t.y) > tol2) {
					helper.failed.add(trackIdx);
				} else {
					// the new previous will be the current location
					p.prev.setTo(p.pixel);
					// Revert the update by KLT
					t.x = (float)p.pixel.x;
					t.y = (float)p.pixel.y;
				}
			}
		});

		dropFailed();
	}

	/** All the information a single black needs */
	private class Helper {
		PyramidKltTracker<I, D> tracker = PointTrackerKltPyramid_MT.this.tracker.copyConcurrent();
		DogArray_I32 failed = new DogArray_I32();

		public void reset() {
			failed.reset();
		}
	}
}
