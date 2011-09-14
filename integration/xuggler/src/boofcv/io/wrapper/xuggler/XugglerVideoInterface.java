package boofcv.io.wrapper.xuggler;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoInterface;
import boofcv.struct.image.ImageBase;


/**
 * @author Peter Abeles
 */
public class XugglerVideoInterface implements VideoInterface {
	@Override
	public <T extends ImageBase> SimpleImageSequence<T> load(String fileName, Class<T> imageType) {
		return new XugglerSimplified<T>(fileName,imageType);
	}
}
