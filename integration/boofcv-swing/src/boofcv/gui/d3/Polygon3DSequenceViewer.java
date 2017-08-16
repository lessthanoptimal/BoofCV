/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.CameraPinhole;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.geometry.GeometryMath_F64;
import georegression.struct.EulerType;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;
import org.ejml.data.DMatrixRMaj;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a sequence polygons in 3D
 *
 * @author Peter Abeles
 */
public class Polygon3DSequenceViewer extends JPanel implements KeyListener, MouseListener,
		MouseMotionListener {

	// the shapes it's drawing
	final List<Poly> polygons = new ArrayList<>();

	// transform from world frame to camera frame
	Se3_F64 worldToCamera = new Se3_F64();

	// intrinsic camera calibration
	DMatrixRMaj K = new DMatrixRMaj(3,3);

	// how far it moves in the world frame for each key press
	double stepSize;

	// previous cursor location
	int prevX;
	int prevY;

	Line2D.Double line = new Line2D.Double();

	Point3D_F64 p1 = new Point3D_F64();
	Point3D_F64 p2 = new Point3D_F64();

	Point2D_F64 x1 = new Point2D_F64();
	Point2D_F64 x2 = new Point2D_F64();

	CameraPinhole camera = new CameraPinhole();

	public Polygon3DSequenceViewer( ) {
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);

		camera.fx = 300;
		camera.fy = 300;
	}

	public void setFocalLength(double focalLength) {
		camera.fx = focalLength;
		camera.fy = focalLength;
	}

	public double getStepSize() {
		return stepSize;
	}

	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}

	public void init() {
		synchronized (polygons) {
			worldToCamera.reset();
			polygons.clear();
		}
	}

	/**
	 * Adds a polygon to the viewer.  GUI Thread safe.
	 *
	 * @param polygon shape being added
	 */
	public void add( Color color , Point3D_F64... polygon ) {

		final Poly p = new Poly(polygon.length,color);

		for( int i = 0; i < polygon.length; i++ )
			p.pts[i] = polygon[i].copy();

		synchronized (polygons) {
			polygons.add( p );
		}
	}

	/**
	 * Adds a polygon to the viewer.  GUI Thread safe.
	 *
	 * @param polygon shape being added
	 */
	public void add( Point3D_F64... polygon ) {

		final Poly p = new Poly(polygon.length,Color.BLACK);

		for( int i = 0; i < polygon.length; i++ )
			p.pts[i] = polygon[i].copy();

		synchronized (polygons) {
			polygons.add( p );
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		camera.width = getWidth();
		camera.height = getHeight();
		camera.cx = camera.width/2;
		camera.cy = camera.height/2;
		PerspectiveOps.calibrationMatrix(camera,K);

		synchronized (polygons) {
			renderPolygons(g2);
		}
	}

	private void renderPolygons(Graphics2D g2) {
		for (Poly poly : polygons) {
			SePointOps_F64.transform(worldToCamera, poly.pts[0], p1);
			GeometryMath_F64.mult(K, p1, x1);

			// don't render what's behind the camera
			if (p1.z < 0)
				continue;

			g2.setColor(poly.color);

			boolean skip = false;
			for (int i = 1; i < poly.pts.length; i++) {
				SePointOps_F64.transform(worldToCamera, poly.pts[i], p2);
				GeometryMath_F64.mult(K, p2, x2);

				if (p2.z < 0) {
					skip = true;
					break;
				}

				line.setLine(x1.x, x1.y, x2.x, x2.y);
				g2.draw(line);

				Point3D_F64 tempP = p1;
				Point2D_F64 tempX = x1;

				p1 = p2;
				p2 = tempP;
				x1 = x2;
				x2 = tempX;
			}
			if (!skip) {
				SePointOps_F64.transform(worldToCamera, poly.pts[0], p2);
				GeometryMath_F64.mult(K, p2, x2);
				line.setLine(x1.x, x1.y, x2.x, x2.y);
				g2.draw(line);
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		Vector3D_F64 T = worldToCamera.getT();

		if( e.getKeyChar() == 'w' ) {
			T.z -= stepSize;
		} else if( e.getKeyChar() == 's' ) {
			T.z += stepSize;
		} else if( e.getKeyChar() == 'a' ) {
			T.x += stepSize;
		} else if( e.getKeyChar() == 'd' ) {
			T.x -= stepSize;
		} else if( e.getKeyChar() == 'q' ) {
			T.y -= stepSize;
		} else if( e.getKeyChar() == 'e' ) {
			T.y += stepSize;
		} else if( e.getKeyChar() == 'h' ) {
			synchronized (polygons) {
				worldToCamera.reset();
			}
		}

		repaint();
	}

	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

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
		double rotX = 0;
		double rotY = 0;
		double rotZ = 0;

		rotY += (e.getX() - prevX)*0.01;
		rotX += (prevY - e.getY())*0.01;

		Se3_F64 rotTran = new Se3_F64();
		ConvertRotation3D_F64.eulerToMatrix(EulerType.XYZ,rotX,rotY,rotZ,rotTran.getR());
		Se3_F64 temp = worldToCamera.concat(rotTran,null);
		synchronized (polygons) {
			worldToCamera.set(temp);
		}

		prevX = e.getX();
		prevY = e.getY();

		repaint();
	}

	@Override
	public void mouseMoved(MouseEvent e) {}

	private static class Poly
	{
		Point3D_F64[] pts;
		Color color;

		public Poly() {
		}

		public Poly(int length, Color color) {
			this.pts = new Point3D_F64[length];
			this.color = color;
		}
	}
}
