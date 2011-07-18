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

package gecv.alg.binary;

import gecv.alg.filter.binary.BinaryImageOps;
import gecv.alg.filter.binary.ThresholdImageOps;
import gecv.alg.misc.PixelMath;
import gecv.core.image.ConvertBufferedImage;
import gecv.gui.image.ImageBinaryLabeledPanel;
import gecv.gui.image.ImageBinaryPanel;
import gecv.gui.image.ShowImages;
import gecv.io.image.UtilImageIO;
import gecv.struct.image.ImageSInt32;
import gecv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;


/**
 * @author Peter Abeles
 */
public class DetectParticlesApp {

	public static void main( String args[] ) {
		BufferedImage originalBuff = UtilImageIO.loadImage("evaluation/data/particles01.jpg");
		ImageUInt8 original = ConvertBufferedImage.convertFrom(originalBuff,(ImageUInt8)null);

		// todo add auto select threshold
		double average = PixelMath.sum(original)/(double)(original.width*original.height);

		ImageUInt8 thresholded = ThresholdImageOps.threshold(original,null,(int)average,true);
		ImageUInt8 mod = BinaryImageOps.erode8(thresholded,null);
		mod = BinaryImageOps.dilate8(mod,null);

		ImageBinaryPanel panel = new ImageBinaryPanel(mod);
		ShowImages.showWindow(panel,"Threshold");

		int max = 5000;
		ImageSInt32 labeled = new ImageSInt32(original.width,original.height);
		int numFound = BinaryImageOps.labelBlobs8(mod,labeled,new int[max]);

		ImageBinaryLabeledPanel panel2 = new ImageBinaryLabeledPanel(labeled,max,234234);
		ShowImages.showWindow(panel2,"Labeled");

		System.out.println("Num found: "+numFound);
	}
}
