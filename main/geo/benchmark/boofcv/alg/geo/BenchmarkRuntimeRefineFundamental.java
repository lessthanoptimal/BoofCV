/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo;

import boofcv.abst.geo.RefineEpipolar;
import boofcv.misc.Performer;
import org.ejml.data.DenseMatrix64F;

/**
 * @author Peter Abeles
 */
public class BenchmarkRuntimeRefineFundamental extends ArtificialStereoScene{
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean FUNDAMENTAL = false;
	
	protected DenseMatrix64F initialF;

	public class Refine implements Performer {

		RefineEpipolar alg;
		String name;
		DenseMatrix64F refinement = new DenseMatrix64F(3,3);

		public Refine( String name , RefineEpipolar alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			alg.fitModel(pairs, initialF, refinement);
		}

		@Override
		public String getName() {
			return name;
		}
	}
	
	public void runAll() {
		System.out.println("=========  Profile numFeatures "+NUM_POINTS);
		System.out.println();

		double tol = 1e-16;
		int MAX_ITER = 100;

		init(NUM_POINTS,FUNDAMENTAL,false);

//		EpipolarMatrixEstimator computeAlg =
//				FUNDAMENTAL ? FactoryMultiView.computeFundamental(8) : FactoryMultiView.computeEssential(8);
//		computeAlg.process(pairs);
//		initialF = computeAlg.getEpipolarMatrix();
//		initialF.data[0] += 0.1;
//		initialF.data[4] -= 0.15;
//		initialF.data[7] -= 0.2;
//
//		ProfileOperation.printOpsPerSec(new Refine("LS Sampson",refineFundamental(tol, MAX_ITER, EpipolarError.SAMPSON)), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Refine("LS Simple",refineFundamental(tol, MAX_ITER, EpipolarError.SIMPLE)), TEST_TIME);
//		ProfileOperation.printOpsPerSec(new Refine("QN Sampson",new QuasiNewtonFundamentalSampson(tol,MAX_ITER)), TEST_TIME);
//
//		System.out.println();
//		System.out.println("Done");
	}
	
	public static void main( String args[] ) {
		BenchmarkRuntimeRefineFundamental alg = new BenchmarkRuntimeRefineFundamental();

		alg.runAll();
	}
}
