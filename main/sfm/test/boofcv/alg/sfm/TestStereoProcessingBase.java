package boofcv.alg.sfm;

import boofcv.alg.misc.PixelMath;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import org.junit.Test;

import static boofcv.alg.sfm.TestAssociateStereoPoint.createStereoParam;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestStereoProcessingBase {

	int width = 320;
	int height = 240;

	/**
	 * Center a point in the left and right images.  Search for the point and see if after rectification
	 * the point can be found on the same row in both images.
	 */
	@Test
	public void checkRectification() {
		// point being viewed
		Point3D_F64 X = new Point3D_F64(-0.01,0.1,3);

		StereoParameters param = createStereoParam(width,height,false);

		// create input images by rendering the point in both
		ImageUInt8 left = new ImageUInt8(width,height);
		ImageUInt8 right = new ImageUInt8(width,height);

		// compute the view in pixels of the point in the left and right cameras
		Point2D_F64 lensLeft = new Point2D_F64();
		Point2D_F64 lensRight = new Point2D_F64();
		SfmTestHelper.renderPointPixel(param,X,lensLeft,lensRight);

		// render the pixel in the image
		left.set((int)lensLeft.x,(int)lensLeft.y,200);
		right.set((int)lensRight.x,(int)lensRight.y,200);

		// test the algorithm
		StereoProcessingBase<ImageUInt8> alg = new StereoProcessingBase<ImageUInt8>(param,ImageUInt8.class);

		alg.setImages(left,right);
		alg.initialize();

		// Test tolerances are set to one pixel due to discretization errors in the image
		// sanity check test
		assertFalse(Math.abs(lensLeft.y - lensRight.y) <= 1);

		// check properties of a rectified image and stereo pairs
		Point2D_F64 foundLeft = centroid(alg.getImageLeftRect());
		Point2D_F64 foundRight = centroid(alg.getImageRightRect());

		assertTrue(Math.abs(foundLeft.y - foundRight.y) <= 1);
		assertTrue(foundRight.x < foundLeft.x);
	}

	/**
	 * Finds the mean point in the image weighted by pixel intensity
	 */
	public static Point2D_F64 centroid( ImageUInt8 image ) {
		double meanX = 0;
		double meanY = 0;
		double totalPixel = PixelMath.sum(image);

		for( int i = 0; i < image.height; i++ ) {
			for( int j = 0; j < image.width; j++ ) {
				meanX += image.get(j,i)*j;
				meanY += image.get(j,i)*i;
			}
		}

		meanX /= totalPixel;
		meanY /= totalPixel;

		return new Point2D_F64(meanX,meanY);
	}
}
