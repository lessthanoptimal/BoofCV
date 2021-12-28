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

import boofcv.abst.sfm.AccessPointTracks;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.alg.sfm.d3.VisOdomMonoOverheadMotion2D;
import boofcv.alg.sfm.overhead.OverheadView;
import boofcv.factory.distort.LensDistortionFactory;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around {@link VisOdomMonoOverheadMotion2D} for {@link MonocularPlaneVisualOdometry}.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class MonoOverhead_to_MonocularPlaneVisualOdometry<T extends ImageBase<T>>
		implements MonocularPlaneVisualOdometry<T>, AccessPointTracks3D {
	// motion estimation algorithm
	VisOdomMonoOverheadMotion2D<T> alg;

	ImageType<T> imageType;

	boolean fault;
	Se3_F64 cameraToWorld = new Se3_F64();

	boolean computed;
	DogArray<Point2D_F64> pixels = new DogArray<>(Point2D_F64::new);
	DogArray<Point3D_F64> points3D = new DogArray<>(Point3D_F64::new);

	Se3_F64 planeToCamera;
	Point2Transform2_F64 normToPixel;

	public MonoOverhead_to_MonocularPlaneVisualOdometry( VisOdomMonoOverheadMotion2D<T> alg, ImageType<T> imageType ) {
		this.alg = alg;
		this.imageType = imageType;
	}

	@Override
	public void setCalibration( MonoPlaneParameters param ) {
		this.planeToCamera = param.planeToCamera;
		alg.configureCamera(param.intrinsic, param.planeToCamera);
		normToPixel = LensDistortionFactory.narrow(param.intrinsic).distort_F64(false, true);
	}

	@Override
	public boolean process( T input ) {
		computed = false;
		fault = alg.process(input);
		return fault;
	}

	@Override
	public ImageType<T> getImageType() {
		return imageType;
	}

	@Override
	public void reset() {
		alg.reset();
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
		return alg.getMotion2D().getFrameID();
	}

	@Override
	public boolean getTrackWorld3D( int index, Point3D_F64 world ) {
		computeTracks();
		world.setTo(points3D.get(index));
		return true;
	}

	@Override
	public int getTotalTracks() {
		computeTracks();
		return pixels.size;
	}

	@Override
	public long getTrackId( int index ) {
		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();

		return accessPlane.getTrackId(index);
	}

	@Override
	public void getTrackPixel( int index, Point2D_F64 pixel ) {
		pixel.setTo(this.pixels.get(index));
	}

	@Override
	public List<Point2D_F64> getAllTracks( @Nullable List<Point2D_F64> storage ) {
		if (storage == null)
			storage = new ArrayList<>();
		else
			storage.clear();

		computeTracks();

		storage.addAll(pixels.toList());
		return storage;
	}

	@Override
	public boolean isTrackInlier( int index ) {
		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();

		return accessPlane.isTrackInlier(index);
	}

	@Override
	public boolean isTrackNew( int index ) {
		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();

		return accessPlane.isTrackNew(index);
	}

	private void computeTracks() {
		if (computed)
			return;

		if (!(alg.getMotion2D() instanceof AccessPointTracks))
			return;

		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();
		List<Point2D_F64> tracksPlane = accessPlane.getAllTracks(null);

		OverheadView<T> map = alg.getOverhead();

		points3D.reset();
		pixels.reset();

		for (int pointIdx = 0; pointIdx < tracksPlane.size(); pointIdx++) {
			Point2D_F64 worldPt = tracksPlane.get(pointIdx);
			// 2D to 3D
			Point3D_F64 p = points3D.grow();
			p.z = worldPt.x*map.cellSize - map.centerX;
			p.x = -(worldPt.y*map.cellSize - map.centerY);
			p.y = 0;

			// 3D world to camera
			SePointOps_F64.transform(planeToCamera, p, p);

			// normalized image coordinates
			normToPixel.compute(p.x/p.z, p.y/p.z, pixels.grow());
		}

		computed = true;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {}
}
