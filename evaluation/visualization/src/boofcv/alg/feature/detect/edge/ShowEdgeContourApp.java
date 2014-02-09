/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.edge;

import boofcv.alg.binary.SelectHistogramThresholdPanel;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.LinearContourLabelChang2004;
import boofcv.alg.misc.GImageStatistics;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.PathLabel;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageSingleBand;
import boofcv.struct.image.ImageUInt8;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays contours selected using different algorithms.
 *
 * @author Peter Abeles
 */
public class ShowEdgeContourApp<T extends ImageSingleBand, D extends ImageSingleBand>
		extends SelectAlgorithmAndInputPanel implements CannyControlBar.Listener , SelectHistogramThresholdPanel.Listener
{
	// shows panel for displaying input image
	ImagePanel panel = new ImagePanel();

	BufferedImage input;
	T workImage;
	Class<T> imageType;
	Class<D> derivType;
	boolean processedImage = false;

	ImageUInt8 binary = new ImageUInt8(1,1);
	ImageSInt32 labeled = new ImageSInt32(1,1);

	CannyControlBar barCanny;
	SelectHistogramThresholdPanel barBinary;
	JPanel bodyPanel = new JPanel();
	int activeAlg;

	int previousBlur;
	CannyEdge<T,D> canny;
	LinearContourLabelChang2004 contour = new LinearContourLabelChang2004(ConnectRule.EIGHT);

	public ShowEdgeContourApp(Class<T> imageType, Class<D> derivType) {
		super(1);
		this.imageType = imageType;
		this.derivType = derivType;

		addAlgorithm(0, "Canny", 0);
		addAlgorithm(0, "Threshold Contour", 1);

		barCanny = new CannyControlBar(1,15);
		barCanny.setListener(this);

		barBinary = new SelectHistogramThresholdPanel(50,true);
		barBinary.setListener(this);

		bodyPanel.setLayout(new BorderLayout());
		bodyPanel.add(panel,BorderLayout.CENTER);
		bodyPanel.add(barCanny,BorderLayout.NORTH);

		setMainGUI(bodyPanel);

		previousBlur = barCanny.getBlurRadius();
		canny =  FactoryEdgeDetectors.canny(previousBlur, true, true, imageType, derivType);
	}

	public void process( BufferedImage input ) {
		setInputImage(input);
		this.input = input;
		workImage = ConvertBufferedImage.convertFromSingle(input, null, imageType);

		// update the binary histogram threshold for this image
		final double threshold = GImageStatistics.mean(workImage);

		binary.reshape(workImage.width,workImage.height);
		labeled.reshape(workImage.width,workImage.height);

		final int width = input.getWidth();
		final int height = input.getHeight();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				barBinary.setThreshold((int)threshold);
				barBinary.getHistogramPanel().update(workImage);
				panel.setPreferredSize(new Dimension(width,height));
				processedImage = true;
				doRefreshAll();
			}});
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public void refreshAll(Object[] cookies) {
		setActiveAlgorithm(0,null,cookies[0]);
	}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {

		activeAlg = ((Integer)cookie).intValue();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// swap control bar on top
				if( activeAlg == 0 ){
					bodyPanel.remove(barBinary);
					bodyPanel.add(barCanny,BorderLayout.NORTH);
					barCanny.repaint();
				} else {
					bodyPanel.remove(barCanny);
					bodyPanel.add(barBinary,BorderLayout.NORTH);
					barBinary.repaint();
				}
				validate();
			}});

		doProcess();
	}

	private void doProcess() {
		if( input == null )
			return;

		final BufferedImage temp;

		if( activeAlg == 0 ) {
			if( previousBlur != barCanny.getBlurRadius() ) {
				previousBlur = barCanny.getBlurRadius();
				canny =  FactoryEdgeDetectors.canny(previousBlur,true, true, imageType, derivType);
			}

			double thresh = barCanny.getThreshold()/100.0;
			canny.process(workImage,(float)thresh*0.1f,(float)thresh,null);
			List<EdgeContour> contours = canny.getContours();

			temp = VisualizeBinaryData.renderContours(contours,null,workImage.width,workImage.height,null);
		} else {
			// create a binary image by thresholding
			GThresholdImageOps.threshold(workImage, binary, barBinary.getThreshold(), barBinary.isDown());

			contour.process(binary,labeled);
			temp = VisualizeBinaryData.renderContours(contour.getContours().toList(),null,0xFF1010,
					workImage.width,workImage.height,null);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				panel.setBufferedImage(temp);
				panel.repaint();
			}});
	}


	@Override
	public void changeInput(String name, int index) {
		BufferedImage image = media.openImage(inputRefs.get(index).getPath());

		if( image != null ) {
			process(image);
		}
	}

	@Override
	public boolean getHasProcessedImage() {
		return processedImage;
	}

	@Override
	public void changeCanny() {
		doProcess();
	}

	@Override
	public void histogramThresholdChange() {
		doProcess();
	}

	public static void main( String args[] ) {
		ShowEdgeContourApp<ImageFloat32,ImageFloat32> app =
				new ShowEdgeContourApp<ImageFloat32, ImageFloat32>(ImageFloat32.class, ImageFloat32.class);
//		ShowFeatureOrientationApp<ImageUInt8, ImageSInt16> app =
//				new ShowFeatureOrientationApp<ImageUInt8,ImageSInt16>(input,ImageUInt8.class, ImageSInt16.class);

		List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("shapes","../data/applet/shapes01.png"));
		inputs.add(new PathLabel("Room","../data/applet/indoors01.jpg"));
		inputs.add(new PathLabel("Objects","../data/applet/simple_objects.jpg"));
		inputs.add(new PathLabel("Indoors","../data/applet/lines_indoors.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}

		ShowImages.showWindow(app,"Contours");

		System.out.println("Done");
	}
}
