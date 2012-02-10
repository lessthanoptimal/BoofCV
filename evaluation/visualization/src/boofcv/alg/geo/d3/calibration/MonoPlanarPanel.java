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

	int selectedImage = 0;

	List<String> names;
	List<BufferedImage> images;
	List<List<Point2D_F64>> features;
	List<ImageResults> results;

	double errorScale = 20;

	MultiSpectral<ImageFloat32> origMS = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,3);
	MultiSpectral<ImageFloat32> correctedMS = new MultiSpectral<ImageFloat32>(ImageFloat32.class,1,1,3);

	ImageDistort<ImageFloat32> undoRadial;

	
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
		
		toolBar.add(checkPoints);
		toolBar.add(checkErrors);
		toolBar.add(checkAll);
		toolBar.add(checkUndistorted);

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

		setSelected(0);
		
		imageList.setListData(new Vector<Object>(names));
		
		BufferedImage image = images.get(selectedImage);
		mainView.setPreferredSize(new Dimension(image.getWidth(),image.getHeight()));

		revalidate();
	}

	public void setResults(List<ImageResults> results) {
		this.results = results;
		ImageResults r = results.get(selectedImage);
		String textMean = String.format("%5.1e",r.meanError);
		String textMax = String.format("%5.1e",r.maxError);
		meanError.setText(textMean);
		maxError.setText(textMax);
	}

	public void setCalibration(ParametersZhang98 found) {
		String textX = String.format("%5.1f",found.x0);
		String textY = String.format("%5.1f",found.y0);
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

		BufferedImage image = images.get(selectedImage);
		ConvertBufferedImage.convertFromMulti(image,origMS,ImageFloat32.class);
		for( int i = 0; i < 3; i++ ) {
			ImageFloat32 in = origMS.getBand(i);
			ImageFloat32 out = correctedMS.getBand(i);

			undoRadial.apply(in,out);
		}
		ConvertBufferedImage.convertTo(correctedMS,undistorted);
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
		BufferedImage image = images.get(selected);

		
		if( undistorted == null ||
				undistorted.getWidth() != image.getWidth() || 
				undistorted.getHeight() != image.getHeight() ) {
			undistorted = new BufferedImage(image.getWidth(),image.getHeight(),image.getType());
			origMS.reshape(undistorted.getWidth(),undistorted.getHeight());
			correctedMS.reshape(undistorted.getWidth(),undistorted.getHeight());
		}

		if( undoRadial != null ) {
			ConvertBufferedImage.convertFromMulti(image,origMS,ImageFloat32.class);
			for( int i = 0; i < 3; i++ ) {
				ImageFloat32 in = origMS.getBand(i);
				ImageFloat32 out = correctedMS.getBand(i);

				undoRadial.apply(in,out);
			}
			ConvertBufferedImage.convertTo(correctedMS,undistorted);
		}

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
			
			if( showUndistorted)
				g2.drawImage(undistorted,0,0,null);
			else
				g2.drawImage(image,0,0,null);

			if( showPoints ) {
				for( Point2D_F64 p : points ) {
					VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,2,Color.RED);
				}
			}

			if( showAll ) {
				for( List<Point2D_F64> l : features ) {
					for( Point2D_F64 p : l ) {
						VisualizeFeatures.drawPoint(g2,(int)p.x,(int)p.y,2,Color.BLUE);
					}
				}
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
				g2.setColor(Color.GREEN);
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
