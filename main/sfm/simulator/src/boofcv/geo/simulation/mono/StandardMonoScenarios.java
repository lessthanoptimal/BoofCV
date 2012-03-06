package boofcv.geo.simulation.mono;

import boofcv.geo.simulation.CameraControl;
import boofcv.geo.simulation.impl.ForwardCameraMotion;
import org.ejml.data.DenseMatrix64F;

/**
 * @author Peter Abeles
 */
public class StandardMonoScenarios {

	int width = 640;
	int height = 480;
	
	DenseMatrix64F K;
	
	int numSimulationSteps=1000;
	
	EvaluateMonoVisualOdometry evaluator;

	public StandardMonoScenarios( EvaluateMonoVisualOdometry evaluator ) {
		this.evaluator = evaluator;
		
		double fx = width*2/3;
		double fy = height*2/3;


		K = new DenseMatrix64F(3,3,true,fx,0,width/2,0,fy,height/2,0,0,1);
	}

	public void forwardScenario( double pixelSigma ) {
		CameraControl control = new ForwardCameraMotion(0.05);

		evaluator.setup(width,height,K,pixelSigma,control);

		for( int i = 0; i < numSimulationSteps; i++ ) {
			System.out.println("STEP "+i);
			evaluator.step();
		}
		
		// todo get results
	}

}
