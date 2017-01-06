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
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.ellipse.BinaryEllipseDetector;
import boofcv.factory.filter.binary.ConfigThreshold;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.*;
import georegression.struct.shapes.EllipseRotated_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
public class DetectBlackEllipseApp<T extends ImageGray<T>> extends DemonstrationBase<T>
		implements ThresholdControlPanel.Listener
{

	Class<T> imageClass;

	DetectEllipseControlPanel controls = new DetectEllipseControlPanel(this);

	VisualizePanel guiImage;

	InputToBinary<T> inputToBinary;
	BinaryEllipseDetector<T> detector;

	BufferedImage original;
	BufferedImage work;
	T inputPrev;
	GrayU8 binary = new GrayU8(1,1);


	public DetectBlackEllipseApp(List<String> examples , Class<T> imageType) {
		super(examples, ImageType.single(imageType));
		this.imageClass = imageType;

		guiImage = new VisualizePanel();

		add(BorderLayout.WEST, controls);
		add(BorderLayout.CENTER, guiImage);

		inputPrev = super.defaultType.createImage(1,1);

		createDetector();
	}

	private synchronized void createDetector() {
		detector = FactoryShapeDetector.ellipse(controls.getConfigEllipse(), imageClass);
		imageThresholdUpdated();
	}

	@Override
	public synchronized void processImage(int sourceID, long frameID, final BufferedImage buffered, ImageBase input) {
		if( buffered != null ) {
			original = ConvertBufferedImage.checkCopy(buffered,original);
			work = ConvertBufferedImage.checkCopy(buffered,work);

			this.original.createGraphics().drawImage(buffered,0,0,null);

			binary.reshape(work.getWidth(), work.getHeight());
			inputPrev.setTo((T)input);

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					Dimension d = guiImage.getPreferredSize();
					if( d.getWidth() < buffered.getWidth() || d.getHeight() < buffered.getHeight() ) {
						guiImage.setPreferredSize(new Dimension(buffered.getWidth(), buffered.getHeight()));
					}
				}});
		} else {
			input = inputPrev;
		}

		inputToBinary.process((T)input, binary);
		detector.process((T)input, binary);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				viewUpdated();
			}
		});
	}

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
			work.setRGB(0, 0, work.getRGB(0, 0));
		} else {
			Graphics2D g2 = work.createGraphics();
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,work.getWidth(),work.getHeight());
			active = work;
		}

		guiImage.setScale(controls.zoom);

		guiImage.setBufferedImage(active);
		guiImage.repaint();
	}

	public void configUpdate() {
		createDetector();
		// does process and render too
	}

	@Override
	public synchronized void imageThresholdUpdated() {

		ConfigThreshold config = controls.getThreshold().createConfig();

		inputToBinary = FactoryThresholdBinary.threshold(config, imageClass);
		processImageThread(0,0,null,null);
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran, Graphics2D g2) {
			synchronized ( DetectBlackEllipseApp.this ) {
				g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

				if (controls.bShowContour) {
					List<Contour> contours = detector.getAllContours();
					g2.setStroke(new BasicStroke(1));
					VisualizeBinaryData.render(contours, null,Color.CYAN, scale, g2);
				}

				if (controls.bShowEllipses) {
					List<EllipseRotated_F64> ellipses = detector.getFoundEllipses().toList();

					g2.setColor(Color.RED);
					g2.setStroke(new BasicStroke(3));

					for (EllipseRotated_F64 p : ellipses) {
						VisualizeShapes.drawEllipse(p,scale,g2);
					}
				}
			}
		}
	}

	public static void main(String[] args) {

		List<String> examples = new ArrayList<>();
		examples.add("fiducial/circle_asymmetric/image01.jpg");
		examples.add("shapes/polygons01.jpg");
		examples.add("shapes/shapes01.png");
		examples.add("shapes/shapes02.png");
		examples.add("fiducial/circle_asymmetric/image00.jpg");
		examples.add("fiducial/circle_asymmetric/movie.mp4");

		DetectBlackEllipseApp app = new DetectBlackEllipseApp(examples,GrayF32.class);

		app.openFile(new File(examples.get(0)));

		app.waitUntilDoneProcessing();

		ShowImages.showWindow(app,"Detect Black Ellipses",true);
	}



}
