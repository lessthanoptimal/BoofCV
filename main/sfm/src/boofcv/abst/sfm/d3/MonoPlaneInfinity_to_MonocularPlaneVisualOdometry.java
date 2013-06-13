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

package boofcv.abst.sfm.d3;

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.alg.sfm.d3.VisOdomMonoPlaneInfinity;
import boofcv.alg.sfm.robust.DistancePlane2DToPixelSq;
import boofcv.alg.sfm.robust.GenerateSe2_PlanePtPixel;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageDataType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.List;

/**
 * Wrapper around {@link VisOdomMonoPlaneInfinity} for {@link MonocularPlaneVisualOdometry}.
 *
 * @author Peter Abeles
 */
public class MonoPlaneInfinity_to_MonocularPlaneVisualOdometry<T extends ImageBase>
		implements MonocularPlaneVisualOdometry<T> , AccessPointTracks3D
{
	VisOdomMonoPlaneInfinity<T> alg;
	DistancePlane2DToPixelSq distance;
	GenerateSe2_PlanePtPixel generator;

	ImageDataType<T> imageType;

	boolean fault;
	Se3_F64 cameraToWorld = new Se3_F64();

	// list of active tracks
	List<PointTrack> active = null;

	public MonoPlaneInfinity_to_MonocularPlaneVisualOdometry(VisOdomMonoPlaneInfinity<T> alg,
															 DistancePlane2DToPixelSq distance,
															 GenerateSe2_PlanePtPixel generator,
															 ImageDataType<T> imageType) {
		this.alg = alg;
		this.distance = distance;
		this.generator = generator;
		this.imageType = imageType;
	}

	@Override
	public void setExtrinsic(Se3_F64 planeToCamera) {
		alg.setExtrinsic(planeToCamera);
		generator.setExtrinsic(planeToCamera);
		distance.setExtrinsic(planeToCamera);
	}

	@Override
	public void setIntrinsic(IntrinsicParameters param) {
		alg.setIntrinsic(param);
		distance.setIntrinsic(param.fx,param.fy,param.skew);
	}

	@Override
	public boolean process(T input) {

		active = null;
		fault = alg.process(input);

		return fault;
	}

	@Override
	public ImageDataType<T> getImageType() {
		return imageType;
	}

	@Override
	public void reset() {
		active = null;
		alg.reset();
		cameraToWorld.reset();
	}

	@Override
	public boolean isFault() {
		return fault;
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		Se3_F64 worldToCamera = alg.getWorldToCurr3D();
		worldToCamera.invert(cameraToWorld);

		return cameraToWorld;
	}

	@Override
	public Point3D_F64 getTrackLocation(int index) {
		return null;// todo implement
	}

	@Override
	public long getTrackId(int index) {
		if( active == null )
			active = alg.getTracker().getActiveTracks(null);

		PointTrack t = active.get(index);
		return t.featureId;
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		if( active == null )
			active = alg.getTracker().getActiveTracks(null);

		return (List)active;
	}

	@Override
	public boolean isInlier(int index) {
		if( active == null )
			active = alg.getTracker().getActiveTracks(null);

		PointTrack t = active.get(index);
		VisOdomMonoPlaneInfinity.VoTrack v = t.getCookie();

		return v.lastInlier == alg.getTick();
	}

	@Override
	public boolean isNew(int index) {
		// need to figure out a way to efficiently implement this
		return false;
	}
}
