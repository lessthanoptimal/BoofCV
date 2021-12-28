/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.fiducial.calib.CalibrationDetectorChessboardX;
import boofcv.alg.feature.detect.chess.ChessboardCorner;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterFinder;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerClusterToGrid;
import boofcv.alg.fiducial.calib.chess.ChessboardCornerGraph;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckDetector;
import boofcv.alg.fiducial.calib.ecocheck.ECoCheckFound;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.core.graph.FeatureGraph2D;
import boofcv.gui.calibration.UtilCalibrationGui;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;
import org.ddogleg.struct.DogArray;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;
import java.util.Objects;

/**
 * Various functions for visualizing chessboard x-corners
 *
 * @author Peter Abeles
 */
public class VisualizeChessboardXCornerUtils {
	public final DogArray<ChessboardCorner> foundCorners = new DogArray<>(ChessboardCorner::new);
	public final DogArray<CalibrationObservation> foundGrids = new DogArray<>(CalibrationObservation::new);
	public final DogArray<FeatureGraph2D> foundClusters = new DogArray<>(FeatureGraph2D::new);

	// Workspace for Swing
	Ellipse2D.Double circle = new Ellipse2D.Double();
	BasicStroke stroke2 = new BasicStroke(2);
	BasicStroke stroke5 = new BasicStroke(5);
	Line2D.Double line = new Line2D.Double();
	Font regular = new Font("Serif", Font.PLAIN, 12);

	public void update( ECoCheckDetector<?> ecocheck ) {
		DogArray<ChessboardCorner> orig = ecocheck.getDetector().getCorners();
		foundCorners.reset();
		for (int i = 0; i < orig.size; i++) {
			foundCorners.grow().setTo(orig.get(i));
		}

		{
			DogArray<ECoCheckFound> found = ecocheck.getFound();
			foundGrids.reset();
			for (int i = 0; i < found.size; i++) {
				ECoCheckFound grid = found.get(i);
				CalibrationObservation c = foundGrids.grow();
				c.points.clear();

				for (int j = 0; j < grid.corners.size(); j++) {
					c.points.add(grid.corners.get(j));
				}
			}
		}

		foundClusters.reset();
		DogArray<ChessboardCornerGraph> clusters = ecocheck.getClusterFinder().getOutputClusters();
		for (int i = 0; i < clusters.size; i++) {
			clusters.get(i).convert(foundClusters.grow());
		}
	}

	public void update( CalibrationDetectorChessboardX detector ) {
		DogArray<ChessboardCorner> orig = detector.getDetector().getCorners();
		foundCorners.reset();
		for (int i = 0; i < orig.size; i++) {
			foundCorners.grow().setTo(orig.get(i));
		}

		{
			DogArray<ChessboardCornerClusterToGrid.GridInfo> found = detector.getDetectorX().getFoundChessboard();
			foundGrids.reset();
			for (int i = 0; i < found.size; i++) {
				ChessboardCornerClusterToGrid.GridInfo grid = found.get(i);
				CalibrationObservation c = foundGrids.grow();
				c.points.clear();

				for (int j = 0; j < grid.nodes.size(); j++) {
					c.points.add(new PointIndex2D_F64(grid.nodes.get(j).corner, j));
				}
			}
		}

		foundClusters.reset();
		DogArray<ChessboardCornerGraph> clusters = detector.getClusterFinder().getOutputClusters();
		for (int i = 0; i < clusters.size; i++) {
			clusters.get(i).convert(foundClusters.grow());
		}
	}

	public void visualizeCorners( Graphics2D g2, double scale, int minPyramidLevel, boolean showNumbers ) {
		for (int i = 0; i < foundCorners.size; i++) {
			ChessboardCorner c = foundCorners.get(i);
			if (c.level2 < minPyramidLevel)
				continue;

			double x = c.x;
			double y = c.y;

			g2.setStroke(stroke5);
			g2.setColor(Color.BLACK);
			VisualizeFeatures.drawCircle(g2, x*scale, y*scale, 5, circle);
			g2.setStroke(stroke2);
			g2.setColor(Color.ORANGE);
			VisualizeFeatures.drawCircle(g2, x*scale, y*scale, 5, circle);

			double dx = 6*Math.cos(c.orientation);
			double dy = 6*Math.sin(c.orientation);

			g2.setStroke(stroke2);
			g2.setColor(Color.CYAN);
			line.setLine((x - dx)*scale, (y - dy)*scale, (x + dx)*scale, (y + dy)*scale);
			g2.draw(line);
		}

		if (showNumbers) {
			UtilCalibrationGui.drawIndexes(g2, 18, foundCorners.toList(), null,
					minPyramidLevel, scale);
		}
	}

	public void visualizeClusters( Graphics2D g2, double scale, int width, int height ) {
		for (int i = 0; i < foundClusters.size; i++) {
			FeatureGraph2D graph = foundClusters.get(i);

			// draw black outline
			g2.setStroke(new BasicStroke(5));
			g2.setColor(Color.black);
			renderGraph(g2, scale, graph);

			// make each graph a different color depending on position
			FeatureGraph2D.Node n0 = graph.nodes.get(0);
			int color = (int)((n0.x/width)*255) << 16 | ((int)((n0.y/height)*200) + 55) << 8 | 255;
			g2.setColor(new Color(color));
			g2.setStroke(new BasicStroke(3));
			renderGraph(g2, scale, graph);
		}
	}

	public void visualizePerpendicular( Graphics2D g2, double scale, ChessboardCornerClusterFinder<GrayF32> clusterFinder ) {
		g2.setFont(regular);
		BasicStroke thin = new BasicStroke(2);
		BasicStroke thick = new BasicStroke(4);
//					g2.setStroke(new BasicStroke(1));
//					List<Vertex> vertexes = detector.getClusterFinder().getVertexes().toList();
		List<ChessboardCornerClusterFinder.LineInfo> lines = clusterFinder.getLines().toList();

		for (int i = 0; i < lines.size(); i++) {
			ChessboardCornerClusterFinder.LineInfo lineInfo = lines.get(i);
			if (lineInfo.isDisconnected() || lineInfo.parallel)
				continue;

			ChessboardCornerClusterFinder.Vertex va = Objects.requireNonNull(lineInfo.endA).dst;
			ChessboardCornerClusterFinder.Vertex vb = Objects.requireNonNull(lineInfo.endB).dst;

			ChessboardCorner ca = foundCorners.get(va.index);
			ChessboardCorner cb = foundCorners.get(vb.index);

			double intensity = lineInfo.intensity == -Double.MAX_VALUE ? Double.NaN : lineInfo.intensity;
			line.setLine(ca.x*scale, ca.y*scale, cb.x*scale, cb.y*scale);

			g2.setStroke(thick);
			g2.setColor(Color.BLACK);
			g2.draw(line);

			g2.setStroke(thin);
			g2.setColor(Color.ORANGE);

			g2.draw(line);

			float x = (float)((ca.x + cb.x)/2.0);
			float y = (float)((ca.y + cb.y)/2.0);

			g2.setColor(Color.RED);
			g2.drawString(String.format("%.1f", intensity), x*(float)scale, y*(float)scale);
		}
	}

	private void renderGraph( Graphics2D g2, double scale, FeatureGraph2D graph ) {
		for (int j = 0; j < graph.edges.size; j++) {
			FeatureGraph2D.Edge e = graph.edges.get(j);
			Point2D_F64 a = graph.nodes.get(e.src);
			Point2D_F64 b = graph.nodes.get(e.dst);

			line.setLine(a.x*scale, a.y*scale, b.x*scale, b.y*scale);
			g2.draw(line);
		}
	}
}
