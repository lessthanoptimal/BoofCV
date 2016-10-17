/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.io.image;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.misc.PerformerBase;
import boofcv.misc.ProfileOperation;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.Planar;

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
	static GrayU8 imgInt8;
	static Planar<GrayU8> multiInt8;
	
	static ImageBase boofImg;

	public static class FromBuffToBoof extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.convertFrom(imgBuff,boofImg,true);
		}
	}

	public static class FromBoofToBuff extends PerformerBase
	{
		@Override
		public void process() {
			ConvertBufferedImage.convertTo(boofImg,imgBuff,true);
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
			ConvertBufferedImage.extractGrayU8(imgBuff);
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

	public static void evaluateConvert( ImageBase image , String name )
	{
		boofImg = image;
		System.out.printf("Buffered to %s  %10.2f ops/sec\n",name,
				ProfileOperation.profileOpsPerSec(new FromBuffToBoof(),1000, false));
		System.out.printf("%s to Buffered  %10.2f ops/sec\n",name,
				ProfileOperation.profileOpsPerSec(new FromBoofToBuff(),1000, false));

	}
	
	public static void main( String args[] ) {
		imgInt8 = new GrayU8(imgWidth,imgHeight);
		multiInt8 = new Planar<>(GrayU8.class,imgWidth,imgHeight,3);
		
		GImageMiscOps.fillUniform(imgInt8, rand, 0, 100);
		for( int i = 0; i < multiInt8.getNumBands(); i++ )
			GImageMiscOps.fillUniform(multiInt8.getBand(0), rand, 0, 100);

		System.out.println("=========  Profiling for GrayU8 ==========");
		System.out.println();

		createBufferedImage(BufferedImage.TYPE_3BYTE_BGR);
		System.out.println("------- BufferedImage RGB interface ---------- ");
		System.out.printf("BufferedImage to GrayU8   %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new FromInt8ToGenericBuff(),1000, false));
		System.out.println("---- TYPE_3BYTE_BGR ----");
		evaluateConvert(imgInt8,"GrayU8");
		evaluateConvert(multiInt8,"Planar_U8");

		System.out.println("---- TYPE_INT_RGB ----");
		createBufferedImage(BufferedImage.TYPE_INT_RGB);
		evaluateConvert(imgInt8,"GrayU8");
		evaluateConvert(multiInt8,"Planar_U8");

		System.out.println("---- TYPE_BYTE_GRAY ----");
		createBufferedImage(BufferedImage.TYPE_BYTE_GRAY);
		evaluateConvert(imgInt8,"GrayU8");

		System.out.printf("extractImageInt8             %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new ExtractImageInt8(),1000, false));
		System.out.printf("extractBuffered              %10.2f ops/sec\n",
				ProfileOperation.profileOpsPerSec(new ExtractBuffered(),1000, false));

		System.out.println();
		System.out.println("=========  Profiling for ImageInterleavedInt8 ==========");
		System.out.println();
	}
}
