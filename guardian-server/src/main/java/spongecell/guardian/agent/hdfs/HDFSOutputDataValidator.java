package spongecell.guardian.agent.hdfs;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;
import static spongecell.webhdfs.WebHdfsParams.FILE;
import static spongecell.webhdfs.WebHdfsParams.FILE_STATUS;
import static spongecell.webhdfs.WebHdfsParams.FILE_STATUSES;
import static spongecell.webhdfs.WebHdfsParams.TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import spongecell.guardian.agent.exception.GuardianWorkFlowException;
import spongecell.guardian.agent.util.Args;
import spongecell.guardian.agent.workflow.GuardianAgentWorkFlowKeys;
import spongecell.guardian.agent.yarn.Agent;
import spongecell.guardian.configuration.repository.IGenericConfigurationRepository;
import spongecell.guardian.model.HDFSDirectory;
import spongecell.guardian.notification.GuardianEvent;
import spongecell.guardian.notification.SlackGuardianWebHook;
import spongecell.webhdfs.FilePath;
import spongecell.webhdfs.WebHdfsConfiguration;
import spongecell.webhdfs.WebHdfsOps;
import spongecell.webhdfs.WebHdfsWorkFlow;
import spongecell.webhdfs.WebHdfsWorkFlow.Builder;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
@EnableConfigurationProperties({ 
	WebHdfsConfiguration.class, 
	WebHdfsWorkFlow.Builder.class
})
public class HDFSOutputDataValidator implements Agent {
	private WebHdfsWorkFlow.Builder builder;
	private WebHdfsWorkFlow workFlow;
	public static final String BEAN_NAME = "hdfsOutputDataValidator";
	public static final String BEAN_CONFIG_PROPS_PREFIX = "hdfs.output.data.validator";
	public static final String WEBHDFS_BEAN_NAME = "hdfsOutputDataValidatorWebhdfsConfigBean";
	public static final String WEBHDFS_BEAN_CONFIG_PROPS_PREFIX = "hdfs.output.webhdfs";
	public static final String WEBHDFS_WORKFLOW_BEAN_NAME = "hdfsWorkFlowBeanName";
	public static final String WEBHDFS_WORKFLOW_CONFIG_PREFIX = "hdfs.output.workflow.webhdfs";
	
	public HDFSOutputDataValidator () { } 
		
	public HDFSOutputDataValidator (IGenericConfigurationRepository repo) { 
		Iterator<Entry<String, ArrayList<String>>> entries = repo.agentIterator();
		if (log.isDebugEnabled()) {
			String[] beanNames = repo.getApplicationContext().getBeanDefinitionNames();
			for (String beanName : beanNames) {
				log.info(beanName);
			}		
		}
		while (entries.hasNext()) {
			Entry<String, ArrayList<String>> entry = entries.next();
			if (entry.getKey().equals(BEAN_NAME)) {
				log.info("Building agent: {} ", entry.getKey());
				buildAgent(entry, repo.getApplicationContext());
			}
		}
	} 
	
	public void buildAgent(Entry<String, ArrayList<String>> agentEntry,
			ApplicationContext ctx) { 
		ArrayList<String> configIds = agentEntry.getValue();
		for (String configId : configIds) {
			if (configId.equals(WEBHDFS_BEAN_CONFIG_PROPS_PREFIX)) {
				builder = (Builder) ctx.getBean(WEBHDFS_WORKFLOW_BEAN_NAME);
				workFlow = builder
					.context(ctx)
					.repoId(WEBHDFS_BEAN_NAME)
					.build();
			}
		} 
	} 
	@Override
	public Args getStatus(Args args) {
		log.info("********** Getting HDFS status.**********");
		WebHdfsConfiguration webHdfsConfig = workFlow.getConfig();
		String jobStatusFileName = (String) args.getMap().get(
				GuardianAgentWorkFlowKeys.JOB_STATUS_FILE);
		if (jobStatusFileName == null) {
			return args;
		}
		log.info("Job status location: {} ", webHdfsConfig.getBaseDir() + "/" +   
			args.getMap().get(GuardianAgentWorkFlowKeys.JOB_STATUS_FILE));
		
		// TODO - DateTimeFormaater and FilePath should be in a utility class
		DateTimeFormatter customDTF = new DateTimeFormatterBuilder()
        	.appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        	.appendValue(MONTH_OF_YEAR, 2)
        	.appendValue(DAY_OF_MONTH, 2)
        	.toFormatter();	
		
		FilePath filePath = new FilePath.Builder()
			.addPathSegment(workFlow.getConfig().getBaseDir())
			.addPathSegment(customDTF.format(LocalDate.now()))
			.addPathSegment(jobStatusFileName)
			.build();
			
		log.info("Path parent is: {}", filePath.getFile().getParent());

		try {
			JsonNode jobStatus = readJobInfoStatusFile(filePath);
			String outputDir = getJobOutputDir(jobStatus);
			ArrayNode fileStatus = getOutputFileStatus(outputDir);
			createFacts(fileStatus, outputDir, args);
		} catch (IllegalStateException | IOException | URISyntaxException e) {
			log.error("Failed to read job status: {} ", e);
			throw new GuardianWorkFlowException("ERROR - HDFS Agent failure", e);
		}
		return args;		
	}	
	
	public Object[] getStatus(Object[] args) {
		return null;	
	}

	/**
	 * curl -i -L "http://<HOST>:<PORT>/webhdfs/v1/<PATH>?op=OPEN
                    [&offset=<LONG>][&length=<LONG>][&buffersize=<INT>]"
	 * @return
	 * @throws URISyntaxException 
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	private JsonNode readJobInfoStatusFile(FilePath jobInfoFileStatusPath)
			throws URISyntaxException, IllegalStateException, IOException {
		WebHdfsWorkFlow workFlow = builder
				.clear()
				.path(jobInfoFileStatusPath.getFile().getParent())
				.addEntry("OpenReadFile", 
						WebHdfsOps.OPENANDREAD, 
						HttpStatus.OK, 
						jobInfoFileStatusPath.getFileName())
				.build();
		CloseableHttpResponse response = workFlow.execute();
		String content = getContent(response.getEntity().getContent());
		JsonNode jobStatus = new ObjectMapper() .readTree(content);
		log.info("Job status file content is: {} ", new ObjectMapper()
			.writerWithDefaultPrettyPrinter()
			.writeValueAsString(jobStatus));	
		return jobStatus;
	}
	
	private String getJobOutputDir (JsonNode jobStatus) {
		Iterator<JsonNode> properties = jobStatus.get("conf").get("property").iterator();
		String outputDir = null;
		while (properties.hasNext()) {
			JsonNode property = properties.next();
			log.debug(property.toString());
			if (property.get("name").asText()
				.equals("mapreduce.output.fileoutputformat.outputdir")) {
				String value = property.get("value").asText();
				outputDir = value.substring(value.lastIndexOf("/data"), value.length());
				break;
			}
		}
		log.info("Output dir is: {} ", outputDir);
		return outputDir;
	}
	
	private ArrayNode getOutputFileStatus (String fileName) {
		log.info("********** Getting output-file status.**********");
		String fileSegment = fileName.substring(
				fileName.lastIndexOf("/") + 1, fileName.length());
		log.info("File segment: {} ", fileSegment);
		int lastIndex = fileName.lastIndexOf("/");
		String baseDirSegment = fileName.substring(0, lastIndex);
		log.info("Basedir segment: {} ", baseDirSegment);

		WebHdfsWorkFlow workFlow = builder
			.clear()
			.path(baseDirSegment)
			.addEntry("ListDirectoryStatus", 
					WebHdfsOps.LISTSTATUS, 
					HttpStatus.OK, 
					fileSegment)
			.build();

		ArrayNode fileStatus = null; 
		CloseableHttpResponse response = null; 
		do  {
			try {
				response = workFlow.execute();
				int responseCode = HttpStatus.OK.value();  
				Assert.isTrue(response.getStatusLine().getStatusCode() == responseCode, 
						"Response code indicates a failed write: " + 
								response.getStatusLine().getStatusCode());
				fileStatus = getFileStatus(response);
				Thread.sleep(2000);
			} catch (URISyntaxException | ParseException | IOException | InterruptedException e) {
				throw new GuardianWorkFlowException("ERROR - HDFS Agent failure", e);
			} 
		} while(fileStatus == null);
		return fileStatus;
	}
	
	private ArrayNode getFileStatus(CloseableHttpResponse response) 
			throws JsonParseException, JsonMappingException, ParseException, IOException {
		ObjectNode dirStatus = new ObjectMapper().readValue(
			EntityUtils.toString(response.getEntity()), 
			new TypeReference<ObjectNode>() {
		});
		log.info("Directory status is: {} ", new ObjectMapper()
			.writerWithDefaultPrettyPrinter()
			.writeValueAsString(dirStatus));
		
		ArrayNode fileStatus  = new ObjectMapper().readValue(dirStatus
			.get(FILE_STATUSES)
			.get(FILE_STATUS).toString(),
			new TypeReference<ArrayNode>() { 
		});
		for (int i = 0; i < fileStatus.size(); i++) {
			JsonNode fileStatusNode = fileStatus.get(i);
			log.info("File status is: {} ", new ObjectMapper()
				.writerWithDefaultPrettyPrinter()
				.writeValueAsString(fileStatusNode));
			if (fileStatusNode.get("pathSuffix").asText().equals("_temporary")) {
				return null; 
			}
			Assert.isTrue(fileStatusNode.get(TYPE).asText().equals(FILE), 
				"ERROR - cannot read the Node. It is not a file. It is a: " 
				+ fileStatusNode.get(TYPE).asText());
		}		
		return fileStatus;
	}
		
	private void createFacts (ArrayNode fileStatus, String path, Args args) {
		HDFSDirectory hdfsDir = new HDFSDirectory();
		hdfsDir.setNumChildren(fileStatus.size());
		hdfsDir.setOwner("root");
		hdfsDir.setFileStatus(fileStatus);
		hdfsDir.setTargetDir(path);
		
		GuardianEvent event = new GuardianEvent();
		event.dateTime = LocalDateTime.now().toString();
		event.absolutePath = workFlow.getConfig().getBaseDir();
		event.setEventSeverity(GuardianEvent.severity.INFORMATIONAL.name());
		
		SlackGuardianWebHook slackClient = new SlackGuardianWebHook();
		
		args.addArg("hdfsDir", hdfsDir);
		args.addArg("event", event);
		args.addArg("slackClient", slackClient);
	}
	
	@Override
	public Object[] getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Agent buildAgent() {
		// TODO Auto-generated method stub
		return null;
	}
	

	/**
	 * Utility: getContent from a stream.
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 */
	private String getContent(InputStream is) throws IOException {
		ByteArrayBuilder bab = new ByteArrayBuilder();
		int value;
		while ((value = is.read()) != -1) {
			bab.append(value);
		}
		String content = new String(bab.toByteArray());
		bab.close();
		return content;
	}	
}
