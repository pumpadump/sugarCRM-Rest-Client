package com.j2911.sugarClient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.multipart.FormDataMultiPart;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;


/**
 * Created by jeremiah on 1/16/14.
 *
 * This is modified from the original post here:
 * http://www.providentcrm.com/news/being-restful-with-sugarcrm-api-and-java/
 * the original post included deprecated code.
 *
 * Posting to sugar's legacy REST Api (Sugar version 6.5.16 at the time of
 * writing this). This is the REST api version that comes out of the can in the
 * community edition. Posting here is weird, they are multi-part form POSTS and
 * are a bit on the tricky side.
 *
 * Non community or upgraded versions need to be handled with different code.
 */
public class SugarClient
{
	private Client client;
	private ObjectMapper mapper = null;
	private String sugarHost = "some ip";
	private String restUri = "/SugarCE/service/v4_1/rest.php?";

	public SugarClient()
	{
		client = Client.create();
		mapper = new ObjectMapper();
	}

	/***
	 * Attempts to login and returns a session id if logged in. Multi-part form
	 * post.
	 * 
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public String login(String username, String password, String url) throws NoSuchAlgorithmException, IOException
	{
		String fullUrl = String.format(url + restUri);

		MessageDigest md5 = MessageDigest.getInstance("MD5");
		String passwordHash = new BigInteger(1, md5.digest(password.getBytes())).toString(16);

		// the order is important, so use a ordered map
		Map<String, String> userCredentials = new LinkedHashMap<String, String>();
		userCredentials.put("user_name", username);
		userCredentials.put("password", passwordHash);

		Map<String, Object> bodyPart = new LinkedHashMap<String, Object>();
		bodyPart.put("user_auth", userCredentials);
		bodyPart.put("application_name", "RestClient");

		FormDataMultiPart multiPart = new FormDataMultiPart();
		multiPart.field("method", "login");
		multiPart.field("input_type", "JSON");
		multiPart.field("response_type", "JSON");
		multiPart.field("rest_data", mapper.writeValueAsString(bodyPart));

		ClientResponse clientResponse = client.resource(fullUrl).accept("application/json").entity(multiPart)
			.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class);

		int responseStatus = clientResponse.getStatus();
		String responseMessage = clientResponse.getEntity(String.class);
		
		if (responseStatus == 200)
		{
			InputStream stream = new ByteArrayInputStream(responseMessage.getBytes());
			JsonNode root = mapper.readValue(stream, JsonNode.class);
			stream.close();

			String id = null;
			if (root.has("id"))
			{
				id = root.path("id").getTextValue();
			}

			return id;
		}else
		{
			throw new IOException("Sugar crm responded with code: "+responseStatus+ " full response: "+responseMessage);
		}
	}
	
	/**
	 * Example message that adds an entry to a costum module that is based on the Person template
	 * 
	 * @param sessionId
	 * @param url
	 * @param prefix
	 * @param modulename
	 * @param firstName
	 * @param lastName
	 * @param email
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public void addPersonToModule(String sessionId, String url, String prefix, String modulename, String firstName, String lastName, String email) throws JsonGenerationException, JsonMappingException, IOException
	{
		String fullUrl = String.format(url + restUri);
		Map<String, Object> details = new LinkedHashMap<String, Object>();
		details.put("session", sessionId);
		details.put("modulename", prefix+"_"+modulename);
		
		List<Map<String, String>> user = new ArrayList<Map<String, String>>();
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("name", "first_name");
		map.put("value", firstName);
		user.add(map);

		map = new LinkedHashMap<String, String>();
		map.put("name", "last_name");
		map.put("value", lastName);
		user.add(map);
		
		map = new LinkedHashMap<String, String>();
		map.put("name", "email1");
		map.put("value", email);
		user.add(map);
		
		
		details.put("name_value_list", user);

		FormDataMultiPart multiPart = new FormDataMultiPart();
		multiPart.field("method", "set_entry");
		multiPart.field("input_type", "JSON");
		multiPart.field("response_type", "JSON");
		multiPart.field("rest_data", mapper.writeValueAsString(details));

		ClientResponse clientResponse = client.resource(fullUrl).accept("application/json").entity(multiPart)
			.type(MediaType.MULTIPART_FORM_DATA_TYPE).post(ClientResponse.class);
		
		int responseStatus = clientResponse.getStatus();
		String responseMessage = clientResponse.getEntity(String.class);
		if(responseStatus != 200)
		{
			throw new IOException("Sugar crm responded with code: "+responseStatus+ " full response: "+responseMessage);
		}
	}

	
}
