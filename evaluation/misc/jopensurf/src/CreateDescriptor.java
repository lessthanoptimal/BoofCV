import com.stromberglabs.jopensurf.SURFInterestPoint;
import com.stromberglabs.jopensurf.Surf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Peter Abeles
 */
public class CreateDescriptor {
	public static List<SURFInterestPoint> loadInterestPoints( String fileName ) throws IOException {
		Scanner in = new Scanner(new BufferedReader(new FileReader(fileName)));

		List<SURFInterestPoint> ret = new ArrayList<SURFInterestPoint>();

		// read in location of points
		while( in.hasNext() ) {
			float x = in.nextFloat();
			float y = in.nextFloat();
			float scale = in.nextFloat();
			float yaw = in.nextFloat();

			ret.add(new SURFInterestPoint(x,y,scale,0));
		}

		return ret;
	}

	public static void process( BufferedImage image , String detectName , String describeName )
			throws IOException
	{
		List<SURFInterestPoint> points = loadInterestPoints(detectName);

		// Compute descriptors for each point
		Surf surf = new Surf(image,0.81F, 0.0004F, 4);

		for( SURFInterestPoint p : points ) {
			surf.getOrientation(p);
			surf.getMDescriptor(p,false);
		}
		// save the descriptors
		PrintStream out = new PrintStream(new FileOutputStream(describeName));

		out.println("64");
		for( SURFInterestPoint p : points ) {

			out.printf("%7.3f %7.3f %7.5f",p.getX(),p.getY(),p.getOrientation());
			float[] desc = p.getDescriptor();
			if( desc.length != 64 )
				throw new RuntimeException("Unexpected descriptor length");

			for( int i = 0; i < desc.length; i++ ) {
				out.printf(" %12.10f",desc[i]);
			}
			out.println();
		}
	}

	private static void processDirectory( String nameDirectory ) throws IOException {
		String nameDetected = "FH";

		for( int i = 1; i <= 6; i++ ) {
			String detectName = String.format("%s/DETECTED_img%d_%s.txt",nameDirectory,i,nameDetected);

			String imageName = String.format("%s/img%d.png",nameDirectory,i);
			BufferedImage img = ImageIO.read(new File(imageName));

			String describeName = String.format("%s/DESCRIBE_img%d_%s.txt",nameDirectory,i,"JOpenSURF");

			System.out.println("Processing "+describeName);
			process(img, detectName, describeName);

		}
	}

	public static void main( String args[] ) throws IOException {
		processDirectory("../../data/mikolajczk/bark");
		processDirectory("../../data/mikolajczk/bikes");
		processDirectory("../../data/mikolajczk/boat");
		processDirectory("../../data/mikolajczk/graf");
		processDirectory("../../data/mikolajczk/leuven");
		processDirectory("../../data/mikolajczk/trees");
		processDirectory("../../data/mikolajczk/ubc");
		processDirectory("../../data/mikolajczk/wall");
	}
}
