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

package boofcv.demonstrations.shapes;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageBase;
import boofcv.struct.image.ImageGray;
import boofcv.struct.image.ImageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

@SuppressWarnings({"NullAway.Init"})
public abstract class DetectBlackShapeAppBase<T extends ImageGray<T>> extends DemonstrationBase
		implements ThresholdControlPanel.Listener {
	protected Class<T> imageClass;

	protected DetectBlackShapePanel controls;

	protected ImageZoomPanel guiImage;

	protected InputToBinary<T> inputToBinary;

	// Lock provided for the BufferedImagesbelow
	protected final Object bufferedImageLock = new Object();
	protected BufferedImage original;
	protected BufferedImage work;
	protected GrayU8 binary = new GrayU8(1, 1);

	// how many input images have been saved to disk
	protected int saveCounter = 0;
	protected boolean saveRequested = false;

	protected DetectBlackShapeAppBase( List<String> examples, Class<T> imageType ) {
		super(examples, ImageType.single(imageType));
		this.imageClass = imageType;


		JMenuItem menuSaveInput = new JMenuItem("Save Input");
		menuSaveInput.addActionListener(e -> requestSaveInputImage());
		BoofSwingUtil.setMenuItemKeys(menuSaveInput, KeyEvent.VK_S, KeyEvent.VK_Y);

		JMenu menu = new JMenu("Data");
		menu.setMnemonic(KeyEvent.VK_D);
		menu.add(menuSaveInput);
		menuBar.add(menu);
	}

	protected void setupGui( ImageZoomPanel guiImage, DetectBlackShapePanel controls ) {
		this.guiImage = guiImage;
		this.controls = controls;

		this.guiImage.autoScaleCenterOnSetImage = false;
		guiImage.setPreferredSize(new Dimension(800, 800));

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, guiImage);

		createDetector(true);

		guiImage.setListener(scale -> {
			DetectBlackShapeAppBase.this.controls.setZoom(scale);
		});

		guiImage.getImagePanel().addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e ) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (inputMethod == InputMethod.VIDEO) {
						streamPaused = !streamPaused;
					}
				}
			}
		});
	}

	protected abstract void createDetector( boolean initializing );

	@Override
	protected void handleInputChange( int source, InputMethod method, final int width, final int height ) {
		// reset the scaling and ensure the entire new image is visible
		BoofSwingUtil.invokeNowOrLater(() -> {
			double zoom = BoofSwingUtil.selectZoomToShowAll(guiImage, width, height);
			controls.setImageSize(width, height);
			controls.setZoom(zoom);
			milliBinary = 0;
			guiImage.setScale(zoom);
			guiImage.updateSize(width, height);
			guiImage.getHorizontalScrollBar().setValue(0);
			guiImage.getVerticalScrollBar().setValue(0);
		});
	}

	double milliBinary = 0;

	@Override
	public void processImage( int sourceID, long frameID, final BufferedImage buffered, ImageBase input ) {
		System.out.flush();

		synchronized (bufferedImageLock) {
			original = ConvertBufferedImage.checkCopy(buffered, original);
			work = ConvertBufferedImage.checkDeclare(buffered, work);
		}

		if (saveRequested) {
			saveInputImage();
			saveRequested = false;
		}

		binary.reshape(work.getWidth(), work.getHeight());

		final double timeInSeconds;
		synchronized (this) {
			long before = System.nanoTime();
			inputToBinary.process((T)input, binary);
			long middle = System.nanoTime();

			double a = (middle - before)*1e-6;
			if (milliBinary == 0) {
				milliBinary = a;
			} else {
				milliBinary = 0.95*milliBinary + 0.05*a;
			}
//			System.out.printf(" binary %7.2f ",milliBinary);

			detectorProcess((T)input, binary);
			long after = System.nanoTime();
			timeInSeconds = (after - before)*1e-9;
		}

		SwingUtilities.invokeLater(() -> {
			controls.setProcessingTimeS(timeInSeconds);
			viewUpdated();
		});
	}

	protected abstract void detectorProcess( T input, GrayU8 binary );

	/**
	 * Makes a request that the input image be saved. This request might be carried out immediately
	 * or when then next image is processed.
	 */
	public void requestSaveInputImage() {
		saveRequested = false;
		switch (inputMethod) {
			case IMAGE:
				new Thread(() -> saveInputImage()).start();
				break;

			case VIDEO:
			case WEBCAM:
				if (streamPaused) {
					saveInputImage();
				} else {
					saveRequested = true;
				}
				break;

			default:
				break;
		}
	}

	protected void saveInputImage() {
		synchronized (bufferedImageLock) {
			String fileName = String.format("saved_input%03d.png", saveCounter++);
			System.out.println("Input image saved to " + fileName);
			UtilImageIO.saveImage(original, fileName);
		}
	}

	/**
	 * Called when how the data is visualized has changed
	 */
	public void viewUpdated() {
		BufferedImage active;
		synchronized (bufferedImageLock) {
			if (controls.selectedView == 0) {
				active = original;
			} else if (controls.selectedView == 1) {
				VisualizeBinaryData.renderBinary(binary, false, work);
				active = work;
				work.setRGB(0, 0, work.getRGB(0, 0)); // hack so that Swing knows it's been modified
			} else {
				Graphics2D g2 = work.createGraphics();
				g2.setColor(Color.BLACK);
				g2.fillRect(0, 0, work.getWidth(), work.getHeight());
				active = work;
			}
		}

		if (active != guiImage.getImage())
			guiImage.setImage(active);
		guiImage.setScale(controls.zoom);

		guiImage.repaint();
	}
}
