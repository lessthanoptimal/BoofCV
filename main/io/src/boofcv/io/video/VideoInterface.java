package boofcv.io.video;

import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageSingleBand;


/**
 * Abstract interface for loading video streams.
 *
 * @author Peter Abeles
 */
public interface VideoInterface {

	public <T extends ImageSingleBand> SimpleImageSequence<T> load( String fileName , Class<T> imageType );
}
