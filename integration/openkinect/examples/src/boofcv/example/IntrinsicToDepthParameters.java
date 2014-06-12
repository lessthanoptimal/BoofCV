package boofcv.examples;

import boofcv.io.UtilIO;
import boofcv.openkinect.UtilOpenKinect;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.VisualDepthParameters;

/**
 * @author Peter Abeles
 */
public class IntrinsicToDepthParameters {

	public static void main( String args[] ) {
		String baseDir = "../data/evaluation/kinect/";

		String nameCalib = baseDir+"intrinsic.xml";

		IntrinsicParameters intrinsic = UtilIO.loadXML(nameCalib);

		VisualDepthParameters depth = new VisualDepthParameters();

		depth.setVisualParam(intrinsic);
		depth.setMaxDepth(UtilOpenKinect.FREENECT_DEPTH_MM_MAX_VALUE);
		depth.setPixelNoDepth(UtilOpenKinect.FREENECT_DEPTH_MM_NO_VALUE);

		UtilIO.saveXML(depth, baseDir + "visualdepth.xml");
	}
}
