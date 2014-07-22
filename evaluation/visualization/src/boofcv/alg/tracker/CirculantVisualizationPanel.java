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

package boofcv.alg.tracker;

import boofcv.alg.misc.PixelMath;
import boofcv.alg.tracker.circulant.CirculantTracker;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.core.image.ConvertImage;
import boofcv.gui.image.VisualizeImageData;
import boofcv.struct.ImageRectangle;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageFloat64;
import georegression.struct.shapes.RectangleLength2D_F32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class CirculantVisualizationPanel extends JPanel implements MouseListener{

	BufferedImage frame;
	CenterPanel centerPanel = new CenterPanel();

	Listener listener;

	int numClicks;
	ImageRectangle selected = new ImageRectangle();
	boolean hasSelected = false;

	BufferedImage template;
	BufferedImage response;

	ImageFloat32 tmp = new ImageFloat32(1,1);


	public CirculantVisualizationPanel(Listener listener) {
		setLayout(new BorderLayout());
		this.listener = listener;

		JPanel right = new JPanel();
		right.setLayout(new  BoxLayout(right,BoxLayout.Y_AXIS));
		right.setPreferredSize(new Dimension(150,400));

		right.add(new TemplatePanel());
		right.add(new ResponsePanel());

		add(centerPanel,BorderLayout.CENTER);
		add(right, BorderLayout.EAST);

		centerPanel.addMouseListener(this);
		centerPanel.grabFocus();
	}

	public void setSelectRectangle( boolean value ) {
		if( value )
			numClicks = 0;
		else
			numClicks = 2;
	}

	public void setFrame( final BufferedImage frame) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				CirculantVisualizationPanel.this.frame = frame;
				centerPanel.setPreferredSize(new Dimension(frame.getWidth(),frame.getHeight()));
				centerPanel.setMinimumSize(centerPanel.getPreferredSize());
				centerPanel.revalidate();
			}
		});
	}

	public synchronized void update( final CirculantTracker tracker  ) {

		if( hasSelected ) {
			RectangleLength2D_F32 r = tracker.getTargetLocation();
			selected.x0 = (int)r.x0;
			selected.y0 = (int)r.y0;
			selected.x1 = selected.x0 + (int)r.width;
			selected.y1 = selected.y0 + (int)r.height;

			ImageFloat64 template = tracker.getTargetTemplate();
			ImageFloat64 response = tracker.getResponse();

			if( this.template == null ) {
				this.template = new BufferedImage(template.width,template.height,BufferedImage.TYPE_INT_RGB);
				this.response = new BufferedImage(template.width,template.height,BufferedImage.TYPE_INT_RGB);
				tmp.reshape(template.width,template.height);
			}

			ConvertImage.convert(template,tmp);
			PixelMath.plus(tmp, 0.5f, tmp);
			PixelMath.multiply(tmp,255,0,255,tmp);
			ConvertBufferedImage.convertTo(tmp,this.template,true);

			ConvertImage.convert(response,tmp);
			VisualizeImageData.colorizeSign(tmp,this.response,-1);
		}

		repaint();
	}

	public void discardSelected() {
		hasSelected = false;
		numClicks = 0;
	}

	private class CenterPanel extends JPanel {
		@Override
		protected synchronized void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D)g;

			g2.drawImage(frame,0,0,null);

			if( hasSelected ) {
//			drawRectangle(g2,trackingRect,Color.GREEN,6);
				drawRectangle(g2,selected,Color.RED,3);
			}
		}
	}

	private class TemplatePanel extends JPanel {

		@Override
		protected synchronized void paintComponent(Graphics g) {
			super.paintComponent(g);

			if( template == null )return;

			Graphics2D g2 = (Graphics2D)g;

//			g2.setTransform(AffineTransform.getScaleInstance(2,2));
			g2.drawImage(template,0,0,null);
		}
	}

	private class ResponsePanel extends JPanel {

		@Override
		protected synchronized void paintComponent(Graphics g) {
			super.paintComponent(g);
			if( response == null )return;

			Graphics2D g2 = (Graphics2D)g;

//			g2.setTransform(AffineTransform.getScaleInstance(2,2));
			g2.drawImage(response,0,0,null);
		}
	}


	private void drawRectangle(Graphics2D g2 , ImageRectangle r , Color c , int size) {
		g2.setColor(c);
		g2.setStroke(new BasicStroke(size));

		g2.drawLine(r.x0,r.y0,r.x1,r.y0);
		g2.drawLine(r.x1,r.y0,r.x1,r.y1);
		g2.drawLine(r.x1,r.y1,r.x0,r.y1);
		g2.drawLine(r.x0,r.y1,r.x0,r.y0);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if( numClicks == 0 ) {
			selected.x0 = e.getX();
			selected.y0 = e.getY();
		} else if( numClicks == 1 ) {
			selected.x1 = e.getX();
			selected.y1 = e.getY();
			hasSelected = true;
			listener.startTracking(selected.x0,selected.y0,selected.x1,selected.y1);
		} else {
			listener.togglePause();
		}
		numClicks++;
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public static interface Listener
	{
		public void startTracking(int x0, int y0, int x1, int y1);

		public void togglePause();
	}
}
