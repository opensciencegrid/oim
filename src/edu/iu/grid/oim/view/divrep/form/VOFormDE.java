package edu.iu.grid.oim.view.divrep.form;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.divrep.DivRep;
import com.divrep.DivRepEvent;
import com.divrep.DivRepEventListener;
import com.divrep.common.DivRepButton;
import com.divrep.common.DivRepCheckBox;
import com.divrep.common.DivRepForm;
import com.divrep.common.DivRepFormElement;
import com.divrep.common.DivRepSelectBox;
import com.divrep.common.DivRepStaticContent;
import com.divrep.common.DivRepTextArea;
import com.divrep.common.DivRepTextBox;
import com.divrep.validator.DivRepUniqueValidator;
import com.divrep.validator.DivRepUrlValidator;

import edu.iu.grid.oim.lib.Authorization;
import edu.iu.grid.oim.lib.Footprints;
import edu.iu.grid.oim.lib.AuthorizationException;
import edu.iu.grid.oim.model.UserContext;
import edu.iu.grid.oim.model.UserContext.MessageType;
import edu.iu.grid.oim.model.db.ContactTypeModel;
import edu.iu.grid.oim.model.db.ContactModel;
import edu.iu.grid.oim.model.db.FieldOfScienceModel;
import edu.iu.grid.oim.model.db.VOOasisUserModel;
import edu.iu.grid.oim.model.db.VOReportContactModel;
import edu.iu.grid.oim.model.db.VOReportNameModel;
import edu.iu.grid.oim.model.db.VOReportNameFqanModel;
import edu.iu.grid.oim.model.db.SCModel;
import edu.iu.grid.oim.model.db.VOContactModel;
import edu.iu.grid.oim.model.db.VOFieldOfScienceModel;
import edu.iu.grid.oim.model.db.VOModel;
import edu.iu.grid.oim.model.db.record.ContactTypeRecord;
import edu.iu.grid.oim.model.db.record.ContactRecord;
import edu.iu.grid.oim.model.db.record.FieldOfScienceRecord;

import edu.iu.grid.oim.model.db.record.VOOasisUserRecord;
import edu.iu.grid.oim.model.db.record.VOReportContactRecord;
import edu.iu.grid.oim.model.db.record.VOReportNameRecord;
import edu.iu.grid.oim.model.db.record.VOReportNameFqanRecord;
import edu.iu.grid.oim.model.db.record.SCRecord;
import edu.iu.grid.oim.model.db.record.VOContactRecord;
import edu.iu.grid.oim.model.db.record.VOFieldOfScienceRecord;
import edu.iu.grid.oim.model.db.record.VORecord;
import edu.iu.grid.oim.view.ToolTip;
import edu.iu.grid.oim.view.divrep.AUPConfirmation;
import edu.iu.grid.oim.view.divrep.Confirmation;
import edu.iu.grid.oim.view.divrep.ContactEditor;
import edu.iu.grid.oim.view.divrep.VOReportNames;
import edu.iu.grid.oim.view.divrep.ContactEditor.Rank;
import edu.iu.grid.oim.view.divrep.FieldOfScience;

public class VOFormDE extends DivRepForm 
{
    static Logger log = Logger.getLogger(VOFormDE.class); 
   
    private UserContext context;
	private Authorization auth;
	private Integer id;
	
	private DivRepTextBox name;
	private DivRepTextBox long_name;
	private DivRepTextArea description;
	private DivRepTextArea community;
	private DivRepSelectBox sc_id;
	private DivRepCheckBox active;
	private DivRepCheckBox disable;
	private DivRepCheckBox child_vo;
	private DivRepSelectBox parent_vo;

	private Confirmation confirmation;
	private DivRepTextArea comment;

	static public ArrayList<ContactTypeRecord.Info> ContactTypes;
	static {
		ContactTypes = new ArrayList<ContactTypeRecord.Info>();
		ContactTypes.add(new ContactTypeRecord.Info(1, "A contact who has registered this virtual organization"));
		ContactTypes.add(new ContactTypeRecord.Info(6, "Contacts who decides on what virtual organizations are allowed to run on VO-owned resources, who are users of this virtual organization, etc"));
		ContactTypes.add(new ContactTypeRecord.Info(3, "Contacts for ticketing and assorted issues. This is typically a user/application support person or a help desk"));
		ContactTypes.add(new ContactTypeRecord.Info(2, "Security notifications sent out by the OSG security team are sent to primary and secondary virtual organization security contacts"));
		ContactTypes.add(new ContactTypeRecord.Info(5, "Contacts who do not fall under any of the above types but would like to be able to edit this virtual organization can be added as miscellaneous contact"));
		ContactTypes.add(new ContactTypeRecord.Info(11, "RA (Registration Authority) agent who can approve user certificate requests."));
	}
	
	private HashMap<Integer, ContactEditor> contact_editors = new HashMap();

	// Moving fields related to only VOs that do actual research, apart 
	//  from providing services on their facility to a separate area that
	//  that can be hidden -agopu 2010-05-31
	private DivRepCheckBox science_vo;
	private ScienceVOInfo science_vo_info;
	
	private DivRepTextArea app_description;
	private VOReportNames vo_report_name_div;
	
	private FieldOfScience field_of_science_de;
	
	private URLs urls;
	private DivRepTextBox primary_url; // Moved out of URLs class to enable direct property manipulation
	private DivRepTextBox aup_url;
	private DivRepTextBox membership_services_url;
	private DivRepTextBox purpose_url;
	private DivRepTextBox support_url;	
	
	private DivRepCheckBox use_oasis;
	private ContactEditor oasis_users;

	class ScienceVOInfo extends DivRepFormElement
	{
		protected void onEvent(DivRepEvent e) {
			// TODO Auto-generated method stub
			
		}	
		
		ScienceVOInfo(DivRep _parent, VORecord rec) {
			super(_parent);

			//new DivRepStaticContent(this, "<div class=\"indent\">");
			//new DivRepStaticContent(this, "<h3>More Extended Descriptions including URLs</h3>");

			app_description = new DivRepTextArea(this);
			app_description.setLabel("Enter an Application Description");
			app_description.setValue(rec.app_description);
			app_description.setSampleValue("CDF Analysis jobs will be run");

			urls = new URLs(this, rec);

			// Fields of Science
			try {
				ArrayList<Integer> selected = new ArrayList<Integer>();
				//select currently selected field of science
				if(rec.id != null) {
					VOFieldOfScienceModel vofsmodel = new VOFieldOfScienceModel(context);
					for(VOFieldOfScienceRecord fsrec : vofsmodel.getByVOID(rec.id)) {
						selected.add(fsrec.field_of_science_id);
					}
				}
				field_of_science_de = new FieldOfScience(this, context, selected);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// Handle reporting names
			new DivRepStaticContent(this, "<h3>Reporting Names for your VO</h3>");
			new DivRepStaticContent(this, "<p>This section allows you to define report names for this VO. These report names are used by the Gratia Accounting software to organize periodic usage accounting reports. You need to define at least one report name -- for example, one with the same name as the VO (short) name and no FQANs. Large VOs with several sub-groups can define different report names, and also one or more FQAN per report name. Contact gratia-operation@opensciencegrid.org if you have any questions about VO report names.</p>");
			ContactModel cmodel = new ContactModel (context);
			VOReportNameModel vorepname_model = new VOReportNameModel(context);
			VOReportNameFqanModel vorepnamefqan_model = new VOReportNameFqanModel(context);

			ArrayList<VOReportNameRecord> vorepname_records;
			try {
				vorepname_records = vorepname_model.getAll();
				vo_report_name_div = new VOReportNames(this, vorepname_records, cmodel);
				if(id != null) {
					for(VOReportNameRecord vorepname_rec : vorepname_model.getAllByVOID(id)) {
						VOReportContactModel vorcmodel = new VOReportContactModel(context);
						Collection<VOReportContactRecord> vorc_list = vorcmodel.getAllByVOReportNameID(vorepname_rec.id);
						Collection<VOReportNameFqanRecord> vorepnamefqan_list = vorepnamefqan_model.getAllByVOReportNameID(vorepname_rec.id);
						vo_report_name_div.addVOReportName(vorepname_rec, vorepnamefqan_list, vorc_list);
					}
				} else {
					//add new one by default
					vo_report_name_div.addVOReportName(
							new VOReportNameRecord(), 
							new ArrayList<VOReportNameFqanRecord>(), 
							new ArrayList<VOReportContactRecord>()
					);		
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			//new DivRepStaticContent(this, "</div>");
		}
		
		public void render(PrintWriter out) {
			out.print("<div id=\""+getNodeID()+"\">");	
			if(!isHidden()) {
				for(DivRep child : childnodes) {
					if(child instanceof DivRepFormElement) {
						out.print("<div class=\"divrep_form_element\">");
						child.render(out);
						out.print("</div>");
					
					} else {
						//non form element..
						child.render(out);
					}
				}
				error.render(out);
			}
			out.print("</div>");
		}
	}

	class URLs extends DivRepFormElement
	{
		public URLs(DivRep _parent, VORecord rec) {
			super(_parent);
			
			new DivRepStaticContent(this, "<h2>Relevant URLs</h2>");
			primary_url = new DivRepTextBox(this);
			primary_url.setLabel("Primary URL");
			primary_url.setValue(rec.primary_url);
			primary_url.addValidator(DivRepUrlValidator.getInstance());
			// primary_url.setRequired(true);
			primary_url.setSampleValue("http://www-cdf.fnal.gov");

			aup_url = new DivRepTextBox(this);
			aup_url.setLabel("AUP URL");
			aup_url.setValue(rec.aup_url);
			aup_url.addValidator(DivRepUrlValidator.getInstance());
			// aup_url.setRequired(true);
			aup_url.setSampleValue("http://www-cdf.fnal.gov");

			membership_services_url = new DivRepTextBox(this);
			membership_services_url.setLabel("Membership Services (VOMS) URL");
			membership_services_url.setValue(rec.membership_services_url);
			membership_services_url.addValidator(DivRepUrlValidator.getInstance());
			// membership_services_url.setRequired(true);
			membership_services_url.setSampleValue("https://voms.fnal.gov:8443/voms/cdf/");

			purpose_url = new DivRepTextBox(this);
			purpose_url.setLabel("Purpose URL"); 
			purpose_url.setValue(rec.purpose_url);
			purpose_url.addValidator(DivRepUrlValidator.getInstance());
			// purpose_url.setRequired(true);
			purpose_url.setSampleValue("http://www-cdf.fnal.gov");

			support_url = new DivRepTextBox(this);
			support_url.setLabel("Support URL"); 
			support_url.setValue(rec.support_url);
			support_url.addValidator(DivRepUrlValidator.getInstance());
			// support_url.setRequired(true);
			support_url.setSampleValue("http://cdfcaf.fnal.gov");
		}
		
		protected void onEvent(DivRepEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void render(PrintWriter out) {
			out.write("<div id=\""+getNodeID()+"\">");
			primary_url.render(out);
			aup_url.render(out);
			membership_services_url.render(out);
			purpose_url.render(out);
			support_url.render(out);
			out.write("<br/></div>");
		}
	}

	public void showHideScienceVODetail()
	{
		Boolean required = science_vo.getValue();

		app_description.setRequired(required);
		primary_url.setRequired(required);
		field_of_science_de.setRequired(required);
		
		science_vo_info.setHidden(!required);
		science_vo_info.redraw();
	}
	
	public void showHideOasisUsers() {
		Boolean required = use_oasis.getValue();
		oasis_users.setHidden(!required);
		oasis_users.redraw();
	}
	
	public VOFormDE(UserContext _context, VORecord rec, String origin_url) throws AuthorizationException, SQLException
	{	
		super(_context.getPageRoot(), origin_url);
		context = _context;
		auth = context.getAuthorization();
		id = rec.id;
		
		new DivRepStaticContent(this, "<h2>Basic VO Information</h2>");
		
		//pull vos for unique validator
		LinkedHashMap<Integer, String> vos = getVONames();
		if(id != null) { //if doing update, remove my own name (I can't use my own name)
			vos.remove(id);
		}
		name = new DivRepTextBox(this);
		name.setLabel("Name");
		name.setValue(rec.name);
		name.addValidator(new DivRepUniqueValidator<String>(vos.values()));
		name.setRequired(true);
		name.setSampleValue("CDF");

		long_name = new DivRepTextBox(this);
		long_name.setLabel("Enter the Long Name for this VO");
		long_name.setValue(rec.long_name);
		long_name.setRequired(true); // TODO: agopu should this be required?
		long_name.setSampleValue("Collider Detector at Fermilab");

		sc_id = new DivRepSelectBox(this, getSCNames());
		sc_id.setLabel("Select a Support Center that supports your users and applications");
		sc_id.setValue(rec.sc_id);
		sc_id.setRequired(true);
		
		//new DivRepStaticContent(this, "<h2>Sub-VO Mapping</h2>");
		//new DivRepStaticContent(this, "<p>Check if this VO is a sub-VO of an existing VO. For example, FermilabMinos is a sub VO of the Fermilab VO.</p>");
		child_vo = new DivRepCheckBox(this);
		child_vo.setLabel("This is a sub-VO of an existing VO (ex. FermilabMinos is a sub VO of the Fermilab VO)");

		parent_vo = new DivRepSelectBox(this, vos);
		parent_vo.setLabel("Select a Parent VO");
		parent_vo.addClass("indent");
		hideParentVOSelector(true);
		child_vo.addEventListener(new DivRepEventListener() {
			public void handleEvent(DivRepEvent e) {	
				if(((String)e.value).compareTo("true") == 0) {
					hideParentVOSelector(false);
				} else {
					hideParentVOSelector(true);
				}
			}
		});
		if(id != null) {
			VOModel model = new VOModel(context);
			VORecord parent_vo_rec = model.getParentVO(id);
			if(parent_vo_rec != null) {
				parent_vo.setValue(parent_vo_rec.id);
				child_vo.setValue(true);
				hideParentVOSelector(false);				
			}
			// AG: Need to clean this up; especially for VOs that are not child VOs of a parent
			// .. perhaps a yes/no first?
		}
		parent_vo.addEventListener(new DivRepEventListener () {
			public void handleEvent(DivRepEvent e) {
				handleParentVOSelection(Integer.parseInt((String)e.value));
			}
		});		

		description = new DivRepTextArea(this);
		description.setLabel("Enter a Description for this VO");
		description.setValue(rec.description);
		description.setRequired(true);
		description.setSampleValue("Collider Detector at Fermilab");

		community = new DivRepTextArea(this);
		community.setLabel("Describe the Community this VO serves");
		community.setValue(rec.community);
		community.setRequired(true);
		community.setSampleValue("The Collider Detector at Fermilab (CDF) experimental collaboration is committed to studying high energy particle collisions");
		
		new DivRepStaticContent(this, "<h2>Additional Information for VOs that include OSG Users</h2>");
		
		///////////////////////////////////////////////////////////////////////////////////////////
		ToolTip tip = new ToolTip("Uncheck this checkbox if your VO does not intend to use any OSG resources, and just wants to provide services to the OSG.");
		new DivRepStaticContent(this, "<span class=\"right\">"+tip.render()+"</span>");
		science_vo = new DivRepCheckBox(this);
		science_vo.setLabel("This VO has users who do OSG-dependent scientific research.");
		science_vo.setValue(rec.science_vo);
		
		science_vo_info = new ScienceVOInfo(this, rec);
		science_vo.addEventListener(new DivRepEventListener() {
			public void handleEvent(DivRepEvent e) {
				showHideScienceVODetail();
			}
		});
		// New VO addition attempt - we want the checkbox checked by default for new VO additions
		if(rec.id == null) { 
			science_vo.setValue(true);
		}
		showHideScienceVODetail();

		///////////////////////////////////////////////////////////////////////////////////////////
		new DivRepStaticContent(this, "<h2>OASIS Information</h2>");
		use_oasis = new DivRepCheckBox(this);
		use_oasis.setLabel("OASIS Enabled");
		use_oasis.setValue(rec.use_oasis);
		use_oasis.addEventListener(new DivRepEventListener() {
			public void handleEvent(DivRepEvent e) {
				showHideOasisUsers();
			}
		});
		ArrayList<VOOasisUserRecord> users = null;
		if(rec.id != null) {
			VOOasisUserModel vooumodel = new VOOasisUserModel(context);
			users = vooumodel.getByVOID(rec.id);
		}
		oasis_users = createOASISUserEditor(users);
		showHideOasisUsers();
		
		///////////////////////////////////////////////////////////////////////////////////////////
		new DivRepStaticContent(this, "<h2>Contact Information</h2>");
		HashMap<Integer/*contact_type_id*/, ArrayList<VOContactRecord>> voclist_grouped = null;
		if(id != null) {
			VOContactModel vocmodel = new VOContactModel(context);
			ArrayList<VOContactRecord> voclist = vocmodel.getByVOID(id);
			voclist_grouped = vocmodel.groupByContactTypeID(voclist);
		} else {
			//set user's contact as submitter
			voclist_grouped = new HashMap<Integer, ArrayList<VOContactRecord>>();

			ArrayList<VOContactRecord> submitter_list = new ArrayList<VOContactRecord>();
			VOContactRecord submitter = new VOContactRecord();
			submitter.contact_id = auth.getContact().id;
			submitter.contact_rank_id = 1;//primary
			submitter.contact_type_id = 1;//submitter
			submitter_list.add(submitter);
			voclist_grouped.put(1/*submitter*/, submitter_list);
			
			// Should we make a function for these steps and call it 4 times? -agopu
			ArrayList<VOContactRecord> manager_list = new ArrayList<VOContactRecord>();
			VOContactRecord manager = new VOContactRecord();
			manager.contact_id = auth.getContact().id;
			manager.contact_rank_id = 1;//primary
			manager.contact_type_id = 6;//manager
			manager_list.add(manager);
			voclist_grouped.put(6/*manager*/, manager_list);

			ArrayList<VOContactRecord> admin_contact_list = new ArrayList<VOContactRecord>();
			VOContactRecord primary_admin = new VOContactRecord();
			primary_admin.contact_id = auth.getContact().id;
			primary_admin.contact_rank_id = 1;//primary
			primary_admin.contact_type_id = 3;//admin
			admin_contact_list.add(primary_admin);
			voclist_grouped.put(3/*admin*/, admin_contact_list);
		
			ArrayList<VOContactRecord> security_contact_list = new ArrayList<VOContactRecord>();
			VOContactRecord primary_security_contact= new VOContactRecord();
			primary_security_contact.contact_id = auth.getContact().id;
			primary_security_contact.contact_rank_id = 1;//primary
			primary_security_contact.contact_type_id = 2;//security_contact
			security_contact_list.add(primary_security_contact);
			voclist_grouped.put(2/*security_contact*/, security_contact_list);
			
			ArrayList<VOContactRecord> ra_list = new ArrayList<VOContactRecord>();
			VOContactRecord ra = new VOContactRecord();
			//make Alain Deximo secondary RA
			ra.contact_id = 623;//alain deximo
			ra.contact_rank_id = 2;//secondary
			ra.contact_type_id = 11;//vo_ra
			ra_list.add(ra);
			voclist_grouped.put(11/*vo ra*/, ra_list);
		}
		ContactTypeModel ctmodel = new ContactTypeModel(context);
		for(ContactTypeRecord.Info contact_type : ContactTypes) {
			tip = new ToolTip(contact_type.desc);
			ContactEditor editor = createContactEditor(voclist_grouped, ctmodel.get(contact_type.id), tip);
			
			//only oim admin can edit submitter
			if(contact_type.id == 1) {//submitter
				if(!auth.allows("admin")) {
					editor.setDisabled(true);
				}
			}
			
			//request authority
			if(contact_type.id == 11) {
				//only admin_ra can edit ra
				if(!auth.allows("admin_ra")) {
					editor.setDisabled(true);
				}
				
				editor.setLabel(Rank.Primary, "Primary RA");
				editor.setLabel(Rank.Secondary, "Secondary RA");
				editor.setLabel(Rank.Tertiary, "Sponsors");
				
				editor.setMaxContacts(Rank.Secondary, 8);
			}
			
			if(contact_type.id != 5 && contact_type.id != 10 && contact_type.id != 11) { //5 = misc, 9 = resource report, 11 = ra agent
				editor.setMinContacts(Rank.Primary, 1);
			}
			contact_editors.put(contact_type.id, editor);
		}

		new DivRepStaticContent(this, "<h2>Confirmation</h2>");
		confirmation = new Confirmation(this, rec, auth);
		
		if(auth.allows("admin")) {
			new DivRepStaticContent(this, "<h2>Administrative Tasks</h2>");
		}
		
		/*
		footprints_id = new DivRepTextBox(this);
		footprints_id.setLabel("Footprints ID (deprecated - don't use this)");
		footprints_id.setValue(rec.footprints_id);
		footprints_id.setRequired(true);
		if(!auth.allows("admin")) {
			footprints_id.setHidden(true);
		}
		*/

		active = new DivRepCheckBox(this);
		active.setLabel("Active");
		active.setValue(rec.active);
		if(!auth.allows("admin")) {
			active.setHidden(true);
		}
		
		disable = new DivRepCheckBox(this);
		disable.setLabel("Disable");
		disable.setValue(rec.disable);
		if(!auth.allows("admin")) {
			disable.setHidden(true);
		}
		
		if(id == null) {
			AUPConfirmation aup = new AUPConfirmation(this);
		}
		
		comment = new DivRepTextArea(this);
		comment.setLabel("Update Comment");
		comment.setSampleValue("Please provide a reason for this update.");
	}
	
	private void hideParentVOSelector(Boolean b)
	{
		parent_vo.setHidden(b);
		parent_vo.redraw();
	}

	private ContactEditor createContactEditor(HashMap<Integer, ArrayList<VOContactRecord>> voclist, ContactTypeRecord ctrec, ToolTip tip) throws SQLException
	{
		new DivRepStaticContent(this, "<h3>" + ctrec.name + " " + tip.render() + "</h3>");
		ContactModel pmodel = new ContactModel(context);		
		ContactEditor editor = new ContactEditor(this, pmodel, ctrec.allow_secondary, ctrec.allow_tertiary);
		
		//if provided, populate currently selected contacts
		if(voclist != null) {
			ArrayList<VOContactRecord> clist = voclist.get(ctrec.id);
			if(clist != null) {
				for(VOContactRecord rec : clist) {
					ContactRecord keyrec = new ContactRecord();
					keyrec.id = rec.contact_id;
					ContactRecord person = pmodel.get(keyrec);
					editor.addSelected(person, rec.contact_rank_id);
				}
			}
		}
		return editor;
	}
	
	private ContactEditor createOASISUserEditor(ArrayList<VOOasisUserRecord> users) throws SQLException
	{
		ContactModel pmodel = new ContactModel(context);		
		ContactEditor editor = new ContactEditor(this, pmodel, false, false);
		editor.setMaxContacts(Rank.Primary, 10);
		editor.setLabel("OASIS Managers");
		editor.addClass("indent");
		editor.setShowRank(false);
		
		//if provided, populate currently selected contacts
		if(users != null) {
			for(VOOasisUserRecord user : users) {
				ContactRecord keyrec = new ContactRecord();
				keyrec.id = user.contact_id;
				ContactRecord person = pmodel.get(keyrec);
				editor.addSelected(person, Rank.Primary);
			}
		}
	
		return editor;
	}
	
	private LinkedHashMap<Integer, String> getSCNames() throws AuthorizationException, SQLException
	{
		SCModel model = new SCModel(context);
		ArrayList<SCRecord> recs = model.getAllActiveNonDisabled();
		Collections.sort(recs, new Comparator<SCRecord> () {
			public int compare(SCRecord a, SCRecord b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		LinkedHashMap<Integer, String> keyvalues = new LinkedHashMap<Integer, String>();
		for(SCRecord rec : recs) {
			keyvalues.put(rec.id, rec.name);
		}
		return keyvalues;
	}
	
	private LinkedHashMap<Integer, String> getVONames() throws AuthorizationException, SQLException
	{
		//pull all VOs
		VOModel model = new VOModel(context);
		ArrayList<VORecord> recs = model.getAll();
		Collections.sort(recs, new Comparator<VORecord> () {
			public int compare(VORecord a, VORecord b) {
				return a.getName().compareToIgnoreCase(b.getName());
			}
		});
		LinkedHashMap<Integer, String> keyvalues = new LinkedHashMap<Integer, String>();
		for(VORecord rec : recs) {
			keyvalues.put(rec.id, rec.name);
		}
		return keyvalues;
	}

	private void handleParentVOSelection(Integer parent_vo_id) {
		VOModel model = new VOModel (context);
		try {
			VORecord parent_vo_rec = model.get(parent_vo_id);
			if ((primary_url.getValue() == null) || (primary_url.getValue().length() == 0)) {
				primary_url.setValue(parent_vo_rec.primary_url);
			}
			if ((aup_url.getValue() == null) || (aup_url.getValue().length() == 0)) {
				aup_url.setValue(parent_vo_rec.aup_url);
			}
			if ((membership_services_url.getValue() == null) || (membership_services_url.getValue().length() == 0)) {
				membership_services_url.setValue(parent_vo_rec.membership_services_url);
			}
			if ((purpose_url.getValue() == null) || (purpose_url.getValue().length() == 0)) {
				purpose_url.setValue(parent_vo_rec.purpose_url);
			}
			if ((support_url.getValue() == null) || (support_url.getValue().length() == 0)) {
				support_url.setValue(parent_vo_rec.support_url);
			}
			redraw();
			
			if (sc_id.getValue() == null) {
				sc_id.setValue(parent_vo_rec.sc_id);
				sc_id.redraw();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected Boolean doSubmit() 
	{
		VORecord rec = new VORecord();
		rec.id = id;
	
		rec.name = name.getValue();
		rec.long_name = long_name.getValue();
		rec.description = description.getValue();
		rec.primary_url = primary_url.getValue();
		rec.aup_url = aup_url.getValue();
		rec.membership_services_url = membership_services_url.getValue();
		rec.purpose_url = purpose_url.getValue();
		rec.support_url = support_url.getValue();
		rec.app_description = app_description.getValue();
		rec.community = community.getValue();
		rec.sc_id = sc_id.getValue();
		rec.confirmed = confirmation.getTimestamp();
		rec.active = active.getValue();
		rec.disable = disable.getValue();
		rec.science_vo = science_vo.getValue();
		rec.use_oasis = use_oasis.getValue();

		context.setComment(comment.getValue());
		
		ArrayList<VOContactRecord> contacts = getContactRecordsFromEditor();
		
		ArrayList<Integer> field_of_science_ids = new ArrayList();
		for(Integer id : field_of_science_de.getSciences().keySet()) {
			DivRepCheckBox elem = field_of_science_de.getSciences().get(id);
			if(elem.getValue()) {
				field_of_science_ids.add(id);
			}
		}
		
		VOModel model = new VOModel(context);
		try {
			if(rec.id == null) {
				model.insertDetail(rec, 
						contacts, 
						parent_vo.getValue(), 
						field_of_science_ids,
						vo_report_name_div.getVOReports(model),
						oasis_users.getContactRecordsByRank(1));//primary
				context.message(MessageType.SUCCESS, "Successfully registered new VO. You should receive a notification with an instruction on how to active your VO.");
				
				try {
					//Find the Footprint ID of the associated SC
					SCModel scmodel = new SCModel(context);
					SCRecord screc = scmodel.get(rec.sc_id);
					
					//create footprint ticket
					Footprints fp = new Footprints(context);
					fp.createNewVOTicket(rec.name, screc);
				} catch (Exception fpe) {
					log.error("Failed to open footprints ticket: ", fpe);
				}
			} else {
				model.updateDetail(rec, 
						contacts, 
						parent_vo.getValue(), 
						field_of_science_ids,
						vo_report_name_div.getVOReports(model),
						oasis_users.getContactRecordsByRank(1));
				context.message(MessageType.SUCCESS, "Successfully updated a VO.");
			}
			return true;
		} catch (Exception e) {
			alert(e.getMessage());
			log.error("Failed to insert/update record", e);
			return false;
		}
	}
	
	//retrieve contact records from the contact editor.
	//be aware that VOContactRecord's vo_id is not populated.. you need to fill it out with
	//appropriate vo_id later
	private ArrayList<VOContactRecord> getContactRecordsFromEditor()
	{
		ArrayList<VOContactRecord> list = new ArrayList();
		
		for(Integer type_id : contact_editors.keySet()) 
		{
			ContactEditor editor = contact_editors.get(type_id);
			HashMap<ContactRecord, Integer> contacts = editor.getContactRecords();
			for(ContactRecord contact : contacts.keySet()) {
				VOContactRecord rec = new VOContactRecord();
				Integer rank_id = contacts.get(contact);
				rec.contact_id = contact.id;
				rec.contact_type_id = type_id;
				rec.contact_rank_id = rank_id;
				list.add(rec);
			}
		}
		
		return list;
	}
}
