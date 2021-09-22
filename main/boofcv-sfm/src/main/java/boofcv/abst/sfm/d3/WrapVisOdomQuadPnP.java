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
import boofcv.alg.feature.associate.AssociateStereo2D;
import boofcv.alg.geo.DistanceFromModelMultiView;
import boofcv.alg.geo.pose.PnPStereoDistanceReprojectionSq;
import boofcv.alg.geo.pose.RefinePnPStereo;
import boofcv.alg.sfm.d3.VisOdomStereoQuadPnP;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.geo.Point2D3D;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import org.ddogleg.struct.DogArray;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Wrapper around {@link VisOdomStereoQuadPnP} for {@link StereoVisualOdometry}.
 *
 * @author Peter Abeles
 */
public class WrapVisOdomQuadPnP<T extends ImageGray<T>, TD extends TupleDesc<TD>>
		implements StereoVisualOdometry<T>, AccessPointTracks3D {
	@Getter VisOdomStereoQuadPnP<T, TD> alg;
	@Nullable RefinePnPStereo refine;
	@Getter AssociateStereo2D<TD> associateStereo;
	@Getter PnPStereoDistanceReprojectionSq distanceStereo;
	@Getter DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceLeft;
	@Getter DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceRight;

	Class<T> imageType;

	// Doesn't really have tracks. So it's hacked by given every feature a new ID
	long totalFeatures;

	public WrapVisOdomQuadPnP( VisOdomStereoQuadPnP<T, TD> alg,
							   @Nullable RefinePnPStereo refine,
							   AssociateStereo2D<TD> associateStereo,
							   PnPStereoDistanceReprojectionSq distance,
							   DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceLeft,
							   DistanceFromModelMultiView<Se3_F64, Point2D3D> distanceRight,
							   Class<T> imageType ) {
		this.alg = alg;
		this.refine = refine;
		this.associateStereo = associateStereo;
		this.distanceStereo = distance;
		this.distanceLeft = distanceLeft;
		this.distanceRight = distanceRight;
		this.imageType = imageType;
	}

	@Override
	public boolean getTrackWorld3D( int index, Point3D_F64 world ) {
		Se3_F64 left_to_world = alg.getLeftToWorld();
		DogArray<VisOdomStereoQuadPnP.TrackQuad> features = alg.getTrackQuads();
		SePointOps_F64.transform(left_to_world, features.get(index).X, world);
		return true;
	}

	@Override
	public int getTotalTracks() {return alg.getTrackQuads().size;}

	@Override
	public long getTrackId( int index ) {return alg.getTrackQuads().get(index).id;}

	@Override
	public void getTrackPixel( int index, Point2D_F64 pixel ) {
		pixel.setTo(alg.getTrackQuads().get(index).v2);
	}

	@Override
	public List<Point2D_F64> getAllTracks( @Nullable List<Point2D_F64> storage ) {
		if (storage == null)
			storage = new ArrayList<>();
		else
			storage.clear();

		DogArray<VisOdomStereoQuadPnP.TrackQuad> features = alg.getTrackQuads();

		for (VisOdomStereoQuadPnP.TrackQuad v : features.toList())
			storage.add(v.v2); // new left camera

		return storage;
	}

	@Override
	public boolean isTrackInlier( int index ) {
		return alg.getTrackQuads().get(index).inlier;
	}

	@Override
	public boolean isTrackNew( int index ) {
		long frameId = alg.getFrameID();
		return alg.getTrackQuads().get(index).firstSceneFrameID == frameId;
	}

	@Override
	public void setCalibration( StereoParameters parameters ) {
		Se3_F64 leftToRight = parameters.getRightToLeft().invert(null);

		alg.setCalibration(parameters);
		associateStereo.setCalibration(parameters);
		distanceStereo.setStereoParameters(parameters);
		distanceLeft.setIntrinsic(0, parameters.left);
		distanceRight.setIntrinsic(0, parameters.right);

		if (refine != null)
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
	public long getFrameID() {
		return alg.getFrameID();
	}

	@Override
	public boolean process( T leftImage, T rightImage ) {
		totalFeatures += alg.getTrackQuads().size;
		return alg.process(leftImage, rightImage);
	}

	@Override
	public boolean isFault() {
		return false;
	}

	@Override
	public ImageType<T> getImageType() {
		return ImageType.single(imageType);
	}

	@Override
	public void setVerbose( @Nullable PrintStream out, @Nullable Set<String> configuration ) {
		alg.setVerbose(out, configuration);
	}
}
