package hu.bakaibalazs.jenkins.PipelineNumberUpdater;

import hudson.Extension;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.kohsuke.stapler.DataBoundConstructor;

public class PipelineNumberBuilder extends Builder {

	private static final String LATEST_GIT_TAG_COMMAND="git describe --abbrev=0";
	private static final String PROPERTY_FILE_NAME = "pipeline-version.properties";
	
	private static final Properties prop = new Properties();
	private static PrintStream logger=null;

	@DataBoundConstructor
	public PipelineNumberBuilder() {
	}

	
	/**
	 * Save the latest git tag and the pipeline version into the property file
	 * When a new git tag comes this plugin initialize the pipeline version to 1
	 * 
	 */
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,BuildListener listener) throws IOException {
		logger=listener.getLogger();
		logger.println("perform method invoked");

		String propertyFilePath = build.getWorkspace() + File.separator+ PROPERTY_FILE_NAME;
		String savedPipelineVersion = readFromPropertyFile(propertyFilePath,PropertyName.PIPELINE_NUMBER.name());
		String savedGitTag = readFromPropertyFile(propertyFilePath,PropertyName.LATEST_GIT_TAG.name());
		String currentGitTag = executeCommand(build, launcher, listener,LATEST_GIT_TAG_COMMAND);

		if(currentGitTag==null){
			logger.println("YOU HAVE TO CREATE A GIT TAG BEFORE YOU USE THIS PLUGIN");
			return false;
		}
		
		
		logger.println("savedPipelineVersion: " + savedPipelineVersion);
		logger.println("savedGitTag: " + savedGitTag);
		logger.println("currentGitTag: " + currentGitTag);

		if (savedPipelineVersion == null) {
			writeIntoPropertyFile(propertyFilePath, PropertyName.PIPELINE_NUMBER.name(), "1");
		} else {
			writeIntoPropertyFile(propertyFilePath, PropertyName.PIPELINE_NUMBER.name(),String.valueOf(Integer.valueOf(savedPipelineVersion) + 1));
		}

		if (savedGitTag == null || !savedGitTag.equals(currentGitTag)) {
			writeIntoPropertyFile(propertyFilePath, PropertyName.LATEST_GIT_TAG.name(),currentGitTag);
		}

		if (savedGitTag != null && !savedGitTag.equals(currentGitTag)) {
			writeIntoPropertyFile(propertyFilePath, PropertyName.PIPELINE_NUMBER.name(), "1");
		}

		return true;
	}

	private String executeCommand(AbstractBuild build, Launcher launcher,BuildListener listener, String command) {

		OutputStream out = new ByteArrayOutputStream();
		ArgumentListBuilder cmd = new ArgumentListBuilder();		
		ProcStarter ps = launcher.new ProcStarter();
		cmd.addTokenized(command);
		ps = ps.cmds(cmd).stdout(out);
		try {
			ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
			Proc proc = launcher.launch(ps);
			proc.join();
			return new String(((ByteArrayOutputStream) out).toByteArray(),"UTF-8").replace("\n", "").replace("\r", "");
		} catch (Exception e) {
			logger.println(stackTraceToString(e));
			return "";
		}
	}

	private String readFromPropertyFile(String propertyFilePath, String name) {
		InputStream input = null;
		try {
			prop.load(new FileInputStream(propertyFilePath));
			return prop.getProperty(name);
		} catch (IOException ex) {
			logger.println(stackTraceToString(ex));
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					logger.println(stackTraceToString(e));
				}
			}
		}
	}

	private void writeIntoPropertyFile(String propertyFilePath, String name,String value) {
		OutputStream output = null;
		try {
			output = new FileOutputStream(propertyFilePath);

			prop.setProperty(name, value);
			prop.store(output, null);
		} catch (IOException io) {
			logger.println(stackTraceToString(io));
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					logger.println(stackTraceToString(e));
				}
			}
		}
	}
	
	private String stackTraceToString(Throwable e) {
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString());
			sb.append("\n");
		}
		return sb.toString();
	}	

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return "Pipeline Number Updater";
		}
	}
}
