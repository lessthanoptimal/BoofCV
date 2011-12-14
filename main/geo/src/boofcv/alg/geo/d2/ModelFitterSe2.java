/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.ModelFitter;
import boofcv.numerics.optimization.LevenbergMarquardt;
import boofcv.numerics.optimization.OptimizationDerivative;
import boofcv.numerics.optimization.OptimizationFunction;
import boofcv.numerics.optimization.OptimizationResidual;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class ModelFitterSe2 implements ModelFitter<Se2_F64,AssociatedPair> {

	LevenbergMarquardt<?,AssociatedPair> alg;
	double param[] = new double[3];

	public ModelFitterSe2() {
		alg = new LevenbergMarquardt<Object, AssociatedPair>(3,new Function(),new Derivative());
	}

	@Override
	public Se2_F64 declareModel() {
		return new Se2_F64();
	}

	@Override
	public boolean fitModel(List<AssociatedPair> dataSet,
							Se2_F64 initParam,
							Se2_F64 foundModel )
	{
		param[0] = initParam.getX();
		param[1] = initParam.getY();
		param[2] = initParam.getYaw();

		if( !alg.process(param,null,dataSet) )
			return false;

		double found[] = alg.getModelParameters();

		foundModel.set(found[0],found[1],found[2]);

		return true;
	}

	@Override
	public int getMinimumPoints() {
		return 3;
	}

	protected static class Function implements OptimizationResidual<Object,AssociatedPair>
	{
		Se2_F64 m = new Se2_F64();
		Point2D_F64 p = new Point2D_F64();

		@Override
		public void setModel(double[] model) {
			m.set(model[0],model[1],model[2]);
		}

		@Override
		public int getNumberOfFunctions() {
			return 2;
		}

		@Override
		public int getModelSize() {
			return 3;
		}

		@Override
		public boolean estimate(AssociatedPair associatedPair, double[] estimated) {
			SePointOps_F64.transform(m,associatedPair.currLoc,p);
			estimated[0] = p.x;
			estimated[1] = p.y;

			return true;
		}

		@Override
		public boolean computeResiduals(Object o, AssociatedPair associatedPair, double[] residuals) {
			estimate(associatedPair,residuals);

			residuals[0] = -(associatedPair.keyLoc.x-residuals[0]);
			residuals[1] = -(associatedPair.keyLoc.y-residuals[1]);

			return true;
		}
	}

	protected static class Derivative implements OptimizationDerivative<AssociatedPair>
	{
		double c;
		double s;
		@Override
		public void setModel(double[] model) {
			double theta = model[2];
			c = Math.cos(theta);
			s = Math.sin(theta);
		}

		@Override
		public boolean computeDerivative(AssociatedPair associatedPair, double[][] gradient) {
			double x = associatedPair.currLoc.x;
			double y = associatedPair.currLoc.y;

			gradient[0][0] = 1;
			gradient[0][1] = 0;
			gradient[0][2] = -x*s - y*c;

			gradient[1][0] = 0;
			gradient[1][1] = 1;
			gradient[1][2] = x*c - y*s;

			return true;
		}
	}
}

