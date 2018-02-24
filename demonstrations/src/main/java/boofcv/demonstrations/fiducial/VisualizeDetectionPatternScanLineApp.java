/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.fiducial;

import boofcv.alg.fiducial.qrcode.DetectionPatternScanLine;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class VisualizeDetectionPatternScanLineApp {
	public static void main(String[] args) {
//		JFrame frame = new JFrame();
//		frame.setVisible(true);
//		File file = BoofSwingUtil.openFileChooser(frame, BoofSwingUtil.FileTypes.IMAGES);
		File file = new File("/Users/pabeles/projects/SCAVision/datasets/qrcode_test/image_hayward_007.jpg");
		System.out.println(file.getAbsolutePath());
		BufferedImage buffered = UtilImageIO.loadImage(file.getAbsolutePath());
		GrayU8 input = ConvertBufferedImage.convertFrom(buffered,(GrayU8)null);

		DetectionPatternScanLine alg = new DetectionPatternScanLine();

		alg.process(input);

		BufferedImage marked = new BufferedImage(buffered.getWidth(),buffered.getHeight(),BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = marked.createGraphics();
		g2.drawImage(buffered,0,0,buffered.getWidth(),buffered.getHeight(),null);
		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(1));

		FastQueue<Point2D_I32> list = alg.detections;
		for ( int i = 0; i < list.size; i += 2 ) {
			Point2D_I32 a = list.get(i);
			Point2D_I32 b = list.get(i+1);

			g2.drawLine(a.x,a.y,b.x,b.y);
		}

		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(buffered,"Original");
		gui.addItem(new ImagePanel(marked, ScaleOptions.DOWN),"Marked");

		ShowImages.showWindow(gui,"Visualized", true);

		for (int i = 0; i < 20; i++) {
			long time0 = System.currentTimeMillis();
			alg.process(input);
			long time1 = System.currentTimeMillis();
			System.out.println("elapsed "+(time1-time0));
		}
	}
}
