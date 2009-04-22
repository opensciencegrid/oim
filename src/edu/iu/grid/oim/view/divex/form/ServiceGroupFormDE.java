package edu.iu.grid.oim.view.divex.form;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.webif.divex.DivEx;
import com.webif.divex.form.FormDE;
import com.webif.divex.StaticDE;
import com.webif.divex.form.CheckBoxFormElementDE;
import com.webif.divex.form.SelectFormElementDE;
import com.webif.divex.form.TextAreaFormElementDE;
import com.webif.divex.form.TextFormElementDE;
import com.webif.divex.form.validator.UniqueValidator;
import com.webif.divex.form.validator.UrlValidator;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.Authorization.AuthorizationException;
import edu.iu.grid.oim.model.db.ContactTypeModel;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.OsgGridTypeModel;
import edu.iu.grid.oim.model.db.ResourceAliasModel;
import edu.iu.grid.oim.model.db.ResourceContactModel;
import edu.iu.grid.oim.model.db.ResourceGroupModel;
import edu.iu.grid.oim.model.db.ResourceServiceModel;
import edu.iu.grid.oim.model.db.ServiceGroupModel;
import edu.iu.grid.oim.model.db.ServiceModel;
import edu.iu.grid.oim.model.db.ResourceModel;
import edu.iu.grid.oim.model.db.SiteModel;
import edu.iu.grid.oim.model.db.record.ContactTypeRecord;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.OsgGridTypeRecord;
import edu.iu.grid.oim.model.db.record.ResourceAliasRecord;
import edu.iu.grid.oim.model.db.record.ResourceContactRecord;
import edu.iu.grid.oim.model.db.record.ResourceGroupRecord;
import edu.iu.grid.oim.model.db.record.ResourceRecord;
import edu.iu.grid.oim.model.db.record.ResourceServiceRecord;
import edu.iu.grid.oim.model.db.record.ServiceGroupRecord;
import edu.iu.grid.oim.model.db.record.SiteRecord;
import edu.iu.grid.oim.view.divex.ContactEditorDE;
import edu.iu.grid.oim.view.divex.OIMHierarchySelector;
import edu.iu.grid.oim.view.divex.ResourceAliasDE;
import edu.iu.grid.oim.view.divex.ResourceServicesDE;

public class ServiceGroupFormDE extends FormDE 
{
    static Logger log = Logger.getLogger(ServiceGroupFormDE.class); 
   
	protected Authorization auth;
	private Integer id;
	
	private TextFormElementDE name;
	private TextAreaFormElementDE description;
	
	public ServiceGroupFormDE(DivEx parent, ServiceGroupRecord rec, String origin_url, Authorization _auth) throws AuthorizationException, SQLException
	{	
		super(parent, origin_url);
		auth = _auth;
		
		id = rec.id;
		
		new StaticDE(this, "<h2>Details</h2>");
		
		//pull vos for unique validator
		HashMap<Integer, String> resource_groups = getResourceGroups();
		if(id != null) {
			//if doing update, remove my own name (I can use my own name)
			resource_groups.remove(id);
		}
		
		name = new TextFormElementDE(this);
		name.setLabel("Name");
		name.setValue(rec.name);
		name.addValidator(new UniqueValidator<String>(resource_groups.values()));
		name.setRequired(true);
		
		description = new TextAreaFormElementDE(this);
		description.setLabel("Description");
		description.setValue(rec.description);
		description.setRequired(true);
	}
	
	protected Boolean doSubmit() {
		
		//Construct VORecord
		ServiceGroupRecord rec = new ServiceGroupRecord();
		rec.id = id;
	
		rec.name = name.getValue();
		rec.description = description.getValue();
		
		ServiceGroupModel model = new ServiceGroupModel(auth);
		try {
			if(rec.id == null) {
				model.insert(rec);
			} else {
				model.update(model.get(rec), rec);
			}
		} catch (Exception e) {
			alert(e.getMessage());
			return false;
		}
		return true;
	}
	
	private HashMap<Integer, String> getResourceGroups() throws SQLException
	{
		ResourceGroupModel model = new ResourceGroupModel(auth);
		HashMap<Integer, String> resource_groups = new HashMap();
		for(ResourceGroupRecord rec : model.getAll()) {
			resource_groups.put(rec.id, rec.name);
		}
		return resource_groups;
	}
}