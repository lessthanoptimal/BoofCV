/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.d3;

import boofcv.struct.FastQueue;
import georegression.geometry.GeometryMath_F64;
import georegression.geometry.RotationMatrixGenerator;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * <p>
 * Renders a 3D point cloud using a perspective pin hole camera model.
 * </p>
 *
 * <p>
 * Rendering speed is improved by first rendering onto a grid and only accepting the highest
 * (closest to viewing camera) point as being visible.
 * </p>
 *
 * @author Peter Abeles
 */
public class PointCloudViewer extends JPanel
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {
	FastQueue<ColorPoint3D> cloud = new FastQueue<ColorPoint3D>(200,ColorPoint3D.class,true);

	// intrinsic camera parameters
	DenseMatrix64F K;
	double focalLengthX;
	double focalLengthY;
	double centerX;
	double centerY;

	// view offset
	double offsetX;
	double offsetY;
	double offsetZ;

	// Data structure that contains the visible point at each pixel
	// size = width*height, row major format
	Pixel data[] = new Pixel[0];

	// tilt angle in degrees
	public int tiltAngle = 0;
	public double radius = 5;

   // motion scale
   public double pixelToDistance;

	// previous mouse location
	int prevX;
	int prevY;

	public PointCloudViewer( double pixelToDistance ) {

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		setFocusable(true);
		requestFocus();
		this.pixelToDistance = pixelToDistance;
	}

   public PointCloudViewer(DenseMatrix64F K, double pixelToDistance ) {
		this(pixelToDistance);
		configure(K);
	}

	/**
	 * Specify camera parameters for rendering purposes
	 *
	 * @param K Intrinsic camera calibration matrix of rectified camera
	 */
	public void configure(DenseMatrix64F K) {
		this.K = K;
		this.focalLengthX = K.get(0,0);
		this.focalLengthY = K.get(1,1);
		this.centerX = K.get(0,2);
		this.centerY = K.get(1,2);
	}

	public void reset() {
		cloud.reset();
	}

	public void addPoint( double x , double y , double z , int rgb ) {
		ColorPoint3D p = cloud.grow();
		p.set(x,y,z);
		p.rgb = rgb;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		projectScene();

		int width = getWidth();
		int h = getHeight();

		int r = 2;
		int w = r*2+1;

		Graphics2D g2 = (Graphics2D)g;

		int index = 0;
		for( int y = 0; y < h; y++ ) {
			for( int x = 0; x < width; x++ ) {
				Pixel p = data[index++];
				if( p.rgb == -1 )
					continue;

				g2.setColor(new Color(p.rgb));
				g2.fillRect(x - r, y - r, w, w);
			}
		}
	}

	private void projectScene() {
		int w = getWidth();
		int h = getHeight();

		int N = w*h;

		if( data.length < N ) {
			data = new Pixel[ N ];
			for( int i = 0; i < N; i++ )
				data[i] = new Pixel();
		} else {
			for( int i = 0; i < N; i++ )
				data[i].reset();
		}

		Se3_F64 pose = createWorldToCamera();
		Point3D_F64 cameraPt = new Point3D_F64();
		Point2D_F64 pixel = new Point2D_F64();

		for( int i = 0; i < cloud.size(); i++ ) {
			ColorPoint3D p = cloud.get(i);

			SePointOps_F64.transform(pose,p,cameraPt);
			pixel.x = cameraPt.x/cameraPt.z;
			pixel.y = cameraPt.y/cameraPt.z;

			GeometryMath_F64.mult(K,pixel,pixel);

			int x = (int)pixel.x;
			int y = (int)pixel.y;

			if( x < 0 || y < 0 || x >= w || y >= h )
				continue;

			Pixel d = data[y*w+x];
			if( d.height > cameraPt.z ) {
				d.height = cameraPt.z;
				d.rgb = p.rgb;
			}
		}
	}

	public Se3_F64 createWorldToCamera() {

		Vector3D_F64 rotPt = new Vector3D_F64(offsetX,offsetY,offsetZ);

		double radians = tiltAngle*Math.PI/180.0;
		DenseMatrix64F R = RotationMatrixGenerator.eulerXYZ(radians,0,0,null);

		Se3_F64 a = new Se3_F64(R,rotPt);

		return a;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if( e.getKeyCode() == KeyEvent.VK_H ) {
			offsetX = offsetY = offsetZ = 0;
			tiltAngle = 0;
			repaint();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	/**
	 * Contains information on visible pixels
	 */
	private static class Pixel
	{
		// the pixel's height.  used to see if it is closer to the  camera or not
		public double height;
		// Color of the pixel
		public int rgb;

		private Pixel() {
			reset();
		}

		public void reset() {
			height = Double.MAX_VALUE;
			rgb = -1;
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		offsetZ -= e.getWheelRotation()*pixelToDistance;

		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {
		prevX = e.getX();
		prevY = e.getY();
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public synchronized void mouseDragged(MouseEvent e) {
		final int deltaX = e.getX()-prevX;
		final int deltaY = e.getY()-prevY;

		if( SwingUtilities.isRightMouseButton(e) ) {
			offsetZ -= deltaY*pixelToDistance;
		} else if( e.isShiftDown() || SwingUtilities.isMiddleMouseButton(e)) {
			tiltAngle += deltaY;
		} else {
			offsetX += deltaX*pixelToDistance;
			offsetY += deltaY*pixelToDistance;
		}

		prevX = e.getX();
		prevY = e.getY();

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}
}
