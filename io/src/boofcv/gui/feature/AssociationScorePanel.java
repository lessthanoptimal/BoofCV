/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.feature;

import georegression.geometry.UtilPoint2D_I32;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.List;


/**
 * Displays relative association scores for different features.  When a feature is clicked on in
 * an image the best fit scores are show in the other image.
 *
 * @author Peter Abeles
 */
public abstract class AssociationScorePanel extends JPanel implements MouseListener {
	// adjusts how close to the optimal answer a point needs to be before it is plotted
	double containmentFraction;

	// how big circles are drawn in association window
	int maxCircleRadius = 30;

	// left and right window information
	BufferedImage leftImage,rightImage;
	List<Point2D_I32> leftPts,rightPts;
	double associationScore[];

	ScorePanel leftPanel,rightPanel;
	JSplitPane splitPane;

	// if the left window is active or not
	boolean isSourceLeft;
	// which feature in active window is being associated against in the other window
	int targetIndex=-1;

	// how big individual images can be
	int maxWidth;
	int maxHeight;

	// is zero the minimum score or can it go negative
	boolean zeroMinimumScore;

	public AssociationScorePanel( int maxWidth , int maxHeight ,
								  double containmentFraction, boolean zeroMinimumScore ) {
		if( containmentFraction <= 0  )
			throw new IllegalArgumentException("containmentFraction must be more than zero");
		this.containmentFraction = containmentFraction;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;
		this.zeroMinimumScore = zeroMinimumScore;
		setLayout(new BorderLayout());

		leftPanel = new ScorePanel(true);
		rightPanel = new ScorePanel(false);
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
		splitPane.setOneTouchExpandable(true);

		add(splitPane);

		leftPanel.addMouseListener(this);
		rightPanel.addMouseListener(this);
	}

	public void setImages(BufferedImage leftImage , BufferedImage rightImage ) {
		this.leftImage = leftImage;
		this.rightImage = rightImage;
		leftPanel.setImage(leftImage);
		rightPanel.setImage(rightImage);
		splitPane.resetToPreferredSizes();
	}

	public void setLocation(List<Point2D_I32> leftPts , List<Point2D_I32> rightPts ) {
		this.leftPts = leftPts;
		this.rightPts = rightPts;
	}

	protected abstract double[] computeScore( boolean isTargetLeft , int targetIndex );

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
	}

	private class ScorePanel extends JPanel
	{
		BufferedImage img;
		boolean isLeft;
		double worst;
		double best;
		int indexBest;

		private ScorePanel(boolean left) {
			isLeft = left;
		}

		public void setImage(BufferedImage img) {
			this.img = img;

			double scale = computeScale();

			int width = (int)(scale*img.getWidth());
			int height = (int)(scale*img.getHeight());

			int w = maxWidth < width ? maxWidth : width;
			int h = maxHeight < height ? maxHeight : height;

			setPreferredSize(new Dimension(w,h));
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			double scale = computeScale();

			Graphics2D g2 = (Graphics2D)g;
			if( scale != 1 ) {
				AffineTransform tran = new AffineTransform();
				tran.setToScale(scale,scale);
				AffineTransform old = g2.getTransform();
				old.concatenate(tran);
				g2.setTransform(old);
			}
			g2.drawImage(img,0,0,img.getWidth(),img.getHeight(),null);

			if( targetIndex == -1 ) {
				// Draw all the found points
				List<Point2D_I32> points =  isLeft ? leftPts : rightPts;
				VisualizeFeatures.drawPoints(g2,Color.blue,points,3);
				return;
			}


			if( isLeft == isSourceLeft ) {
				// Just draw the selected point
				Point2D_I32 p = getPoint(targetIndex);
				g2.setColor(Color.BLACK);
				g2.setStroke(new BasicStroke(5));
				g2.drawLine(p.x-15,p.y,p.x+15,p.y);
				g2.drawLine(p.x,p.y-15,p.x,p.y+15);
				g2.setColor(Color.RED);
				g2.setStroke(new BasicStroke(1));
				g2.drawLine(p.x-15,p.y,p.x+15,p.y);
				g2.drawLine(p.x,p.y-15,p.x,p.y+15);
			} else {
				final int N = isLeft ? leftPts.size() : rightPts.size();

				findStatistics();

				// draw all the features, adjusting their size based on the first score
				g2.setColor(Color.RED);
				g2.setStroke(new BasicStroke(3));

				double normalizer;
				if( zeroMinimumScore )
					normalizer = best*containmentFraction;
				else
					normalizer = Math.abs(best)*(Math.exp(-1.0/containmentFraction));

				for( int i = 0; i < N; i++ ) {
					Point2D_I32 p = getPoint(i);

					double s = associationScore[i];

					// scale the circle based on how bad it is
					double ratio = 1-Math.abs(s-best)/normalizer;
					if( ratio < 0 )
						continue;
					
					int r = maxCircleRadius - (int)(maxCircleRadius*ratio);
					if( r > 0 ) {
						g2.drawOval(p.x-r,p.y-r,r*2+1,r*2+1);
					}
				}

				// draw the best feature
				g2.setColor(Color.GREEN);
				g2.setStroke(new BasicStroke(10));
				Point2D_I32 p = getPoint(indexBest);
				int w = maxCircleRadius*2+1;
				g2.drawOval(p.x-maxCircleRadius,p.y-maxCircleRadius,w,w);
			}
		}

		public double computeScale() {
			double scaleX = (double)maxWidth/(double)img.getWidth();
			double scaleY = (double)maxHeight/(double)img.getHeight();

			double scale = Math.min(scaleX,scaleY);
			if( scale > 1 ) scale = 1;
			return scale;
		}

		private Point2D_I32 getPoint( int index ) {
			if( isLeft )
				return leftPts.get(index);
			else
				return rightPts.get(index);
		}


		private void findStatistics() {
			final int N = isLeft ? leftPts.size() : rightPts.size();

			indexBest = -1;
			worst = -Double.MAX_VALUE;
			best = Double.MAX_VALUE;
			for( int i = 0; i < N; i++ ) {
				double s = associationScore[i];
				if( s > worst )
					worst = s;
				if( s < best ) {
					best = s;
					indexBest = i;
				}
			}
		}
	}

	@Override
	public synchronized void mouseClicked(MouseEvent e) {
		// double click means show all the features again
		if( e.getClickCount() > 1 ) {
			targetIndex = -1;
			repaint();
			return;
		}
		
		isSourceLeft = leftPanel == e.getSource();
		List<Point2D_I32> l = isSourceLeft ? leftPts : rightPts;
		double scale = isSourceLeft ? leftPanel.computeScale() : rightPanel.computeScale();

		targetIndex = -1;
		double dist = Double.MAX_VALUE;

		int px = (int)(e.getPoint().x/scale);
		int py = (int)(e.getPoint().y/scale);

		for( int i = 0; i < l.size(); i++ ) {
			Point2D_I32 p = l.get(i);

			if( Math.abs(p.x-px) > 20 || Math.abs(p.y-py) > 20 )
				continue;

			double r = UtilPoint2D_I32.distance(p.x,p.y,px,py);
			if( r < dist ) {
				dist = r;
				targetIndex = i;
			}
		}

		if( targetIndex > -1 )
			associationScore = computeScore(isSourceLeft,targetIndex);
		repaint();
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
