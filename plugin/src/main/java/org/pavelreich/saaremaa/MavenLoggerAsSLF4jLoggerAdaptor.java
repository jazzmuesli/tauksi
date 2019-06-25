package org.pavelreich.saaremaa;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.Logger;
import org.slf4j.Marker;

public final class MavenLoggerAsSLF4jLoggerAdaptor implements Logger {
	private Log log;

	public MavenLoggerAsSLF4jLoggerAdaptor(Log log) {
		this.log = log;
	}

	public Log getLog() {
		return log;
	}
	
	@Override
	public void debug(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(String arg0, Object... arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(String arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker arg0, String arg1, Object... arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void debug(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(String arg0) {
		getLog().error(arg0);
	}

	@Override
	public void error(String arg0, Object arg1) {
		getLog().error(arg0);							
	}

	@Override
	public void error(String arg0, Object... arg1) {
		getLog().error(arg0);							
	}

	@Override
	public void error(String arg0, Throwable arg1) {
		getLog().error(arg0,arg1);							
	}

	@Override
	public void error(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(String arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker arg0, String arg1, Object... arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void error(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void info(String arg0) {
		// TODO Auto-generated method stub
		getLog().info(arg0);
	}

	@Override
	public void info(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(String arg0, Object... arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(String arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker arg0, String arg1, Object... arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void info(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isDebugEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDebugEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isErrorEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isErrorEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isInfoEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isInfoEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isTraceEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTraceEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isWarnEnabled() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean isWarnEnabled(Marker arg0) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void trace(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(String arg0, Object... arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(String arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker arg0, String arg1, Object... arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void trace(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(String arg0) {
		getLog().warn(arg0);
	}

	@Override
	public void warn(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		
		
	}

	@Override
	public void warn(String arg0, Object... arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(String arg0, Throwable arg1) {
		// TODO Auto-generated method stub
		getLog().warn(arg0, arg1);
	}

	@Override
	public void warn(Marker arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(String arg0, Object arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker arg0, String arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker arg0, String arg1, Object... arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker arg0, String arg1, Throwable arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void warn(Marker arg0, String arg1, Object arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}
}
