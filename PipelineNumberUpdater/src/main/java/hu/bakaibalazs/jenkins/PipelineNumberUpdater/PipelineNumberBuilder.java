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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Properties;
import java.util.Scanner;

import org.kohsuke.stapler.DataBoundConstructor;

public class PipelineNumberBuilder extends Builder {

	private static final String propertyFileName = "pipeline-version.properties";
	private static final Properties prop = new Properties();

	@DataBoundConstructor
	public PipelineNumberBuilder() {
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException {
		listener.getLogger().println("---perform---");

		String propertyFilePath = build.getWorkspace() + File.separator
				+ propertyFileName;

		String currentPipelineVersion = readFromPropertyFile(propertyFilePath,"PIPELINE_NUMBER");
		String latestGitTag = readFromPropertyFile(propertyFilePath,"LATEST_GIT_TAG");
		String currentGitTag = executeCommand(build,launcher,listener,"git describe --abbrev=0");

		
					
		listener.getLogger().println("currentPipelineVersion: " + currentPipelineVersion);
		listener.getLogger().println("latestGitTag: " + latestGitTag);
		listener.getLogger().println("currentGitTag: " + currentGitTag);

		if (currentPipelineVersion==null) {
			writeToPropertyFile(propertyFilePath, "PIPELINE_NUMBER", "1");
		} else {
			writeToPropertyFile(propertyFilePath, "PIPELINE_NUMBER",
					String.valueOf(Integer.valueOf(currentPipelineVersion) + 1));
		}

		if (latestGitTag==null || !latestGitTag.equals(currentGitTag)) {
			writeToPropertyFile(propertyFilePath, "LATEST_GIT_TAG",currentGitTag);
		}

		if (latestGitTag!=null && currentGitTag!=null && !latestGitTag.equals(currentGitTag)) {
			writeToPropertyFile(propertyFilePath, "PIPELINE_NUMBER", "1");
		}

		return true;
	}

	private String executeCommand(AbstractBuild build, Launcher launcher,BuildListener listener,String command) {
	
		OutputStream out=new ByteArrayOutputStream();			
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.addTokenized(command);		
		ProcStarter ps = launcher.new ProcStarter();
		ps = ps.cmds(cmd).stdout(out);
		try {
			ps = ps.pwd(build.getWorkspace()).envs(build.getEnvironment(listener));
			Proc proc = launcher.launch(ps);
			proc.join();			
			return new String(((ByteArrayOutputStream)out).toByteArray(), "UTF-8").replace("\n", "").replace("\r", "");			
		}catch(Exception e){
			stackTraceToString(e);
			return "";
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

	private String readFromPropertyFile(String propertyFilePath, String name) {
		InputStream input = null;
		try {
			prop.load(new FileInputStream(propertyFilePath));
			return prop.getProperty(name);
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void writeToPropertyFile(String propertyFilePath, String name,
			String value) {
		OutputStream output = null;
		try {
			output = new FileOutputStream(propertyFilePath);

			prop.setProperty(name, value);
			prop.store(output, null);
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends
			BuildStepDescriptor<Builder> {

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true; // Indicates that this builder can be used with all
							// kinds of project types
		}

		public String getDisplayName() {
			return "Pipeline Number Updater"; // This human readable name is
												// used in the configuration
												// screen.
		}

	}
}
