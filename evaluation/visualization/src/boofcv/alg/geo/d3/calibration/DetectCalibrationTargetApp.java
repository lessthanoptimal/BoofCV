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
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ImageListManager;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_I32;

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
public class DetectCalibrationTargetApp <T extends ImageSingleBand>
		extends SelectImagePanel implements ProcessInput
{
	Class<T> imageType;
	DetectCalibrationTarget<T> alg;

	// gray scale image that targets are detected inside of
	T gray;
	
	int threshold = 60;

	ImagePanel gui = new ImagePanel();
	
	boolean processedImage = false;

	public DetectCalibrationTargetApp(Class<T> imageType) {
		this.imageType = imageType;
		alg = new DetectCalibrationTarget<T>(imageType);
		gray = GeneralizedImageOps.createSingleBand(imageType, 1, 1);

		setMainGUI(gui);
	}

	public void process( BufferedImage input ) {
		gray.reshape(input.getWidth(),input.getHeight());
		ConvertBufferedImage.convertFrom(input,gray);
		
		alg.process(gray,threshold);

		Graphics2D g2 = input.createGraphics();
		List<Point2D_I32> targetBounds = alg.getTargetQuadrilateral();
		List<Point2D_I32> targetPoints = alg.getCalibrationPoints();

		drawBounds(g2,targetBounds);
		drawPoints(g2, targetPoints);
		drawNumbers(g2, targetPoints);
		
		gui.setBufferedImage(input);
		gui.setPreferredSize(new Dimension(input.getWidth(),input.getHeight()));
		gui.repaint();

		processedImage = true;
	}

	private void drawBounds( Graphics2D g2 , java.util.List<Point2D_I32> corners ) {
		g2.setColor(Color.BLUE);
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

	public static void main(String args[]) {

//		DetectCalibrationTargetApp<ImageFloat32> app
//				= new DetectCalibrationTargetApp<ImageFloat32>(ImageFloat32.class);
		DetectCalibrationTargetApp<ImageUInt8> app
				= new DetectCalibrationTargetApp<ImageUInt8>(ImageUInt8.class);

		ImageListManager manager = new ImageListManager();
		manager.add("View 01","../data/evaluation/calibration/Sony_DSC-HX5V/image01.jpg");
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
