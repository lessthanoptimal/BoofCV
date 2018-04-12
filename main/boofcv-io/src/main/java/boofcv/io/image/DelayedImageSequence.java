package boofcv.io.image;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

public class DelayedImageSequence<T extends ImageBase<T>> implements SimpleImageSequence<T> {
	final SimpleImageSequence<T> src;

	private T next = null;
	private T last = null;
	private Object nextGUI;
	private Object lastGUI;

	public DelayedImageSequence(SimpleImageSequence<T> src) {
		this.src = src;
		if (src.hasNext())
			next = src.next();
	}

	@Override
	public int getNextWidth() {
		return src.getNextWidth();
	}

	@Override
	public int getNextHeight() {
		return src.getNextHeight();
	}

	@Override
	public boolean hasNext() {
		//return last!=null;
		return true;
	}

	@Override
	public T next() {
		last = next;
		lastGUI = nextGUI;
		next = src.current();
		nextGUI = src.getGuiImage();
		if (last == null && next!=null)
			return next;
		return current();
	}

	@Override
	public <InternalImage> InternalImage getGuiImage() {
		return (InternalImage) lastGUI;
	}

	@Override
	public T current() {
		return last;
	}

	@Override
	public void close() {
		//nothing
	}

	@Override
	public int getFrameNumber() {
		return src.getFrameNumber();
	}

	@Override
	public void setLoop(boolean loop) {
		//nothing
	}

	@Override
	public ImageType<T> getImageType() {
		return src.getImageType();
	}

	@Override
	public void reset() {
		//nothing
	}
}
