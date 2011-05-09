/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.detect.corner;

import gecv.abst.detect.corner.GeneralCornerDetector;
import gecv.abst.detect.corner.GeneralCornerIntensity;
import gecv.abst.detect.corner.WrapperFastCornerIntensity;
import gecv.abst.detect.corner.WrapperGradientCornerIntensity;
import gecv.abst.detect.extract.CornerExtractor;
import gecv.abst.detect.extract.WrapperNonMax;
import gecv.alg.detect.extract.FastNonMaxCornerExtractor;
import gecv.alg.drawing.BasicDrawing;
import gecv.alg.drawing.impl.BasicDrawing_I8;
import gecv.alg.filter.derivative.GradientSobel;
import gecv.gui.image.ShowImages;
import gecv.struct.QueueCorner;
import gecv.struct.image.ImageSInt16;
import gecv.struct.image.ImageUInt8;

import java.util.Random;

/**
 * @author Peter Abeles
 */
public class BenchmarkCornerAccuracy {

	int width = 500;
	int height = 400;
	int radius = 2;
	int maxCorners = 6;

	Random rand = new Random(234);

	ImageUInt8 image= new ImageUInt8(width,height);
	ImageSInt16 derivX = new ImageSInt16(width,height);
	ImageSInt16 derivY = new ImageSInt16(width,height);

	public void createSquare( int x0, int y0, int x1 , int y1 ) {
		BasicDrawing.fill(image,0);
		BasicDrawing.rectangle(image,100,x0,y0,x1,y1);
		BasicDrawing_I8.addNoise(image,rand,-2,2);
		GradientSobel.process(image,derivX,derivY);
	}

	public QueueCorner detectCorners( FastCornerIntensity<ImageUInt8> intensity  ) {
		return detectCorners(new WrapperFastCornerIntensity<ImageUInt8, ImageSInt16>(intensity));
	}

	public QueueCorner detectCorners( GradientCornerIntensity<ImageSInt16> intensity  ) {
		return detectCorners(new WrapperGradientCornerIntensity<ImageUInt8, ImageSInt16>(intensity));
	}

	public QueueCorner detectCorners( GeneralCornerIntensity<ImageUInt8, ImageSInt16> intensity  ) {
		CornerExtractor extractor = new WrapperNonMax(new FastNonMaxCornerExtractor(radius + 10, radius + 10, 1f));
		GeneralCornerDetector<ImageUInt8, ImageSInt16> det =
				new GeneralCornerDetector<ImageUInt8, ImageSInt16>(intensity, extractor, maxCorners);

		if( det.getRequiresGradient() ) {
			derivX = new ImageSInt16(width,height);
			derivY = new ImageSInt16(width,height);

			GradientSobel.process(image,derivX,derivY);
			det.process(image,derivX,derivY);
		} else {
			det.process(image,null,null);
		}

		ShowImages.showWindow(det.getIntensity(),"Intensity",true);

		return det.getCorners();
	}

	public void evaluateAll() {

		createSquare(40,50,100,90);

		ShowImages.showWindow(image,"Evaluation Image");
		ShowImages.showWindow(derivX,"DerivX");
		ShowImages.showWindow(derivY,"DerivY");

		// todo try different noise levels

		evaluate(detectCorners(FactoryCornerIntensity.createFast12_I8(width, height, 10 , 11)),"FAST");
		evaluate(detectCorners(FactoryCornerIntensity.createHarris_I16(width, height, radius, 0.01f)),"Harris");
		evaluate(detectCorners(FactoryCornerIntensity.createKitRos_I16(width, height, radius)),"KitRos");
		evaluate(detectCorners(FactoryCornerIntensity.createKlt_I16(width, height, radius )),"KLT");
	}

	public void evaluate( QueueCorner corners , String name ) {
		
		System.out.println(name+" num corners: "+corners.size());
	}

	public static void main( String args[] ) {
		BenchmarkCornerAccuracy benchmark = new BenchmarkCornerAccuracy();

		benchmark.evaluateAll();
	}
}
