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

package boofcv.gui.d2;

import georegression.struct.point.Point2D_F64;
import georegression.struct.se.Se2_F64;
import georegression.transform.se.SePointOps_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Used to display a 2D point cloud.
 *
 * @author Peter Abeles
 */
public class PlaneView2D extends JPanel implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener {

	FastQueue<Point2D_F64> points = new FastQueue<>(Point2D_F64.class, true);

	Se2_F64 transform = new Se2_F64();
	double scale = 1;

	Point2D_F64 a = new Point2D_F64();

	// previous mouse location
	int prevX;
	int prevY;

	double pixelToUnit;

	public PlaneView2D( double pixelToUnit ) {
		this.pixelToUnit = pixelToUnit;

		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}

	public void reset() {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				points.reset();
			}
		});
	}

	public void addPoint( final double x , final double y ) {
		SwingUtilities.invokeLater( new Runnable() {
			@Override
			public void run() {
				points.grow().set(x, y);
			}
		});
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		int offsetX = getWidth()/2;
		int offsetY = getHeight()/2;

		int H = getHeight()-1;

		int r = 2;
		int w =2*r+1;

		for( Point2D_F64 p : points.toList() ) {
			SePointOps_F64.transform(transform,p,a);
			a.x *= scale/pixelToUnit;
			a.y *= scale/pixelToUnit;

			g2.fillOval(offsetX+(int)a.x,H-(offsetY+(int)a.y),w,w);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if( e.getWheelRotation() > 0 ) {
			scale *= e.getWheelRotation()*1.2;
		} else {
			scale *= -e.getWheelRotation()*0.8;
		}

		if( scale < 0.001 )
			scale = 0.001;
		else if( scale > 1000 )
			scale = 1000;

		repaint();
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		grabFocus();
	}

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
	public void mouseDragged(MouseEvent e) {
		final int deltaX = e.getX()-prevX;
		final int deltaY = e.getY()-prevY;

//		if( SwingUtilities.isRightMouseButton(e) ) {
//			offsetZ -= deltaY*pixelToDistance;
//		} else if( e.isShiftDown() || SwingUtilities.isMiddleMouseButton(e)) {
//			tiltAngle += deltaY;
//		} else {
		transform.T.x += deltaX*pixelToUnit/scale;
		transform.T.y -= deltaY*pixelToUnit/scale;
//		}

		prevX = e.getX();
		prevY = e.getY();

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {
		if( e.getKeyChar() == 'w' ) {
			transform.T.y -= pixelToUnit/scale;
		} else if( e.getKeyChar() == 's' ) {
			transform.T.y += pixelToUnit/scale;
		} else if( e.getKeyChar() == 'a' ) {
			transform.T.x += pixelToUnit/scale;
		} else if( e.getKeyChar() == 'd' ) {
			transform.T.x -= pixelToUnit/scale;
		} else if( e.getKeyChar() == 'q' ) {
			scale *= 1.05;
		} else if( e.getKeyChar() == 'e' ) {
			scale *= 0.95;
		} else if( e.getKeyChar() == 'h' ) {
			transform.reset();
			scale = 1;
		}

		if( scale < 0.001 )
			scale = 0.001;
		else if( scale > 1000 )
			scale = 1000;

		repaint();
	}

	@Override
	public void keyPressed(KeyEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void keyReleased(KeyEvent e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
