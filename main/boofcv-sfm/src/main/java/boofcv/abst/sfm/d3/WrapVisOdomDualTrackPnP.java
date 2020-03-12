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
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceFromModelMultiView;
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

import javax.annotation.Nullable;
import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class WrapVisOdomDualTrackPnP<T extends ImageGray<T>>
		implements StereoVisualOdometry<T>, AccessPointTracks3D
{
	RefinePnPStereo refine;
	PnPStereoEstimator pnp;
	DistanceFromModelMultiView<Se3_F64,Point2D3D> distanceMono;
	PnPStereoDistanceReprojectionSq distanceStereo;
	AssociateStereo2D<?> assoc;

	VisOdomDualTrackPnP<T,?> alg;

	Class<T> imageType;

	boolean success;

	public WrapVisOdomDualTrackPnP(PnPStereoEstimator pnp,
								   DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceMono,
								   PnPStereoDistanceReprojectionSq distanceStereo,
								   AssociateStereo2D<?> assoc,
								   VisOdomDualTrackPnP<T, ?> alg,
								   RefinePnPStereo refine,
								   Class<T> imageType) {
		this.pnp = pnp;
		this.distanceMono = distanceMono;
		this.distanceStereo = distanceStereo;
		this.assoc = assoc;
		this.alg = alg;
		this.refine = refine;
		this.imageType = imageType;
	}

	@Override
	public boolean getTrackWorld3D(int index, Point3D_F64 world ) {
		VisOdomDualTrackPnP.LeftTrackInfo info = alg.getCandidates().get(index).getCookie();
		world.set( info.location.location );
		return true;
	}

	@Override
	public int getTotalTracks() {
		return alg.getCandidates().size();
	}

	@Override
	public long getTrackId(int index) {
		return alg.getCandidates().get(index).featureId;
	}

	@Override
	public void getTrackPixel(int index, Point2D_F64 pixel) {
		pixel.set( alg.getCandidates().get(index).pixel );
	}

	@Override
	public List<Point2D_F64> getAllTracks(@Nullable List<Point2D_F64> storage ) {
		return PointTrack.extractTrackPixels(storage,alg.getCandidates());
	}

	@Override
	public boolean isTrackInlier(int index) {
		VisOdomDualTrackPnP.LeftTrackInfo info = alg.getCandidates().get(index).getCookie();
		return info.lastInlier == alg.getFrameID();
	}

	@Override
	public boolean isTrackNew(int index) {
		return false;
	}

	@Override
	public void setCalibration(StereoParameters parameters) {

		Se3_F64 leftToRight = parameters.getRightToLeft().invert(null);

		pnp.setLeftToRight(leftToRight);
		if( refine != null )
			refine.setLeftToRight(leftToRight);
		alg.setCalibration(parameters);

		CameraPinholeBrown left = parameters.left;
		distanceMono.setIntrinsic(0,left);
		distanceStereo.setLeftToRight(parameters.rightToLeft.invert(null));
		distanceStereo.setIntrinsic(0,parameters.left);
		distanceStereo.setIntrinsic(1,parameters.right);
		assoc.setCalibration(parameters);
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		return alg.getCurrToWorld();
	}

	@Override
	public long getFrameID() {
		return alg.getFrameID();
	}

	@Override
	public boolean process(T leftImage, T rightImage) {
		return success = alg.process(leftImage,rightImage);
	}

	@Override
	public boolean isFault() {
		if( !success)
			return alg.isFault();
		else
			return false;
	}

	@Override
	public ImageType<T> getImageType() {
		return ImageType.single(imageType);
	}
}
