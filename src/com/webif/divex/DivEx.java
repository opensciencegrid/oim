package com.webif.divex;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class DivEx {
	private String nodeid;
	private Boolean needupdate = false;
	private String js = "";
	private HashMap<String, String> div_attr = new HashMap();
	
	public DivEx(DivEx _parent) {
		nodeid = DivExRoot.getNewNodeID();
		if(_parent != null) {
			_parent.add(this);
		}
		
		div_attr.put("class", "divex"); //what is this really for? so that user can put border:0px stuff for .divex?
	}
	
	protected ArrayList<DivEx> childnodes = new ArrayList();
	
	//set attr to apply to the div
	public void setAttr(String attr, String value)
	{
		div_attr.put(attr, value);
	}
	
	public void alert(String msg)
	{
		js += "alert('"+msg+"');";
	}
	public void js(String _js)
	{
		js += _js;
	}
	public void redirect(String url) {
		js += "document.location = '"+url+"';";
	}
	public void scrollToShow() {
		js += "var targetOffset = $(\"#"+nodeid+"\").offset().top;";
		js += "$('html,body').animate({scrollTop: targetOffset}, 500);";
	}

	private ArrayList<EventListener> event_listeners = new ArrayList();
	
	public String getNodeID() { return nodeid; }

	//DivEx calls this on *root* upon completion of event handler
	protected String outputUpdatecode()
	{
		//start with user specified js code
		String code = js;
		js = "";
		
		//find child nodes who needs update
		if(needupdate) {
			String success = "";
			code += "$(\"#"+nodeid+"\").load(\"divex?action=load&nodeid="+nodeid+"\", function(){"+success+"});";
			//I don't need to update any of my child - parent will redraw all of it.
			setNeedupdate(false);
		} else {
			//see if any of my children needs update
			for(DivEx d : childnodes) {
				code += d.outputUpdatecode();
			}
		}
		
		return code;
	}
	
	//recursively set mine and my children's needupdate flag
	public void setNeedupdate(Boolean b)
	{
		needupdate = b;
		for(DivEx node : childnodes) {
			node.setNeedupdate(b);
		}
	}
	
	public void add(DivEx child)
	{
		childnodes.add(child);
	}
	
	//recursively do search
	public DivEx findNode(String _nodeid)
	{
		if(nodeid.compareTo(_nodeid) == 0) return this;
		for(DivEx child : childnodes) {
			DivEx node = child.findNode(_nodeid);
			if(node != null) return node;
		}
		return null;
	}
	
	//if you have a custom controller that uses more fundamental html element, override this
	//but don't forget to put all of the dynamic part of the content to renderInside()
	//divex.load action will only call renderInside to redraw the content
	//whatever you put in render() will remains until any of its parent redraws
	public String render() {
		String attrs = "";
		for(String attr : div_attr.keySet()) {
			String value = div_attr.get(attr);
			attrs += attr + "=\""+value+"\" ";
		}
		
		String html = "";
		html += "<div "+attrs+" id='"+nodeid+"'>";
		html += renderInside();
		html += "</div>";
		return html;
	}
	
	//override this to draw the inside of your div.
	public String renderInside()
	{
		String html = "";
		for(DivEx child : childnodes) {
			html += child.render();
		}
		return html;
	}
	
	public void addEventListener(EventListener listener)
	{
		event_listeners.add(listener);
	}
		
	public void doGet(DivExRoot root, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String action = request.getParameter("action");
		if(action.compareTo("load") == 0) {
			PrintWriter writer = response.getWriter();
			response.setContentType("text/html");
			writer.print(renderInside());
		} else if(action.compareTo("request") == 0) {
			this.onRequest(request, response);
		} else {
			Event e = new Event(request, response);

			//handle my event handler
			this.onEvent(e);
			//notify event listener
			for(EventListener listener : event_listeners) {
				listener.handleEvent(e);
			}

			//emit all requested update code
			PrintWriter writer = response.getWriter();
			response.setContentType("text/javascript");
			writer.print(root.outputUpdatecode());
		}
	}
	
	//events are things like click, drag, change.. you are responsible for updating 
	//the internal state of the target div, and framework will call outputUpdatecode()
	//to emit re-load request which will then re-render the divs that are changed.
	//Override this to handle local events (for remote events, use listener)
	protected void onEvent(Event e) {}
	
	//request are things like outputting XML or JSON back to browser without changing
	//any internal state. it's like load but it doesn't return html necessary. it could
	//be XML, JSON, Image, etc.. The framework will not emit any update code
	protected void onRequest(HttpServletRequest request, HttpServletResponse response) {}
	
	//only set needupdate on myself - since load will redraw all its children
	//once drawing is done, needudpate = false will be performec recursively
	public void redraw() {
		needupdate = true;
	}
	
	public static String encodeHTML(String s)
	{
	    StringBuffer out = new StringBuffer();
	    for(int i=0; i<s.length(); i++)
	    {
	        char c = s.charAt(i);
	        if(c > 127 || c=='"' || c=='\'' || c=='<' || c=='>')
	        {
	           out.append("&#"+(int)c+";");
	        }
	        else
	        {
	            out.append(c);
	        }
	    }
	    return out.toString();
	}
}
