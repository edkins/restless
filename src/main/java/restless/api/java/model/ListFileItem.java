package restless.api.java.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = ListFileItemImpl.class)
public interface ListFileItem
{
	@JsonProperty("url")
	String url();
}
