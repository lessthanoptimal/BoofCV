package boofcv.alg.sfm;

import boofcv.alg.distort.LensDistortionOps;
import boofcv.alg.geo.UtilIntrinsic;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F32;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.transform.se.SePointOps_F64;

/**
 * Various helper functions for testing SFM algorithms
 *
 * @author Peter Abeles
 */
public class SfmTestHelper {

	/**
	 * Renders a 3D point in the left and right camera views given the stereo parameters. Lens distortion
	 * is taken in account.
	 *
	 * @param param Stereo parameters
	 * @param X Point location in 3D space
	 * @param left location in pixels in left camera
	 * @param right location in pixels in right camera
	 */
	public static void renderPointPixel( StereoParameters param , Point3D_F64 X ,
										 Point2D_F64 left , Point2D_F64 right ) {
		// compute the location of X in the right camera's reference frame
		Point3D_F64 rightX = new Point3D_F64();
		SePointOps_F64.transform(param.getRightToLeft().invert(null), X, rightX);

		// location of object in normalized image coordinates
		Point2D_F64 normLeft = new Point2D_F64(X.x/X.z,X.y/X.z);
		Point2D_F64 normRight = new Point2D_F64(rightX.x/rightX.z,rightX.y/rightX.z);

		// convert into pixel coordinates
		Point2D_F64 pixelLeft =  UtilIntrinsic.convertNormToPixel(param.left, normLeft.x, normRight.y, null);
		Point2D_F64 pixelRight =  UtilIntrinsic.convertNormToPixel(param.right,normRight.x,normRight.y,null);

		// take in account lens distortion
		PointTransform_F32 distLeft = LensDistortionOps.transformPixelToRadial_F32(param.left);
		PointTransform_F32 distRight = LensDistortionOps.transformPixelToRadial_F32(param.right);

		Point2D_F32 lensLeft = new Point2D_F32();
		Point2D_F32 lensRight = new Point2D_F32();

		distLeft.compute((float)pixelLeft.x,(float)pixelLeft.y,lensLeft);
		distRight.compute((float)pixelRight.x,(float)pixelRight.y,lensRight);

		// output solution
		left.set(lensLeft.x,lensLeft.y);
		right.set(lensRight.x,lensRight.y);
	}
}
