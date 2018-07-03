/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.Point3dRgbI;
import georegression.struct.point.Point3D_F64;
import javafx.embed.swing.JFXPanel;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;

import java.util.List;

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
	private static final double CAMERA_INITIAL_DISTANCE = -450;
	private static final double CAMERA_INITIAL_X_ANGLE = 70.0;
	private static final double CAMERA_INITIAL_Y_ANGLE = 320.0;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 10000.0;
	private static final double AXIS_LENGTH = 250.0;
	private static final double CONTROL_MULTIPLIER = 0.1;
	private static final double SHIFT_MULTIPLIER = 10.0;
	private static final double MOUSE_SPEED = 0.1;
	private static final double ROTATION_SPEED = 2.0;
	private static final double TRACK_SPEED = 0.3;

	double mousePosX;
	double mousePosY;
	double mouseOldX;
	double mouseOldY;
	double mouseDeltaX;
	double mouseDeltaY;



	public void setCloud(List<Point3dRgbI> cloud ) {
		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);

		buildCamera();
		buildAxes();
		buildCloud(cloud);

		Scene scene = new Scene(root, 1024, 768, true);
		scene.setFill(Color.GREY);
//		handleKeyboard(scene, world);
		handleMouse(scene, world);

		scene.setCamera(camera);
		setScene(scene);
	}

	private void buildCamera() {
		root.getChildren().add(cameraXform);
		cameraXform.getChildren().add(cameraXform2);
		cameraXform2.getChildren().add(cameraXform3);
		cameraXform3.getChildren().add(camera);
//		cameraXform3.setRotateZ(180.0);

		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateZ(CAMERA_INITIAL_DISTANCE);
//		cameraXform.ry.setAngle(CAMERA_INITIAL_Y_ANGLE);
//		cameraXform.rx.setAngle(CAMERA_INITIAL_X_ANGLE);
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

		final Box xAxis = new Box(AXIS_LENGTH, 1, 1);
		final Box yAxis = new Box(1, AXIS_LENGTH, 1);
		final Box zAxis = new Box(1, 1, AXIS_LENGTH);

		xAxis.setMaterial(redMaterial);
		yAxis.setMaterial(greenMaterial);
		zAxis.setMaterial(blueMaterial);

		axisGroup.getChildren().addAll(xAxis, yAxis, zAxis);
		axisGroup.setVisible(true);
		world.getChildren().addAll(axisGroup);
	}

	private void buildCloud( List<Point3dRgbI> cloud ) {

		Xform pointGroup = new Xform();

		Point3D_F64 mean = new Point3D_F64();
		Point3D_F64 stdev = new Point3D_F64();

		int N = cloud.size();
		for (int i = 0; i < N; i++) {
			Point3D_F64 p = cloud.get(i);
			mean.x += p.x / N;
			mean.y += p.y / N;
			mean.z += p.z / N;
		}

		for (int i = 0; i < N; i++) {
			Point3D_F64 p = cloud.get(i);
			double dx = p.x-mean.x;
			double dy = p.y-mean.y;
			double dz = p.z-mean.z;

			stdev.x += dx*dx/N;
			stdev.y += dy*dy/N;
			stdev.z += dz*dz/N;
		}

		float scale = (float)Math.max(Math.sqrt(stdev.x),Math.sqrt(stdev.y));
		scale = (float)(0.1*AXIS_LENGTH/Math.max(scale,Math.sqrt(stdev.z)));
		float radius =1f;

		System.out.println("scale = "+scale+"  radius = "+radius);

		for (int i = 0; i < cloud.size(); i++) {
			PhongMaterial material = new PhongMaterial();

			Point3dRgbI p = cloud.get(i);

			int rgb = p.rgb;

			Color color = Color.rgb((rgb>>16)&0xFF, (rgb>>8)&0xFF,rgb&0xFF);
			material.setDiffuseColor(color);

			Sphere pointBox = new Sphere(radius);
			pointBox.setCache(true);
			pointBox.setCacheHint(CacheHint.SPEED);

			pointBox.setMaterial(material);
			pointBox.setTranslateX((p.x-mean.x) * scale);
			pointBox.setTranslateY((p.y-mean.y) * scale);
			pointBox.setTranslateZ((p.z-mean.z) * scale);

			pointGroup.getChildren().add(pointBox);
		}
		world.getChildren().addAll(pointGroup);
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
