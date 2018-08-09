/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2017 JaamSim Software Inc.
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
package com.jaamsim.ProcessFlow;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;

public class Unpack extends LinkedService {

	@Keyword(description = "An expression returning a string value that determines which of the "
	                     + "entities in the container are eligible to be removed. "
	                     + "If used, the only entities eligible for selection are the ones whose "
	                     + "inputs for the container's Match keyword are equal to value returned by "
	                     + "the expression entered for this Match keyword. "
	                     + "Expressions that return a dimensionless integer or an object are also "
	                     + "valid. The returned number or object is converted to a string "
	                     + "automatically. A floating point number is truncated to an integer.",
	         exampleList = {"this.obj.Attrib1"})
	private final StringProvInput matchForEntities;

	@Keyword(description = "The service time required to unpack each entity.",
	         exampleList = { "3.0 h", "NormalDistribution1", "'1[s] + 0.5*[TimeSeries1].PresentValue'" })
	private final SampleInput serviceTime;

	private String entityMatch;   // Match value for the entities to be removed from the container
	private int numberToRemove;   // Number of entities to remove from the present EntityContainer

	{
		matchForEntities = new StringProvInput("MatchForEntities", KEY_INPUTS, null);
		matchForEntities.setUnitType(DimensionlessUnit.class);
		matchForEntities.setEntity(this);
		this.addInput(matchForEntities);

		serviceTime = new SampleInput("ServiceTime", KEY_INPUTS, new SampleConstant(TimeUnit.class, 0.0));
		serviceTime.setUnitType(TimeUnit.class);
		serviceTime.setEntity(this);
		serviceTime.setValidRange(0, Double.POSITIVE_INFINITY);
		this.addInput(serviceTime);
	}

	private EntityContainer container;	// the received EntityContainer
	private int numberRemoved;   // Number of entities removed from the received EntityContainer

	public Unpack() {}

	@Override
	public void earlyInit() {
		super.earlyInit();
		container = null;
		numberRemoved = 0;
	}

	@Override
	protected boolean startProcessing(double simTime) {

		// Determine the match value
		String m = this.getNextMatchValue(getSimTime());
		this.setMatchValue(m);

		// Is there a container waiting to be unpacked?
		if (container == null && waitQueue.getValue().getMatchCount(m) == 0) {
			return false;
		}

		if (container == null) {

			// Remove the container from the queue
			container = (EntityContainer)this.getNextEntityForMatch(m);
			numberToRemove = this.getNumberToRemove();
			entityMatch = null;
			if (matchForEntities.getValue() != null)
				entityMatch = matchForEntities.getValue().getNextString(simTime, "%s", 1.0d, true);
			numberRemoved = 0;
		}

		return true;
	}

	protected void disposeContainer(EntityContainer c) {
		c.kill();
	}

	protected int getNumberToRemove() {
		return Integer.MAX_VALUE;
	}

	@Override
	protected void processStep(double simTime) {

		// Remove the next entity from the container
		if (numberRemoved < numberToRemove && !container.isEmpty(entityMatch)) {
			this.sendToNextComponent(container.removeEntity(entityMatch));
			numberRemoved++;
		}

		// Stop when the desired number of entities have been removed
		else {
			this.disposeContainer(container);
			container = null;
		}
	}

	@Override
	public void updateGraphics(double simTime) {
		if (container == null)
			return;
		moveToProcessPosition(container);
	}

	@Override
	protected double getStepDuration(double simTime) {
		double dur = 0.0;
		if (numberRemoved < numberToRemove && !container.isEmpty(entityMatch))
			dur = serviceTime.getValue().getNextSample(simTime);
		return dur;
	}

}
