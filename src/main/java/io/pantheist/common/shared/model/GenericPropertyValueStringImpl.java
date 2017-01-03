package io.pantheist.common.shared.model;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.inject.assistedinject.Assisted;

import io.pantheist.common.util.OtherPreconditions;

final class GenericPropertyValueStringImpl implements GenericPropertyValue
{
	private final String name;
	private final String value;

	@Inject
	private GenericPropertyValueStringImpl(@Assisted("name") final String name, @Assisted("value") final String value)
	{
		this.name = OtherPreconditions.checkNotNullOrEmpty(name);
		this.value = checkNotNull(value);
	}

	@Override
	public String name()
	{
		return name;
	}

	@Override
	public TypeInfo typeInfo()
	{
		return TypeInfoImpl.STRING;
	}

	@Override
	public boolean booleanValue()
	{
		throw new IllegalStateException("Is a string, not a boolean");
	}

	@Override
	public String stringValue()
	{
		return value;
	}

	@Override
	public boolean matchesJsonNodeExactly(final JsonNode jsonNode)
	{
		return jsonNode.isTextual() && value.equals(jsonNode.textValue());
	}

	@Override
	public boolean isArrayContainingJsonNode(final JsonNode jsonNode)
	{
		return false;
	}

	@Override
	public String jsonValue(final ObjectMapper objectMapper) throws JsonProcessingException
	{
		return objectMapper.writeValueAsString(value);
	}

	@Override
	public JsonNode toJsonNode(final JsonNodeFactory nodeFactory)
	{
		return nodeFactory.textNode(value);
	}
}
