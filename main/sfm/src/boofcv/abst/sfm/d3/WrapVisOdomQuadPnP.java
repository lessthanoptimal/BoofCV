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

import boofcv.abst.sfm.AccessPointTracks3D;
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.PnPStereoDistanceReprojectionSq;
import boofcv.alg.geo.pose.RefinePnPStereo;
import boofcv.alg.sfm.d3.VisOdomQuadPnP;
import boofcv.struct.calib.CameraPinholeRadial;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import boofcv.struct.sfm.Stereo2D3D;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.fitting.modelset.ModelMatcher;
import org.ddogleg.struct.FastQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper around {@link VisOdomQuadPnP} for {@link StereoVisualOdometry}.
 *
 * @author Peter Abeles
 */
public class WrapVisOdomQuadPnP<T extends ImageGray,TD extends TupleDesc>
		implements StereoVisualOdometry<T>, AccessPointTracks3D
{
	VisOdomQuadPnP<T,TD> alg;
	RefinePnPStereo refine;
	AssociateStereo2D<TD> associateStereo;
	PnPStereoDistanceReprojectionSq distance;
	DistanceModelMonoPixels<Se3_F64,Point2D3D> distanceMono;
	Class<T> imageType;

	public WrapVisOdomQuadPnP(VisOdomQuadPnP<T, TD> alg,
							  RefinePnPStereo refine,
							  AssociateStereo2D<TD> associateStereo,
							  PnPStereoDistanceReprojectionSq distance,
							  DistanceModelMonoPixels<Se3_F64,Point2D3D> distanceMono,
							  Class<T> imageType)
	{
		this.alg = alg;
		this.refine = refine;
		this.associateStereo = associateStereo;
		this.distance = distance;
		this.distanceMono = distanceMono;
		this.imageType = imageType;
	}

	@Override
	public Point3D_F64 getTrackLocation(int index) {
		FastQueue<VisOdomQuadPnP.QuadView> features =  alg.getQuadViews();

		return features.get(index).X;
	}

	@Override
	public long getTrackId(int index) {
		return 0;
	}

	@Override
	public List<Point2D_F64> getAllTracks() {
		FastQueue<VisOdomQuadPnP.QuadView> features =  alg.getQuadViews();

		List<Point2D_F64> ret = new ArrayList<>();
		for( VisOdomQuadPnP.QuadView v : features.toList() )
			ret.add(v.v2); // new left camera

		return ret;
	}

	@Override
	public boolean isInlier(int index) {
		ModelMatcher<Se3_F64, Stereo2D3D> matcher = alg.getMatcher();
		int N = matcher.getMatchSet().size();
		for( int i = 0; i < N; i++ ) {
			if( matcher.getInputIndex(i) == index )
				return true;
		}
		return false;
	}

	@Override
	public boolean isNew(int index) {
		return false;// its always new
	}

	@Override
	public void setCalibration(StereoParameters parameters) {
		Se3_F64 leftToRight = parameters.getRightToLeft().invert(null);

		alg.setCalibration(parameters);
		associateStereo.setCalibration(parameters);
		distance.setStereoParameters(parameters);

		CameraPinholeRadial left = parameters.left;
		distanceMono.setIntrinsic(left.fx,left.fy,left.skew);

		if( refine != null )
			refine.setLeftToRight(leftToRight);
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public Se3_F64 getCameraToWorld() {
		return alg.getLeftToWorld();
	}

	@Override
	public boolean process(T leftImage, T rightImage) {
		return alg.process(leftImage,rightImage);
	}

	@Override
	public boolean isFault() {
		return false;
	}

	@Override
	public ImageType<T> getImageType() {
		return ImageType.single(imageType);
	}
}
