package boofcv.io.webcamcapture;

import com.github.sarxos.webcam.Webcam;

import java.awt.*;

/**
 * Utility functions related to Webcam capture
 *
 * @author Peter Abeles
 */
public class UtilWebcamCapture {

	/**
	 * Opens the default camera while adjusting its resolution
	 */
	public static Webcam openDefault( int desiredWidth , int desiredHeight) {
		Webcam webcam = Webcam.getDefault();
		Dimension[] sizes = webcam.getViewSizes();
		int bestError = Integer.MAX_VALUE;
		Dimension best = null;
		for( Dimension d : sizes ) {
			int error = (d.width-desiredWidth)*(d.height-desiredHeight);
			if( error < bestError ) {
				bestError = error;
				best = d;
			}
		}
		webcam.setViewSize(best);
		webcam.open();
		return webcam;
	}
}
