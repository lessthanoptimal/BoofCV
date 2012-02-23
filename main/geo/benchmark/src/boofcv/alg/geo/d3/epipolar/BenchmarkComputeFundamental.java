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

package boofcv.alg.geo.d3.epipolar;

import boofcv.abst.geo.epipolar.EpipolarMatrixEstimator;
import boofcv.abst.geo.epipolar.RefineEpipolarMatrix;
import boofcv.alg.geo.AssociatedPair;
import boofcv.factory.geo.d3.epipolar.FactoryEpipolar;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BenchmarkComputeFundamental extends ArtificialStereoScene{
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean FUNDAMENTAL = false;
	
	protected DenseMatrix64F initialF;

	List<AssociatedPair> pairs8 = new ArrayList<AssociatedPair>();
	List<AssociatedPair> pairs7 = new ArrayList<AssociatedPair>();


	public class Estimate implements Performer {

		EpipolarMatrixEstimator alg;
		String name;
		List<AssociatedPair> list;
		
		public Estimate( String name , EpipolarMatrixEstimator alg , List<AssociatedPair> list ) {
			this.alg = alg;
			this.name = name;
			this.list = list;
		}

		@Override
		public void process() {
			alg.process(list);
			alg.getEpipolarMatrix();
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public class Refine implements Performer {

		RefineEpipolarMatrix alg;

		public Refine(RefineEpipolarMatrix alg) {
			this.alg = alg;
		}

		@Override
		public void process() {
			alg.process(initialF,pairs);
			alg.getRefinement();
		}

		@Override
		public String getName() {
			return alg.getClass().getSimpleName();
		}
	}
	
	public void runAll() {
		System.out.println("=========  Profile numFeatures "+NUM_POINTS);
		System.out.println();

		init(NUM_POINTS,FUNDAMENTAL,false);
		
		for( int i = 0; i < 7; i++ ) {
			pairs7.add(pairs.get(i));
			pairs8.add(pairs.get(i));
		}
		pairs8.add(pairs.get(7));

		System.out.println("Minimum Number");
		ProfileOperation.printOpsPerSec(new Estimate("Linear 8",FactoryEpipolar.computeFundamental(FUNDAMENTAL,8),pairs8), TEST_TIME);
		ProfileOperation.printOpsPerSec(new Estimate("Linear 7",FactoryEpipolar.computeFundamental(FUNDAMENTAL,7),pairs7), TEST_TIME);

		System.out.println("N");
		ProfileOperation.printOpsPerSec(new Estimate("Linear 8",FactoryEpipolar.computeFundamental(FUNDAMENTAL,8),pairs), TEST_TIME);

	}
	
	public static void main( String args[] ) {
		BenchmarkComputeFundamental alg = new BenchmarkComputeFundamental();

		alg.runAll();
	}
}
