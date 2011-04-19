package gecv.alg.filter.convolve;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.alg.filter.convolve.impl.*;
import gecv.struct.convolve.Kernel1D_F32;
import gecv.struct.convolve.Kernel1D_I32;
import gecv.struct.convolve.Kernel2D_F32;
import gecv.struct.convolve.Kernel2D_I32;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt32;
import gecv.struct.image.ImageInt8;

/**
 * Benchmark for different convolution operations.
 * @author Peter Abeles
 */
public class BenchmarkConvolve {
	static int imgWidth = 640;
	static int imgHeight = 480;
	static int radius;
	static long TEST_TIME = 1000;

	static Kernel2D_F32 kernel2D_F32;
	static Kernel1D_F32 kernelF32;
	static ImageFloat32 imgFloat32;
	static ImageFloat32 out_F32;
	static Kernel1D_I32 kernelI32;
	static Kernel2D_I32 kernel2D_I32;
	static ImageInt8 imgInt8;
	static ImageInt16 imgInt16;
	static ImageInt8 out_I8;
	static ImageInt16 out_I16;
	static ImageInt32 out_I32;

	public static class Horizontal_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelF32,imgFloat32,out_F32,false);
		}
	}

	public static class Horizontal_F32_Border extends PerformerBase
	{
		@Override
		public void process() {
			Convolve1DBorders.horizontal(kernelF32,imgFloat32,out_F32,false);
		}
	}

	public static class Horizontal_I8_I8_div extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelI32,imgInt8,out_I8,10,false);
		}
	}

	public static class Horizontal_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelI32,imgInt8,out_I16,false);
		}

	}

	public static class Horizontal_I16_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.horizontal(kernelI32,imgInt8,out_I16,false);
		}
	}

	public static class Vertical_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.vertical(kernelF32,imgFloat32,out_F32,false);
		}
	}

	public static class Vertical_F32_Border extends PerformerBase
	{
		@Override
		public void process() {
			Convolve1DBorders.vertical(kernelF32,imgFloat32,out_F32,false);
		}
	}

	public static class Vertical_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.vertical(kernelI32,imgInt8,out_I16,false);
		}
	}

	public static class Vertical_I16_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.vertical(kernelI32,imgInt16,out_I16,false);
		}
	}

	public static class Convolve2D_F32 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.convolve(kernel2D_F32,imgFloat32,out_F32);
		}
	}

	public static class Convolve2D_I8_I16 extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveImageStandard.convolve(kernel2D_I32,imgInt8,out_I16);
		}
	}

	public static class HorizontalUnrolled_F32 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_F32_F32.horizontal(kernelF32,imgFloat32,out_F32,false) )
				throw new RuntimeException();
		}
	}

	public static class VerticalUnrolled_F32 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_F32_F32.vertical(kernelF32,imgFloat32,out_F32,false) )
				throw new RuntimeException();
		}
	}

	public static class HorizontalUnrolled_I8 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I8_I16.horizontal(kernelI32,imgInt8,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class VerticalUnrolled_I8 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I8_I16.vertical(kernelI32,imgInt8,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class HorizontalUnrolled_I16 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I16_I16.horizontal(kernelI32,imgInt16,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class VerticalUnrolled_I16 extends PerformerBase
	{
		@Override
		public void process() {
			if( !ConvolveImageUnrolled_I16_I16.vertical(kernelI32,imgInt16,out_I16,false) )
				throw new RuntimeException();
		}
	}

	public static class Box_I8_I32_Vertical extends PerformerBase
	{
		@Override
		public void process() {
			ConvolveBox_I8_I32.vertical(imgInt8,out_I32,radius,false);
		}
	}

	public static void main( String args[] ) {
		imgInt8 = new ImageInt8(imgWidth,imgHeight);
		imgInt16 = new ImageInt16(imgWidth,imgHeight);
		out_I32 = new ImageInt32(imgWidth,imgHeight);
		out_I16 = new ImageInt16(imgWidth,imgHeight);
		out_I8 = new ImageInt8(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);
		out_F32 = new ImageFloat32(imgWidth,imgHeight);


		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		for( int radius = 1; radius < 10; radius += 1 ) {
			System.out.println("Radius: "+radius);
			System.out.println();
			BenchmarkConvolve.radius = radius;
			kernelF32 = KernelFactory.gaussian1D_F32(radius,true);
			kernelI32 = KernelFactory.gaussian1D_I32(radius);
			kernel2D_F32 = KernelFactory.gaussian2D_F32(1.0,radius,true);
			kernel2D_I32 = KernelFactory.gaussian2D_I32(1.0,radius);
			
			ProfileOperation.printOpsPerSec(new Horizontal_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_I8_I8_div(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_I16_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Horizontal_F32_Border(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HorizontalUnrolled_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HorizontalUnrolled_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new HorizontalUnrolled_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_I8_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_I16_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new VerticalUnrolled_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new VerticalUnrolled_I8(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new VerticalUnrolled_I16(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Vertical_F32_Border(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Box_I8_I32_Vertical(),TEST_TIME);
//
			ProfileOperation.printOpsPerSec(new Convolve2D_F32(),TEST_TIME);
			ProfileOperation.printOpsPerSec(new Convolve2D_I8_I16(),TEST_TIME);
		}


	}
}
