package gecv.detect.corner;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.detect.corner.*;
import gecv.alg.detect.corner.impl.*;
import gecv.core.image.UtilImageFloat32;
import gecv.core.image.UtilImageInt16;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;

import java.util.Random;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkSsdDetectors {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int windowRadius = 2;
	static long TEST_TIME = 1000;

	static ImageFloat32 derivX_F32;
	static ImageFloat32 derivY_F32;
	static ImageInt16 derivX_I16;
	static ImageInt16 derivY_I16;

	static Random rand = new Random(234);

	public static class KLT_F32 extends PerformerBase
	{
		KltCorner_F32 corner = new KltCorner_F32(imgWidth,imgHeight,windowRadius);

		@Override
		public void process() {
			corner.process(derivX_F32,derivY_F32);
		}
	}

	public static class KLT_I16 extends PerformerBase
	{
		KltCorner_I16 corner = new KltCorner_I16(imgWidth,imgHeight,windowRadius);

		@Override
		public void process() {
			corner.process(derivX_I16,derivY_I16);
		}
	}

	public static class KLT_Naive_I16 extends PerformerBase
	{
		SsdCornerNaive_I16 corner = new SsdCornerNaive_I16(imgWidth,imgHeight,windowRadius);

		@Override
		public void process() {
			corner.process(derivX_I16,derivY_I16);
		}
	}

	public static class Harris_F32 extends PerformerBase
	{
		HarrisCornerDetector_F32 corner = new HarrisCornerDetector_F32(imgWidth,imgHeight,windowRadius);

		@Override
		public void process() {
			corner.process(derivX_F32,derivY_F32);
		}
	}

	public static class Harris_I16 extends PerformerBase
	{
		HarrisCornerDetector_I16 corner = new HarrisCornerDetector_I16(imgWidth,imgHeight,windowRadius);

		@Override
		public void process() {
			corner.process(derivX_I16,derivY_I16);
		}
	}

	public static class KitRos_F32 extends PerformerBase
	{
		KitRosCornerDetector_F32 corner = new KitRosCornerDetector_F32(imgWidth,imgHeight,windowRadius);

		@Override
		public void process() {
			corner.process(derivX_F32,derivY_F32);
		}
	}

	public static class KitRos_I16 extends PerformerBase
	{
		KitRosCornerDetector_I16 corner = new KitRosCornerDetector_I16(imgWidth,imgHeight,windowRadius);

		@Override
		public void process() {
			corner.process(derivX_I16,derivY_I16);
		}
	}


	public static void main( String args[] ) {
		derivX_F32 = new ImageFloat32(imgWidth,imgHeight);
		derivY_F32 = new ImageFloat32(imgWidth,imgHeight);
		derivX_I16 = new ImageInt16(imgWidth,imgHeight);
		derivY_I16 = new ImageInt16(imgWidth,imgHeight);

		UtilImageFloat32.randomize(derivX_F32,rand,0,255);
		UtilImageFloat32.randomize(derivY_F32,rand,0,255);
		UtilImageInt16.randomize(derivX_I16,rand,0,255);
		UtilImageInt16.randomize(derivY_I16,rand,0,255);

		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		ProfileOperation.printOpsPerSec(new KLT_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Harris_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new KitRos_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new KLT_I16(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new Harris_I16(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new KitRos_I16(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new KLT_Naive_I16(),TEST_TIME);

	}
}
