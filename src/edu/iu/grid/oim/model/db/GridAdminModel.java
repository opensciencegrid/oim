package edu.iu.grid.oim.model.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.GridAdminRecord;
import edu.iu.grid.oim.model.db.record.RecordBase;
import edu.iu.grid.oim.model.db.record.ResourceContactRecord;
import edu.iu.grid.oim.model.db.record.VORecord;

public class GridAdminModel extends SmallTableModelBase<GridAdminRecord> {
    static Logger log = Logger.getLogger(GridAdminModel.class);  
    
    public GridAdminModel(UserContext context) 
    {
    	super(context, "grid_admin");
    }
    
    GridAdminRecord createRecord() throws SQLException
	{
		return new GridAdminRecord();
	}
    /*
	public ArrayList<GridAdminRecord> getAll() throws SQLException
	{
		ArrayList<GridAdminRecord> list = new ArrayList<GridAdminRecord>();
		for(RecordBase it : getCache()) {
			list.add((GridAdminRecord)it);
		}
		return list;
	}
	*/
    
    //group records by domain name
    public LinkedHashMap<String, ArrayList<GridAdminRecord>> getAll() throws SQLException
    {
    	//ContactModel cmodel = new ContactModel(context);
    	LinkedHashMap<String, ArrayList<GridAdminRecord>> list = new LinkedHashMap<String, ArrayList<GridAdminRecord>>();
		for(RecordBase it : getCache()) {
			GridAdminRecord rec = (GridAdminRecord)it;
			if(list.containsKey(rec.domain)) {
				ArrayList<GridAdminRecord> clist = list.get(rec.domain);
				clist.add(rec);
			} else {
				ArrayList<GridAdminRecord> clist = new ArrayList<GridAdminRecord>();
				clist.add(rec);
				list.put(rec.domain, clist);
			}
		}
    	
    	return list;
    }
    
	public ArrayList<GridAdminRecord> getByDomain(String domain) throws SQLException
	{ 
		ArrayList<GridAdminRecord> list = new ArrayList<GridAdminRecord>();
		for(RecordBase rec : getCache()) {
			GridAdminRecord vcrec = (GridAdminRecord)rec;
			if(vcrec.domain.equals(domain)) list.add(vcrec);
		}
		return list;
	}
	
	public HashMap<VORecord, ArrayList<GridAdminRecord>> getByDomainGroupedByVO(String domain) throws SQLException
	{ 
		VOModel vmodel = new VOModel(context);
		HashMap<VORecord, ArrayList<GridAdminRecord>> groups = new HashMap<VORecord, ArrayList<GridAdminRecord>>();
		for(GridAdminRecord rec : getByDomain(domain)) {
			VORecord vo = vmodel.get(rec.vo_id);
			if(!groups.keySet().contains(vo)) {
				//create new group if it doesn't exist yet
				groups.put(vo, new ArrayList<GridAdminRecord>());
			}
			ArrayList<GridAdminRecord> list = groups.get(vo);
			list.add(rec);
		}
		return groups;
	}
	
	/*
	public ArrayList<ContactRecord> getContactsByDomainAndVO(String domain, Integer vo_id) throws SQLException
	{ 
		ArrayList<ContactRecord> list = new ArrayList<ContactRecord>();
		ArrayList<GridAdminRecord> recs = getByDomain(domain);
    	ContactModel cmodel = new ContactModel(context);
		for(GridAdminRecord rec : recs) {
			if(rec.vo_id.equals(vo_id)) {
				list.add(cmodel.get(rec.contact_id));
			}
		}
		return list;
	}
	*/
	/*
	public GridAdminRecord get(int id) throws SQLException {
		GridAdminRecord keyrec = new GridAdminRecord();
		keyrec.id = id;
		return get(keyrec);
	}
	*/
	
	//search for gridadmin with most specific domain name registered for given fqdn.
	//return null if not found
	public String getDomainByFQDN(String fqdn) throws SQLException {
		String domain = null;
		LinkedHashMap<String, ArrayList<GridAdminRecord>> list = getAll();
		for(String rec_domain : list.keySet()) {
			if(fqdn.endsWith(rec_domain)) {
				//keep - if we find more specific domain
				if(domain == null || domain.length() < rec_domain.length()) {
					domain = rec_domain;
				} 
			}
		}
		return domain;
	}
	
    public String getName()
    {
    	return "GridAdmin";
    }
	public Boolean hasLogAccess(XPath xpath, Document doc) throws XPathExpressionException
	{
		if(auth.allows("admin")) {
			return true;
		}
		return false;
	}

	public ArrayList<GridAdminRecord> getGridAdminsByContactID(Integer id) throws SQLException {
		ArrayList<GridAdminRecord> list = new ArrayList<GridAdminRecord>();
		for(RecordBase it : getCache()) {
			GridAdminRecord rec = (GridAdminRecord)it;
			if(rec.contact_id.equals(id)) {
				list.add(rec);
			}
		}
		return list;
	}
}