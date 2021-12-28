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

import boofcv.abst.feature.dense.DescribeImageDenseHoG;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.factory.feature.dense.FactoryDescribeImageDense;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.io.UtilIO;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageType;
import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I32;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Displays an image and lets the user click on it to show the closest HOG descriptor
 * at that location.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class VisualizeHogDescriptorApp<T extends ImageBase<T>> extends DemonstrationBase {
	ControlHogDescriptorPanel controlPanel = new ControlHogDescriptorPanel(this);
	VisualizePanel imagePanel = new VisualizePanel();

	ConfigDenseHoG config = new ConfigDenseHoG();
	final Object hogLock = new Object();
	DescribeImageDenseHoG<T> hog;

	Color[] colors;
	float[] cos, sin;

	@Nullable Point2D_I32 selectedPixel;
	int selectedIndex;
	@Nullable TupleDesc_F64 targetDesc;
	@Nullable Point2D_I32 targetLocation;

	T input;

	public VisualizeHogDescriptorApp( List<String> exampleInputs, ImageType<T> imageType ) {
		super(exampleInputs, imageType);


		add(controlPanel, BorderLayout.WEST);
		add(imagePanel, BorderLayout.CENTER);

		config.pixelsPerCell = 20;
		config.cellsPerBlockY = 5;

		updateDescriptor();

		setColors(controlPanel.doShowLog);

		imagePanel.setScaling(ScaleOptions.NONE);
		imagePanel.setCentering(false);
		imagePanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				selectRegion(e.getX(), e.getY());
			}
		});
	}

	private void setColors( boolean logIntensity ) {
		Color colors[] = new Color[256];
		if (logIntensity) {
			double k = 255.0/Math.log(255);
			for (int i = 0; i < colors.length; i++) {
				int v = (int)(k*Math.log(i + 1));
				colors[i] = new Color(v, v, v);
			}
		} else {
			for (int i = 0; i < colors.length; i++) {
				colors[i] = new Color(i, i, i);
			}
		}
		this.colors = colors;
	}

	private void selectRegion( int x, int y ) {
		int bestIndex = -1;
		synchronized (hogLock) {
			double bestDistance = Double.MAX_VALUE;
			List<Point2D_I32> locations = hog.getLocations();
			for (int i = 0; i < locations.size(); i++) {
				Point2D_I32 p = locations.get(i);
				double d = UtilPoint2D_I32.distance(p.x, p.y, x, y);
				if (d < bestDistance) {
					bestDistance = d;
					bestIndex = i;
				}
			}

			if (bestIndex >= 0) {
				selectedPixel = new Point2D_I32(x, y);
				selectedIndex = bestIndex;
				targetDesc = hog.getDescriptions().get(bestIndex);
				targetLocation = hog.getLocations().get(bestIndex);
				imagePanel.repaint();
			}
		}
	}

	private void updateDescriptor() {
		synchronized (hogLock) {
			hog = (DescribeImageDenseHoG<T>)FactoryDescribeImageDense.hog(config, (ImageType)getImageType(0));
		}

		int numAngles = config.orientationBins;
		cos = new float[numAngles];
		sin = new float[numAngles];

		for (int i = 0; i < numAngles; i++) {
			double theta = Math.PI*(i + 0.5)/numAngles;

			cos[i] = (float)Math.cos(theta);
			sin[i] = (float)Math.sin(theta);
		}
	}

	@Override
	protected void handleInputChange( int source, InputMethod method, int width, int height ) {
		super.handleInputChange(source, method, width, height);

		imagePanel.setPreferredSize(new Dimension(width, height));
		imagePanel.setMinimumSize(new Dimension(width, height));
	}

	@Override
	public void processImage( int sourceID, long frameID, BufferedImage buffered, ImageBase input ) {
		boolean inputSizeChanged = false;
		synchronized (hogLock) {
			inputSizeChanged =
					this.input == null || this.input.width != input.width || this.input.height != input.height;
			this.input = (T)input;
			hog.process((T)input);

			if (inputSizeChanged) {
				targetDesc = null;
				targetLocation = null;
				selectedPixel = null;
			} else {
				if (isRegionSelected()) {
					targetDesc = hog.getDescriptions().get(selectedIndex);
					targetLocation = hog.getLocations().get(selectedIndex);
				}
			}
		}

		imagePanel.setImage(buffered);
		imagePanel.repaint();
	}

	private boolean isRegionSelected() {
		return targetDesc != null;
	}

	private void renderHog( int bcx, int bcy,
							TupleDesc_F64 desc,
							Graphics2D g2 ) {

		Line2D.Float line = new Line2D.Float();

		int gridWidth = config.pixelsPerCell*config.cellsPerBlockX;
		int gridHeight = config.pixelsPerCell*config.cellsPerBlockY;

		int tl_x = bcx - gridWidth/2;
		int tl_y = bcy - gridHeight/2;

		g2.setColor(Color.BLACK);
		g2.fillRect(tl_x, tl_y, gridWidth, gridHeight);

		double maxValue = 0;
		for (int i = 0; i < desc.data.length; i++) {
			maxValue = Math.max(maxValue, desc.data[i]);
		}

		float foo = config.pixelsPerCell/2.0f;

		Color[] colors = this.colors; // safety just in case it changes in another thread
		int index = 0;
		for (int cellY = 0; cellY < config.cellsPerBlockY; cellY++) {
			for (int cellX = 0; cellX < config.cellsPerBlockX; cellX++) {
				int c_x = tl_x + (int)((cellX + 0.5)*config.pixelsPerCell);
				int c_y = tl_y + (int)((cellY + 0.5)*config.pixelsPerCell);

				for (int i = 0; i < config.orientationBins; i++) {
					int a = (int)(255.0f*desc.data[index++]/maxValue);
					g2.setColor(colors[a]);

					float x0 = c_x - foo*cos[i];
					float x1 = c_x + foo*cos[i];
					float y0 = c_y - foo*sin[i];
					float y1 = c_y + foo*sin[i];

					line.setLine(x0, y0, x1, y1);
					g2.draw(line);
				}
			}
		}

		if (controlPanel.doShowGrid) {
			g2.setColor(Color.RED);
			for (int cellY = 0; cellY <= config.cellsPerBlockY; cellY++) {
				int y = tl_y + cellY*config.pixelsPerCell;
				g2.drawLine(tl_x, y, tl_x + gridWidth, y);
			}
			for (int cellX = 0; cellX <= config.cellsPerBlockX; cellX++) {
				int x = tl_x + cellX*config.pixelsPerCell;
				g2.drawLine(x, tl_y, x, tl_y + gridHeight);
			}
		}
	}

	private class VisualizePanel extends ImagePanel {
		@Override
		public void paintComponent( Graphics g ) {
			super.paintComponent(g);

			synchronized (hogLock) {
				if (isRegionSelected()) {
					Objects.requireNonNull(targetLocation);
					renderHog(targetLocation.x, targetLocation.y, Objects.requireNonNull(targetDesc), (Graphics2D)g);
				}
			}
		}
	}

	public void visualsChanged() {
		setColors(controlPanel.doShowLog);
		imagePanel.repaint();
	}

	public void configChanged() {
		config.pixelsPerCell = controlPanel.cellWidth;
		config.cellsPerBlockX = controlPanel.gridX;
		config.cellsPerBlockY = controlPanel.gridY;
		config.fastVariant = controlPanel.fast;
		config.orientationBins = controlPanel.histogram;

		synchronized (hogLock) {
			updateDescriptor();
			hog.process(input);
		}

		synchronized (hogLock) {
			if (selectedPixel != null) {
				selectRegion(selectedPixel.x, selectedPixel.y);
			}
		}
	}

	public static void main( String[] args ) {
		List<String> examples = new ArrayList<>();

		examples.add(UtilIO.pathExample("segment/berkeley_horses.jpg"));
		examples.add(UtilIO.pathExample("segment/berkeley_man.jpg"));
		examples.add(UtilIO.pathExample("shapes/shapes01.png"));
		examples.add(UtilIO.pathExample("shapes/shapes02.png"));
		ImageType imageType = ImageType.single(GrayF32.class);

		VisualizeHogDescriptorApp app = new VisualizeHogDescriptorApp(examples, imageType);

		app.openFile(new File(examples.get(0)));
		app.display("Hog Descriptor Visualization");
	}
}
