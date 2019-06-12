# jaxb-delegate-plugin
Plugin for the JAXB (Java API for XML Binding) Schema-to-Source compiler (XJC) that generates code
to add arbitrary methods to classes generated from an XML Schema document.

## History
### *1.0.0*
- First public release

### *1.1.0*
- Small fixes for void method generation

### *2.0.0*
- Fixed handling of parameterized java types as parameter and method return types
- At least Java 8 is required now

### *2.1.0*
- Added support for method and class type parameters

### *2.2.0*
- Added support for referencing delegate definitions declared elsewhere in the XSD or binding model
- Added support for nested class delegation
- Added support for literal class type arguments


## Motivation
Usually, classes generated with XJC are more or less pure data structures. Any business logic,
but also additional derived property values must be implemented externally to the
generated class.
The reference implementation of JAXB ships with a "source code injection" plugin
that provides a means to add arbitrary java code to the XSD which will then end
up in the generated Java code.
This approach, however, leads to ugly XML Schema documents which are tightly
tied to Java as a consuming programming language. XSDs "enhanced" in this way
are unsuitable to publish e.g. as part of the interface description for a
REST service, for example.
There also is a way in the JAXB RI to extend generated classes by inheritance.
This approach, however, has the disadvantage that you cannot rely on the exact
classes, since inherited classes have a different identity.
The "delegate" plugin, on the other hand, tries to keep Java-specific intrusion
into the XSD to a minimum and maintains the exact type of the generated classes
by describing methods that execute additional logic totally in XSD as JAXB
binding customization elements, with only some type
name specifications being specific to the programming language used.
This way, also clients using different programming
frameworks can consume the XSDs without any issues and base their own logic
on the additional annotations in the XSD.

## Function
The "delegate" plugin generates methods that automatically delegate to
compatible methods of a defined delegate class. The delegate can be a
utility class, containing only static methods, or a delegate instance,
in which case the plugin also generates an instance field in the target
class holding the instance of the delegate, and code to lazily instantiate
the delegate instance upon first use.
The included XSD has extensive information about the available binding
customizations and their use.

## Usage

- Add a delegate class to your project. As it is very likely that there
  is a circular dependency between the generated class and the delegate,
  it will have to be defined in the same compilation module that the
  source code is generated in. To avoid circular dependencies, additional
  interfaces will have to be defined.
- Add jaxb-delegate-plugin.jar to the classpath of the XJC. See below on
  examples about how to do that with Maven.
- Enable extension processing in XJC by specifying the "-extension"
  command line option. See below for Maven example.
- Enable jaxb-delegate-plugin by giving "-Xdelegate" on the XJC command
  line.
- Add appropriate binding customizations (see included XSD) to complexType
  definitions in your XSD or separate binding customization file.

## Reference
### Plugin artifact
groupId: net.codesup.util
artifactId: jaxb-format-plugin

### Plugin Activation
		-Xdelegate

The delegate class does not need to be in the classpath at the time code is generated. It also does
not need to implement a specific interface. It must, however, be in the classpath when
the generated code is compiled by the java compiler.

The delegate can either be a utility class with static methods,
or a delegate instance, which is automatically created by the
generated code.

If it is a delegate instance, it must fulfil the following constraints:

- Public one-argument constructor, taking the instance of the target class
  as an argument
- Methods having the same name, parameter types, and return types
  as the methods defined in the binding customizations.

If it is a utility class, it will never be instantiated, but each
of the methods will have to be declared as "public static", and there must
be an additional first parameter representing the instance of the target class.


### Binding customizations
For binding customization elements, see the attached XSD.

#### Maven setup

Enable the jaxb2-maven-plugin to generate java code from XSD:

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
                            <arg>-Xdelegate</arg> <!-- delegate plugin activation -->
                        </args>
                        <plugins>
							<!-- ... other XJC plugin references -->
                            <plugin>
                                <!-- format plugin reference -->
                                <groupId>net.codesup.util</groupId>
                                <artifactId>jaxb-delegate-plugin</artifactId>
                                <version>2.2.0</version>
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
In any case, you must declare a namespace prefix for the "http://www.codesup.net/jaxb/plugins/delegate"
namespace, and then use (at least) the "method" customization. Also note the declaration of
the JAXB namespace, and the jxb:version and jxb:extensionBindingPrefixes attributes.
Note that the "delegate" binding customization can only be applied to a named or
anonymous complexType declaration.

		<schema xmlns="http://www.w3.org/2001/XMLSchema" version="1.0"
			targetNamespace="http://my.namespace.org/myschema"
			xmlns:jxb="http://java.sun.com/xml/ns/jaxb"
			jxb:version="2.1"
			jxb:extensionBindingPrefixes="delegate"
			xmlns:delegate="http://www.codesup.net/jaxb/plugins/delegate">

			<!-- ... other definitions -->

			<complexType name="my-type">
				<annotation>
					<appInfo>
					<delegate:delegate class="org.namespace.my.myschema.Delegate">
						<delegate:method name="hashCode" type="int"/>
					</delegate:delegate>
					</appInfo>
				</annotation>
				<sequence>
					<element name="created-at" type="datetime"/>
				</sequence>
				<attribute name="name" type="string"/>
			</complexType>
		</schema>

#### Write your delegate class
In this example, it is a delegate with a one-argument constructor.
Note that the plugin gets all information for code generation from
the binding customizations above. It does not in any way parse or access
the source code of the delegate class. This code must however be accessible when
the generated class is compiled by the java compiler.

		package org.namespace.my.myschema;
		
		public class Delegate {
			private final MyType myType;
			
			public Delegate(final MyType myType) {
				this.myType = myType;
			}
			
			@Override
			public int hashCode() {
				return myType.getCreatedAt().hashCode() ^ myType.getName().hashCode();
			}
		}
		
		
#### Use the generated "hashCode" method
You can now write something like this:

		MyType myObject = new MyType();
		myObject.setName("First instance");
		myObject.setCreatedAt(new Date());
		System.out.println(myObject.hashCode());
