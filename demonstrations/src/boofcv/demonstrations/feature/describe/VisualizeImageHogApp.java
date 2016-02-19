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

package boofcv.demonstrations.feature.describe;

import boofcv.alg.feature.dense.DescribeDenseHogAlg;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDenseAlg;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.ImageUInt8;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO add ability to control visualization options
	// TODO change HOG cell size
	// TODO render grid overlay
public class VisualizeImageHogApp <T extends ImageBase> extends DemonstrationBase<T> {

	DescribeDenseHogAlg<T,?> hog;
	VisualizeHogCells visualizers;

	ImagePanel imagePanel = new ImagePanel();

	BufferedImage work;

	public VisualizeImageHogApp(ConfigDenseHoG config, List<String> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		hog = FactoryDescribeImageDenseAlg.hog(config,imageType);
		visualizers = new VisualizeHogCells(hog,true);
		visualizers.setLocalMax(false);

		add(imagePanel,BorderLayout.CENTER);

		imagePanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {
				int row = e.getY()/hog.getWidthCell();
				int col = e.getX()/hog.getWidthCell();

				if( row >= 0 && col >= 0 && row < hog.getCellRows() &&  col < hog.getCellCols() ) {
					DescribeDenseHogAlg.Cell c = hog.getCell(row,col);
					System.out.print("Cell["+row+" , "+col+"] histogram =");
					for (int i = 0; i < c.histogram.length; i++) {
						System.out.print("  "+c.histogram[i]);
					}
					System.out.println();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}
		});
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {
		hog.setInput(input);
		hog.process();

		work = visualizers.createOutputBuffered(work);

		Graphics2D g2 = work.createGraphics();
		g2.setColor(Color.BLACK);
		g2.fillRect(0,0,work.getWidth(),work.getHeight());

		visualizers.render(g2);

		imagePanel.setBufferedImage(work);
		imagePanel.setPreferredSize(new Dimension(work.getWidth(),work.getHeight()));
		imagePanel.setMinimumSize(new Dimension(work.getWidth(),work.getHeight()));
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<String>();

		examples.add(UtilIO.pathExample("shapes/shapes01.png"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("particles01.jpg"));
		ImageType imageType = ImageType.single(ImageUInt8.class);

		ConfigDenseHoG config = new ConfigDenseHoG();
		config.widthCell = 20;

		VisualizeImageHogApp app = new VisualizeImageHogApp(config, examples, imageType);

		app.openFile(new File(examples.get(0)));
		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Hog Descriptor Cell Visualization",true);
	}
}
