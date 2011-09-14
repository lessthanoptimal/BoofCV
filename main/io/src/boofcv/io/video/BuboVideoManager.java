package boofcv.io.video;

import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;


/**
 * Allows a {@link VideoInterface} to be created abstractly without directly referencing
 * the codec class.
 *
 * @author Peter Abeles
 */
public class BuboVideoManager {

	/**
	 * Loads the default {@link VideoInterface}.
	 *
	 * @return Video interface
	 */
	public static VideoInterface loadManagerDefault() {
		return loadManager("boofcv.io.wrapper.xuggler.XugglerVideoInterface");
	}

	/**
	 * Loads the specified default {@link VideoInterface}.
	 *
	 * @return Video interface
	 */
	public static VideoInterface loadManager( String pathToManager ) {
		try {
			Class c = Class.forName(pathToManager);
			return (VideoInterface) c.newInstance();
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Class not found.  Is it included in the class path?");
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
