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

package boofcv.alg.geo.epipolar;

import boofcv.abst.geo.epipolar.EpipolarMatrixEstimator;
import boofcv.abst.geo.epipolar.RefineEpipolarMatrix;
import boofcv.factory.geo.d3.epipolar.EpipolarError;
import boofcv.factory.geo.d3.epipolar.FactoryEpipolar;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import org.ejml.data.DenseMatrix64F;

import static boofcv.factory.geo.d3.epipolar.FactoryEpipolar.refineFundamental;

/**
 * @author Peter Abeles
 */
public class BenchmarkRefineFundamental extends ArtificialStereoScene{
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean FUNDAMENTAL = false;
	
	protected DenseMatrix64F initialF;

	public class Refine implements Performer {

		RefineEpipolarMatrix alg;
		String name;

		public Refine( String name , RefineEpipolarMatrix alg) {
			this.alg = alg;
			this.name = name;
		}

		@Override
		public void process() {
			alg.process(initialF,pairs);
			alg.getRefinement();
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

		EpipolarMatrixEstimator computeAlg = FactoryEpipolar.computeFundamental(FUNDAMENTAL,8);
		computeAlg.process(pairs);
		initialF = computeAlg.getEpipolarMatrix();

		ProfileOperation.printOpsPerSec(new Refine("LS Sampson",refineFundamental(tol, MAX_ITER, EpipolarError.SAMPSON)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Refine("LS Simple",refineFundamental(tol, MAX_ITER, EpipolarError.SIMPLE)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Refine("QN Sampson",new QuasiNewtonFundamentalSampson(tol,MAX_ITER)), TEST_TIME);

		System.out.println();
		System.out.println("Done");
	}
	
	public static void main( String args[] ) {
		BenchmarkRefineFundamental alg = new BenchmarkRefineFundamental();

		alg.runAll();
	}
}
