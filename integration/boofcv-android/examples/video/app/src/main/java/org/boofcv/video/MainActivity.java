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

package org.boofcv.video;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Simple activity used to start other activities
 *
 * @author Peter Abeles
 */
public class MainActivity extends Activity {

	@Override
	public void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		requestCameraPermission();
	}

	public void clickedGradient( View view ) {
		Intent intent = new Intent(this, GradientActivity.class);
		startActivity(intent);
	}

	public void clickedExposure( View view ) {
		Intent intent = new Intent(this, ExposureActivity.class);
		startActivity(intent);
	}

	public void clickedQrCode( View view ) {
		Intent intent = new Intent(this, QrCodeActivity.class);
		startActivity(intent);
	}

	public void clickedBitmap( View view ) {
		Intent intent = new Intent(this, BitmapActivity.class);
		startActivity(intent);
	}

	/**
	 * Newer versions of Android require explicit permission from the user
	 */
	private void requestCameraPermission() {
		int permissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.CAMERA);

		if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					0);
			// a dialog should open and this dialog will resume when a decision has been made
		}
	}

	@Override
	public void onRequestPermissionsResult( int requestCode,
											String[] permissions, int[] grantResults ) {
		switch (requestCode) {
			case 0: {
				// If request is cancelled, the result arrays are empty.
				if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
					dialogNoCameraPermission();
				}
			}
		}
	}

	private void dialogNoCameraPermission() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Denied access to the camera! Exiting.")
				.setCancelable(false)
				.setPositiveButton("OK", ( dialog, id ) -> System.exit(0));
		AlertDialog alert = builder.create();
		alert.show();
	}
}
