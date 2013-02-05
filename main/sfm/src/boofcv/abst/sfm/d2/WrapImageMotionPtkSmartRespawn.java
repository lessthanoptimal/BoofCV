/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.sfm.AccessPointTracks;
import boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn;
import boofcv.struct.GrowQueue_B;
import boofcv.struct.geo.AssociatedPair;
import boofcv.struct.image.ImageBase;
import georegression.struct.InvertibleTransform;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link boofcv.alg.sfm.d2.ImageMotionPtkSmartRespawn} for {@link ImageMotion2D}.
 *
 * @author Peter Abeles
 */
public class WrapImageMotionPtkSmartRespawn<T extends ImageBase, IT extends InvertibleTransform>
		implements ImageMotion2D<T,IT>, AccessPointTracks
{
	ImageMotionPtkSmartRespawn<T,IT> alg;
	boolean first = true;

	List<Point2D_F64> allTracks = new ArrayList<Point2D_F64>();

	boolean inliersMarked = false;
	GrowQueue_B inliers = new GrowQueue_B(10);

	public WrapImageMotionPtkSmartRespawn(ImageMotionPtkSmartRespawn<T, IT> alg) {
		this.alg = alg;
	}

	@Override
	public boolean process(T input) {
		inliersMarked = false;

		boolean ret = alg.process(input);
		if( first ) {
			alg.getMotion().changeKeyFrame(true);
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
		// TODO this will force new features to be detected.  instead just adjust the initial transform
		alg.getMotion().changeKeyFrame(true);
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
	public long getTrackId(int index) {
		return 0;
	}

	private void checkInitialize() {
		if( !inliersMarked ) {
			inliersMarked = true;

			List<PointTrack> active = alg.getMotion().getTracker().getActiveTracks(null);

			allTracks.clear();

			for( int i = 0; i < active.size(); i++ ) {
				allTracks.add(active.get(i));
			}
			inliers.resize(active.size());
			for( int i = 0; i < inliers.size; i++ )
				inliers.data[i] = false;

			ModelMatcher<IT, AssociatedPair> mm = alg.getMotion().getModelMatcher();

			int N = mm.getMatchSet().size();

			for( int i = 0; i < N; i++ ) {
				inliers.data[ mm.getInputIndex(i) ] = true;
			}
		}
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		checkInitialize();

		return allTracks;
	}

	@Override
	public boolean isInlier(int index) {
		checkInitialize();

		return inliers.data[ index ];
	}

	@Override
	public boolean isNew(int index) {
		return false;
	}
}
