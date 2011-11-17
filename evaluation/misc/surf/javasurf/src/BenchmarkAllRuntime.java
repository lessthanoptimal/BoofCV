import org.mite.jsurf.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * @author Peter Abeles
 */
public class BenchmarkAllRuntime {

	public static void benchmark( String directory , int imageNumber )
			throws IOException
	{
		String imageName = String.format("%s/img%d.png", directory, imageNumber);

		BufferedImage image = ImageIO.read(new File(imageName));



		long best = Long.MAX_VALUE;

		for( int i = 0; i < 10; i++ ) {

			long before = System.currentTimeMillis();

			// Compute descriptors for each point
			ISURFfactory mySURF = SURF.createInstance(image, 0.81f, 638, 4, image);

			// detect interest points
			IDetector detector = mySURF.createDetector();
			ArrayList<InterestPoint> found = detector.generateInterestPoints();

			// describe the regions
			IDescriptor descriptor = mySURF.createDescriptor(found);
			descriptor.generateAllDescriptors();

			long after = System.currentTimeMillis();
			long elapsed = after-before;

			System.out.println("time = "+elapsed+" num found = "+found.size());

			if( elapsed < best )
				best = elapsed;
		}

		System.out.println();
		System.out.println("Best = "+best);
	}

	public static void main( String args[] ) throws IOException {
		benchmark("../../../data/mikolajczk/boat",1);
	}
}
