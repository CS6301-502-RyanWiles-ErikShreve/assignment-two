package edu.utdallas.cs6301_502.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="bug_report")
public class BugReport {
	
	private String title;
	private String description;
	private String systemVersion;
	private String unitTest;
	private ChangeSet changeSet;
	
	public String getTitle() {
		return title;
	}
	
	@XmlElement(name="title")
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	@XmlElement(name="description")
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getSystemVersion() {
		return systemVersion;
	}
	
	@XmlElement(name="system_version")
	public void setSystemVersion(String systemVersion) {
		this.systemVersion = systemVersion;
	}
	
	public String getUnitTest() {
		return unitTest;
	}
	
	@XmlElement(name="system_version")
	public void setUnitTest(String unitTest) {
		this.unitTest = unitTest;
	}
	
	public ChangeSet getChangeSet() {
		return changeSet;
	}
	
	@XmlElement(name="change_set")
	public void setChangeSet(ChangeSet changeSet) {
		this.changeSet = changeSet;
	}
	
	
}
