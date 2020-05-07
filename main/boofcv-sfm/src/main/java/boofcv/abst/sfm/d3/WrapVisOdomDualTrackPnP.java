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
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.pose.PnPStereoDistanceReprojectionSq;
import boofcv.alg.geo.pose.PnPStereoEstimator;
import boofcv.alg.geo.pose.RefinePnPStereo;
import boofcv.alg.sfm.d3.VisOdomDualTrackPnP;
import boofcv.struct.calib.CameraPinholeBrown;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import lombok.Getter;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class WrapVisOdomDualTrackPnP<T extends ImageGray<T>>
		implements StereoVisualOdometry<T>, AccessPointTracks3D
{
	@Getter	RefinePnPStereo refine;
	@Getter	PnPStereoEstimator pnp;
	@Getter	DistanceFromModelMultiView<Se3_F64,Point2D3D> distanceMono;
	@Getter	PnPStereoDistanceReprojectionSq distanceStereo;
	@Getter	AssociateStereo2D<?> assoc;

	VisOdomDualTrackPnP<T,?> visualOdometry;

	Class<T> imageType;

	boolean success;

	public WrapVisOdomDualTrackPnP(VisOdomDualTrackPnP<T, ?> visualOdometry, PnPStereoEstimator pnp,
								   DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceMono,
								   PnPStereoDistanceReprojectionSq distanceStereo,
								   AssociateStereo2D<?> assoc,
								   RefinePnPStereo refine,
								   Class<T> imageType) {
		this.visualOdometry = visualOdometry;
		this.pnp = pnp;
		this.distanceMono = distanceMono;
		this.distanceStereo = distanceStereo;
		this.assoc = assoc;
		this.refine = refine;
		this.imageType = imageType;
	}

	@Override
	public boolean getTrackWorld3D(int index, Point3D_F64 world ) {
		VisOdomDualTrackPnP.TrackInfo info = visualOdometry.getVisibleTracks().get(index);
		PerspectiveOps.homogenousTo3dPositiveZ(info.worldLoc,1e8,1e-8,world);
		return true;
	}

	@Override public int getTotalTracks() {return visualOdometry.getVisibleTracks().size();}
	@Override public long getTrackId(int index) {return visualOdometry.getVisibleTracks().get(index).id;}

	@Override
	public void getTrackPixel(int index, Point2D_F64 pixel) {
		// If this throws a null pointer exception then that means there's a bug. The only way a visible track
		// could have a null trackerTrack is if the trackerTrack was dropped. In that case it's no longer visible
		pixel.set( visualOdometry.getVisibleTracks().get(index).visualTrack.pixel );
	}

	@Override
	public List<Point2D_F64> getAllTracks(@Nullable List<Point2D_F64> storage ) {
		throw new RuntimeException("Not supported any more");
	}

	@Override
	public boolean isTrackInlier(int index) {
		VisOdomDualTrackPnP.TrackInfo info = visualOdometry.getVisibleTracks().get(index);
		return info.lastInlier == visualOdometry.getFrameID();
	}

	@Override public boolean isTrackNew(int index) {
		VisOdomDualTrackPnP.TrackInfo track = visualOdometry.getVisibleTracks().get(index);
		return track.visualTrack.spawnFrameID == visualOdometry.getFrameID();
	}

	@Override
	public void setCalibration(StereoParameters parameters) {
		Se3_F64 leftToRight = parameters.getRightToLeft().invert(null);

		pnp.setLeftToRight(leftToRight);
		if( refine != null )
			refine.setLeftToRight(leftToRight);
		visualOdometry.setCalibration(parameters);

		CameraPinholeBrown left = parameters.left;
		distanceMono.setIntrinsic(0,left);
		distanceStereo.setLeftToRight(parameters.rightToLeft.invert(null));
		distanceStereo.setIntrinsic(0,parameters.left);
		distanceStereo.setIntrinsic(1,parameters.right);
		assoc.setCalibration(parameters);
	}

	@Override public void reset() {
		visualOdometry.reset();}
	@Override public Se3_F64 getCameraToWorld() {return visualOdometry.getCurrentToWorld();}
	@Override public long getFrameID() {return visualOdometry.getFrameID();}
	@Override public boolean process(T leftImage, T rightImage) {return success = visualOdometry.process(leftImage,rightImage);}

	@Override
	public boolean isFault() {
		if( !success)
			return visualOdometry.isFault();
		else
			return false;
	}

	@Override public ImageType<T> getImageType() {return ImageType.single(imageType);}

	@Override
	public void setVerbose(@Nullable PrintStream out, @Nullable Set<String> configuration) {
		visualOdometry.setVerbose(out,configuration);
	}

	public VisOdomDualTrackPnP<T,?> getAlgorithm() {
		return visualOdometry;
	}
}
