/*
 * JSP generated by Resin Professional 4.0.36 (built Fri, 26 Apr 2013 03:33:09 PDT)
 */

package _jsp._coach;
import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.http.*;
import java.sql.*;
import java.io.*;
import java.util.*;
import CP_Classes.vo.*;

public class _slotgroup__jsp extends com.caucho.jsp.JavaPage
{
  private static final java.util.HashMap<String,java.lang.reflect.Method> _jsp_functionMap = new java.util.HashMap<String,java.lang.reflect.Method>();
  private boolean _caucho_isDead;
  private boolean _caucho_isNotModified;
  private com.caucho.jsp.PageManager _jsp_pageManager;
  
  public void
  _jspService(javax.servlet.http.HttpServletRequest request,
              javax.servlet.http.HttpServletResponse response)
    throws java.io.IOException, javax.servlet.ServletException
  {
    javax.servlet.http.HttpSession session = request.getSession(true);
    com.caucho.server.webapp.WebApp _jsp_application = _caucho_getApplication();
    com.caucho.jsp.PageContextImpl pageContext = _jsp_pageManager.allocatePageContext(this, _jsp_application, request, response, null, session, 8192, true, false);

    TagState _jsp_state = null;

    try {
      _jspService(request, response, pageContext, _jsp_application, session, _jsp_state);
    } catch (java.lang.Throwable _jsp_e) {
      pageContext.handlePageException(_jsp_e);
    } finally {
      _jsp_pageManager.freePageContext(pageContext);
    }
  }
  
  private void
  _jspService(javax.servlet.http.HttpServletRequest request,
              javax.servlet.http.HttpServletResponse response,
              com.caucho.jsp.PageContextImpl pageContext,
              javax.servlet.ServletContext application,
              javax.servlet.http.HttpSession session,
              TagState _jsp_state)
    throws Throwable
  {
    javax.servlet.jsp.JspWriter out = pageContext.getOut();
    final javax.el.ELContext _jsp_env = pageContext.getELContext();
    javax.servlet.ServletConfig config = getServletConfig();
    javax.servlet.Servlet page = this;
    javax.servlet.jsp.tagext.JspTag _jsp_parent_tag = null;
    com.caucho.jsp.PageContextImpl _jsp_parentContext = pageContext;
    response.setContentType("text/html");
    response.setCharacterEncoding("utf-8");

    // Author: Dai Yong in June 2013
    out.write(_jsp_string0, 0, _jsp_string0.length);
    Coach.CoachSlotGroup CoachSlotGroup;
    synchronized (pageContext.getSession()) {
      CoachSlotGroup = (Coach.CoachSlotGroup) pageContext.getSession().getAttribute("CoachSlotGroup");
      if (CoachSlotGroup == null) {
        CoachSlotGroup = new Coach.CoachSlotGroup();
        pageContext.getSession().setAttribute("CoachSlotGroup", CoachSlotGroup);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    Coach.CoachSlot CoachSlot;
    synchronized (pageContext.getSession()) {
      CoachSlot = (Coach.CoachSlot) pageContext.getSession().getAttribute("CoachSlot");
      if (CoachSlot == null) {
        CoachSlot = new Coach.CoachSlot();
        pageContext.getSession().setAttribute("CoachSlot", CoachSlot);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    Coach.LoginStatus LoginStatus;
    synchronized (pageContext.getSession()) {
      LoginStatus = (Coach.LoginStatus) pageContext.getSession().getAttribute("LoginStatus");
      if (LoginStatus == null) {
        LoginStatus = new Coach.LoginStatus();
        pageContext.getSession().setAttribute("LoginStatus", LoginStatus);
      }
    }
    out.write(_jsp_string2, 0, _jsp_string2.length);
    // Author: Dai Yong in June 2013
    out.write(_jsp_string3, 0, _jsp_string3.length);
    
		int logSlotGroupPK = 0;
		int PKselSlotGroup = CoachSlotGroup.getFirstSlotGroupPK();
		
		Vector CoachSlots = new Vector();
		if (request.getParameter("proceed") == null) {
			/* start up get the select Schedule detail vector */

			//System.out.println("init Schedule jsp:");
			if(LoginStatus.getSelectedSlotGroup()==0){
				CoachSlots = CoachSlotGroup.getSelectedSlotGroupDetails(1);
				LoginStatus.setSelectedSlotGroup(PKselSlotGroup);
				logSlotGroupPK = PKselSlotGroup;
			}else{
				CoachSlots = CoachSlotGroup.getSelectedSlotGroupDetails(LoginStatus.getSelectedSlotGroup());
				logSlotGroupPK = LoginStatus.getSelectedSlotGroup();
			}
			

		}
		if (request.getParameter("proceed") != null) {
			if (Integer.parseInt(request.getParameter("selSlotGroup"))==0) {
				
    out.write(_jsp_string4, 0, _jsp_string4.length);
    
				
			}
			if (request.getParameter("selSlotGroup") != null) {
				/* get the select Schedule detail vector */
				//System.out.println("selSlotGroup"+request.getParameter("selSlotGroup"));
				PKselSlotGroup = Integer.parseInt(request.getParameter("selSlotGroup"));
				//System.out.println("Old Page selSlotGroup jsp:" + PKselSlotGroup);
				CoachSlots = CoachSlotGroup.getSelectedSlotGroupDetails(PKselSlotGroup);
				LoginStatus.setSelectedSlotGroup(PKselSlotGroup);
				logSlotGroupPK = PKselSlotGroup;
			}
		}
		if(request.getParameter("deleteSlotGroup")!= null){
			int PKSlotGroup = new Integer(request.getParameter("deleteSlotGroup")).intValue();
			//System.out.println("deleteSlotGroup PK:"+PKSlotGroup);
			 Boolean delete =CoachSlotGroup.deleteSlotGroup(PKSlotGroup);
			 if(delete){
				 LoginStatus.setSelectedSlotGroup(1);
				 CoachSlots = CoachSlotGroup.getSelectedSlotGroupDetails(1);
				 
    out.write(_jsp_string5, 0, _jsp_string5.length);
     
			 }
			 else{
				 
    out.write(_jsp_string6, 0, _jsp_string6.length);
     
			 }
			
		}
		if(request.getParameter("deleteSlot")!= null){
			//System.out.println("request:"+request.getParameter("deleteSlot"));
			int PKSlot = Integer.valueOf(request.getParameter("deleteSlot"));
			//System.out.println("deleteSlot PK:"+PKSlot);
			 Boolean delete =CoachSlot.deleteSlot(PKSlot);
			 if(delete){
				 
    out.write(_jsp_string7, 0, _jsp_string7.length);
     
				 logSlotGroupPK=LoginStatus.getSelectedSlotGroup();
				 CoachSlots = CoachSlotGroup.getSelectedSlotGroupDetails(LoginStatus.getSelectedSlotGroup());
			 }
			 else{
				 
    out.write(_jsp_string8, 0, _jsp_string8.length);
     
			 }
		}
	
    out.write(_jsp_string9, 0, _jsp_string9.length);
    
		Vector SlotGroupList = new Vector();
		SlotGroupList = CoachSlotGroup.getAllSlotGroup();
		//System.out.println("size of Schedule size jsp: " + SlotGroupList.size());
	
    out.write(_jsp_string10, 0, _jsp_string10.length);
    
							voCoachSlotGroup voCoachSlotGroup = new voCoachSlotGroup();

							for (int i = 0; i < SlotGroupList.size(); i++) {
								voCoachSlotGroup = (voCoachSlotGroup) SlotGroupList
										.elementAt(i);
								int slotGroupPK = voCoachSlotGroup.getPk();
								String slotGroupName = voCoachSlotGroup.getSlotGroupName();

								if (logSlotGroupPK == slotGroupPK) {
						
    out.write(_jsp_string11, 0, _jsp_string11.length);
    out.print((slotGroupPK));
    out.write(_jsp_string12, 0, _jsp_string12.length);
    out.print((slotGroupName));
    out.write(_jsp_string13, 0, _jsp_string13.length);
    
								} else {
							
    out.write(_jsp_string14, 0, _jsp_string14.length);
    out.print((slotGroupPK));
    out.write('>');
    out.print((slotGroupName));
    out.write(_jsp_string13, 0, _jsp_string13.length);
    
								}
								}
							
    out.write(_jsp_string15, 0, _jsp_string15.length);
    
				int DisplayNo = 1;
					int pkslot=0;
					for (int i = 0; i < CoachSlots.size(); i++) {
						voCoachSlot voCoachSlot = new voCoachSlot();
						voCoachSlot = (voCoachSlot) CoachSlots.elementAt(i);

						pkslot = voCoachSlot.getPK();
						int startingTime = voCoachSlot.getStartingtime();
						int endingingTime = voCoachSlot.getEndingtime();
						String startingTime4Digits;
						String endingTime4Digits;
					if (startingTime < 1000) {
						startingTime4Digits="0"+startingTime;
					} else {
						startingTime4Digits=""+startingTime;
					}
					if (endingingTime < 1000) {
						endingTime4Digits="0"+endingingTime;
					} else {
						endingTime4Digits=""+endingingTime;
					}
				

					
					//System.out.println("ending time" + endingingTime);
			
    out.write(_jsp_string16, 0, _jsp_string16.length);
    out.print((pkslot));
    out.write(_jsp_string17, 0, _jsp_string17.length);
    out.print((DisplayNo));
    out.write(_jsp_string18, 0, _jsp_string18.length);
    out.print((startingTime4Digits));
    out.write(_jsp_string18, 0, _jsp_string18.length);
    out.print((endingTime4Digits));
    out.write(_jsp_string19, 0, _jsp_string19.length);
    
				DisplayNo++;
				}
			
    out.write(_jsp_string20, 0, _jsp_string20.length);
  }

  private com.caucho.make.DependencyContainer _caucho_depends
    = new com.caucho.make.DependencyContainer();

  public java.util.ArrayList<com.caucho.vfs.Dependency> _caucho_getDependList()
  {
    return _caucho_depends.getDependencies();
  }

  public void _caucho_addDepend(com.caucho.vfs.PersistentDependency depend)
  {
    super._caucho_addDepend(depend);
    _caucho_depends.add(depend);
  }

  protected void _caucho_setNeverModified(boolean isNotModified)
  {
    _caucho_isNotModified = true;
  }

  public boolean _caucho_isModified()
  {
    if (_caucho_isDead)
      return true;

    if (_caucho_isNotModified)
      return false;

    if (com.caucho.server.util.CauchoSystem.getVersionId() != -7791540776389363938L)
      return true;

    return _caucho_depends.isModified();
  }

  public long _caucho_lastModified()
  {
    return 0;
  }

  public void destroy()
  {
      _caucho_isDead = true;
      super.destroy();
    TagState tagState;
  }

  public void init(com.caucho.vfs.Path appDir)
    throws javax.servlet.ServletException
  {
    com.caucho.vfs.Path resinHome = com.caucho.server.util.CauchoSystem.getResinHome();
    com.caucho.vfs.MergePath mergePath = new com.caucho.vfs.MergePath();
    mergePath.addMergePath(appDir);
    mergePath.addMergePath(resinHome);
    com.caucho.loader.DynamicClassLoader loader;
    loader = (com.caucho.loader.DynamicClassLoader) getClass().getClassLoader();
    String resourcePath = loader.getResourcePathSpecificFirst();
    mergePath.addClassPath(resourcePath);
    com.caucho.vfs.Depend depend;
    depend = new com.caucho.vfs.Depend(appDir.lookup("Coach/SlotGroup.jsp"), 2675268664139555094L, false);
    _caucho_depends.add(depend);
    depend = new com.caucho.vfs.Depend(appDir.lookup("Coach/nav.jsp"), 5445090986036096754L, false);
    _caucho_depends.add(depend);
  }

  final static class TagState {

    void release()
    {
    }
  }

  public java.util.HashMap<String,java.lang.reflect.Method> _caucho_getFunctionMap()
  {
    return _jsp_functionMap;
  }

  public void caucho_init(ServletConfig config)
  {
    try {
      com.caucho.server.webapp.WebApp webApp
        = (com.caucho.server.webapp.WebApp) config.getServletContext();
      init(config);
      if (com.caucho.jsp.JspManager.getCheckInterval() >= 0)
        _caucho_depends.setCheckInterval(com.caucho.jsp.JspManager.getCheckInterval());
      _jsp_pageManager = webApp.getJspApplicationContext().getPageManager();
      com.caucho.jsp.TaglibManager manager = webApp.getJspApplicationContext().getTaglibManager();
      com.caucho.jsp.PageContextImpl pageContext = new com.caucho.jsp.InitPageContextImpl(webApp, this);
    } catch (Exception e) {
      throw com.caucho.config.ConfigException.create(e);
    }
  }

  private final static char []_jsp_string14;
  private final static char []_jsp_string3;
  private final static char []_jsp_string13;
  private final static char []_jsp_string17;
  private final static char []_jsp_string10;
  private final static char []_jsp_string6;
  private final static char []_jsp_string18;
  private final static char []_jsp_string11;
  private final static char []_jsp_string8;
  private final static char []_jsp_string0;
  private final static char []_jsp_string4;
  private final static char []_jsp_string12;
  private final static char []_jsp_string2;
  private final static char []_jsp_string16;
  private final static char []_jsp_string15;
  private final static char []_jsp_string20;
  private final static char []_jsp_string19;
  private final static char []_jsp_string7;
  private final static char []_jsp_string1;
  private final static char []_jsp_string5;
  private final static char []_jsp_string9;
  static {
    _jsp_string14 = "\r\n						\r\n						<option value=".toCharArray();
    _jsp_string3 = "\r\n<html>\r\n<head>\r\n<!-- CSS -->\r\n\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap.css\">\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap-responsive.css\">\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap.min.css\">\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap-responsive.min.css\">\r\n\r\n\r\n<!-- jQuery -->\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap.min.js\"></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap.js\"></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/jquery-1.9.1.js\"></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap.min.js\" ></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap-dropdown.js\"></script>\r\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\r\n<style>\r\ninput\r\n{\r\nclass=\"btn btn-primary\";\r\n} \r\n</style>\r\n</head>\r\n<body>\r\n	<p>&nbsp;</p>	\r\n	<ul class=\"breadcrumb\">\r\n    <li><a href=\"Coach.jsp\">Coach</a> <span class=\"divider\">/</span></li>\r\n    <li><a href=\"Venue.jsp\">Venue</a> <span class=\"divider\">/</span></li>\r\n    <li><a href=\"SlotGroup.jsp\">Time Slot</a> <span class=\"divider\">/</span></li>\r\n    <li><a href=\"DateGroup.jsp\">Coaching Period</a> <span class=\"divider\">/</span></li>\r\n    <li><a href=\"SessionManagement.jsp\">Coaching Session</a> <span class=\"divider\">/</span></li>\r\n    <li><a href=\"UserAssignment.jsp\">Candidate Assignment</a> <span class=\"divider\">/</span></li>\r\n    </ul>\r\n	\r\n <!-- Le javascript\r\n    ================================================== -->\r\n    <!-- Placed at the end of the document so the pages load faster -->\r\n    <script type=\"text/javascript\" src=\"http://platform.twitter.com/widgets.js\"></script>\r\n    <script src=\"../lib/js/jquery.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-transition.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-alert.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-modal.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-dropdown.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-scrollspy.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-tab.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-tooltip.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-popover.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-button.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-collapse.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-carousel.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-typeahead.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-affix.js\"></script>\r\n    <script src=\"../lib/js/holder/holder.js\"></script>\r\n    <script src=\"../lib/js/google-code-prettify/prettify.js\"></script>\r\n    <script src=\"../lib/js/application.js\"></script>\r\n\r\n\r\n    <!-- Analytics\r\n    ================================================== -->\r\n    <script>\r\n      var _gauges = _gauges || [];\r\n      (function() {\r\n        var t   = document.createElement('script');\r\n        t.type  = 'text/javascript';\r\n        t.async = true;\r\n        t.id    = 'gauges-tracker';\r\n        t.setAttribute('data-site-id', '4f0dc9fef5a1f55508000013');\r\n        t.src = '//secure.gaug.es/track.js';\r\n        var s = document.getElementsByTagName('script')[0];\r\n        s.parentNode.insertBefore(t, s);\r\n      })();\r\n    </script>\r\n</body>\r\n</html> \r\n	".toCharArray();
    _jsp_string13 = "\r\n							".toCharArray();
    _jsp_string17 = "></font></td>\r\n				<td align=\"center\">".toCharArray();
    _jsp_string10 = "\r\n	<br>\r\n	<!-- display Schedule-->\r\n	<form>\r\n		<table>\r\n			<tr>\r\n				<td width=\"120\"><font face=\"Arial\" size=\"2\">Time Slot Name:</font></td>\r\n				<td width=\"23\">:</td>\r\n				<td width=\"500\" colspan=\"1\"><select size=\"1\"\r\n					name=\"selSlotGroup\" onChange=\"proceed(this.form)\">\r\n					<option value=0 >Please Select Time Slot</option>\r\n						".toCharArray();
    _jsp_string6 = "<script>\r\n				 alert(\"Time Slot used in Coaching Assgiment and cannot be deleted\");\r\n				 </script>".toCharArray();
    _jsp_string18 = "</td>\r\n				<td align=\"center\">".toCharArray();
    _jsp_string11 = "\r\n						<option value=".toCharArray();
    _jsp_string8 = "<script>\r\n				 alert(\"Timing used in user assingment and cannot be deleted.\");\r\n				 </script>".toCharArray();
    _jsp_string0 = "\r\n\r\n\r\n\r\n\r\n\r\n\r\n<html>\r\n<head>\r\n<title>Schedule here</title>\r\n\r\n".toCharArray();
    _jsp_string4 = "\r\n				<script>\r\n				alert(\"Please Select Time Slot\");\r\n				</script>\r\n				".toCharArray();
    _jsp_string12 = " selected>".toCharArray();
    _jsp_string2 = "\r\n\r\n\r\n<script>\r\nvar x = parseInt(window.screen.width) / 2 - 240;  // the number 250 is the exact half of the width of the pop-up and so should be changed according to the size of the pop-up\r\nvar y = parseInt(window.screen.height) / 2 - 115;  // the number 125 is the exact half of the height of the pop-up and so should be changed according to the size of the pop-up\r\n\r\n\r\nfunction check(field)\r\n{\r\n	var isValid = 0;\r\n	var clickedValue = 0;\r\n	//check whether any checkbox selected\r\n	if( field == null ) {\r\n		isValid = 2;\r\n	\r\n	} else {\r\n\r\n		if(isNaN(field.length) == false) {\r\n			for (i = 0; i < field.length; i++)\r\n				if(field[i].checked) {\r\n					clickedValue = field[i].value;\r\n					isValid = 1;\r\n				}\r\n		}else {		\r\n			if(field.checked) {\r\n				clickedValue = field.value;\r\n				isValid = 1;\r\n			}\r\n				\r\n		}\r\n	}\r\n	\r\n	if(isValid == 1)\r\n		return clickedValue;\r\n	else if(isValid == 0)\r\n		alert(\"No record selected\");\r\n	else if(isValid == 2)\r\n		alert(\"No record available\");\r\n	\r\n	isValid = 0;\r\n\r\n}\r\n\r\n	function proceed(form) {\r\n		form.action = \"SlotGroup.jsp?proceed=1\";\r\n		form.method = \"post\";\r\n		form.submit();\r\n	}\r\n	function addSlotGroup(form){\r\n		var myWindow=window.open('AddSlotGroup.jsp','windowRef','scrollbars=no, width=480, height=250');\r\n		myWindow.moveTo(x,y);\r\n	    myWindow.location.href = 'AddSlotGroup.jsp';\r\n	}\r\n		\r\n	function editSlotGroup(form, field){\r\n		var myWindow=window.open('EditSlotGroup.jsp','windowRef','scrollbars=no, width=480, height=250');\r\n		myWindow.moveTo(x,y);\r\n	    myWindow.location.href = 'EditSlotGroup.jsp';\r\n	}\r\n\r\n	function deleteSlotGroup(form, field){\r\n		if(confirm(\"Are you sure to delete the time slot\")){\r\n		form.action=\"SlotGroup.jsp?deleteSlotGroup=\"+field.value;\r\n		form.method=\"post\";\r\n		form.submit();\r\n	}\r\n	}\r\n	function addSlot(form){\r\n		var myWindow=window.open('AddSlot.jsp','windowRef','scrollbars=no, width=480, height=250');\r\n		myWindow.moveTo(x,y);\r\n	    myWindow.location.href = 'AddSlot.jsp';\r\n	}\r\n		\r\n	function editSlot(form, field){\r\n		var value = check(field);\r\n		\r\n		if(value)\r\n		{						\r\n			var myWindow=window.open('EditSlot.jsp?editedSlot='+ value,'windowRef','scrollbars=no, width=480, height=250');\r\n			var query = \"EditSlot.jsp?editedSlot=\" + value;\r\n			myWindow.moveTo(x,y);\r\n	    	myWindow.location.href = query;\r\n		}\r\n		\r\n	}\r\n\r\n\r\n	function deleteSlot(form, field) {\r\n		var value = check(field);\r\n\r\n		if (value) {\r\n			if (confirm(\"Are you sure to delete the timing\")) {\r\n				form.action = \"SlotGroup.jsp?deleteSlot=\" + value;\r\n				form.method = \"post\";\r\n				form.submit();\r\n			}\r\n		}\r\n	}\r\n</script>\r\n</head>\r\n<body>\r\n".toCharArray();
    _jsp_string16 = "\r\n			<tr onMouseOver=\"this.bgColor = '#99ccff'\"\r\n				onMouseOut=\"this.bgColor = '#FFFFCC'\">\r\n				<td style=\"border-width: 1px\"><font size=\"2\"> <input type=\"radio\" name=\"selslot\" value=".toCharArray();
    _jsp_string15 = "\r\n						\r\n				</select></td>\r\n			</tr>\r\n		</table>\r\n		<br>\r\n		<!--  button for schedule-->\r\n		<p></p>\r\n		<input class=\"btn btn-primary\" type=\"button\" name=\"AddSlotGroup\" value=\"Add Time Slot\"\r\n			onclick=\"addSlotGroup(this.form)\"> \r\n		<input class=\"btn btn-primary\" type=\"button\" name=\"EditSlotGroup\" value=\"Edit Time Slot\"\r\n			onclick=\"editSlotGroup(this.form, this.form.selSlotGroup)\"> \r\n		<input class=\"btn btn-primary\" type=\"button\" name=\"DeleteSlotGroup\" value=\"Delete Time Slot\"\r\n			onclick=\"deleteSlotGroup(this.form, this.form.selSlotGroup)\">\r\n		<p></p>\r\n		\r\n\r\n		<!--Display selected Schedule details  -->\r\n		<br> <br> <br>\r\n		<p>\r\n			<b><font color=\"#000080\" size=\"2\" face=\"Arial\">Selected Time Slot Information:</font></b>\r\n		</p>\r\n		<table>\r\n			\r\n			<th width=\"30\" bgcolor=\"navy\" bordercolor=\"#3399FF\" align=\"center\"><b>\r\n					<font style='color: white'>&nbsp;</font>\r\n			</b></th>\r\n			<th width=\"30\" bgcolor=\"navy\" bordercolor=\"#3399FF\" align=\"center\"><b>\r\n					<font style='color: white'>No</font>\r\n			</b></th>\r\n			<th width=\"150\" bgcolor=\"navy\" bordercolor=\"#3399FF\" align=\"center\"><b>\r\n					<font style='color: white'>Starting Time</font>\r\n			</b></th>\r\n			<th width=\"150\" bgcolor=\"navy\" bordercolor=\"#3399FF\" align=\"center\"><b>\r\n					<font style='color: white'>Ending Time</font>\r\n			</b></th>\r\n\r\n			".toCharArray();
    _jsp_string20 = "\r\n		</table>\r\n		<!--  button for slot-->\r\n		<p></p>\r\n			<input class=\"btn btn-primary\" type=\"button\" name=\"AddSlot\" value=\"Add Coaching Time\" onclick=\"addSlot(this.form)\"> \r\n			<input class=\"btn btn-primary\" type=\"button\" name=\"EditSlot\" value=\"Edit Coaching Time\" onclick=\"editSlot(this.form, this.form.selslot)\"> \r\n			<input class=\"btn btn-primary\" type=\"button\" name=\"DeleteSlot\" value=\"Delete Coaching Time\" onclick=\"deleteSlot(this.form, this.form.selslot)\">\r\n		\r\n		<p></p>\r\n	</form>\r\n</body>\r\n</html>".toCharArray();
    _jsp_string19 = "</td>\r\n			</tr>\r\n			".toCharArray();
    _jsp_string7 = "<script>\r\n				 alert(\"Timing deleted successfully.\");\r\n				 </script>".toCharArray();
    _jsp_string1 = "\r\n".toCharArray();
    _jsp_string5 = "<script>\r\n				 alert(\"Time Slot deleted successfully.\");\r\n				 </script>".toCharArray();
    _jsp_string9 = "\r\n\r\n	<p>\r\n		<br>\r\n			<b><font color=\"#000080\" size=\"3\" face=\"Arial\">Time Slot Management</font></b>\r\n		<br>\r\n	</p>\r\n\r\n	<!-- list all the Schedule  -->\r\n	".toCharArray();
  }
}
