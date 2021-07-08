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
import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.sfm.DepthSparse3D;
import boofcv.alg.sfm.d3.VisOdomMonoDepthPnP;
import boofcv.alg.sfm.d3.structure.VisOdomBundleAdjustment;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.distort.Point2Transform2_F32;
import boofcv.struct.distort.PointToPixelTransform_F32;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.sfm.Point2D3DTrack;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Point4D_F64;
import georegression.struct.se.Se3_F64;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static boofcv.factory.distort.LensDistortionFactory.narrow;

/**
 * Wrapper around {@link VisOdomMonoDepthPnP} for {@link DepthVisualOdometry}.
 *
 * @author Peter Abeles
 */
// TODO WARNING! active list has been modified by dropping and adding tracks
// this is probably true of other SFM algorithms
public class VisOdomPixelDepthPnP_to_DepthVisualOdometry<Vis extends ImageBase<Vis>, Depth extends ImageGray<Depth>>
		implements DepthVisualOdometry<Vis, Depth>, AccessPointTracks3D {
	// low level algorithm
	DepthSparse3D<Depth> sparse3D;
	VisOdomMonoDepthPnP<Vis> alg;
	DistanceFromModelMultiView<Se3_F64, Point2D3D> distance;
	ImageType<Vis> visualType;
	Class<Depth> depthType;
	boolean success;

	List<PointTrack> active = new ArrayList<>();
	long frameID;

	public VisOdomPixelDepthPnP_to_DepthVisualOdometry( DepthSparse3D<Depth> sparse3D, VisOdomMonoDepthPnP<Vis> alg,
														DistanceFromModelMultiView<Se3_F64, Point2D3D> distance,
														ImageType<Vis> visualType, Class<Depth> depthType ) {
		this.sparse3D = sparse3D;
		this.alg = alg;
		this.distance = distance;
		this.visualType = visualType;
		this.depthType = depthType;
	}

	@Override
	public boolean getTrackWorld3D( int index, Point3D_F64 world ) {
		try {
			Point4D_F64 p = ((VisOdomBundleAdjustment.BTrack)active.get(index).getCookie()).worldLoc;
			world.setTo(p.x/p.w, p.y/p.w, p.z/p.w);
			return true;
		} catch (RuntimeException ignore) { /* not handled */ }
		world.setTo(((Point2D3D)active.get(index).getCookie()).getLocation());
		return true;
	}

	@Override
	public int getTotalTracks() {
		return active.size();
	}

	@Override
	public long getTrackId( int index ) {
		return active.get(index).featureId;
	}

	@Override
	public void getTrackPixel( int index, Point2D_F64 pixel ) {
		pixel.setTo(active.get(index).pixel);
	}

	@Override
	public List<Point2D_F64> getAllTracks( @Nullable List<Point2D_F64> storage ) {
		return PointTrack.extractTrackPixels(storage, active);
	}

	@Override
	public boolean isTrackInlier( int index ) {
		try {
			Point2D3DTrack t = active.get(index).getCookie();
			return t.lastInlier == alg.getFrameID();
		} catch (RuntimeException ignore) {}

		VisOdomMonoDepthPnP.Track t = active.get(index).getCookie();
		return t.lastUsed == alg.getFrameID();
	}

	@Override
	public boolean isTrackNew( int index ) {
		return active.get(index).spawnFrameID == frameID;
	}

	@Override
	public void setCalibration( CameraPinholeBrown paramVisual, Point2Transform2_F32 visToDepth ) {
		PointToPixelTransform_F32 visToDepth_pixel = new PointToPixelTransform_F32(visToDepth);
		sparse3D.configure(narrow(paramVisual), visToDepth_pixel);
		alg.setCamera(paramVisual);
		distance.setIntrinsic(0, paramVisual);
	}

	@Override
	public boolean process( Vis visual, Depth depth ) {
		sparse3D.setDepthImage(depth);
		success = alg.process(visual);
		frameID = alg.getFrameID();

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
		return alg.getCurrentToWorld();
	}

	@Override
	public long getFrameID() {
		return alg.getFrameID();
	}

	@Override
	public ImageType<Vis> getVisualType() {
		return visualType;
	}

	@Override
	public Class<Depth> getDepthType() {
		return depthType;
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		alg.setVerbose(out, configuration);
	}
}
