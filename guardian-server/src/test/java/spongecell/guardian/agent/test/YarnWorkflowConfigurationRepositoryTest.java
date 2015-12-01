package spongecell.guardian.agent.test;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import spongecell.guardian.agent.hdfs.HDFSOutputDataValidator;
import spongecell.guardian.agent.workflow.GuardianAgentWorkFlow;
import spongecell.guardian.agent.yarn.Agent;
import spongecell.guardian.agent.yarn.YarnAgentConfigurationRegistry;
import spongecell.webhdfs.WebHdfsConfiguration;
import spongecell.webhdfs.WebHdfsWorkFlow;
import spongecell.workflow.config.repository.IGenericConfigurationRepository;


@Slf4j
@ContextConfiguration(classes = { 
	YarnWorkflowConfigurationRepositoryTest.class, 
	GuardianAgentWorkFlow.class,
})
@EnableConfigurationProperties ({ 
	YarnAgentConfigurationRegistry.class,
	WebHdfsConfiguration.class,
	WebHdfsWorkFlow.Builder.class
})
public class YarnWorkflowConfigurationRepositoryTest extends AbstractTestNGSpringContextTests {
	@Autowired private ApplicationContext ctx;
	@Autowired private GuardianAgentWorkFlow workFlow;	
	private static final String GROUP_ID = "spongecell";
	private static final String ARTIFACT_ID = "heston-alpha-agent";
	private static final String VERSION_ID = "0.0.1-SNAPSHOT";
	private static final String MODULE_ID = "heston-alpha-module";
	private static final String SESSION_ID = "heston-alpha-session";
	
	@BeforeClass
	public void init () {
		Assert.assertNotNull(workFlow.getKieMFSessionHandler());
		log.info("Building the session.");
		
		// Simulate how the guardian resource
		// will build rules.
		//************************************
		workFlow.newSessionBuilder()
			.groupId(GROUP_ID)
			.artifactId(ARTIFACT_ID)
			.versionId(VERSION_ID)
			.moduleId(MODULE_ID)
			.sessionId(SESSION_ID)
			.build();		
	}

	@Test
	public void validateYarnAgentConfigurationProperties() {
		log.info("Validating.");
		
		YarnAgentConfigurationRegistry repo = ctx.getBean(
				YarnAgentConfigurationRegistry.class);
		
		Iterator<Entry<String, String>> entries = repo.mapIterator();
		
		while (entries.hasNext()) {
			Entry<String, String> entry = entries.next();
			log.info("Entry - name {}, value {}", 
				entry.getKey(), entry.getValue());
					
		}
	}
	
	@Test
	public void validateYarnAgentBeanConfigurationProperties() {
		log.info("Validating.");
		
		YarnAgentConfigurationRegistry repo = ctx.getBean(
				YarnAgentConfigurationRegistry.class);
		
		Iterator<Entry<String, ArrayList<String>>> entries = repo.agentIterator();
		
		while (entries.hasNext()) {
			Entry<String, ArrayList<String>> entry = entries.next();
			log.info("Entry - name {}, value {}", 
				entry.getKey(), entry.getValue());
			ArrayList<String> ids = entry.getValue();
			for (String configId : ids) {
				if (configId.equals(HDFSOutputDataValidator
						.WEBHDFS_BEAN_CONFIG_PROPS_PREFIX)) {
					WebHdfsConfiguration config = (WebHdfsConfiguration) ctx
							.getBean(HDFSOutputDataValidator.WEBHDFS_BEAN_NAME);
					Assert.assertNotNull(config);
				}
			}
		}
	}
	
	@Test
	public void validateYarnAgentWorkFlow() throws URISyntaxException {
		log.info("Validating.");
		IGenericConfigurationRepository repo = ctx.getBean(
				YarnAgentConfigurationRegistry.class);
		GuardianAgentWorkFlow workFlow = ctx.getBean(GuardianAgentWorkFlow.class);
		
		Iterator<Entry<String, ArrayList<String>>> entries = repo.agentIterator();
		
		while (entries.hasNext()) {
			Entry<String, ArrayList<String>> entry = entries.next();
			log.info("Entry - name {}, value {}", 
				entry.getKey(), entry.getValue());
			Agent agent = (Agent) ctx.getBean(entry.getKey());
			workFlow.addEntry("step1", agent);
		}
		// TODO - pass in the args.
		workFlow.execute();
	}	
}