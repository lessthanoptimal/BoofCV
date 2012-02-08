/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d2;

import boofcv.alg.geo.AssociatedPair;
import boofcv.numerics.fitting.modelset.ModelFitter;
import boofcv.numerics.optimization.FactoryOptimization;
import boofcv.numerics.optimization.UnconstrainedLeastSquares;
import boofcv.numerics.optimization.functions.FunctionNtoM;
import boofcv.numerics.optimization.functions.FunctionNtoMxN;
import boofcv.numerics.optimization.impl.UtilOptimize;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;

import java.util.List;

/**
 * @author Peter Abeles
 */
public class ModelFitterSe2 implements ModelFitter<Se2_F64,AssociatedPair> {

	UnconstrainedLeastSquares ls = FactoryOptimization.leastSquaresLM(1e-8,1e-8,1e-3,false);

	List<AssociatedPair> pairs;
	
	FunctionNtoM function;
	FunctionNtoMxN jacobian;
	
	double param[] = new double[3];

	public ModelFitterSe2() {
		ls = FactoryOptimization.leastSquaresLM(1e-8,1e-8,1e-3,false);
		function = new F();
		jacobian = new D();
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
		this.pairs = dataSet;
		param[0] = initParam.getX();
		param[1] = initParam.getY();
		param[2] = initParam.getYaw();

		ls.setFunction(function,jacobian);
		ls.initialize(param);

		if(UtilOptimize.process(ls, 100) ) {
			double found[] = ls.getParameters();

			foundModel.set(found[0],found[1],found[2]);

			return true;
		} else {
			return false;
		}
	}

	@Override
	public int getMinimumPoints() {
		return 3;
	}

	protected class F implements FunctionNtoM
	{
		Se2_F64 m = new Se2_F64();
		Point2D_F64 p = new Point2D_F64();

		@Override
		public int getN() {
			return 3;
		}

		@Override
		public int getM() {
			return pairs.size()*2;
		}

		@Override
		public void process(double[] model, double[] output) {
			m.set(model[0],model[1],model[2]);
			int i = 0;
			for( AssociatedPair pair : pairs ) {
				SePointOps_F64.transform(m,pair.currLoc,p);

				output[i++] = p.x - pair.keyLoc.x;
				output[i++] = p.y - pair.keyLoc.y;
			}
		}
	}

	protected class D implements FunctionNtoMxN
	{
		@Override
		public int getN() {
			return 3;
		}

		@Override
		public int getM() {
			return pairs.size()*2;
		}

		@Override
		public void process(double[] model, double[] output) {
			double c = Math.cos(model[2]);
			double s = Math.sin(model[2]);
			
			int i = 0;
			for( AssociatedPair pair : pairs ) {
				Point2D_F64 p = pair.currLoc;

				output[i++] = 1;
				output[i++] = 0;
				output[i++] = -p.x*s - p.y*c;

				output[i++] = 0;
				output[i++] = 1;
				output[i++] = p.x*c - p.y*s;
			}
		}
	}
}

