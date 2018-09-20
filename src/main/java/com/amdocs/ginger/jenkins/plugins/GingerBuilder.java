package com.amdocs.ginger.jenkins.plugins;

import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractProject;
import hudson.model.ModelObject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
//import java.io.File;
//import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
//import java.io.PrintWriter;
//import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import jenkins.tasks.SimpleBuildStep;

import org.apache.log4j.Logger;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundSetter;

   


public class GingerBuilder extends Builder implements SimpleBuildStep {
	public static final String GINGER_PARAM_FILE_NAME = "GingerParam.txt";
	private static Logger logger = Logger.getLogger(GingerBuilder.class.getName());
	
	private String				solutionFolder;
	private String				runSetName;
	private String				targetEnvCode;
	private String				gingerConsoleFolder;


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


   
    

  //  @DataBoundSetter
  //  public void setUseFrench(boolean useFrench) {
  //      this.useFrench = useFrench;
  //  }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
 
    	String fileName = null; 
    	listener.getLogger().println("Solution folder: " + solutionFolder);
    	listener.getLogger().println("Run set name: " + runSetName);
    	listener.getLogger().println("Target environment: " + targetEnvCode);
    	listener.getLogger().println("Ginger console folder: " + gingerConsoleFolder);
    	fileName = createParamFile(solutionFolder,runSetName,targetEnvCode,gingerConsoleFolder, listener);
    	
    	try {
    		//listener.getLogger().println("dotnet /home/ginger/ginger_shell/publish/GingerShellPluginConsole.dll");
			executeShellCommand(gingerConsoleFolder,fileName,listener);
		} catch (Exception e) {
			
			e.printStackTrace();
		}
    }


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

    private String getAbsolutePath()
    {
    	String curDir = System.getProperty("user.dir");
    	if (curDir != null && curDir.length() > 0)
    		curDir = curDir.substring(0,curDir.length()-1);
    	
    	return curDir;
    }
	

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
        //	String curDir = System.getProperty("user.dir");
        	String curDir = "/home/ginger/jenkins/plugin"; //getAbsolutePath();
        	listener.getLogger().println("curDir: " + curDir);
        	filename = curDir + "/" + dateStr +  GINGER_PARAM_FILE_NAME;
        	listener.getLogger().println("filename: " + filename);
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
      	  logger.error("Failed to create ginger parameter file");
      	  e.printStackTrace();
      	  throw e;
        }
        finally{
      	    if (pw != null)
      	    	pw.close();
        }
        return filename;
     }

     public void executeShellCommand(String gingerConsolFolder,String fileName,TaskListener listener) throws Exception
     {
    	 BufferedReader b = null;
    	 try
    	 {
    		 Runtime r = Runtime.getRuntime();
    		//  dotnet GingerConsole.dll filename
    		 //variable: 
    		 // path for the GingerConsole.dll
    		 // filename - needs to be unique for ever exec
    		 
    		 //Process p = r.exec("dotnet " + gingerConsolFolder + "GingerConsole.dll " + fileName);
    		 //dotnet /home/ginger/ginger_shell/publish/GingerShellPluginConsole.dll
    		 Process p = r.exec("dotnet " + gingerConsolFolder + "GingerShellPluginConsole.dll " + fileName);
    		 p.waitFor();
    		 b = new BufferedReader(new InputStreamReader(p.getInputStream()));
    		 String line = "";
    		 while ((line = b.readLine()) != null) {
    		   System.out.println(line);
    			 listener.getLogger().println("line");
    			 logger.info(line);
    		 }
    	 }
    	 catch(Exception e)
    	 {
    		 logger.error("Faile to execute shell command");
    		 e.printStackTrace();
    		 throw e;
    		 
    	 }
    	 finally{
    		 if (b != null)
    			 b.close();
    	 }
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
