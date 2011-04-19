package gecv.filter.derivative;

import gecv.struct.image.ImageFloat32;


/**
 * An abstract class that does implements functiosn relating to setting and getting input and output
 * images.
 *
 * @author Peter Abeles
 */
public abstract class DerivativeXYBase_F32 implements DerivativeXY_F32 {

	protected ImageFloat32 image;
	protected ImageFloat32 derivX;
	protected ImageFloat32 derivY;

	@Override
	public void setOutputs(ImageFloat32 derivX, ImageFloat32 derivY) {
		this.derivX = derivX;
		this.derivY = derivY;
	}

	@Override
	public void createOutputs(int imageWidth, int imageHeight) {
		this.derivX = new ImageFloat32(imageWidth, imageHeight);
		this.derivY = new ImageFloat32(imageWidth, imageHeight);
	}

	@Override
	public void setInputs(ImageFloat32 image) {
		this.image = image;
	}

	@Override
	public ImageFloat32 getDerivX() {
		return derivX;
	}

	@Override
	public ImageFloat32 getDerivY() {
		return derivY;
	}
}
