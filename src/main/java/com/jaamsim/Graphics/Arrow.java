/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;

public class Arrow extends DisplayEntity {

	@Keyword(description = "The colour of the arrow.",
	         exampleList = {"red"})
	private final ColourInput color;

	@Keyword(description = "The width of the Arrow line segments in pixels.",
	         exampleList = {"1"})
	private final ValueInput width;

	@Keyword(description = "A set of (x, y, z) numbers that define the size of the arrowhead.",
	         exampleList = {"0.165 0.130 0.0 m"})
	private final Vec3dInput arrowHeadSize;

	{
		color = new ColourInput("Colour", GRAPHICS, ColourInput.BLACK);
		color.setDefaultText("PolylineModel");
		this.addInput(color);
		this.addSynonym(color, "Color");

		width = new ValueInput("Width", GRAPHICS, 1.0d);
		width.setUnitType(DimensionlessUnit.class);
		width.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		width.setDefaultText("PolylineModel");
		this.addInput(width);

		arrowHeadSize = new Vec3dInput( "ArrowHeadSize", GRAPHICS, new Vec3d(0.1d, 0.1d, 0.0d) );
		arrowHeadSize.setUnitType(DistanceUnit.class);
		arrowHeadSize.setDefaultText("PolylineModel");
		this.addInput( arrowHeadSize );
		this.addSynonym(arrowHeadSize, "ArrowSize");
	}

	public Arrow() {}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		// If Points were input, then use them to set the start and end coordinates
		if( in == pointsInput || in == color || in == width ) {
			invalidateScreenPoints();
			return;
		}
	}

	@Override
	public PolylineInfo[] buildScreenPoints(double simTime) {
		int wid = -1;
		if (!width.isDefault())
			wid = Math.max(1, width.getValue().intValue());

		Color4d col = null;
		if (!color.isDefault())
			col = color.getValue();

		PolylineInfo[] ret = new PolylineInfo[1];
		ret[0] = new PolylineInfo(getCurvePoints(), col, wid);
		return ret;
	}

	public Vec3dInput getArrowHeadSizeInput() {
		return arrowHeadSize;
	}

}
