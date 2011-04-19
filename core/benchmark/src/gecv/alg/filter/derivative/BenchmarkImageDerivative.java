package gecv.alg.filter.derivative;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.filter.derivative.sobel.GradientSobel_Naive;
import gecv.alg.filter.derivative.sobel.GradientSobel_Outer;
import gecv.alg.filter.derivative.sobel.GradientSobel_UnrolledOuter;
import gecv.alg.filter.derivative.three.GradientThree_Standard;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

/**
 * Benchmarks related to computing image derivatives
 * 
 * @author Peter Abeles
 */
public class BenchmarkImageDerivative {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static long TEST_TIME = 1000;

	static ImageFloat32 imgFloat32;
	static ImageFloat32 derivX_F32;
	static ImageFloat32 derivY_F32;
	static ImageInt8 imgInt8;
	static ImageInt16 derivX_I16;
	static ImageInt16 derivY_I16;

	public static class SobelNaive_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Naive.process_I8(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelNaive_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Naive.process_F32(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class SobelOuter_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Outer.process_I8(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelOuter_I8_Sub extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Outer.process_I8_sub(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelOuter_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_Outer.process_F32(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class SobelUnrolledOuter_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_UnrolledOuter.process_I8(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class SobelUnrolledOuter_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_UnrolledOuter.process_F32(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class SobelUnrolledOuter_F32_Sub extends PerformerBase
	{
		@Override
		public void process() {
			GradientSobel_UnrolledOuter.process_F32_sub(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class DerivativeThree_Std_F32 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree_Standard.deriv_F32(imgFloat32,derivX_F32,derivY_F32);
		}
	}

	public static class DerivativeThree_Std_I8 extends PerformerBase
	{
		@Override
		public void process() {
			GradientThree_Standard.deriv_I8(imgInt8,derivX_I16,derivY_I16);
		}
	}

	public static class LaplacianEdge_F32 extends PerformerBase
	{
		@Override
		public void process() {
			LaplacianEdge.process_F32(imgFloat32,derivX_F32);
		}
	}

	public static class LaplacianEdge_I8 extends PerformerBase
	{
		@Override
		public void process() {
			LaplacianEdge.process_I8(imgInt8,derivX_I16);
		}
	}

	public static void main( String args[] ) {
		imgInt8 = new ImageInt8(imgWidth,imgHeight);
		derivX_I16 = new ImageInt16(imgWidth,imgHeight);
		derivY_I16 = new ImageInt16(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);
		derivX_F32 = new ImageFloat32(imgWidth,imgHeight);
		derivY_F32 = new ImageFloat32(imgWidth,imgHeight);


		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();
		System.out.println("             ImageInt8");
		System.out.println();

		ProfileOperation.printOpsPerSec(new SobelNaive_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelOuter_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelOuter_I8_Sub(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelUnrolledOuter_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new DerivativeThree_Std_I8(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new LaplacianEdge_I8(),TEST_TIME);

		System.out.println("\n             ImageFloat32");
		System.out.println();
		ProfileOperation.printOpsPerSec(new SobelNaive_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelOuter_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelUnrolledOuter_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new SobelUnrolledOuter_F32_Sub(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new DerivativeThree_Std_F32(),TEST_TIME);
		ProfileOperation.printOpsPerSec(new LaplacianEdge_F32(),TEST_TIME);
	}
}
