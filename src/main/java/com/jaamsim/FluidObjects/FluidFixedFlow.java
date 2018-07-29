/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.FluidObjects;

import com.jaamsim.Graphics.PolylineInfo;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.VolumeFlowUnit;

/**
 * FluidFixedFlow models a specified flow rate between a source and destination.
 * A null source is taken to be an infinite reservoir to supply fluid.
 * A null destination is taken to be an infinite reservoir to receive fluid.
 * @author Harry King
 *
 */
public class FluidFixedFlow extends FluidFlowCalculation {

	@Keyword(description = "The constant volumetric flow rate from the source to the destination.",
	         exampleList = {"1.0 m3/s"})
	private final ValueInput flowRateInput;

	@Keyword(description = "The width of the pipe segments in pixels.",
	         exampleList = {"1"})
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the pipe.",
	         exampleList = {"red"})
	private final ColourInput colourInput;

	{
		flowRateInput = new ValueInput( "FlowRate", KEY_INPUTS, 0.0d);
		flowRateInput.setValidRange( 0.0d, Double.POSITIVE_INFINITY);
		flowRateInput.setUnitType( VolumeFlowUnit.class );
		this.addInput( flowRateInput);

		widthInput = new ValueInput("Width", GRAPHICS, 1.0d);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		widthInput.setUnitType( DimensionlessUnit.class );
		widthInput.setDefaultText("PolylineModel");
		this.addInput(widthInput);

		colourInput = new ColourInput("Colour", GRAPHICS, ColourInput.BLACK);
		colourInput.setDefaultText("PolylineModel");
		this.addInput(colourInput);
		this.addSynonym(colourInput, "Color");
	}

	@Override
	protected void calcFlowRate(FluidComponent source, FluidComponent destination, double dt) {

		// Update the flow rate
		this.setFlowRate( flowRateInput.getValue() );
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		// If Points were input, then use them to set the start and end coordinates
		if( in == pointsInput || in == colourInput || in == widthInput ) {
			invalidateScreenPoints();
			return;
		}
	}

	@Override
	public PolylineInfo[] buildScreenPoints(double simTime) {
		int wid = -1;
		if (!widthInput.isDefault())
			wid = Math.max(1, widthInput.getValue().intValue());

		Color4d col = null;
		if (!colourInput.isDefault())
			col = colourInput.getValue();

		PolylineInfo[] ret = new PolylineInfo[1];
		ret[0] = new PolylineInfo(getCurvePoints(), col, wid);
		return ret;
	}
}
