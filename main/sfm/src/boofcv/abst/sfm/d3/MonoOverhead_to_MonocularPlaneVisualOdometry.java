/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.sfm.d3.VisOdomMonoOverheadMotion2D;
import boofcv.alg.sfm.overhead.OverheadView;
import boofcv.struct.calib.MonoPlaneParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.FastQueue;

import java.util.List;

/**
 * Wrapper around {@link VisOdomMonoOverheadMotion2D} for {@link MonocularPlaneVisualOdometry}.
 *
 * @author Peter Abeles
 */
public class MonoOverhead_to_MonocularPlaneVisualOdometry<T extends ImageBase>
		implements MonocularPlaneVisualOdometry<T> , AccessPointTracks3D
{
	// motion estimation algorithm
	VisOdomMonoOverheadMotion2D<T> alg;


	ImageType<T> imageType;

	boolean fault;
	Se3_F64 cameraToWorld = new Se3_F64();

	boolean computed;
	FastQueue<Point2D_F64> pixels = new FastQueue<>(Point2D_F64.class, true);
	FastQueue<Point3D_F64> points3D = new FastQueue<>(Point3D_F64.class, true);

	Se3_F64 planeToCamera;
	Point2Transform2_F64 normToPixel;

	public MonoOverhead_to_MonocularPlaneVisualOdometry(VisOdomMonoOverheadMotion2D<T> alg, ImageType<T> imageType) {
		this.alg = alg;
		this.imageType = imageType;
	}

	@Override
	public void setCalibration( MonoPlaneParameters param ) {
		this.planeToCamera = param.planeToCamera;
		alg.configureCamera(param.intrinsic, param.planeToCamera);
		normToPixel = LensDistortionOps.transformPoint(param.intrinsic).distort_F64(false,true);
	}

	@Override
	public boolean process(T input) {
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
	public Point3D_F64 getTrackLocation(int index) {
		computeTracks();

		return points3D.get(index);
	}

	@Override
	public long getTrackId(int index) {
		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();

		return accessPlane.getTrackId(index);
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		computeTracks();

		return pixels.toList();
	}

	@Override
	public boolean isInlier(int index) {
		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();

		return accessPlane.isInlier(index);
	}

	@Override
	public boolean isNew(int index) {
		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();

		return accessPlane.isNew(index);
	}

	private void computeTracks() {
		if( computed )
			return;

		if( !(alg.getMotion2D() instanceof AccessPointTracks))
			return;

		AccessPointTracks accessPlane = (AccessPointTracks)alg.getMotion2D();
		List<Point2D_F64> tracksPlane = accessPlane.getAllTracks();

		OverheadView<T> map = alg.getOverhead();

		points3D.reset();
		pixels.reset();

		for( Point2D_F64 worldPt : tracksPlane ) {
			// 2D to 3D
			Point3D_F64 p = points3D.grow();
			p.z = worldPt.x*map.cellSize-map.centerX;
			p.x = -(worldPt.y*map.cellSize-map.centerY);
			p.y = 0;

			// 3D world to camera
			SePointOps_F64.transform(planeToCamera,p,p);

			// normalized image coordinates
			normToPixel.compute(p.x/p.z,p.y/p.z,pixels.grow());
		}

		computed = true;
	}
}