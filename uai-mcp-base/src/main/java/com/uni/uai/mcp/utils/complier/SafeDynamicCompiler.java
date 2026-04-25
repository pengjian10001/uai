package com.uni.uai.mcp.utils.complier;

import javax.tools.*;

import com.uni.uai.mcp.utils.ServletUtil;
import com.uni.ubag.common.log.Logger;
import com.uni.ubag.common.log.LoggerFactory;

import jakarta.servlet.ServletContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
	功能概述：
	SafeDynamicCompiler 是一个用于在运行时动态编译、加载 Java 类的工具类，支持处理包含内部类的 Java 源代码，并通过弱引用管理类加载器以优化资源回收，避免内存泄漏。
	
	核心特性：
	
	动态编译与加载：通过 Java 编译器（JavaCompiler）在内存中编译 Java 源代码，生成字节码并加载为 Class 对象，无需写入本地文件。
	内部类支持：能够正确处理包含内部类的源代码，确保主类与内部类被同一类加载器加载。
	内存安全：使用弱引用（WeakReference）缓存类加载器，配合主动清理机制，减少内存泄漏风险。
	零文件 IO：所有编译过程在内存中完成，避免磁盘 IO 操作，提高效率。
	
	主要组件：
	
	DynamicClassLoader：自定义类加载器，负责加载编译生成的字节码（包括主类和内部类）。
	MemoryJavaFileManager：内存中的文件管理器，用于收集编译生成的所有类字节码。
	MemoryJavaFileObject：内存中的文件对象，存储源代码或编译后的字节码。
	
	关键方法：
	
	compileAndLoad(String className, String sourceCode)：编译指定类名的源代码并加载为 Class 对象，返回主类。
	compileSource(String className, String sourceCode)：编译源代码，返回所有生成类（含内部类）的字节码映射。
	cleanUnusedLoaders()：清理已被回收的类加载器弱引用，优化内存使用。 
	 
 
 
 */
public class SafeDynamicCompiler {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
    // 用弱引用存储类加载器，避免强引用导致无法回收
    private final Map<String, WeakReference<DynamicClassLoader>> loaderCache = new ConcurrentHashMap<>();

    // 动态编译类并加载（返回主类，同时加载所有内部类）
    public Class<?> compileAndLoad(String className, String sourceCode) throws Exception {
        // 1. 编译Java源码为字节码（获取所有类的字节码，包括内部类）
        Map<String, byte[]> allClassBytes = compileSource(className, sourceCode);

        // 2. 创建自定义类加载器并加载所有类
        // 获取当前上下文类加载器（Web环境下为WebAppClassLoader）
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        // 传入上下文类加载器作为父加载器
        DynamicClassLoader classLoader = new DynamicClassLoader(contextClassLoader, allClassBytes);
        Class<?> mainClass = classLoader.loadClass(className);

        // 3. 用弱引用缓存加载器，避免强引用泄漏
        loaderCache.put(className, new WeakReference<>(classLoader));

        return mainClass;
    }

    // 编译源码为字节码（返回所有类的字节码）
    private Map<String, byte[]> compileSource(String className, String sourceCode) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("未找到JDK编译器（需使用JDK而非JRE）");
        }
        
        // 内存中输出字节码，避免写本地文件
        MemoryJavaFileManager fileManager = new MemoryJavaFileManager(compiler.getStandardFileManager(null, null, null));
        List<JavaFileObject> compilationUnits = Collections.singletonList(
                new MemoryJavaFileObject(className, JavaFileObject.Kind.SOURCE, sourceCode)
        );
        
        // 编译选项：指定类路径（包含Web应用的依赖）
        List<String> options = null;
        // 获取Web应用的类路径（关键：添加WEB-INF/classes和WEB-INF/lib下的所有JAR）
        String webAppClasspath = getWebAppClasspath();
        if(webAppClasspath!=null) {
        	options = new ArrayList<>();
        	options.add("-classpath");
            options.add(webAppClasspath); // 添加Web应用的类路径
        }
        		
        // 编译
        JavaCompiler.CompilationTask task = compiler.getTask(
                new PrintWriter(System.err), // 错误输出
                fileManager,
                null, // 诊断监听器
                options, // 传入编译选项（类路径）
                null, // 注解处理类
                compilationUnits
        );
        if (!task.call()) {
            throw new RuntimeException("编译失败: " + className);
        }

        // 返回所有生成的类字节码（包括内部类）
        return fileManager.getAllGeneratedCodes();
    }
    
    // 辅助方法：获取Web应用的类路径（需根据Web容器类型适配）
    private String getWebAppClasspath() {
    	// 3. 拼接类路径（用系统路径分隔符，Windows为;，Linux为:）
        List<String> classpathEntries = new ArrayList<>();
        
        // 1. Web应用的classes目录
        Set<String> classesPath = ServletUtil.getInstance().getClasspath();
        if(classesPath!=null && classesPath.size() > 0) {
        	for(String path : classesPath) {
        		classpathEntries.add(path);
        	}
        }
        
        // 2. Web应用的lib目录下所有JAR
        String libPath = ServletUtil.getInstance().getJarpath();
        if(libPath!=null) {
        	File libDir = new File(libPath);
            String[] jarFiles = libDir.list((dir, name) -> name.endsWith(".jar"));
            if (jarFiles != null) {
                for (String jar : jarFiles) {
                    classpathEntries.add(new File(libDir, jar).getAbsolutePath());
                }
            }
        }
        
        if(classpathEntries.size() > 0) {
        	return String.join(File.pathSeparator, classpathEntries);
        }else {
        	return null;
        }
    }

    // 自定义类加载器（加载所有编译生成的类，包括内部类）
    static class DynamicClassLoader extends ClassLoader {
        // 存储所有类的字节码（包括内部类）
        private final Map<String, byte[]> classBytes;

        // 接受父类加载器作为参数（关键修改）
        public DynamicClassLoader(ClassLoader parent, Map<String, byte[]> classBytes) {
            super(parent); // 显式指定父加载器
            this.classBytes = classBytes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // 先从字节码缓存中查找
        	byte[] bytes = classBytes.get(name);
            if (bytes != null) {
                return defineClass(name, bytes, 0, bytes.length);
            }
            // 缓存中没有则委托给父类加载器
            return super.findClass(name);
        }
    }

    // 内存中的Java文件管理器（收集所有类的字节码）
    static class MemoryJavaFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, MemoryJavaFileObject> classFiles = new HashMap<>();

        protected MemoryJavaFileManager(JavaFileManager fileManager) {
            super(fileManager);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
            MemoryJavaFileObject file = new MemoryJavaFileObject(className, kind, "");
            classFiles.put(className, file);
            return file;
        }

        // 获取所有生成的类字节码（包括内部类）
        public Map<String, byte[]> getAllGeneratedCodes() {
            Map<String, byte[]> result = new HashMap<>();
            for (Map.Entry<String, MemoryJavaFileObject> entry : classFiles.entrySet()) {
                result.put(entry.getKey(), entry.getValue().getBytes());
            }
            return result;
        }
    }

    // 内存中的Java文件对象（存储源码/字节码）
    static class MemoryJavaFileObject extends SimpleJavaFileObject {
        private final String content;
        private ByteArrayOutputStream byteCode;

        public MemoryJavaFileObject(String className, Kind kind, String content) {
            super(URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind);
            this.content = content;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return content;
        }

        @Override
        public OutputStream openOutputStream() {
            byteCode = new ByteArrayOutputStream();
            return byteCode;
        }

        public byte[] getBytes() {
            return byteCode != null ? byteCode.toByteArray() : new byte[0];
        }
    }

    // 主动清理过期的类加载器引用
    public void cleanUnusedLoaders() {
        loaderCache.entrySet().removeIf(entry -> {
            WeakReference<DynamicClassLoader> ref = entry.getValue();
            return ref.get() == null; // 弱引用已被回收，移除缓存
        });
        System.gc();
    }
    
    public static void main(String[] args) throws Exception {
    	// 1. 创建动态编译器实例
    	SafeDynamicCompiler compiler = new SafeDynamicCompiler();

        // 2. 定义类名和源代码
        String className = "com.uai.userpackage.Person";
        String sourceCode = """
package com.uai.userpackage;
     	import java.util.Optional;
import javax.validation.constraints.Pattern;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.output.ServiceOutputParser;
public class Person {
    @Description("姓名，必需字段。")
    @JsonProperty(required=true)
    String name;

    @Description("年龄，必需满足大于0，小于200")
    int age;

    @Pattern(regexp="^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
    @Description("用户邮箱，必须符合邮箱格式")
    private String email;

    Address address;

    @Description("一个地址")
	public static class Address {
	    String street;
	    String city;
	    Contury contury;

		@Description("一个国家")
		public static class Contury {
     		        @Description("国家")
		    String name;
		}

	}
}
        		""";
        
        // 编译并加载类
        Class<?> dynamicClass = compiler.compileAndLoad(className, sourceCode);
        System.out.println("成功加载主类: " + dynamicClass);
        
        // 验证内部类是否被正确加载
        Class<?>[] innerClasses = dynamicClass.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses) {
        	System.out.println("成功加载内部类: " + innerClass.getName());
            
            Class<?>[] innerClasses2 = innerClass.getDeclaredClasses();
            for (Class<?> innerClass2 : innerClasses2) {
            	System.out.println("成功加载内部类2: " + innerClass2.getName());
                
            }
        }
	}
}
