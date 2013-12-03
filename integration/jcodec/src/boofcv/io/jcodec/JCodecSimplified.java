package boofcv.io.jcodec;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.SimpleImageSequence;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.NIOUtils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Reads movie files using JCodec
 *
 * @author Peter Abeles
 */
public class JCodecSimplified<T extends ImageBase> implements SimpleImageSequence<T> {

	FrameGrab grabber;

	T image;
	// type of output image
	ImageType<T> typeOutput;

	BufferedImage frameCurrent;
	BufferedImage frameNext;
	int frame = 0;

	String filename;

	public JCodecSimplified(String filename, ImageType<T> typeOutput) {
		image = typeOutput.createImage(1,1);
		this.typeOutput = typeOutput;
		this.filename = filename;
		reset();
	}

	@Override
	public boolean hasNext() {
		return frameNext != null;
	}

	@Override
	public T next() {
		frameCurrent = frameNext;
		try {
			frameNext = grabber.getFrame();
		} catch (IOException e) {
			frameNext = null;
		}

		image.reshape(frameCurrent.getWidth(), frameCurrent.getHeight());
		ConvertBufferedImage.convertFrom(frameCurrent, image, true);
		frame++;
		return image;
	}

	@Override
	public <InternalImage> InternalImage getGuiImage() {
		return (InternalImage)frameCurrent;
	}

	@Override
	public void close() {
	}

	@Override
	public int getFrameNumber() {
		return frame;
	}

	@Override
	public void setLoop(boolean loop) {
		throw new IllegalArgumentException("Not supported");
	}

	@Override
	public ImageType<T> getImageType() {
		return typeOutput;
	}

	@Override
	public void reset() {
		try {
			grabber = new FrameGrab(NIOUtils.readableFileChannel(new File(filename)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (JCodecException e) {
			throw new RuntimeException(e);
		}
		try {
			frameCurrent = null;
			frameNext = grabber.getFrame();
		} catch (IOException e) {
			frameNext = null;
		}
	}
}
