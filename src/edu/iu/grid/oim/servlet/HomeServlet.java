package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.webif.divrep.DivRepRoot;

import edu.iu.grid.oim.lib.Config;
import edu.iu.grid.oim.model.MenuItem;
import edu.iu.grid.oim.view.ContentView;
import edu.iu.grid.oim.view.HtmlView;
import edu.iu.grid.oim.view.MenuView;
import edu.iu.grid.oim.view.Page;
import edu.iu.grid.oim.view.IView;
import edu.iu.grid.oim.view.SideContentView;

public class HomeServlet extends ServletBase  {
	private static final long serialVersionUID = 1L;
    static Logger log = Logger.getLogger(HomeServlet.class);  
    
    public HomeServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		//setContext(request);
		
		MenuView menuview = new MenuView(context, "home");
		ContentView contentview;
		/*
		if(auth.getUserDN() == null || auth.getUserCN() == null) {
			//User didn't login with valid certificate
			response.sendRedirect(Config.getApplicationBase() + "/needcert");
		} else {
					//all good.
		 */
			contentview = createContentView();
			Page page = new Page(menuview, contentview, new SideContentView());
			page.render(response.getWriter());	
		//}
	}
	
	protected ContentView createContentView()
	{
		ContentView contentview = new ContentView();
		
		contentview.add(new HtmlView("<h1>OIM Home</h1>"));

		// TODO agopu: need to clean this up with some divs etc. Nicer font, etc.
		String welcome_string = "<p>Welcome to the OSG Information Management System.</p>";

		if(auth.isGuest()) {
			welcome_string += "<p>Please provide a DOE certificate via your web browser in order to use this system.</p>";
		} else {
			welcome_string += "<p>In the menu along the top, you will find options for registering or updating information for various OSG entities.</p>";
				//hayashis: this is not correct - as soon as anybody register their DN, they can create / edit stuff
				//"<p>If you are registering your personal information with us for the first time, you will need to wait for the OIM administrators to activate your membership before you are able to register or update Virtual Organization, Support Center or Resource information.</p>"+

				//hayashis: do we really need to say this?
				//"<p>For Standard Operating Procedures, Registration Instructions, and OIM Definitions for entering OIM data please see the Help Menu.</p>"; 
		}
		welcome_string += "<p>Please see Help page for more information.";
		contentview.add(new HtmlView(welcome_string));
		
		return contentview;
	}
}
