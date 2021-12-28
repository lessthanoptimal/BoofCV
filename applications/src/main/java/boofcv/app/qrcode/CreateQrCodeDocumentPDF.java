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

package boofcv.app.qrcode;

import boofcv.alg.fiducial.qrcode.QrCode;
import boofcv.alg.fiducial.qrcode.QrCodeGenerator;
import boofcv.app.PaperSize;
import boofcv.app.fiducials.CreateFiducialDocumentPDF;
import boofcv.generate.Unit;
import boofcv.pdf.PdfFiducialEngine;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Generates the QR Code PDF Document
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"NullAway.Init"})
public class CreateQrCodeDocumentPDF extends CreateFiducialDocumentPDF {

	java.util.List<QrCode> markers;
	QrCodeGenerator g;

	public CreateQrCodeDocumentPDF( String documentName, PaperSize paper, Unit units ) {
		super(documentName, paper, units);
	}

	@Override
	protected String getMarkerType() {
		return "QRCode ";
	}

	@Override
	protected void configureRenderer( PdfFiducialEngine r ) {
		g = new QrCodeGenerator(markerWidth*UNIT_TO_POINTS);
		g.setRender(r);
	}

	@Override
	protected void render( int markerIndex ) {
		QrCode qr = markers.get(markerIndex%markers.size());
		g.render(qr);
	}

	public void render( java.util.List<QrCode> markers ) throws IOException {
		this.markers = markers;

		totalMarkers = markers.size();
		names = new ArrayList<>();
		for (int i = 0; i < markers.size(); i++) {
			// Truncate the message so it's not too long
			String message = markers.get(i).message;
			// get rid of new line
			message = message.replaceAll("\\r|\\n", "");
			message = message.substring(0, Math.min(message.length(), 30));
			// remove non-printable
			message = message.replaceAll("\\p{C}", "?");
			names.add(message);
		}

		render();
	}
}
