/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.LinkDisplayable;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.states.StateEntity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.RateUnit;
import com.jaamsim.units.TimeUnit;

/**
 * LinkedComponents are used to form a chain of components that process DisplayEntities that pass through the system.
 * Sub-classes for EntityGenerator, Server, and EntitySink.
 */
public abstract class LinkedComponent extends StateEntity implements Linkable, LinkDisplayable {

	@Keyword(description = "The default value for the output obj.\n"
	                     + "Normally, obj is set to the last entity received by this object. "
	                     + "Prior to receiving its first entity, obj is set to the object "
	                     + "provided by DefaultEntity. If an input for DefaultEntity is not "
	                     + "provided, then obj is set to null until the first entity is received.",
	         exampleList = {"SimEntity1"})
	protected final EntityInput<DisplayEntity> defaultEntity;

	@Keyword(description = "The next object to which the processed DisplayEntity is passed.",
			exampleList = {"Queue1"})
	protected final InterfaceEntityInput<Linkable> nextComponent;

	@Keyword(description = "The state to be assigned to each entity on arrival at this object.\n" +
			"No state is assigned if the entry is blank.",
	         exampleList = {"Service"})
	protected final StringInput stateAssignment;

	private long numberAdded;     // Number of entities added to this component from upstream after initialisation
	private long numberProcessed; // Number of entities processed by this component after initialisation
	private long initialNumberAdded;     // Number of entities added to this component from upstream during initialisation
	private long initialNumberProcessed; // Number of entities processed by this component during initialisation
	private DisplayEntity receivedEntity; // Entity most recently received by this component
	private double releaseTime = Double.NaN;

	{
		attributeDefinitionList.setHidden(false);
		workingStateListInput.setHidden(true);

		defaultEntity = new EntityInput<>(DisplayEntity.class, "DefaultEntity", "Key Inputs", null);
		this.addInput(defaultEntity);
		this.addSynonym(defaultEntity, "TestEntity");

		nextComponent = new InterfaceEntityInput<>(Linkable.class, "NextComponent", "Key Inputs", null);
		nextComponent.setRequired(true);
		this.addInput(nextComponent);

		stateAssignment = new StringInput("StateAssignment", "Key Inputs", "");
		this.addInput(stateAssignment);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == defaultEntity) {
			receivedEntity = defaultEntity.getValue();
			return;
		}
	}

	@Override
	public void validate() {
		super.validate();

		// If a state is to be assigned, ensure that the prototype is a StateEntity
		if (defaultEntity.getValue() != null && !stateAssignment.getValue().isEmpty()) {
			if (!(defaultEntity.getValue() instanceof StateEntity)) {
				throw new InputErrorException("Only a SimEntity can be specified for the TestEntity keyword if a state is be be assigned.");
			}
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		numberAdded = 0;
		numberProcessed = 0;
		initialNumberAdded = 0;
		initialNumberProcessed = 0;
		receivedEntity = defaultEntity.getValue();
		releaseTime = Double.NaN;
	}

	@Override
	public String getInitialState() {
		return "None";
	}

	@Override
	public boolean isValidState(String state) {
		return true;
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		this.registerEntity(ent);
	}

	protected void registerEntity(DisplayEntity ent) {

		receivedEntity = ent;
		numberAdded++;

		// Assign a new state to the received entity
		if (!stateAssignment.getValue().isEmpty() && ent instanceof StateEntity)
			((StateEntity)ent).setPresentState(stateAssignment.getValue());
	}

	protected void setReceivedEntity(DisplayEntity ent) {
		receivedEntity = ent;
	}

	/**
	 * Sends the specified entity to the next component downstream.
	 * @param ent - the entity to be sent downstream.
	 */
	public void sendToNextComponent(DisplayEntity ent) {
		numberProcessed++;
		releaseTime = this.getSimTime();
		if( nextComponent.getValue() != null )
			nextComponent.getValue().addEntity(ent);
	}

	/**
	 * Returns the number of entities that have been received from upstream during the entire
	 * simulation run, including the initialisation period.
	 */
	public long getTotalNumberAdded() {
		return initialNumberAdded + numberAdded;
	}

	/**
	 * Returns the number of entities that have been passed downstream during the entire
	 * simulation run, including the initialisation period.
	 */
	public long getTotalNumberProcessed() {
		return initialNumberProcessed + numberProcessed;
	}

	/**
	 * Returns the number of entities that have been received from upstream after the
	 * initialisation period.
	 * @return
	 */
	public long getNumberAdded() {
		return numberAdded;
	}

	public void incrementNumberProcessed() {
		numberProcessed++;
	}

	/**
	 * Returns the number of entities that have been received but whose processing has not been
	 * completed yet.
	 */
	public long getNumberInProgress() {
		return  initialNumberAdded + numberAdded - initialNumberProcessed - numberProcessed;
	}

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		initialNumberAdded = numberAdded;
		initialNumberProcessed = numberProcessed;
		numberAdded = 0;
		numberProcessed = 0;
	}

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (nextComponent.getHidden() || !(nextEnt instanceof Linkable)
				|| nextEnt instanceof EntityGenerator) {
			return;
		}

		ArrayList<String> toks = new ArrayList<>();
		toks.add(nextEnt.getName());
		KeywordIndex kw = new KeywordIndex(nextComponent.getKeyword(), toks, null);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

	// LinkDisplayable
	@Override
	public ArrayList<Entity> getDestinationEntities() {
		ArrayList<Entity> ret = new ArrayList<>();
		Linkable l = nextComponent.getValue();
		if (l != null && (l instanceof Entity)) {
			ret.add((Entity)l);
		}
		return ret;
	}

	@Override
	public ArrayList<Entity> getSourceEntities() {
		return new ArrayList<>();
	}

	@Override
	public Vec3d getSourcePoint() {
		return getGlobalPosition();
	}
	@Override
	public Vec3d getSinkPoint() {
		return getGlobalPosition();
	}

	@Override
	public double getRadius() {
		return getSize().mag2()/2.0;
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "obj",
	 description = "The entity that was received most recently.",
	    sequence = 0)
	public DisplayEntity getReceivedEntity(double simTime) {
		return receivedEntity;
	}

	@Output(name = "NumberAdded",
	 description = "The number of entities received from upstream after the initialization period.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 1)
	public long getNumberAdded(double simTime) {
		return numberAdded;
	}

	@Output(name = "NumberProcessed",
	 description = "The number of entities processed by this component after the initialization period.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 2)
	public long getNumberProcessed(double simTime) {
		return numberProcessed;
	}

	@Output(name = "NumberInProgress",
	 description = "The number of entities that have been received but whose processing has not been completed yet.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public long getNumberInProgress(double simTime) {
		return  this.getNumberInProgress();
	}

	@Output(name = "ProcessingRate",
	 description = "The number of entities processed per unit time by this component after the initialization period.",
	    unitType = RateUnit.class,
	    sequence = 4)
	public double getProcessingRate(double simTime) {
		double dur = simTime - Simulation.getInitializationTime();
		if (dur <= 0.0)
			return 0.0;
		return numberProcessed/dur;
	}

	@Output(name = "ReleaseTime",
	 description = "The time at which the last entity was released.",
	    unitType = TimeUnit.class,
	    sequence = 5)
	public double getReleaseTime(double simTime) {
		return releaseTime;
	}

}
