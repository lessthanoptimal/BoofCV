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

package boofcv.demonstrations.shapes;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase2;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public abstract class DetectBlackShapeAppBase<T extends ImageGray<T>> extends DemonstrationBase2
		implements ThresholdControlPanel.Listener
{
	protected Class<T> imageClass;

	protected DetectBlackShapePanel controls;

	ImageZoomPanel guiImage;

	protected InputToBinary<T> inputToBinary;

	BufferedImage original;
	BufferedImage work;
	GrayU8 binary = new GrayU8(1,1);

	public DetectBlackShapeAppBase(List<String> examples , Class<T> imageType) {
		super(examples, ImageType.single(imageType));
		this.imageClass = imageType;
	}

	protected void setupGui(ImageZoomPanel guiImage, DetectBlackShapePanel controls) {
		this.guiImage = guiImage;
		this.controls = controls;

		guiImage.setPreferredSize(new Dimension(800,800));

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, guiImage);

		createDetector(true);

		guiImage.addMouseWheelListener(new MouseAdapter() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {

				double curr = DetectBlackShapeAppBase.this.controls.zoom;

				if( e.getWheelRotation() > 0 )
					curr *= 1.1;
				else
					curr /= 1.1;

				DetectBlackShapeAppBase.this.controls.setZoom(curr);
			}
		});
	}

	protected abstract void createDetector( boolean initializing );

	@Override
	protected void handleInputChange( int source , InputMethod method , final int width , final int height ) {
		// reset the scaling and ensure the entire new image is visible
		BoofSwingUtil.invokeNowOrLater(new Runnable() {
			@Override
			public void run() {
				double zoom = BoofSwingUtil.selectZoomToShowAll(guiImage,width,height);
				controls.setZoom(zoom);
				controls.setImageSize(width,height);
				milliBinary = 0;
			}
		});
	}

	double milliBinary = 0;

	@Override
	public void processImage(int sourceID, long frameID, final BufferedImage buffered, ImageBase input) {
		System.out.flush();

		original = ConvertBufferedImage.checkCopy(buffered,original);
		work = ConvertBufferedImage.checkDeclare(buffered,work);

		binary.reshape(work.getWidth(), work.getHeight());

		final double timeInSeconds;
		synchronized (this) {
			long before = System.nanoTime();
			inputToBinary.process((T)input, binary);
			long middle = System.nanoTime();

			double a = (middle-before)*1e-6;
			if( milliBinary == 0 ) {
				milliBinary = a;
			} else {
				milliBinary = 0.95*milliBinary+0.05*a;
			}
			System.out.printf(" binary %7.2f ",milliBinary);

			detectorProcess((T)input, binary);
			long after = System.nanoTime();
			timeInSeconds = (after-before)*1e-9;
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				controls.setProcessingTime(timeInSeconds);
				viewUpdated();
			}
		});
	}

	protected abstract void detectorProcess(T input , GrayU8 binary );

	/**
	 * Called when how the data is visualized has changed
	 */
	public void viewUpdated() {
		BufferedImage active = null;
		if( controls.selectedView == 0 ) {
			active = original;
		} else if( controls.selectedView == 1 ) {
			VisualizeBinaryData.renderBinary(binary,false,work);
			active = work;
			work.setRGB(0, 0, work.getRGB(0, 0)); // hack so that Swing knows it's been modified
		} else {
			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,work.getWidth(),work.getHeight());
			active = work;
		}

		guiImage.setBufferedImage(active);
		guiImage.setScale(controls.zoom);

		guiImage.repaint();
	}
}
