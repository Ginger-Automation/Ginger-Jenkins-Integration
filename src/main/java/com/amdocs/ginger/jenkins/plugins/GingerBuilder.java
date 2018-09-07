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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;

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


    @DataBoundConstructor
    public GingerBuilder(
				    		 String solutionFolder,
				    		 String runSetName,
				    		 String targetEnvCode
    						 ) {

        this.solutionFolder = solutionFolder;
        this.runSetName = runSetName;
        this.targetEnvCode = targetEnvCode;
          

    }


   
    

  //  @DataBoundSetter
  //  public void setUseFrench(boolean useFrench) {
  //      this.useFrench = useFrench;
  //  }

    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {

    	listener.getLogger().println("Solution folder: " + solutionFolder);
    	listener.getLogger().println("Run set name: " + runSetName);
    	listener.getLogger().println("Target Env: " + targetEnvCode);
    	createParamFile(solutionFolder,runSetName,targetEnvCode, listener);
    	
    //	executeShellCommand();
    }

 //   @Symbol("greet")
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
    	
    	

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Ginger Plugin"; //Messages.HelloWorldBuilder_DescriptorImpl_DisplayName();
        }
    }

	

    public void createParamFile(String solutionFolder,String runSetName,String targetEnvCode,TaskListener listener) throws IOException  {
    	
    	File file;
    	Writer w;
    	PrintWriter pw = null;
    	String filename;
        try{
        	String curDir = System.getProperty("user.dir");
        	listener.getLogger().println("curDir: " + curDir);
        	filename = curDir + "/" + GINGER_PARAM_FILE_NAME;
        	file = new File(filename);
        	w = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_16.name());
        	pw = new PrintWriter(w);
        	
        	
			if (solutionFolder != null)
				pw.println("SolutionFolder=" + solutionFolder);
			if (runSetName != null)
				pw.println("RunSetName=" + runSetName);
			if (targetEnvCode != null)
				pw.println("TargetEnv=" + targetEnvCode);
			
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

     }

  /*   public void executeShellCommand()
     {
    	 BufferedReader b = null;
    	 try
    	 {
    		 Runtime r = Runtime.getRuntime();
    		 Process p = r.exec("uname -a");
    		 p.waitFor();
    		 b = new BufferedReader(new InputStreamReader(p.getInputStream()));
    		 String line = "";
              //  dotnet GingerConsole.dll filename 
    		 while ((line = b.readLine()) != null) {
    		   System.out.println(line);
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
*/

	public String getSolutionFolder() {
		return solutionFolder;
	}





	public void setSolutionFolder(String solutionFolder) {
		this.solutionFolder = solutionFolder;
	}





	public String getRunSetName() {
		return runSetName;
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
