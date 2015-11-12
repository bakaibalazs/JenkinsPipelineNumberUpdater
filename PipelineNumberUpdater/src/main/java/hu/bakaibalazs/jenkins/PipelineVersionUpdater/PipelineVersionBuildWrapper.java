package hu.bakaibalazs.jenkins.PipelineVersionUpdater;

import hudson.Extension;
import hudson.Launcher;
import hudson.Proc;
import hudson.Launcher.ProcStarter;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
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

public class PipelineVersionBuildWrapper extends BuildWrapper {
	
	private static final String LATEST_GIT_TAG_COMMAND="git describe --abbrev=0";
	private static final String PROPERTY_FILE_NAME = "pipeline.properties";
	
	private static final Properties prop = new Properties();
	private static PrintStream logger=null;
	
	

	@DataBoundConstructor
	public PipelineVersionBuildWrapper() {
	}

	/**
	 * Save the latest git tag and the pipeline version into the property file.
	 * When a new git tag comes this plugin initialize the pipeline version to 1
	 * 
	 */
	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,BuildListener listener) throws IOException, InterruptedException {
		
		logger=listener.getLogger();
		logger.println("perform method invoked");		
					
		String propertyFilePath = build.getWorkspace() + File.separator+ PROPERTY_FILE_NAME;
		
		createPropertyFileIfNotExists(propertyFilePath);
		
		String savedPipelineBuildVersion = readFromPropertyFile(propertyFilePath,PropertyName.PIPELINE_BUILD_VERSION.name());
		String savedGitTag = readFromPropertyFile(propertyFilePath,PropertyName.LATEST_GIT_TAG.name());
		String currentGitTag = executeCommand(build, launcher, listener,LATEST_GIT_TAG_COMMAND);

		if(currentGitTag==null){
			logger.println("---YOU HAVE TO CREATE A GIT TAG BEFORE YOU USE THE PIPELINE VERSION UPDATER JENKINS PLUGIN---");
			return null;
		}
		
		
		logger.println("savedPipelineBuildVersion: " + savedPipelineBuildVersion);
		logger.println("savedGitTag: " + savedGitTag);
		logger.println("currentGitTag: " + currentGitTag);

		if (savedPipelineBuildVersion == null) {
			writeIntoPropertyFile(propertyFilePath, PropertyName.PIPELINE_BUILD_VERSION.name(), "1");
		} else {
			writeIntoPropertyFile(propertyFilePath, PropertyName.PIPELINE_BUILD_VERSION.name(),String.valueOf(Integer.valueOf(savedPipelineBuildVersion) + 1));
		}

		if (savedGitTag == null || !savedGitTag.equals(currentGitTag)) {
			writeIntoPropertyFile(propertyFilePath, PropertyName.LATEST_GIT_TAG.name(),currentGitTag);
		}

		if (savedGitTag != null && !savedGitTag.equals(currentGitTag)) {
			writeIntoPropertyFile(propertyFilePath, PropertyName.PIPELINE_BUILD_VERSION.name(), "1");
		}

		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)throws IOException, InterruptedException {
				return true;
			}
		};
	}

	private void createPropertyFileIfNotExists(String propertyFilePath) {
		
		try {
			File pf=new File(propertyFilePath);		
			if(!pf.exists())			
					pf.createNewFile();		
		} catch (IOException e) {
			logger.println(stackTraceToString(e));
		}
	}

	private String executeCommand(AbstractBuild build, Launcher launcher,BuildListener listener, String command) {
		assert logger!=null;
		
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
		assert logger!=null;
		
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
		assert logger!=null;
		
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
		assert e!=null;
		
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace()) {
			sb.append(element.toString());
			sb.append("\n");
		}
		return sb.toString();
	}		
	
	
	@Extension(ordinal=9999999999.0)
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Pipeline Version Updater";
		}
	}

}
