package boofcv.io.jcodec;

import boofcv.io.image.SimpleImageSequence;
import boofcv.io.video.VideoInterface;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * @author Peter Abeles
 */
public class JCodecVideoInterface implements VideoInterface {
	@Override
	public <T extends ImageBase> SimpleImageSequence<T> load(String fileName, ImageType<T> imageType) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
