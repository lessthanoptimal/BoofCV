package gecv.core.image;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.struct.image.ImageFloat32;
import gecv.struct.image.ImageInt16;
import gecv.struct.image.ImageInt8;

import java.awt.image.BufferedImage;

/**
 * Benchmarks related to functions inside of ConvertImage
 * 
 * @author Peter Abeles
 */
public class BenchmarkConvertImage {
	static int imgWidth = 640;
	static int imgHeight = 480;

	static ImageFloat32 imgFloat32;
	static ImageInt8 imgInt8;
	static ImageInt16 imgInt16;

	public static class Float32toInt8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertImage.convert(imgFloat32,imgInt8);
		}
	}

	public static class Int8ToFloat32 extends PerformerBase
	{
		boolean isSigned;

		public Int8ToFloat32(boolean signed) {
			isSigned = signed;
		}

		@Override
		public void process() {
			ConvertImage.convert(imgInt8,imgFloat32,isSigned);
		}
	}

	public static class Int16ToFloat32 extends PerformerBase
	{
		boolean isSigned;

		public Int16ToFloat32(boolean signed) {
			isSigned = signed;
		}

		@Override
		public void process() {
			ConvertImage.convert(imgInt16,imgFloat32,isSigned);
		}
	}

	public static void main( String args[] ) {
		imgInt8 = new ImageInt8(imgWidth,imgHeight);
		imgInt16 = new ImageInt16(imgWidth,imgHeight);
		imgFloat32 = new ImageFloat32(imgWidth,imgHeight);

		System.out.println("=========  Profile Image Size "+imgWidth+" x "+imgHeight+" ==========");
		System.out.println();

		System.out.printf("Float32 to Int8              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Float32toInt8(),1000));
		System.out.printf("Int8 to Float32 signed       %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int8ToFloat32(false),1000));
		System.out.printf("Int8 to Float32 unsigned     %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int8ToFloat32(true),1000));
		System.out.printf("Int16 to Float32 signed       %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int16ToFloat32(false),1000));
		System.out.printf("Int16 to Float32 unsigned     %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new Int16ToFloat32(true),1000));

	}
}
