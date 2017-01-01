package io.pantheist.testhelpers.app;

import static com.google.common.base.Preconditions.checkNotNull;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.pantheist.system.initializer.Initializer;
import io.pantheist.system.main.AllPantheistModule;
import io.pantheist.testhelpers.session.TestSession;

public class AppRule implements TestRule
{
	private final TestSession session;

	private AppRule(final TestSession session)
	{
		this.session = checkNotNull(session);
	}

	public static TestRule forTest(final TestSession session)
	{
		return new AppRule(session);
	}

	private Injector createInjector()
	{
		final String[] args = new String[] { "-c", session.configFile().getAbsolutePath() };
		return Guice.createInjector(new AllPantheistModule(args));
	}

	@Override
	public Statement apply(final Statement base, final Description description)
	{
		return new Statement() {

			@Override
			public void evaluate() throws Throwable
			{
				try (Initializer init = createInjector().getInstance(Initializer.class))
				{
					init.start();
					base.evaluate();
				}
			}
		};
	}

}