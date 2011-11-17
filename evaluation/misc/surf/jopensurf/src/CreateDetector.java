import com.stromberglabs.jopensurf.FastHessian;
import com.stromberglabs.jopensurf.IntegralImage;
import com.stromberglabs.jopensurf.SURFInterestPoint;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class CreateDetector {

	public static void process( BufferedImage image ,  String detectName )
			throws IOException
	{
		// save the descriptors
		PrintStream out = new PrintStream(new FileOutputStream(detectName));

		IntegralImage ii = new IntegralImage(image);
		FastHessian detector = new FastHessian(ii,4,1,0.0008f, 0.81F);
		try {
			List<SURFInterestPoint> found = detector.getIPoints();

			for( SURFInterestPoint p : found ) {
				out.printf("%7.3f %7.3f %7.5f %7.5f\n",p.getX(),p.getY(),p.getScale(),0f);
			}
			System.out.println("Points Found: "+found.size());
		} catch( RuntimeException e ) {
			e.printStackTrace();
			System.out.println("Exception");
		}
		out.close();

	}

	private static void processDirectory( String nameDirectory ) throws IOException {

		for( int i = 1; i <= 6; i++ ) {
			String detectName = String.format("%s/DETECTED_img%d_%s.txt",nameDirectory,i,"JOpenSURF");

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
