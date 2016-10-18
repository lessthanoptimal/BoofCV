/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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
import boofcv.factory.geo.EnumEpipolar;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.misc.Performer;
import boofcv.misc.ProfileOperation;
import boofcv.struct.geo.AssociatedPair;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BenchmarkRuntimeFundamental extends ArtificialStereoScene{
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean FUNDAMENTAL = false;

	List<AssociatedPair> pairs8 = new ArrayList<>();
	List<AssociatedPair> pairs7 = new ArrayList<>();
	List<AssociatedPair> pairs6 = new ArrayList<>();
	List<AssociatedPair> pairs5 = new ArrayList<>();

	DenseMatrix64F found = new DenseMatrix64F(3,3);

	public class Estimate implements Performer {

		Estimate1ofEpipolar alg;
		String name;
		List<AssociatedPair> list;
		
		public Estimate( String name ,
						 Estimate1ofEpipolar alg ,
						 List<AssociatedPair> list )
		{
			this.alg = alg;
			this.name = name;
			this.list = list;
		}

		@Override
		public void process() {
			alg.process(list, found);
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
		for( int i = 0; i < 6; i++ )
			pairs6.add(pairs.get(i));

		for( int i = 0; i < 7; i++ ) {
			pairs7.add(pairs.get(i));
			pairs8.add(pairs.get(i));
		}
		pairs8.add(pairs.get(7));

		System.out.println("Minimum Number");
		if( FUNDAMENTAL ) {
			ProfileOperation.printOpsPerSec(new Estimate("Linear 8", FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_8_LINEAR, 0),pairs8), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Estimate("Linear 7", FactoryMultiView.computeFundamental_1(EnumEpipolar.FUNDAMENTAL_7_LINEAR, 1),pairs8), TEST_TIME);
		} else {
			ProfileOperation.printOpsPerSec(new Estimate("Linear 8", FactoryMultiView.computeFundamental_1(EnumEpipolar.ESSENTIAL_8_LINEAR, 0),pairs8), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Estimate("Linear 7", FactoryMultiView.computeFundamental_1(EnumEpipolar.ESSENTIAL_7_LINEAR, 1),pairs8), TEST_TIME);
			ProfileOperation.printOpsPerSec(new Estimate("Linear 5", FactoryMultiView.computeFundamental_1(EnumEpipolar.ESSENTIAL_5_NISTER, 1),pairs6), TEST_TIME);
		}

	}
	
	public static void main( String args[] ) {
		BenchmarkRuntimeFundamental alg = new BenchmarkRuntimeFundamental();

		alg.runAll();
	}
}
