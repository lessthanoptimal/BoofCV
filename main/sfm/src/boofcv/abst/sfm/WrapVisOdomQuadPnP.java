/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.abst.sfm;

import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceModelMonoPixels;
import boofcv.alg.geo.pose.PnPStereoDistanceReprojectionSq;
import boofcv.alg.geo.pose.RefinePnPStereo;
import boofcv.alg.sfm.d3.VisOdomQuadPnP;
import boofcv.struct.FastQueue;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class WrapVisOdomQuadPnP<T extends ImageSingleBand,TD extends TupleDesc>
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

		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();
		for( VisOdomQuadPnP.QuadView v : features.toList() )
			ret.add(v.v0);

		return ret;
	}

	@Override
	public boolean isInlier(int index) {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public boolean isNew(int index) {
		return false;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void setCalibration(StereoParameters parameters) {
		Se3_F64 leftToRight = parameters.getRightToLeft().invert(null);

		alg.setCalibration(parameters);
		associateStereo.setCalibration(parameters);
		distance.setStereoParameters(parameters);

		IntrinsicParameters left = parameters.left;
		distanceMono.setIntrinsic(left.fx,left.fy,left.skew);

		if( refine != null )
			refine.setLeftToRight(leftToRight);
	}

	@Override
	public void reset() {
		alg.reset();
	}

	@Override
	public boolean isFatal() {
		return false;
	}

	@Override
	public Se3_F64 getLeftToWorld() {
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
	public Class<T> getImageType() {
		return imageType;
	}
}
