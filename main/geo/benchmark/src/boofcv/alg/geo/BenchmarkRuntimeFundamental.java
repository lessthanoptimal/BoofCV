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

package boofcv.alg.geo;

import boofcv.abst.geo.EpipolarMatrixEstimator;
import boofcv.factory.geo.FactoryEpipolar;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BenchmarkRuntimeFundamental extends ArtificialStereoScene{
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean FUNDAMENTAL = false;

	List<AssociatedPair> pairs8 = new ArrayList<AssociatedPair>();
	List<AssociatedPair> pairs7 = new ArrayList<AssociatedPair>();
	List<AssociatedPair> pairs5 = new ArrayList<AssociatedPair>();

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
	
	public void runAll() {
		System.out.println("=========  Profile numFeatures "+NUM_POINTS);
		System.out.println();

		init(NUM_POINTS,FUNDAMENTAL,false);

		for( int i = 0; i < 5; i++ )
			pairs5.add(pairs.get(i));

		for( int i = 0; i < 7; i++ ) {
			pairs7.add(pairs.get(i));
			pairs8.add(pairs.get(i));
		}
		pairs8.add(pairs.get(7));

		System.out.println("Minimum Number");
		if( FUNDAMENTAL ) {
			ProfileOperation.printOpsPerSec(new Estimate("Linear 8",FactoryEpipolar.computeFundamental(8),pairs8), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Estimate("Linear 7",FactoryEpipolar.computeFundamental(7),pairs7), TEST_TIME);

			System.out.println("N");
			ProfileOperation.printOpsPerSec(new Estimate("Linear 8",FactoryEpipolar.computeFundamental(8),pairs), TEST_TIME);
		} else {
			ProfileOperation.printOpsPerSec(new Estimate("Linear 8",FactoryEpipolar.computeEssential(8),pairs8), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Estimate("Linear 7",FactoryEpipolar.computeEssential(7),pairs7), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Estimate("Poly   5",FactoryEpipolar.computeEssential(5),pairs5), TEST_TIME);

			System.out.println("N");
			ProfileOperation.printOpsPerSec(new Estimate("Linear 8",FactoryEpipolar.computeEssential(8),pairs), TEST_TIME);
		}

	}
	
	public static void main( String args[] ) {
		BenchmarkRuntimeFundamental alg = new BenchmarkRuntimeFundamental();

		alg.runAll();
	}
}
