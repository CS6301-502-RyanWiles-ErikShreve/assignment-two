package edu.utdallas.cs6301_502.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="modified_methods")
public class ModifiedMethods {
	private List<Method> methods;
	
	@XmlElement(name = "method")
	public List<Method> getMethods() {
		return methods;
	}

	public void setMethods(List<Method> methods) {
		this.methods = methods;
	}

	
}
