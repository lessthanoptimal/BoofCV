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

package boofcv.app;

import boofcv.app.batch.BatchControlPanel;
import boofcv.app.batch.BatchProcessControlPanel;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ShowImages;

import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * GUI for batch scanning of QR codes.
 *
 * @author Peter Abeles
 */
public class BatchScanQrCodesGui extends BatchProcessControlPanel
		implements BatchControlPanel.Listener {
	Preferences prefs = Preferences.userRoot().node(BatchScanQrCodesGui.class.getSimpleName());

	JLabel labelStatus = new JLabel();
	int count = 0;

	public BatchScanQrCodesGui() {
		labelStatus.setPreferredSize(new Dimension(300, 20));

		textOutputFile.setText("qrcode.txt");
		addStandardControls(prefs);
		addAlignCenter(labelStatus);

		setPreferredSize(new Dimension(350, 200));

		ShowImages.showWindow(this, "Batch Scan QR", true);
	}

	@Override
	protected void handleStart() {
		BoofSwingUtil.checkGuiThread();

		BatchScanQrCodes batch = new BatchScanQrCodes();
		batch.inputPattern = textInputSource.getText();
		batch.pathOutput = textOutputFile.getText();
		batch.listener = this;
		count = 0;

		bAction.setEnabled(false);

		prefs.put(KEY_INPUT, batch.inputPattern);
		prefs.put(KEY_OUTPUT, batch.pathOutput);

		new Thread(() -> {
			try {
				batch.process();
			} catch (Exception e) {
				BoofSwingUtil.warningDialog(this, e);
			} finally {
				SwingUtilities.invokeLater(() -> {
					labelStatus.setText("Total " + count);
					bAction.setEnabled(true);
				});
			}
		}).start();
	}

	@Override
	public void batchUpdate( String fileName ) {
		SwingUtilities.invokeLater(() -> {
			labelStatus.setText(count++ + "  " + fileName);
			labelStatus.repaint();
		});
	}

	public static void main( String[] args ) {
		SwingUtilities.invokeLater(BatchScanQrCodesGui::new);
	}
}
