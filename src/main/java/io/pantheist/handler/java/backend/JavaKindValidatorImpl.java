package io.pantheist.handler.java.backend;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;

import io.pantheist.handler.kind.model.AnnotationClause;
import io.pantheist.handler.kind.model.ArgClause;
import io.pantheist.handler.kind.model.ConstructorClause;
import io.pantheist.handler.kind.model.JavaClause;
import io.pantheist.handler.kind.model.JavaKind;

final class JavaKindValidatorImpl implements JavaKindValidator
{
	@Inject
	private JavaKindValidatorImpl()
	{
	}

	/**
	 * Return what we think this is, or empty if we have no idea.
	 */
	private Optional<JavaKind> classify(final TypeDeclaration<?> type)
	{
		if (type instanceof ClassOrInterfaceDeclaration)
		{
			if (((ClassOrInterfaceDeclaration) type).isInterface())
			{
				return Optional.of(JavaKind.INTERFACE);
			}
			else
			{
				return Optional.of(JavaKind.CLASS);
			}
		}
		else if (type instanceof EnumDeclaration)
		{
			return Optional.of(JavaKind.ENUM);
		}
		else
		{
			// possibly annotation?
			return Optional.empty();
		}
	}

	@Override
	public boolean validateKind(final CompilationUnit compilationUnit, final JavaClause javaClause)
	{
		checkNotNull(compilationUnit);
		checkNotNull(javaClause);

		if (compilationUnit.getTypes() == null || compilationUnit.getTypes().size() != 1)
		{
			// currently reject all files with no types defined or multiple types defined.
			return false;
		}

		final TypeDeclaration<?> mainType = compilationUnit.getType(0);
		if (javaClause.javaKind() != null)
		{

			// Work out the actual java kind and see if it agrees.
			final TypeDeclaration<?> type = mainType;
			final Optional<JavaKind> javaKind = classify(type);
			if (!javaKind.isPresent() || !javaClause.javaKind().encompasses(javaKind))
			{
				return false;
			}
		}

		if (javaClause.anyAnnotation() != null)
		{
			if (!validateAnyAnnotation(mainType.getAnnotations(), javaClause.anyAnnotation()))
			{
				return false;
			}
		}

		if (javaClause.anyConstructor() != null)
		{
			if (!validateAnyConstructor(mainType, javaClause.anyConstructor()))
			{
				return false;
			}
		}

		// Nothing left to complain about.
		return true;
	}

	private boolean validateAnyConstructor(
			final TypeDeclaration<?> type,
			final ConstructorClause clause)
	{
		final List<BodyDeclaration<?>> members = type.getMembers();
		for (int i = 0; i < members.size(); i++)
		{
			final BodyDeclaration<?> m = members.get(i);

			if (m instanceof ConstructorDeclaration)
			{
				if (validateConstructor((ConstructorDeclaration) m, clause))
				{
					return true;
				}
			}
		}
		return false;
	}

	private boolean validateAnyArg(final NodeList<Parameter> params, final ArgClause clause)
	{
		for (int i = 0; i < params.size(); i++)
		{
			if (validateArg(params.get(i), clause))
			{
				return true;
			}
		}
		return false;
	}

	private boolean validateAnyAnnotation(final NodeList<AnnotationExpr> annotations, final AnnotationClause clause)
	{
		for (int i = 0; i < annotations.size(); i++)
		{
			if (validateAnnotation(annotations.get(i), clause))
			{
				return true;
			}
		}
		return false;
	}

	private boolean validateArg(final Parameter parameter, final ArgClause clause)
	{
		if (clause.anyAnnotation() != null)
		{
			if (!validateAnyAnnotation(parameter.getAnnotations(), clause.anyAnnotation()))
			{
				return false;
			}
		}
		return true;
	}

	private boolean validateConstructor(final ConstructorDeclaration c, final ConstructorClause clause)
	{
		if (clause.anyArg() != null)
		{
			final NodeList<Parameter> params = c.getParameters();
			if (!validateAnyArg(params, clause.anyArg()))
			{
				return false;
			}
		}
		return true;
	}

	private boolean validateAnnotation(final AnnotationExpr annotationExpr, final AnnotationClause clause)
	{
		if (clause.name() != null)
		{
			if (!annotationExpr.getNameAsString().equals(clause.name()))
			{
				return false;
			}
		}
		return true;
	}

}