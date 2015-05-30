package br.com.felipezorzo.sonar.plsql;

import java.nio.charset.Charset;

import org.sonar.squidbridge.api.SquidConfiguration;

public class PlSqlConfiguration extends SquidConfiguration {

	private boolean ignoreHeaderComments;

	public PlSqlConfiguration(Charset charset) {
		super(charset);
	}

	public void setIgnoreHeaderComments(boolean ignoreHeaderComments) {
		this.ignoreHeaderComments = ignoreHeaderComments;
	}

	public boolean getIgnoreHeaderComments() {
		return ignoreHeaderComments;
	}

}
