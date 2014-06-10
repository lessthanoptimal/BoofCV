package boofcv.processing;

import boofcv.abst.feature.detdesc.DetectDescribePoint;
import boofcv.struct.feature.TupleDesc;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.struct.point.Point2D_F64;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Simplified version of {@link DetectDescribePoint}.
 *
 * @author Peter Abeles
 */
public class SimpleDetectDescribePoint<T extends ImageBase, Desc extends TupleDesc> {

	DetectDescribePoint<T,Desc> detectDescribe;

	T input;

	public SimpleDetectDescribePoint(DetectDescribePoint<T, Desc> detectDescribe, ImageType<T> imageType ) {
		this.detectDescribe = detectDescribe;

		input = imageType.createImage(1,1);
	}

	public void process( PImage image ) {
		input.reshape(image.width, image.height);
		ConvertProcessing.convertFromRGB(image,input);

		detectDescribe.detect(input);
	}

	public void process( SimpleImage<T> image ) {
		detectDescribe.detect(image.image);
	}

	public List<Desc> getDescriptions() {
		List<Desc> ret = new ArrayList<Desc>();

		for (int i = 0; i < detectDescribe.getNumberOfFeatures(); i++) {
			ret.add( (Desc)detectDescribe.getDescription(i).copy() );
		}

		return ret;
	}

	public List<Point2D_F64> getLocations() {
		List<Point2D_F64> ret = new ArrayList<Point2D_F64>();

		for (int i = 0; i < detectDescribe.getNumberOfFeatures(); i++) {
			ret.add( detectDescribe.getLocation(i).copy() );
		}

		return ret;
	}

	public DetectDescribePoint<T, Desc> getDetectDescribe() {
		return detectDescribe;
	}
}
