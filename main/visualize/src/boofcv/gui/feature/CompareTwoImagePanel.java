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

import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;


/**
 * Panel for displaying two images next to each other separated by a border.
 *
 * @author Peter Abeles
 */
public abstract class CompareTwoImagePanel extends JPanel implements MouseListener {

	// how close a click needs to be to a point
	private double clickDistance = 20;

	// list of features in both images
	protected List<Point2D_F64> leftPts,rightPts;

	// draw a selected pair
	protected int selectedIndex=-1;
	protected boolean selectedIsLeft;

	// size of the border between the images
	protected int borderSize;

	// left and right window information
	protected BufferedImage leftImage,rightImage;
	protected double scaleLeft,scaleRight;

	public CompareTwoImagePanel(int borderSize ) {
		this.borderSize = borderSize;
		addMouseListener(this);
	}

	public void setLocation( List<Point2D_F64> leftPts , List<Point2D_F64> rightPts) {
		this.leftPts = leftPts;
		this.rightPts = rightPts;
		selectedIndex = -1;
	}

	/**
	 * Sets the internal images.  Not thread safe.
	 *
	 * @param leftImage
	 * @param rightImage
	 */
	public synchronized void setImages(BufferedImage leftImage , BufferedImage rightImage ) {
		this.leftImage = leftImage;
		this.rightImage = rightImage;

		int width = leftImage.getWidth() + rightImage.getWidth()+borderSize;
		int height = Math.max(leftImage.getHeight(),rightImage.getHeight());
		setPreferredSize(new Dimension(width,height));
	}


	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		if( leftImage == null || rightImage == null )
			return;

		computeScales();

		Graphics2D g2 = (Graphics2D)g;

		// location in the current frame, taking in account the scale of each image
		int x1 = (int)(scaleLeft*leftImage.getWidth());
		int x2 = x1+borderSize;
		int x3 = x2+(int)(scaleRight*rightImage.getWidth());
		int y1 = (int)(scaleLeft*leftImage.getHeight());
		int y2 = (int)(scaleRight*rightImage.getHeight());

		// draw the background images
		g2.drawImage(leftImage,0,0,x1,y1,0,0,leftImage.getWidth(),leftImage.getHeight(),null);
		g2.drawImage(rightImage,x2,0,x3,y2,0,0,rightImage.getWidth(),rightImage.getHeight(),null);

		drawFeatures(g2,scaleLeft,0,0,scaleRight,x2,0);
	}

	/**
	 * Implement this function to draw features related to each image.
	 *
	 * @param scaleLeft Scale of left image.
	 * @param leftX Left image (0,0) coordinate.
	 * @param leftY Left image (0,0) coordinate.
	 * @param scaleRight Scale of right image.
	 * @param rightX Right image (0,0) coordinate.
	 * @param rightY Right image (0,0) coordinate.
	 */
	protected abstract void drawFeatures( Graphics2D g2 ,
										  double scaleLeft , int leftX , int leftY ,
										  double scaleRight , int rightX , int rightY );

	/**
	 * Compute individually how each image will be scaled
	 */
	private void computeScales() {
		int width = getWidth();
		int height = getHeight();

		width = (width-borderSize)/2;

		// compute the scale factor for each image
		scaleLeft = scaleRight = 1;
		if( leftImage.getWidth() > width || leftImage.getHeight() > height ) {
			double scaleX = (double)width/(double)leftImage.getWidth();
			double scaleY = (double)height/(double)leftImage.getHeight();
			scaleLeft = Math.min(scaleX,scaleY);
		}
		if( rightImage.getWidth() > width || rightImage.getHeight() > height ) {
			double scaleX = (double)width/(double)rightImage.getWidth();
			double scaleY = (double)height/(double)rightImage.getHeight();
			scaleRight = Math.min(scaleX,scaleY);
		}
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

			int bestIndex = findBestPoint(x, y, leftPts );

			if( bestIndex != -1 ) {
				selectedIndex = bestIndex;
			}

		} else if( e.getX() >= rightBeginX ) {
			selectedIsLeft = false;

			int x = (int)((e.getX()-rightBeginX)/scaleRight);
			int y = (int)(e.getY()/scaleRight);

			int bestIndex = findBestPoint(x, y, rightPts );

			if( bestIndex != -1 ) {
				selectedIndex = bestIndex;
			}

		}
//		System.out.println("selected index "+selectedIndex);
		repaint();
	}

	private int findBestPoint(int x, int y,  List<Point2D_F64> pts ) {
		double bestDist = clickDistance;
		int bestIndex = -1;
		for( int i = 0; i < pts.size(); i++ ) {
			if( !isValidPoint(i) )
				continue;

			Point2D_F64 p = pts.get(i);
			double d = UtilPoint2D_F64.distance(p.x, p.y, x, y);
			if( d < bestDist ) {
				bestDist = d;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	protected abstract boolean isValidPoint( int index );

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}
}
