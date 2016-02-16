package edu.utdallas.cs6301_502.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "change_set")
public class ChangeSet {

	private String systemRevision;
	private ModifiedMethods modifiedMethods;

	public String getSystemRevision() {
		return systemRevision;
	}
	
	@XmlElement(name="system_revision")
	public void setSystemRevision(String systemRevision) {
		this.systemRevision = systemRevision;
	}

	@XmlElement(name = "modified_methods")
	public ModifiedMethods getModifiedMethods() {
		return modifiedMethods;
	}

	public void setModifiedMethods(ModifiedMethods modifiedMethods) {
		this.modifiedMethods = modifiedMethods;
	}

}
