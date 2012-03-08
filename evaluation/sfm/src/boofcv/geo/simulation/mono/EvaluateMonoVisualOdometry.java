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

package boofcv.geo.simulation.mono;

import boofcv.abst.feature.tracker.ImagePointTracker;
import boofcv.abst.sfm.MonocularVisualOdometry;
import boofcv.alg.distort.LeftToRightHanded_F64;
import boofcv.alg.distort.RemoveRadialPtoN_F64;
import boofcv.geo.simulation.CameraControl;
import boofcv.geo.simulation.CameraModel;
import boofcv.geo.simulation.EnvironmentModel;
import boofcv.geo.simulation.SimulationEngine;
import boofcv.geo.simulation.impl.BasicEnvironment;
import boofcv.geo.simulation.impl.DistortedPinholeCamera;
import boofcv.geo.simulation.impl.SimulatedTracker;
import boofcv.gui.image.ShowImages;
import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.metric.UtilAngle;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Peter Abeles
 */
// todo render output in a GUI
// TODO move into a separate package?
public abstract class EvaluateMonoVisualOdometry {

	int targetTracks = 300;

	Random rand;
	long seed;
	
	MonocularVisualOdometry<?> alg;
	SimulationEngine sim;
	ImagePointTracker tracker;
	CameraModel camera;
	
	int numFaults;
	int numExceptions;
	List<MonoMetrics> errors = new ArrayList<MonoMetrics>();
	List<Double> processingTime = new ArrayList<Double>();

	boolean hasPrevious;
	Se3_F64 prevTruth = new Se3_F64();
	Se3_F64 prevFound = new Se3_F64();
	Se3_F64 changeTruth = new Se3_F64();
	Se3_F64 changeFound = new Se3_F64();

	boolean visualize;
	MonoSimulationPanel gui;

	protected EvaluateMonoVisualOdometry( boolean visualize  ) {
		this.visualize = visualize;
		
	}

	public abstract MonocularVisualOdometry<?> createAlg( ImagePointTracker<?> tracker ,
														  PointTransform_F64 pixelToNormalized );

	public void setup( int imageWidth , int imageHeight ,
					   DenseMatrix64F K , double sigmaPixel ,
					   CameraControl control , long seed )
	{
		rand = new Random(seed);
		this.seed = seed;
		
		// define the simulator
		camera = new DistortedPinholeCamera(rand,K,null,imageWidth,imageHeight,true,sigmaPixel);
		EnvironmentModel environment = new BasicEnvironment(rand,20,1,2);
		control.setCamera(camera);

		sim = new SimulationEngine(environment);
		sim.addCamera(camera,control);
		
		// create the algorithm
		RemoveRadialPtoN_F64 p2n = new RemoveRadialPtoN_F64();
		p2n.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		tracker = new SimulatedTracker(environment,camera,targetTracks);
		alg = createAlg(tracker,new LeftToRightHanded_F64(p2n));

		if( visualize ) {
			gui = new MonoSimulationPanel(imageWidth,imageHeight);
			ShowImages.showWindow(gui,"Features");
		}

		// reset error statistics
		hasPrevious = false;
		numFaults = 0;
		numExceptions = 0;
		errors.clear();
		processingTime.clear();
	}


	public boolean step() {
		// update the simulator and pose estimate
		boolean worked;
		sim.step();
		long deltaTime = 0;
		try {
			long before = System.nanoTime();
			worked = alg.process(null);
			deltaTime = System.nanoTime()-before;
		} catch( Exception e ) {
			e.printStackTrace();
			System.out.println("Random Seed = "+seed);
			numExceptions = 1;
			return false;
		}
		sim.maintenance();

		processingTime.add( deltaTime / 1e9 );
		
		if( worked ) {
			Se3_F64 worldToCamera = camera.getWorldToCamera();
			Se3_F64 cameraToWorld = worldToCamera.invert(null);

			Se3_F64 foundCtoW = alg.getCameraToWorld();
			Se3_F64 foundWtoC = foundCtoW.invert(null);

			if( hasPrevious ) {
				foundCtoW.concat(prevFound,changeFound);
				cameraToWorld.concat(prevTruth,changeTruth);

				double normFound = changeFound.getT().norm();
				double normTrue = changeTruth.getT().norm();
				
				double ratio = normTrue/normFound;
				
				double errorRotation = errorRotation( changeFound.getR() , changeTruth.getR() );

				Vector3D_F64 vF = changeFound.getT().times(ratio);
				double errorTranslation = vF.distance(changeTruth.getT());
			
				errors.add( new MonoMetrics(errorRotation,ratio,errorTranslation));
			}
			prevFound.set(foundWtoC);
			prevTruth.set(worldToCamera);
			hasPrevious = true;
		} else if( alg.isFatal() ) {
			if( hasPrevious )
				numFaults++;
			
			hasPrevious = false;
		} else if( !hasPrevious ) {
			// observations are being collected, but no update has been made
			// when updated the motion will be relative to the first frame it collected
			// data at
			Se3_F64 worldToCamera = camera.getWorldToCamera();

			Se3_F64 foundCtoW = alg.getCameraToWorld();
			Se3_F64 foundWtoC = foundCtoW.invert(null);

			prevFound.set(foundWtoC);
			prevTruth.set(worldToCamera);
			hasPrevious = true;
		}
		
		Vector3D_F64 T = alg.getCameraToWorld().getT();

		double euler[] = RotationMatrixGenerator.matrixToEulerXYZ(alg.getCameraToWorld().getR());
		
		double angle = Math.sqrt( euler[0]*euler[0] + euler[1]*euler[1] + euler[2]*euler[2] );
		
		double dist = T.norm();

		if( gui != null ) {
			gui.setFeatures(tracker.getActiveTracks());
			gui.repaint();
		}
		
		System.out.println(" angle = "+angle+"  dist "+dist);
		
		// update position score
		if( !worked && alg.isFatal() ) {
			numFaults++;
		}
		return  true;
	}

	private double errorRotation( DenseMatrix64F A , DenseMatrix64F B ) {

		double eulerA[] = RotationMatrixGenerator.matrixToEulerXYZ(A);
		double eulerB[] = RotationMatrixGenerator.matrixToEulerXYZ(B);

		// this will not handle all the equivalent cases correctly.
		double errorX = UtilAngle.dist(eulerA[0],eulerB[0]);
		double errorY = UtilAngle.dist(eulerA[1],eulerB[1]);
		double errorZ = UtilAngle.dist(eulerA[2],eulerB[2]);

		return errorX+errorY+errorZ;
	}

	private void computeTimeMetrics(MonoTrialResults results) {
		
		double sum = 0;
		for( double d : processingTime ) {
			sum += d;
		}
		results.secondsPerFrame = sum/processingTime.size();
	}
	
	private void computeScaleDrift(MonoTrialResults results) {
		double aveDriftMag = 0;
		for( int i = 1; i < errors.size(); i++ ) {
			MonoMetrics a = errors.get(i-1);
			MonoMetrics b = errors.get(i);
			
			double drift = Math.abs(a.scaleRatio-b.scaleRatio)/Math.max(a.scaleRatio,b.scaleRatio);
			aveDriftMag += drift;
		}
		results.scaleDrift = aveDriftMag / errors.size();
	}

	private void computePoseError( MonoTrialResults results ) {
		double tran = 0, rot = 0;
		for( int i = 0; i < errors.size(); i++ ) {
			MonoMetrics b = errors.get(i);

			rot += b.rotationError;
			tran += b.translationError;
		}

		results.rotation = rot / errors.size();
		results.translation = tran / errors.size();
	}
	
	public MonoTrialResults computeStatistics() {
		MonoTrialResults results = new MonoTrialResults();
		results.randomSeed = seed;
		results.exception = numExceptions != 0;
		results.numFaults = numFaults;
		computeTimeMetrics(results);
		computeScaleDrift(results);
		computePoseError(results);
		
		return results;
	}

}
