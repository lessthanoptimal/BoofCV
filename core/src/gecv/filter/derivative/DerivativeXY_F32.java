package gecv.filter.derivative;

import gecv.struct.image.ImageFloat32;


/**
 * A generic interface for computing image derivative along the x and y axes for {@link ImageFloat32}.
 *
 * @author Peter Abeles
 */
public interface DerivativeXY_F32 {

	public void setOutputs(ImageFloat32 derivX, ImageFloat32 derivY);

	public void setInputs(ImageFloat32 image);

	public void createOutputs(int imageWidth, int imageHeight);

	public void process();

	/**
	 * How many pixels wide is the region that is not processed along the outside
	 * border of the image.
	 *
	 * @return number of pixels.
	 */
	public int getBorder();

	public ImageFloat32 getDerivX();

	public ImageFloat32 getDerivY();
}
