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

import boofcv.struct.FastQueue;
import boofcv.struct.feature.AssociatedIndex;
import jgrl.geometry.UtilPoint2D_I32;
import jgrl.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;


/**
 * Shows which two features are associated with each other.  An individual feature
 * can be shown alone by clicking on it.
 *
 * @author Peter Abeles
 */
public class AssociationPanel extends JPanel implements MouseListener {

	// how close a click needs to be to a point
	double clickDistance = 20;

	// size of the border between the images
	int borderSize;

	// maximum size of sub-images inside of panel
	int maxWidth;
	int maxHeight;

	// left and right window information
	BufferedImage leftImage,rightImage;
	double scaleLeft,scaleRight;

	// list of features in both images
	List<Point2D_I32> leftPts,rightPts;

	// color of each points.  Randomly select at runtime
	Color colors[];

	// which features are associated with each other
	int assocLeft[],assocRight[];

	// draw a selected pair
	int selectedIndex=-1;
	boolean selectedIsLeft;

	public AssociationPanel(int borderSize, int maxWidth, int maxHeight) {
		this.borderSize = borderSize;
		this.maxWidth = maxWidth;
		this.maxHeight = maxHeight;

		addMouseListener(this);
	}

	public synchronized void setImages(BufferedImage leftImage , BufferedImage rightImage ) {
		this.leftImage = leftImage;
		this.rightImage = rightImage;

		scaleLeft = scaleRight = 1;

		// compute the scale factor for each image
		if( leftImage.getWidth() > maxWidth || leftImage.getHeight() > maxHeight ) {
			double scaleX = (double)maxWidth/(double)leftImage.getWidth();
			double scaleY = (double)maxHeight/(double)leftImage.getHeight();
			scaleLeft = Math.min(scaleX,scaleY);
		}
		if( rightImage.getWidth() > maxWidth || rightImage.getHeight() > maxHeight ) {
			double scaleX = (double)maxWidth/(double)rightImage.getWidth();
			double scaleY = (double)maxHeight/(double)rightImage.getHeight();
			scaleRight = Math.min(scaleX,scaleY);
		}

		// compute the prefered size of the whole image
		int panelWidth = (int)(scaleLeft*leftImage.getWidth()) + (int)(scaleRight*rightImage.getWidth()) + borderSize;
		int panelHeight = Math.max((int)(scaleLeft*leftImage.getHeight()) ,(int)(scaleRight*rightImage.getHeight()));

		setPreferredSize(new Dimension(panelWidth,panelHeight));
	}

	public synchronized void setAssociation( List<Point2D_I32> leftPts , List<Point2D_I32> rightPts,
											 FastQueue<AssociatedIndex> matches ) {
		this.leftPts = leftPts;
		this.rightPts = rightPts;

		assocLeft = new int[ leftPts.size() ];
		assocRight = new int[ rightPts.size() ];

		for( int i = 0; i < assocLeft.length; i++ )
			assocLeft[i] = -1;
		for( int i = 0; i < assocRight.length; i++ )
			assocRight[i] = -1;

		for( int i = 0; i < matches.size; i++ ) {
			AssociatedIndex a = matches.get(i);
			assocLeft[a.src] = a.dst;
			assocRight[a.dst] = a.src;
		}

		Random rand = new Random(234);
		colors = new Color[ leftPts.size() ];
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = new Color(rand.nextInt() | 0xFF000000 );
		}
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		int panelWidth = getPreferredSize().width;
		int panelHeight = getPreferredSize().height;

		Graphics2D g2 = (Graphics2D)g;

		int rightX = (int)(scaleLeft*leftImage.getWidth())+borderSize;

		// draw the background images
		g2.drawImage(leftImage,0,0,rightX-borderSize,panelHeight,0,0,leftImage.getWidth(),leftImage.getHeight(),null);
		g2.drawImage(rightImage,rightX,0,panelWidth,panelHeight,0,0,rightImage.getWidth(),rightImage.getHeight(),null);

		// draw the features
		if( selectedIndex == -1 )
			drawAllFeatures(g2, rightX);
		else {
			// draw just an individual feature pair
			Point2D_I32 l,r;
			Color color;

			if( selectedIsLeft ) {
				l = leftPts.get(selectedIndex);
				r = rightPts.get(assocLeft[selectedIndex]);
				color = colors[selectedIndex];
			} else {
				l = leftPts.get(assocRight[selectedIndex]);
				r = rightPts.get(selectedIndex);
				color = colors[assocRight[selectedIndex]];
			}

			drawAssociation(g2, rightX, l, r, color);
		}
	}

	private void drawAllFeatures(Graphics2D g2, int rightX) {
		for( int i = 0; i < assocLeft.length; i++ ) {
			if( assocLeft[i] == -1 )
				continue;

			Point2D_I32 l = leftPts.get(i);
			Point2D_I32 r = rightPts.get(assocLeft[i]);

			Color color = colors[i];

			drawAssociation(g2, rightX, l, r, color);
		}
	}

	private void drawAssociation(Graphics2D g2, int rightX, Point2D_I32 l, Point2D_I32 r, Color color) {
		int x1 = (int)(scaleLeft*l.x);
		int y1 = (int)(scaleLeft*l.y);
		int x2 = (int)(scaleRight*r.x) + rightX;
		int y2 = (int)(scaleRight*r.y);

		drawPoint(g2,x1,y1,color);
		drawPoint(g2,x2,y2,color);

		g2.setColor(color);
		g2.drawLine(x1,y1,x2,y2);
	}

	private void drawPoint( Graphics2D g2 , int x , int y , Color color ) {
		int r = 5;
		int w = r*2+1;

		int r2 = r+2;
		int w2 = r2*2+1;

		g2.setColor(Color.BLACK);
		g2.fillOval(x-r2,y-r2,w2,w2);

		g2.setColor(color);
		g2.fillOval(x-r,y-r,w,w);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		selectedIndex = -1;
		if( e.getClickCount() > 1 ) {
			repaint();
			return;
		}

		int leftEndX = (int)(scaleLeft*leftImage.getWidth());
		int rightBeginX = leftEndX + borderSize;

		if( e.getX() < leftEndX ) {
			selectedIsLeft = true;

			int x = (int)(e.getX()/scaleLeft);
			int y = (int)(e.getY()/scaleLeft);

			int bestIndex = findBestPoint(x, y, assocLeft, leftPts );

			if( bestIndex != -1 ) {
				selectedIndex = bestIndex;
			}

		} else if( e.getX() >= rightBeginX ) {
			selectedIsLeft = false;

			int x = (int)((e.getX()-rightBeginX)/scaleRight);
			int y = (int)(e.getY()/scaleRight);

			int bestIndex = findBestPoint(x, y, assocRight, rightPts );

			if( bestIndex != -1 ) {
				selectedIndex = bestIndex;
			}

		}
		repaint();
	}

	private int findBestPoint(int x, int y, int assoc[] , List<Point2D_I32> pts ) {
		double bestDist = clickDistance;
		int bestIndex = -1;
		for( int i = 0; i < assoc.length; i++ ) {
			if( assoc[i] == -1 )
				continue;

			Point2D_I32 p = pts.get(i);
			double d = UtilPoint2D_I32.distance(p.x,p.y,x,y);
			if( d < bestDist ) {
				bestDist = d;
				bestIndex = i;
			}
		}
		return bestIndex;
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
