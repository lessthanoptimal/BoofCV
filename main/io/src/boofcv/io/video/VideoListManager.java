package boofcv.io.video;

import boofcv.io.InputListManager;
import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.images.BufferedFileImageSequence;
import boofcv.struct.image.ImageBase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * List of preselected videos and their labels.
 *
 * @author Peter Abeles
 */
public class VideoListManager<T extends ImageBase> implements InputListManager {
	protected List<String> imageLabels = new ArrayList<String>();
	protected List<String[]> fileNames = new ArrayList<String[]>();

	protected Class<T> imageType;

	public VideoListManager(Class<T> imageType) {
		this.imageType = imageType;
	}

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

	public SimpleImageSequence<T> loadSequence( int index ) {
		return loadSequence(index,0);
	}

	public SimpleImageSequence<T> loadSequence( int labelIndex , int imageIndex ) {
		File directory = new File(fileNames.get(labelIndex)[imageIndex]);
		return new BufferedFileImageSequence<T>(imageType,directory,"jpg");
	}
}
