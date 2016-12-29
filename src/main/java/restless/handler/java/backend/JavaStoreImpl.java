package restless.handler.java.backend;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import restless.common.util.FailureReason;
import restless.common.util.OtherPreconditions;
import restless.common.util.Possible;
import restless.common.util.View;
import restless.handler.filesystem.backend.FilesystemSnapshot;
import restless.handler.filesystem.backend.FilesystemStore;
import restless.handler.filesystem.backend.FsPath;
import restless.handler.java.model.JavaComponent;
import restless.handler.java.model.JavaModelFactory;
import restless.handler.kind.model.JavaClause;
import restless.handler.kind.model.JavaKind;

final class JavaStoreImpl implements JavaStore
{
	private static final Logger LOGGER = LogManager.getLogger(JavaStoreImpl.class);
	private static final String ROOT = ".";
	private final FilesystemStore filesystem;
	private final JavaModelFactory modelFactory;

	@Inject
	private JavaStoreImpl(final FilesystemStore filesystem, final JavaModelFactory modelFactory)
	{
		this.filesystem = checkNotNull(filesystem);
		this.modelFactory = checkNotNull(modelFactory);
	}

	private FsPath pathToType(final String pkg, final String file)
	{
		OtherPreconditions.checkNotNullOrEmpty(pkg);
		OtherPreconditions.checkNotNullOrEmpty(file);
		if (pkg.startsWith(".") || pkg.contains("..") || pkg.endsWith(".") || pkg.contains("/"))
		{
			throw new IllegalArgumentException("Bad pkg: " + pkg);
		}
		if (file.contains(".") || file.contains("/"))
		{
			throw new IllegalArgumentException("Bad filename: " + file);
		}

		FsPath path = filesystem.systemBucket().segment("java");
		for (final String seg : pkg.split("\\."))
		{
			path = path.segment(seg);
		}
		return path.segment(file + ".java");
	}

	@Override
	public Possible<Void> putJava(final String pkg, final String file, final String code)
	{
		try
		{
			final CompilationUnit compilationUnit = JavaParser.parse(code);

			final Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();

			if (!packageDeclaration.isPresent())
			{
				LOGGER.warn("Java code has no package declaration");
				return FailureReason.REQUEST_FAILED_SCHEMA.happened();
			}

			final NodeList<TypeDeclaration<?>> types = compilationUnit.getTypes();
			if (types == null || types.size() != 1)
			{
				LOGGER.warn(
						"Java code does not contain a unique type declaration. We need this to determine where the file gets put");
				return FailureReason.REQUEST_FAILED_SCHEMA.happened();
			}

			if (!pkg.equals(packageDeclaration.get().getPackageName()))
			{
				LOGGER.warn("Wrong package. Request said " + pkg + ", java code said " + packageDeclaration.get());
				return FailureReason.REQUEST_FAILED_SCHEMA.happened();
			}
			if (!file.equals(types.get(0).getNameAsString()))
			{
				LOGGER.warn(
						"Wrong type name. Request said " + file + ", java code said " + types.get(0).getNameAsString());
				return FailureReason.REQUEST_FAILED_SCHEMA.happened();
			}
			final FsPath filePath = pathToType(pkg, file);

			final FilesystemSnapshot snapshot = filesystem.snapshot();

			if (snapshot.isFile(filePath))
			{
				return FailureReason.ALREADY_EXISTS.happened();
			}

			filePath.parent().leadingPortions().forEach(dirPath -> snapshot.isDir(dirPath));

			snapshot.write(map -> {
				for (final FsPath dirPath : filePath.parent().leadingPortions())
				{
					map.get(dirPath).mkdir();
				}
				FileUtils.write(map.get(filePath), code, StandardCharsets.UTF_8);
			});
			return View.noContent();
		}
		catch (final ParseProblemException e)
		{
			LOGGER.catching(e);
			return FailureReason.REQUEST_HAS_INVALID_SYNTAX.happened();
		}
	}

	@Override
	public Possible<String> getJava(final String pkg, final String file)
	{
		final FsPath filePath = pathToType(pkg, file);
		final FilesystemSnapshot snapshot = filesystem.snapshot();
		if (snapshot.isFile(filePath))
		{
			final String data = snapshot.read(filePath,
					inputStream -> IOUtils.toString(inputStream, StandardCharsets.UTF_8));
			return View.ok(data);
		}
		else
		{
			return FailureReason.DOES_NOT_EXIST.happened();
		}
	}

	@Override
	public Optional<JavaComponent> getJavaComponent(final String pkg, final String file, final String componentId)
	{
		if (componentId.equals(ROOT))
		{
			return Optional.of(modelFactory.component(true));
		}
		else
		{
			return Optional.empty();
		}
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
	public boolean validateKind(final String pkg, final String file, final JavaClause javaClause)
	{
		checkNotNull(javaClause);
		final String code = getJava(pkg, file).get();
		final CompilationUnit compilationUnit = JavaParser.parse(code);

		if (javaClause.javaKind() != null)
		{
			if (compilationUnit.getTypes() == null || compilationUnit.getTypes().size() != 1)
			{
				// can only compare java kind when there's exactly one type defined.
				return false;
			}

			// Work out the actual java kind and see if it agrees.
			final TypeDeclaration<?> type = compilationUnit.getType(0);
			final Optional<JavaKind> javaKind = classify(type);
			if (!javaKind.isPresent() || !javaClause.javaKind().encompasses(javaKind))
			{
				return false;
			}
		}

		// Nothing left to complain about.
		return true;
	}
}
