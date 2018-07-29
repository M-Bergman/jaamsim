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
package com.jaamsim.Commands;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;

public class KeywordCommand implements Command {

	private final Entity entity;
	private final String entityName;
	private final KeywordIndex[] oldKws;
	private final KeywordIndex[] newKws;
	private final int index;

	public KeywordCommand(Entity ent, KeywordIndex... kws) {
		this(ent, 0, kws);
	}

	public KeywordCommand(Entity ent, int ind, KeywordIndex... kws) {
		this(ent, ind, getPresentKws(ent, kws), kws);
	}

	public KeywordCommand(Entity ent, int ind, KeywordIndex[] kws0, KeywordIndex[] kws1) {
		entity = ent;
		entityName = ent.getName();
		oldKws = kws0;
		newKws = kws1;
		index = ind;
	}

	private static KeywordIndex[] getPresentKws(Entity ent, KeywordIndex[] kws) {
		KeywordIndex[] ret = new KeywordIndex[kws.length];
		for (int i = 0; i < kws.length; i++) {
			String key = kws[i].keyword;
			Input<?> in = ent.getInput(key);
			ret[i] = new KeywordIndex(key, in.getValueTokens(), null);
		}
		return ret;
	}

	@Override
	public void execute() {
		for (int i = 0; i < newKws.length; i++) {
			InputAgent.processKeyword(entity, newKws[i]);
		}
	}

	@Override
	public void undo() {
		for (int i = 0; i < oldKws.length; i++) {
			InputAgent.processKeyword(entity, oldKws[i]);
		}
	}

	@Override
	public Command tryMerge(Command cmd) {
		if (!(cmd instanceof KeywordCommand)) {
			return null;
		}
		KeywordCommand kwCmd = (KeywordCommand) cmd;
		if (entity != kwCmd.entity || index != kwCmd.index
				|| newKws.length != kwCmd.newKws.length) {
			return null;
		}
		for (int i = 0; i < newKws.length; i++) {
			if (!newKws[i].keyword.equals(kwCmd.newKws[i].keyword))
				return null;
		}

		return new KeywordCommand(entity, index, oldKws, kwCmd.newKws);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(entityName);
		if (newKws.length == 1)
			sb.append(" keyword: ");
		else
			sb.append(" keywords: ");

		for (int i = 0; i < newKws.length; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append(newKws[i].keyword);
		}
		return sb.toString();
	}

}
