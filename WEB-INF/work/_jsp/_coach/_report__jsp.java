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
import java.util.Date;
import java.text.SimpleDateFormat;
import CP_Classes.vo.*;

public class _report__jsp extends com.caucho.jsp.JavaPage
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
    CP_Classes.Login logchk;
    synchronized (pageContext.getSession()) {
      logchk = (CP_Classes.Login) pageContext.getSession().getAttribute("logchk");
      if (logchk == null) {
        logchk = new CP_Classes.Login();
        pageContext.getSession().setAttribute("logchk", logchk);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    CP_Classes.Database Database;
    synchronized (pageContext.getSession()) {
      Database = (CP_Classes.Database) pageContext.getSession().getAttribute("Database");
      if (Database == null) {
        Database = new CP_Classes.Database();
        pageContext.getSession().setAttribute("Database", Database);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    CP_Classes.User User;
    synchronized (pageContext.getSession()) {
      User = (CP_Classes.User) pageContext.getSession().getAttribute("User");
      if (User == null) {
        User = new CP_Classes.User();
        pageContext.getSession().setAttribute("User", User);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    Coach.CoachOrganization CoachOrganization;
    synchronized (pageContext.getSession()) {
      CoachOrganization = (Coach.CoachOrganization) pageContext.getSession().getAttribute("CoachOrganization");
      if (CoachOrganization == null) {
        CoachOrganization = new Coach.CoachOrganization();
        pageContext.getSession().setAttribute("CoachOrganization", CoachOrganization);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    Coach.SessionSetup SessionSetup;
    synchronized (pageContext.getSession()) {
      SessionSetup = (Coach.SessionSetup) pageContext.getSession().getAttribute("SessionSetup");
      if (SessionSetup == null) {
        SessionSetup = new Coach.SessionSetup();
        pageContext.getSession().setAttribute("SessionSetup", SessionSetup);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    Coach.CoachDateGroup CoachDateGroup;
    synchronized (pageContext.getSession()) {
      CoachDateGroup = (Coach.CoachDateGroup) pageContext.getSession().getAttribute("CoachDateGroup");
      if (CoachDateGroup == null) {
        CoachDateGroup = new Coach.CoachDateGroup();
        pageContext.getSession().setAttribute("CoachDateGroup", CoachDateGroup);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    Coach.CoachSlotGroup CoachSlotGroup;
    synchronized (pageContext.getSession()) {
      CoachSlotGroup = (Coach.CoachSlotGroup) pageContext.getSession().getAttribute("CoachSlotGroup");
      if (CoachSlotGroup == null) {
        CoachSlotGroup = new Coach.CoachSlotGroup();
        pageContext.getSession().setAttribute("CoachSlotGroup", CoachSlotGroup);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    Coach.CoachVenue Venue;
    synchronized (pageContext.getSession()) {
      Venue = (Coach.CoachVenue) pageContext.getSession().getAttribute("Venue");
      if (Venue == null) {
        Venue = new Coach.CoachVenue();
        pageContext.getSession().setAttribute("Venue", Venue);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    CP_Classes.Export Export;
    synchronized (pageContext.getSession()) {
      Export = (CP_Classes.Export) pageContext.getSession().getAttribute("Export");
      if (Export == null) {
        Export = new CP_Classes.Export();
        pageContext.getSession().setAttribute("Export", Export);
      }
    }
    out.write(_jsp_string1, 0, _jsp_string1.length);
    CP_Classes.Setting setting;
    synchronized (pageContext.getSession()) {
      setting = (CP_Classes.Setting) pageContext.getSession().getAttribute("setting");
      if (setting == null) {
        setting = new CP_Classes.Setting();
        pageContext.getSession().setAttribute("setting", setting);
      }
    }
    out.write(_jsp_string2, 0, _jsp_string2.length);
    
		String username = (String) session.getAttribute("username");
		Vector userAssignments=new Vector();
		if (!logchk.isUsable(username)) {
	
    out.write(_jsp_string3, 0, _jsp_string3.length);
    
 	} else {
		Vector sessionlist=SessionSetup.getSessionAllNames();
		userAssignments=SessionSetup.getUserAssignment();
 	if(request.getParameter("report") != null){
 		SessionSetup.setSelectedSession(Integer.parseInt(request.getParameter("selSession")));
 		if(SessionSetup.getSelectedSession()==0){
 			
    out.write(_jsp_string4, 0, _jsp_string4.length);
     
 			}
 		else{
				
		String file_name = "";
		int iExportType = 12;
		
		Export.setOrgID(logchk.getOrg()); //Set OrgID first
		Export.setSelectedSession(SessionSetup.getSelectedSession());
		Export.export(iExportType, logchk.getPKUser());
		file_name = Export.getCoachingStatusFileName();
		
		//read the file name.		
		String output = setting.getReport_Path() + "\\"+file_name;
		File f = new File (output);
	
        response.reset();
		//set the content type(can be excel/word/powerpoint etc..)
		response.setContentType ("application/xls");
		//set the header and also the Name by which user will be prompted to save
		response.addHeader ("Content-Disposition", "attachment;filename=\"" + file_name + "\"");
	
		//get the file name
		String name = f.getName().substring(f.getName().lastIndexOf("/") + 1,f.getName().length());
		//OPen an input stream to the file and post the file contents thru the 
		//servlet output stream to the client m/c
		InputStream in = new FileInputStream(f);
		ServletOutputStream outs = response.getOutputStream();
		
		int bit = 256;
		int i = 0;
	
	   	try 
	   	{
	       	while ((bit) >= 0) 
	       	{
	       		bit = in.read();
	       		outs.write(bit);
	       	}
	       	//System.out.println("" +bit);
	   	} catch (IOException ioe) 
	  	{
	     	ioe.printStackTrace(System.out);
	    }
	    outs.flush();
	    outs.close();
	    in.close();
 		}
	
 		}
 			/************************************************** ADDING TOGGLE FOR SORTING PURPOSE *************************************************/

	int toggle = SessionSetup.getToggle();	//0=asc, 1=desc
	int type = 1; //1=date, 2=starting time 3= coach 4= status
			
	if(request.getParameter("name") != null)
	{	 
		if(toggle == 0)
			toggle = 1;
		else
			toggle = 0;
		
		SessionSetup.setToggle(toggle);
		
		type = Integer.parseInt(request.getParameter("name"));			 
		SessionSetup.setSortType(type);									
	} 
	
/************************************************** ADDING TOGGLE FOR SORTING PURPOSE *************************************************/
 		
 
    out.write(_jsp_string5, 0, _jsp_string5.length);
    
								for (int i = 0; i < sessionlist.size(); i++) {

									voCoachSession sessionDistic = (voCoachSession) sessionlist.elementAt(i);
										int sessionPK = sessionDistic.getPK();
										String sessionName = sessionDistic.getName();
										String sessionCode = sessionDistic.getDescription();

										if (SessionSetup.getSelectedSession() == sessionPK) {
							
    out.write(_jsp_string6, 0, _jsp_string6.length);
    out.print((sessionPK));
    out.write(_jsp_string7, 0, _jsp_string7.length);
    out.print((sessionName));
    out.write(_jsp_string8, 0, _jsp_string8.length);
    
									} else {
								
    out.write(_jsp_string9, 0, _jsp_string9.length);
    out.print((sessionPK));
    out.write('>');
    out.print((sessionName));
    out.write(_jsp_string8, 0, _jsp_string8.length);
    
									}
										}
								
    out.write(_jsp_string10, 0, _jsp_string10.length);
    
 	}
 
    out.write(_jsp_string11, 0, _jsp_string11.length);
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
    depend = new com.caucho.vfs.Depend(appDir.lookup("Coach/Report.jsp"), 4642015050599898483L, false);
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

  private final static char []_jsp_string1;
  private final static char []_jsp_string11;
  private final static char []_jsp_string10;
  private final static char []_jsp_string4;
  private final static char []_jsp_string7;
  private final static char []_jsp_string2;
  private final static char []_jsp_string3;
  private final static char []_jsp_string8;
  private final static char []_jsp_string0;
  private final static char []_jsp_string5;
  private final static char []_jsp_string6;
  private final static char []_jsp_string9;
  static {
    _jsp_string1 = "\r\n	".toCharArray();
    _jsp_string11 = "\r\n   <!-- Le javascript\r\n    ================================================== -->\r\n    <!-- Placed at the end of the document so the pages load faster -->\r\n    <script type=\"text/javascript\" src=\"http://platform.twitter.com/widgets.js\"></script>\r\n    <script src=\"../lib/js/jquery.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-transition.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-alert.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-modal.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-dropdown.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-scrollspy.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-tab.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-tooltip.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-popover.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-button.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-collapse.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-carousel.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-typeahead.js\"></script>\r\n    <script src=\"../lib/js/bootstrap-affix.js\"></script>\r\n\r\n    <script src=\"../lib/js/holder/holder.js\"></script>\r\n    <script src=\"../lib/js/google-code-prettify/prettify.js\"></script>\r\n\r\n    <script src=\"../lib/js/application.js\"></script>\r\n\r\n\r\n    <!-- Analytics\r\n    ================================================== -->\r\n    <script>\r\n      var _gauges = _gauges || [];\r\n      (function() {\r\n        var t   = document.createElement('script');\r\n        t.type  = 'text/javascript';\r\n        t.async = true;\r\n        t.id    = 'gauges-tracker';\r\n        t.setAttribute('data-site-id', '4f0dc9fef5a1f55508000013');\r\n        t.src = '//secure.gaug.es/track.js';\r\n        var s = document.getElementsByTagName('script')[0];\r\n        s.parentNode.insertBefore(t, s);\r\n      })();\r\n    </script>\r\n</body>\r\n</html>".toCharArray();
    _jsp_string10 = "\r\n					</select></td>\r\n				</tr>\r\n				<tr>\r\n				<td height=\"20\">\r\n				</td>\r\n				</tr>\r\n				\r\n			</table>\r\n			<br>\r\n		<br>\r\n           \r\n             	<input class=\"btn btn-primary\" type=\"button\" name=\"report\"\r\n							value=\"Generate Booking Status Report\"\r\n							onclick=\"bookingStatusReport(this.form)\">   \r\n              \r\n              \r\n         \r\n</form> ".toCharArray();
    _jsp_string4 = "\r\n				<script type=\"text/javascript\">\r\n					alert(\"Please select coaching session\");\r\n				</script>\r\n				".toCharArray();
    _jsp_string7 = " selected>".toCharArray();
    _jsp_string2 = "\r\n	<script type=\"text/javascript\">\r\n	var x = parseInt(window.screen.width) / 2 - 240;  // the number 250 is the exact half of the width of the pop-up and so should be changed according to the size of the pop-up\r\n	var y = parseInt(window.screen.height) / 2 - 115;  // the number 125 is the exact half of the height of the pop-up and so should be changed according to the size of the pop-up\r\n\r\n		function proceed(form) {\r\n			\r\n		}\r\n		function check(field)\r\n		{\r\n			var isValid = 0;\r\n			var clickedValue = 0;\r\n			//check whether any checkbox selected\r\n			if( field == null ) {\r\n				isValid = 2;\r\n			\r\n			} else {\r\n\r\n				if(isNaN(field.length) == false) {\r\n					for (i = 0; i < field.length; i++)\r\n						if(field[i].checked) {\r\n							clickedValue = field[i].value;\r\n							isValid = 1;\r\n						}\r\n				}else {		\r\n					if(field.checked) {\r\n						clickedValue = field.value;\r\n						isValid = 1;\r\n					}\r\n						\r\n				}\r\n			}\r\n			\r\n			if(isValid == 1)\r\n				return clickedValue;\r\n			else if(isValid == 0)\r\n				alert(\"No record selected\");\r\n			else if(isValid == 2)\r\n				alert(\"No record available\");\r\n			\r\n			isValid = 0;\r\n\r\n		}\r\n		function bookingStatusReport(form){\r\n			form.action=\"Report.jsp?report=1\";\r\n			form.method=\"post\";\r\n			form.submit();\r\n		}\r\n	</script>\r\n</head>\r\n\r\n<body>\r\n	\r\n	<!-- select Session -->\r\n\r\n	".toCharArray();
    _jsp_string3 = "\r\n	<font size=\"2\"> <script>\r\n		parent.location.href = \"../index.jsp\";\r\n	</script> ".toCharArray();
    _jsp_string8 = "\r\n								".toCharArray();
    _jsp_string0 = "\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\r\n<html>\r\n<head>\r\n<!-- CSS -->\r\n\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap.css\">\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap-responsive.css\">\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap.min.css\">\r\n<link type=\"text/css\" rel=\"stylesheet\" href=\"../lib/css/bootstrap-responsive.min.css\">\r\n\r\n\r\n<!-- jQuery -->\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap.min.js\"></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap.js\"></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/jquery-1.9.1.js\"></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap.min.js\" ></script>\r\n<script type=\"text/javascript\" src=\"../lib/js/bootstrap-dropdown.js\"></script>\r\n\r\n\r\n<title>User Assignment</title>\r\n\r\n<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n<style type=\"text/css\">\r\n<!--\r\nbody {\r\n	\r\n}\r\n-->\r\n</style>\r\n".toCharArray();
    _jsp_string5 = "\r\n					<h2>Coach Booking Report</h2>\r\n					<br>\r\n		<form method=\"post\">\r\n			<table>\r\n				<tr>\r\n				<td>\r\n				<b><font color=\"#000080\" size=\"2\" face=\"Arial\">Session Name:  </font></b>\r\n				</td>\r\n					<td width=\"500\" colspan=\"1\"><select size=\"1\"\r\n						name=\"selSession\" onChange=\"setSessionName(this.form)\">\r\n						<option value=0>Select a Session Name</option>\r\n							".toCharArray();
    _jsp_string6 = "\r\n							<option value=".toCharArray();
    _jsp_string9 = "\r\n							\r\n							<option value=".toCharArray();
  }
}
