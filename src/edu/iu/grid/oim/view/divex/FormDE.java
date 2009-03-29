package edu.iu.grid.oim.view.divex;

import com.webif.divex.ButtonDE;
import com.webif.divex.DivEx;
import com.webif.divex.Event;
import com.webif.divex.EventListener;
import com.webif.divex.form.IFormElementDE;

abstract public class FormDE extends DivEx {
	
	//URL to go after cancel or submit button is selected
	private String origin_url;
	
	public ButtonDE submitbutton;
	public ButtonDE cancelbutton;
	
	//private String error;
	private Boolean valid;
	
	public FormDE(DivEx parent, String _origin_url)
	{
		super(parent);
		
		origin_url = _origin_url;
		
		submitbutton = new ButtonDE(this, "Submit");
		submitbutton.addEventListener(new EventListener() {
			public void handleEvent(Event e) { submit(); }
		});
		
		cancelbutton = new ButtonDE(this, "Cancel");
		cancelbutton.setStyle(ButtonDE.Style.ALINK);
		cancelbutton.addEventListener(new EventListener() {
			public void handleEvent(Event e) { 
				redirect(origin_url);
			}
		});
	}
	
	private void submit()
	{
		if(isValid()) {
			if(doSubmit()) {
				alert("Success!");
				redirect(origin_url);	
			}
		} else {
			//error = "Please correct the issues before submitting your form.";
			//redraw();
			//scrollToShow();
			alert("Please correct the issues before submitting your form.");
		}
	}
	
	abstract protected Boolean doSubmit();
	
	public void validate()
	{
		redraw();
		valid = true;
		
		//validate *all* elements
		for(DivEx child : childnodes) {
			if(child instanceof IFormElementDE) { 
				IFormElementDE element = (IFormElementDE)child;
				if(element != null) {
					if(!element.isValid()) {
						valid = false;
					}
				}
			}
		}
	}
	public Boolean isValid()
	{
		validate();
		return valid;
	}
	
	/*
	public IFormElementDE getElement(String name) {
		for(DivEx child : childnodes) {
			if(child instanceof IFormElementDE) { 
				IFormElementDE element = (IFormElementDE)child;
				if(element.getName().compareTo(name) == 0) {
					return element;
				}
			}
		}
		return null;
	}
	*/

	public String render() 
	{
		String out = "";
		out += "<div id=\""+getNodeID()+"\" class='form'>";	
		for(DivEx child : childnodes) {
			//we display submit / cancel button at the end
			if(child == submitbutton || child == cancelbutton) continue;
			
			out += "<div class=\"form_element\">" + child.render() + "</div>";
		}

		out += submitbutton.render();
		if(cancelbutton != null) {
			out += " or ";
			out += cancelbutton.render();
		}
		
		out += "</div>";
		return out;
	}
}
