import org.mite.jsurf.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 * @author Peter Abeles
 */
public class CreateDetect {


	public static void process( BufferedImage image , String detectName )
			throws IOException
	{

		// Compute descriptors for each point
		ISURFfactory mySURF = SURF.createInstance(image, 0.9f, 520, 4, image);
		// setting the threshold to zero produced a ridiculous number of detections

		// save the descriptors
		PrintStream out = new PrintStream(new FileOutputStream(detectName));

		IDetector descriptor = mySURF.createDetector();
		ArrayList<InterestPoint> found = descriptor.generateInterestPoints();

		for( InterestPoint p : found ) {
			out.printf("%7.3f %7.3f %7.5f %7.5f\n",p.getX(),p.getY(),p.getScale(),0f);
		}
		out.close();
		System.out.println("Found: "+found.size());
	}

	private static void processDirectory( String nameDirectory ) throws IOException {

		for( int i = 1; i <= 6; i++ ) {
			String detectName = String.format("%s/DETECTED_img%d_%s.txt",nameDirectory,i,"JavaSURF");

			String imageName = String.format("%s/img%d.png",nameDirectory,i);
			BufferedImage img = ImageIO.read(new File(imageName));

			System.out.println("Processing "+detectName);
			process(img, detectName);

		}
	}

	public static void main( String args[] ) throws IOException {
		processDirectory("../../../data/mikolajczk/bark");
		processDirectory("../../../data/mikolajczk/bikes");
		processDirectory("../../../data/mikolajczk/boat");
		processDirectory("../../../data/mikolajczk/graf");
		processDirectory("../../../data/mikolajczk/leuven");
		processDirectory("../../../data/mikolajczk/trees");
		processDirectory("../../../data/mikolajczk/ubc");
		processDirectory("../../../data/mikolajczk/wall");
	}

}