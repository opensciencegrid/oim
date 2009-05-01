package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.webif.divex.ButtonDE;
import com.webif.divex.DivEx;
import com.webif.divex.DivExRoot;
import com.webif.divex.Event;
import com.webif.divex.EventListener;
import com.webif.divex.form.FormDE;
import com.webif.divex.form.TextFormElementDE;
import com.webif.divex.form.validator.IFormElementValidator;
import com.webif.divex.form.validator.UniqueValidator;

import edu.iu.grid.oim.lib.Config;
import edu.iu.grid.oim.model.MenuItem;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.DNAuthorizationTypeModel;
import edu.iu.grid.oim.model.db.DNModel;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.DNAuthorizationTypeRecord;
import edu.iu.grid.oim.model.db.record.DNRecord;
import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.DivExWrapper;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.MenuView;
import edu.iu.grid.oim.view.Page;
import edu.iu.grid.oim.view.IView;
import edu.iu.grid.oim.view.SideContentView;

public class RegisterServlet extends ServletBase  {
	private static final long serialVersionUID = 1L;
    static Logger log = Logger.getLogger(RegisterServlet.class);  
    private String origin_url = Config.getApplicationBase() + "/home";
    
    public RegisterServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		setContext(request);
		
		MenuView menuview = new MenuView(context, "register");
		ContentView contentview = createContentView();
		Page page = new Page(menuview, contentview, new SideContentView());
		page.render(response.getWriter());	
	}
	
	protected ContentView createContentView()
	{
		ContentView contentview = new ContentView();
		
		contentview.add(new HtmlView("<h1>OIM Registration</h1>"));
		
		WizardDE wizard = new WizardDE(context.getDivExRoot());
		wizard.setPage(new GreetingPage(wizard));
		contentview.add(new DivExWrapper(wizard));
		
		return contentview;
	}

	interface IWizardPage {
		public void render(PrintWriter out);	
	}
	class WizardDE extends DivEx
	{
		IWizardPage currentpage = null;
		protected void onEvent(Event e) {
			// TODO Auto-generated method stub
			
		}
		
		public WizardDE(DivEx _parent) {
			super(_parent);
		}
		public void setPage(IWizardPage page)
		{
			currentpage = page;
			redraw();
		}

		@Override
		public void render(PrintWriter out) {
			out.write("<div id=\""+getNodeID()+"\">");
			currentpage.render(out);
			out.write("</div>");
		}
	}
	class GreetingPage extends DivEx implements IWizardPage
 	{
		ButtonDE button;
		WizardDE wizard;
		public GreetingPage(WizardDE _wizard) {
			super(_wizard);
			wizard = _wizard;
			button = new ButtonDE(this, "Register");
			button.addEventListener(new EventListener() {

				@Override
				public void handleEvent(Event e) {
					wizard.setPage(new EnterEmailPage(wizard));
				}});
		}
		
		public void render(PrintWriter out) {
			out.write("<p>Your DN ("+auth.getUserDN()+") is not yet registered at OIM. </p>");
			out.write("<p>If you'd like to register now, please click register button.</p>");		
			button.render(out);
		}

		protected void onEvent(Event e) {
			// TODO Auto-generated method stub
			
		}
	}
	class EnterEmailPage extends FormDE implements IWizardPage
 	{
		WizardDE wizard;
		private TextFormElementDE email;
		private TextFormElementDE email_check;
		private TextFormElementDE phone;
		
		public EnterEmailPage(WizardDE _wizard) {
			super(_wizard, origin_url);
			wizard = _wizard;
			
			email = new TextFormElementDE(this);
			email.setLabel("Your Email");
			email.setRequired(true);
			
			class CheckValidator implements IFormElementValidator
			{
				TextFormElementDE other;
				public CheckValidator(TextFormElementDE _other) {
					other = _other;
				}
				public String getMessage() {
					return "Email address doesn't match.";
				}
				public Boolean isValid(Object value) {
					if(other.getValue() == null) return false;
					return other.getValue().equals((String)value);
				}
			}
			
			email_check = new TextFormElementDE(this);
			email_check.setLabel("Your Email (Again)");
			email_check.setRequired(true);
			email_check.addValidator(new CheckValidator(email));
			
			phone = new TextFormElementDE(this);
			phone.setLabel("Your Phone Number");
			phone.setRequired(true);
		}

		protected void onEvent(Event e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		protected Boolean doSubmit() {
			ContactModel model = new ContactModel(context);
			DNModel dnmodel = new DNModel(context);
			try {
				//Find contact record with the same email address
				ContactRecord rec = model.getByemail(email.getValue());
				//Create new one if none is found
				if(rec == null) {
					rec = new ContactRecord();
					rec.primary_email = email.getValue();
					rec.primary_phone = phone.getValue();
					rec.person = true;
					rec.active = true;
					rec.disable = false;
					model.insert(rec);
				} else {
					//Make sure that this contact is not used by any DN already
					if(dnmodel.getByContactID(rec.id) != null) {
						alert("The email address specified is already associated with a different DN. Please try different email address.");
						email.setValue("");
						email_check.setValue("");
						return false;
					}
				}
				
				//Then insert a new DN record
				DNRecord dnrec = new DNRecord();
				dnrec.contact_id = rec.id;
				dnrec.dn_string = auth.getUserDN();
				dnmodel.insert(dnrec);
				
				//Make him OSG end user access
				DNAuthorizationTypeModel dnauthmodel = new DNAuthorizationTypeModel(context);
				DNAuthorizationTypeRecord dnauthrec = new DNAuthorizationTypeRecord();
				dnauthrec.dn_id = dnrec.id;
				dnauthrec.authorization_type_id = 1; //OSG End User
				dnauthmodel.insert(dnauthrec);
				
				//jump to profile page
				redirect(Config.getApplicationBase()+"/profileedit");
				
			} catch (SQLException e) {
				alert(e.toString());
				redirect(origin_url);
			}
			
			return false;
		}
	}
}
