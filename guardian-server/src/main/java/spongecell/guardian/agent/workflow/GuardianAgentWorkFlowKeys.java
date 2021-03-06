package spongecell.guardian.agent.workflow;

/**
 * Constants for the GuardianAgentWorkFlow.
 * 
 * @author jbrinnand
 */
public class GuardianAgentWorkFlowKeys {
	public static final String WORKFLOW = "workFlow";
	public static final String WORKFLOW_ID = "workFlowId";
	public static final String AGENT_IDS = "agentIds";
	public static final String REGISTRY_ID = "registryId";
	public static final String REGISTRY_CLAZZ_NAME = "registryClazzName";
	public static final String WORKFLOW_CLAZZ_NAME = "workFlowClazzName";
	public static final String OP = "op";
	public static final String CREATE = "create";
	public static final String STEP = "step";
	public static final String COMMA = ",";
	public static final String APP_STATUS = "appStatus";
	public static final String JOB_STATUS_FILE = "jobStatusFile";
	
	public enum STATUS {
		STARTED, COMPLETED, FAILED
	}
}
