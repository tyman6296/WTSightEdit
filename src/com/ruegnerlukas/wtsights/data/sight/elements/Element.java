package com.ruegnerlukas.wtsights.data.sight.elements;

import com.ruegnerlukas.wtsights.data.DataPackage;
import com.ruegnerlukas.wtsights.data.sight.elements.layouts.ILayoutData;

public abstract class Element {

	public String name;
	public ElementType type;
	
	protected Element(String name, ElementType type) {
		this.name = name;
		this.type = type;
	}
	
	
	public abstract ILayoutData layout(DataPackage data, double canvasWidth, double canvasHeight);
	
	public abstract void setDirty();
	
}
