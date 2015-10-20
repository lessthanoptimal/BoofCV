/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

package boofcv.demonstrations.calibration;

import boofcv.alg.feature.detect.squares.SquareEdge;
import boofcv.alg.feature.detect.squares.SquareGrid;
import boofcv.alg.feature.detect.squares.SquareNode;
import boofcv.alg.filter.binary.Contour;
import boofcv.gui.DemonstrationBase;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.feature.VisualizeShapes;
import boofcv.gui.image.ImageZoomPanel;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.distort.PointTransform_F32;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import georegression.struct.point.Point2D_F32;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static boofcv.gui.fiducial.VisualizeFiducial.drawLine;

/**
 * base class for chessboard and square grid calibration target detection
 *
 * @author Peter Abeles
 */
public abstract class CommonDetectCalibrationApp extends DemonstrationBase
		implements ChessboardPanel.Listener
{
	ImageFloat32 gray = new ImageFloat32(1,1);

	boolean success;

	ChessboardPanel controlPanel = new ChessboardPanel(true);

	VisualizePanel imagePanel = new VisualizePanel();
	BufferedImage input;
	BufferedImage binary;

	public CommonDetectCalibrationApp(List<String> exampleInputs ) {
		super(exampleInputs);
		add(imagePanel,BorderLayout.CENTER);
		add(controlPanel,BorderLayout.WEST);

		controlPanel.setListener(this);

		imagePanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				double scale = imagePanel.getScale();
				System.out.println("clicked at " + (e.getX()/scale) + " " + (e.getY()/scale));
			}
		});
	}

	protected abstract boolean process( ImageFloat32 image );

	protected abstract ImageUInt8 getBinaryImage();

	protected abstract java.util.List<java.util.List<SquareNode>> getClusters();

	protected abstract java.util.List<Point2D_F64> getCalibrationPoints();

	protected abstract java.util.List<Contour> getContours();

	protected abstract FastQueue<Polygon2D_F64> getFoundPolygons();

	protected abstract java.util.List<SquareGrid> getGrids();

	public void process( final BufferedImage input ) {
		this.input = input;

		imagePanel.setBufferedImage(this.input);
		gray.reshape(input.getWidth(), input.getHeight());
		ConvertBufferedImage.convertFrom(input, gray, true);

		binary = new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);

		new Thread() {
			@Override
			public void run() {
				calibEventProcess();
			}
		}.start();

	}

	private void renderGraph( Graphics2D g2 , double scale ) {
		java.util.List<java.util.List<SquareNode>> graphs = getClusters();

		g2.setStroke(new BasicStroke(3));
		for( int i = 0; i < graphs.size(); i++ ) {

			java.util.List<SquareNode> graph = graphs.get(i);

			int key = graphs.size() == 1 ? 0 : 255 * i / (graphs.size() - 1);

			int rgb = key << 8 | (255 - key);
			g2.setColor(new Color(rgb));

			java.util.List<SquareEdge> edges = new ArrayList<SquareEdge>();

			for( SquareNode n : graph ) {
				for (int j = 0; j < 4; j++) {
					if( n.edges[j] != null && !edges.contains(n.edges[j])) {
						edges.add( n.edges[j]);
					}
				}
			}

			for( SquareEdge e : edges ) {
				Point2D_F64 a = e.a.center;
				Point2D_F64 b = e.b.center;

				g2.drawLine((int)(a.x*scale+0.5),(int)(a.y*scale+0.5),(int)(b.x*scale+0.5),(int)(b.y*scale+0.5));
			}
		}
	}

	@Override
	public void calibEventGUI() {
		if( controlPanel.getSelectedView() == 0 ) {
			imagePanel.setBufferedImage(input);
		} else if( controlPanel.getSelectedView() == 1 ){
			imagePanel.setBufferedImage(binary);
		} else {
			throw new RuntimeException("Unknown");
		}

		imagePanel.setScale(controlPanel.getScale());
		imagePanel.repaint();
	}

	@Override
	public void calibEventProcess() {
		synchronized ( this ) {
			success = process(gray);
			VisualizeBinaryData.renderBinary(getBinaryImage(), false, binary);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (success)
					controlPanel.setSuccessMessage("FOUND", true);
				else
					controlPanel.setSuccessMessage("FAILED", false);
				imagePanel.setPreferredSize(new Dimension(input.getWidth(), input.getHeight()));
			}
		});
		imagePanel.repaint();
	}

	class VisualizePanel extends ImageZoomPanel {
		@Override
		protected void paintInPanel(AffineTransform tran,Graphics2D g2 ) {

			synchronized ( CommonDetectCalibrationApp.this ) {

				if( success ) {
					if ( controlPanel.isShowPoints() ) {
						java.util.List<Point2D_F64> candidates = getCalibrationPoints();
						for (Point2D_F64 c : candidates) {
							VisualizeFeatures.drawPoint(g2, (int) (scale * c.x + 0.5), (int) (scale * c.y + 0.5), 1, Color.RED);
						}
					}
					if( controlPanel.isShowNumbers() ) {
						drawNumbers(g2, getCalibrationPoints(),null,scale);
					}
				}

				if( controlPanel.doShowContour ) {
					java.util.List<Contour> contour = getContours();

					g2.setStroke(new BasicStroke(1));
					g2.setColor(Color.RED);
					VisualizeBinaryData.renderExternal(contour,scale,g2);
				}

				if( controlPanel.isShowGraph() ) {
					renderGraph(g2, scale);
				}

				if( controlPanel.isShowOrder() ) {
					renderOrder(g2,scale);
				}

				if( controlPanel.isShowSquares() ) {
					FastQueue<Polygon2D_F64> squares =  getFoundPolygons();

					for (int i = 0; i < squares.size(); i++) {
						Polygon2D_F64 p = squares.get(i);
						if (isInGrids(p))
							continue;
						g2.setColor(Color.cyan);
						g2.setStroke(new BasicStroke(4));
						drawPolygon(p, g2, scale);
						g2.setColor(Color.blue);
						g2.setStroke(new BasicStroke(2));
						drawPolygon(p, g2, scale);

						drawCornersInside(g2,scale,p);
					}
				}

				if( controlPanel.isShowGrids() ) {
					java.util.List<SquareGrid> grids = getGrids();

					for (int i = 0; i < grids.size(); i++) {
						SquareGrid g = grids.get(i);
						int a = grids.size() == 1 ? 0 : 255 * i / (grids.size() - 1);

						int rgb = a << 16 | (255 - a) << 8;

						g2.setStroke(new BasicStroke(3));

						Color color = new Color(rgb);
						for (int j = 0; j < g.nodes.size(); j++) {
							SquareNode n = g.nodes.get(j);
							g2.setColor(color);
							VisualizeShapes.drawPolygon(n.corners, true, scale, g2);

							drawCornersInside(g2,scale,n.corners);
						}
					}
				}
			}
		}
	}

	private void renderOrder(Graphics2D g2, double scale ) {
		java.util.List<SquareGrid> grids = getGrids();
		g2.setStroke(new BasicStroke(3));

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);
			int a = grids.size() == 1 ? 0 : 255 * i / (grids.size() - 1);

			for (int j = 0; j < g.nodes.size() - 1; j++) {
				double fraction = j / ((double) g.nodes.size() - 1);
				fraction = fraction * 0.6 + 0.4;

				int lineRGB = (int) (fraction * a) << 16 | (int) (fraction * (255 - a)) << 8;

				g2.setColor(new Color(lineRGB));
				SquareNode p0 = g.nodes.get(j);
				SquareNode p1 = g.nodes.get(j + 1);
				g2.drawLine((int)(scale*p0.center.x+0.5), (int) (scale*p0.center.y+0.5),
						(int)(scale*p1.center.x+0.5), (int)(scale*p1.center.y+0.5));
			}
		}
	}

	public static void drawPolygon( Polygon2D_F64 polygon, Graphics2D g2 , double scale ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Line2D.Double l = new Line2D.Double();

		for( int i = 0; i < polygon.size()-1; i++ ) {
			Point2D_F64 p0 = polygon.get(i);
			Point2D_F64 p1 = polygon.get(i+1);
			drawLine(g2, l, p0.x*scale,  p0.y*scale,  p1.x*scale,  p1.y*scale);
		}
		if(  polygon.size() > 0) {
			Point2D_F64 p0 = polygon.get(0);
			Point2D_F64 p1 = polygon.get(polygon.size()-1);
			drawLine(g2, l, p0.x*scale, p0.y*scale, p1.x*scale, p1.y*scale);
		}

	}

	private boolean isInGrids( Polygon2D_F64 p ) {
		java.util.List<SquareGrid> grids = getGrids();

		for (int i = 0; i < grids.size(); i++) {
			SquareGrid g = grids.get(i);
			for (int j = 0; j < g.nodes.size(); j++) {
				if( g.nodes.get(j).corners == p )
					return true;
			}
		}
		return false;
	}

	private void drawCornersInside( Graphics2D g2 , double scale, Polygon2D_F64 poly ) {
		double x0 = poly.get(0).x*scale;
		double y0 = poly.get(0).y*scale;

		double x1 = poly.get(1).x*scale;
		double y1 = poly.get(1).y*scale;

		double x2 = poly.get(2).x*scale;
		double y2 = poly.get(2).y*scale;

		double x3 = poly.get(3).x*scale;
		double y3 = poly.get(3).y*scale;

		double dx02 = x2-x0;
		double dy02 = y2-y0;

		double dx13 = x3-x1;
		double dy13 = y3-y1;

		double fraction = 0.2;

		x0 += dx02*fraction;
		y0 += dy02*fraction;

		x2 -= dx02*fraction;
		y2 -= dy02*fraction;

		x1 += dx13*fraction;
		y1 += dy13*fraction;

		x3 -= dx13*fraction;
		y3 -= dy13*fraction;

		VisualizeFeatures.drawPoint(g2, x0, y0, 3, Color.RED, false);
		VisualizeFeatures.drawPoint(g2, x1, y1, 3, new Color(190, 0, 0), false);
		VisualizeFeatures.drawPoint(g2, x2, y2, 3, Color.GREEN, false);
		VisualizeFeatures.drawPoint(g2, x3, y3, 3, new Color(0, 190, 0), false);

	}

	public static void drawNumbers( Graphics2D g2 , java.util.List<Point2D_F64> foundTarget ,
									PointTransform_F32 transform ,
									double scale ) {

		Font regular = new Font("Serif", Font.PLAIN, 16);
		g2.setFont(regular);

		Point2D_F32 adj = new Point2D_F32();

		AffineTransform origTran = g2.getTransform();
		for (int i = 0; i < foundTarget.size(); i++) {
			Point2D_F64 p = foundTarget.get(i);

			if (transform != null) {
				transform.compute((float) p.x, (float) p.y, adj);
			} else {
				adj.set((float) p.x, (float) p.y);
			}

			String text = String.format("%2d", i);

			int x = (int) (adj.x * scale);
			int y = (int) (adj.y * scale);

			g2.setColor(Color.BLACK);
			g2.drawString(text, x - 1, y);
			g2.drawString(text, x + 1, y);
			g2.drawString(text, x, y - 1);
			g2.drawString(text, x, y + 1);
			g2.setTransform(origTran);
			g2.setColor(Color.GREEN);
			g2.drawString(text, x, y);
		}
	}

	public void openFile(File file) {
		BufferedImage buffered = UtilImageIO.loadImage(file.getAbsolutePath());
		if( buffered == null ) {
			// TODO see if it's a video instead
			System.err.println("Couldn't read "+file.getPath());
			System.exit(0);
		} else {
			process(buffered);
		}
	}
}
