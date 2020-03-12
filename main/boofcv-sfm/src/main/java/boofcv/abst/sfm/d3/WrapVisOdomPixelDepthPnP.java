/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.sfm.StereoSparse3D;
import boofcv.alg.sfm.d3.VisOdomPixelDepthPnP;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO WARNING! active list has been modified by dropping and adding tracks
// this is probably true of other SFM algorithms
public class WrapVisOdomPixelDepthPnP<T extends ImageGray<T>>
		implements StereoVisualOdometry<T>, AccessPointTracks3D {

	// low level algorithm
	VisOdomPixelDepthPnP<T> alg;
	StereoSparse3D<T> stereo;
	DistanceFromModelMultiView<Se3_F64,Point2D3D> distance;
	Class<T> imageType;
	boolean success;

	List<PointTrack> active = new ArrayList<>();

	public WrapVisOdomPixelDepthPnP(VisOdomPixelDepthPnP<T> alg,
									StereoSparse3D<T> stereo,
									DistanceFromModelMultiView<Se3_F64,Point2D3D> distance,
									Class<T> imageType) {
		this.alg = alg;
		this.stereo = stereo;
		this.distance = distance;
		this.imageType = imageType;
	}

	@Override
	public boolean getTrackWorld3D(int index, Point3D_F64 world) {
		Point4D_F64 p = alg.getVisibleTracks().get(index).worldLoc;
		world.set(p.x/p.w, p.y/p.w, p.z/p.w);
		return true;
	}

	@Override
	public int getTotalTracks() {
		return alg.getVisibleTracks().size();
	}

	@Override
	public long getTrackId(int index) {
		return alg.getVisibleTracks().get(index).id;
	}

	@Override
	public void getTrackPixel(int index, Point2D_F64 pixel) {
		// If this throws a null pointer exception then that means there's a bug. The only way a visible track
		// could have a null trackerTrack is if the trackerTrack was dropped. In that case it's no longer visible
		pixel.set( alg.getVisibleTracks().get(index).trackerTrack.pixel );
	}

	@Override
	public List<Point2D_F64> getAllTracks(@Nullable List<Point2D_F64> storage ) {
		throw new RuntimeException("Not supported any more");
	}

	@Override
	public boolean isTrackInlier(int index) {
		return alg.getInlierTracks().contains(alg.getVisibleTracks().get(index));
	}

	@Override
	public boolean isTrackNew(int index) {
		VisOdomPixelDepthPnP.Track track = alg.getVisibleTracks().get(index);
		return track.trackerTrack.spawnFrameID == alg.getFrameID();
	}

	@Override
	public void setCalibration( StereoParameters parameters ) {
		stereo.setCalibration(parameters);
		alg.setCamera(parameters.left);
		distance.setIntrinsic(0,parameters.left);
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

	@Override
	public long getFrameID() {
		return alg.getFrameID();
	}

	public VisOdomPixelDepthPnP<T> getAlgorithm() {
		return alg;
	}
}
