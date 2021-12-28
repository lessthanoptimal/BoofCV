/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.feature.dense.DescribeDenseHogFastAlg;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDenseAlg;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.io.UtilIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * App which shows histogram for each cell in HOG descriptor.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeImageHogCellApp<T extends ImageBase<T>> extends DemonstrationBase {

	// use the fast variant since it confidently computes each cell individually and doesn't normalize it
	DescribeDenseHogFastAlg<T> hog;
	VisualizeHogCells visualizers;

	ImagePanel imagePanel = new ImagePanel();

	BufferedImage work;

	final Object lock = new Object();

	ControlHogCellPanel control = new ControlHogCellPanel(this);

	boolean showInput = false;

	public VisualizeImageHogCellApp( List<String> exampleInputs, ImageType<T> imageType ) {
		super(exampleInputs, imageType);

		createHoG(imageType);
		visualizers = new VisualizeHogCells(hog);
		visualizers.setShowLog(control.doShowLog);
		visualizers.setLocalMax(control.doShowLocal);
		visualizers.setShowGrid(control.doShowGrid);

		add(imagePanel, BorderLayout.CENTER);
		add(control, BorderLayout.WEST);

		imagePanel.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked( MouseEvent e ) {}

			@Override
			public void mousePressed( MouseEvent e ) {
				int row = e.getY()/hog.getPixelsPerCell();
				int col = e.getX()/hog.getPixelsPerCell();

				if (row >= 0 && col >= 0 && row < hog.getCellRows() && col < hog.getCellCols()) {
					DescribeDenseHogFastAlg.Cell c = hog.getCell(row, col);
					System.out.print("Cell[" + row + " , " + col + "] histogram =");
					for (int i = 0; i < c.histogram.length; i++) {
						System.out.print("  " + c.histogram[i]);
					}
					System.out.println();
				}
			}

			@Override
			public void mouseReleased( MouseEvent e ) {}

			@Override
			public void mouseEntered( MouseEvent e ) {}

			@Override
			public void mouseExited( MouseEvent e ) {}
		});
	}

	private void createHoG( ImageType<T> imageType ) {
		ConfigDenseHoG config = new ConfigDenseHoG();
		config.pixelsPerCell = control.cellWidth;
		config.orientationBins = control.histogram;

		hog = FactoryDescribeImageDenseAlg.hogFast(config, imageType);
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);


		imagePanel.setPreferredSize(new Dimension(width, height));
		imagePanel.setMinimumSize(new Dimension(width, height));
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		synchronized (lock) {
			hog.setInput((T)input);
			hog.process();

			work = visualizers.createOutputBuffered(work);

			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, work.getWidth(), work.getHeight());

			visualizers.render(g2);

			if (showInput)
				g2.drawImage(buffered, 0, 0, buffered.getWidth()/5, buffered.getHeight()/5, null);
		}

		imagePanel.setImage(work);
		imagePanel.repaint();
	}

	public void setCellWidth( int width ) {
		synchronized (lock) {
			createHoG((ImageType)getImageType(0));
			visualizers.setHoG(hog);
			reprocessImageOnly();
		}
	}

	public void setOrientationBins( int histogram ) {
		synchronized (lock) {
			createHoG((ImageType)getImageType(0));
			visualizers.setHoG(hog);
			reprocessImageOnly();
		}
	}

	public void setShowGrid( boolean showGrid ) {
		visualizers.showGrid = showGrid;
		reprocessImageOnly();
	}

	public void setShowLocal( boolean show ) {
		visualizers.localMax = show;
		reprocessImageOnly();
	}

	public void setShowLog( boolean show ) {
		visualizers.setShowLog(show);
		reprocessImageOnly();
	}

	public void setShowInput( boolean show ) {
		this.showInput = show;
		reprocessImageOnly();
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();

		examples.add(UtilIO.pathExample("shapes/shapes01.png"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("segment/berkeley_horses.jpg"));
		examples.add(UtilIO.pathExample("segment/berkeley_man.jpg"));
		ImageType imageType = ImageType.single(GrayF32.class);

		VisualizeImageHogCellApp app = new VisualizeImageHogCellApp(examples, imageType);

		app.openFile(new File(examples.get(0)));
		app.display("Hog Descriptor Unnormalized Cell");
	}
}
