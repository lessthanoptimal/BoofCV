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

import boofcv.alg.geo.epipolar.pose.PnPLepetitEPnP;
import boofcv.alg.geo.epipolar.pose.PoseFromPairLinear6;
import boofcv.alg.geo.epipolar.pose.RefineLepetitEPnP;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import org.ejml.data.DenseMatrix64F;

/**
 * @author Peter Abeles
 */
public class BenchmarkComputePose extends ArtificialStereoScene{
	static final long TEST_TIME = 1000;
	static final int NUM_POINTS = 500;
	static final boolean FUNDAMENTAL = false;
	
	protected DenseMatrix64F initialF;

	public class EPnP extends PerformerBase {

		PnPLepetitEPnP alg = new PnPLepetitEPnP();

		@Override
		public void process() {
			alg.process(worldPoints,observationCurrent);
		}
	}

	public class EPnPRefine extends PerformerBase {

		PnPLepetitEPnP alg = new PnPLepetitEPnP();
		RefineLepetitEPnP refine = new RefineLepetitEPnP(alg,1e-12,200);

		@Override
		public void process() {
			alg.process(worldPoints,observationCurrent);
			refine.refine();
		}
	}

	public class PairLinear extends PerformerBase {

		PoseFromPairLinear6 alg = new PoseFromPairLinear6();

		@Override
		public void process() {
			alg.process(pairs,worldPoints);
		}
	}
	
	public void runAll() {
		System.out.println("=========  Profile numFeatures "+NUM_POINTS);
		System.out.println();

		init(NUM_POINTS,FUNDAMENTAL,false);

		ProfileOperation.printOpsPerSec(new EPnP(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new EPnPRefine(), TEST_TIME);
		ProfileOperation.printOpsPerSec(new PairLinear(), TEST_TIME);

		System.out.println();
		System.out.println("Done");
	}
	
	public static void main( String args[] ) {
		BenchmarkComputePose alg = new BenchmarkComputePose();

		alg.runAll();
	}
}
