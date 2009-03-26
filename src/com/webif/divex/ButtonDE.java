package com.webif.divex;

public class ButtonDE extends DivEx {
	String title;
	
	static public enum Style { BUTTON, ALINK };
	Style style = Style.BUTTON;
	public void setStyle(Style _style) { style = _style; }
	
	public ButtonDE(DivEx parent, String _title) {
		super(parent);
		title = _title;
	}
	
	public String render() {
		String html = "";
	
		switch(style) {
		case BUTTON:
			html += "<input type='button' id='"+getNodeID()+"' onclick='divex_click(this.id);' value='"+title+"' />";
			break;
		case ALINK:
			html += "<a href='#' id='"+getNodeID()+"' onclick='divex_click(this.id);return false;'>"+title+"</a>";
			break;
		}

		return html;
	}
}