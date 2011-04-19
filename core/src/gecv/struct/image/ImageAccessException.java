package gecv.struct.image;

/**
 * This exception is thrown when an attempt has been made to access part of an
 * image which is out of bounds.
 *
 * @author Peter Abeles
 */
public class ImageAccessException extends RuntimeException {
	public ImageAccessException() {
	}

	public ImageAccessException(String message) {
		super(message);
	}

	public ImageAccessException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImageAccessException(Throwable cause) {
		super(cause);
	}
}
