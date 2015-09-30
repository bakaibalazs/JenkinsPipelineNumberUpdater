package hu.bakaibalazs.jenkins.PipelineNumberUpdater;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

import java.io.File;
import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

public class PipelineNumberBuilder extends Builder {

	@DataBoundConstructor
	public PipelineNumberBuilder() {
	}

	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws IOException {
		listener.getLogger().println("---perform---");

		File file = new File("pipeline-version.properties");
		if(!file.exists()){
			listener.getLogger().println("pipeline-version.properties does not exists on this path: "+ file.getAbsolutePath());
			listener.getLogger().println("pipeline-version.properties does not exists on this path: "+ file.getCanonicalPath());
			listener.getLogger().println("pipeline-version.properties does not exists on this path: "+ file.getPath());
			
			file.setWritable(true,true);			
			file.createNewFile();
			
		}else{
			listener.getLogger().println("pipeline-version.properties exists");
		}
			

		return true;
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
