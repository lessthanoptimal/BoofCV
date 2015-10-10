/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.shape;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.shapes.polygon.BinaryPolygonConvexDetector;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.factory.filter.binary.FactoryThresholdBinary;
import boofcv.factory.shape.FactoryShapeDetector;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

// todo minimum contour size
// todo video support
// todo webcam support

/**
 * Application which lets you configure the black polygon detector in real-time
 *
 * @author Peter Abeles
 */
public class DetectBlackPolygonApp<T extends ImageSingleBand> extends JPanel
		implements ThresholdControlPanel.Listener, ActionListener
{

	Class<T> imageType;

	DetectPolygonControlPanel controls = new DetectPolygonControlPanel(this);

	ImagePanel guiImage;

	InputToBinary<T> inputToBinary;
	BinaryPolygonConvexDetector<T> detector;

	BufferedImage original;
	BufferedImage work;
	T input;
	ImageUInt8 binary = new ImageUInt8(1,1);

	final JFileChooser fc = new JFileChooser();

	// menu items
	JMenuItem menuFile,menuWebcam,menuQuit;

	public DetectBlackPolygonApp(Class<T> imageType) {
		setLayout(new BorderLayout());

		this.imageType = imageType;

		guiImage = new ImagePanel();

		add(BorderLayout.WEST,controls);
		add(BorderLayout.CENTER,guiImage);
		createMenuBar();

		createDetector();
	}

	private void createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu menu = new JMenu("File");
		menuBar.add(menu);

		menuFile = new JMenuItem("Open File", KeyEvent.VK_F);
		menuFile.addActionListener(this);
		menuFile.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		menuWebcam = new JMenuItem("Open Webcam", KeyEvent.VK_W);
		menuWebcam.addActionListener(this);
		menuWebcam.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_W, ActionEvent.CTRL_MASK));
		menuQuit = new JMenuItem("Quit", KeyEvent.VK_Q);
		menuQuit.addActionListener(this);
		menuQuit.setAccelerator(KeyStroke.getKeyStroke(
				KeyEvent.VK_Q, ActionEvent.CTRL_MASK));

		menu.add(menuFile);
		menu.add(menuWebcam);
		menu.addSeparator();
		menu.add(menuQuit);

		add(BorderLayout.NORTH,menuBar);
	}

	private synchronized void createDetector() {
		detector = FactoryShapeDetector.polygon(controls.getConfig(),imageType);
		if( input == null )
			input = GeneralizedImageOps.createSingleBand(detector.getInputType(),1,1);
		imageThresholdUpdated();
	}

	private synchronized void processImage() {
		inputToBinary.process(input, binary);
		detector.process(input, binary);
	}

	public synchronized void setInput( BufferedImage image ) {
		this.original = image;
		work = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_BGR);

		input.reshape(work.getWidth(), work.getHeight());
		binary.reshape(work.getWidth(), work.getHeight());

		ConvertBufferedImage.convertFrom(original, input, true);

		guiImage.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
		processImage();
		renderImage();
	}

	private synchronized void renderImage() {
		if( work == null )
			return;

		Graphics2D g2 = work.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if( controls.selectedView == 0 ) {
			g2.drawImage(original, 0, 0, null);
		} else if( controls.selectedView == 1 ) {
			VisualizeBinaryData.renderBinary(binary,false,work);
		} else {
			g2.setColor(Color.BLACK);
			g2.fillRect(0,0,work.getWidth(),work.getHeight());
		}

		work.setRGB(0, 0, work.getRGB(0, 0));

		if (controls.bShowContour) {
			List<Contour> contours = detector.getAllContours();

			VisualizeBinaryData.renderExternal(contours, new Color(0xFF00FF), work);
		}

		if (controls.bShowLines) {
			List<Polygon2D_F64> polygons = detector.getFoundPolygons().toList();

			g2.setColor(Color.RED);
			g2.setStroke(new BasicStroke(3));
			for (Polygon2D_F64 p : polygons) {
				int red = 255*((p.size()-3)%4)/3;
				int green = 255*((p.size())%5)/4;
				int blue = 255*((p.size()+2)%6)/5;

				g2.setColor(new Color(red,green,blue));

				VisualizeShapes.drawPolygon(p, true, g2);
			}
		}

		if (controls.bShowCorners) {
			List<Polygon2D_F64> polygons = detector.getFoundPolygons().toList();

			g2.setColor(Color.BLUE);
			g2.setStroke(new BasicStroke(1));
			for (Polygon2D_F64 p : polygons) {
				for (int i = 0; i < p.size(); i++) {
					Point2D_F64 c = p.get(i);
					VisualizeFeatures.drawCircle(g2, c.x, c.y, 5);
				}
			}
		}

		guiImage.setBufferedImage(work);
	}

	/**
	 * Called when how the data is visualized has changed
	 */
	public void viewUpdated() {
		renderImage();
	}

	public void configUpdate() {
		createDetector();
		// does process and render too
	}

	@Override
	public synchronized void imageThresholdUpdated() {

		int threshold = controls.getThreshold().vThreshold;
		boolean down = controls.getThreshold().vDirectionDown;
		int radius = controls.getThreshold().vRadius;

		switch( controls.getThreshold().vType ) {
			case FIXED:
				inputToBinary = FactoryThresholdBinary.globalFixed(threshold,down,imageType);
				break;

			case GLOBAL_OTSU:
				inputToBinary = FactoryThresholdBinary.globalOtsu(0, 255, down, imageType);
				break;

			case GLOBAL_ENTROPY:
				inputToBinary = FactoryThresholdBinary.globalEntropy(0, 255, down, imageType);
				break;

			case LOCAL_GAUSSIAN:
				inputToBinary = FactoryThresholdBinary.localGaussian(radius, 0, down, imageType);
				break;

			case LOCAL_SQUARE:
				inputToBinary = FactoryThresholdBinary.localSquare(radius, 0, down, imageType);
				break;

			case LOCAL_SAVOLA:
				inputToBinary = FactoryThresholdBinary.localSauvola(radius, 0.3f, down, imageType);
				break;

			default:
				throw new RuntimeException("Unknown threshold type");
		}

		processImage();
		renderImage();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if( menuFile == e.getSource()) {
			int returnVal = fc.showOpenDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				BufferedImage buffered = UtilImageIO.loadImage(file.getAbsolutePath());
				if( buffered == null ) {
					// TODO see if it's a video instead
					System.err.println("Couldn't read "+file.getPath());
				} else {
					setInput(buffered);
				}
			} else {
			}
		} else if( menuWebcam == e.getSource() ) {

		} else if( menuQuit == e.getSource() ) {
			System.exit(0);
		}
	}

	class WebcamThread extends Thread {

		boolean requestStop = false;
		boolean running = true;

//		Webcam

		@Override
		public void run() {
			while( !requestStop ) {

			}

		}
	}

	public static void main(String[] args) {

		String path = "data/example/polygons01.jpg";
		BufferedImage buffered = UtilImageIO.loadImage(path);
		if( buffered == null ) {
			System.err.println("Couldn't find "+path);
			System.exit(1);
		}

		DetectBlackPolygonApp app = new DetectBlackPolygonApp(ImageUInt8.class);

		app.setInput(buffered);

		ShowImages.showWindow(app,"Detect Black Polygons",true);
	}



}
