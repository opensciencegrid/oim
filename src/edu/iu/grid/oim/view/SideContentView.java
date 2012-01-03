package edu.iu.grid.oim.view;

import java.io.PrintWriter;
import java.util.ArrayList;
import com.divrep.DivRep;
import com.divrep.DivRepPage;

//put each content under a side content header
public class SideContentView implements IView {

	private ArrayList<IView> children = new ArrayList<IView>();
	
	public void add(IView v) {
		children.add(v);
	}
	
	public void add(DivRepPage de) {
		add(new DivRepWrapper(de));
	}
	
	public void add(String html) {
		add(new HtmlView(html));
	}
	public void add(String title, IView v) {
		children.add(new HtmlView("<h3>"+title+"</h3>"));
		children.add(new HtmlView("<div class=\"indent\">"));
		children.add(v);
		children.add(new HtmlView("</div>"));
	}
	
	public void add(String title, DivRep de) {
		add(title, new DivRepWrapper(de));
	}
	
	public void add(String title, String html) {
		add(title, new HtmlView(html));
	}
	
	public void addContactNote () {
		//TODO agopu need external link icon in CSS
		add("Contacts", new HtmlView("<p>The contact editor allow you to search and assign contacts.</p><p>When assigning various contacts on this form, if you do not find the contact you are searching for, then <a href=\"contactedit\" target=\"_blank\">add a new contact</a> first."));		
	}
	public void addContactLegend () {
		add("Contact Rank Legend", new HtmlView("<p>Contacts are flagged by their rank:</p><p><div class=\'contact_rank contact_Primary\'>Primary</div></p><p><div class=\'contact_rank contact_Secondary\'>Secondary</div></p><p><div class=\'contact_rank contact_Tertiary\'>Tertiary</div></p>"));		
	}
	public void addContactGroupFlagLegend () {
		add("Contact Grouping Legend", new HtmlView("<p>Contacts are flagged based on whether they are tagged as a group contact or not:</p><p><div class=\'contact_rank contact_flag_Group\'>Group Contact (Mailing list, etc.)</div></p><p><div class=\'contact_rank contact_flag_Person\'>Non-Group Contact (Human user or Service-cert. mapped user)</div></p>"));		
	}
	public void render(PrintWriter out)
	{
		out.println("<div id=\"sideContent\">");
		for(IView v : children) {
			v.render(out);
		}
		out.println("</div>");
	}

}
