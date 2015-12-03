package spongecell.guardian.agent.hdfs;

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import spongecell.webhdfs.WebHdfsConfiguration;
import spongecell.webhdfs.WebHdfsWorkFlow;
import spongecell.workflow.config.framework.BeanConfigurations;
import spongecell.workflow.config.repository.BetaGenericConfigurationRepository;
import spongecell.workflow.config.repository.GenericConfigurationRepository;

@Slf4j
@Getter @Setter
@Component
@EnableConfigurationProperties(BetaGenericConfigurationRepository.class)
@ComponentScan(value="hdfsOutputDataValidatorRegistry")
public class HDFSOutputDataValidatorRegistry {
	private @Autowired BetaGenericConfigurationRepository configRepo;

	public HDFSOutputDataValidatorRegistry() { }

	public HDFSOutputDataValidatorRegistry (
			GenericConfigurationRepository configRepo1) {
	}
	
	@PostConstruct
	public void init() {
		configRepo.addRegistryBeans(getClass());

		String[] beanNames = configRepo.getApplicationContext().getBeanDefinitionNames();
		for (String beanName : beanNames) {
			log.info(beanName);
		}	
		log.info("foo");
	}
	
	//*************************************************************
	// HDFS Data Validator Agent.
	//*************************************************************
//	@Bean(name=HDFSOutputDataValidator.WEBHDFS_BEAN_NAME)
//	@ConfigurationProperties(prefix=
//	HDFSOutputDataValidator.WEBHDFS_BEAN_CONFIG_PROPS_PREFIX)
//	@BeanConfigurations(parent=HDFSOutputDataValidator.BEAN_NAME)
//	public WebHdfsConfiguration buildWebHdfsConfig() {
//		log.info("1 WebHdfsConfiguration");
//		return new WebHdfsConfiguration();
//	}	
//
//	@Bean(name=HDFSOutputDataValidator.WEBHDFS_WORKFLOW_BEAN_NAME)
//	@ConfigurationProperties(prefix=HDFSOutputDataValidator.WEBHDFS_WORKFLOW_CONFIG_PREFIX)
//	@BeanConfigurations(parent=HDFSOutputDataValidator.BEAN_NAME)
//	public WebHdfsWorkFlow.Builder buildWebHdfsWorkFlow() {
//		log.info("2 WebHdfsWorkFlow.Builder");
//		return new WebHdfsWorkFlow.Builder();
//	}	
//	@Bean(name=HDFSOutputDataValidator.BEAN_NAME)
//	@ConfigurationProperties(prefix=HDFSOutputDataValidator.BEAN_CONFIG_PROPS_PREFIX)
//	public HDFSOutputDataValidator buildHdfsOutputDataValidator () {
//		log.info("3 HDFSOutputDataValidator");
//		return new HDFSOutputDataValidator(configRepo);
//	}	
//***************************
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
	
}