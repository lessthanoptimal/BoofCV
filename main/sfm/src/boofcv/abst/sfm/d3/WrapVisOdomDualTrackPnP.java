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
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.PnPStereoDistanceReprojectionSq;
import boofcv.alg.geo.pose.PnPStereoEstimator;
import boofcv.alg.geo.pose.RefinePnPStereo;
import boofcv.alg.sfm.d3.VisOdomDualTrackPnP;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO comment
 *
 * @author Peter Abeles
 */
public class WrapVisOdomDualTrackPnP<T extends ImageGray>
		implements StereoVisualOdometry<T>, AccessPointTracks3D
{
	RefinePnPStereo refine;
	PnPStereoEstimator pnp;
	DistanceModelMonoPixels<Se3_F64,Point2D3D> distanceMono;
	PnPStereoDistanceReprojectionSq distanceStereo;
	AssociateStereo2D<?> assoc;

	VisOdomDualTrackPnP<T,?> alg;

	Class<T> imageType;

	boolean success;

	public WrapVisOdomDualTrackPnP(PnPStereoEstimator pnp,
								   DistanceModelMonoPixels<Se3_F64, Point2D3D> distanceMono,
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
	public Point3D_F64 getTrackLocation(int index) {
		VisOdomDualTrackPnP.LeftTrackInfo info = alg.getCandidates().get(index).getCookie();
		return info.location.location;
	}

	@Override
	public long getTrackId(int index) {
		return alg.getCandidates().get(index).featureId;
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		List<Point2D_F64> ret = new ArrayList<>();

		for( PointTrack c : alg.getCandidates() ) {
			ret.add(c);
		}

		return ret;
	}

	@Override
	public boolean isInlier(int index) {
		VisOdomDualTrackPnP.LeftTrackInfo info = alg.getCandidates().get(index).getCookie();
		return info.lastInlier == alg.getTick();
	}

	@Override
	public boolean isNew(int index) {
		return false;
	}

	@Override
	public void setCalibration(StereoParameters parameters) {

		Se3_F64 leftToRight = parameters.getRightToLeft().invert(null);

		pnp.setLeftToRight(leftToRight);
		if( refine != null )
			refine.setLeftToRight(leftToRight);
		alg.setCalibration(parameters);

		CameraPinholeRadial left = parameters.left;
		distanceMono.setIntrinsic(left.fx,left.fy,left.skew);
		distanceStereo.setStereoParameters(parameters);
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
