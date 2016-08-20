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

import boofcv.abst.feature.dense.DescribeImageDenseHoG;
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
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays an image and lets the user click on it to show the closest HOG descriptor
 * at that location.
 *
 * @author Peter Abeles
 */
// TODO render rectangle over selected descriptor
// TODO select region with mouse
// TODO draw descriptor in a new window which can be resized
// TODO adjust HOG settings in GUI
public class VisualizeHogDescriptorApp<T extends ImageBase> extends DemonstrationBase<T>
{
	ImagePanel imagePanel = new ImagePanel();

	ConfigDenseHoG config = new ConfigDenseHoG();
	DescribeImageDenseHoG<T> hog;

	Object imageLock = new Object();

	Color colors[];
	float cos[],sin[];


	public VisualizeHogDescriptorApp(List<String> exampleInputs, ImageType<T> imageType) {
		super(exampleInputs, imageType);

		colors = new Color[256];
		for (int i = 0; i < colors.length; i++) {
			colors[i] = new Color(i, i, i);
		}

		add(imagePanel,BorderLayout.CENTER);

		config.pixelsPerCell = 20;
		config.cellsPerBlockY = 5;

		updateDescriptor();
	}

	private void updateDescriptor() {
		hog = (DescribeImageDenseHoG<T>)FactoryDescribeImageDense.hog(config,imageType);

		int numAngles = config.orientationBins;
		cos = new float[numAngles];
		sin = new float[numAngles];

		for (int i = 0; i < numAngles; i++) {
			double theta = Math.PI*(i+0.5)/numAngles;

			cos[i] = (float)Math.cos(theta);
			sin[i] = (float)Math.sin(theta);
		}
	}

	@Override
	public void processImage(BufferedImage buffered, T input) {
		hog.process(input);

		Graphics2D g2 = buffered.createGraphics();

		List<TupleDesc_F64> descriptions = hog.getDescriptions();
		List<Point2D_I32> locations = hog.getLocations();

		int N = descriptions.size()/2;

		TupleDesc_F64 desc = descriptions.get(N);
		Point2D_I32 location = locations.get(N);

		renderHog(location.x,location.y,desc,g2);

		imagePanel.setBufferedImage(buffered);
		imagePanel.setPreferredSize(new Dimension(buffered.getWidth(),buffered.getHeight()));
		imagePanel.setMinimumSize(new Dimension(buffered.getWidth(),buffered.getHeight()));

	}

	private void renderHog(int bcx , int bcy ,
						   TupleDesc_F64 desc ,
						   Graphics2D g2 ) {

		Line2D.Float line = new Line2D.Float();

		int gridWidth = config.pixelsPerCell*config.cellsPerBlockX;
		int gridHeight = config.pixelsPerCell*config.cellsPerBlockY;

		int tl_x = bcx - gridWidth/2;
		int tl_y = bcy - gridHeight/2;

		g2.setColor(Color.BLACK);
		g2.fillRect(tl_x,tl_y,gridWidth,gridHeight);

		float foo = config.pixelsPerCell/2.0f;

		int index = 0;
		for (int cellY = 0; cellY < config.cellsPerBlockY; cellY++) {
			for (int cellX = 0; cellX < config.cellsPerBlockX; cellX++) {
				int c_x = tl_x + (int)((cellX+0.5)*config.pixelsPerCell);
				int c_y = tl_y + (int)((cellY+0.5)*config.pixelsPerCell);

				for (int i = 0; i < config.orientationBins; i++) {
					int a = (int) (255.0f * desc.value[index++]);
					g2.setColor(colors[a]);

					float x0 = c_x - foo * cos[i];
					float x1 = c_x + foo * cos[i];
					float y0 = c_y - foo * sin[i];
					float y1 = c_y + foo * sin[i];

					line.setLine(x0, y0, x1, y1);
					g2.draw(line);
				}
			}
		}
	}

	public static void main(String[] args) {
		List<String> examples = new ArrayList<String>();

		examples.add(UtilIO.pathExample("shapes/shapes01.png"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		examples.add(UtilIO.pathExample("segment/berkeley_horses.jpg"));
		examples.add(UtilIO.pathExample("segment/berkeley_man.jpg"));
		ImageType imageType = ImageType.single(GrayF32.class);

		VisualizeHogDescriptorApp app = new VisualizeHogDescriptorApp(examples, imageType);

		app.openFile(new File(examples.get(0)));
		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app, "Hog Descriptor Visualization",true);

	}
}
