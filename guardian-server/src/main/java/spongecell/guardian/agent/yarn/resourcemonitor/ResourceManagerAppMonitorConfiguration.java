package spongecell.guardian.agent.yarn.resourcemonitor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author jbrinnand
 */
@ToString
@Getter @Setter
@ConfigurationProperties(prefix ="app.monitor")
public class ResourceManagerAppMonitorConfiguration {
	public static enum RunStates {
		UNKNOWN("UNKNOWN"),
		UNDEFINED("UNDEFINED"),
		RUNNING("RUNNING"),
		SUCCEEDED("SUCCEEDED"),
		FINISHED("FINISHED");
		
		private RunStates (String states) { }
	}	
	public static final String STATES = "states";
	public static final String STATE = "state";
	public static final String APP = "app";
	public static final String FINAL_STATUS = "finalStatus";
	public static final String USER = "user";
	
	public String scheme = "http";
	public String host = "hadoop-production-resourcemanager.spongecell.net";
	public int port = 8088;
	public String cluster = "ws/v1/cluster";
	public String endpoint = "apps";
	public String groupId = "spongecell";
	public String artifactId = "yarn-monitor";
	public String version = "0.0.1-SNAPSHOT";
	public String sessionId = "yarn-session-v1";	
	public String moduleId = "yarn-monitor-module-v1";
	public String [] users = { "heston", "sponge", "root"} ;
	public int retryCount = 5; 
	public int waitTime = 1000; 

}
