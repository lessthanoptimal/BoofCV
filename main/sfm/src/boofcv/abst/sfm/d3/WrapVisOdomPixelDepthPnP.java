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

import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.VisOdomPixelDepthPnP;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.sfm.Point2D3DTrack;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

import static boofcv.alg.distort.LensDistortionOps.transformPoint;

/**
 * @author Peter Abeles
 */
// TODO WARNING! active list has been modified by dropping and adding tracks
// this is probably true of other SFM algorithms
public class WrapVisOdomPixelDepthPnP<T extends ImageGray>
		implements StereoVisualOdometry<T>, AccessPointTracks3D {

	// low level algorithm
	VisOdomPixelDepthPnP<T> alg;
	StereoSparse3D<T> stereo;
	DistanceModelMonoPixels<Se3_F64,Point2D3D> distance;
	Class<T> imageType;
	boolean success;

	List<PointTrack> active = new ArrayList<>();

	public WrapVisOdomPixelDepthPnP(VisOdomPixelDepthPnP<T> alg,
									StereoSparse3D<T> stereo,
									DistanceModelMonoPixels<Se3_F64,Point2D3D> distance,
									Class<T> imageType) {
		this.alg = alg;
		this.stereo = stereo;
		this.distance = distance;
		this.imageType = imageType;
	}

	@Override
	public Point3D_F64 getTrackLocation(int index) {
		// TODO see comment above
		PointTrack t = alg.getTracker().getActiveTracks(null).get(index);
		return ((Point2D3D)t.getCookie()).getLocation();
	}

	@Override
	public long getTrackId(int index) {
		return active.get(index).featureId;
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		return (List)active;
	}

	@Override
	public boolean isInlier(int index) {
		Point2D3DTrack t = active.get(index).getCookie();
		return t.lastInlier == alg.getTick();
	}

	@Override
	public boolean isNew(int index) {
		PointTrack t = alg.getTracker().getActiveTracks(null).get(index);
		return alg.getTracker().getNewTracks(null).contains(t);
	}

	@Override
	public void setCalibration( StereoParameters parameters ) {
		stereo.setCalibration(parameters);

		CameraPinholeRadial l = parameters.left;

		Point2Transform2_F64 leftPixelToNorm = transformPoint(l).undistort_F64(true, false);
		Point2Transform2_F64 leftNormToPixel = transformPoint(l).distort_F64(false, true);

		alg.setPixelToNorm(leftPixelToNorm);
		alg.setNormToPixel(leftNormToPixel);
		distance.setIntrinsic(l.fx,l.fy,l.skew);
	}

	@Override
	public boolean process(T leftImage, T rightImage) {
		stereo.setImages(leftImage,rightImage);
		success = alg.process(leftImage);

		active.clear();
		alg.getTracker().getActiveTracks(active);

		return success;
	}

	@Override
	public ImageType<T> getImageType() {
		return ImageType.single(imageType);
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public boolean isFault() {
		return !success;
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		return alg.getCurrToWorld();
	}
}
