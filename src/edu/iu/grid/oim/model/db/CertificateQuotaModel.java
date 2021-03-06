package edu.iu.grid.oim.model.db;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.record.ContactRecord;

public class CertificateQuotaModel {
	static Logger log = Logger.getLogger(CertificateRequestModelBase.class);  
	private UserContext context;
	
    public CertificateQuotaModel(UserContext context) {
		this.context = context;
	}
    
    public boolean canRequestUserCert(Integer requester_contact_id) {
		ConfigModel config = new ConfigModel(context);
		Integer global_max = config.QuotaGlobalUserCertYearMax.getInteger();
		Integer global_count = config.QuotaGlobalUserCertYearCount.getInteger();		
		
		//reached global max?
		if(global_count >= global_max) return false;
		
		//need to check requester counter?
		if(requester_contact_id != null) {
			//reached user max?
			try {
				ContactModel cmodel = new ContactModel(context);
				ContactRecord user = cmodel.get(requester_contact_id);
				Integer user_max = config.QuotaUserCertYearMax.getInteger();
				if(user.count_usercert_year >= user_max) return false;
			} catch (SQLException e) {
				log.error("Failed to obtain contact information");
				return false;
			}
		}
		return true;
    }
    
    public void incrementUserCertRequest(Integer requester_contact_id) throws SQLException {
		//increment global count
		ConfigModel config = new ConfigModel(context);
		Integer global_count = config.QuotaGlobalUserCertYearCount.getInteger();		
		global_count++;
		config.QuotaGlobalUserCertYearCount.set(global_count.toString());
		
		//increment global total count(won't get reset every year)
		Integer global_total_count = config.QuotaGlobalUserCertTotalCount.getInteger();		
		global_total_count++;
		config.QuotaGlobalUserCertTotalCount.set(global_total_count.toString());	
		
		if(requester_contact_id != null) {
			//increment user count too
			ContactModel cmodel = new ContactModel(context);
			ContactRecord user = cmodel.get(requester_contact_id);
			user.count_usercert_year++;
			cmodel.emptyCache();//force next get() to pull from the DB instead of cache - which I just updated..
			cmodel.update(cmodel.get(user), user);
		}
    }
    
    public boolean canApproveHostCert(int count) {
		//reached global max?
		ConfigModel config = new ConfigModel(context);
		Integer global_max = config.QuotaGlobalHostCertYearMax.getInteger();
		Integer global_count = config.QuotaGlobalHostCertYearCount.getInteger();		
		if(global_count + count > global_max) return false;   	
		
		//reached personal day max?
		Authorization auth = context.getAuthorization();
		ContactRecord user = auth.getContact();
		Integer day_max = config.QuotaUserHostDayMax.getInteger();
		if(user.count_hostcert_day + count > day_max) return false;
		
		//reached personal year max?
		Integer year_max = config.QuotaUserHostYearMax.getInteger();
		if(user.count_hostcert_year + count > year_max) return false;
		
		return true;
    }
    
    public void incrementHostCertApproval(int inc) throws SQLException {
		//increment global count
		ConfigModel config = new ConfigModel(context);
		Integer global_count = config.QuotaGlobalHostCertYearCount.getInteger();		
		global_count+=inc;
		config.QuotaGlobalHostCertYearCount.set(global_count.toString());
		
		//increment global total count(won't get reset every year)
		Integer global_total_count = config.QuotaGlobalHostCertTotalCount.getInteger();		
		global_total_count+=inc;
		config.QuotaGlobalHostCertTotalCount.set(global_total_count.toString());	
		
		//increment user count
		Authorization auth = context.getAuthorization();
		if(auth.isUser()) {
			ContactRecord user = auth.getContact();
			user.count_hostcert_year+=inc;
			user.count_hostcert_day+=inc;
			ContactModel cmodel = new ContactModel(context);
			cmodel.emptyCache();//force next get() to pull from the DB instead of cache - which I just updated..
			cmodel.update(cmodel.get(user), user);
		}
    }
}
