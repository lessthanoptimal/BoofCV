/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.geo.d3.calibration;

import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.calibration.ParametersZhang98;
import boofcv.app.ImageResults;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.StandardAlgConfigPanel;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Vector;

/**
 * GUI interface for {@link CalibrateMonoPlanarGuiApp}.  Displays results for each calibration
 * image in a window.
 * 
 * @author Peter Abeles
 */
public class MonoPlanarPanel extends JPanel implements ItemListener ,
		ListSelectionListener 
{

	MainView mainView = new MainView();
	
	JCheckBox checkPoints;
	JCheckBox checkErrors;
	JCheckBox checkUndistorted;
	JCheckBox checkAll;
	JCheckBox checkNumbers;
	JList imageList;

	JTextArea meanError;
	JTextArea maxError;

	JTextArea paramCenterX;
	JTextArea paramCenterY;
	JTextArea paramA;
	JTextArea paramB;
	JTextArea paramC;

	boolean showPoints = true;
	boolean showErrors = true;
	boolean showUndistorted = false;
	boolean showAll = false;
	boolean showNumbers = true;

	int selectedImage = 0;

	List<String> names;
	List<BufferedImage> images;
	List<List<Point2D_F64>> features;
	List<ImageResults> results;

	double errorScale = 20;

	MultiSpectral<ImageFloat32> origMS;
	MultiSpectral<ImageFloat32> correctedMS;

	ImageDistort<ImageFloat32> undoRadial;

	// true if the undistorted image has been computed
	boolean hasUndistorted = false;
	BufferedImage undistorted;
	
	public MonoPlanarPanel() {
		super(new BorderLayout());
		
		JToolBar toolBar = new JToolBar("Controls");

		checkPoints = new JCheckBox("Show Points");
		checkPoints.setSelected(showPoints);
		checkPoints.addItemListener(this);

		checkErrors = new JCheckBox("Show Errors");
		checkErrors.setSelected(showErrors);
		checkErrors.addItemListener(this);

		checkAll = new JCheckBox("All Points");
		checkAll.setSelected(showAll);
		checkAll.addItemListener(this);

		checkUndistorted = new JCheckBox("Undistort");
		checkUndistorted.setSelected(showUndistorted);
		checkUndistorted.addItemListener(this);
		checkUndistorted.setEnabled(false);

		checkNumbers = new JCheckBox("Numbers");
		checkNumbers.setSelected(showNumbers);
		checkNumbers.addItemListener(this);
		
		toolBar.add(checkPoints);
		toolBar.add(checkErrors);
		toolBar.add(checkAll);
		toolBar.add(checkUndistorted);
		toolBar.add(checkNumbers);

		imageList = new JList();
		imageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		imageList.addListSelectionListener(this);

		meanError = createErrorComponent();
		maxError = createErrorComponent();
		paramCenterX = createErrorComponent();
		paramCenterY = createErrorComponent();
		paramA = createErrorComponent();
		paramB = createErrorComponent();
		paramC = createErrorComponent();

		add(toolBar, BorderLayout.PAGE_START);
		add(mainView, BorderLayout.CENTER);
		add(new SideBar(), BorderLayout.WEST);
	}

	private JTextArea createErrorComponent() {
		JTextArea comp = new JTextArea(1,6);
		comp.setMaximumSize(comp.getPreferredSize());
		comp.setEditable(false);
		return comp;
	}

	public void setImages( List<String> names , 
						   List<BufferedImage> images ,
						   List<List<Point2D_F64>> features  ) {
		this.names = names;
		this.images = images;
		this.features = features;

		imageList.setListData(new Vector<Object>(names));
		imageList.setSelectedIndex(0);
		
		BufferedImage image = images.get(selectedImage);
		mainView.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));

		revalidate();
	}

	public void setResults(List<ImageResults> results) {
		this.results = results;
		updateResultsGUI();
	}

	public void setCalibration(ParametersZhang98 found) {
		String textX = String.format("%5.1f",found.x0);
		String textY = String.format("%5.1f", found.y0);
		paramCenterX.setText(textX);
		paramCenterY.setText(textY);

		String textA = String.format("%5.1f",found.a);
		String textB = String.format("%5.1f",found.b);
		paramA.setText(textA);
		paramB.setText(textB);
		String textC = String.format("%5.1e",found.c);
		paramC.setText(textC);
	}

	public void setCorrection( ImageDistort<ImageFloat32> undoRadial )
	{
		this.undoRadial = undoRadial;
		checkUndistorted.setEnabled(true);
		hasUndistorted = false;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if( e.getSource() == checkPoints ) {
			showPoints = checkPoints.isSelected();
		} else if( e.getSource() == checkErrors ) {
			showErrors = checkErrors.isSelected();
		} else if( e.getSource() == checkAll ) {
			showAll = checkAll.isSelected();
		} else if( e.getSource() == checkUndistorted ) {
			showUndistorted = checkUndistorted.isSelected();
		} else if( e.getSource() == checkNumbers ) {
			showNumbers = checkNumbers.isSelected();
		}
		
		mainView.repaint();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if( e.getValueIsAdjusting() || e.getFirstIndex() == -1)
			return;
		setSelected(imageList.getSelectedIndex());
		mainView.repaint();
	}

	private void setSelected( int selected ) {
		selectedImage = selected;
		hasUndistorted = false;

		BufferedImage image = images.get(selected);
		int numBands = image.getRaster().getNumBands();

		if( origMS == null || origMS.getNumBands() != numBands ) {
			origMS = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,numBands);
			correctedMS = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,numBands);
		}
		if( image.getWidth() != origMS.getWidth() || image.getHeight() != origMS.getHeight() ) {
			int type = numBands == 1 ? BufferedImage.TYPE_INT_BGR : image.getType();
			undistorted = new BufferedImage(image.getWidth(),image.getHeight(),type);
			origMS.reshape(image.getWidth(),image.getHeight());
			correctedMS.reshape(image.getWidth(),image.getHeight());
		}

		if( results != null ) {
			updateResultsGUI();
		}
	}

	private void updateResultsGUI() {
		ImageResults r = results.get(selectedImage);
		String textMean = String.format("%5.1e", r.meanError);
		String textMax = String.format("%5.1e",r.maxError);
		meanError.setText(textMean);
		maxError.setText(textMax);
	}

	private void undoRadialDistortion(BufferedImage image) {
		ConvertBufferedImage.convertFromMulti(image, origMS, ImageFloat32.class);
		System.out.println("number of bands "+origMS.getNumBands());
		for( int i = 0; i < origMS.getNumBands(); i++ ) {
			ImageFloat32 in = origMS.getBand(i);
			ImageFloat32 out = correctedMS.getBand(i);

			undoRadial.apply(in,out);
		}
		if( correctedMS.getNumBands() == 3 )
			ConvertBufferedImage.convertTo(correctedMS,undistorted);
		else if( correctedMS.getNumBands() == 1 )
			ConvertBufferedImage.convertTo(correctedMS.getBand(0),undistorted);
		else
			throw new RuntimeException("What kind of image has "+correctedMS.getNumBands()+"???");
	}

	private class MainView extends JPanel
	{
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if( images == null )
				return;
			
			BufferedImage image = images.get(selectedImage);
			List<Point2D_F64> points = features.get(selectedImage);
			
			Graphics2D g2 = (Graphics2D)g;
			
			if( showUndistorted) {
				if( !hasUndistorted) {
					undoRadialDistortion(image);
					hasUndistorted = true;
				}
				g2.drawImage(undistorted,0,0,null);
			} else
				g2.drawImage(image,0,0,null);

			if( showPoints ) {
				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(3));
				for( Point2D_F64 p : points ) {
					VisualizeFeatures.drawCross(g2, (int) p.x, (int) p.y, 4);
				}
				g2.setStroke(new BasicStroke(1));
				g2.setColor(Color.RED);
				for( Point2D_F64 p : points ) {
					VisualizeFeatures.drawCross(g2, (int) p.x, (int) p.y, 4);
				}
			}

			if( showAll ) {
				for( List<Point2D_F64> l : features ) {
					for( Point2D_F64 p : l ) {
						VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,2,Color.BLUE);
					}
				}
			}

			if( showNumbers ) {
				DetectCalibrationChessApp.drawNumbers(g2,points);
			}

			if( showErrors && results != null ) {
				ImageResults result = results.get(selectedImage);

				Stroke before = g2.getStroke();
				g2.setStroke(new BasicStroke(4));
				g2.setColor(Color.BLACK);
				for( int i = 0; i < points.size(); i++ ) {
					Point2D_F64 p = points.get(i);

					int r = (int)(errorScale*result.pointError[i]);
					if( r < 1 )
						continue;

					int x = (int)p.x - r ;
					int y = (int)p.y - r;
					int w = r*2+1;

					g2.drawOval(x, y, w, w);
				}

				g2.setStroke(before);
				g2.setColor(Color.ORANGE);
				for( int i = 0; i < points.size(); i++ ) {
					Point2D_F64 p = points.get(i);

					int r = (int)(errorScale*result.pointError[i]);
					if( r < 1 )
						continue;
					
					int x = (int)p.x - r ;
					int y = (int)p.y - r;
					int w = r*2+1;
					
					g2.drawOval(x, y, w, w);
				}
			}
		}
	}

	private class SideBar extends StandardAlgConfigPanel
	{
		public SideBar() {
			JScrollPane scroll = new JScrollPane(imageList);

			addLabeled(meanError,"Mean Error",this);
			addLabeled(maxError, "Max Error", this);
			addSeparator(200);
			addLabeled(paramCenterX,"Xc",this);
			addLabeled(paramCenterY,"Yc",this);
			addLabeled(paramA,"fx",this);
			addLabeled(paramB,"fy",this);
			addLabeled(paramC,"skew",this);
			addSeparator(200);
			add(scroll);
		}
	}
}
