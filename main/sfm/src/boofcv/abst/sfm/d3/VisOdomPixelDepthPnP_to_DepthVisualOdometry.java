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
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.sfm.d3.VisOdomPixelDepthPnP;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.distort.PixelTransform2_F32;
import boofcv.struct.distort.Point2Transform2_F64;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
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
 * Wrapper around {@link VisOdomPixelDepthPnP} for {@link DepthVisualOdometry}.
 *
 * @author Peter Abeles
 */
// TODO WARNING! active list has been modified by dropping and adding tracks
// this is probably true of other SFM algorithms
public class VisOdomPixelDepthPnP_to_DepthVisualOdometry<Vis extends ImageBase, Depth extends ImageGray>
	implements DepthVisualOdometry<Vis,Depth> , AccessPointTracks3D
{
	// low level algorithm
	DepthSparse3D<Depth> sparse3D;
	VisOdomPixelDepthPnP<Vis> alg;
	DistanceModelMonoPixels<Se3_F64,Point2D3D> distance;
	ImageType<Vis> visualType;
	Class<Depth> depthType;
	boolean success;

	List<PointTrack> active = new ArrayList<>();

	public VisOdomPixelDepthPnP_to_DepthVisualOdometry(DepthSparse3D<Depth> sparse3D, VisOdomPixelDepthPnP<Vis> alg,
													   DistanceModelMonoPixels<Se3_F64, Point2D3D> distance,
													   ImageType<Vis> visualType, Class<Depth> depthType) {
		this.sparse3D = sparse3D;
		this.alg = alg;
		this.distance = distance;
		this.visualType = visualType;
		this.depthType = depthType;
	}

	@Override
	public Point3D_F64 getTrackLocation(int index) {
		// TODO see comment above
		try {
			PointTrack t = alg.getTracker().getActiveTracks(null).get(index);
			return ((Point2D3D)t.getCookie()).getLocation();
		} catch( IndexOutOfBoundsException e ) {
			return new Point3D_F64();
		}
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
	public void setCalibration(CameraPinholeRadial paramVisual, PixelTransform2_F32 visToDepth) {
		sparse3D.configure(paramVisual,visToDepth);

		Point2Transform2_F64 leftPixelToNorm = transformPoint(paramVisual).undistort_F64(true,false);
		Point2Transform2_F64 leftNormToPixel = transformPoint(paramVisual).distort_F64(false,true);

		alg.setPixelToNorm(leftPixelToNorm);
		alg.setNormToPixel(leftNormToPixel);

		distance.setIntrinsic(paramVisual.fx,paramVisual.fy,paramVisual.skew);
	}

	@Override
	public boolean process(Vis visual, Depth depth) {
		sparse3D.setDepthImage(depth);
		success = alg.process(visual);

		active.clear();
		alg.getTracker().getActiveTracks(active);

		return success;
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

	@Override
	public ImageType<Vis> getVisualType() {
		return visualType;
	}

	@Override
	public Class<Depth> getDepthType() {
		return depthType;
	}
}
