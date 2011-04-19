package gecv.core.image;

import gecv.PerformerBase;
import gecv.ProfileOperation;
import gecv.struct.image.ImageInt8;
import sun.awt.image.ByteInterleavedRaster;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Benchmarks related to converting to and from BufferedImage.
 * 
 * @author Peter Abeles
 */
public class BenchmarkConvertBufferedImage {
	static Random rand = new Random(342543);

	static int imgWidth = 640;
	static int imgHeight = 480;

	static BufferedImage imgBuff;
	static ImageInt8 imgInt8;

	public static class FromBuffToInt8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.convertFrom(imgBuff,imgInt8);
		}
	}

	public static class FromInt8ToBuff extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.convertTo(imgInt8,imgBuff);
		}
	}

	public static class FromInt8ToGenericBuff extends PerformerBase
	{
		@Override
		public void process() {
			ConvertRaster.grayToBuffered(imgInt8,imgBuff);
		}
	}

	public static class ExtractImageInt8 extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.extractImageInt8(imgBuff);
		}
	}

	public static class ExtractBuffered extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.extractBuffered(imgInt8);
		}
	}

	public static void createBufferedImage( int type ) {
		imgBuff = new BufferedImage(imgWidth,imgHeight,type);

		// randomize it to prevent some pathological condition
		for( int i = 0; i < imgHeight; i++ ) {
			for( int j = 0; j < imgWidth; j++ ) {
				imgBuff.setRGB(j,i,rand.nextInt());
			}
		}
	}

	public static void main( String args[] ) {
		imgInt8 = new ImageInt8(imgWidth,imgHeight);

		System.out.println("=========  Profiling for ImageInt8 ==========");
		System.out.println();

		createBufferedImage(BufferedImage.TYPE_3BYTE_BGR);
		System.out.printf("BufferedImage to ImageInt8   %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToGenericBuff(),1000));
		System.out.printf("TYPE_3BYTE_BGR to ImageInt8  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromBuffToInt8(),1000));
		System.out.printf("ImageInt8 to TYPE_3BYTE_BGR  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToBuff(),1000));

		createBufferedImage(BufferedImage.TYPE_INT_RGB);
		System.out.printf("TYPE_INT_RGB to ImageInt8    %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromBuffToInt8(),1000));
		System.out.printf("ImageInt8 to TYPE_INT_RGB    %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToBuff(),1000));

		createBufferedImage(BufferedImage.TYPE_BYTE_GRAY);
		System.out.printf("TYPE_BYTE_GRAY to ImageInt8  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromBuffToInt8(),1000));
		System.out.printf("ImageInt8 to TYPE_BYTE_GRAY  %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToBuff(),1000));

		System.out.printf("extractImageInt8             %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new ExtractImageInt8(),1000));
		System.out.printf("wxtractBuffered              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new ExtractBuffered(),1000));

		System.out.println();
		System.out.println("=========  Profiling for ImageInterleavedInt8 ==========");
		System.out.println();
	}
}
