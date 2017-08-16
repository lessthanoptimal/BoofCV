/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.examples;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.jcodec.JCodecSimplified;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.image.BufferedImage;

/**
 * Loads a file using JCodec and displays the image
 *
 * @author Peter Abeles
 */
public class ExampleJCodecDisplayFrames {
	public static void main(String[] args) {

		String fileName = UtilIO.pathExample("background/highway_bridge_jitter.mp4");
		ImageType type = ImageType.pl(3, GrayU8.class);
//		ImageType type = ImageType.single(GrayU8.class);
//		ImageType type = ImageType.pl(3, GrayF32.class);
//		ImageType type = ImageType.single(GrayF32.class);

		JCodecSimplified sequence = new JCodecSimplified<>(fileName, type);

		BufferedImage out;
		if(sequence.hasNext()) {
			ImageBase frame = sequence.next();
			out = new BufferedImage(frame.width,frame.height,BufferedImage.TYPE_INT_RGB);
			ConvertBufferedImage.convertTo(frame,out,false);
		} else {
			throw new RuntimeException("No first frame?!?!");
		}

		ImagePanel gui = new ImagePanel(out);
		ShowImages.showWindow(gui,"Video!", true);

		long totalNano = 0;
		while(sequence.hasNext()) {
			long before = System.nanoTime();
			ImageBase frame = sequence.next();
			totalNano += System.nanoTime()-before;
			ConvertBufferedImage.convertTo(frame,out,false);
			gui.repaint();

			try {
				Thread.sleep(22);
			} catch (InterruptedException e) {}
		}
		System.out.println("Only read FPS = "+(totalNano/1000000.0)/sequence.getFrameNumber());
	}
}
