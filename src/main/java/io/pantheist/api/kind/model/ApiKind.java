package io.pantheist.api.kind.model;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.pantheist.common.api.model.ListClassifierResponse;
import io.pantheist.common.api.model.ReplaceAction;
import io.pantheist.handler.kind.model.JavaClause;
import io.pantheist.handler.kind.model.KindLevel;

@JsonDeserialize(as = ApiKindImpl.class)
public interface ApiKind extends ListClassifierResponse
{
	@Nullable
	@JsonProperty("kindId")
	String kindId(); // optional on put requests, but if present must agree with where you're putting it.

	@JsonProperty("level")
	KindLevel level();

	@JsonProperty("discoverable")
	boolean discoverable();

	@Nullable
	@JsonProperty("java")
	JavaClause java();

	/**
	 * Return whether this is used by the system itself.
	 *
	 * For now this is just a tag that is remembered but doesn't do anything.
	 */
	@JsonProperty("partOfSystem")
	boolean partOfSystem();

	/**
	 * If two kinds match, the higher number will be chosen.
	 */
	@JsonProperty("precedence")
	int precedence();

	@JsonProperty("replaceAction")
	ReplaceAction replaceAction();
}