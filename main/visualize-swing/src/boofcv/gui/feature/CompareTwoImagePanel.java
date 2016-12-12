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

package boofcv.gui.feature;

import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import org.ddogleg.struct.GrowQueue_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


/**
 * Panel for displaying two images next to each other separated by a border.
 *
 * @author Peter Abeles
 */
public abstract class CompareTwoImagePanel extends JPanel implements MouseListener , MouseMotionListener{

	// how close a click needs to be to a point
	private double clickDistance = 20;

	// list of features in both images
	protected List<Point2D_F64> leftPts,rightPts;

	// draw a selected pair
	List<Integer> selected = new ArrayList<>();
	protected boolean selectedIsLeft;

	// can it select more than one?
	protected boolean selectRegion;
	
	// size of the border between the images
	protected int borderSize;

	// left and right window information
	protected BufferedImage leftImage,rightImage;
	protected double scaleLeft,scaleRight;

	// where it first clicked when selecting a region
	protected Point2D_I32 firstClick;
	// current position of the mouse while being dragged
	protected Point2D_I32 mousePosition = new Point2D_I32();
	
	public CompareTwoImagePanel(int borderSize , boolean canSelectRegion) {
		this.borderSize = borderSize;
		this.selectRegion = canSelectRegion;
		addMouseListener(this);
		addMouseMotionListener(this);
	}

	public void setLocation( List<Point2D_F64> leftPts , List<Point2D_F64> rightPts) {
		this.leftPts = leftPts;
		this.rightPts = rightPts;
		selected.clear();
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
		
		// draw the selected region
		if( selectRegion && firstClick != null ) {
			int x0 = mousePosition.getX() < firstClick.x ? mousePosition.getX() : firstClick.x;
			x1 = mousePosition.getX() >= firstClick.x ? mousePosition.getX() : firstClick.x;
			int y0 = mousePosition.getY() < firstClick.y ? mousePosition.getY() : firstClick.y;
			y1 = mousePosition.getY() >= firstClick.y ? mousePosition.getY() : firstClick.y;

			g2.setColor(Color.WHITE);
			g2.setStroke(new BasicStroke(3));
			g2.drawRect(x0,y0,x1-x0,y1-y0);
			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke(1));
			g2.drawRect(x0,y0,x1-x0,y1-y0);
		}
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
		firstClick = null;
		selected.clear();
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

			findBestPoints(x, y, leftPts , selected );

		} else if( e.getX() >= rightBeginX ) {
			selectedIsLeft = false;

			int x = (int)((e.getX()-rightBeginX)/scaleRight);
			int y = (int)(e.getY()/scaleRight);

			findBestPoints(x, y, rightPts , selected );
		}
//		System.out.println("selected index "+selectedIndex);
		repaint();
	}

	private void findBestPoints(int x, int y,  List<Point2D_F64> pts , List<Integer> selected ) {
		double bestDist = clickDistance*clickDistance;
		GrowQueue_I32 bestIndexes = new GrowQueue_I32(20);
		for( int i = 0; i < pts.size(); i++ ) {
			if( !isValidPoint(i) )
				continue;

			Point2D_F64 p = pts.get(i);
			double d = UtilPoint2D_F64.distanceSq(p.x, p.y, x, y);
			if( d < bestDist ) {
				bestDist = d;
				bestIndexes.reset();
				bestIndexes.add(i);
			} else if( Math.abs(d - bestDist) < 0.01 ) {
				bestIndexes.add(i);
			}
		}

		if( bestIndexes.size() > 0 ) {
			int indexRight = bestIndexes.get(0);
		}

		for (int i = 0; i < bestIndexes.size(); i++) {
			selected.add(bestIndexes.get(i));
		}
	}

	protected abstract boolean isValidPoint( int index );

	@Override
	public void mousePressed(MouseEvent e) {
		if( selectRegion )
			firstClick = new Point2D_I32(e.getX(),e.getY());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if( !selectRegion ) {
			return;
		}

		// adjust the selected region for scale and the image that was selected
		int leftEndX = (int)(scaleLeft*leftImage.getWidth());
		int rightBeginX = leftEndX + borderSize;
		selectedIsLeft = e.getX() < leftEndX;

		int x0 = e.getX() < firstClick.x ? e.getX() : firstClick.x;
		int x1 = e.getX() >= firstClick.x ? e.getX() : firstClick.x;
		int y0 = e.getY() < firstClick.y ? e.getY() : firstClick.y;
		int y1 = e.getY() >= firstClick.y ? e.getY() : firstClick.y;

		double scale = selectedIsLeft ? scaleLeft : scaleRight;
		
		if( selectedIsLeft) {
			x0 /= scale;
			x1 /= scale;
		} else {
			x0 = (int)((x0 - rightBeginX)/scale);
			x1 = (int)((x1 - rightBeginX)/scale);
		}
		y0 /= scale;
		y1 /= scale;
		
		// find all the points in the region
		if( selectedIsLeft ) {
			findPointsInRegion(x0,y0,x1,y1,leftPts);
		} else {
			findPointsInRegion(x0,y0,x1,y1,rightPts);
		}
		
		// reset the selector
		firstClick = null;

		repaint();
	}
	
	private void findPointsInRegion( int x0 , int y0 , int x1 , int y1 , List<Point2D_F64> pts )
	{
		selected.clear();
		for( int i = 0; i < pts.size(); i++ ) {
			if( !isValidPoint(i) )
				continue;

			Point2D_F64 p = pts.get(i);
			
			if( p.x >= x0 && p.x < x1 && p.y >= y0 && p.y < y1 ) {
				selected.add(i);
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e){
		if( selectRegion ) {
			mousePosition.x = e.getX();
			mousePosition.y = e.getY();
			repaint();
		}
	}

	@Override
	public void mouseMoved(MouseEvent e){}
}
