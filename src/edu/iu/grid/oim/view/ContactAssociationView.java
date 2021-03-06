package edu.iu.grid.oim.view;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.ContactRank;
import edu.iu.grid.oim.model.db.ContactTypeModel;
import edu.iu.grid.oim.model.db.ResourceContactModel;
import edu.iu.grid.oim.model.db.ResourceModel;
import edu.iu.grid.oim.model.db.SCContactModel;
import edu.iu.grid.oim.model.db.SCModel;
import edu.iu.grid.oim.model.db.VOContactModel;
import edu.iu.grid.oim.model.db.VOModel;
import edu.iu.grid.oim.model.db.record.ContactTypeRecord;
import edu.iu.grid.oim.model.db.record.ResourceContactRecord;
import edu.iu.grid.oim.model.db.record.ResourceRecord;
import edu.iu.grid.oim.model.db.record.SCContactRecord;
import edu.iu.grid.oim.model.db.record.SCRecord;
import edu.iu.grid.oim.model.db.record.VOContactRecord;
import edu.iu.grid.oim.model.db.record.VORecord;

public class ContactAssociationView extends GenericView {
    static Logger log = Logger.getLogger(ContactAssociationView.class);  
    
	private GenericView view;
	private UserContext context;
	private boolean show_new_buttons;
	private int contactid;
    
	public ContactAssociationView(UserContext context, int contactid) throws SQLException
	{
		this.context = context;
		this.contactid = contactid;
		show_new_buttons = false;
	}
	
	public void showNewButtons(boolean b) {
		show_new_buttons = b;
	}
	
	private GenericView createView(int contactid) 
	{
		GenericView view = new GenericView();
		view.add(new HtmlView("<div class=\"contact_association\">"));

		try {
			ResourceModel rmodel = new ResourceModel(context);
			ResourceContactModel rcontactmodel = new ResourceContactModel(context);
			
			VOModel vomodel = new VOModel(context);
			VOContactModel vocontactmodel = new VOContactModel(context);
			
			SCModel scmodel = new SCModel(context);
			SCContactModel sccontactmodel = new SCContactModel(context);
			
			//aux..
			ContactTypeModel ctmodel = new ContactTypeModel(context);
			HashMap<Integer, ContactTypeRecord> ctlist = ctmodel.getAll();
			//ContactRankModel crmodel = new ContactRankModel(context);
			//HashMap<Integer, ContactRankRecord> crlist = crmodel.getAll();
			
			if(show_new_buttons) {
				view.add(new HtmlView("<a href=\"resourceedit\" class=\"btn pull-right\"><i class=\"icon-plus-sign\"></i> Register New Resource</a>"));
			}
			view.add(new HtmlView("<h3>My Resources</h3>"));
			ArrayList<ResourceContactRecord> rcrecs = rcontactmodel.getByContactID(contactid);
			HashMap<Integer, String> resourceassoc = new HashMap<Integer, String>();
			for(ResourceContactRecord rcrec : rcrecs) {
				ResourceRecord rrec = rmodel.get(rcrec.resource_id);
				resourceassoc.put(rrec.id, rrec.name);
			}
			
			BootItemTableView table = new BootItemTableView(3);
			for(Integer rid : resourceassoc.keySet()) {
				String name = resourceassoc.get(rid);
				GenericView tview = new GenericView();
				String cls = "";
				ResourceRecord rrec = rmodel.get(rid);
				if(rrec.active == false) {
					cls += " well_inactive";
					name += "&nbsp;<span class=\"pull-right label\">Inactive</span>";
				}
				if(rrec.disable == true) {
					cls += " well_disabled";
					name += "&nbsp;<span class=\"pull-right label\">Disabled</span>";
				}
				tview.add(new HtmlView("<div class=\"well "+cls+"\">"));
				tview.add(new HtmlView("<a href=\"resourceedit?id="+rid+"\">"+name+"</a><br>"));
				
				//show which contact types the user is associated with
				ArrayList<ResourceContactRecord> recs = rcontactmodel.getByResourceID(rid);
				for(ResourceContactRecord rec : recs) {
					if(rec.contact_id.equals(contactid)) {
						ContactTypeRecord ctrec = ctlist.get(rec.contact_type_id);
						ContactRank rank = ContactRank.get(rec.contact_rank_id);
						tview.add(new HtmlView("<div class=\"contact_rank contact_"+rank+"\">"+ctrec.name+"</div>"));
					}
				}
				tview.add(new HtmlView("</div>"));
				table.add(tview);
			}
			view.add(table);	
			if(resourceassoc.size() == 0) {
				view.add(new HtmlView("<p>None</p>"));
			}
			view.add(new HtmlView("<br clear=\"all\">"));
			
			if(show_new_buttons) {
				view.add(new HtmlView("<a href=\"voedit\" class=\"btn pull-right\"><i class=\"icon-plus-sign\"></i> Register New Virtual Organization</a>"));
			}
			view.add(new HtmlView("<h3>My Virtual Organizations</h3>"));
			ArrayList<VOContactRecord> vocrecs = vocontactmodel.getByContactID(contactid);
			HashMap<Integer, String> voassoc = new HashMap<Integer, String>();
			for(VOContactRecord vocrec : vocrecs) {
				VORecord vorec = vomodel.get(vocrec.vo_id);
				voassoc.put(vorec.id, vorec.name);
			}
			
			table = new BootItemTableView(3);
			for(Integer vo_id : voassoc.keySet()) {
				String name = voassoc.get(vo_id);
				GenericView tview = new GenericView();
				VORecord vorec = vomodel.get(vo_id);
				String cls = "";
				if(vorec.active == false) {
					cls += " well_inactive";
					name += "<span class=\"pull-right label\">Inactive</span>";
				}
				if(vorec.disable == true) {
					cls += " well_disabled";
					name += "<span class=\"pull-right label\">Disabled</span>";
				}
				tview.add(new HtmlView("<div class=\"well "+cls+"\">"));
				tview.add(new HtmlView("<a href=\"voedit?id="+vo_id+"\">"+name+"</a>"));
				
				//show which contact types the user is associated with
				ArrayList<VOContactRecord> recs = vocontactmodel.getByVOID(vo_id);
				for(VOContactRecord rec : recs) {
					if(rec.contact_id.equals(contactid)) {
						ContactTypeRecord ctrec = ctlist.get(rec.contact_type_id);
						ContactRank rank = ContactRank.get(rec.contact_rank_id);
						tview.add(new HtmlView("<div class=\"contact_rank contact_"+rank+"\">"+ctrec.name+"</div>"));
					}
				}
				tview.add(new HtmlView("</div>"));
				table.add(tview);
			}
			view.add(table);	
			if(voassoc.size() == 0) {
				view.add(new HtmlView("<p>None</p>"));
			}
			view.add(new HtmlView("<br clear=\"all\">"));
			
			if(show_new_buttons) {
				view.add(new HtmlView("<a href=\"scedit\" class=\"btn pull-right\"><i class=\"icon-plus-sign\"></i> Register New Support Center</a>"));
			}
			view.add(new HtmlView("<h3>My Support Centers</h3>"));
			ArrayList<SCContactRecord> sccrecs = sccontactmodel.getByContactID(contactid);
			HashMap<Integer, String> scassoc = new HashMap<Integer, String>();
			for(SCContactRecord sccrec : sccrecs) {
				SCRecord screc = scmodel.get(sccrec.sc_id);
				//if(screc.active && !screc.disable) {
				//if(scmodel.canEdit(screc.id)) {
					scassoc.put(screc.id, screc.name);
				//}
			}
			
			
			table = new BootItemTableView(3);
			for(Integer scid : scassoc.keySet()) {
				String name = scassoc.get(scid);
				GenericView tview = new GenericView();
				SCRecord screc = scmodel.get(scid);
				String cls = "";
				if(screc.active == false) {
					cls += " well_inactive";
					name += "<span class=\"pull-right label\">Inactive</span>";
				}
				if(screc.disable == true) {
					cls += " well_disabled";
					name += "<span class=\"pull-right label\">Disabled</span>";
				}
				tview.add(new HtmlView("<div class=\"well "+cls+"\">"));
				tview.add(new HtmlView("<a href=\"scedit?id="+scid+"\">"+name+"</a>"));
				
				//show which contact types the user is associated with
				ArrayList<SCContactRecord> recs = sccontactmodel.getBySCID(scid);
				for(SCContactRecord rec : recs) {
					if(rec.contact_id.equals(contactid)) {
						ContactTypeRecord ctrec = ctlist.get(rec.contact_type_id);
						ContactRank rank = ContactRank.get(rec.contact_rank_id);
						tview.add(new HtmlView("<div class=\"contact_rank contact_"+rank+"\">"+ctrec.name+"</div>"));
					}
				}
				tview.add(new HtmlView("</div>"));
				table.add(tview);
			}
			view.add(table);		
			if(scassoc.size() == 0) {
				view.add(new HtmlView("<p>None</p>"));
			}
			view.add(new HtmlView("<br clear=\"all\">"));
			
			
			//view.add(new HtmlView("<br style=\"clear:both;\" />"));
		} catch (SQLException e) {
			view.add(new HtmlView("<p>Error while constructing conact association view</p>"));
			log.error("Failed to construct contact association view", e);
		}
		
		view.add(new HtmlView("</div>"));
		return view;
	}
	
	public void render(PrintWriter out)
	{
		view = createView(contactid);
		view.render(out);
	}
}
