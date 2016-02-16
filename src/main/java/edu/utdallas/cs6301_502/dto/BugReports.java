package edu.utdallas.cs6301_502.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "bug_reports")
public final class BugReports {
	private List<BugReport> bugReport;

	@XmlElement(name="bug_report")
	public List<BugReport> getBugReports() {
		return bugReport;
	}

	public void setBugReports(List<BugReport> bugReport) {
		this.bugReport = bugReport;
	}

}