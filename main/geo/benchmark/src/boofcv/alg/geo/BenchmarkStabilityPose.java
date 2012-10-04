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

import boofcv.abst.geo.Estimate1ofPnP;
import boofcv.factory.geo.FactoryMultiView;
import boofcv.struct.geo.PointPosePair;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkStabilityPose extends ArtificialStereoScene {

	static final int NUM_POINTS = 500;

	Estimate1ofPnP target;
	Se3_F64 found = new Se3_F64();
	
	public void evaluateObservationNoise( double min , double max , int N , boolean isPlanar )
	{
		System.out.println("------------------------");
		
		rand = new Random(234);
		List<PointPosePair> inputs = new ArrayList<PointPosePair>();
		
		for( int i = 0; i <= N; i++ ) {
			inputs.clear();
			double mag = (max-min)*i/N+min;
			
			init(NUM_POINTS,false,isPlanar);
			addPixelNoise(mag);
			
			for( int j = 0; j < NUM_POINTS; j++ ) {
				inputs.add(new PointPosePair(pairs.get(j).currLoc, worldPoints.get(j)));
			}
			
			if( !target.process(inputs,found) )
				throw new RuntimeException("Not expection it to fail");
			
			double expectedEuler[] = RotationMatrixGenerator.matrixToEulerXYZ(motion.getR());
			double foundEuler[] = RotationMatrixGenerator.matrixToEulerXYZ(found.getR());

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
	
	public static void main( String args[] ) {
		double max = 15;
		boolean planar = false;

		BenchmarkStabilityPose app = new BenchmarkStabilityPose();
		app.target = FactoryMultiView.computePnPwithEPnP(0, 1);
		app.evaluateObservationNoise(0,max,20,planar);
		app.target = FactoryMultiView.computePnPwithEPnP(10, 0.1);
		app.evaluateObservationNoise(0,max,20,planar);

	}
}
