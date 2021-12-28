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

package boofcv.abst.sfm.d3;

import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.abst.tracker.PointTrack;
import boofcv.alg.sfm.d3.VisOdomMonoPlaneInfinity;
import boofcv.alg.sfm.robust.DistancePlane2DToPixelSq;
import boofcv.alg.sfm.robust.GenerateSe2_PlanePtPixel;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Wrapper around {@link VisOdomMonoPlaneInfinity} for {@link MonocularPlaneVisualOdometry}.
 *
 * @author Peter Abeles
 */
public class MonoPlaneInfinity_to_MonocularPlaneVisualOdometry<T extends ImageBase<T>>
		implements MonocularPlaneVisualOdometry<T>, AccessPointTracks3D {
	VisOdomMonoPlaneInfinity<T> alg;
	DistancePlane2DToPixelSq distance;
	GenerateSe2_PlanePtPixel generator;

	ImageType<T> imageType;

	boolean fault;
	Se3_F64 cameraToWorld = new Se3_F64();

	// list of active tracks
	@Nullable List<PointTrack> active = null;

	public MonoPlaneInfinity_to_MonocularPlaneVisualOdometry( VisOdomMonoPlaneInfinity<T> alg,
															  DistancePlane2DToPixelSq distance,
															  GenerateSe2_PlanePtPixel generator,
															  ImageType<T> imageType ) {
		this.alg = alg;
		this.distance = distance;
		this.generator = generator;
		this.imageType = imageType;
	}

	@Override
	public void setCalibration( MonoPlaneParameters param ) {
		alg.setIntrinsic(param.intrinsic);
		distance.setIntrinsic(param.intrinsic.fx, param.intrinsic.fy, param.intrinsic.skew);

		alg.setExtrinsic(param.planeToCamera);
		generator.setExtrinsic(param.planeToCamera);
		distance.setExtrinsic(param.planeToCamera);
	}

	@Override
	public boolean process( T input ) {
		active = null;
		fault = alg.process(input);

		return fault;
	}

	@Override
	public ImageType<T> getImageType() {
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
	public long getFrameID() {
		return alg.getFrameID();
	}

	@Override
	public boolean getTrackWorld3D( int index, Point3D_F64 world ) {
		if (active == null)
			active = alg.getTracker().getActiveTracks(null);
		VisOdomMonoPlaneInfinity.VoTrack track = active.get(index).getCookie();

		if (track.onPlane) {
			world.x = -track.ground.y;
			world.z = track.ground.x;
			world.y = 0;
		} else {
			// just put it some place far away
			world.x = -track.ground.y*1000;
			world.z = track.ground.x*1000;
			world.y = 0;
		}

//		SePointOps_F64.transform(cameraToWorld,point3D,point3D);

		return true;
	}

	@Override
	public int getTotalTracks() {
		if (active == null)
			active = alg.getTracker().getActiveTracks(null);
		return active.size();
	}

	@Override
	public long getTrackId( int index ) {
		if (active == null)
			active = alg.getTracker().getActiveTracks(null);

		PointTrack t = active.get(index);
		return t.featureId;
	}

	@Override
	public void getTrackPixel( int index, Point2D_F64 pixel ) {
		pixel.setTo(Objects.requireNonNull(active).get(index).pixel);
	}

	@Override
	public List<Point2D_F64> getAllTracks( @Nullable List<Point2D_F64> storage ) {
		if (storage == null)
			storage = new ArrayList<>();
		else
			storage.clear();

		if (active == null)
			active = alg.getTracker().getActiveTracks(null);

		for (int i = 0; i < active.size(); i++) {
			storage.add(active.get(i).pixel);
		}

		return storage;
	}

	@Override
	public boolean isTrackInlier( int index ) {
		if (active == null)
			active = alg.getTracker().getActiveTracks(null);

		PointTrack t = active.get(index);
		VisOdomMonoPlaneInfinity.VoTrack v = t.getCookie();

		return v.lastInlier == alg.getFrameID();
	}

	@Override
	public boolean isTrackNew( int index ) {
		// need to figure out a way to efficiently implement this
		return false;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {

	}
}
