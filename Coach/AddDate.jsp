<%// Author: Dai Yong in June 2013%>
<%@ page import = "java.sql.*" %>
<%@ page import = "java.io.*" %>
<%@ page import = "java.util.*" %>
<%@ page import = "java.text.SimpleDateFormat" %>
<%@ page import = "CP_Classes.vo.*" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

<title>Add Coaching Date</title>

<body style="background-color: #DEE3EF">
<jsp:useBean id="Database" class="CP_Classes.Database" scope="session"/>
<jsp:useBean id="logchk" class="CP_Classes.Login" scope="session"/>   
<jsp:useBean id="CoachDate" class="Coach.CoachDate"scope="session" />
<jsp:useBean id="CoachDateGroup" class="Coach.CoachDateGroup"scope="session" />
<jsp:useBean id="LoginStatus" class="Coach.LoginStatus" scope="session" />
<link rel="stylesheet" type="text/css" media="all" href="jsDatePick_ltr.min.css" />

<script type="text/javascript" src="jsDatePick.min.1.3.js"></script>
<script type="text/javascript">
	window.onload = function(){
		new JsDatePick({
			useMode:2,
			target:"inputField",
			dateFormat:"%d-%M-%Y"
		});
	};
</script>


<script language = "javascript">
function confirmAddDate(form,field)
{
	
	if(field.value != "") {
		if(confirm("Add Coaching Date?")) {
			form.action = "AddDate.jsp?add=1";
			form.method = "post";
			form.submit();
			return true;
		}else
			return false;
	} else {
		if(field.value == "") {
			alert("Please enter coaching date");
			form.Date.focus();
		}
		return false;
	}
	return true;
}

function cancelAdd()
{
	window.close();
}

function confirmDelete(){
var agree=confirm("Are you sure you want to add date before today?");
	if(agree){
 		return true;
	}
		else{ 
	return false;
	}
}

</script>

<%
String username=(String)session.getAttribute("username");
int FKCoachDateGroup=LoginStatus.getSelectedDateGroup();

  if (!logchk.isUsable(username)) 
  {
%>
   
<script>
	parent.location.href = "../index.jsp";
</script> <%
 	} 
    else {
 		//System.out.println("Starting Time: "+request.getParameter("StartingTime"));
 		//System.out.println("Ending Time: "+request.getParameter("EndingTime"));
 		if (request.getParameter("add") != null) {
 			if (request.getParameter("inputField") != "") {
 				String date = request.getParameter("inputField");
 				Calendar cal = Calendar.getInstance();
 				Calendar today = Calendar.getInstance();
 				SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");
 				cal.setTime(sdf.parse(date));// all done
 				boolean beforeToday = cal.before(today);

 				System.out.println("date: " + beforeToday);
 				if (beforeToday) {
 					%> <script>
						if(confirmDelete()){
						}else{
						opener.location.reload(false);
						}
					</script> 
					<%
 				}
 				boolean Exist = false;
 				//check time valid
 				Vector v = CoachDate.getAllDate(FKCoachDateGroup);
 				for (int i = 0; i < v.size(); i++) {
 					voCoachDate vo = (voCoachDate) v.elementAt(i);

 					String dates = vo.getDate();
 					//System.out.println("Existing Schedule Name:"+DateGroupName);
 					if (dates.trim().equalsIgnoreCase((date.trim()))) {
 						Exist = true;
 						//System.out.println("Same Coaching Date Exists");
 					}

 				}

 				if (!Exist) {
 					try {
 						boolean add = CoachDate.addDate(FKCoachDateGroup, date);

 						if (add) {LoginStatus.setSelectedDateGroup(FKCoachDateGroup);
 %> <script>
		opener.location.href = "DateGroup.jsp";
		window.close();
	</script> <%
 	} else {
 						}
 					} catch (Exception SE) {
 						System.out.println(SE);
 					}
 				} else {
 %> <script>
	alert("Same Coaching Date exists");
	window.location.href = 'AddDate.jsp';
</script> <%
 	}

 			} // end of input is valid

 		}//end of adding
 %>

<form name="AddDate" method="post">
	<p>	
		<br>
			<b><font color="#000080" size="3" face="Arial">Add Coaching Date</font></b>
		<br>
	</p>
  <table border="0" width="480"style='font-size:10.0pt;font-family:Arial'>
    <tr>
      <td width="120" height="33">Coaching Date:</td>
      <td width="200" height="33">
       <input name="inputField" type="text" size="12" id="inputField"  style='font-size:10.0pt;font-family:Arial' id="Date" size="10" maxlength="50"/>
	  </td>
    </tr>
   
  </table>
  <blockquote>
    <blockquote>
      <p>
		<font face="Arial">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
		</font>		<font face="Arial" span style="font-size: 10.0pt; font-family: Arial">		
	        <input class="btn btn-primary" type="button" name="Submit" value="Submit" onClick="confirmAddDate(this.form,this.form.inputField)"></font><font span style='font-family:Arial'>	    
		</font>
			<font face="Arial">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
        </font>
		<font face="Arial" span style="font-size: 10.0pt; font-family: Arial">
			<input name="Cancel" class="btn btn-primary" type="button" id="Cancel" value="Cancel" onClick="cancelAdd()">
			</font> </p>
    </blockquote>
  </blockquote>
</form>
<% } %>
</body>
</html>