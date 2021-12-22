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

package boofcv.alg.geo.robust;

import boofcv.abst.geo.TriangulateNViewsProjective;
import boofcv.abst.geo.triangulate.TriangulateRefineProjectiveLS;
import boofcv.alg.geo.trifocal.TrifocalExtractGeometries;
import boofcv.factory.geo.ConfigTriangulation;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.AssociatedTriple;
import boofcv.struct.geo.TrifocalTensor;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point4D_F64;
import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates the accuracy of a trifocal tensor using reprojection error. The camera matrices are extracted from
 * the tensor, these are used to triangulate the observation. the found point is then reprojected back to each
 * view and the delta computed. Optional non-linear refinement.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class DistanceTrifocalReprojectionSq implements DistanceFromModel<TrifocalTensor, AssociatedTriple> {
	DMatrixRMaj P1 = CommonOps_DDRM.identity(3, 4);
	DMatrixRMaj P2 = new DMatrixRMaj(3, 4);
	DMatrixRMaj P3 = new DMatrixRMaj(3, 4);

	List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
	List<Point2D_F64> observations = new ArrayList<>();

	TrifocalExtractGeometries extractor = new TrifocalExtractGeometries();
	TriangulateNViewsProjective triangulator = FactoryMultiView.triangulateNViewProj(ConfigTriangulation.DLT());

	TriangulateRefineProjectiveLS refiner;

	Point4D_F64 X = new Point4D_F64();
	Point2D_F64 pixel = new Point2D_F64();

	/**
	 * Call this constructor if you wish to apply non-linear refinement.
	 *
	 * @param gtol convergence tolerance. Try 1e-8
	 * @param maxIterations Max iterations. Try 50
	 */
	public DistanceTrifocalReprojectionSq( double gtol, int maxIterations ) {
		this();
		refiner = new TriangulateRefineProjectiveLS(gtol, maxIterations);
	}

	public DistanceTrifocalReprojectionSq() {
		cameraMatrices.add(P1);
		cameraMatrices.add(P2);
		cameraMatrices.add(P3);

		observations.add(null);
		observations.add(null);
		observations.add(null);
	}

	@Override
	public void setModel( TrifocalTensor trifocalTensor ) {
		extractor.setTensor(trifocalTensor);
		extractor.extractCamera(P2, P3);
	}

	@Override
	public double distance( AssociatedTriple pt ) {
		observations.set(0, pt.p1);
		observations.set(1, pt.p2);
		observations.set(2, pt.p3);

		if (!triangulator.triangulate(observations, cameraMatrices, X))
			return 1e200; // not returning max value out of fear of overflow

		if (refiner != null)
			refiner.process(observations, cameraMatrices, X, X);

		double error = 0;
		GeometryMath_F64.mult(P1, X, pixel);
		error += pixel.distance2(pt.p1);
		GeometryMath_F64.mult(P2, X, pixel);
		error += pixel.distance2(pt.p2);
		GeometryMath_F64.mult(P3, X, pixel);
		error += pixel.distance2(pt.p3);

		return error;
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
	public Class<TrifocalTensor> getModelType() {
		return TrifocalTensor.class;
	}
}
