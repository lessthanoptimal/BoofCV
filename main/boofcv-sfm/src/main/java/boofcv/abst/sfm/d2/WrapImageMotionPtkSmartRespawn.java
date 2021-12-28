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

package boofcv.abst.sfm.d2;

import boofcv.abst.sfm.AccessPointTracks;
import boofcv.abst.tracker.PointTrack;
import boofcv.alg.sfm.d2.AssociatedPairTrack;
import boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn;
import boofcv.struct.image.ImageBase;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray_B;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn} for {@link ImageMotion2D}.
 *
 * @author Peter Abeles
 */
public class WrapImageMotionPtkSmartRespawn<T extends ImageBase<T>, IT extends InvertibleTransform<IT>>
		implements ImageMotion2D<T, IT>, AccessPointTracks {
	ImageMotionPtkSmartRespawn<T, IT> alg;
	boolean first = true;

	List<Point2D_F64> allTracks = new ArrayList<>();

	boolean inliersMarked = false;
	DogArray_B inliers = new DogArray_B(10);

	public WrapImageMotionPtkSmartRespawn( ImageMotionPtkSmartRespawn<T, IT> alg ) {
		this.alg = alg;
	}

	@Override
	public boolean process( T input ) {
		inliersMarked = false;

		boolean ret = alg.process(input);
		if (first) {
			alg.getMotion().changeKeyFrame();
			alg.getMotion().resetTransforms();
			first = false;
			return true;
		}
		return ret;
	}

	@Override
	public void reset() {
		first = true;
		alg.getMotion().reset();
	}

	@Override
	public void setToFirst() {
		alg.getMotion().changeKeyFrame();
		alg.getMotion().resetTransforms();
	}

	@Override
	public long getFrameID() {
		return alg.getMotion().getFrameID();
	}

	@Override
	public IT getFirstToCurrent() {
		return alg.getMotion().getWorldToCurr();
	}

	@Override
	public Class<IT> getTransformType() {
		return alg.getMotion().getModelType();
	}

	@Override
	public int getTotalTracks() {
		checkInitialize();
		return allTracks.size();
	}

	@Override
	public long getTrackId( int index ) {
		return 0;
	}

	@Override
	public void getTrackPixel( int index, Point2D_F64 pixel ) {
		pixel.setTo(allTracks.get(index));
	}

	private void checkInitialize() {
		if (!inliersMarked) {
			inliersMarked = true;

			List<PointTrack> active = alg.getMotion().getTracker().getActiveTracks(null);

			allTracks.clear();

			long tick = getFrameID();
			inliers.resize(active.size());

			for (int i = 0; i < active.size(); i++) {
				PointTrack t = active.get(i);
				AssociatedPairTrack info = t.getCookie();
				allTracks.add(t.pixel);
				// if it was used in the previous update then it is in the inlier set
				inliers.data[i] = info.lastUsed == tick;
			}
		}
	}

	@Override
	public List<Point2D_F64> getAllTracks( @Nullable List<Point2D_F64> storage ) {
		if (storage == null)
			storage = new ArrayList<>();
		else
			storage.clear();

		checkInitialize();
		storage.addAll(allTracks);

		return storage;
	}

	@Override
	public boolean isTrackInlier( int index ) {
		checkInitialize();

		return inliers.data[index];
	}

	@Override
	public boolean isTrackNew( int index ) {
		return false;
	}
}
