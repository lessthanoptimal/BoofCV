/*
 * Copyright (c) 2026, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.geometry;

import boofcv.chainrule.Rodrigues3Numerics;
import boofcv.chainrule.RotationNumerics;
import boofcv.struct.calib.CameraPinhole;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import org.ddogleg.chainrule.CRArray;
import org.ddogleg.chainrule.grid.ConfigGridCoefND;
import org.ddogleg.chainrule.ops.CROperatorBilinearPolyND;
import org.ddogleg.chainrule.ops.CROperatorGridBilinearWeights;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/// The scene the three camera-and-grid examples share: a planar target seen from several views by a
/// camera whose lens shifts each pixel by a small amount, and observations of where the points
/// landed. Fit: the pose of every view and the shift field, with the pinhole held at its true value.
///
/// Several views are what makes the field identifiable. From one view a shift field can absorb
/// almost any pose error, so the two would trade off with no unique answer.
public class ChainRuleCameraGridScene {
	public static final int WIDTH = 640, HEIGHT = 480;
	public static final RotationNumerics ROTATION = new Rodrigues3Numerics();
	public static final int POSE_DOF = 3 + 3;

	public final CameraPinhole pinhole = new CameraPinhole(500, 500, 0, WIDTH/2, HEIGHT/2, WIDTH, HEIGHT);
	public final ConfigGridCoefND gridShape = new ConfigGridCoefND().cols(3).rows(3).coefs(1).numOut(2);

	public final List<Point3D_F64> target = ChainRuleExampleOps.createTarget(6, 5, 0.05);
	public final List<Se3_F64> views = ChainRuleExampleOps.createViews();

	/// Observed pixel of every point of every view, views laid end to end
	public final List<Point2D_F64> observations = new ArrayList<>();

	/// The shift field the observations were rendered with
	public final double[] truthGrid;

	public ChainRuleCameraGridScene() {
		truthGrid = new double[gridShape.dof()];
		for (int i = 0; i < truthGrid.length; i++) {
			truthGrid[i] = 2.5*Math.sin(i*1.9); // a couple of pixels, like a real lens
		}

		var rand = new Random(0xDEED);
		var inView = new Point3D_F64();
		for (Se3_F64 targetToView : views) {
			for (Point3D_F64 p : target) {
				targetToView.transform(p, inView);
				Point2D_F64 pixel = project(inView);
				shift(pixel, truthGrid, pixel);
				pixel.x += rand.nextGaussian()*0.05;
				pixel.y += rand.nextGaussian()*0.05;
				observations.add(pixel);
			}
		}
	}

	/// Pinhole projection of a point already in the camera's frame
	public Point2D_F64 project( Point3D_F64 inView ) {
		return new Point2D_F64(pinhole.fx*inView.x/inView.z + pinhole.cx,
				pinhole.fy*inView.y/inView.z + pinhole.cy);
	}

	/// Adds the field's shift at a pixel, which is what every example is fitting
	public void shift( Point2D_F64 pixel, double[] grid, Point2D_F64 output ) {
		var weights = new CROperatorGridBilinearWeights().configure(WIDTH, HEIGHT, gridShape);
		var poly = new CROperatorBilinearPolyND().configure(gridShape);
		var scratch = new CRArray<double[]>();
		var coors = new int[ConfigGridCoefND.NUM_KNOTS];
		var out = new CRArray<>(new double[2], 0);

		weights.setInput(CROperatorGridBilinearWeights.IN_PIXEL,
				new CRArray<>(new double[]{pixel.x, pixel.y}, 0));
		var w = new CRArray<>(new double[ConfigGridCoefND.NUM_KNOTS], 0);
		weights.forward(w);

		gridShape.knotOffsets(weights.col(), weights.row(), 0, coors);
		poly.setInput(CROperatorBilinearPolyND.IN_WEIGHTS, w);
		poly.setInput(CROperatorBilinearPolyND.IN_VALUE, new CRArray<>(new double[]{0}, 0));
		for (int knot = 0; knot < ConfigGridCoefND.NUM_KNOTS; knot++) {
			poly.setInput(CROperatorBilinearPolyND.IN_KNOT_0 + knot, scratch.wrap(grid, coors[knot]));
		}
		poly.forward(out);

		output.setTo(pixel.x + out.a[0], pixel.y + out.a[1]);
	}

	/// Poses laid end to end, then the grid: the parameter vector all three examples fit
	public double[] createParameters() {
		var param = new double[views.size()*POSE_DOF + gridShape.dof()];
		for (int i = 0; i < views.size(); i++) {
			// seeded at the true pose so the examples stay about wiring rather than initialization
			Se3_F64 v = views.get(i);
			int offset = i*POSE_DOF;
			param[offset] = v.T.x;
			param[offset + 1] = v.T.y;
			param[offset + 2] = v.T.z;
			ROTATION.matrixToParam(v.R, param, offset + 3);
		}
		return param;
	}

	public int offsetGrid() {return views.size()*POSE_DOF;}

	public int numParameters() {return offsetGrid() + gridShape.dof();}

	/// Where the model says every observation landed, for the error report
	public List<Point2D_F64> predict( double[] param ) {
		var found = new ArrayList<Point2D_F64>();
		var grid = new double[gridShape.dof()];
		System.arraycopy(param, offsetGrid(), grid, 0, grid.length);

		var targetToView = new Se3_F64();
		var inView = new Point3D_F64();
		for (int i = 0; i < views.size(); i++) {
			int offset = i*POSE_DOF;
			ROTATION.set(param, offset + 3);
			ROTATION.toMatrix(targetToView.R);
			targetToView.T.setTo(param[offset], param[offset + 1], param[offset + 2]);

			for (Point3D_F64 p : target) {
				targetToView.transform(p, inView);
				Point2D_F64 pixel = project(inView);
				shift(pixel, grid, pixel);
				found.add(pixel);
			}
		}
		return found;
	}
}
