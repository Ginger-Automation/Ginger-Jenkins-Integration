package com.amdocs.ginger.jenkins.plugins;

import hudson.Launcher;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;


import ch.ethz.ssh2.StreamGobbler;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import jenkins.tasks.SimpleBuildStep;

import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

 /*************************************************************************/
 /*The class GingerBuilder is jenkins plugin for running Ginger on windows*/
 /*or unix                                                                */
 /*************************************************************************/
public class GingerBuilder extends Builder implements SimpleBuildStep {
	public static final String GINGER_PARAM_FILE_NAME = "GingerParam.txt";
	public static final int MINIMAL_SUMMARY_LEN = 45;
	private static Logger logger = Logger.getLogger(GingerBuilder.class.getName());
	
	private String				solutionFolder;
	private String				runSetName;
	private String				targetEnvCode;
	private String				gingerConsoleFolder;

	private String result = "Failure";
	
    /*****************************************************************************/
    /*  The jenkins plugin constracor that gets as input all the plugin variables*/
    /*****************************************************************************/
    @DataBoundConstructor
    public GingerBuilder(
				    		 String solutionFolder,
				    		 String runSetName,
				    		 String targetEnvCode,
				    		 String gingerConsoleFolder
    						 ) {

        this.solutionFolder = solutionFolder;
        this.runSetName = runSetName;
        this.targetEnvCode = targetEnvCode;
        this.gingerConsoleFolder = gingerConsoleFolder;
    }


   
    
    /*****************************************************************************/
    /*  The function override perform the Hudson run command                     */
    /*****************************************************************************/
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
 
    	String fileName = null; 
    	listener.getLogger().println("Solution folder: " + solutionFolder);
    	listener.getLogger().println("Run set name: " + runSetName);
    	listener.getLogger().println("Target environment: " + targetEnvCode);
    	
    	final EnvVars env = run.getEnvironment(listener);
        String dotnetVar = env.get("dotnet","dotnet "); 
   	    if (dotnetVar == null || "null".equals(dotnetVar) || "".equals(dotnetVar))
		   dotnetVar = "dotnet ";
   	    else
   	       dotnetVar += " ";
   	 //   listener.getLogger().println("dotnetVar-" + dotnetVar);
    	
   	    
   	    
    	int len = gingerConsoleFolder.length();
    	if (gingerConsoleFolder != null && len > 1)
    	{
    		if (gingerConsoleFolder.charAt(len-1) == '/' ||
    			gingerConsoleFolder.charAt(len-1) == '\\' )
    		  gingerConsoleFolder = gingerConsoleFolder.substring(0,len-1);
    	}
    	listener.getLogger().println("Ginger console folder: " + gingerConsoleFolder);
    	// Creates parameter file for Ginger
    	fileName = createParamFile(solutionFolder,runSetName,targetEnvCode,gingerConsoleFolder, listener);
    	
    	try {
 			executeShellCommand(gingerConsoleFolder,fileName,listener,dotnetVar);
 			
 		//	listener.getLogger().println("result-" + result);
 			if ("Failure".equals(result))
 				throw new AbortException("Run set completed with failure");
 			
		} 
        catch (AbortException e) {
			
			throw e;
			
		}
    	
    	
    	catch (Exception e) {
			
			e.printStackTrace();
			
		}
    }
    
    
    /*****************************************************************************/
    /*  The function performs plugin variables validation					     */
    /*****************************************************************************/
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

    	
    	public FormValidation doCheckSolutionFolder(@QueryParameter String value)
                throws IOException, ServletException {
            if (value == null || value.length() == 0)
                return FormValidation.error("Please set the solution folder name");
            
            return FormValidation.ok();
        }
    	
    	public FormValidation doCheckRunSetName(@QueryParameter String value)
                throws IOException, ServletException {
            if (value == null || value.length() == 0)
                return FormValidation.error("Please set the run set name");
            
            return FormValidation.ok();
        }
    	
    	public FormValidation doCheckTargetEnvCode(@QueryParameter String value)
                throws IOException, ServletException {
            if (value == null || value.length() == 0)
                return FormValidation.error("Please set the environment");
            
            return FormValidation.ok();
        }
    	
    	public FormValidation doCheckGingerConsoleFolder(@QueryParameter String value)
                throws IOException, ServletException {
            if (value == null || value.length() == 0)
                return FormValidation.error("Please set ginger console folder");
            
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Configure ginger execution";
        }
    }
    /*****************************************************************************/
    /*  The function gets the absolute path					   				     */
    /*****************************************************************************/
    private String getAbsolutePath()
    {
    	String curDir = System.getProperty("user.dir");
    	if (curDir != null && curDir.length() > 0)
    		curDir = curDir.substring(0,curDir.length()-1);
    	
    	return curDir;
    }
	
    /*****************************************************************************/
    /*  The function creates configuration file for Ginger that includes         */
    /*  all the plugin variable values     										 */
    /*****************************************************************************/
    public String createParamFile(String solutionFolder,String runSetName,
    		String targetEnvCode,String gingerConsoleFolder, TaskListener listener) throws IOException  {
    	
    	File file;
    	Writer w;
    	PrintWriter pw = null;
    	String filename = null;
    	
    	String dateStr;
    	Date date = new Date();
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    	Calendar c = Calendar.getInstance();
    	c.setTime(date);
    	String year = String.valueOf(c.get(Calendar.YEAR));
    	String month = String.valueOf(c.get(Calendar.MONTH)+1);
    	String day = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
    	String hour = String.valueOf(c.get(Calendar.HOUR));
    	String minute =String.valueOf(c.get(Calendar.MINUTE));
    	String second = String.valueOf(c.get(Calendar.SECOND));
    	dateStr =  year + month + day + hour + minute + second + "-";    	
    	
        try{
        	String configDir = gingerConsoleFolder + "/config"; 
        	createConfigFolder(configDir);
         	filename = configDir + "/" + dateStr +  GINGER_PARAM_FILE_NAME;
   //     	listener.getLogger().println("filename: " + filename);
        	file = new File(filename);
        	w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_16.name());
        	pw = new PrintWriter(w);
        	
        	
			if (solutionFolder != null)
				pw.println("Solution folder=" + solutionFolder);
			if (runSetName != null)
				pw.println("Run set name=" + runSetName);
			if (targetEnvCode != null)
				pw.println("Target environment=" + targetEnvCode);
			if (gingerConsoleFolder != null)
				pw.println("Ginger console folder=" + gingerConsoleFolder);
			
			listener.getLogger().println("File " + filename + " was created");
        }
        catch(IOException  e)
        {
      	  e.printStackTrace();
      	  throw e;
        }
        finally{
      	    if (pw != null)
      	    	pw.close();
        }
        return filename;
     }
    /******************************************************************************/
    /*  The function creates config folder in case it doesn't exists              */
    /*                 															  */
    /******************************************************************************/
    private void createConfigFolder(String directoryName)
      {
    	  try
    	  {
    		  File directory = new File(directoryName);
    		    if (! directory.exists()){
    		        directory.mkdir();
     		    }
    	  }
    	  catch(Exception e)
    	  {
    		  throw e;
    	  }
    	  
      }
      /*****************************************************************************/
      /*  The function creates config folder if it does not exists                 */
      /*  Works for both OS Windows and Unix using dotnet command                  */
      /*****************************************************************************/
     public void executeShellCommand(String gingerConsolFolder,String fileName,TaskListener listener,String dotnetVar) throws Exception
     {
    	 String command;
    	 try
    	 {
    		// runUnixCommand(listener);
             

    		 
       		 command =  dotnetVar + gingerConsolFolder + "/GingerConsole.dll " + fileName;
       		 // gingerConsolFolder = /home/ginger/jenkins/plugin/gingerconsole
    	     //	 command =  findDotnetPath(listener) + gingerConsolFolder + "/GingerConsole.dll " + fileName;
    		 listener.getLogger().println("command-" + command);
     		  
    		 ProcessBuilder builder = new ProcessBuilder();
    		 if (isUnix())
    		 {
		       builder.command("/bin/bash","-c",command);   //ksh
			   builder.directory(new File(System.getProperty("user.home")));
    		 }
    		 else if (isWindows())
    		 {
    			 builder.command("cmd","/c",command);   
    		 }
			Process process = builder.start();
			StreamGobbler streamGobbler = 
			  new StreamGobbler(process.getInputStream(), listener);
			Executors.newSingleThreadExecutor().submit(streamGobbler);
			int exitCode = process.waitFor();
    		 
    		 
    	 }
    	 catch(Exception e)
    	 {
    		 listener.getLogger().println(e.getMessage());
    		 e.printStackTrace();
    		 throw e;
    		 
    	 }
    	 
     }
     /*****************************************************************************/
     /*  Check if the OS is windows							                      */
     /*                													          */
     /*****************************************************************************/
     private boolean isWindows()
     {
    	 String os = System.getProperty("os.name").toLowerCase();
    	 if (os.contains("windows"))
    		 return true;
    	 else
    		 return false;
     }
     /*****************************************************************************/
     /*  Check if the OS is unix							                      */
     /*                													          */
     /*****************************************************************************/
     private boolean isUnix()
     {
    	 String os = System.getProperty("os.name");
    	 if ("Linux".equals(os) || "Unix".equals(os))
    		 return true;
    	 else
    		 return false;
     }
     /*****************************************************************************/
     /*   The function finds the dotnet path					             	  */
     /*                   														  */
     /*****************************************************************************/
     public String findDotnetPath(TaskListener listener) throws Exception
     {
         String dotnetPath = "dotnet ";
    	 try
    	 {
    	   if (isUnix())
    	   {
    		listener.getLogger().println("Unix environment");   
       		ProcessBuilder builder = new ProcessBuilder();
        	  builder.command("/bin/bash","-c","echo $dotnet");  //ksh  whereis dotnet
       		builder.directory(new File(System.getProperty("user.home")));
       		Process process = builder.start();
       		int exitCode = process.waitFor();
       		dotnetPath = output(process.getInputStream(),listener); 
    	   }
    	   else if (isWindows())
    	   {
    		   listener.getLogger().println("Windows environment");   
    	   }
    	 }
    	 catch(Exception e)
    	 {
    		 listener.getLogger().println("Failed to find dotnet command");
    		 e.printStackTrace();
    	 }
    	 return dotnetPath;
     }
     private static String output(InputStream inputStream,TaskListener listener) throws IOException {
         StringBuilder sb = new StringBuilder();
         String dotnetPath = "";
         BufferedReader br = null;
         try {
             br = new BufferedReader(new InputStreamReader(inputStream));
             String line = null;
             while ((line = br.readLine()) != null) {
                 sb.append(line);
             }
             listener.getLogger().println(sb.toString());
             if (sb != null && sb.length() == 1 )
    		 {
            	 dotnetPath = sb.toString();
    			 if (dotnetPath.startsWith("dotnet:"))
    			 {
    			   listener.getLogger().println("dotnetPath-" + dotnetPath);
    			   dotnetPath = dotnetPath.substring(7,dotnetPath.length()); 
    			   dotnetPath += "/";
    			   listener.getLogger().println("dotnetPath=" + dotnetPath);
    			 }
    		 }
         } finally {
        	 if (br != null)
             br.close();
         }
         return sb.toString();
       //  return "/home/ginger/dotnet/dotnet  ";
     }

     

     
     
     /*****************************************************************************/
     /* The function runs unix shell command				  		          	  */
     /*                   														  */
     /*****************************************************************************/
  
     private void runUnixCommand(TaskListener listener) throws Exception
     {
    	 try
    	 {
    		listener.getLogger().println("----------------Start----------------");
    		listener.getLogger().println("run - whereis dotnet");
    		ProcessBuilder builder = new ProcessBuilder();
    		     // builder.command("/bin/bash","-c","whereis dotnet");
    		     builder.command("/bin/bash","-c","whereis dotnet");
    		builder.directory(new File(System.getProperty("user.home")));
   // 		builder.redirectErrorStream(true);
    		Process process = builder.start();
    //		IOUtils.copy(process.getInputStream(), System.out);
    		StreamGobbler streamGobbler = 
    		  new StreamGobbler(process.getInputStream(), listener);
    		Executors.newSingleThreadExecutor().submit(streamGobbler);
    		int exitCode = process.waitFor();
    		listener.getLogger().println("exitCode-" + exitCode);
    		listener.getLogger().println("---------------End----------------");
    	 }
    	 catch(Exception e)
    	 {
    		 listener.getLogger().println("Failed to execute shell command");
    		 e.printStackTrace();
    		 
    	 } 
     }
     
     /*****************************************************************************/
     /*   The function is listening for the process command  result               */
     /*                                                                           */
     /*****************************************************************************/
     private  class StreamGobbler implements Runnable {
         private InputStream inputStream;
         private TaskListener listener;
      
         public StreamGobbler(InputStream inputStream, TaskListener listener) {
             this.inputStream = inputStream;
             this.listener = listener;
         }
          
         
         public void run() {
         	
         	String line = null;

         	String summaryStr = "*** Test Summary:";
         	BufferedReader br =   new BufferedReader(new InputStreamReader(inputStream));
         	try {
     			while ((line = br.readLine())!=null)
     			{
     				listener.getLogger().println(line);
     				
 				    if (line.startsWith(summaryStr))
 				    {
 				      if (line.length() >= MINIMAL_SUMMARY_LEN)	
 				        printSummary(listener,line.substring(summaryStr.length(), line.length()));
 				      else
	     			    listener.getLogger().println("Invalid summary syntax");	
 				    }
      			}
      		} catch (IOException e) {
     			listener.getLogger().println(e.getMessage());
     			e.printStackTrace();
     		}
         	try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
         }
     }
    		
     /*****************************************************************************/
     /*   The function check the validation of the test summary and print it      */
     /*                   														  */
     /*****************************************************************************/
  	 // *** Test Summary: Pass:2, Fail:3, Total:5 ***	
     private void printSummary(TaskListener listener,String line)
     {
    	 boolean valid = true;
    	 String passStr  = " Pass:";
    	 String faileStr = " Fail:";
    	 String totalStr = " Total:";
    	 int pass = 0;
    	 int fail = 0 ;
    	 int total = 0;
    	 StringBuffer numStr = new StringBuffer();
    	 try
    	 {
    		 // pass
    		 if (line.startsWith(passStr)){
    			line = line.substring(passStr.length(),line.length());
    			numStr = getNum(line,',');
    			if (numStr == null)
    				valid = false;
    			else{
    				line = line.substring(numStr.length()+1,line.length());
    				pass = Integer.parseInt(numStr.toString());
    			}
    			numStr = new StringBuffer();
    		 }
    		 else 
    			 valid = false;
    		 // Fail
    		 if (valid && line.startsWith(faileStr)){
     			line = line.substring(faileStr.length(),line.length());
     			numStr = getNum(line,',');
     			if (numStr == null)
     				valid = false;
     			else{
     				line = line.substring(numStr.length()+1,line.length());
     				fail = Integer.parseInt(numStr.toString());
     			}
     			numStr = new StringBuffer();
     		 }
     		 else 
     			 valid = false;
    		 // Total
    		 if (valid && line.startsWith(totalStr)){
      			line = line.substring(totalStr.length(),line.length());
      			numStr = getNum(line,' ');
      			if (numStr == null)
      				valid = false;
      			else{
      				line = line.substring(numStr.length()+1,line.length());
      				total = Integer.parseInt(numStr.toString());
      			}
      			numStr = new StringBuffer();
      		 }
      		 else 
      			 valid = false;
    		 if (!valid)
    			 listener.getLogger().println("Invalid summary syntax");
    	/*	 else
    		 {
    			 listener.getLogger().println("Total summary:");
    			 listener.getLogger().println("--------------");
    			 listener.getLogger().println("Passed: " + pass);
    			 listener.getLogger().println("Failed: " + fail);
    			 listener.getLogger().println("Total:  " + total);
    		 }
    	  */	 
    		 if (fail == 0)
    			 result = "Success";
    		 else
    			 result = "Failure"; 
     	 }
    	 catch(Exception e)
    	 {
    		 if (valid == false)
    		   listener.getLogger().println("Invalid summary syntax");
    	 }
     }
     
   private StringBuffer getNum(String line, char stopChar )
   {
	 StringBuffer numStr = new StringBuffer();  
	 try
	 {
	   
	   for(int i=0;i<line.length() && line.charAt(i) != stopChar; i++)
	   {
		  if (line.charAt(i) >= '0' && line.charAt(i) <= '9')
			  numStr.append(line.charAt(i));
		  else
		  {
			  numStr = null;
			  break;
		  }
		}
	    if (numStr != null && numStr.length() == 0)
	    	numStr = null;
	 }
	 catch(Exception e)
  	 {
		 numStr = null;
  	 }
	 return numStr;
   }
     

	public String getRunSetName() {
		return runSetName;
	}





	public String getSolutionFolder() {
		return solutionFolder;
	}





	public void setSolutionFolder(String solutionFolder) {
		this.solutionFolder = solutionFolder;
	}





	public String getGingerConsoleFolder() {
		return gingerConsoleFolder;
	}





	public void setGingerConsoleFolder(String gingerConsoleFolder) {
		this.gingerConsoleFolder = gingerConsoleFolder;
	}





	public void setRunSetName(String runSetName) {
		this.runSetName = runSetName;
	}





	public String getTargetEnvCode() {
		return targetEnvCode;
	}





	public void setTargetEnvCode(String targetEnvCode) {
		this.targetEnvCode = targetEnvCode;
	}






	
    
    
    
    

}
