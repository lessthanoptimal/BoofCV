/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

package boofcv.gui;

import javax.swing.*;
import java.io.*;

/**
 * Created by Jalal on 4/29/2017.
 */
public class OutputStreamCapturer {

	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private PrintStream textStream = new PrintStream(new TextOutputStream());

	private PrintStream std;
	private JTextArea textArea;

	public OutputStreamCapturer(JTextArea textArea) {
		std = System.out;
		this.textArea = textArea;
	}

	public void toDisplayOutput() {
		System.out.flush();
		System.setOut(textStream);
	}

	public void toStdOutput() {
		System.out.flush();
		System.setOut(std);
	}

	class TextOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			textArea.setText(textArea.getText() + String.valueOf((char) b));
		}
	}
}