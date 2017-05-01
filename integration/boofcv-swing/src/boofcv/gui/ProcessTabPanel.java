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

/**
 * A jpanel that keeps track of the process id it is supposed to be displaying information for.
 * Intended to contain the source code and output.
 * Created by Jalal on 4/30/2017.
 */
public class ProcessTabPanel extends JPanel {

	private long processId;

	public ProcessTabPanel() {
		super();
	}

	public ProcessTabPanel(long processId) {
		this.processId = processId;
	}

	public void setProcessId(long processId) {
		this.processId = processId;
	}

	public long getProcessId() {
		return this.processId;
	}
}
