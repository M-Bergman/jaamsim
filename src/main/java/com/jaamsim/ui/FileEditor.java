/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.ui;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.input.FileInput;
import com.jaamsim.input.InputAgent;

/**
 * Handles file inputs.
 *
 */
public class FileEditor extends ChooserEditor {

	private static File lastDir;  // last directory accessed by the file chooser

	public FileEditor(JTable table) {
		super(table, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if("button".equals(e.getActionCommand())) {

			// Create a file chooser
			if (lastDir == null)
				lastDir = InputAgent.getConfigFile();
			JFileChooser fileChooser = new JFileChooser(lastDir);

			// Set the file extension filters
			FileNameExtensionFilter[] filters = ((FileInput)input).getFileNameExtensionFilters();
			if (filters.length > 0) {
				// Turn off the "All Files" filter
				fileChooser.setAcceptAllFileFilterUsed(false);
				// Include a separate filter for each extension
				for (FileNameExtensionFilter f : filters) {
					fileChooser.addChoosableFileFilter(f);
				}
			}

			// Show the file chooser and wait for selection
			int returnVal = fileChooser.showDialog(null, "Load");

			// Process the selected file
			if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = fileChooser.getSelectedFile();
				lastDir = fileChooser.getCurrentDirectory();
				setValue(file.getPath());
	        }

			// Apply editing
			stopCellEditing();

			// Focus the cell
			propTable.requestFocusInWindow();
		}
	}

}
