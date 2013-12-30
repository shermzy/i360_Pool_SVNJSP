/*
 * JSP generated by Resin-3.1.8 (built Mon, 17 Nov 2008 12:15:21 PST)
 */

package _jsp;
import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.http.*;
import java.sql.*;
import java.io.*;
import CP_Classes.vo.votblConsultingCompany;
import java.lang.String;

public class _edit_0company__jsp extends com.caucho.jsp.JavaPage
{
  private static final java.util.HashMap<String,java.lang.reflect.Method> _jsp_functionMap = new java.util.HashMap<String,java.lang.reflect.Method>();
  private boolean _caucho_isDead;
  
  public void
  _jspService(javax.servlet.http.HttpServletRequest request,
              javax.servlet.http.HttpServletResponse response)
    throws java.io.IOException, javax.servlet.ServletException
  {
    javax.servlet.http.HttpSession session = request.getSession(true);
    com.caucho.server.webapp.WebApp _jsp_application = _caucho_getApplication();
    javax.servlet.ServletContext application = _jsp_application;
    com.caucho.jsp.PageContextImpl pageContext = _jsp_application.getJspApplicationContext().allocatePageContext(this, _jsp_application, request, response, null, session, 8192, true, false);
    javax.servlet.jsp.PageContext _jsp_parentContext = pageContext;
    javax.servlet.jsp.JspWriter out = pageContext.getOut();
    final javax.el.ELContext _jsp_env = pageContext.getELContext();
    javax.servlet.ServletConfig config = getServletConfig();
    javax.servlet.Servlet page = this;
    response.setContentType("text/html");
    response.setCharacterEncoding("UTF-8");
    request.setCharacterEncoding("UTF-8");
    try {
      out.write(_jsp_string0, 0, _jsp_string0.length);
      CP_Classes.Login logchk;
      synchronized (pageContext.getSession()) {
        logchk = (CP_Classes.Login) pageContext.getSession().getAttribute("logchk");
        if (logchk == null) {
          logchk = new CP_Classes.Login();
          pageContext.getSession().setAttribute("logchk", logchk);
        }
      }
      out.write(_jsp_string0, 0, _jsp_string0.length);
      CP_Classes.Create_Edit_Survey CE_Survey;
      synchronized (pageContext.getSession()) {
        CE_Survey = (CP_Classes.Create_Edit_Survey) pageContext.getSession().getAttribute("CE_Survey");
        if (CE_Survey == null) {
          CE_Survey = new CP_Classes.Create_Edit_Survey();
          pageContext.getSession().setAttribute("CE_Survey", CE_Survey);
        }
      }
      out.write(_jsp_string1, 0, _jsp_string1.length);
      CP_Classes.Database db;
      synchronized (pageContext.getSession()) {
        db = (CP_Classes.Database) pageContext.getSession().getAttribute("db");
        if (db == null) {
          db = new CP_Classes.Database();
          pageContext.getSession().setAttribute("db", db);
        }
      }
      out.write(_jsp_string1, 0, _jsp_string1.length);
      CP_Classes.ConsultingCompany CC;
      synchronized (pageContext.getSession()) {
        CC = (CP_Classes.ConsultingCompany) pageContext.getSession().getAttribute("CC");
        if (CC == null) {
          CC = new CP_Classes.ConsultingCompany();
          pageContext.getSession().setAttribute("CC", CC);
        }
      }
      out.write(_jsp_string1, 0, _jsp_string1.length);
      CP_Classes.Translate trans;
      synchronized (pageContext.getSession()) {
        trans = (CP_Classes.Translate) pageContext.getSession().getAttribute("trans");
        if (trans == null) {
          trans = new CP_Classes.Translate();
          pageContext.getSession().setAttribute("trans", trans);
        }
      }
      out.write(_jsp_string2, 0, _jsp_string2.length);
      // by lydia Date 05/09/2008 Fix jsp file to support Thai language 
      out.write(_jsp_string3, 0, _jsp_string3.length);
      out.print((trans.tslt("Enter Consulting Company Name")));
      out.write(_jsp_string4, 0, _jsp_string4.length);
      out.print((trans.tslt("Enter Consulting Company Description")));
      out.write(_jsp_string5, 0, _jsp_string5.length);
      

String username=(String)session.getAttribute("username");

  if (!logchk.isUsable(username)) 
  {
      out.write(_jsp_string6, 0, _jsp_string6.length);
        } 
  else 
  { 	
  
if(request.getParameter("btnEdit") != null)
{
	boolean canEdit = false;	
	boolean isExist = false;
	String xCCName = request.getParameter("txtCCName");
	String xCCDesc = request.getParameter("txtCCDesc");
    //Added by Ha 02/06/08-reedit by Ha 09/08/08
    int action  = 2;//2 means edit
    isExist = CC.checkRecordExist(xCCName, xCCDesc, logchk.getPKUser(),logchk.getCompany(),action);
    	if (isExist == false)    	
    		canEdit = CC.editRecord(logchk.getCompany(),xCCName, xCCDesc, logchk.getPKUser());
    
	if (canEdit)
	{
      out.write(_jsp_string7, 0, _jsp_string7.length);
      }
	else
	{
      out.write(_jsp_string8, 0, _jsp_string8.length);
      }
	
      out.write(_jsp_string9, 0, _jsp_string9.length);
      
}


      out.write(_jsp_string10, 0, _jsp_string10.length);
      out.print(( trans.tslt("Edit Company") ));
      out.write(_jsp_string11, 0, _jsp_string11.length);
      
	int NameSeq=0;

	votblConsultingCompany vo = CC.getConsultingCompany(logchk.getCompany());
	int CCID = vo.getCompanyID();
	String CCName = vo.getCompanyName();
	if (CCName == null) CCName = "";
	String CCDesc = vo.getCompanyDesc();
	if (CCDesc == null) CCDesc = "";

	
      out.write(_jsp_string12, 0, _jsp_string12.length);
      out.print(( trans.tslt("Company Name") ));
      out.write(_jsp_string13, 0, _jsp_string13.length);
      out.print((CCName));
      out.write(_jsp_string14, 0, _jsp_string14.length);
      out.print(( trans.tslt("Company Description") ));
      out.write(_jsp_string15, 0, _jsp_string15.length);
      out.print((CCDesc));
      out.write(_jsp_string16, 0, _jsp_string16.length);
      out.print(( trans.tslt("Save Changes") ));
      out.write(_jsp_string17, 0, _jsp_string17.length);
      out.print(( trans.tslt("Close") ));
      out.write(_jsp_string18, 0, _jsp_string18.length);
      	}	
      out.write(_jsp_string19, 0, _jsp_string19.length);
    } catch (java.lang.Throwable _jsp_e) {
      pageContext.handlePageException(_jsp_e);
    } finally {
      _jsp_application.getJspApplicationContext().freePageContext(pageContext);
    }
  }

  private java.util.ArrayList _caucho_depends = new java.util.ArrayList();

  public java.util.ArrayList _caucho_getDependList()
  {
    return _caucho_depends;
  }

  public void _caucho_addDepend(com.caucho.vfs.PersistentDependency depend)
  {
    super._caucho_addDepend(depend);
    com.caucho.jsp.JavaPage.addDepend(_caucho_depends, depend);
  }

  public boolean _caucho_isModified()
  {
    if (_caucho_isDead)
      return true;
    if (com.caucho.server.util.CauchoSystem.getVersionId() != 1886798272571451039L)
      return true;
    for (int i = _caucho_depends.size() - 1; i >= 0; i--) {
      com.caucho.vfs.Dependency depend;
      depend = (com.caucho.vfs.Dependency) _caucho_depends.get(i);
      if (depend.isModified())
        return true;
    }
    return false;
  }

  public long _caucho_lastModified()
  {
    return 0;
  }

  public java.util.HashMap<String,java.lang.reflect.Method> _caucho_getFunctionMap()
  {
    return _jsp_functionMap;
  }

  public void init(ServletConfig config)
    throws ServletException
  {
    com.caucho.server.webapp.WebApp webApp
      = (com.caucho.server.webapp.WebApp) config.getServletContext();
    super.init(config);
    com.caucho.jsp.TaglibManager manager = webApp.getJspApplicationContext().getTaglibManager();
    com.caucho.jsp.PageContextImpl pageContext = new com.caucho.jsp.PageContextImpl(webApp, this);
  }

  public void destroy()
  {
      _caucho_isDead = true;
      super.destroy();
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
    depend = new com.caucho.vfs.Depend(appDir.lookup("Edit_Company.jsp"), -5734590294642409904L, false);
    com.caucho.jsp.JavaPage.addDepend(_caucho_depends, depend);
  }

  private final static char []_jsp_string17;
  private final static char []_jsp_string14;
  private final static char []_jsp_string3;
  private final static char []_jsp_string2;
  private final static char []_jsp_string19;
  private final static char []_jsp_string13;
  private final static char []_jsp_string16;
  private final static char []_jsp_string15;
  private final static char []_jsp_string11;
  private final static char []_jsp_string12;
  private final static char []_jsp_string18;
  private final static char []_jsp_string7;
  private final static char []_jsp_string10;
  private final static char []_jsp_string4;
  private final static char []_jsp_string8;
  private final static char []_jsp_string1;
  private final static char []_jsp_string0;
  private final static char []_jsp_string6;
  private final static char []_jsp_string5;
  private final static char []_jsp_string9;
  static {
    _jsp_string17 = "\" name=\"btnEdit\" style=\"float: right\"></td>\r\n		<td align=\"right\" width=\"24%\">&nbsp;\r\n		</td>\r\n		<td align=\"right\" width=\"22%\">\r\n		<font size=\"2\">\r\n   \r\n		<input type=\"button\" value=\"".toCharArray();
    _jsp_string14 = "\"></td>\r\n	</tr>\r\n	<tr>\r\n		<td width=\"181\">&nbsp;</td>\r\n		<td colspan=\"2\">&nbsp;</td>\r\n		<td>&nbsp;</td>\r\n	</tr>\r\n	<tr>\r\n		<td width=\"181\"><font face=\"Arial\" size=\"2\">".toCharArray();
    _jsp_string3 = "\r\n\r\n<title>Edit Consulting Company</title>\r\n</head>\r\n<SCRIPT LANGUAGE=\"JavaScript\">\r\nfunction validate()\r\n{\r\n    x = document.Edit_Company\r\n    if (x.txtCCName.value == \"\")\r\n    {\r\n	alert(\"".toCharArray();
    _jsp_string2 = "\r\n<html>\r\n<head>\r\n<meta http-equiv=\"Content-Type\" content=\"text/html\">\r\n\r\n".toCharArray();
    _jsp_string19 = "\r\n</body>\r\n</html>".toCharArray();
    _jsp_string13 = ":</font></td>\r\n		<td colspan=\"3\"><input type=\"text\" name=\"txtCCName\" size=\"50\" value=\"".toCharArray();
    _jsp_string16 = "\"></td>\r\n	</tr>\r\n	<tr>\r\n		<td width=\"181\">&nbsp;</td>\r\n		<td colspan=\"2\">&nbsp;</td>\r\n		<td>&nbsp;</td>\r\n	</tr>\r\n	<tr>\r\n		<td width=\"29%\" align=\"right\">&nbsp;</td>\r\n		<td align=\"right\" colspan=\"3\">&nbsp;</td>\r\n	</tr>\r\n	<tr>\r\n		<td width=\"29%\" align=\"right\">\r\n		<p align=\"center\">\r\n		</td>\r\n		<td align=\"right\" width=\"25%\">\r\n		<font size=\"2\">\r\n   \r\n		<input type=\"submit\" value=\"".toCharArray();
    _jsp_string15 = ":</font></td>\r\n		<td colspan=\"3\"><input type=\"text\" name=\"txtCCDesc\" size=\"50\" value=\"".toCharArray();
    _jsp_string11 = "</font></b></td>\r\n	</tr>\r\n	<tr>\r\n		<td>&nbsp;</td>\r\n	</tr>\r\n	<tr>\r\n		<td>&nbsp;</td>\r\n	</tr>\r\n</table>\r\n<table border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">\r\n".toCharArray();
    _jsp_string12 = "\r\n\r\n	<tr>\r\n		<td width=\"181\"><font face=\"Arial\" size=\"2\">".toCharArray();
    _jsp_string18 = "\" name=\"btnClose\" style=\"float: right\" onclick=\"window.close()\"></td>\r\n	</tr>\r\n</table>\r\n</form>\r\n".toCharArray();
    _jsp_string7 = "\r\n		<script>\r\n		alert(\"Edited successfully\");\r\n		window.close();\r\n		//Edited by Xuehai, 06 Jun 2011. Changing location.href() to location.href='';\r\n		//opener.location.href('OrganizationList.jsp');\r\n		opener.location.href='OrganizationList.jsp';\r\n		</script>\r\n	".toCharArray();
    _jsp_string10 = "\r\n  <form name=\"Edit_Company\" action=\"Edit_Company.jsp\" method=\"post\" onsubmit=\"return validate()\">\r\n<table border=\"0\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\">\r\n	<tr>\r\n		<td><b><font face=\"Arial\" color=\"#000080\" size=\"2\">".toCharArray();
    _jsp_string4 = "\");\r\n	return false \r\n	}\r\n	if (x.txtCCDesc.value == \"\")\r\n    {\r\n	alert(\"".toCharArray();
    _jsp_string8 = "\r\n		<script>\r\n		alert(\"Record Exists\");		\r\n		//Edited by Xuehai, 06 Jun 2011. Changing location.href() to location.href='';\r\n		//opener.location.href('Edit_Company.jsp');\r\n		opener.location.href='Edit_Company.jsp';\r\n		</script>\r\n	".toCharArray();
    _jsp_string1 = "\r\n".toCharArray();
    _jsp_string0 = "  \r\n".toCharArray();
    _jsp_string6 = " <font size=\"2\">\r\n   \r\n	<script>\r\n	parent.location.href = \"index.jsp\";\r\n	</script>\r\n".toCharArray();
    _jsp_string5 = "\");\r\n	return false \r\n	}\r\n	//\\\\Added by Ha 02/06/08\r\n	if (confirm (\"Edit Company? \"))\r\n	return true;\r\n	else return false;\r\n	\r\n	return true\r\n}\r\n</SCRIPT>\r\n<body bgcolor=\"#FFFFCC\">\r\n".toCharArray();
    _jsp_string9 = "\r\n	<script>\r\n		window.close();\r\n		//Edited by Xuehai, 06 Jun 2011. Changing location.href() to location.href='';\r\n		//opener.location.href('OrganizationList.jsp');\r\n		opener.location.href='OrganizationList.jsp';\r\n	</script>\r\n	".toCharArray();
  }
}