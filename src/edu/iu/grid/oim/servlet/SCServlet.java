package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

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

import edu.iu.grid.oim.lib.Authorization.AuthorizationException;
import edu.iu.grid.oim.model.db.ContactRankModel;
import edu.iu.grid.oim.model.db.ContactTypeModel;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.SCContactModel;
import edu.iu.grid.oim.model.db.SCModel;
import edu.iu.grid.oim.model.db.record.SCRecord;
import edu.iu.grid.oim.model.db.record.SCContactRecord;
import edu.iu.grid.oim.model.db.record.ContactRankRecord;
import edu.iu.grid.oim.model.db.record.ContactTypeRecord;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.DivExWrapper;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.IView;
import edu.iu.grid.oim.view.MenuView;
import edu.iu.grid.oim.view.Page;
import edu.iu.grid.oim.view.RecordTableView;
import edu.iu.grid.oim.view.SideContentView;
import edu.iu.grid.oim.view.TableView;
import edu.iu.grid.oim.view.Utils;
import edu.iu.grid.oim.view.TableView.Row;

public class SCServlet extends ServletBase implements Servlet {
	private static final long serialVersionUID = 1L;
	static Logger log = Logger.getLogger(SCServlet.class);  
	
    public SCServlet() {
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{	
		setAuth(request);
		
		//pull list of all SCs
		Collection<SCRecord> scs = null;
		SCModel model = new SCModel(auth);
		try {
			scs = model.getAllEditable();
		
			//construct view
			MenuView menuview = createMenuView("sc");
			DivExRoot root = DivExRoot.getInstance(request);
			ContentView contentview = createContentView(root, scs);
			Page page = new Page(menuview, contentview, createSideView(root));
			page.render(response.getWriter());			
		} catch (SQLException e) {
			log.error(e);
			throw new ServletException(e);
		}
	}
	
	protected ContentView createContentView(final DivExRoot root, Collection<SCRecord> scs) 
		throws ServletException, SQLException
	{
		ContentView contentview = new ContentView();	
		contentview.add(new HtmlView("<h1>Support Centers</h1>"));
	
		for(SCRecord rec : scs) {
			contentview.add(new HtmlView("<h2>"+StringEscapeUtils.escapeHtml(rec.name)+"</h2>"));
			
			log.debug("Rendering SC " + rec.name);
	
			RecordTableView table = new RecordTableView();
			contentview.add(table);

		 	table.addRow("Long Name", rec.long_name);
			table.addRow("Description", rec.description);
			table.addRow("Community", rec.community);
			if(auth.allows("admin")) {
				table.addRow("Footprints ID", rec.footprints_id);
				table.addRow("Active", rec.active);
				table.addRow("Disable", rec.disable);
			}

			ContactTypeModel ctmodel = new ContactTypeModel(auth);
			ContactRankModel crmodel = new ContactRankModel(auth);
			ContactModel pmodel = new ContactModel(auth);
			
			//contacts (only shows contacts that are filled out)
			SCContactModel sccmodel = new SCContactModel(auth);
			ArrayList<SCContactRecord> scclist = sccmodel.getBySCID(rec.id);
			HashMap<Integer, ArrayList<SCContactRecord>> scclist_grouped = sccmodel.groupByContactTypeID(scclist);
			for(Integer type_id : scclist_grouped.keySet()) {
				ArrayList<SCContactRecord> clist = scclist_grouped.get(type_id);
				ContactTypeRecord ctrec = ctmodel.get(type_id);
				
				String cliststr = "";
				
				for(SCContactRecord sccrec : clist) {
					ContactRecord person = pmodel.get(sccrec.contact_id);
					ContactRankRecord rank = crmodel.get(sccrec.contact_rank_id);

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
			table.add(new DivExWrapper(new EditButtonDE(root, BaseURL()+"/scedit?sc_id=" + rec.id)));
			
			/*
			class DeleteDialogDE extends DialogDE
			{
				VORecord rec;
				public DeleteDialogDE(DivEx parent, VORecord _rec)
				{
					super(parent, "Delete " + _rec.name, "Are you sure you want to delete this Virtual Organization and associated contacts?");
					rec = _rec;
				}
				protected void onEvent(Event e) {
					if(e.getValue().compareTo("ok") == 0) {
						VOModel model = new VOModel(con, auth);
						try {
							model.delete(rec.id);
							alert("Record Successfully removed.");
							redirect("vo");
						} catch (AuthorizationException e1) {
							log.error(e1);
							alert(e1.getMessage());
						} catch (SQLException e1) {
							log.error(e1);
							alert(e1.getMessage());
						}
					}
				}
			}
		
			if(auth.allows("admin_vo")) {
				final DeleteDialogDE delete_dialog = new DeleteDialogDE(root, rec);
				table.add(" or ");
				table.add(delete_dialog);
				
				class DeleteButtonDE extends ButtonDE
				{
					public DeleteButtonDE(DivEx parent, String _name)
					{
						super(parent, "Delete");
						setStyle(ButtonDE.Style.ALINK);
					}
					protected void onEvent(Event e) {
						delete_dialog.open();
					}
				};
				table.add(new DeleteButtonDE(root, rec.name));
			}	
			*/

		}
		
		return contentview;
	}
	
	private String getSCName(Integer sc_id) throws SQLException
	{
		if(sc_id == null) return null;
		SCModel model = new SCModel(auth);
		SCRecord sc = model.get(sc_id);	
		if(sc == null) {
			return null;
		}
		return sc.name;
	}
	
	private SideContentView createSideView(DivExRoot root)
	{
		SideContentView view = new SideContentView();
		
		class NewButtonDE extends ButtonDE
		{
			String url;
			public NewButtonDE(DivEx parent, String _url)
			{
				super(parent, "Add New Support Center");
				url = _url;
			}
			protected void onEvent(Event e) {
				redirect(url);
			}
		};
		view.add("Operation", new NewButtonDE(root, "scedit"));
		view.add("About", new HtmlView("This page shows a list of Support Centers that you have access to edit."));		
		return view;
	}
}