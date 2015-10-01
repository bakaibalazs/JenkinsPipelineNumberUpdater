package hu.bakaibalazs.jenkins.PipelineNumberUpdater;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

public class PreBuildAction extends BuildWrapper {
	private static final Logger LOG = Logger.getLogger(PreBuildAction.class.getName());

	@DataBoundConstructor
	public PreBuildAction() {
	}

	@Override
	public Environment setUp(AbstractBuild build, Launcher launcher,BuildListener listener) throws IOException, InterruptedException {
		

		return new Environment() {
			@Override
			public boolean tearDown(AbstractBuild build, BuildListener listener)
					throws IOException, InterruptedException {
				return true;
			}
		};
	}

	@Extension
	public static class DescriptorImpl extends BuildWrapperDescriptor {
		@Override
		public boolean isApplicable(AbstractProject<?, ?> item) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Pipeline Number Updater";
		}
	}

}
