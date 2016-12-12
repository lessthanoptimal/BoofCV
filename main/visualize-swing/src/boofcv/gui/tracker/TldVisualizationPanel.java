/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.tracker;

import boofcv.alg.tracker.tld.TldHelperFunctions;
import boofcv.alg.tracker.tld.TldRegion;
import boofcv.alg.tracker.tld.TldTracker;
import boofcv.struct.ImageRectangle;
import georegression.struct.shapes.Rectangle2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class TldVisualizationPanel extends JPanel implements MouseListener{

	BufferedImage frame;
	CenterPanel centerPanel = new CenterPanel();

	Listener listener;

	int numClicks;
	ImageRectangle selected = new ImageRectangle();
	boolean hasSelected = false;

	FastQueue<TldRegion> detections = new FastQueue<>(TldRegion.class, true);

	TldTemplatePanel positivePanel = new TldTemplatePanel(15);
	TldTemplatePanel negativePanel = new TldTemplatePanel(15);


	public TldVisualizationPanel(Listener listener) {
		setLayout(new BorderLayout());
		this.listener = listener;


		JScrollPane scrollPositive = new JScrollPane(positivePanel);
		scrollPositive.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		JScrollPane scrollNegative = new JScrollPane(negativePanel);
		scrollNegative.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		add(scrollPositive, BorderLayout.WEST);
		add(centerPanel,BorderLayout.CENTER);
		add(scrollNegative, BorderLayout.EAST);

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
				TldVisualizationPanel.this.frame = frame;
				centerPanel.setPreferredSize(new Dimension(frame.getWidth(),frame.getHeight()));
				centerPanel.setMinimumSize(centerPanel.getPreferredSize());
				centerPanel.revalidate();
			}
		});
	}

	public synchronized void update( final TldTracker tracker , boolean hasSelected ) {

		this.hasSelected = hasSelected;

		if( hasSelected ) {
			Rectangle2D_F64 r = tracker.getTargetRegion();
			TldHelperFunctions.convertRegion(r, this.selected);

			addDetections(tracker.getDetection().getLocalMaximums());

			positivePanel.update(tracker.getTemplateMatching().getTemplatePositive(),false);
			negativePanel.update(tracker.getTemplateMatching().getTemplateNegative(),false);

		} else {
			detections.reset();
		}

		repaint();
	}

	private void addDetections( FastQueue<TldRegion> detections ) {
		this.detections.reset();
		for( TldRegion r : detections.toList() ){
			TldRegion a = this.detections.grow();

			a.confidence = r.confidence;
			a.rect.set(r.rect);
		}
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

//		for( TldRegion region : detections.toList() ){
//			drawRectangle(g2, region.rect, Color.BLUE, 3);
//		}

			if( hasSelected ) {
//			drawRectangle(g2,trackingRect,Color.GREEN,6);
				drawRectangle(g2,selected,Color.RED,3);
			}
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

	public void turnOffSelect() {
		numClicks = 2;
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
			System.out.println("Selected = "+selected);
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
		public void startTracking( int x0 , int y0 , int x1 , int y1 );

		public void togglePause();
	}
}
