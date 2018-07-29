/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ProcessFlow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.TreeSet;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.Statistics.TimeBasedFrequency;
import com.jaamsim.Statistics.TimeBasedStatistics;
import com.jaamsim.StringProviders.StringProvInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.EntityTarget;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventHandle;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.ProcessTarget;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.InterfaceEntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;

public class Queue extends LinkedComponent {

	@Keyword(description = "The priority for positioning the received entity in the queue. "
	                     + "Priority is integer valued and a lower numerical value indicates a "
	                     + "higher priority. "
	                     + "For example, priority 3 is higher than 4, and priorities 3, 3.2, and "
	                     + "3.8 are equivalent.",
	         exampleList = {"this.obj.Attrib1"})
	private final SampleInput priority;

	@Keyword(description = "An expression returning a string value that categorizes the queued "
	                     + "entities. The expression is evaluated and the value saved when the "
	                     + "entity first arrives at the queue. "
	                     + "Expressions that return a dimensionless integer or an object are also "
	                     + "valid. The returned number or object is converted to a string "
	                     + "automatically. A floating point number is truncated to an integer.",
	         exampleList = {"this.obj.Attrib1"})
	private final StringProvInput match;

	@Keyword(description = "Determines the order in which entities are placed in the queue (FIFO "
	                     + "or LIFO):\n"
	                     + "TRUE = first in first out (FIFO) order (the default setting),\n"
	                     + "FALSE = last in first out (LIFO) order.",
	         exampleList = {"FALSE"})
	private final BooleanInput fifo;

	@Keyword(description = "The time an entity will wait in the queue before deciding whether or "
	                     + "not to renege. Evaluated when the entity first enters the queue.",
	         exampleList = {"3.0 h", "NormalDistribution1",
	                        "'1[s] + 0.5*[TimeSeries1].PresentValue'"})
	private final SampleInput renegeTime;

	@Keyword(description = "A logical condition that determines whether an entity will renege "
	                     + "after waiting for its RenegeTime value. Note that TRUE and FALSE are "
	                     + "entered as 1 and 0, respectively.",
	         exampleList = {"1", "'this.QueuePosition > 1'",
	                        "'this.QueuePostion > [Queue2].QueueLength'"})
	private final SampleInput renegeCondition;

	@Keyword(description = "The object to which an entity will be sent if it reneges.",
	         exampleList = {"Branch1"})
	protected final InterfaceEntityInput<Linkable> renegeDestination;

	@Keyword(description = "The amount of graphical space shown between DisplayEntity objects in "
	                     + "the queue.",
	         exampleList = {"1 m"})
	private final ValueInput spacing;

	@Keyword(description = "The number of queuing entities in each row.",
			exampleList = {"4"})
	protected final IntegerInput maxPerLine; // maximum items per sub line-up of queue

	private final TreeSet<QueueEntry> itemSet;  // contains all the entities in queue order
	private final HashMap<String, TreeSet<QueueEntry>> matchMap; // each TreeSet contains the queued entities for a given match value

	private String matchForMaxCount;  // match value with the largest number of entities
	private int maxCount;     // largest number of entities for a given match value

	private final ArrayList<QueueUser> userList;  // other objects that use this queue

	//	Statistics
	private final TimeBasedStatistics stats;
	private final TimeBasedFrequency freq;
	protected long numberReneged;  // number of entities that reneged from the queue

	{
		defaultEntity.setHidden(true);
		nextComponent.setHidden(true);

		priority = new SampleInput("Priority", KEY_INPUTS, new SampleConstant(0));
		priority.setUnitType(DimensionlessUnit.class);
		priority.setEntity(this);
		priority.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(priority);

		match = new StringProvInput("Match", KEY_INPUTS, null);
		match.setUnitType(DimensionlessUnit.class);
		match.setEntity(this);
		this.addInput(match);

		fifo = new BooleanInput("FIFO", KEY_INPUTS, true);
		this.addInput(fifo);

		renegeTime = new SampleInput("RenegeTime", KEY_INPUTS, null);
		renegeTime.setUnitType(TimeUnit.class);
		renegeTime.setEntity(this);
		renegeTime.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(renegeTime);

		renegeCondition = new SampleInput("RenegeCondition", KEY_INPUTS, new SampleConstant(1));
		renegeCondition.setUnitType(DimensionlessUnit.class);
		renegeCondition.setEntity(this);
		renegeCondition.setValidRange(0.0d, 1.0d);
		this.addInput(renegeCondition);

		renegeDestination = new InterfaceEntityInput<>(Linkable.class, "RenegeDestination", KEY_INPUTS, null);
		this.addInput(renegeDestination);

		spacing = new ValueInput("Spacing", KEY_INPUTS, 0.0d);
		spacing.setUnitType(DistanceUnit.class);
		spacing.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		this.addInput(spacing);

		maxPerLine = new IntegerInput("MaxPerLine", KEY_INPUTS, Integer.MAX_VALUE);
		maxPerLine.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(maxPerLine);
	}

	public Queue() {
		itemSet = new TreeSet<>();
		userList = new ArrayList<>();
		matchMap = new HashMap<>();
		stats = new TimeBasedStatistics();
		freq = new TimeBasedFrequency(0, 10);
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == renegeTime) {
			boolean bool = renegeTime.getValue() != null;
			renegeDestination.setRequired(bool);
			return;
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Clear the entries in the queue
		itemSet.clear();
		matchMap.clear();

		matchForMaxCount = null;
		maxCount = -1;

		// Clear statistics
		stats.clear();
		stats.addValue(0.0d, 0.0d);
		freq.clear();
		freq.addValue(0.0d, 0);
		numberReneged = 0;

		// Identify the objects that use this queue
		userList.clear();
		for (Entity each : Entity.getClonesOfIterator(Entity.class)) {
			if (each instanceof QueueUser) {
				QueueUser u = (QueueUser)each;
				if (u.getQueues().contains(this))
					userList.add(u);
			}
		}
	}

	private static class QueueEntry implements Comparable<QueueEntry> {
		final DisplayEntity entity;
		final long entNum;
		final int priority;
		final String match;
		final double timeAdded;
		final Vec3d orientation;
		final EventHandle renegeHandle;

		public QueueEntry(DisplayEntity ent, long n, int pri, String m, double t, Vec3d orient, EventHandle rh) {
			entity = ent;
			entNum = n;
			priority = pri;
			match = m;
			timeAdded = t;
			orientation = orient;
			renegeHandle = rh;
		}

		@Override
		public int compareTo(QueueEntry entry) {
			if (this.priority > entry.priority)
				return 1;
			else if (this.priority < entry.priority)
				return -1;
			else {
				if (this.entNum > entry.entNum)
					return 1;
				else if (this.entNum < entry.entNum)
					return -1;
				return 0;
			}
		}
	}

	private final DoQueueChanged userUpdate = new DoQueueChanged(this);
	private final EventHandle userUpdateHandle = new EventHandle();
	private static class DoQueueChanged extends ProcessTarget {
		private final Queue queue;

		public DoQueueChanged(Queue q) {
			queue = q;
		}

		@Override
		public void process() {
			for (QueueUser each : queue.userList)
				each.queueChanged();
		}

		@Override
		public String getDescription() {
			return queue.getName() + ".UpdateAllQueueUsers";
		}
	}

	// ******************************************************************************************************
	// QUEUE HANDLING METHODS
	// ******************************************************************************************************

	@Override
	public void addEntity(DisplayEntity ent) {
		super.addEntity(ent);
		double simTime = getSimTime();

		// Update the queue statistics
		stats.addValue(simTime, itemSet.size() + 1);
		freq.addValue(simTime, itemSet.size() + 1);

		// Build the entry for the entity
		long n = this.getTotalNumberAdded();
		if (!fifo.getValue())
			n *= -1;
		int pri = (int) priority.getValue().getNextSample(simTime);
		String m = null;
		if (match.getValue() != null)
			m = match.getValue().getNextString(simTime, "%s", 1.0d, true);

		EventHandle rh = null;
		if (renegeTime.getValue() != null)
			rh = new EventHandle();

		QueueEntry entry = new QueueEntry(ent, n, pri, m, simTime, ent.getOrientation(), rh);

		// Add the entity to the TreeSet of all the entities in the queue
		boolean bool = itemSet.add(entry);
		if (!bool)
			error("Entity %s is already present in the queue.", ent);

		// Does the entry have a match value?
		if (entry.match != null) {

			// Add the entity to the TreeSet of all the entities with this match value
			TreeSet<QueueEntry> matchSet = matchMap.get(entry.match);
			if (matchSet == null) {
				matchSet = new TreeSet<>();
				matchSet.add(entry);
				matchMap.put(entry.match, matchSet);
			}
			else {
				matchSet.add(entry);
			}

			// Update the maximum count
			if (entry.match.equals(matchForMaxCount)) {
				maxCount++;
			}
			else {
				if (matchSet.size() > maxCount) {
					matchForMaxCount = entry.match;
					maxCount = matchSet.size();
				}
			}
		}

		// Notify the users of this queue
		if (!userUpdateHandle.isScheduled())
			EventManager.scheduleTicks(0, 2, false, userUpdate, userUpdateHandle);

		// Schedule the time to check the renege condition
		if (renegeTime.getValue() != null) {
			double dur = renegeTime.getValue().getNextSample(getSimTime());
			// Schedule the renege tests in FIFO order so that if two or more entities are added to
			// the queue at the same time, the one nearest the front of the queue is tested first
			EventManager.scheduleSeconds(dur, 5, true, new RenegeActionTarget(this, entry), rh);
		}
	}

	private static class RenegeActionTarget extends EntityTarget<Queue> {
		private final QueueEntry entry;

		RenegeActionTarget(Queue q, QueueEntry e) {
			super(q, "renegeAction");
			entry = e;
		}

		@Override
		public void process() {
			ent.renegeAction(entry);
		}
	}

	public void renegeAction(QueueEntry entry) {

		// Temporarily set the obj entity to the one that might renege
		double simTime = this.getSimTime();
		DisplayEntity oldEnt = this.getReceivedEntity(simTime);
		this.setReceivedEntity(entry.entity);

		// Check the condition for reneging
		boolean bool = (renegeCondition.getValue().getNextSample(simTime) == 0.0d);
		this.setReceivedEntity(oldEnt);
		if (bool) {
			return;
		}

		// Remove the entity from the queue and send it to the renege destination
		this.remove(entry);
		numberReneged++;
		renegeDestination.getValue().addEntity(entry.entity);
	}

	public DisplayEntity removeEntity(DisplayEntity ent) {
		return this.remove(getQueueEntry(ent));
	}

	/**
	 * Removes a specified entity from the queue
	 */
	private DisplayEntity remove(QueueEntry entry) {
		double simTime = getSimTime();

		// Update the queue statistics
		stats.addValue(simTime, itemSet.size() - 1);
		freq.addValue(simTime, itemSet.size() - 1);

		// Remove the entity from the TreeSet of all entities in the queue
		boolean found = itemSet.remove(entry);
		if (!found)
			error("Cannot find the entry in itemSet.");

		// Kill the renege event
		if (entry.renegeHandle != null)
			EventManager.killEvent(entry.renegeHandle);

		// Does the entry have a match value?
		if (entry.match != null) {

			// Remove the entity from the TreeSet for that match value
			TreeSet<QueueEntry> matchSet = matchMap.get(entry.match);
			if (matchSet == null)
				error("Cannot find an entry in matchMap for match value: %s", entry.match);
			found = matchSet.remove(entry);
			if (!found)
				error("Cannot find the entry in matchMap.");

			// If there are no more entities for this match value, remove it from the HashMap of match values
			if (matchSet.isEmpty())
				matchMap.remove(entry.match);

			// Update the maximum count
			if (entry.match.equals(matchForMaxCount)) {
				matchForMaxCount = null;
				maxCount = -1;
			}
		}

		// Reset the entity's orientation to its original value
		entry.entity.setOrientation(entry.orientation);

		this.incrementNumberProcessed();
		return entry.entity;
	}

	private QueueEntry getQueueEntry(DisplayEntity ent) {
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			QueueEntry entry = itr.next();
			if (entry.entity == ent)
				return entry;
		}
		return null;
	}

	/**
	 * Returns the position of the specified entity in the queue.
	 * Returns -1 if the entity is not found.
	 * @param ent - entity in question
	 * @return index of the entity in the queue.
	 */
	public int getPosition(DisplayEntity ent) {
		int ret = 0;
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			if (itr.next().entity == ent)
				return ret;

			ret++;
		}
		return -1;
	}

	/**
	 * Removes the first entity from the queue
	 */
	public DisplayEntity removeFirst() {
		if (itemSet.isEmpty())
			error("Cannot remove an entity from an empty queue");
		return this.remove(itemSet.first());
	}

	/**
	 * Returns the first entity in the queue.
	 * @return first entity in the queue.
	 */
	public DisplayEntity getFirst() {
		if (itemSet.isEmpty())
			return null;
		return itemSet.first().entity;
	}

	/**
	 * Returns the number of entities in the queue
	 */
	public int getCount() {
		return itemSet.size();
	}

	/**
	 * Returns true if the queue is empty
	 */
	public boolean isEmpty() {
		return itemSet.isEmpty();
	}

	/**
	 * Returns the number of seconds spent by the first object in the queue
	 */
	public double getQueueTime() {
		return this.getSimTime() - itemSet.first().timeAdded;
	}

	/**
	 * Returns the priority value for the first object in the queue
	 */
	public int getFirstPriority() {
		return itemSet.first().priority;
	}

	/**
	 * Returns the number of times that the specified match value appears in
	 * the queue. If the match value is null, then every entity is counted.
	 * @param m - value to be matched.
	 * @return number of entities that have this match value.
	 */
	public int getMatchCount(String m) {
		if (m == null)
			return itemSet.size();
		TreeSet<QueueEntry> matchSet = matchMap.get(m);
		if (matchSet == null)
			return 0;
		return matchSet.size();
	}

	public DisplayEntity getFirstForMatch(String m) {
		if (m == null) {
			return this.getFirst();
		}
		TreeSet<QueueEntry> matchSet = matchMap.get(m);
		if (matchSet == null)
			return null;
		return matchSet.first().entity;
	}

	/**
	 * Returns the first entity in the queue whose match value is equal to the
	 * specified value. The returned entity is removed from the queue.
	 * If the match value is null, the first entity is removed.
	 * @param m - value to be matched.
	 * @return entity whose match value equals the specified value.
	 */
	public DisplayEntity removeFirstForMatch(String m) {

		if (m == null)
			return this.removeFirst();

		TreeSet<QueueEntry> matchSet = matchMap.get(m);
		if (matchSet == null)
			return null;
		return this.remove(matchSet.first());
	}

	/**
	 * Returns the match value that has the largest number of entities in the queue.
	 * @return match value with the most entities.
	 */
	public String getMatchForMax() {
		if (matchForMaxCount == null)
			this.setMaxCount();
		return matchForMaxCount;
	}

	/**
	 * Returns the number of entities in the longest match value queue.
	 * @return number of entities in the longest match value queue.
	 */
	public int getMaxCount() {
		if (matchForMaxCount == null)
			this.setMaxCount();
		return maxCount;
	}

	/**
	 * Determines the longest queue for a give match value.
	 */
	private void setMaxCount() {
		maxCount = -1;
		for (Entry<String, TreeSet<QueueEntry>> each : matchMap.entrySet()) {
			int count = each.getValue().size();
			if (count > maxCount) {
				maxCount = count;
				matchForMaxCount = each.getKey();
			}
		}
	}

	/**
	 * Returns a match value that has sufficient numbers of entities in each
	 * queue. The first match value that satisfies the criterion is selected.
	 * If the numberList is too short, then the last value is used.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @return match value.
	 */
	public static String selectMatchValue(ArrayList<Queue> queueList, IntegerVector numberList) {

		// Check whether each queue has sufficient entities for any match value
		int number;
		for (int i=0; i<queueList.size(); i++) {
			if (numberList == null) {
				number = 1;
			}
			else {
				int ind = Math.min(i, numberList.size()-1);
				number = numberList.get(ind);
			}
			if (queueList.get(i).getMaxCount() < number)
				return null;
		}

		// Find the queue with the fewest match values
		Queue shortest = null;
		int count = -1;
		for (Queue que : queueList) {
			if (que.matchMap.size() > count) {
				count = que.matchMap.size();
				shortest = que;
			}
		}

		// Return the first match value that has sufficient entities in each queue
		for (String m : shortest.matchMap.keySet()) {
			if (Queue.sufficientEntities(queueList, numberList, m))
				return m;
		}
		return null;
	}

	/**
	 * Returns true if each of the queues contains sufficient entities with
	 * the specified match value for processing to begin.
	 * If the numberList is too short, then the last value is used.
	 * If the numberList is null, then one entity per queue is required.
	 * If the match value m is null, then all the entities in each queue are counted.
	 * @param queueList - list of queues to check.
	 * @param numberList - number of matches required for each queue.
	 * @param m - match value.
	 * @return true if there are sufficient entities in each queue.
	 */
	public static boolean sufficientEntities(ArrayList<Queue> queueList, IntegerVector numberList, String m) {
		int number;
		for (int i=0; i<queueList.size(); i++) {
			if (numberList == null) {
				number = 1;
			}
			else {
				int ind = Math.min(i, numberList.size()-1);
				number = numberList.get(ind);
			}
			if (queueList.get(i).getMatchCount(m) < number)
				return false;
		}
		return true;
	}

	/**
	 * Update the position of all entities in the queue. ASSUME that entities
	 * will line up according to the orientation of the queue.
	 */
	@Override
	public void updateGraphics(double simTime) {

		Vec3d queueOrientation = getOrientation();
		Vec3d qSize = this.getSize();
		Vec3d tmp = new Vec3d();

		double distanceX = 0.5d * qSize.x;
		double distanceY = 0;
		double maxWidth = 0;

		// Copy the item set to avoid some concurrent modification exceptions
		TreeSet<QueueEntry> itemSetCopy = new TreeSet<>(itemSet);

		// find widest vessel
		if (itemSetCopy.size() >  maxPerLine.getValue()){
			Iterator<QueueEntry> itr = itemSetCopy.iterator();
			while (itr.hasNext()) {
				 maxWidth = Math.max(maxWidth, itr.next().entity.getSize().y);
			 }
		}

		// update item locations
		int i = 0;
		Iterator<QueueEntry> itr = itemSetCopy.iterator();
		while (itr.hasNext()) {
			DisplayEntity item = itr.next().entity;

			// if new row is required, set reset distanceX and move distanceY up one row
			if( i > 0 && i % maxPerLine.getValue() == 0 ){
				 distanceX = 0.5d * qSize.x;
				 distanceY += spacing.getValue() + maxWidth;
			}
			i++;

			// Rotate each transporter about its center so it points to the right direction
			item.setOrientation(queueOrientation);
			Vec3d itemSize = item.getSize();
			distanceX += spacing.getValue() + 0.5d * itemSize.x;
			tmp.set3(-distanceX / qSize.x, distanceY/qSize.y, 0.0d);

			// increment total distance
			distanceX += 0.5d * itemSize.x;

			// Set Position
			Vec3d itemCenter = this.getGlobalPositionForAlignment(tmp);
			item.setGlobalPositionForAlignment(item.getAlignment(), itemCenter);
		}
	}

	// *******************************************************************************************************
	// STATISTICS
	// *******************************************************************************************************

	@Override
	public void clearStatistics() {
		super.clearStatistics();
		double simTime = this.getSimTime();
		stats.clear();
		stats.addValue(simTime, itemSet.size());
		freq.clear();
		freq.addValue(simTime, itemSet.size());
		numberReneged = 0;
	}

	@Override
	public void linkTo(DisplayEntity nextEnt) {
		if (!(nextEnt instanceof LinkedService))
			return;

		LinkedService serv = (LinkedService)nextEnt;
		serv.addQueue(this);
	}

	// LinkDisplayable
	@Override
	public ArrayList<Entity> getDestinationEntities() {
		ArrayList<Entity> ret = super.getDestinationEntities();
		Linkable l = renegeDestination.getValue();
		if (l != null && (l instanceof Entity)) {
			ret.add((Entity)l);
		}
		return ret;
	}

	// ******************************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************************

	@Output(name = "QueueLength",
	 description = "The present number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 0)
	public int getQueueLength(double simTime) {
		return itemSet.size();
	}

	@Output(name = "QueueList",
	 description = "The entities in the queue.",
	    sequence = 1)
	public ArrayList<DisplayEntity> getQueueList(double simTime) {
		ArrayList<DisplayEntity> ret = new ArrayList<>(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().entity);
		}
		return ret;
	}

	@Output(name = "QueueTimes",
	 description = "The waiting time for each entity in the queue.",
	    unitType = TimeUnit.class,
	    sequence = 2)
	public ArrayList<Double> getQueueTimes(double simTime) {
		ArrayList<Double> ret = new ArrayList<>(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(simTime - itr.next().timeAdded);
		}
		return ret;
	}

	@Output(name = "PriorityValues",
	 description = "The Priority expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 3)
	public IntegerVector getPriorityValues(double simTime) {
		IntegerVector ret = new IntegerVector(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			ret.add(itr.next().priority);
		}
		return ret;
	}

	@Output(name = "MatchValues",
	 description = "The Match expression value for each entity in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 4)
	public ArrayList<String> getMatchValues(double simTime) {
		ArrayList<String> ret = new ArrayList<>(itemSet.size());
		Iterator<QueueEntry> itr = itemSet.iterator();
		while (itr.hasNext()) {
			String m = itr.next().match;
			if (m != null) {
				ret.add(m);
			}
		}
		return ret;
	}

	@Output(name = "QueueLengthAverage",
	 description = "The average number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 5)
	public double getQueueLengthAverage(double simTime) {
		return stats.getMean(simTime);
	}

	@Output(name = "QueueLengthStandardDeviation",
	 description = "The standard deviation of the number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 6)
	public double getQueueLengthStandardDeviation(double simTime) {
		return stats.getStandardDeviation(simTime);
	}

	@Output(name = "QueueLengthMinimum",
	 description = "The minimum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 7)
	public int getQueueLengthMinimum(double simTime) {
		return (int) stats.getMin();
	}

	@Output(name = "QueueLengthMaximum",
	 description = "The maximum number of entities in the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 8)
	public int getQueueLengthMaximum(double simTime) {
		// An entity that is added to an empty queue and removed immediately
		// does not count as a non-zero queue length
		int ret = (int) stats.getMax();
		if (ret == 1 && freq.getBinTime(simTime, 1) == 0.0d)
			return 0;
		return ret;
	}

	@Output(name = "QueueLengthTimes",
	 description = "The total time that the queue has length 0, 1, 2, etc.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 9)
	public double[] getQueueLengthDistribution(double simTime) {
		return freq.getBinTimes(simTime);
	}

	@Output(name = "AverageQueueTime",
	 description = "The average time each entity waits in the queue.  Calculated as total queue time to date divided " +
			"by the total number of entities added to the queue.",
	    unitType = TimeUnit.class,
	  reportable = true,
	    sequence = 10)
	public double getAverageQueueTime(double simTime) {
		return stats.getSum(simTime) / getNumberAdded();
	}

	@Output(name = "MatchValueCount",
	 description = "The present number of unique match values in the queue.",
	    unitType = DimensionlessUnit.class,
	    sequence = 11)
	public int getMatchValueCount(double simTime) {
		return matchMap.size();
	}

	@Output(name = "UniqueMatchValues",
	 description = "The list of unique Match values for the entities in the queue.",
	    sequence = 12)
	public ArrayList<String> getUniqueMatchValues(double simTime) {
		ArrayList<String> ret = new ArrayList<>(matchMap.keySet());
		Collections.sort(ret);
		return ret;
	}

	@Output(name = "MatchValueCountMap",
	 description = "The number of entities in the queue for each Match expression value.\n"
	             + "For example, '[Queue1].MatchValueCountMap(\"SKU1\")' returns the number of "
	             + "entities whose Match value is \"SKU1\".",
	    unitType = DimensionlessUnit.class,
	    sequence = 13)
	public LinkedHashMap<String, Integer> getMatchValueCountMap(double simTime) {
		LinkedHashMap<String, Integer> ret = new LinkedHashMap<>(matchMap.size());
		for (String m : getUniqueMatchValues(simTime)) {
			ret.put(m, matchMap.get(m).size());
		}
		return ret;
	}

	@Output(name = "MatchValueMap",
	 description = "Provides a list of entities in the queue for each Match expression value.\n"
	             + "For example, '[Queue1].MatchValueMap(\"SKU1\")' returns a list of entities "
	             + "whose Match value is \"SKU1\".",
	    sequence = 14)
	public LinkedHashMap<String, ArrayList<DisplayEntity>> getMatchValueMap(double simTime) {
		LinkedHashMap<String, ArrayList<DisplayEntity>> ret = new LinkedHashMap<>(matchMap.size());
		for (String m : getUniqueMatchValues(simTime)) {
			TreeSet<QueueEntry> entrySet = matchMap.get(m);
			ArrayList<DisplayEntity> list = new ArrayList<>(entrySet.size());
			for (QueueEntry entry : entrySet) {
				list.add(entry.entity);
			}
			ret.put(m, list);
		}
		return ret;
	}

	@Output(name = "NumberReneged",
	 description = "The number of entities that reneged from the queue.",
	    unitType = DimensionlessUnit.class,
	  reportable = true,
	    sequence = 15)
	public long getNumberReneged(double simTime) {
		return numberReneged;
	}

	@Output(name = "QueuePosition",
	 description = "The position in the queue for an entity undergoing the RenegeCondition test.\n"
	             + "First in queue = 1, second in queue = 2, etc.",
	    unitType = DimensionlessUnit.class,
	  reportable = false,
	    sequence = 16)
	public long getQueuePosition(double simTime) {
		DisplayEntity objEnt = this.getReceivedEntity(simTime);
		if (objEnt == null)
			return -1;
		int pos = this.getPosition(objEnt);
		if (pos >= 0)
			pos++;
		return pos;
	}

}
