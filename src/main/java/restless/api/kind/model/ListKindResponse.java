package restless.api.kind.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = ListKindResponseImpl.class)
public interface ListKindResponse
{
	@JsonProperty("childResources")
	List<ListKindItem> childResources();
}
