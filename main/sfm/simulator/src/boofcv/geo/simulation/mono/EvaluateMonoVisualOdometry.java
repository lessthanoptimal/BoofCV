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
import boofcv.struct.distort.PointTransform_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Vector3D_F64;
import org.ejml.data.DenseMatrix64F;

import java.util.Random;

/**
 * @author Peter Abeles
 */
// todo render output in a GUI
// TODO move into a separate package?
public abstract class EvaluateMonoVisualOdometry {

	int targetTracks = 300;

	Random rand = new Random(234);
	
	MonocularVisualOdometry<?> alg;
	SimulationEngine sim;
	
	int numFaults;

	protected EvaluateMonoVisualOdometry() {
	}

	public abstract MonocularVisualOdometry<?> createAlg( ImagePointTracker<?> tracker ,
														  PointTransform_F64 pixelToNormalized );

	public void setup( int imageWidth , int imageHeight ,
					   DenseMatrix64F K , double sigmaPixel ,
					   CameraControl control )
	{
		// define the simulator
		CameraModel camera = new DistortedPinholeCamera(rand,K,null,imageWidth,imageHeight,true,sigmaPixel);
		EnvironmentModel environment = new BasicEnvironment(rand,20,1,2);
		control.setCamera(camera);

		sim = new SimulationEngine(environment);
		sim.addCamera(camera,control);
		
		// create the algorithm
		RemoveRadialPtoN_F64 p2n = new RemoveRadialPtoN_F64();
		p2n.set(K.get(0,0),K.get(1,1),K.get(0,1),K.get(0,2),K.get(1,2));

		ImagePointTracker tracker = new SimulatedTracker(environment,camera,targetTracks);
		alg = createAlg(tracker,new LeftToRightHanded_F64(p2n));


		// reset error statistics
		numFaults = 0;
	}


	public void step() {
		// update the simulator and pose estimate
		sim.step();
		boolean worked = alg.process(null);
		sim.maintenance();

		Vector3D_F64 T = alg.getPose().getT();

		double euler[] = RotationMatrixGenerator.matrixToEulerXYZ(alg.getPose().getR());
		
		double angle = Math.sqrt( euler[0]*euler[0] + euler[1]*euler[1] + euler[2]*euler[2] );
		
		double dist = T.norm();
		
		System.out.println(" angle = "+angle+"  dist "+dist);
		
		// update position score
		if( !worked && alg.isFatal() ) {
			numFaults++;
		}
	}

	public void computeStatistics() {

	}

}
