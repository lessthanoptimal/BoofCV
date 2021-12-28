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

package boofcv.io.geo;

import boofcv.abst.geo.bundle.SceneObservations;
import boofcv.abst.geo.bundle.SceneObservations.View;
import boofcv.abst.geo.bundle.SceneStructureCommon;
import boofcv.abst.geo.bundle.SceneStructureMetric;
import boofcv.alg.geo.bundle.cameras.BundlePinholeSnavely;
import boofcv.io.UtilIO;
import boofcv.struct.geo.PointIndex2D_F64;
import georegression.geometry.ConvertRotation3D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;

import java.io.*;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Reading and writing data in the Bundle Adjustment in the Large format.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CodecBundleAdjustmentInTheLarge {
	public SceneStructureMetric scene;
	public SceneObservations observations;

	public void parse( File file ) throws IOException {
		InputStream stream = UtilIO.openStream(file.getPath());
		if (stream == null) throw new IOException("Can't open file: " + file.getPath());

		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, UTF_8));

		String[] words = reader.readLine().split("\\s+");

		if (words.length != 3)
			throw new IOException("Unexpected number of words on first line");

		int numCameras = Integer.parseInt(words[0]);
		int numPoints = Integer.parseInt(words[1]);
		int numObservations = Integer.parseInt(words[2]);

		scene = new SceneStructureMetric(false);
		scene.initialize(numCameras, numCameras, numPoints);

		observations = new SceneObservations();
		observations.initialize(numCameras);

		for (int i = 0; i < numObservations; i++) {
			words = reader.readLine().split("\\s+");
			if (words.length != 4)
				throw new IOException("Unexpected number of words in obs");
			int cameraID = Integer.parseInt(words[0]);
			int pointID = Integer.parseInt(words[1]);
			float pixelX = Float.parseFloat(words[2]);
			float pixelY = Float.parseFloat(words[3]);

			if (pointID >= numPoints) {
				throw new RuntimeException("Out of bounds pointID");
			}
			if (cameraID >= numCameras) {
				throw new RuntimeException("Out of bounds cameraID");
			}

			observations.getView(cameraID).add(pointID, pixelX, pixelY);
		}

		Se3_F64 worldToCameraGL = new Se3_F64();
		Rodrigues_F64 rod = new Rodrigues_F64();
		for (int i = 0; i < numCameras; i++) {
			rod.unitAxisRotation.x = Double.parseDouble(reader.readLine());
			rod.unitAxisRotation.y = Double.parseDouble(reader.readLine());
			rod.unitAxisRotation.z = Double.parseDouble(reader.readLine());

			rod.theta = rod.unitAxisRotation.norm();
			if (rod.theta != 0)
				rod.unitAxisRotation.divide(rod.theta);

			worldToCameraGL.T.x = Double.parseDouble(reader.readLine());
			worldToCameraGL.T.y = Double.parseDouble(reader.readLine());
			worldToCameraGL.T.z = Double.parseDouble(reader.readLine());

			ConvertRotation3D_F64.rodriguesToMatrix(rod, worldToCameraGL.R);

			BundlePinholeSnavely camera = new BundlePinholeSnavely();

			camera.f = Double.parseDouble(reader.readLine());
			camera.k1 = Double.parseDouble(reader.readLine());
			camera.k2 = Double.parseDouble(reader.readLine());

			scene.setCamera(i, false, camera);
			scene.setView(i, i, false, worldToCameraGL);
		}

		Point3D_F64 P = new Point3D_F64();
		for (int i = 0; i < numPoints; i++) {
			P.x = Float.parseFloat(reader.readLine());
			P.y = Float.parseFloat(reader.readLine());
			P.z = Float.parseFloat(reader.readLine());

//            GeometryMath_F64.mult(glToCv.R,P,P);

			scene.setPoint(i, P.x, P.y, P.z);
		}

		for (int i = 0; i < observations.views.size; i++) {
			View v = observations.getView(i);

			for (int j = 0; j < v.point.size; j++) {
				scene.connectPointToView(v.getPointId(j), i);
			}
		}
		reader.close();

		observations.checkOneObservationPerView();
	}

	public void save( File file ) throws IOException {
		PrintStream writer = new PrintStream(file);

		writer.println(scene.views.size + " " + scene.points.size + " " + observations.getObservationCount());

		PointIndex2D_F64 o = new PointIndex2D_F64();
		for (int viewIdx = 0; viewIdx < observations.views.size; viewIdx++) {
			SceneObservations.View view = observations.views.data[viewIdx];

			for (int obsIdx = 0; obsIdx < view.size(); obsIdx++) {
				view.getPixel(obsIdx, o);
				writer.printf("%d %d %.8f %.8f\n", viewIdx, o.index, o.p.x, o.p.y);
			}
		}

		Rodrigues_F64 axisAngle = new Rodrigues_F64();
		for (int viewIdx = 0; viewIdx < scene.views.size; viewIdx++) {
			SceneStructureMetric.View view = scene.views.data[viewIdx];
			BundlePinholeSnavely camera = Objects.requireNonNull(scene.cameras.get(view.camera).getModel());
			Se3_F64 parent_to_view = scene.getParentToView(viewIdx);

			ConvertRotation3D_F64.matrixToRodrigues(parent_to_view.R, axisAngle);

			double axisX = axisAngle.unitAxisRotation.x*axisAngle.theta;
			double axisY = axisAngle.unitAxisRotation.y*axisAngle.theta;
			double axisZ = axisAngle.unitAxisRotation.z*axisAngle.theta;

			writer.printf("%.10f\n%.10f\n%.10f\n", axisX, axisY, axisZ);
			writer.printf("%.10f\n%.10f\n%.10f\n", parent_to_view.T.x, parent_to_view.T.y, parent_to_view.T.z);
			writer.printf("%.10f\n%.10f\n%.10f\n", camera.f, camera.k1, camera.k2);
		}

		for (int pointId = 0; pointId < scene.points.size; pointId++) {
			SceneStructureCommon.Point p = scene.points.data[pointId];
			writer.printf("%.10f\n%.10f\n%.10f\n", p.coordinate[0], p.coordinate[1], p.coordinate[2]);
		}
		writer.close();
	}

	public static void main( String[] args ) throws IOException {
		CodecBundleAdjustmentInTheLarge alg = new CodecBundleAdjustmentInTheLarge();

		alg.parse(new File("data/bundle_adjustment/ladybug/problem-49-7776-pre.txt"));

		System.out.println("Done!");
	}
}
