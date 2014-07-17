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

package boofcv.alg.sfm.d2;

import boofcv.gui.feature.VisualizeFeatures;
import georegression.struct.homography.Homography2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.transform.homography.HomographyPointOps_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * TODO Comment
 *
 * @author Peter Abeles
 */
public abstract class Motion2DPanel extends JPanel
{
	// rendered distorted image
	BufferedImage stitched;
	// input image
	BufferedImage input;

	int windowWidth,windowHeight;
	int distortOffX;

	Homography2D_F64 currToWorld = new Homography2D_F64();

	// copies of feature location for GUI thread
	FastQueue<Point2D_F64> inliers = new FastQueue<Point2D_F64>(300,Point2D_F64.class,true);
	FastQueue<Point2D_F64> allTracks = new FastQueue<Point2D_F64>(300,Point2D_F64.class,true);

	boolean showImageView;
	StitchingFromMotion2D.Corners corners;

	public void setImages(BufferedImage input , BufferedImage stitched)
	{
		this.input = input;
		this.stitched = stitched;
	}

	public void setCorners(StitchingFromMotion2D.Corners corners) {
		this.corners = corners;
	}

	public synchronized void setInliers(java.util.List<Point2D_F64> list) {
		inliers.reset();

		if( list != null ) {
			for( Point2D_F64 p : list ) {
				inliers.grow().set(p);
			}
		}
	}

	public synchronized void setAllTracks(java.util.List<Point2D_F64> list) {
		allTracks.reset();

		if( list != null ) {
			for( Point2D_F64 p : list ) {
				allTracks.grow().set(p);
			}
		}
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		if( stitched == null )
			return;

		Graphics2D g2 = (Graphics2D)g;

		int w = getWidth();
		int h = getHeight();

		double scaleX = w/(double) windowWidth;
		double scaleY = h/(double) windowHeight;

		double scale = Math.min(scaleX,scaleY);
		if( scale > 1 ) scale = 1;

		drawImages(scale,g2);

		drawFeatures((float)scale,g2);
//
		if(showImageView)
			drawImageBounds(g2,distortOffX,0,scale);
	}

	protected abstract void drawImages( double scale , Graphics2D g2 );

	protected abstract void drawFeatures( float scale, Graphics2D g2  );

	/**
	 * Draw features after applying a homography transformation.
	 */
	protected void drawFeatures( float scale , int offsetX , int offsetY ,
								 FastQueue<Point2D_F64> all,
								 FastQueue<Point2D_F64> inliers,
								 Homography2D_F64 currToGlobal, Graphics2D g2 ) {

		Point2D_F64 distPt = new Point2D_F64();

		for( int i = 0; i < all.size; i++  ) {
			HomographyPointOps_F64.transform(currToGlobal, all.get(i), distPt);

			distPt.x = offsetX + distPt.x*scale;
			distPt.y = offsetY + distPt.y*scale;

			VisualizeFeatures.drawPoint(g2, (int) distPt.x, (int) distPt.y, Color.RED);
		}

		for( int i = 0; i < inliers.size; i++  ) {
			HomographyPointOps_F64.transform(currToGlobal,inliers.get(i),distPt);

			distPt.x = offsetX + distPt.x*scale;
			distPt.y = offsetY + distPt.y*scale;

			VisualizeFeatures.drawPoint(g2, (int) distPt.x, (int) distPt.y, Color.BLUE);
		}
	}

	private void drawImageBounds( Graphics2D g2 , int tx , int ty , double scale ) {
		StitchingFromMotion2D.Corners c = corners;
		if( c == null )
			return;

		g2.setColor(Color.BLUE);
		drawLine(g2,tx,ty,scale,c.p0,c.p1);
		drawLine(g2,tx,ty,scale,c.p1,c.p2);
		drawLine(g2,tx,ty,scale,c.p2,c.p3);
		drawLine(g2,tx,ty,scale,c.p3,c.p0);
	}

	private void drawLine( Graphics2D g2 , int tx , int ty , double scale , Point2D_F64 p0 , Point2D_F64 p1 )
	{
		g2.drawLine((int)(p0.x*scale)+tx,(int)(p0.y*scale)+ty,(int)(p1.x*scale)+tx,(int)(p1.y*scale)+ty);
	}


	public void setCurrToWorld(Homography2D_F64 currToWorld) {
		this.currToWorld.set(currToWorld);
	}

	public void setShowImageView(boolean showImageView) {
		this.showImageView = showImageView;
	}
}
