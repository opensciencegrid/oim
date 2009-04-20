package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.webif.divex.ButtonDE;
import com.webif.divex.DivEx;
import com.webif.divex.DivExRoot;
import com.webif.divex.Event;

import edu.iu.grid.oim.lib.Config;
import edu.iu.grid.oim.model.db.ContactRankModel;
import edu.iu.grid.oim.model.db.ContactTypeModel;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.ResourceAliasModel;
import edu.iu.grid.oim.model.db.ResourceContactModel;
import edu.iu.grid.oim.model.db.ResourceDowntimeModel;
import edu.iu.grid.oim.model.db.ResourceDowntimeServiceModel;
import edu.iu.grid.oim.model.db.ResourceGroupModel;
import edu.iu.grid.oim.model.db.ResourceModel;
import edu.iu.grid.oim.model.db.ResourceServiceModel;
import edu.iu.grid.oim.model.db.ServiceModel;
import edu.iu.grid.oim.model.db.record.ContactRankRecord;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.ContactTypeRecord;
import edu.iu.grid.oim.model.db.record.ResourceAliasRecord;
import edu.iu.grid.oim.model.db.record.ResourceContactRecord;
import edu.iu.grid.oim.model.db.record.ResourceDowntimeRecord;
import edu.iu.grid.oim.model.db.record.ResourceDowntimeServiceRecord;
import edu.iu.grid.oim.model.db.record.ResourceGroupRecord;
import edu.iu.grid.oim.model.db.record.ResourceRecord;
import edu.iu.grid.oim.model.db.record.ResourceServiceRecord;
import edu.iu.grid.oim.model.db.record.ServiceRecord;
import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.DivExWrapper;
import edu.iu.grid.oim.view.GenericView;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.IView;
import edu.iu.grid.oim.view.MenuView;
import edu.iu.grid.oim.view.Page;
import edu.iu.grid.oim.view.RecordTableView;
import edu.iu.grid.oim.view.SideContentView;

public class ResourceServlet extends ServletBase implements Servlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(ResourceServlet.class);  
	
    public ResourceServlet() {
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{	
		setAuth(request);
		
		//pull list of all vos
		Collection<ResourceRecord> resources = null;
		ResourceModel model = new ResourceModel(auth);
		try {
			resources = model.getAllEditable();
		
			//construct view
			MenuView menuview = createMenuView("resource");
			DivExRoot root = DivExRoot.getInstance(request);
			ContentView contentview = createContentView(root, resources);
			Page page = new Page(menuview, contentview, createSideView(root));
			page.render(response.getWriter());			
		} catch (SQLException e) {
			log.error(e);
			throw new ServletException(e);
		}
	}
	
	protected ContentView createContentView(final DivExRoot root, Collection<ResourceRecord> resources) 
		throws ServletException, SQLException
	{
		ContentView contentview = new ContentView();	
		contentview.add(new HtmlView("<h1>Resource</h1>"));
	
		for(ResourceRecord rec : resources) {
			
			//TODO - need to make "disabled" more conspicuous
			String name = rec.name;
			/*
			if(rec.disable) {
				name += " (Disabled)";
			}
			*/
			contentview.add(new HtmlView("<h2>"+StringEscapeUtils.escapeHtml(name)+"</h2>"));
	
			/*
			//RSS feed button
			contentview.add(new HtmlView("<div class=\"right\"><a href=\"http://oimupdate.blogspot.com/feeds/posts/default/-/resource_"+rec.id+"\" target=\"_blank\"/>"+
					"Subscribe to Updates</a></div>"));
			*/
			RecordTableView table = new RecordTableView();
			contentview.add(table);

			table.addRow("Description", rec.description);
			table.addRow("URL", new HtmlView("<a target=\"_blank\" href=\""+rec.url+"\">"+rec.url+"</a>"));
			table.addRow("Active", rec.active);
			table.addRow("Disable", rec.disable);
			table.addRow("FQDN", rec.fqdn);
			table.addRow("Alias", new HtmlView(getAlias(rec.id)));
			
			//pull parent VO
			ResourceGroupModel model = new ResourceGroupModel(auth);
			ResourceGroupRecord resource_group_rec = model.get(rec.resource_group_id);
			String resource_group_name = null;
			if(resource_group_rec != null) {
				resource_group_name = resource_group_rec.name;
			}
			table.addRow("Resource Group Name", resource_group_name);
			
			//Resource Services
			ResourceServiceModel rsmodel = new ResourceServiceModel(auth);
			ArrayList<ResourceServiceRecord> services = rsmodel.getAllByResourceID(rec.id);
			GenericView services_view = new GenericView();
			for(ResourceServiceRecord rsrec : services) {
				services_view.add(createServiceView(rsrec));
			}
			table.addRow("Services", services_view);
			
			//downtime
			GenericView downtime_view = new GenericView();
			ResourceDowntimeModel dmodel = new ResourceDowntimeModel(auth);
			for(ResourceDowntimeRecord drec : dmodel.getFutureDowntimesByResourceID(rec.id)) {
				downtime_view.add(createDowntimeView(root, drec));
			}
			table.addRow("Future Downtime Schedule", downtime_view);
	
			ContactTypeModel ctmodel = new ContactTypeModel(auth);
			ContactRankModel crmodel = new ContactRankModel(auth);
			ContactModel pmodel = new ContactModel(auth);
			
			//contacts (only shows contacts that are filled out)
			ResourceContactModel rcmodel = new ResourceContactModel(auth);
			ArrayList<ResourceContactRecord> rclist = rcmodel.getByResourceID(rec.id);
			HashMap<Integer, ArrayList<ResourceContactRecord>> voclist_grouped = rcmodel.groupByContactTypeID(rclist);
			for(Integer type_id : voclist_grouped.keySet()) {
				ArrayList<ResourceContactRecord> clist = voclist_grouped.get(type_id);
				ContactTypeRecord ctrec = ctmodel.get(type_id);
				
				String cliststr = "";
				
				for(ResourceContactRecord vcrec : clist) {
					ContactRecord person = pmodel.get(vcrec.contact_id);
					ContactRankRecord rank = crmodel.get(vcrec.contact_rank_id);

					cliststr += "<div class='contact_rank contact_"+rank.name+"'>";
					cliststr += person.name;
					cliststr += "</div>";
				
				}
				
				table.addRow(ctrec.name, new HtmlView(cliststr));
			}			
		
			class EditButtonDE extends ButtonDE
			{
				String url;
				public EditButtonDE(DivEx parent, String _url)
				{
					super(parent, "Edit");
					url = _url;
				}
				protected void onEvent(Event e) {
					redirect(url);
				}
			};
			table.add(new DivExWrapper(new EditButtonDE(root, Config.getApplicationBase()+"/resourceedit?resource_id=" + rec.id)));
		}
		
		return contentview;
	}
	
	private IView createServiceView(ResourceServiceRecord rec)
	{
		GenericView view = new GenericView();
		
		try {
			ServiceModel smodel = new ServiceModel(auth);
			ServiceRecord srec;
			srec = smodel.get(rec.service_id);

			RecordTableView table = new RecordTableView("inner_table");
			table.addHeaderRow(srec.description);
			table.addRow("Hidden", rec.hidden);
			table.addRow("Central", rec.central);
			table.addRow("End Point Override", rec.endpoint_override);
			table.addRow("Server List RegEx", rec.server_list_regex);
			view.add(table);

		} catch (SQLException e) {
			log.error(e);
		}
		
		return view;
	}
	
	private IView createDowntimeView(final DivExRoot root, ResourceDowntimeRecord rec) throws SQLException
	{
		GenericView view = new GenericView();
		RecordTableView table = new RecordTableView("inner_table");
		table.addHeaderRow("Downtime");
		table.addRow("Summary", rec.downtime_summary);
		table.addRow("Start Time", rec.start_time.toString() + " UTC");
		table.addRow("End Time", rec.end_time.toString() + " UTC");
		table.addRow("Downtime Class", rec.toString(rec.downtime_class_id, auth));
		table.addRow("Downtime Severity", rec.toString(rec.downtime_severity_id, auth));
		table.addRow("Affected Services", createAffectedServices(rec.id));
		table.addRow("DN", rec.toString(rec.dn_id, auth));
		table.addRow("Disable", rec.disable);		

		view.add(table);
		
		return view;
	}
	
	private IView createAffectedServices(int downtime_id) throws SQLException
	{
		String html = "";
		ResourceDowntimeServiceModel model = new ResourceDowntimeServiceModel(auth);
		Collection<ResourceDowntimeServiceRecord> services;

		services = model.getByDowntimeID(downtime_id);
		for(ResourceDowntimeServiceRecord service : services) {
			html += service.toString(service.service_id, auth) + "<br/>";
		}
		return new HtmlView(html);

	}
	
	private String getAlias(int resource_id) throws SQLException
	{
		String html = "";
		ResourceAliasModel ramodel = new ResourceAliasModel(auth);
		ArrayList<ResourceAliasRecord> recs = ramodel.getAllByResourceID(resource_id);
		for(ResourceAliasRecord rec : recs) {
			html += StringEscapeUtils.escapeHtml(rec.resource_alias) + "<br/>";
		}
		return html;
	}
		
	private SideContentView createSideView(DivExRoot root)
	{
		SideContentView view = new SideContentView();
		
		class NewButtonDE extends ButtonDE
		{
			String url;
			public NewButtonDE(DivEx parent, String _url)
			{
				super(parent, "Add New Resource");
				url = _url;
			}
			protected void onEvent(Event e) {
				redirect(url);
			}
		};
		view.add("Operation", new NewButtonDE(root, "resourceedit"));
		view.add("About", new HtmlView("This page shows a list of resources that you have access to edit."));		
		return view;
	}
}
