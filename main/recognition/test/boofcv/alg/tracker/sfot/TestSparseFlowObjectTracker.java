package boofcv.alg.tracker.sfot;

import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.misc.ImageMiscOps;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.RectangleRotate_F64;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageUInt8;
import georegression.metric.UtilAngle;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestSparseFlowObjectTracker {

	Random rand = new Random(2342);

	@Test
	public void noMotion() {
		checkMotion(0,0,0);
	}

	@Test
	public void translation() {
		checkMotion(0, 0, 0);
		checkMotion(0,-7.6,0);
	}

	@Test
	public void rotation() {
		checkMotion(0, 0, 0.05);
		checkMotion(0,0,-0.05);
	}

	@Test
	public void both() {
		checkMotion(10, -7.6, 0.05);
	}

	protected void checkMotion( double tranX , double tranY , double rot ) {
		ImageUInt8 frame0 = new ImageUInt8(320,240);
		ImageUInt8 frame1 = new ImageUInt8(320,240);
		ImageMiscOps.fillUniform(frame0,rand,0,256);

		double c = Math.cos(rot);
		double s = Math.sin(rot);

		DistortImageOps.affine(frame0,frame1, TypeInterpolate.BILINEAR,c,-s,s,c,tranX,tranY);

		SfotConfig config = new SfotConfig();

		ImageGradient<ImageUInt8,ImageSInt16> gradient = FactoryDerivative.sobel(ImageUInt8.class,ImageSInt16.class);

		SparseFlowObjectTracker<ImageUInt8,ImageSInt16> alg = new SparseFlowObjectTracker<ImageUInt8, ImageSInt16>(
				config,ImageUInt8.class,ImageSInt16.class,gradient);

		RectangleRotate_F64 region0 = new RectangleRotate_F64(120,140,30,40,0.1);
		RectangleRotate_F64 region1 = new RectangleRotate_F64();

		alg.init(frame0,region0);
		assertTrue(alg.update(frame1,region1));

		double expectedX = c*region0.cx - s*region0.cy + tranX;
		double expectedY = s*region0.cx + c*region0.cy + tranY;
		double expectedYaw = UtilAngle.bound(region0.theta + rot);


		assertEquals(expectedX, region1.cx, 0.5);
		assertEquals(expectedY, region1.cy, 0.5);
		assertEquals(expectedYaw, region1.theta, 0.01);
	}

}
