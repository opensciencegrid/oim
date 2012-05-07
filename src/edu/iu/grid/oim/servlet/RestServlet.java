package edu.iu.grid.oim.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.AuthorizationException;
import edu.iu.grid.oim.lib.StaticConfig;
import edu.iu.grid.oim.model.CertificateRequestException;
import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.HostCertificateRequestModel;
import edu.iu.grid.oim.model.db.record.CertificateRequestHostRecord;
import edu.iu.grid.oim.model.db.record.ContactRecord;


public class RestServlet extends ServletBase  {
	private static final long serialVersionUID = 1L;
    static Logger log = Logger.getLogger(RestServlet.class);  
    
	static enum Status {OK, FAILED};
    class Reply {
    	Status status = Status.OK;
    	String detail = "Nothing to report";
    	HashMap<String, String> params = new HashMap<String, String>();
    }
    
    @SuppressWarnings("serial")
	class RestException extends Exception {
    	public RestException(String message) {
    		super(message);
    	}
    	public RestException(String message, Exception e) {
    		super(message, e);
    	}
    }

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
	{
		response.setContentType("text/xml");
		Reply reply = new Reply();
		
		try {
			String action = request.getParameter("action");
			if(action.equals("host_certs_request")) {
				doHostCertsRequest(request, reply);
			} else if(action.equals("host_certs_renew")) {
				doHostCertsRenew(request, reply);
			} else if(action.equals("host_certs_retrieve")) {
				doHostCertsRetrieve(request, reply);
			} else if(action.equals("host_certs_approve")) {
				doHostCertsApprove(request, reply);
			}
		
		} catch (RestException e) {
			reply.status = Status.FAILED;
			reply.detail = e.toString();
			if(e.getMessage() != null) reply.detail += " -- " + e.getMessage();	
		} catch(AuthorizationException e) {
			reply.status = Status.FAILED;
			reply.detail = e.toString();	
		} catch(Exception e) {
			reply.status = Status.FAILED;
			reply.detail = e.toString();
			if(e.getMessage() != null) reply.detail += " -- " + e.getMessage();	
		}
		PrintWriter out = response.getWriter();
		/*
		if(reply.status.equals(Status.FAILED)) {
			response.setStatus(500);
		}
		*/
		out.write("{");
		
		out.write("\"status\": \""+reply.status.toString()+"\",");

		for(String key : reply.params.keySet()) {
			String value = reply.params.get(key);
			out.write("\""+key+"\": \""+StringEscapeUtils.escapeJavaScript(value)+"\",");
		}
		
		out.write("\"detail\": \""+StringEscapeUtils.escapeJavaScript(reply.detail)+"\"");

		out.write("}");
		
	}
 
	private void doHostCertsRequest(HttpServletRequest request, Reply reply) throws AuthorizationException, RestException {
		UserContext context = new UserContext(request);	
		Authorization auth = context.getAuthorization();
		
		String[] csrs = request.getParameterValues("csrs");
		String name, email, phone;
		if(auth.isGuest()) {
			name = request.getParameter("name");
			email = request.getParameter("email");
			phone = request.getParameter("phone");
			
			//TODO - validate
			
			if(name == null || email == null || phone == null) {
				throw new RestException("Please provide name, email, phone in order to create GOC ticket.");
			}
			context.setComment("Guest user; " + name + " submitted host certificatates request.");
		} else if(auth.isUser()){
			ContactRecord user = auth.getContact();
			name = user.name;
			email = user.primary_email;
			phone = user.primary_phone;
			
			context.setComment("OIM authenticated user; " + name + " submitted host certificatates request.");
		} else {
			throw new AuthorizationException("Sorry, you can't call this action - maybe not registered?");
		}
		
		HostCertificateRequestModel model = new HostCertificateRequestModel(context);
		CertificateRequestHostRecord rec;
		try {
			if(auth.isUser()) {
				rec = model.requestAsGuest(csrs, name, email, phone);
			} else {
				rec = model.requestAsUser(csrs,  auth.getContact());
			}
			if(rec == null) {
				throw new RestException("Failed to make request");
			}
			reply.params.put("gocticket_url", StaticConfig.conf.getProperty("url.gocticket")+"/"+rec.goc_ticket_id);
			reply.params.put("host_request_id", rec.id.toString());
		} catch (CertificateRequestException e) {
			throw new RestException("SQLException while makeing request", e);
		}
	}
	
	private void doHostCertsRenew(HttpServletRequest request, Reply reply) throws AuthorizationException {
		UserContext context = new UserContext(request);	
		//Authorization auth = context.getAuthorization();
	}
	
	private void doHostCertsRetrieve(HttpServletRequest request, Reply reply) throws AuthorizationException, RestException {
		UserContext context = new UserContext(request);	
		//Authorization auth = context.getAuthorization();
		String dirty_host_request_id = request.getParameter("host_request_id");
		Integer host_request_id = Integer.parseInt(dirty_host_request_id);
		String dirty_idx = request.getParameter("idx");
		Integer idx = Integer.parseInt(dirty_idx);
		HostCertificateRequestModel model = new HostCertificateRequestModel(context);
		try {
			CertificateRequestHostRecord rec = model.get(host_request_id);
			reply.params.put("pkcs7", model.getPkcs7(rec, idx));
		} catch (SQLException e) {
			throw new RestException("SQLException while makeing request", e);
		} catch (CertificateRequestException e) {
			throw new RestException("HostCertificateRequestException while makeing request", e);
		}
	}
	
	private void doHostCertsApprove(HttpServletRequest request, Reply reply) throws AuthorizationException, RestException {
		UserContext context = new UserContext(request);	
		//Authorization auth = context.getAuthorization();
		
		String dirty_host_request_id = request.getParameter("host_request_id");
		Integer host_request_id = Integer.parseInt(dirty_host_request_id);
		HostCertificateRequestModel model = new HostCertificateRequestModel(context);
	
		try {
			CertificateRequestHostRecord rec = model.get(host_request_id);
			if(model.canApprove(rec)) {
				model.approve(rec);
			} else {
				throw new AuthorizationException("You can't approve this request");
			}
		} catch (SQLException e) {
			throw new RestException("SQLException while makeing request", e);
		} catch (CertificateRequestException e) {
			throw new RestException("HostCertificateRequestException while makeing request", e);
		}
	}
}