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
public class BenchmarkDetectRuntime {

	public static void benchmark( String directory , int imageNumber )
			throws IOException
	{
		String imageName = String.format("%s/img%d.png",directory,imageNumber);

		BufferedImage image = ImageIO.read(new File(imageName));

		long best = Long.MAX_VALUE;

		for( int i = 0; i < 10; i++ ) {

			long before = System.currentTimeMillis();

			// caches result, need to declare Surf here
			Surf surf = new Surf(image,0.81F, 0.00004F, 4);
			List<SURFInterestPoint> found = surf.getFreeOrientedInterestPoints();

			long after = System.currentTimeMillis();
			long elapsed = after-before;

			System.out.println("time = "+elapsed+" num found = "+found.size());

			if( elapsed < best )
				best = elapsed;
		}

		System.out.println();
		System.out.println("Best = "+best+" ");
	}

	public static void main( String args[] ) throws IOException {
		benchmark("../../data/mikolajczk/boat",1);
	}
}
