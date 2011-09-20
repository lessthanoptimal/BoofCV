package boofcv.io.image;

import boofcv.io.InputListManager;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * List of preselected images and their labels.
 *
 * @author Peter Abeles
 */
public class ImageListManager implements InputListManager {
	protected List<String> imageLabels = new ArrayList<String>();
	protected List<String[]> fileNames = new ArrayList<String[]>();

	public void add( String label , String ...names ) {
		imageLabels.add(label);
		fileNames.add(names.clone());
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
		BufferedImage image = UtilImageIO.loadImage(fileNames.get(index)[0]);
		if( image == null ) {
			System.err.println("Can't load image "+fileNames.get(index));
		}
		return image;
	}

	public BufferedImage loadImage( int labelIndex , int imageIndex ) {
		BufferedImage image = UtilImageIO.loadImage(fileNames.get(labelIndex)[imageIndex]);
		if( image == null ) {
			System.err.println("Can't load image "+fileNames.get(labelIndex));
		}
		return image;
	}
}
