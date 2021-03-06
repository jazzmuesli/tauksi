
package org.pavelreich.saaremaa.jagent;

import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.brutusin.instrumentation.Interceptor;
import org.bson.Document;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.pavelreich.saaremaa.mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * based on https://github.com/brutusin/logging-instrumentation/blob/master/src/main/java/org/brutusin/instrumentation/logging/LoggingInterceptor.java
 */
public class MethodInterceptor extends Interceptor {
	private MongoDBClient db;
	private String testClassName = null;
	private long processStartTime;
	private String prodClassName;
	private String prodClassNameFilter;
	private String sessionId = null;
	private static final Logger LOG = LoggerFactory.getLogger(MethodInterceptor.class);

    @Override
    public void init(String arg) throws Exception {
    	this.db = new MongoDBClient(getClass().getSimpleName());
		if (arg.contains("testClassName=")) {
			this.testClassName = arg.replaceAll("testClassName=([^;]+).*", "$1");
			this.prodClassName = this.testClassName.replaceAll("Test$", "");
			this.prodClassNameFilter = this.prodClassName.replaceAll("\\.","/");
		}
		if (arg.contains("sessionId=")) {
			this.sessionId = arg.replaceAll(".*?sessionId=([^;]+).*", "$1");
		}
     	this.processStartTime=System.currentTimeMillis();
        LOG.debug("[LoggingInterceptor agent] Logging to mongo:arg=" + arg + 
        		", prodClassNameFilter: " + prodClassNameFilter + 
        		", prodClassName: "+ prodClassName + ", sessionId: " + sessionId);
    }

    Set<String> classes = new HashSet<>();
    static final Collection<String> excludedClasses = Arrays.asList("jacoco","mongo","bson","brutusin","pavelreich","junit","sun/reflect","jdk/internal");
    @Override
    public boolean interceptClass(String className, byte[] byteCode) {
    	boolean excluded = excludedClasses.stream().anyMatch(p->className.contains(p));
    	boolean ret = !excluded  
    			|| className.contains("org/junit/Assert") || (prodClassNameFilter != null && className.equals(prodClassNameFilter));
    	if (classes.add(className)) {
//    		System.out.println("className: " + className + ", ret: " + ret);    		
    	}
		return ret;
    }
    
    @Override
    public boolean interceptMethod(ClassNode cn, MethodNode mn) {
        return true;
    }

    private Set<List<StackTraceElement>> cachedStacks = new CopyOnWriteArraySet<>();
    @Override
    protected void doOnStart(Object source, Object[] arg, String executionId) {
        Exception ex = new Exception("interesting");
        List<StackTraceElement> stackTrace = Arrays.asList(ex.getStackTrace());
        boolean cachedStack = cachedStacks.add(stackTrace);
		if (!cachedStack) {
        	return;
        }
        List<Document> stackElements = Collections.emptyList();
        Set<String> stackClasses = stackTrace.stream().map(p->p.getClassName()).collect(Collectors.toSet());
        boolean isConstructor = "init()".equals(source);
		boolean isRelevant = testClassName != null && (stackClasses.contains(testClassName) || stackClasses.contains(prodClassName));
		if ((isConstructor && !isRelevant) || (!isConstructor && isRelevant)) {
            stackElements = stackTrace.stream().
                    map(x->new Document().
                    		append("fileName", x.getFileName()).
                    		append("className", x.getClassName()).
                    		append("methodName", x.getMethodName()).
                    		append("lineNumber", x.getLineNumber())).
                    collect(Collectors.toList());
        } else {
        	return;
        }
        
        
        try {
            Document document = new Document().
            		//append("executionId", executionId). ignore
            		append("sessionId", sessionId).
            		append("source", String.valueOf(source)).
            		append("testClassName", testClassName).
            		append("processStartTime", processStartTime).
            		append("sourceClass", source.getClass().getCanonicalName()).
            		append("argsCount", arg.length);
            if (source instanceof Executable) {
            	Executable executable = (Executable) source;
            	List<String> argClasses = Arrays.asList(executable.getParameterTypes()).stream().map(x->x.getCanonicalName()).collect(Collectors.toList());
            	document = document.append("methodName", executable.getName()).
						append("className", executable.getDeclaringClass().getCanonicalName()).
						append("modifiers", executable.getModifiers()).
						append("argClasses", argClasses);
            	if (executable instanceof Method) {
            		Method method = (Method) executable;
            		document = document.append("returnType", method.getReturnType().getCanonicalName());
            	}
            }
            document.append("stackElements", stackElements);
            
			db.insertCollection("interceptions", Arrays.asList(document));
			db.waitForOperationsToFinish();
        } catch (Exception e) {
        	LOG.error(e.getMessage(), e);
        }
    }
    
    private static String md5(String s) {
    	return DigestUtils.md5Hex(s);
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