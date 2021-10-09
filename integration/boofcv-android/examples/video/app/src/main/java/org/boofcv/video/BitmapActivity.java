package org.boofcv.video;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.InputStream;

import boofcv.abst.fiducial.QrCodeDetector;
import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.android.ConvertBitmap;
import boofcv.concurrency.BoofConcurrency;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.struct.image.GrayU8;

/**
 * Activity which shows you how to select and image, load it as a Bitmap image, convert it into
 * a BoofCV format, then process it.
 */
public class BitmapActivity extends AppCompatActivity {
	public static final int PICK_IMAGE = 1;

	private ImageView imageView;
	private TextView textView;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_bitmap);

		imageView = findViewById(R.id.selected_image);
		textView = findViewById(R.id.bitmap_decoded);

		// Java 1.8 issues with older SDK versions
		BoofConcurrency.USE_CONCURRENT = android.os.Build.VERSION.SDK_INT >= 24;
	}

	/**
	 * Respond to the user clicking the select image button by opening up an image select intent
	 */
	public void clickSelectImage( View view ) {
		var intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(intent, PICK_IMAGE);
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult(requestCode, resultCode, data);
		// Abort if anything goes wrong or the user canceled
		if (requestCode != PICK_IMAGE || resultCode != Activity.RESULT_OK) {
			return;
		}
		if (data == null) {
			return;
		}
		Uri uri = data.getData();
		if (uri == null) {
			return;
		}
		try {
			// Load the image
			InputStream inputStream = getContentResolver().openInputStream(uri);
			Bitmap original = BitmapFactory.decodeStream(inputStream);

			// Display the image
			imageView.setImageBitmap(original);

			// Scale the image. We are doing this to keep memory consumption down (could be an
			// old device) and for fast processing
			double factor = Math.max(original.getWidth()/1024.0, original.getHeight()/1024.0);
			int newWidth = (int)(original.getWidth()/factor);
			int newHeight = (int)(original.getHeight()/factor);
			Bitmap scaled = Bitmap.createScaledBitmap(original, newWidth, newHeight, true);

			// Convert it into a BoofCV image that can be processed
			var gray = new GrayU8(1, 1);
			ConvertBitmap.bitmapToBoof(scaled, gray, null);

			// Send it through the QR code detector
			QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null, GrayU8.class);
			detector.process(gray);

			String text = "Total Detected = " + detector.getDetections().size() + "\n";
			for (QrCode qr : detector.getDetections()) {
				text += qr.message + "\n";
			}

			textView.setText(text);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}
