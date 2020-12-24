# 相关

##    JCP

 JCP（Java Community Process）是管理 Java 生态（包括 J2SE、J2EE 等等）发展的合作组织。 

## JSR

JSR（Java Specification Request）就是组织内的成员针对 Java 的发展提出的一些需求，通过审核之后即会融入到新版本的 Java 功能中成为 Java 的一项特性或功能，不同的发行版本和虚拟机都会遵守这些约定。 

##  JSR175

JSR-175 的全文标题是 **A Metadata Facility for the Java Programming Language （为 Java 语言提供元数据设施）**。它明确提出了在 Java 平台引入 “元编程”（Meta Programming）的思想，要求提供对 “元数据”（Meta Data）的支持。这就是我们现在大量使用的 “@” 注解（Annotation）功能的最早来源。JSR-175 之后的 JSR-181（Web 服务支持）、JSR-250、[JSR-330](https://link.zhihu.com/?target=https%3A//www.chkui.com/article/java/java_jsr330) 都是基于 “元数据” 功能提出的一些更细节的实现。

至于 “元编程”、“元数据” 是什么这里就不详细展开说明了，它的理论很早就提出了，据说最早是在 Lisp 这一类函数式编程语言上开始使用的。网上有很多相关的资料，简单的说它就是 “对源码进行编码”，比如下面这样：

```java
class MyClass {
	@Autowired
	private Interface support;
}
```

通过 @Autowired 这个注解来对 support 这个域进行编码就可以很轻松的扩展原先类的功能。

##  JSR269

插件化注解处理(Pluggable Annotation Processing)APIJSR 269提供一套标准API来处理AnnotationsJSR 175,实际上JSR 269不仅仅用来处理Annotation，我觉得更强大的功能是它建立了Java 语言本身的一个模型,它把method、package、constructor、type、variable、enum、annotation等Java语言元素映射为Types和Elements，从而将Java语言的语义映射成为对象，我们可以在javax.lang.model包下面可以看到这些类。所以我们可以利用JSR 269提供的API来构建一个功能丰富的元编程(metaprogramming)环境。
JSR 269用Annotation Processor在编译期间而不是运行期间处理Annotation, Annotation Processor相当于编译器的一个插件,所以称为插入式注解处理.如果Annotation Processor处理Annotation时(执行process方法)产生了新的Java代码，编译器会再调用一次Annotation Processor，如果第二次处理还有新代码产生，就会接着调用Annotation Processor，直到没有新代码产生为止。每执行一次process()方法被称为一个"round"，这样整个Annotation processing过程可以看作是一个round的序列。
JSR 269主要被设计成为针对Tools或者容器的API。这个特性虽然在JavaSE 6已经存在，但是很少人知道它的存在。lombok就是使用这个特性实现编译期的代码插入的。另外，如果没有猜错，像IDEA在编写代码时候的标记语法错误的红色下划线也是通过这个特性实现的。KAPT(Annotation Processing for Kotlin)，也就是Kotlin的编译也是通过此特性的。

Pluggable Annotation Processing API的核心是Annotation Processor即注解处理器，一般需要继承抽象类javax.annotation.processing.AbstractProcessor。注意，与运行时注解RetentionPolicy.RUNTIME不同，注解处理器只会处理编译期注解，也就是RetentionPolicy.SOURCE的注解类型，处理的阶段位于Java代码编译期间。

# 使用步骤

插件化注解处理API的使用步骤大概如下：

1. 自定义一个Annotation Processor，需要继承javax.annotation.processing.AbstractProcessor，并覆写process方法。
2. 自定义一个注解，注解的元注解需要指定@Retention(RetentionPolicy.SOURCE)。
3. 需要在声明的自定义Annotation Processor中使用javax.annotation.processing.SupportedAnnotationTypes指定在第2步创建的注解类型的名称(注意需要全类名，“包名.注解类型名称”，否则会不生效)。
4. 需要在声明的自定义Annotation Processor中使用javax.annotation.processing.SupportedSourceVersion指定编译版本。
5. 可选操作，可以通在声明的自定义Annotation Processor中使用javax.annotation.processing.SupportedOptions指定编译参数。

# 实战

## 创建父项目

创建一个SpringBoot项目automatic-design-patterns

### 整理项目

作为父项目，只保存pom文件即可，删除src、target文件夹

### pom

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <packaging>pom</packaging>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.4.1</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>cn.yzstu</groupId>
    <artifactId>automatic-design-patterns</artifactId>
    <version>1.0.0</version>
    <name>automatic-design-patterns</name>
    <description>Design pattern automation programming component</description>

    <modules>
        <-- This project will be created later as the core component -->
        <module>automatic-design-patterns-core</module>
    </modules>

    <properties>
        <java.version>1.8</java.version>
    </properties>
</project>

```



## automatic-design-patterns-core

创建**maven项目**作为核心项目（下文出现core项目指automatic-design-patterns-core项目）

### Processor

创建BuilderProcessor

```java
package cn.yzstu.core.processor;


import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Baldwin
 */
// target annotation
@SupportedAnnotationTypes(value = {"cn.yzstu.core.annotation.Builder"})
// set the java version
@SupportedSourceVersion(value = SourceVersion.RELEASE_8)
public class BuilderProcessor extends AbstractProcessor {

    /**
     *
     * @param annotations to be processed
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement typeElement : annotations) {
            // Elements to be processed
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(typeElement);
            // Collect annotated methods
            Map<Boolean, List<Element>> annotatedMethods
                    = annotatedElements.stream().collect(Collectors.partitioningBy(
                    element -> ((ExecutableType) element.asType()).getParameterTypes().size() == 1
                            && element.getSimpleName().toString().startsWith("set")));
            List<Element> setters = annotatedMethods.get(true);
            List<Element> otherMethods = annotatedMethods.get(false);
            // If the annotated method is not a getter method
            otherMethods.forEach(element ->
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "@Builder must be applied to a setXxx method "
                                    + "with a single argument", element));
            // Pretreatment
            Map<String, String> setterMap = setters.stream().collect(Collectors.toMap(
                    setter -> setter.getSimpleName().toString(),
                    setter -> ((ExecutableType) setter.asType())
                            .getParameterTypes().get(0).toString()
            ));
            String className = ((TypeElement) setters.get(0)
                    .getEnclosingElement()).getQualifiedName().toString();
            try {
                writeBuilderFile(className, setterMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Build Builder
     * @param className the parent class
     * @param setterMap to be deal
     * @throws IOException
     */
    private void writeBuilderFile(
            String className, Map<String, String> setterMap)
            throws IOException {
        String packageName = null;
        // get the package
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }
        // get the class name
        String simpleClassName = className.substring(lastDot + 1);
        // the builder class name
        String builderClassName = className + "Builder";
        // package + builder class
        String builderSimpleClassName = builderClassName
                .substring(lastDot + 1);

        // create the builder class
        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);

        // write to the builder class
        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }
            out.print("public class ");
            out.print(builderSimpleClassName);
            out.println(" {");
            out.println();
            out.print("    private ");
            out.print(simpleClassName);
            out.print(" object = new ");
            out.print(simpleClassName);
            out.println("();");
            out.println();
            out.print("    public ");
            out.print(simpleClassName);
            out.println(" build() {");
            out.println("        return object;");
            out.println("    }");
            out.println();
            setterMap.forEach((methodName, argumentType) -> {
                out.print("    public ");
                out.print(builderSimpleClassName);
                out.print(" ");
                out.print(methodName);

                out.print("(");

                out.print(argumentType);
                out.println(" value) {");
                out.print("        object.");
                out.print(methodName);
                out.println("(value);");
                out.println("        return this;");
                out.println("    }");
                out.println();
            });
            out.println("}");
        }
    }
}
```

### 创建注解

```java
package cn.yzstu.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Builder pattern annotation
 * @author Administrator
 */
// Acting on setter methods
@Target({ElementType.METHOD})
// Acting on compilation
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {

}
```

### 注册Processor

在resources\META-INF\services文件夹下创建javax.annotation.processing.Processor文件，写入注册信息

```text
cn.yzstu.core.processor.BuilderProcessor
```

### install

在automatic-design-patterns-core项目下执行mvn install。

此时，我们已经将core项目打包至本地仓库，可在本地仓库对应文件夹下查看

## example项目

创建一个SpringBoot项目并命名为example

### 引入core项目

pom文件如下：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<artifactId>automatic-design-patterns</artifactId>
		<groupId>cn.yzstu</groupId>
		<version>1.0.0</version>
	</parent>
	<groupId>cn.yzstu</groupId>
	<artifactId>example</artifactId>
	<version>1.0.0</version>
	<name>example</name>
	<description>example</description>

	<properties>
		<java.version>1.8</java.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<!-- Introduce the core project -->
		<dependency>
			<groupId>cn.yzstu</groupId>
			<artifactId>automatic-design-patterns-core</artifactId>
			<version>1.0.0</version>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.5.1</version>
				<configuration>
					<source>1.8</source>
					<target>1.8</target>
					<encoding>UTF-8</encoding>
					<annotationProcessors>
						<annotationProcessor>
							<!-- Declare a custom processor -->
							cn.yzstu.core.processor.BuilderProcessor
						</annotationProcessor>
					</annotationProcessors>
				</configuration>
			</plugin>
		</plugins>
	</build>

</project>

```

### 示例bean

创建一个实体类，并使用@Builder注解对某些getter方法进行注解

```java
package cn.yzstu.example;

import cn.yzstu.core.annotation.Builder;

public class Boy {
    private int age;
    private String name;

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    // use builder
    @Builder
    public void setName(String name) {
        this.name = name;
    }
}
```

###  compile

编译example项目

### 查看结果

![1608737731847](G:\个人\MDFile\Java\JSR269\images\result.png)

就自动生成了一个BoyBuilder

# Processor相关

- init(ProcessingEnvironment env): 每一个注解处理器类都**必须有一个空的构造函数**。然而，这里有一个特殊的init()方法，它会被注解处理工具调用，并输入ProcessingEnviroment参数。ProcessingEnviroment提供很多有用的工具类Elements,Types和Filer。
- process(Set<? extends TypeElement> annotations, RoundEnvironment env): 这相当于每个处理器的主函数main()。 在这里写扫描、评估和处理注解的代码，以及生成Java文件。输入参数RoundEnviroment，可以让查询出包含特定注解的被注解元素。
- getSupportedAnnotationTypes(): 这里必须指定，这个注解处理器是注册给哪个注解的。注意，它的返回值是一个字符串的集合，包含本处理器想要处理的注解类型的合法全称。换句话说，在这里定义你的注解处理器注册到哪些注解上。
- getSupportedSourceVersion(): 用来指定你使用的Java版本。通常这里返回SourceVersion.latestSupported()。然而，如果有足够的理由只支持Java 6的话，也可以返回SourceVersion.RELEASE_6。推荐使用前者。
