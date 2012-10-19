package boofcv.alg.sfm;

import boofcv.abst.feature.disparity.StereoDisparitySparse;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.distort.PointTransform_F64;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se3_F64;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestAssociateStereoPoint {

	int width = 640;
	int height = 480;

	/**
	 * Manually compute image transforms and compare to found solution.  Assume calculations performed
	 * by its base class are done correctly and override them for ease of testing
	 */
	@Test
	public void basicTest() {

		int disparity = 50;

		StereoParameters param = createStereoParam(width,height,false);
		AssociateStereoPoint alg = new AssociateStereoPoint(new DummyDisparity(disparity),ImageUInt8.class);
		alg.setCalibration(param);

		PointTransform_F64 leftPixelToRect = RectifyImageOps.transformPixelToRect_F64(param.left, alg.rect1);
		PointTransform_F64 rightRectToPixel = RectifyImageOps.transformRectToPixel_F64(param.right, alg.rect2);

		Point2D_F64 found = new Point2D_F64();

		Point2D_F64 leftRect = new Point2D_F64();
		Point2D_F64 rightPixel = new Point2D_F64();
		leftPixelToRect.compute(30,40,leftRect);
		// disparity can only be computed starting at integer pixels, which is why there is the type cast
		rightRectToPixel.compute((int)leftRect.x+disparity,leftRect.y,rightPixel);

		alg.associate(30,40,found);

		assertEquals(rightPixel.x, found.x, 1e-8);
		assertEquals(rightPixel.y, found.y, 1e-8);
	}

	public static StereoParameters createStereoParam( int width , int height , boolean flipY ) {
		StereoParameters ret = new StereoParameters();

		ret.setRightToLeft(new Se3_F64());
		ret.getRightToLeft().getT().set(-0.2,0.001,-0.012);
		RotationMatrixGenerator.eulerXYZ(0.001,-0.01,0.0023,ret.getRightToLeft().getR());

		ret.left = new IntrinsicParameters(300,320,0,width/2,height/2,width,height, flipY, new double[]{0.1,1e-4});
		ret.right = new IntrinsicParameters(290,310,0,width/2+2,height/2-6,width,height, flipY, new double[]{0.05,-2e-4});

		return ret;
	}

	private static class DummyDisparity implements StereoDisparitySparse {

		int disparity;

		private DummyDisparity(int disparity) {
			this.disparity = disparity;
		}

		@Override
		public void setImages(ImageSingleBand imageLeft, ImageSingleBand imageRight) {}

		@Override
		public boolean process(int x, int y) {return true;}

		@Override
		public double getDisparity() {
			return disparity;
		}

		@Override
		public int getBorderX() {return 0;}

		@Override
		public int getBorderY() {return 0;}

		@Override
		public int getMinDisparity() {return 0;}

		@Override
		public int getMaxDisparity() {return 0;}

		@Override
		public Class getInputType() {return null;}
	}
}
