package boofcv.android.gui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import boofcv.android.ConvertBitmap;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

/**
 * Visualizes a process where the output is simply a rendered Bitmap image.  Provides an input image
 * in a BoofCV image and a Bitmap to render the output to.
 *
 * @author Peter Abeles
 */
public abstract class VideoImageProcessing<T extends ImageBase> extends VideoRenderProcessing<T> {

	// output image which is modified by processing thread
	private Bitmap output;
	// output image which is displayed by the GUI
	private Bitmap outputGUI;
	// storage used during image convert
	private byte[] storage;

	/**
	 * Constructor
	 *
	 * @param imageType Type of image the video stream is to be converted to
	 */
	protected VideoImageProcessing(ImageType<T> imageType) {
		super(imageType);
	}

	@Override
	protected void declareImages( int width , int height ) {
		super.declareImages(width,height);
		output = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
		outputGUI = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888 );
		storage = ConvertBitmap.declareStorage(output,storage);
	}

	@Override
	protected void process(T gray) {
		process(gray,output,storage);
		synchronized ( lockGui ) {
			Bitmap tmp = output;
			output = outputGUI;
			outputGUI = tmp;
		}
	}

	@Override
	protected void render(Canvas canvas, double imageToOutput) {
		synchronized ( lockGui ) {
			canvas.drawBitmap(outputGUI,0,0,null);
		}
	}

	/**
	 * Process video stream and computes output Bitmap for display.
	 *
	 * @param image (Input) Video stream translated into a BoofCV type image.
	 * @param output (Output) Storage for Bitmap image which is to be displayed.
	 * @param storage Array which can be used when converting the boofcv image into a Bitmap.
	 */
	protected abstract void process( T image , Bitmap output , byte[] storage );
}
