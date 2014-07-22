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

package boofcv.gui.tracker;

import georegression.geometry.UtilPolygons2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.RectangleLength2D_I32;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * Panel for displaying results of {@link boofcv.abst.tracker.TrackerObjectQuad}.
 *
 * @author Peter Abeles
 */
public class TrackerObjectQuadPanel extends JPanel implements MouseListener {

	boolean selectMode = true;

	int numSelected = 0;

	Quadrilateral_F64 quad = new Quadrilateral_F64();
	boolean targetVisible;

	Listener listener;

	BufferedImage bg;

	public TrackerObjectQuadPanel(Listener listener) {
		super(new BorderLayout());
		this.listener = listener;
		if( listener != null ){
			addMouseListener(this);
			grabFocus();
		}
	}

	public synchronized void setBackGround( BufferedImage image ) {
		if( bg == null || bg.getWidth() != image.getWidth() || bg.getHeight() != image.getHeight() ) {
			bg = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_RGB);
		}
		bg.createGraphics().drawImage(image,0,0,null);
	}

	public synchronized void enterSelectMode() {
		selectMode = true;
		numSelected = 0;
		repaint();
	}

	public synchronized void setTarget( Quadrilateral_F64 quad , boolean visible ) {
		if( quad != null )
			this.quad.set(quad);
		selectMode = false;
		targetVisible = visible;
		repaint();
	}

	public synchronized void setTarget( RectangleLength2D_I32 rect , boolean visible ) {
		if( quad != null )
			UtilPolygons2D_F64.convert(rect, quad);
		selectMode = false;
		targetVisible = visible;
		repaint();
	}

	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;

		g2.drawImage(bg,0,0,null);

		g2.setStroke(new BasicStroke(5));
		g2.setColor(Color.gray);

		if( selectMode ) {
			if( numSelected == 1 ) {
				drawCorner(g2, quad.a, Color.RED);
			} else if( numSelected == 2 ) {
				drawLine(g2, quad.a, quad.b);
				drawCorner(g2,quad.a,Color.RED);
				drawCorner(g2,quad.b,Color.ORANGE);
			} else if( numSelected == 3 ) {
				drawLine(g2,quad.a,quad.b);
				drawLine(g2, quad.b, quad.c);
				drawCorner(g2,quad.a,Color.RED);
				drawCorner(g2,quad.b,Color.ORANGE);
				drawCorner(g2,quad.c,Color.CYAN);
			} else if( numSelected == 4 ) {
				drawLine(g2,quad.a,quad.b);
				drawLine(g2,quad.b,quad.c);
				drawLine(g2,quad.c,quad.d);
				drawLine(g2,quad.d,quad.a);
				drawCorner(g2,quad.a,Color.RED);
				drawCorner(g2,quad.b,Color.ORANGE);
				drawCorner(g2,quad.c,Color.CYAN);
				drawCorner(g2,quad.d,Color.BLUE);
			}
		} else if( targetVisible ) {
			drawLine(g2,quad.a,quad.b);
			drawLine(g2,quad.b,quad.c);
			drawLine(g2,quad.c,quad.d);
			drawLine(g2,quad.d,quad.a);
			drawCorner(g2,quad.a,Color.RED);
			drawCorner(g2,quad.b,Color.ORANGE);
			drawCorner(g2,quad.c,Color.CYAN);
			drawCorner(g2,quad.d,Color.BLUE);
		}
	}

	private void drawLine( Graphics2D g2 , Point2D_F64 a , Point2D_F64 b ) {
		g2.drawLine((int)a.x,(int)a.y,(int)b.x,(int)b.y);
	}

	private void drawCorner( Graphics2D g2 , Point2D_F64 a , Color color ) {

		int r = 6;
		int w = r*2+1;
		int x = (int)a.x - r;
		int y = (int)a.y - r;

		g2.setColor(color);
		g2.fillOval(x, y, w, w);
	}

	@Override
	public synchronized void mouseClicked(MouseEvent e) {
		if( !selectMode || numSelected >= 4 )
			return;

		if( numSelected == 0 ) {
			quad.a.set(e.getX(),e.getY());
		} else if( numSelected == 1 ) {
			quad.b.set(e.getX(),e.getY());
		} else if( numSelected == 2 ) {
			quad.c.set(e.getX(),e.getY());
		} else if( numSelected == 3 ) {
			quad.d.set(e.getX(),e.getY());
		}
		numSelected++;
		repaint();

		if( numSelected == 4 ) {
			listener.selectedTarget(quad);
			selectMode = false;
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if( !selectMode ) {
			if( e.getButton() == 2 ) {
				System.out.println("Entering select mode");
				enterSelectMode();
			} else {
				listener.togglePause();
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	public boolean isSelectMode() {
		return selectMode;
	}

	public synchronized void setDefaultTarget(Quadrilateral_F64 target) {
		this.quad.set(target);
		numSelected = 4;
		selectMode = false;
		targetVisible = true;
		repaint();
	}

	public static interface Listener
	{
		public void selectedTarget( Quadrilateral_F64 target );

		public void togglePause();
	}
}
