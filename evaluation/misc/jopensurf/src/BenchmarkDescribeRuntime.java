import com.stromberglabs.jopensurf.SURFInterestPoint;
import com.stromberglabs.jopensurf.Surf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class BenchmarkDescribeRuntime {

	public static void benchmark( String directory , int imageNumber , String detector )
			throws IOException
	{
		String detectName = String.format("%s/DETECTED_img%d_%s.txt",directory,imageNumber,detector);
		String imageName = String.format("%s/img%d.png",directory,imageNumber);

		BufferedImage image = ImageIO.read(new File(imageName));

		List<SURFInterestPoint> points = CreateDescriptor.loadInterestPoints(detectName);

		// Compute descriptors for each point
		Surf surf = new Surf(image,0.81F, 0.0004F, 4);

		long best = Long.MAX_VALUE;

		for( int i = 0; i < 10; i++ ) {

			long before = System.currentTimeMillis();

			for( SURFInterestPoint p : points ) {
				surf.getOrientation(p);
				surf.getMDescriptor(p,false);
			}

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
