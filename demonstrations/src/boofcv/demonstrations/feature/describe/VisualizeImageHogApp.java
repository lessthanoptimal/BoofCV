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

	Object lock = new Object();

	ControlHogPanel control = new ControlHogPanel(this);

	boolean showInput = false;

	public VisualizeImageHogApp(List<String> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		createHoG(imageType);
		visualizers = new VisualizeHogCells(hog);
		visualizers.setShowLog(control.doShowLog);
		visualizers.setLocalMax(control.doShowLocal);
		visualizers.setShowGrid(control.doShowGrid);

		add(imagePanel,BorderLayout.CENTER);
		add(control,BorderLayout.WEST);

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

	private void createHoG(ImageType<T> imageType) {
		ConfigDenseHoG config = new ConfigDenseHoG();
		config.widthCell = control.cellWidth;
		config.orientationBins = control.histogram;

		hog = FactoryDescribeImageDenseAlg.hog(config,imageType);
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {
		synchronized (lock) {
			hog.setInput(input);
			hog.process();

			work = visualizers.createOutputBuffered(work);

			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, work.getWidth(), work.getHeight());

			visualizers.render(g2);

			if( showInput )
				g2.drawImage(buffered,0,0,buffered.getWidth()/5,buffered.getHeight()/5,null);
		}

		imagePanel.setBufferedImage(work);
		imagePanel.setPreferredSize(new Dimension(work.getWidth(),work.getHeight()));
		imagePanel.setMinimumSize(new Dimension(work.getWidth(),work.getHeight()));
	}

	public void setCellWidth( int width ) {
		synchronized (lock) {
			hog.setWidthCell(width);
			reprocessSingleImage();
		}
	}

	public void setHistogram(int histogram) {
		synchronized (lock) {
			createHoG(imageType);
			visualizers.setHoG(hog);
			reprocessSingleImage();
		}
	}

	public void setShowGrid(boolean showGrid) {
		visualizers.showGrid = showGrid;
		reprocessSingleImage();
	}

	public void setShowLocal(boolean show ) {
		visualizers.localMax = show;
		reprocessSingleImage();
	}

	public void setShowLog(boolean show ) {
		visualizers.setShowLog(show);
		reprocessSingleImage();
	}

	public void setShowInput( boolean show ) {
		this.showInput = show;
		reprocessSingleImage();
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<String>();

		examples.add(UtilIO.pathExample("shapes/shapes01.png"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("particles01.jpg"));
		ImageType imageType = ImageType.single(ImageUInt8.class);

		VisualizeImageHogApp app = new VisualizeImageHogApp(examples, imageType);

		app.openFile(new File(examples.get(0)));
		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Hog Descriptor Cell Visualization",true);
	}
}
