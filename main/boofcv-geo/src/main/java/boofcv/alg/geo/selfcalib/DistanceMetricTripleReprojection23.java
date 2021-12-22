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

package boofcv.alg.geo.selfcalib;

import boofcv.abst.geo.TriangulateNViewsMetric;
import boofcv.alg.distort.pinhole.PinholePtoN_F64;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import lombok.Getter;
import lombok.Setter;
import org.ddogleg.fitting.modelset.DistanceFromModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Image based reprojection error using error in view 2 and view 3. If triangulation fails or it appears behind
 * a camera then it returns the max allowed distance
 */
@SuppressWarnings({"NullAway.Init"})
public class DistanceMetricTripleReprojection23 implements DistanceFromModel<MetricCameraTriple, AssociatedTriple> {

	// the model
	protected MetricCameraTriple model;

	/** algorithm used to triangulate feature in 3D space */
	protected @Getter @Setter TriangulateNViewsMetric triangulate = FactoryMultiView.triangulateNViewMetric(null);

	// storage for normalized image coordinate
	private final Point2D_F64 norm1 = new Point2D_F64();
	private final Point2D_F64 norm2 = new Point2D_F64();
	private final Point2D_F64 norm3 = new Point2D_F64();

	// storage for predicted pixel observation
	private final Point2D_F64 pixel = new Point2D_F64();

	// triangulated feature location in view 1
	private final Point3D_F64 X = new Point3D_F64();
	// feature location in the current camera view
	private final Point3D_F64 Xcam = new Point3D_F64();

	// Precomputed pixel to norm for each view
	private final PinholePtoN_F64 pixelToNorm1 = new PinholePtoN_F64();
	private final PinholePtoN_F64 pixelToNorm2 = new PinholePtoN_F64();
	private final PinholePtoN_F64 pixelToNorm3 = new PinholePtoN_F64();

	Se3_F64 view_1_to_1 = new Se3_F64();
	List<Point2D_F64> observations = new ArrayList<>();
	List<Se3_F64> locations = new ArrayList<>();

	public DistanceMetricTripleReprojection23() {
		observations.add(norm1);
		observations.add(norm2);
		observations.add(norm3);
	}

	@Override
	public void setModel( MetricCameraTriple model ) {
		this.model = model;

		pixelToNorm1.setK(model.view1);
		pixelToNorm2.setK(model.view2);
		pixelToNorm3.setK(model.view3);

		locations.clear();
		locations.add(view_1_to_1);
		locations.add(model.view_1_to_2);
		locations.add(model.view_1_to_3);
	}

	@Override
	public double distance( AssociatedTriple obs ) {

		// normalized image coordinates for each view
		pixelToNorm1.compute(obs.p1.x, obs.p1.y, norm1);
		pixelToNorm2.compute(obs.p2.x, obs.p2.y, norm2);
		pixelToNorm3.compute(obs.p3.x, obs.p3.y, norm3);

		// Find the feature's location and compute the reprojection error in view 3. If behind camera treat that
		// as a failure since it couldn't be possibly seen
		if (!triangulate.triangulate(observations, locations, X) || X.z < 0)
			return Double.MAX_VALUE;

		// Error in view-3
		SePointOps_F64.transform(model.view_1_to_3, X, Xcam);
		if (Xcam.z < 0)
			return Double.MAX_VALUE;
		PerspectiveOps.renderPixel(model.view3, Xcam, pixel);
		double error = pixel.distance2(obs.p3);

		// Error in view-2
		SePointOps_F64.transform(model.view_1_to_2, X, Xcam);
		if (X.z < 0)
			return Double.MAX_VALUE;
		PerspectiveOps.renderPixel(model.view2, Xcam, pixel);
		return error + pixel.distance2(obs.p2);
	}

	@Override
	public void distances( List<AssociatedTriple> observations, double[] distance ) {
		for (int i = 0; i < observations.size(); i++) {
			distance[i] = distance(observations.get(i));
		}
	}

	@Override
	public Class<AssociatedTriple> getPointType() {
		return AssociatedTriple.class;
	}

	@Override
	public Class<MetricCameraTriple> getModelType() {
		return MetricCameraTriple.class;
	}
}
