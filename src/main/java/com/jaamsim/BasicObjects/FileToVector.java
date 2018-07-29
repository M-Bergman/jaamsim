/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2018 JaamSim Software Inc.
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
package com.jaamsim.BasicObjects;

import java.net.URI;
import java.util.ArrayList;

import com.jaamsim.input.ExpResult;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Output;

public class FileToVector extends FileToArray {

	ArrayList<ExpResult> value;

	public FileToVector() {
		value = new ArrayList<>();
	}

	@Override
	protected void setValueForURI(URI uri, double simTime) {
		value = getVectorForURI(uri, simTime);
	}

	@Override
	protected void clearValue() {
		value = new ArrayList<>();
	}

	private ArrayList<ExpResult> getVectorForURI(URI uri, double simTime) {
		ArrayList<ArrayList<String>> tokens = FileInput.getTokensFromURI(uri);
		int n = 0;
		for (ArrayList<String> record : tokens) {
			n += record.size();
		}
		ArrayList<ExpResult> ret = new ArrayList<>(n);
		for (ArrayList<String> record : tokens) {
			for (int i=0; i<record.size(); i++) {
				String str = record.get(i);
				ret.add(getExpResult(i, str, simTime));
			}
		}
		return ret;
	}

	@Output(name = "Value",
	 description = "A vector containing the data from the input file.",
	    sequence = 1)
	public ArrayList<ExpResult> getValue(double simTime) {
		return value;
	}

}
