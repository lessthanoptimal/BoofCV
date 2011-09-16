package boofcv.io.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * List of preselected images and their labels.
 *
 * @author Peter Abeles
 */
public class ImageListManager {
	protected List<String> imageLabels = new ArrayList<String>();
	protected List<String> fileNames = new ArrayList<String>();

	public void add( String label , String fileName ) {
		imageLabels.add(label);
		fileNames.add(fileName);
	}

	public int size() {
		return imageLabels.size();
	}

	public List<String> getLabels() {
		return imageLabels;
	}

	public String getLabel( int index ) {
		return imageLabels.get(index);
	}

	public BufferedImage loadImage( int index ) {
		BufferedImage image = UtilImageIO.loadImage(fileNames.get(index));
		if( image == null ) {
			System.err.println("Can't load image "+fileNames.get(index));
		}
		return image;
	}
}
