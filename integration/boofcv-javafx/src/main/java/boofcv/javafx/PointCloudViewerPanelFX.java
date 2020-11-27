/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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

package boofcv.javafx;

import georegression.geometry.ConvertRotation3D_F64;
import georegression.metric.UtilAngle;
import georegression.struct.se.Se3_F64;
import georegression.struct.so.Rodrigues_F64;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import org.ddogleg.struct.DogArray_F32;
import org.ddogleg.struct.DogArray_I32;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Point cloud viewer written in JavaFX
 *
 * @author Peter Abeles
 */
public class PointCloudViewerPanelFX extends JFXPanel {

	final Group root = new Group();
	final Xform axisGroup = new Xform();
	final Xform world = new Xform();
	final PerspectiveCamera camera = new PerspectiveCamera(true);
	final Xform cameraXform = new Xform();
	final Xform cameraXform2 = new Xform();
	final Xform cameraXform3 = new Xform();
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 10000.0;
	private static final double CONTROL_MULTIPLIER = 0.1;
	private static final double SHIFT_MULTIPLIER = 10.0;
	private static final double MOUSE_SPEED = 0.1;
	private static final double ROTATION_SPEED = 2.0;
	private static final double TRACK_SPEED = 0.3;

	float pointRadius=1f;

	double mousePosX;
	double mousePosY;
	double mouseOldX;
	double mouseOldY;
	double mouseDeltaX;
	double mouseDeltaY;

	// Mesh for points added one at a time
	TriangleMesh meshSingles = new TriangleMesh();
	DogArray_F32 pointSingles = new DogArray_F32();
	DogArray_I32 facesSingles = new DogArray_I32();
	final Object lockSingles = new Object();
	Timer timerSingles = null;
	TimerTask taskSingles = null;

	// description of the primitive shape
	float[] templatePoints;
	int[] templateFaces;
	float[] texCoords;

	public void initialize() {
		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);

		buildCamera();
		buildAxes();

		Scene scene = new Scene(root, 1024, 768, true);
		scene.setFill(Color.GREY);
//		handleKeyboard(scene, world);
		handleMouse(scene, world);
		setPointRadius(pointRadius);
		scene.setCamera(camera);
		setScene(scene);
	}

	public void showAxis( boolean active ) {
		axisGroup.setVisible(active);
	}

	public void setPointRadius( float radius ) {
		this.pointRadius = radius;
		declareTetrahedron(radius);
	}

	public void clearPoints() {
		world.getChildren().clear();
	}

	public void addCloud(DogArray_F32 cloudXYZ) {
		int N = cloudXYZ.size/3;

		float[] points = new float[ templatePoints.length*N ];
		int[] faces = new int[ templateFaces.length*N ];
		int numPointsInShape = templatePoints.length/3;

		for (int indexPoint = 0; indexPoint < N; indexPoint++) {

			float cx = cloudXYZ.data[indexPoint*3];
			float cy = cloudXYZ.data[indexPoint*3+1];
			float cz = cloudXYZ.data[indexPoint*3+2];

			int idx = templatePoints.length*indexPoint;
			for (int j = 0; j < templatePoints.length; ) {
				points[idx++] = templatePoints[j++] + cx;
				points[idx++] = templatePoints[j++] + cy;
				points[idx++] = templatePoints[j++] + cz;
			}

			idx = templateFaces.length*indexPoint;
			for (int j = 0; j < templateFaces.length;) {
				faces[idx++] = templateFaces[j++] + numPointsInShape*indexPoint;
				faces[idx++] = templateFaces[j++];
			}

		}

		TriangleMesh mesh = new TriangleMesh();
		mesh.getPoints().setAll(points);
		mesh.getTexCoords().setAll(texCoords);
		mesh.getFaces().setAll(faces);

		final MeshView meshView = new MeshView(mesh);
		meshView.setMaterial(new PhongMaterial(Color.RED));
		meshView.setDepthTest(DepthTest.ENABLE);
		meshView.setDrawMode(DrawMode.FILL);
		meshView.setCullFace(CullFace.BACK);

		world.getChildren().addAll(meshView);
	}

	public void addPoint( float x , float y , float z ) {
		synchronized (lockSingles) {
			int indexPoint = pointSingles.size / templatePoints.length;
			int numPointsInShape = templatePoints.length / 3;

			for (int j = 0; j < templatePoints.length; ) {
				pointSingles.add(templatePoints[j++] + x);
				pointSingles.add(templatePoints[j++] + y);
				pointSingles.add(templatePoints[j++] + z);
			}

			for (int j = 0; j < templateFaces.length; ) {
				facesSingles.add(templateFaces[j++] + numPointsInShape * indexPoint);
				facesSingles.add(templateFaces[j++]);
			}
		}

		// for performance reasons we don't want to rapidly spam javafx. So we create
		// a timer task where it is update only if no  point has been added in the last 20 MS
		synchronized (lockSingles) {
			if( timerSingles != null ) {
				// already scheduled. Let it be
				return;
			}				// no task so create a new one
			taskSingles = new TimerTask() {
				@Override
				public void run() {
					Platform.runLater(()->{
						synchronized (lockSingles) {
							boolean firstPoint = meshSingles.getPoints().size() == 0;
							meshSingles.getPoints().setAll(pointSingles.data, 0, pointSingles.size);
							meshSingles.getFaces().setAll(facesSingles.data, 0, facesSingles.size);
							if (firstPoint) {
								meshSingles.getTexCoords().setAll(texCoords);
								final MeshView meshView = new MeshView(meshSingles);
								meshView.setMaterial(new PhongMaterial(Color.RED));
								meshView.setDepthTest(DepthTest.ENABLE);
								meshView.setDrawMode(DrawMode.FILL);
								meshView.setCullFace(CullFace.BACK);

								world.getChildren().addAll(meshView);
							}
							timerSingles = null;
							taskSingles = null;
						}
					});
				}
			};
			timerSingles = new Timer(true);
			timerSingles.schedule(taskSingles,20);
		}
	}

	private void buildCamera() {
		root.getChildren().add(cameraXform);
		cameraXform.getChildren().add(cameraXform2);
		cameraXform2.getChildren().add(cameraXform3);
		cameraXform3.getChildren().add(camera);

		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
	}

	public void setHorizontalFieldOfView( double radians ) {
		camera.setFieldOfView(UtilAngle.degree(radians));
	}

	public void setCameraToWorld( Se3_F64 cameraToWorld ) {
		camera.setTranslateX(cameraToWorld.T.x);
		camera.setTranslateY(cameraToWorld.T.y);
		camera.setTranslateZ(cameraToWorld.T.z);

		Rodrigues_F64 rod = new Rodrigues_F64();
		ConvertRotation3D_F64.matrixToRodrigues(cameraToWorld.R,rod);

		Point3D V = new Point3D(rod.unitAxisRotation.x,rod.unitAxisRotation.y,rod.unitAxisRotation.z);

		camera.setRotationAxis(V);
		camera.setRotate(UtilAngle.degree(rod.theta));
	}

	private void buildAxes() {
		final PhongMaterial redMaterial = new PhongMaterial();
		redMaterial.setDiffuseColor(Color.DARKRED);
		redMaterial.setSpecularColor(Color.RED);

		final PhongMaterial greenMaterial = new PhongMaterial();
		greenMaterial.setDiffuseColor(Color.DARKGREEN);
		greenMaterial.setSpecularColor(Color.GREEN);

		final PhongMaterial blueMaterial = new PhongMaterial();
		blueMaterial.setDiffuseColor(Color.DARKBLUE);
		blueMaterial.setSpecularColor(Color.BLUE);

		float axisLength = pointRadius*200;

		final Box xAxis = new Box(axisLength, pointRadius, pointRadius);
		final Box yAxis = new Box(pointRadius, axisLength, pointRadius);
		final Box zAxis = new Box(pointRadius, pointRadius, axisLength);

		xAxis.setMaterial(redMaterial);
		yAxis.setMaterial(greenMaterial);
		zAxis.setMaterial(blueMaterial);

		axisGroup.getChildren().addAll(xAxis, yAxis, zAxis);
		axisGroup.setVisible(false);
		world.getChildren().addAll(axisGroup);
	}

	private void declareTetrahedron( float r ) {
		templatePoints = new float[]{
				0,-r,0,
				-r,r,-r,
				r,r,-r,
				0,r,r,
		};

		templateFaces = new int[]{
				0,0,
				1,1,
				2,2,
				0,0,
				2,1,
				3,2,
				0,0,
				3,1,
				1,2,
				3,0,
				2,1,
				1,2
		};

		texCoords = new float[]{
				0,0,
				1,0,
				0,1,
		};
	}

	private void handleMouse(Scene scene, final Node root) {
		scene.setOnMousePressed(me -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
		});
		scene.setOnMouseDragged(me -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = (mousePosX - mouseOldX);
			mouseDeltaY = (mousePosY - mouseOldY);

			double modifier = 1.0;

			if (me.isControlDown()) {
				modifier = CONTROL_MULTIPLIER;
			}
			if (me.isShiftDown()) {
				modifier = SHIFT_MULTIPLIER;
			}
			if (me.isPrimaryButtonDown()) {
				cameraXform.ry.setAngle(cameraXform.ry.getAngle() - mouseDeltaX*MOUSE_SPEED*modifier*ROTATION_SPEED);
				cameraXform.rx.setAngle(cameraXform.rx.getAngle() + mouseDeltaY*MOUSE_SPEED*modifier*ROTATION_SPEED);
			}
			else if (me.isSecondaryButtonDown()) {
				double z = camera.getTranslateZ();
				double newZ = z + mouseDeltaX*MOUSE_SPEED*modifier;
				camera.setTranslateZ(newZ);
			}
			else if (me.isMiddleButtonDown()) {
				cameraXform2.t.setX(cameraXform2.t.getX() + mouseDeltaX*MOUSE_SPEED*modifier*TRACK_SPEED);
				cameraXform2.t.setY(cameraXform2.t.getY() + mouseDeltaY*MOUSE_SPEED*modifier*TRACK_SPEED);
			}
		});
	}
}
