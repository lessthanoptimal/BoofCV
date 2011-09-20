package boofcv.abst.filter.blur;

import boofcv.abst.filter.FilterImageInterface;
import boofcv.struct.image.ImageBase;


/**
 * Interface for filters which blur the image.
 *
 * @author Peter Abeles
 */
public interface BlurFilter<T extends ImageBase> extends FilterImageInterface<T,T> {

	/**
	 * Radius of the square region.  The width is defined as the radius*2 + 1.
	 *
	 * @return Blur region's radius.
	 */
	public int getRadius();

	public void setRadius(int radius);
}
