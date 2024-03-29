<%// Author: Dai Yong in June 2013%>
<%@ page import="java.sql.*,
                 java.io.*,
                 javazoom.upload.*,
				 java.util.*,
				 CP_Classes.vo.*"
				 %>
				 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html">
<%@ page pageEncoding="UTF-8"%>
<style type="text/css">
<!--
body {
	background-color: #eaebf4;
}
.style3 {color: #000066; font-weight: bold; }
-->
</style>
</head>

<title>Update Coach info</title>
<meta http-equiv="Content-Type" content="text/html">

<jsp:useBean id="upBean" scope="page" class="javazoom.upload.UploadBean" >
  <jsp:setProperty name="upBean" property="folderstore"/>
</jsp:useBean>

<jsp:useBean id="org" class="CP_Classes.Organization" scope="session"/>
<jsp:useBean id="logchk" class="CP_Classes.Login" scope="session"/>  
<jsp:useBean id="setting" class="CP_Classes.Setting" scope="session"/>
<jsp:useBean id="Coach" class="Coach.Coach"scope="session" />
<jsp:useBean id="LoginStatus" class="Coach.LoginStatus" scope="session" />

<body>

<%
	int CoachID;
	if(request.getParameter("UploadCoachInfo")!=null){
	CoachID=Integer.parseInt(request.getParameter("UploadCoachInfo"));
	LoginStatus.setSelectedCoach(CoachID);
	}
	else{
	CoachID=LoginStatus.getSelectedCoach();
	}
	voCoach coach=Coach.getSelectedCoach(CoachID);
	String CoachName=coach.getCoachName();

	
	if (MultipartFormDataRequest.isMultipartFormData(request)) {
        // Uses MultipartFormDataRequest to parse the HTTP request.
        MultipartFormDataRequest mrequest = new MultipartFormDataRequest(request);
        String todo = null;
        if (mrequest != null) todo = mrequest.getParameter("todo");
		if ((todo != null) && (todo.equalsIgnoreCase("upload"))) {
                Hashtable files = mrequest.getFiles();
				upBean.setFolderstore(setting.getCoachFilePath());
				upBean.setOverwrite(true);
				
                if ( (files != null) && (!files.isEmpty()) )
                {
                    UploadFile file = (UploadFile) files.get("uploadfile");
                    // Uses the bean now to store specified by jsp:setProperty at the top.

                    if (file != null)
	                {
						String sFile = file.getFileName();
						String sFileCopy = "";
						if(sFile != null)
							sFileCopy = sFile.toLowerCase();
						
						if(sFile != null) {
							if(sFileCopy.indexOf("pdf")!= -1 || sFileCopy.indexOf("doc")!= -1 ) {
								boolean bIsUpdated = Coach.editUploadFile(CoachID,  file.getFileName());
													
								if(bIsUpdated) {
%>
							<script>
								alert("Updated successfully");
								window.close();
								opener.location.href = "Coach.jsp";
							</script>
<%						
								} 
							
							} else {
%>	
							<script>
								alert("Format is not supported. Formats suported are .pdf/.doc");
							</script>
<%	
							}
						} else {
%>	
							<script>
								alert("Please choose a .pdf/.doc file to upload");
							</script>
<%							
						}
					}

                    upBean.store(mrequest, "uploadfile");
                }
                else
                {
                  out.println("<li>No uploaded files");
                }
		} else out.println("<BR> todo="+todo);
    }
%>

<form method="post" action="UploadCoachFile.jsp" name="upform" enctype="multipart/form-data" onsubmit="confirm('Upload File?')">
<table width="392" border="0" style='font-size:10.0pt;font-family:Arial'>
  <tr>
    <td width="97" align="left"><span class="style3">Coach Name:</span></td>
    <td width="285" align="left"><span class="style3"><%=CoachName%></span></td>
  </tr>
</table>
<p></p>
<p style='font-size:10.0pt;font-family:Arial'>Please click the browse button and choose the .pdf/.doc file to be imported </p>
<input name="uploadfile" type="file" size="50">
<p></p>
<input type="hidden" name="todo" value="upload">
<input type="submit" name="Submit" value="Upload">
<input type="reset" name="Reset" value="Cancel" onClick=window.close()>
</form>

</body>
</html>
