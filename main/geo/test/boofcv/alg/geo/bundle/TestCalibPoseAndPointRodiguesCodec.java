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

package boofcv.alg.geo.bundle;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.ops.MatrixFeatures;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestCalibPoseAndPointRodiguesCodec {

	Random rand = new Random(123);

	/**
	 * Test by endcoding data, then decoding the same data
	 */
	@Test
	public void encode_decode() {
		checkEncode_Decode(2,3,false,false);
	}

	/**
	 * Have one of the views be known, thus its parameters are not encoded
	 */
	@Test
	public void handleKnownViews() {
		checkEncode_Decode(2,3,true,false);
	}

	public void checkEncode_Decode( int numViews , int numPoints , boolean ...known ) {
		// randomly configure the model
		CalibratedPoseAndPoint model = new CalibratedPoseAndPoint();

		configure(model,numViews,numPoints,rand);

		int numUnknown = 0;
		for( int i = 0; i < known.length; i++ ) {
			if( known[i])
				model.setViewKnown(i,true);
			numUnknown++;
		}

		// encode the model
		CalibPoseAndPointRodriguesCodec codec = new CalibPoseAndPointRodriguesCodec();
		codec.configure(numViews,numPoints,numViews,known);
		
		double param[] = new double[ codec.getParamLength() ];

		// make sure there are the correct number of parameters
		assertEquals(numUnknown*6+numPoints*3,codec.getParamLength());
		
		codec.encode(model,param);

		// decode the model
		CalibratedPoseAndPoint found = new CalibratedPoseAndPoint();
		found.configure(numViews,numPoints);

		codec.decode(param,found);
		
		// compare results
		for( int i = 0; i < numViews; i++ ) {
			if( known[i])
				continue;
			
			Se3_F64 o = model.getWorldToCamera(i);
			Se3_F64 f = found.getWorldToCamera(i);
			
			assertEquals(0,o.getT().distance(f.getT()),1e-8);
			assertTrue(MatrixFeatures.isIdentical(o.getR(),f.getR(),1e-8));
		}

		for( int i = 0; i < numPoints; i++ ) {
			Point3D_F64 o = model.getPoint(i);
			Point3D_F64 f = found.getPoint(i);

			assertEquals(0,o.distance(f),1e-8);
		}
	}

	/**
	 * A pathological situation for Rodigues coordinates is zero degrees
	 */
	@Test
	public void checkZeroAngle() {
		int numViews = 1;
		int numPoints = 0;

		// Set a world transform of zero degrees
		CalibratedPoseAndPoint model = new CalibratedPoseAndPoint();
		model.configure(numViews,numPoints);
		model.getWorldToCamera(0).getT().set(1,2,3);
		
		// encode the model
		CalibPoseAndPointRodriguesCodec codec = new CalibPoseAndPointRodriguesCodec();
		codec.configure(numViews,numPoints,numViews, new boolean[]{false});

		double param[] = new double[ codec.getParamLength() ];

		codec.encode(model,param);

		// decode the model
		CalibratedPoseAndPoint found = new CalibratedPoseAndPoint();
		found.configure(numViews,numPoints);

		codec.decode(param,found);

		// compare results
		for( int i = 0; i < numViews; i++ ) {
			Se3_F64 o = model.getWorldToCamera(i);
			Se3_F64 f = found.getWorldToCamera(i);

			assertEquals(0,o.getT().distance(f.getT()),1e-8);
			assertTrue(MatrixFeatures.isIdentical(o.getR(),f.getR(),1e-8));
		}
	}
	
	public static void configure(CalibratedPoseAndPoint model ,
								 int numViews , int numPoints , Random rand ) {
		model.configure(numViews,numPoints);

		for( int i = 0; i < numViews; i++ ) {
			setPose(model.getWorldToCamera(i),rand);
		}

		for( int i = 0; i < numPoints; i++ ) {
			model.getPoint(i).set(rand.nextGaussian(),rand.nextGaussian(),rand.nextGaussian());
		}
	}
	
	private static void setPose( Se3_F64 pose , Random rand )  {
		
		double rotX = 2*(rand.nextDouble()-0.5);
		double rotY = 2*(rand.nextDouble()-0.5);
		double rotZ = 2*(rand.nextDouble()-0.5);

		double x = rand.nextGaussian();
		double y = rand.nextGaussian();
		double z = rand.nextGaussian();

		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,pose.getR());
		pose.getT().set(x,y,z);
	}
}
