package com.scm.deep.helper;

import lombok.Data;

@Data
public class MessageCenter {

	
	private String content;
	private String type;

	public MessageCenter(String content, String type) {
		super();
		this.content = content;
		this.type = type;
	}
	
	
	
}
