package boofcv.io.wrapper.xuggler;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.struct.image.ImageBase;

/**
 * @author Peter Abeles
 */
public class XugglerMediaManager extends DefaultMediaManager {
	@Override
	public <T extends ImageBase> SimpleImageSequence<T> openVideo(String fileName, Class<T> type) {
		return new XugglerSimplified(fileName,type);
	}
}
