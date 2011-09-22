/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.binary;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.PixelMath;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImageBinaryLabeledPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * @author Peter Abeles
 */
public class DetectParticlesApp {

	Random rand = new Random(234);
	ListDisplayPanel binaryPanel = new ListDisplayPanel();
	ListDisplayPanel labeledPanel = new ListDisplayPanel();
	int colors[];

	public void process( ImageUInt8 original ) {

		colors = new int[ original.width*original.height/4 ];
		for( int i = 1; i < colors.length; i++ ) {
			colors[i] = rand.nextInt(0xFFFFFF);
		}

		double average = PixelMath.sum(original)/(double)(original.width*original.height);

		average *= 0.8;

		useThreshold(original,average);
		useHysteresis4(original,average);
		useHysteresis8(original,average);
		useThresholdMorph(original,average);

		ShowImages.showWindow(binaryPanel,"Binary Images");
		ShowImages.showWindow(labeledPanel,"Labeled Images");
	}

	private void useThreshold( ImageUInt8 input , double threshold ) {
		ImageSInt32 labeled = new ImageSInt32(input.width,input.height);
		ImageUInt8 thresholded = ThresholdImageOps.threshold(input,null,(int)threshold,true);

		int numBlobs = BinaryImageOps.labelBlobs4(thresholded,labeled);
		binaryPanel.addImage(VisualizeBinaryData.renderBinary(thresholded,null),"Threshold");
		labeledPanel.addItem(new ImageBinaryLabeledPanel(labeled,numBlobs+1,2342), "Threshold");
	}

	private void useHysteresis4( ImageUInt8 input , double threshold ) {
		ImageSInt32 labeled = new ImageSInt32(input.width,input.height);
		int numBlobs = ThresholdImageOps.hysteresisLabel4(input,labeled,(int)(threshold*0.6),(int)threshold,true,null);
		ImageUInt8 binary = BinaryImageOps.labelToBinary(labeled,null);

		binaryPanel.addImage(VisualizeBinaryData.renderBinary(binary,null),"Hysteresis4");
		labeledPanel.addItem(new ImageBinaryLabeledPanel(labeled,numBlobs+1,2342), "Hysteresis4");
	}

	private void useHysteresis8( ImageUInt8 input , double threshold ) {
		ImageSInt32 labeled = new ImageSInt32(input.width,input.height);
		int numBlobs = ThresholdImageOps.hysteresisLabel8(input,labeled,(int)(threshold*0.6),(int)threshold,true,null);
		ImageUInt8 binary = BinaryImageOps.labelToBinary(labeled,null);

		binaryPanel.addImage(VisualizeBinaryData.renderBinary(binary,null),"Hysteresis8");
		labeledPanel.addItem(new ImageBinaryLabeledPanel(labeled,numBlobs+1,2342), "Hysteresis8");
	}

	private void useThresholdMorph( ImageUInt8 input , double threshold ) {
		ImageSInt32 labeled = new ImageSInt32(input.width,input.height);

		ImageUInt8 thresholded = ThresholdImageOps.threshold(input,null,(int)threshold,true);
		ImageUInt8 mod = BinaryImageOps.erode8(thresholded,null);
		mod = BinaryImageOps.dilate8(mod,null);

		int numBlobs = BinaryImageOps.labelBlobs4(mod,labeled);
		binaryPanel.addImage(VisualizeBinaryData.renderBinary(mod,null),"Threshold + Morph");
		labeledPanel.addItem(new ImageBinaryLabeledPanel(labeled,numBlobs+1,2342), "Threshold + Morph");
	}

	public static void main( String args[] ) {
		BufferedImage originalBuff = UtilImageIO.loadImage("data/particles01.jpg");
		ImageUInt8 original = ConvertBufferedImage.convertFrom(originalBuff,(ImageUInt8)null);

		ShowImages.showWindow(original,"Original");

		DetectParticlesApp app = new DetectParticlesApp();

		app.process(original);
	}
}
