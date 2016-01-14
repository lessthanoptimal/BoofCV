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

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.factory.geo.EnumPNP;
import boofcv.factory.geo.FactoryMultiView;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkStabilityPnP extends ArtificialStereoScene {

	static final int NUM_POINTS = 500;

	Estimate1ofPnP target;
	Se3_F64 found = new Se3_F64();
	String name;

	public void evaluateObservationNoise( double minPixelError , double maxPixelError , int N , boolean isPlanar )
	{
		System.out.println("------------------------");
		
		rand = new Random(234);

		for( int i = 0; i <= N; i++ ) {
			double mag = (maxPixelError-minPixelError)*i/N+minPixelError;
			
			init(NUM_POINTS,false,isPlanar);
			addPixelNoise(mag);
			
			if( !target.process(observationPose,found) )
				throw new RuntimeException("Not expected to fail");
			
			double expectedEuler[] = ConvertRotation3D_F64.matrixToEuler(motion.getR(),EulerType.XYZ,(double[])null);
			double foundEuler[] = ConvertRotation3D_F64.matrixToEuler(found.getR(),EulerType.XYZ,(double[])null);

			Vector3D_F64 expectedTran = motion.getT();
			Vector3D_F64 foundTran = found.getT();

			double errorTran = expectedTran.distance(foundTran);
			double errorEuler = 0;
			double sum = 0;
			for( int j = 0; j < 3; j++ ) {
				double e = expectedEuler[j]-foundEuler[j];
				errorEuler += e*e;
				sum += expectedEuler[j];
			}
			errorEuler = 100*Math.sqrt(errorEuler)/Math.sqrt(sum);

			System.out.printf("%3d angle %6.2f%% translation %6.2e\n", i,errorEuler, errorTran);
		}
	}

	public void evaluateAllMinimal() {
		double pixelSigma = 0.5;
		boolean isPlanar = false;
		int numTrials = 1000;
		int numTestPoints = 1;

		target = FactoryMultiView.computePnPwithEPnP(10, 0.1);
		name = "EPnP";
		evaluateMinimal(pixelSigma,isPlanar,numTrials);

		target = FactoryMultiView.computePnP_1(EnumPNP.P3P_GRUNERT, -1, numTestPoints);
		name = "Grunert";
		evaluateMinimal(pixelSigma, isPlanar, numTrials);

		target = FactoryMultiView.computePnP_1(EnumPNP.P3P_FINSTERWALDER,-1,numTestPoints);
		name = "Finsterwalder";
		evaluateMinimal(pixelSigma,isPlanar,numTrials);
	}

	public void evaluateMinimal( double pixelSigma , boolean isPlanar , int numTrials ) {

		// make sure each test case has the same random seed
		rand = new Random(234);

		double totalEuler = 0;
		double totalTran = 0;
		int totalFail = 0;

		for( int i = 0; i < numTrials; i++ )  {
			init(target.getMinimumPoints(),false,isPlanar);
			addPixelNoise(pixelSigma);

			if( !target.process(observationPose,found) ) {
				totalFail++;
				continue;
			}

			double expectedEuler[] = ConvertRotation3D_F64.matrixToEuler(motion.getR(),EulerType.XYZ,(double[])null);
			double foundEuler[] = ConvertRotation3D_F64.matrixToEuler(found.getR(),EulerType.XYZ,(double[])null);

			Vector3D_F64 expectedTran = motion.getT();
			Vector3D_F64 foundTran = found.getT();

			double errorTran = expectedTran.distance(foundTran);
			double distanceTrue = expectedTran.norm();

			double errorEuler = 0;
			double sum = 0;
			for( int j = 0; j < 3; j++ ) {
				double e = expectedEuler[j]-foundEuler[j];
				errorEuler += e*e;
				sum += expectedEuler[j];
			}
			errorEuler = 100.0*Math.sqrt(errorEuler)/Math.sqrt(sum);
			errorTran = 100.0*(errorTran/distanceTrue);

//			System.out.println(errorTran);

			totalEuler += errorEuler;
			totalTran += errorTran;
		}

		int N = numTrials-totalFail;

		System.out.printf("%20s N = %d failed %.3f%% euler = %3f%% tran = %3f%%\n", name, target.getMinimumPoints(),
				(totalFail/(double)numTrials), (totalEuler / N), (totalTran / N));

	}
	
	public static void main( String args[] ) {
		double max = 15;
		boolean planar = true;

		BenchmarkStabilityPnP app = new BenchmarkStabilityPnP();

		app.evaluateAllMinimal();

//		app.target = FactoryMultiView.computePnPwithEPnP(0, 1);
//		app.evaluateObservationNoise(0,max,20,planar);
//		app.target = FactoryMultiView.computePnPwithEPnP(10, 0.1);
//		app.evaluateObservationNoise(0,max,20,planar);
	}
}
