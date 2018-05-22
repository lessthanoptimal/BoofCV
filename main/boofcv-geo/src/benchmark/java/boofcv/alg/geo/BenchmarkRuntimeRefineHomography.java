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

package boofcv.alg.geo;

import boofcv.abst.geo.Estimate1ofEpipolar;
import boofcv.alg.geo.h.HomographyDirectLinearTransform;
import boofcv.factory.geo.EpipolarError;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.Performer;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.geo.AssociatedPair;
import org.ddogleg.fitting.modelset.ModelFitter;
import org.ejml.data.DMatrixRMaj;

import static boofcv.factory.geo.FactoryMultiView.refineHomography;

/**
 * @author Peter Abeles
 */
public class BenchmarkRuntimeRefineHomography extends ArtificialStereoScene{
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean PIXELS = false;
	
	protected DMatrixRMaj H = new DMatrixRMaj(3,3);
	DMatrixRMaj refinedF = new DMatrixRMaj(3,3);

	public class Refine implements Performer {

		ModelFitter<DMatrixRMaj,AssociatedPair> alg;
		String name;

		public Refine(String name , ModelFitter<DMatrixRMaj,AssociatedPair> alg) {
			this.name = name;
			this.alg = alg;
		}

		@Override
		public void process() {
			alg.fitModel(pairs, H, refinedF);
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public class Linear4 extends PerformerBase {

		HomographyDirectLinearTransform alg = new HomographyDirectLinearTransform(true);

		@Override
		public void process() {
			alg.process(pairs,refinedF);
		}
	}
	
	public void runAll() {
		System.out.println("=========  Profile numFeatures "+NUM_POINTS);
		System.out.println();

		double tol = 1e-16;
		int MAX_ITER = 200;

		init(NUM_POINTS, PIXELS, true);

		Estimate1ofEpipolar computeAlg = FactoryMultiView.computeHomographyDLT(true);
		computeAlg.process(pairs, H);
		H.data[0] += 0.1;
		H.data[4] -= 0.15;
		H.data[7] -= 0.2;


		ProfileOperation.printOpsPerSec(new Refine("Simple",refineHomography(tol, MAX_ITER, EpipolarError.SIMPLE)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Refine("Sampson",refineHomography(tol, MAX_ITER, EpipolarError.SAMPSON)), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Linear4(), TEST_TIME);

		
		System.out.println();
		System.out.println("Done");
	}
	
	public static void main( String args[] ) {
		BenchmarkRuntimeRefineHomography alg = new BenchmarkRuntimeRefineHomography();

		alg.runAll();
	}
}
