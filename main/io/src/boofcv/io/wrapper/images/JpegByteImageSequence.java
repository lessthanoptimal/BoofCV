package boofcv.io.wrapper.images;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageSingleBand;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Create a sequence from an array of jpeg images in byte[] array format.  Each image is decompressed
 * as need
 *
 * @author Peter Abeles
 */
public class JpegByteImageSequence<T extends ImageSingleBand> implements SimpleImageSequence<T> {

	int index;
	List<byte[]> jpegData = new ArrayList<byte[]>();

	// type of image it outputs
	Class<T> imageType;

	BufferedImage imageGUI;
	T output;

	// loop back and forth in the sequence
	boolean loop = false;
	// is it traversing in the forwards or backwards direction
	boolean forward = true;

	public JpegByteImageSequence(Class<T> imageType, List<byte[]> jpegData, boolean loop) {
		this.imageType = imageType;
		this.jpegData = jpegData;
		this.loop = loop;

		output = GeneralizedImageOps.createSingleBand(imageType, 1, 1);
	}

	@Override
	public boolean hasNext() {
		if( loop )
			return true;
		return index < jpegData.size();
	}

	@Override
	public T next() {
		try {
			imageGUI = ImageIO.read(new ByteArrayInputStream(jpegData.get(index)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		if(forward) {
			index++;
			if( loop && index >= jpegData.size() ) {
				index = jpegData.size()-1;
				forward = false;
			}
		} else {
			index--;
			if( loop && index < 0) {
				index = 1;
				forward = true;
			}
		}

		output.reshape(imageGUI.getWidth(),imageGUI.getHeight());
		ConvertBufferedImage.convertFromSingle(imageGUI, output, imageType);

		return output;
	}

	@Override
	public BufferedImage getGuiImage() {
		return imageGUI;
	}

	@Override
	public void close() {
	}

	@Override
	public int getFrameNumber() {
		return index;
	}

	@Override
	public Class<T> getImageType() {
		return imageType;
	}

	@Override
	public void reset() {
		index = 0;
		forward = true;
	}
}
