/*
 * Copyright 2014 brutusin.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.instrumentation.logging;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.brutusin.instrumentation.Interceptor;
import org.bson.Document;
import org.jline.utils.Log;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * based onhttps://github.com/brutusin/logging-instrumentation/blob/master/src/main/java/org/brutusin/instrumentation/logging/LoggingInterceptor.java
 * TODO: rename 
 */
public class LoggingInterceptor extends Interceptor {

    private final Map<String, Long> startMap = new HashMap<String, Long>();
	private MongoDBClient db;
	private String testClassName = null;
	private long processStartTime;
	private static final Logger LOG = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public void init(String arg) throws Exception {
    	this.db = new MongoDBClient(getClass().getSimpleName());
    	if (arg.contains("testClassName=")) {
    		this.testClassName = arg.replaceAll("testClassName=", "");
    	}
    	this.processStartTime=System.currentTimeMillis();
        System.err.println("[LoggingInterceptor agent] Logging to mongo:arg=" + arg);
    }

    Set<String> classes = new HashSet<>();
    @Override
    public boolean interceptClass(String className, byte[] byteCode) {
//    	if (classes.add(className)) {
//    		System.out.println("className: " + className);    		
//    	}
    	
    	return className.contains("org/junit/Assert");
    }

    @Override
    public boolean interceptMethod(ClassNode cn, MethodNode mn) {
        return true;
    }

    @Override
    protected void doOnStart(Object source, Object[] arg, String executionId) {
        long start = System.currentTimeMillis();
        startMap.put(executionId, start);
        Exception ex = new Exception("interesting");
        List<StackTraceElement> stackTrace = Arrays.asList(ex.getStackTrace());
        List<Document> stackElements = Collections.emptyList();
        if (testClassName != null && stackTrace.stream().
                filter(p->p.getClassName().equals(testClassName)).count() > 0) {
            stackElements = stackTrace.stream().
                    map(x->new Document().
                    		append("fileName", x.getFileName()).
                    		append("className",x.getClassName()).
                    		append("methodName", x.getMethodName()).
                    		append("lineNumber", x.getLineNumber())).
                    collect(Collectors.toList());
        } else {
        	return;
        }
        
        
        try {
            Document document = new Document().
            		append("startTime", start).
            		append("executionId", executionId).
            		append("source", String.valueOf(source)).
            		append("testClassName", testClassName).
            		append("processStartTime", processStartTime).
            		append("argsCount", arg.length);
            if (source instanceof Method) {
            	Method method = (Method) source;
            	List<String> argClasses = Arrays.asList(method.getParameterTypes()).stream().map(x->x.getCanonicalName()).collect(Collectors.toList());
            	document = document.append("methodName", method.getName()).
						append("className", method.getDeclaringClass().getCanonicalName()).
						append("returnType", method.getReturnType().getCanonicalName()).
						append("argClasses", argClasses);
            }
            document.append("stackElements", stackElements);
			db.insertCollection("interceptions", Arrays.asList(document));
			db.waitForOperationsToFinish();
        } catch (Exception e) {
        	LOG.error(e.getMessage(), e);
        }
    }

    @Override
    protected void doOnThrowableThrown(Object source, Throwable throwable, String executionId) {
    }

    @Override
    protected void doOnThrowableUncatched(Object source, Throwable throwable, String executionId) {
    }

    @Override
    protected void doOnFinish(Object source, Object result, String executionId) {
    }

}