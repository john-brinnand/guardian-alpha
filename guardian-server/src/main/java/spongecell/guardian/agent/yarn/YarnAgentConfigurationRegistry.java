package spongecell.guardian.agent.yarn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import spongecell.datasource.airstream.framework.BeanConfigurations;
import spongecell.guardian.agent.hdfs.HDFSOutputDataValidator;
import spongecell.webhdfs.WebHdfsConfiguration;
import spongecell.webhdfs.WebHdfsWorkFlow;
import spongecell.workflow.config.repository.GenericConfigurationRepository;
import spongecell.workflow.config.repository.IGenericConfigurationRepository;


@Getter
@EnableConfigurationProperties({ GenericConfigurationRepository.class })
public class YarnAgentConfigurationRegistry implements IGenericConfigurationRepository {
	@Autowired private GenericConfigurationRepository configRepo;

	public YarnAgentConfigurationRegistry() { }
	
	@PostConstruct
	public void init() {
		configRepo.addBeans(getClass());
	}

	@Override
	public Iterator<Object> iterator() {
		return configRepo.iterator();
	}
	
	public Iterator<Entry<String, String>> mapIterator() {
		return configRepo.mapIterator();
	}	
	
	public Entry<String, String> getEntry(String name) {
		return configRepo.getEntry(name);
	}
	
	@Override
	public Iterator<Entry<String, ArrayList<String>>> agentIterator() {
		return configRepo.agentIterator();
	}	
	
	@Bean(name=HDFSOutputDataValidator.BEAN_NAME)
	@DependsOn(value={ 
		HDFSOutputDataValidator.WEBHDFS_BEAN_NAME, 
		HDFSOutputDataValidator.WEBHDFS_WORKFLOW_BEAN_NAME
	})
	@ConfigurationProperties(prefix=HDFSOutputDataValidator.BEAN_CONFIG_PROPS_PREFIX)
	public HDFSOutputDataValidator buildHdfsOutputDataValidator () {
		return new HDFSOutputDataValidator(configRepo);
	}	
	
	@Bean(name=HDFSOutputDataValidator.WEBHDFS_WORKFLOW_BEAN_NAME)
	@ConfigurationProperties(prefix=
		HDFSOutputDataValidator.WEBHDFS_WORKFLOW_CONFIG_PREFIX)
	@BeanConfigurations(include=false)
	public WebHdfsWorkFlow.Builder buildWebHdfsWorkFlow() {
		return new WebHdfsWorkFlow.Builder();
	}
	
	@Bean(name=HDFSOutputDataValidator.WEBHDFS_BEAN_NAME)
	@ConfigurationProperties(prefix=
		HDFSOutputDataValidator.WEBHDFS_BEAN_CONFIG_PROPS_PREFIX)
	@BeanConfigurations(parent=HDFSOutputDataValidator.BEAN_NAME)
	public WebHdfsConfiguration buildWebHdfsConfig() {
		return new WebHdfsConfiguration();
	}

	@Override
	public <T> T getAgent(String agentId) {
		return configRepo.getAgent(agentId);
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return configRepo.getApplicationContext();
	}	
}