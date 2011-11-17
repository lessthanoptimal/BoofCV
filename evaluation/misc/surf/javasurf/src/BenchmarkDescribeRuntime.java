import org.mite.jsurf.IDescriptor;
import org.mite.jsurf.ISURFfactory;
import org.mite.jsurf.InterestPoint;
import org.mite.jsurf.SURF;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Peter Abeles
 */
public class BenchmarkDescribeRuntime {

	public static void benchmark( String directory , int imageNumber , String detector )
			throws IOException
	{
		String detectName = String.format("%s/DETECTED_img%d_%s.txt", directory, imageNumber, detector);
		String imageName = String.format("%s/img%d.png", directory, imageNumber);

		BufferedImage image = ImageIO.read(new File(imageName));

		ArrayList<InterestPoint> points = CreateDescription.loadInterestPoints(image,detectName);

		// Compute descriptors for each point
		ISURFfactory mySURF = SURF.createInstance(image, 0.9f, 800, 4, image);

		long best = Long.MAX_VALUE;

		for( int i = 0; i < 10; i++ ) {

			long before = System.currentTimeMillis();

			IDescriptor descriptor = mySURF.createDescriptor(points);
			descriptor.generateAllDescriptors();

			long after = System.currentTimeMillis();
			long elapsed = after-before;

			System.out.println("time = "+elapsed);

			if( elapsed < best )
				best = elapsed;
		}

		System.out.println();
		System.out.println("Best = "+best);
	}

	public static void main( String args[] ) throws IOException {
		benchmark("../../data/mikolajczk/boat",1,"FH");
	}
}
