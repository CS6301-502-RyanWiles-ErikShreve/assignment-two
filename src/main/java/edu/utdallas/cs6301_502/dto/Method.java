package edu.utdallas.cs6301_502.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="method")
public class Method {

	private String id;
	private String signature;
	private String file;
	
	public String getId() {
		return id;
	}

	@XmlElement(name="id")
	public void setId(String id) {
		this.id = id;
	}

	public String setSignature() {
		return signature;
	}

	@XmlElement(name="signature")
	public void setSignature(String signature) {
		this.signature = signature;
	}

	public String getFile() {
		return file;
	}

	@XmlElement(name="file")
	public void setFile(String file) {
		this.file = file;
	}

	
}
