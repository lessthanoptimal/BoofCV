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

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I32;

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

	DescribeImageDense<T,TupleDesc_F64> hog;
	ConfigDenseHoG config = new ConfigDenseHoG();
	VisualizeHogCells visualizers;

	ImagePanel imagePanel = new ImagePanel();

	BufferedImage work;

	Object lock = new Object();

	ControlHogPanel control = new ControlHogPanel(this);

	boolean showInput = false;

	public VisualizeImageHogApp(List<String> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		createHoG(imageType);
		visualizers = new VisualizeHogCells(hog,config);
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
				List<TupleDesc_F64> descriptions = hog.getDescriptions();
				List<Point2D_I32> locations = hog.getLocations();

				double bestDistance = Double.MAX_VALUE;
				Point2D_I32 bestPt = null;
				TupleDesc_F64 bestDesc = null;

				int cellClickX = e.getX()/config.widthCell;
				int cellClickY = e.getY()/config.widthCell;

				for (int i = 0; i < locations.size(); i++) {
					Point2D_I32 p = locations.get(i);

					// go from center to lower extent
					int x = p.x - (config.widthCell*config.widthBlock)/2;
					int y = p.y - (config.widthCell*config.widthBlock)/2;

					int d = UtilPoint2D_I32.distanceSq(x/config.widthCell,y/config.widthCell,
							cellClickX, cellClickY);
					if( d < bestDistance ) {
						bestDistance = d;
						bestPt = p;
						bestDesc = descriptions.get(i);
					}
				}

				if( bestDesc != null ) {
					System.out.println("location = "+bestPt.x+"  "+bestPt.y);
					int numAngles = config.orientationBins;
					int cellIndex = 0;
					for (int cellRow = 0; cellRow < config.widthBlock; cellRow++) {
						for (int cellCol = 0; cellCol < config.widthBlock; cellCol++, cellIndex++ ) {
							int start = cellIndex*numAngles;

							System.out.printf("cell[%2d] = [ ",cellIndex);
							for (int i = 0; i < numAngles; i++) {
								System.out.printf("%f ",bestDesc.value[start+i]);
							}
							System.out.println(" ]");
						}
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
		config.orientationBins = control.histogram;
		config.widthCell = control.cellWidth;
		config.fastVariant = control.doUseFast;
		config.widthBlock = 1;
		config.stepBlock = 1;

		hog = FactoryDescribeImageDense.hog(config,imageType);
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {
		synchronized (lock) {
			hog.process(input);

			work = new BufferedImage(buffered.getWidth(),buffered.getHeight(), BufferedImage.TYPE_INT_RGB);

			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0, 0, work.getWidth(), work.getHeight());

			visualizers.render(g2, buffered.getWidth(),buffered.getHeight());

			if( showInput )
				g2.drawImage(buffered,0,0,buffered.getWidth()/5,buffered.getHeight()/5,null);
		}

		imagePanel.setBufferedImage(work);
		imagePanel.setPreferredSize(new Dimension(work.getWidth(),work.getHeight()));
		imagePanel.setMinimumSize(new Dimension(work.getWidth(),work.getHeight()));
	}

	public void setCellWidth( int width ) {
		synchronized (lock) {
			config.widthCell = width;
			createHoG(imageType);
			visualizers.setHoG(hog,config);
			reprocessSingleImage();
		}
	}

	public void setOrientationBins(int numBins) {
		synchronized (lock) {
			config.orientationBins = numBins;
			createHoG(imageType);
			visualizers.setHoG(hog,config);
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

	public void setUseFast( boolean useFast ) {
		synchronized (lock) {
			config.fastVariant = useFast;
			createHoG(imageType);
			visualizers.setHoG(hog,config);
			reprocessSingleImage();
		}
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<String>();

		examples.add(UtilIO.pathExample("shapes/shapes01.png"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("segment/berkeley_horses.jpg"));
		examples.add(UtilIO.pathExample("segment/berkeley_man.jpg"));
		ImageType imageType = ImageType.single(GrayF32.class);

		VisualizeImageHogApp app = new VisualizeImageHogApp(examples, imageType);

		app.openFile(new File(examples.get(0)));
		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Hog Descriptor Cell Visualization",true);
	}
}
