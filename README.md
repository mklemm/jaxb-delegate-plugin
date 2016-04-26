# jaxb-format-plugin
Plugin for the JAXB (Java API for XML Binding) Schema-to-Source compiler (XJC) that generates code
to format instances of generated classes via an arbitrary helper class.

## Motivation
There are several plugins for XJC currently to generate a "toString()" method in generated JAXB class files.
Unfortunately, however, most of these plugins are based on the assumption that a "toString()" or other formatting
method should return a generic string representation of the object they are called on.
This plugin, however, gives you full control over the shape of the string representation of an object, and
also lets you specify the name of the generated method (defaults to "toString") and a helper class
that does the actual formatting.
An example formatter class that uses XPath expressions as the building blocks of the formatting engine
is given in the [jaxb-object-formatter](http://github.com/mklemm/jaxb-object-formatter) repository,
which uses a fork of the apache [commons-jxpath](http://github.com/mklemm/commons-jxpath) project, modified to
support XPath expressions using the actual XML names of JAXB-bindable properties, by processing JAXB-specific
source-level annotations.

## Usage
- Add a formatter class (see below) to the compile and runtime classpath of your application.
- Add jaxb-format-plugin.jar to the classpath of the XJC. See below on examples about how to do that with Maven.
- Enable extension processing in XJC by specifying the "-extension" command line option. See below for Maven example.
- Enable jaxb-format-plugin by giving "-Xformat" on the XJC command line, followed optionally by one of the options
  explained below.
- Specify the fully qualified name of the formatter class either with the "-formatter" command line option or
  in the XSD or binding-config file with the <formatter> binding customization on the global or complexType level.
- Add "expression" binding customizations to complexType definitions in your XSD or separate binding customization file.
  Also, it is possible to override the global command-line settings with binding customizations, see [reference](#reference) below.

## Reference
### Plugin artifact
groupId: com.kscs.util
artifactId: jaxb-format-plugin

### Plugin Activation
		-Xformat
		-formatter=<class name>                     Fully qualified name of formatter class. Optional, but if missing, class must be specified in global or local "formatter" binding customization.
		-formatter-method=<method name>             Name of formatter instance method to invoke. Optional, default: "format"
		-formatter-field=<field name>               Name of the instance field that holds the formatter instance in the generated class. Optional, default: "__objectFormatter"
		-generated-method=<method name>             Name of the generated method. Optional, default: "toString"
		-generated-method-type=<class name>         Fully qualified name of the return type of the generated method. Optional, default: "java.lang.String"
		-generated-method-modifiers=<Modifiers>     Space-separated list of modifiers for the generated method. Optional, default: "public"

The formatter class does not need to be in the classpath at the time code is generated. It also does
not need to implement a specific interface. It must, however, be in the classpath when
the generated code is compiled by the java compiler.

The formatter class must have the following properties:
1. Public constructor taking the expression string as single argument. The helper
	class will be instantiated once for every generated class that is given an
	"expression" customization. The expression will be passed into this constructor,
	the implementation should the compile or otherwise process the expression to
	an internal state.
2. Public instance method that takes an instance of the generated class as an argument.
	The return type of this method must be the same as the return type of the generated method.

### Binding customizations
For binding customization elements, see the attached XSD.

## Examples
### Using with Maven and [jxpath-object-formatter](http://github.com/mklemm/jxpath-object-formatter)
This shows you how to generate "toString()" methods for your generated classes, which return a
string representation of the object based on an XPath expression that evaluates to a string.

Based on apache [commons-jxpath](http://github.com/mklemm/commons-jxpath), you can specify
an XPath expression on every complexType definition, which will then be evaluated against
the java object tree in memory, NOT the serialized XML representation of your JAXB object.
This way, generated toString methods can be used anywhere in your code at runtime without
serializing/deserializing your object.

The [jxpath-object-formatter](http://github.com/mklemm/jxpath-object-formatter) implementation
uses a modified version of jxpath that lets you write your XPath expressions using the XML names
of object's properties, represented as XML elements and attributes, as opposed to the standard
commons-jxpath that can evaluate expressions only if node references are given as JavaBeans property
names. This way, there should be no syntactical difference in your XPath expressions, whether they
are processing the serialized XML document or the object graph in memory.
Additionally, jxpath-object-formatter defines custom XPath functions to format java.util.Date values etc.

#### Maven setup
1. Add runtime dependency to jxpath-object-formatter:

		<dependencies>
			<!-- ... other dependencies -->
			<dependency>
                <groupId>com.kscs.util</groupId>
                <artifactId>jxpath-object-formatter</artifactId>
                <version>1.0.0</version>
            </dependency>
			<!-- ... other dependencies -->
        </depenendcies>

2. Enable the jaxb2-maven-plugin to generate java code from XSD:

		<build>
			<!-- ... other build stuff -->
			<plugins>
				<!-- ... other plugins -->
				<plugin>
                	<groupId>org.jvnet.jaxb2.maven2</groupId>
                    <artifactId>maven-jaxb2-plugin</artifactId>
                    <version>0.11.0</version>
                    <executions>
                        <execution>
                            <id>xsd-generate-2.2</id>
                            <phase>generate-sources</phase>
                            <goals>
                                <goal>generate</goal>
                            </goals>
                        </execution>
                    </executions>
                    <configuration>
                        <schemaIncludes>
                            <schemaInclude>**/*.xsd</schemaInclude>
                        </schemaIncludes>
                        <strict>true</strict>
                        <verbose>true</verbose>
                        <extension>true</extension>
                        <removeOldOutput>true</removeOldOutput>
                        <specVersion>2.2</specVersion>
                        <episode>true</episode>
                        <useDependenciesAsEpisodes>true</useDependenciesAsEpisodes>
                        <scanDependenciesForBindings>false</scanDependenciesForBindings>
                        <args>
							<!-- ... other XJC plugin args -->
                            <arg>-Xformat</arg> <!-- format plugin activation -->
                            <arg>-formatter</arg>
                            <arg>com.kscs.util.jaxb.ObjectFormatter</arg> <!-- class name of formatter class (see above) -->
                        </args>
                        <plugins>
							<!-- ... other XJC plugin references -->
                            <plugin>
                                <!-- format plugin reference -->
                                <groupId>com.kscs.util</groupId>
                                <artifactId>jaxb-format-plugin</artifactId>
                                <version>1.0.0</version>
                            </plugin>
                        </plugins>
                    </configuration>
                </plugin>
			</plugins>
		</build>

#### Use it in XSD
Ths is an example how to specify the binding customizations inline in the XSD file,
please refer to the JAXB/XJC documentation on how to do that in a separate binding
file.
In any case, you must declare a namespace prefix for the "http://www.kscs.com/util/jaxb/format"
namespace, and then use (at least) the "expression" customization. Also note the declaration of
the JAXB namespace, and the jxb:version and jxb:extensionBindingPrefixes attributes.

		<schema xmlns="http://www.w3.org/2001/XMLSchema" version="1.0"
			targetNamespace="http://my.namespace.org/myschema"
			xmlns:jxb="http://java.sun.com/xml/ns/jaxb"
			jxb:version="2.1"
			jxb:extensionBindingPrefixes="format"
			xmlns:format="http://www.kscs.com/util/jaxb/format">

			<!-- ... other definitions -->

			<complexType name="my-type">
				<annotation>
					<appInfo>
						<format:expression select="concat('My Object is ', @name, ', created at: ', format:isoDate(created-at))"/>
					</appInfo>
				</annotation>
				<sequence>
					<element name="created-at" type="datetime"/>
				</sequence>
				<attribute name="name" type="string"/>
			</complexType>
		</schema>

#### Use the generated "toString()" method
You can now write something like this:

		MyType myObject = new MyType();
		myObject.setName("First instance");
		myObject.setCreatedAt(new Date());
		System.out.println(myObject);

And it will print something like:

		My object is First instance, created at: 2015-01-26T11:30:00Z
