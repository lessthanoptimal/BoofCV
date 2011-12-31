/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.geo.d3.calibration;

import boofcv.alg.feature.detect.calibgrid.DetectCalibrationTarget;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.GeneralizedImageOps;
import boofcv.gui.ProcessInput;
import boofcv.gui.SelectImagePanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Detects and displays detected calibration grids.  Shows intermediate steps.
 *
 * @author Peter Abeles
 */
// TODO improve filtering of noisy blobs
//  -- If too many, filter by brightness uniformity?
//  -- Compute a quality of polygon?
//  Form a grid graph with all objects.
//     * Prune islands
//     * break off connections in which the distance is much  greater than either side
public class DetectCalibrationTargetApp <T extends ImageSingleBand>
		extends SelectImagePanel implements ProcessInput , GridCalibPanel.Listener
{
	Class<T> imageType;
	// detects the calibration target
	DetectCalibrationTarget<T> alg;

	// gray scale image that targets are detected inside of
	T gray;

	// GUI components
	GridCalibPanel calibGUI;
	ImagePanel gui = new ImagePanel();

	// has an iamge been processed
	boolean processedImage = false;

	// work buffer that stuff is drawn inside of and displayed
	BufferedImage workImage;
	// original untainted image
	BufferedImage input;

	// if a target was found or not
	boolean foundTarget;

	public DetectCalibrationTargetApp(Class<T> imageType) {
		this.imageType = imageType;
		alg = new DetectCalibrationTarget<T>(imageType,500,4,3);
		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout());
		
		calibGUI = new GridCalibPanel();
		calibGUI.setListener( this );
		calibGUI.setMinimumSize(calibGUI.getPreferredSize());
		
		panel.add(calibGUI,BorderLayout.WEST);
		panel.add(gui,BorderLayout.CENTER);
		
		setMainGUI(panel);
	}

	public synchronized void process( final BufferedImage input ) {
		this.input = input;
		workImage = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
		gray.reshape(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFrom(input,gray);

		foundTarget = alg.process(gray,calibGUI.getThresholdLevel());
		if( !foundTarget ) {
			System.out.println("Extract target failed!");
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				renderOutput();
			}
		});
	}

	private synchronized void renderOutput() {

		switch( calibGUI.getSelectedView() ) {
			case 0:
				workImage.createGraphics().drawImage(input,null,null);
				break;

			case 1:
				VisualizeBinaryData.renderBinary(alg.getThresholded(), workImage);
				break;

			case 2:
				int numLabels = alg.getNumBlobs();
				VisualizeBinaryData.renderLabeled(alg.getBlobs(),numLabels,workImage);
				break;

			default:
				throw new RuntimeException("Unknown mode");
		}
		List<Point2D_I32> targetBounds = alg.getTargetQuadrilateral();
		List<Point2D_I32> targetPoints = alg.getCalibrationPoints();

		Graphics2D g2 = workImage.createGraphics();

		if( calibGUI.isShowBound())
			drawBounds(g2,targetBounds);
		if( calibGUI.isShowPoints())
			drawPoints(g2, targetPoints);
		if( calibGUI.isShowNumbers())
			drawNumbers(g2, targetPoints);

		if( foundTarget )
			calibGUI.setSuccessMessage("FOUND",true);
		else
			calibGUI.setSuccessMessage("FAILED", false);
		
		gui.setBufferedImage(workImage);
		gui.repaint();
		
		processedImage = true;
	}

	private void drawBounds( Graphics2D g2 , java.util.List<Point2D_I32> corners ) {
		g2.setColor(Color.BLUE);
		g2.setStroke(new BasicStroke(2.0f));
		g2.drawLine(corners.get(0).x,corners.get(0).y,corners.get(1).x,corners.get(1).y);
		g2.drawLine(corners.get(1).x,corners.get(1).y,corners.get(2).x,corners.get(2).y);
		g2.drawLine(corners.get(2).x,corners.get(2).y,corners.get(3).x,corners.get(3).y);
		g2.drawLine(corners.get(3).x,corners.get(3).y,corners.get(0).x,corners.get(0).y);
	}

	private void drawPoints( Graphics2D g2 , java.util.List<Point2D_I32> foundTarget) {
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_I32 p = foundTarget.get(i);
			VisualizeFeatures.drawPoint(g2, p.x, p.y, Color.RED);
		}
	}

	/**
	 * Draw the number assigned to each corner point with a bold outline
	 */
	private void drawNumbers( Graphics2D g2 , java.util.List<Point2D_I32> foundTarget ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		g2.setStroke(new BasicStroke(4.0f));

		AffineTransform origTran = g2.getTransform();
		for( int i = 0; i < foundTarget.size(); i++ ) {
			Point2D_I32 p = foundTarget.get(i);
			String text = String.format("%2d",i);

			GlyphVector gv = regular.createGlyphVector(g2.getFontRenderContext(), text);
			Shape shape = gv.getOutline();

			AffineTransform adjusted = AffineTransform.getTranslateInstance(p.x, p.y);
			adjusted.concatenate(origTran);
			g2.setTransform(adjusted);
			g2.setColor(Color.BLACK);
			g2.draw(shape);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text,p.x,p.y);
		}
	}

	@Override
	public void changeImage(String name, int index) {
		ImageListManager manager = getInputManager();

		BufferedImage image = manager.loadImage(index);
		if (image != null) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void calibEventGUI() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				renderOutput();
			}
		});
	}

	@Override
	public synchronized void calibEventProcess() {
		foundTarget = alg.process(gray,calibGUI.getThresholdLevel());
		if( !foundTarget ) {
			System.out.println("Extract target failed!");
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
				renderOutput();
			}
		});
	}

	public static void main(String args[]) {

//		DetectCalibrationTargetApp<ImageFloat32> app
//				= new DetectCalibrationTargetApp<ImageFloat32>(ImageFloat32.class);
		DetectCalibrationTargetApp<ImageUInt8> app
				= new DetectCalibrationTargetApp<ImageUInt8>(ImageUInt8.class);

		ImageListManager manager = new ImageListManager();
		manager.add("View 01","../data/evaluation/calibration/hp_dm1/img01.jpg");
		manager.add("View 02","../data/evaluation/calibration/hp_dm1/img02.jpg");
		manager.add("View 03","../data/evaluation/calibration/hp_dm1/img03.jpg");
		manager.add("View 04","../data/evaluation/calibration/hp_dm1/img04.jpg");
		manager.add("View 05","../data/evaluation/calibration/hp_dm1/img05.jpg");
		manager.add("View 06","../data/evaluation/calibration/hp_dm1/img06.jpg");
		manager.add("View 07","../data/evaluation/calibration/hp_dm1/img07.jpg");
		manager.add("View 08","../data/evaluation/calibration/hp_dm1/img08.jpg");

		manager.add("BView 01","../data/evaluation/calibration/Sony_DSC-HX5V/image01.jpg");
		manager.add("View 10","../data/evaluation/calibration/Sony_DSC-HX5V/image10.jpg");
		manager.add("View 12","../data/evaluation/calibration/Sony_DSC-HX5V/image12.jpg");

		app.setInputManager(manager);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app, "Calibration Target Detection");
	}
}
