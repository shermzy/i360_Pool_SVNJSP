/*
 * JSP generated by Resin Professional 4.0.36 (built Fri, 26 Apr 2013 03:33:09 PDT)
 */

package _jsp;
import javax.servlet.*;
import javax.servlet.jsp.*;
import javax.servlet.http.*;
import java.sql.*;
import java.util.*;
import java.io.*;
import CP_Classes.PrelimQuestion;
import CP_Classes.PrelimQuestionAns;

public class _surveyprelimq__jsp extends com.caucho.jsp.JavaPage
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
    response.setContentType("text/html; charset=ISO-8859-1");

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
    CP_Classes.Translate trans;
    synchronized (pageContext.getSession()) {
      trans = (CP_Classes.Translate) pageContext.getSession().getAttribute("trans");
      if (trans == null) {
        trans = new CP_Classes.Translate();
        pageContext.getSession().setAttribute("trans", trans);
      }
    }
    out.write(_jsp_string2, 0, _jsp_string2.length);
    CP_Classes.PrelimQuestionController PrelimQController;
    synchronized (pageContext.getSession()) {
      PrelimQController = (CP_Classes.PrelimQuestionController) pageContext.getSession().getAttribute("PrelimQController");
      if (PrelimQController == null) {
        PrelimQController = new CP_Classes.PrelimQuestionController();
        pageContext.getSession().setAttribute("PrelimQController", PrelimQController);
      }
    }
    out.write(_jsp_string2, 0, _jsp_string2.length);
    CP_Classes.Create_Edit_Survey CE_Survey;
    synchronized (pageContext.getSession()) {
      CE_Survey = (CP_Classes.Create_Edit_Survey) pageContext.getSession().getAttribute("CE_Survey");
      if (CE_Survey == null) {
        CE_Survey = new CP_Classes.Create_Edit_Survey();
        pageContext.getSession().setAttribute("CE_Survey", CE_Survey);
      }
    }
    out.write(_jsp_string2, 0, _jsp_string2.length);
    CP_Classes.PrelimQuestionBean PrelimQuestionBean;
    synchronized (pageContext.getSession()) {
      PrelimQuestionBean = (CP_Classes.PrelimQuestionBean) pageContext.getSession().getAttribute("PrelimQuestionBean");
      if (PrelimQuestionBean == null) {
        PrelimQuestionBean = new CP_Classes.PrelimQuestionBean();
        pageContext.getSession().setAttribute("PrelimQuestionBean", PrelimQuestionBean);
      }
    }
    out.write(_jsp_string3, 0, _jsp_string3.length);
    out.print((trans.tslt("Are you sure you want to delete this question")));
    out.write(_jsp_string4, 0, _jsp_string4.length);
    out.print((trans.tslt("Are you sure you want to change the rating scale for this question")));
    out.write(_jsp_string5, 0, _jsp_string5.length);
    
String username=(String)session.getAttribute("username");

  if (!logchk.isUsable(username)){

    out.write(_jsp_string6, 0, _jsp_string6.length);
    }		
	
	int SurveyID = CE_Survey.getSurvey_ID();
	System.out.println(SurveyID);
	Vector<PrelimQuestion> v = PrelimQController.getQuestions(SurveyID);
	if(request.getParameter("add") != null)
	{
		PrelimQuestionBean.setQuestionCount(PrelimQuestionBean.getQuestionCount()+1);
	}
	
	if(request.getParameter("entry") != null)
	{
		PrelimQuestionBean.setQuestionCount(v.size());
	}
	int QuestionCount = PrelimQuestionBean.getQuestionCount();
	if(request.getParameter("save") != null)
	{
		if(request.getParameter("header")!=null)
		{
			String header = PrelimQController.escapeInvalidChars(request.getParameter("header"));
			//save the header for the additional questions 
			if(!PrelimQController.getPrelimQnHeader(SurveyID).equals(""))
			{
				//header already exists update the header
				PrelimQController.updatePrelimQnHeader(SurveyID, header);
				
			}
			else
			{
				//header does not exist insert the new header
				PrelimQController.savePrelimQnHeader(SurveyID, header);
			
			}
		}
		
		
		//check and save each questions
		for(int i=0;i<QuestionCount+1;i++)
		{
			String qn = request.getParameter("qn"+i);
			if (qn!=null)
			{
				String qnid = request.getParameter("qid"+i);
				if (qnid!=null)
				{
					if(!qn.equals(""))
					{
						if(qnid.equals("0"))
						{
							if(request.getParameter("changeID"+i) != "")
								PrelimQController.saveQuestion(SurveyID, qn, Integer.parseInt(request.getParameter("changeID"+i)));
							else{
								PrelimQController.saveQuestion(SurveyID, qn, PrelimQController.getPrelimRatingID(Integer.parseInt(qnid)));
							}
						}
						else
						{
							if(request.getParameter("changeID"+i) != "")
								PrelimQController.updateQuestion(Integer.parseInt(qnid), SurveyID, qn, Integer.parseInt(request.getParameter("changeID"+i)));
							else{
								PrelimQController.updateQuestion(Integer.parseInt(qnid), SurveyID, qn, PrelimQController.getPrelimRatingID(Integer.parseInt(qnid)));
							}
						}
					}
				}
			}
		}
		
		if(request.getParameter("save").equals("1"))
		{
		
    out.write(_jsp_string7, 0, _jsp_string7.length);
    
		}
		else
		{
			v = PrelimQController.getQuestions(SurveyID);
		}
		//end of save
	}
	

	
	if(request.getParameter("delete") != null)
	{
		int delqid = Integer.parseInt(request.getParameter("delete"));
		int confirm = 0;
		if(request.getParameter("confirm") != null){
			confirm = Integer.parseInt(request.getParameter("confirm"));
		}
		if(PrelimQController.checkHavingAns(delqid) && confirm != 1){
			
    out.write(_jsp_string8, 0, _jsp_string8.length);
    out.print((trans.tslt("Answers related to this question will be removed, are you sure")));
    out.write(_jsp_string9, 0, _jsp_string9.length);
    out.print((delqid));
    out.write(_jsp_string10, 0, _jsp_string10.length);
    
		}else{
			PrelimQController.deleteQuestion(delqid);
			
    out.write(_jsp_string11, 0, _jsp_string11.length);
    
		}
	
	}
	if(request.getParameter("changeRating") != null)
	{
		int qid = Integer.parseInt(request.getParameter("changeRating"));
		int scaleID = Integer.parseInt(request.getParameter("value"));
		if(PrelimQController.checkRatingScaleExist(scaleID)){
			PrelimQController.changeRatingScaleID(qid,scaleID);
			
    out.write(_jsp_string12, 0, _jsp_string12.length);
    
		}
		else {
		
    out.write(_jsp_string13, 0, _jsp_string13.length);
    			
		}
		
	
	}
	
	
    out.write(_jsp_string14, 0, _jsp_string14.length);
    out.print(( trans.tslt("Survey Detail") ));
    out.write(_jsp_string15, 0, _jsp_string15.length);
    out.print((trans.tslt("Cluster")));
    out.write(_jsp_string16, 0, _jsp_string16.length);
    out.print(( trans.tslt("Competency") ));
    out.write(_jsp_string17, 0, _jsp_string17.length);
    out.print(( trans.tslt("Key Behaviour") ));
    out.write(_jsp_string18, 0, _jsp_string18.length);
    out.print(( trans.tslt("Demographics") ));
    out.write(_jsp_string19, 0, _jsp_string19.length);
    out.print(( trans.tslt("Rating Task") ));
    out.write(_jsp_string20, 0, _jsp_string20.length);
    out.print(( trans.tslt("Advanced Settings") ));
    out.write(_jsp_string21, 0, _jsp_string21.length);
    out.print(( trans.tslt("Preliminary Questions") ));
    out.write(_jsp_string22, 0, _jsp_string22.length);
    out.print(( trans.tslt("Additional Questions") ));
    out.write(_jsp_string23, 0, _jsp_string23.length);
    out.print((trans.tslt("Preliminary Questions Header")));
    out.write(_jsp_string24, 0, _jsp_string24.length);
     String h = PrelimQController.getPrelimQnHeader(SurveyID); 
		if(!h.equals(""))
		{
			out.print(h);
		}

    out.write(_jsp_string25, 0, _jsp_string25.length);
    out.print((trans.tslt("Preliminary Questions")));
    out.write(_jsp_string26, 0, _jsp_string26.length);
     

System.out.println("question count "+QuestionCount);

if(QuestionCount>0)
{
for(int x=0; x<QuestionCount;x++)
	{
	out.println("<tr>");
	out.println("<td>");
	out.print("<input type=\"hidden\" name=qid"+x+"");
	if(x<v.size()) //check if the answer is saved already, retrieve the question from db
	{
			out.print(" value="+v.get(x).getPrelimQnId());
	}
	else
	{
		out.print(" value=0");
	}
	out.println(" />");
	out.print("<input type=\"text\" name=qn"+x+" id=qn"+x+" size=50 ");
	if(x<v.size())
	{
		if(v.get(x)!=null)
		{
			out.print("value=\""+v.get(x).getQuestion()+"\"");
		}
	}
	out.println(" />");
	if(x<v.size())
	{
		int ratingScaleId = PrelimQController.getPrelimRatingID(v.get(x).getPrelimQnId());
		out.println("Rating Scale ID: ");
		out.print("<select name=changeID"+x+" id=changeID"+x+">");
		Vector scales = PrelimQController.getAllRatingScale();
		for(Object scale : scales){
			String selected = "";
			if(Integer.parseInt(scale.toString()) == ratingScaleId){
				selected = " selected";
			}
			out.print("<option value=\""+ scale.toString()+"\""+selected+">"+scale.toString()+"</option>");
		}
		out.println("</select>");
		//out.println("<INPUT type=button value=\"Change Rating\" onclick=\"ChangeRating(this.form,this.form.changeID"+x+","+v.get(x).getPrelimQnId()+")\"/>");
		out.println("<INPUT type=button value=Delete onclick=\"Delete(this.form, "+v.get(x).getPrelimQnId()+")\"/>");
	}else{
		
		//String defaultOption = "<option value=></option>";
		out.println("Rating Scale ID: ");
		out.print("<select name=changeID"+x+" id=changeID"+x+">");
		//out.print(defaultOption);
		Vector scales = PrelimQController.getAllRatingScale();
		for(Object scale : scales){
			String selected = "";
			out.print("<option value=\""+ scale.toString()+"\""+selected+">"+scale.toString()+"</option>");
		}
		out.println("</select>");
		//out.println("<INPUT disabled type=button value=\"Change Rating\"/>");
		out.println("<INPUT disabled type=button value=Delete />");
	}

	
	out.println("</td>");
	out.println("</tr>");
	}
}
else
{
	out.println("<tr>");
	out.println("<td>");
	out.println("<input type=\"hidden\" name=qid0 value=0 />");	
	out.println("<input type=\"text\" name=qn0 id=qn0 size=100 disabled/>");
	out.println("</td>");
	out.println("</tr>");
}
	
    out.write(_jsp_string27, 0, _jsp_string27.length);
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
    depend = new com.caucho.vfs.Depend(appDir.lookup("SurveyPrelimQ.jsp"), -8292948447725687752L, false);
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

  private final static char []_jsp_string24;
  private final static char []_jsp_string10;
  private final static char []_jsp_string3;
  private final static char []_jsp_string6;
  private final static char []_jsp_string20;
  private final static char []_jsp_string15;
  private final static char []_jsp_string23;
  private final static char []_jsp_string12;
  private final static char []_jsp_string21;
  private final static char []_jsp_string22;
  private final static char []_jsp_string16;
  private final static char []_jsp_string19;
  private final static char []_jsp_string18;
  private final static char []_jsp_string8;
  private final static char []_jsp_string11;
  private final static char []_jsp_string7;
  private final static char []_jsp_string0;
  private final static char []_jsp_string4;
  private final static char []_jsp_string9;
  private final static char []_jsp_string13;
  private final static char []_jsp_string5;
  private final static char []_jsp_string26;
  private final static char []_jsp_string2;
  private final static char []_jsp_string1;
  private final static char []_jsp_string14;
  private final static char []_jsp_string17;
  private final static char []_jsp_string27;
  private final static char []_jsp_string25;
  static {
    _jsp_string24 = "</b></font>\r\n</td>\r\n</tr>\r\n\r\n<tr><td>\r\n<textarea rows=5 cols=75 name=\"header\" id=\"header\">\r\n".toCharArray();
    _jsp_string10 = ";\r\n			}\r\n			</Script>\r\n			".toCharArray();
    _jsp_string3 = "\r\n\r\n\r\n\r\n<html>\r\n<head>\r\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\">\r\n<title>AdditionalQuestions</title>\r\n</head>\r\n<SCRIPT LANGUAGE=JAVASCRIPT>\r\n\r\nfunction Save(form){\r\n		form.action=\"SurveyPrelimQ.jsp?save=1\";\r\n		form.method=\"post\";\r\n		form.submit();	\r\n}\r\n\r\n/*\r\nfunction checkRatingScaleChanged(sel, ratingScaleID){\r\n	//int value = parseInt(sel.options[sel.selectedIndex].value);\r\n	/*if(isNan(value)){\r\n		return false;\r\n	}\r\n	if(value == ratingScaleID){\r\n		return false;\r\n	}\r\n	return true;\r\n}*/\r\n\r\n\r\nfunction Add(form){\r\n		form.action=\"SurveyPrelimQ.jsp?add=1&save=2\";\r\n		form.method=\"post\";\r\n		form.submit();\r\n}\r\n\r\nfunction RatingScale(form) {\r\n	myWindow=window.open('PrelimQ_AddRatingScale.jsp','windowRef','scrollbars=no, width=850, height=800');\r\n	myWindow.moveTo(50,50);\r\n    myWindow.location.href = 'PrelimQ_AddRatingScale.jsp';\r\n}\r\n\r\nfunction Delete(form, qid)\r\n{\r\n	if(confirm(\"".toCharArray();
    _jsp_string6 = " <font size=\"2\">   \r\n   <script>\r\n	parent.location.href = \"index.jsp\";\r\n	</script>\r\n".toCharArray();
    _jsp_string20 = "</a></font></b></td>\r\n	</tr>\r\n	<tr>\r\n		<td width=\"114\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"20\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"113\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"24\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"109\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"23\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"135\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\"><b><font face=\"Arial\" size=\"2\">\r\n		<a href=\"AdvanceSettings.jsp\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string15 = "</a></font></td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\"><b>\r\n		<font size=\"2\">\r\n		<img border=\"0\" src=\"images/gray_arrow.gif\" width=\"19\" height=\"26\"></font></b></td>\r\n		\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\">\r\n		<font face=\"Arial\" style=\"font-size: 10pt; font-weight: 700\" color=blue><u>\r\n		<a href=\"SurveyCluster.jsp\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string23 = "</a></font></b></td>\r\n	</tr>\r\n	<tr>\r\n		<td width=\"114\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"20\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"113\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"24\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"109\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"23\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"135\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n	</tr>\r\n</table>\r\n<form name=\"SurveyPrelimQ\" action=\"SurveyPrelimQ.jsp\" method=\"post\">\r\n<table border=1 bordercolor=\"#3399FF\">\r\n<tr>\r\n<td bgcolor=\"#000080\">\r\n<font color=\"white\"  face=\"Verdana\" style=\"font-weight: 700\" size=\"2\">\r\n<b>".toCharArray();
    _jsp_string12 = "\r\n			<Script>\r\n			alert(\"Changed successfully\")\r\n			</Script>\r\n			".toCharArray();
    _jsp_string21 = "</a></font></b>\r\n		\r\n		</td>\r\n	</tr>\r\n	<tr height=\"30\">\r\n		<td width=\"114\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"20\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"113\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"24\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"109\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"23\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"135\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\">&nbsp; <font size=\"2\">\r\n   \r\n		<span style=\"font-weight: 700\">\r\n		<font face=\"Arial\" style=\"font-size: 10pt; font-weight: 700\" color=\"#CC0000\">".toCharArray();
    _jsp_string22 = "</font></span>\r\n		</td>\r\n	</tr>\r\n		<tr height=\"30\">\r\n		<td width=\"114\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"20\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"20\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"113\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"24\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"109\" style=\"border-style: none; border-width: medium\">&nbsp;\r\n		</td>\r\n		<td width=\"23\" style=\"border-style: none; border-width: medium\">&nbsp;</td>\r\n		<td width=\"135\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\"><b><font face=\"Arial\" size=\"2\">\r\n		<a href=\"AdditionalQuestions.jsp?entry=1\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string16 = "</a></u></font></td>\r\n		<td width=\"28\" style=\"border-style: none; border-width: medium\"><b>\r\n		<font size=\"2\">\r\n		<img border=\"0\" src=\"images/gray_arrow.gif\" width=\"19\" height=\"26\"></font></b></td>\r\n		<td width=\"101\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\">\r\n		<font face=\"Arial\" style=\"font-size: 10pt; font-weight: 700\" size=\"2\">\r\n		<a href=\"SurveyCompetency.jsp\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string19 = "</a></font></b></td>\r\n		<td width=\"23\" style=\"border-style: none; border-width: medium\"><b>\r\n		<font size=\"2\">\r\n		<img border=\"0\" src=\"images/gray_arrow.gif\" width=\"19\" height=\"26\"></font></b></td>\r\n		<td width=\"135\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\"><b><font face=\"Arial\" size=\"2\">\r\n		<a href=\"SurveyRating.jsp\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string18 = "</a></font></b></td>\r\n		<td width=\"24\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\"><b>\r\n		<font size=\"2\">\r\n		<img border=\"0\" src=\"images/gray_arrow.gif\" width=\"19\" height=\"26\"></font></b></td>\r\n		<td width=\"109\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\"><b><font face=\"Arial\" size=\"2\">\r\n		<a href=\"SurveyDemos.jsp\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string8 = "\r\n			<Script>\r\n			if(confirm(\"".toCharArray();
    _jsp_string11 = "\r\n			<Script>\r\n			alert(\"Deleted successfully\")\r\n			location.href='SurveyPrelimQ.jsp?entry=1';\r\n			</Script>\r\n			".toCharArray();
    _jsp_string7 = "\r\n		<Script>\r\n		alert(\"Saved successfully\")\r\n		location.href='SurveyPrelimQ.jsp?entry=1';\r\n		</Script>\r\n		".toCharArray();
    _jsp_string0 = "  \r\n\r\n\r\n<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\r\n".toCharArray();
    _jsp_string4 = "?\"))\r\n	{\r\n		form.action=\"SurveyPrelimQ.jsp?delete=\"+qid;\r\n		form.method=\"post\";\r\n		form.submit();\r\n	}	\r\n}\r\n\r\nfunction ChangeRating(form,textbox,qid){\r\n	if(textbox.value == \"\"){\r\n		alert(\"Please enter valid input\");\r\n	}\r\n	else if(confirm(\"".toCharArray();
    _jsp_string9 = "?\"))\r\n			{\r\n				location.href='SurveyPrelimQ.jsp?confirm=1&delete='+".toCharArray();
    _jsp_string13 = "\r\n		<Script>\r\n		alert(\"The entered scale ID does not exist!\")\r\n		</Script>\r\n		".toCharArray();
    _jsp_string5 = "?\")){\r\n		form.action=\"SurveyPrelimQ.jsp?changeRating=\"+qid+\"&value=\"+textbox.value;\r\n		form.method=\"post\";\r\n		form.submit();\r\n	}\r\n}\r\n\r\n\r\n</SCRIPT>\r\n<body>\r\n".toCharArray();
    _jsp_string26 = "</b></font>\r\n</td>\r\n</tr>\r\n".toCharArray();
    _jsp_string2 = "\r\n".toCharArray();
    _jsp_string1 = "        \r\n".toCharArray();
    _jsp_string14 = "\r\n\r\n\r\n<font size=\"2\">\r\n   \r\n<table border=\"0\" width=\"78%\" cellspacing=\"0\" cellpadding=\"0\" bordercolor=\"#000000\" style=\"border-width: 0px\">\r\n	<tr>\r\n		<td width=\"114\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\">\r\n		<font face=\"Arial\" style=\"font-size: 10pt; font-weight: 700\">\r\n		<a href=\"SurveyDetail.jsp\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string17 = "</a></font></td>\r\n		<td width=\"20\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\"><b>\r\n		<font size=\"2\">\r\n		<img border=\"0\" src=\"images/gray_arrow.gif\" width=\"19\" height=\"26\"></font></b></td>\r\n		<td width=\"113\" style=\"border-style: none; border-width: medium\">\r\n		<p align=\"center\"><b><font face=\"Arial\" size=\"2\">\r\n		<a href=\"SurveyKeyBehaviour.jsp\" style=\"cursor:pointer\">".toCharArray();
    _jsp_string27 = "\r\n</table>\r\n<br/>\r\n<INPUT type=\"button\" value=\"Add\" onclick=\"Add(this.form)\"/> \r\n<INPUT type=\"button\" value=\"Save\" onclick=\"Save(this.form)\"/>\r\n<INPUT type=\"button\" value=\"Manage Rating Scale\" onclick=\"RatingScale(this.form)\"/> \r\n\r\n		</font>\r\n</form>\r\n</body>\r\n</html>".toCharArray();
    _jsp_string25 = "\r\n</textarea>\r\n</td></tr>\r\n\r\n</table>\r\n\r\n\r\n<br/>\r\n<br/>\r\n\r\n<table border=1 bordercolor=\"#3399FF\">\r\n\r\n<tr>\r\n<td bgcolor=\"#000080\">\r\n<font color=\"white\"  face=\"Verdana\" style=\"font-weight: 700\" size=\"2\">\r\n<b>".toCharArray();
  }
}