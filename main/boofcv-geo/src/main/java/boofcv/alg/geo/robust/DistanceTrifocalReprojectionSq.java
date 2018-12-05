/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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
import boofcv.alg.geo.trifocal.TrifocalExtractGeometries;
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
 * view and the delta computed.
 *
 * @author Peter Abeles
 */
public class DistanceTrifocalReprojectionSq implements DistanceFromModel<TrifocalTensor, AssociatedTriple>
{
	DMatrixRMaj P1 = CommonOps_DDRM.identity(3,4);
	DMatrixRMaj P2 = new DMatrixRMaj(3,4);
	DMatrixRMaj P3 = new DMatrixRMaj(3,4);

	List<DMatrixRMaj> cameraMatrices = new ArrayList<>();
	List<Point2D_F64> observations = new ArrayList<>();

	TrifocalExtractGeometries extractor = new TrifocalExtractGeometries();
	TriangulateNViewsProjective triangulator = FactoryMultiView.triangulateNViewDLT();


	Point4D_F64 X = new Point4D_F64();
	Point2D_F64 pixel = new Point2D_F64();

	public DistanceTrifocalReprojectionSq() {

		cameraMatrices.add(P1);
		cameraMatrices.add(P2);
		cameraMatrices.add(P3);

		observations.add(null);
		observations.add(null);
		observations.add(null);
	}

	@Override
	public void setModel(TrifocalTensor trifocalTensor) {
		extractor.setTensor(trifocalTensor);
		extractor.extractCamera(P2, P3);
	}

	@Override
	public double computeDistance(AssociatedTriple pt) {
		observations.set(0,pt.p1);
		observations.set(1,pt.p2);
		observations.set(2,pt.p3);

		triangulator.triangulate(observations,cameraMatrices,X);

		// TODO optional refinement of X

		double error = 0;
		GeometryMath_F64.mult(P1,X,pixel);
		error += pixel.distance2(pt.p1);
		GeometryMath_F64.mult(P2,X,pixel);
		error += pixel.distance2(pt.p1);
		GeometryMath_F64.mult(P2,X,pixel);
		error += pixel.distance2(pt.p1);

		return error;
	}

	@Override
	public void computeDistance(List<AssociatedTriple> observations, double[] distance) {
		for (int i = 0; i < observations.size(); i++) {
			distance[i] = computeDistance(observations.get(i));
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
