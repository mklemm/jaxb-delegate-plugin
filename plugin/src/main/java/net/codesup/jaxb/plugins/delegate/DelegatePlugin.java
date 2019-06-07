/*
 * MIT License
 *
 * Copyright (c) 2014 Klemm Software Consulting, Mirko Klemm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.codesup.jaxb.plugins.delegate;

import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDeclaration;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.codemodel.JTypeVar;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;

import static java.lang.Thread.currentThread;

/**
 * XJC plugin to generate a "toString"-like method by generating an invocation of a delegate object formatter class. Delegate class, method names, method return types and modifiers can be customized
 * on the XJC command line or as binding customizations.
 *
 * @author Mirko Klemm 2015-01-22
 */
public class DelegatePlugin extends AbstractPlugin {
	private static final JAXBContext JAXB_CONTEXT;
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(DelegatePlugin.class.getName());
	private static final String OPTION_NAME = "-Xdelegate";
	private static final String CUSTOMIZATION_NS = "http://www.codesup.net/jaxb/plugins/delegate";
	private static final String DELEGATES_CUSTOMIZATION_NAME = "delegates";
	private static final String DELEGATE_CUSTOMIZATION_NAME = "delegate";
	private static final String DELEGATE_REF_CUSTOMIZATION_NAME = "delegate-ref";
	private static final String METHOD_CUSTOMIZATION_NAME = "method";
	private static final String PARAM_CUSTOMIZATION_NAME = "param";
	private static final String TYPE_PARAM_CUSTOMIZATION_NAME = "type-param";
	private static final String DOCUMENTATION_CUSTOMIZATION_NAME = "documentation";
	private static final List<String> CUSTOM_ELEMENTS = Arrays.asList(
			DelegatePlugin.DELEGATES_CUSTOMIZATION_NAME,
			DelegatePlugin.DELEGATE_CUSTOMIZATION_NAME,
			DelegatePlugin.DELEGATE_REF_CUSTOMIZATION_NAME,
			DelegatePlugin.METHOD_CUSTOMIZATION_NAME,
			DelegatePlugin.PARAM_CUSTOMIZATION_NAME,
			DelegatePlugin.TYPE_PARAM_CUSTOMIZATION_NAME,
			DelegatePlugin.DOCUMENTATION_CUSTOMIZATION_NAME);
	private static final String DEFAULT_DELEGATE_FIELD_PATTERN = "__delegate%s";

	static {
		try {
			JAXB_CONTEXT = JAXBContext.newInstance(Delegates.class, Delegate.class);
		} catch (final JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getOptionName() {
		return DelegatePlugin.OPTION_NAME.substring(1);
	}

	@Override
	public List<String> getCustomizationURIs() {
		return Collections.singletonList(DelegatePlugin.CUSTOMIZATION_NS);
	}

	@Override
	public boolean isCustomizationTagName(final String nsUri, final String localName) {
		return DelegatePlugin.CUSTOMIZATION_NS.equals(nsUri) && DelegatePlugin.CUSTOM_ELEMENTS.contains(localName);
	}

	@Override
	public String getUsage() {
		return DelegatePlugin.RESOURCE_BUNDLE.getString("usageText");
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {
		try {
			final Unmarshaller unmarshaller = DelegatePlugin.JAXB_CONTEXT.createUnmarshaller();
			final Map<String, Delegate> delegateCache = new HashMap<>();
			for (final ClassOutline classOutline : outline.getClasses()) {
				final CPluginCustomization delegatesCustomization = getCustomizationElement(classOutline, DelegatePlugin.DELEGATES_CUSTOMIZATION_NAME);
				if (delegatesCustomization != null) {
					delegatesCustomization.markAsAcknowledged();
					final JAXBElement<Delegates> delegatesElement = unmarshaller.unmarshal(delegatesCustomization.element, Delegates.class);
					delegatesElement.getValue().getDelegateOrDelegateRef().stream()
							.filter(Delegate.class::isInstance)
							.map(Delegate.class::cast)
							.forEach(delegate -> {
								if(delegate.getId() != null) {
									delegateCache.put(delegate.getId(), delegate);
								}
								generateDelegateCode(outline, errorHandler, classOutline, delegate, delegatesCustomization.locator);
							});
				} else {
					final CPluginCustomization delegateCustomization = getCustomizationElement(classOutline, DelegatePlugin.DELEGATE_CUSTOMIZATION_NAME);
					if (delegateCustomization != null) {
						delegateCustomization.markAsAcknowledged();
						final JAXBElement<Delegate> delegateElement = unmarshaller.unmarshal(delegateCustomization.element, Delegate.class);
						final Delegate delegate = delegateElement.getValue();
						if(delegate.getId() != null) {
							delegateCache.put(delegate.getId(), delegate);
						}
						generateDelegateCode(outline, errorHandler, classOutline, delegate, delegateCustomization.locator);
					}
				}
			}
			for (final ClassOutline classOutline : outline.getClasses()) {
				final CPluginCustomization delegatesCustomization = getCustomizationElement(classOutline, DelegatePlugin.DELEGATES_CUSTOMIZATION_NAME);
				if (delegatesCustomization != null) {
					delegatesCustomization.markAsAcknowledged();
					final JAXBElement<Delegates> delegatesElement = unmarshaller.unmarshal(delegatesCustomization.element, Delegates.class);
					delegatesElement.getValue().getDelegateOrDelegateRef().stream()
							.filter(DelegateRef.class::isInstance)
							.map(DelegateRef.class::cast)
							.forEach(delegateRef -> {
										final String delegateRefId = delegateRef.getRefid();
										final Delegate delegate = delegateCache.get(delegateRefId);
										if (delegate != null) {
											generateDelegateCode(outline, errorHandler, classOutline, delegate, delegatesCustomization.locator);
										} else {
											try {
												errorHandler.error(new SAXParseException(getMessage("error.invalidRef", delegateRef.getRefid()), delegatesCustomization.locator));
											} catch(final SAXException e) {

											}
										}
									}
							);
				} else {
					final CPluginCustomization delegateRefCustomization = getCustomizationElement(classOutline, DelegatePlugin.DELEGATE_REF_CUSTOMIZATION_NAME);
					if (delegateRefCustomization != null) {
						delegateRefCustomization.markAsAcknowledged();
						final JAXBElement<DelegateRef> delegateRef = unmarshaller.unmarshal(delegateRefCustomization.element, DelegateRef.class);
						final String delegateRefId = delegateRef.getValue().getRefid();
						final Delegate delegate = delegateCache.get(delegateRefId);
						if (delegate != null) {
							generateDelegateCode(outline, errorHandler, classOutline, delegate, delegateRefCustomization.locator);
						} else {
							try {
								errorHandler.error(new SAXParseException(getMessage("error.invalidRef", delegateRef.getValue().getRefid()), delegateRefCustomization.locator));
							} catch (final SAXException e) {

							}
						}
					}
				}
			}
			return true;
		} catch (final Exception e) {
			errorHandler.error(new SAXParseException(e.getMessage(), outline.getModel().getLocator()));
		}
		return true;
	}

	private void generateDelegateCode(final Outline outline, final ErrorHandler errorHandler, final ClassOutline classOutline, final Delegate delegateAnnotation, final Locator rootLocator) {
		if (delegateAnnotation.getClazz() == null) {
			try {
				errorHandler.error(new SAXParseException(getMessage("error.classRequired"), rootLocator));
			} catch (final SAXException e) {
				// do nothing
			}
			return;
		}
		final JCodeModel model = outline.getCodeModel();
		final JClass delegateClass = model.ref(delegateAnnotation.getClazz());
		final Delegate delegate = gatherRuntimeInformation(delegateAnnotation, delegateClass);
		final String delegateFieldName = String.format(DelegatePlugin.DEFAULT_DELEGATE_FIELD_PATTERN, delegateClass.name());
		final boolean staticDelegate = coalesce(delegate.isStatic(), Boolean.FALSE);
		final JDefinedClass definedClass = classOutline.implClass;
		final JFieldVar delegateField = staticDelegate ? null : definedClass.field(JMod.PRIVATE | JMod.TRANSIENT, delegateClass, delegateFieldName, JExpr._null());
		if (delegate.getDocumentation() != null) {
			delegateField.javadoc().append(delegate.getDocumentation());
		}
		for (final TypeParameterType typeParam : delegate.getTypeParam()) {
			final JClass extendsType = typeParam.getExtends() == null ? null : (JClass)parseType(model, typeParam.getExtends());
			definedClass.generify(typeParam.getName(), extendsType);
		}
		for (final Method method : delegate.getMethod()) {
			final boolean staticMethod = coalesce(method.isStatic(), Boolean.FALSE);
			final int modifiers = parseModifiers(coalesce(method.getModifiers(), "public"));
			final JType returnType = parseType(model, method.getType());
			final JMethod implMethod = definedClass.method(staticMethod ? JMod.STATIC | modifiers : modifiers, returnType, method.getName());
			if (method.getDocumentation() != null) {
				implMethod.javadoc().append(method.getDocumentation());
			}
			final List<JTypeVar> typeParams = new ArrayList<>();
			for (final TypeParameterType param : method.getTypeParam()) {
				final JClass extendsType = param.getExtends() == null ? null : (JClass)parseType(model, param.getExtends());
				final JTypeVar implParam = implMethod.generify(param.getName(), extendsType);
				typeParams.add(implParam);
				if (param.getDocumentation() != null) {
					implMethod.javadoc().addParam(param.getName()).append(param.getDocumentation());
				}
			}
			final List<JVar> params = new ArrayList<>();
			int i = 0;
			for (final MethodParameterType param : method.getParam()) {
				final JType paramType = parseType(model, param.getType());
				final JVar implParam = implMethod.param(paramType, coalesce(param.getName(), "p" + i++));
				params.add(implParam);
				if (param.getDocumentation() != null) {
					implMethod.javadoc().addParam(implParam).append(param.getDocumentation());
				}
			}
			final JInvocation invoke;
			if (staticDelegate) {
				invoke = delegateClass.staticInvoke(method.getName());
				if (!staticMethod) {
					invoke.arg(JExpr._this());
				}
			} else {
				if (staticMethod) {
					invoke = delegateClass.staticInvoke(method.getName());
				} else {
					final JConditional ifStatement = implMethod.body()._if(delegateField.eq(JExpr._null()));
					ifStatement._then().assign(delegateField, JExpr._new(delegateClass).arg(JExpr._this()));
					invoke = delegateField.invoke(method.getName());
				}
			}
			for (final JVar param : params) {
				invoke.arg(param);
			}
//			for (final JTypeVar typeParam:typeParams) {
//				invoke.arg(typeParam);
//			}
			if (returnType.compareTo(JType.parse(model, "void")) == 0) {
				implMethod.body().add(invoke);
			} else {
				implMethod.body()._return(invoke);
			}
		}
	}

	private Delegate gatherRuntimeInformation(final Delegate delegateAnnotation, final JClass delegateClass) {
		if (delegateClass instanceof JDeclaration) {
			try {
				final Class<?> referencedClass = findRuntimeClass(delegateClass);
				if (delegateAnnotation.getMethod().isEmpty()) {
					for (final java.lang.reflect.Method runtimeMethod : referencedClass.getMethods()) {
						delegateAnnotation.getMethod().add(createMethodDescriptor(delegateAnnotation, runtimeMethod));
					}
				} else {
					for (final Method method : delegateAnnotation.getMethod()) {
						try {
							final java.lang.reflect.Method runtimeMethod = findRuntimeMethod(referencedClass, method);
							extendMethodDescriptor(delegateAnnotation, method, runtimeMethod);
						} catch (final NoSuchMethodException nmx) {
							// fall through
						}
					}
				}
			} catch (final ClassNotFoundException cnf) {
				// fall through
			}
		}
		return delegateAnnotation;
	}

	private java.lang.reflect.Method findRuntimeMethod(final Class<?> referencedClass, final Method method) throws NoSuchMethodException {
		if (method.getParam().isEmpty()) {
			try {
				return referencedClass.getMethod(method.getName());
			} catch (final NoSuchMethodException nsmx) {
				for (final java.lang.reflect.Method runtimeMethod : referencedClass.getMethods()) {
					if (runtimeMethod.getName().equals(method.getName())) {
						return runtimeMethod;
					}
				}
				throw new NoSuchMethodException("Method " + referencedClass.getName() + "#" + method.getName() + "(<anyType>) not found");
			}
		} else {
			final Class<?>[] runtimeParameterTypes = new Class<?>[method.getParam().size()];
			int paramIndex = 0;
			for (final MethodParameterType param : method.getParam()) {
				final int currentIndex = paramIndex++;
				try {
					runtimeParameterTypes[currentIndex] = Class.forName(param.getType());
				} catch (final ClassNotFoundException cnfe) {
					runtimeParameterTypes[currentIndex] = Object.class;
				}
			}
			return referencedClass.getMethod(method.getName(), runtimeParameterTypes);
		}
	}

	private Method createMethodDescriptor(final Delegate delegateAnnotation, final java.lang.reflect.Method runtimeMethod) {
		final boolean staticDelegate = coalesce(delegateAnnotation.isStatic(), Boolean.FALSE);
		final Method method = new Method();
		method.setModifiers(Modifier.toString(runtimeMethod.getModifiers() & ~Modifier.STATIC));
		method.setName(runtimeMethod.getName());
		method.setStatic((runtimeMethod.getModifiers() & Modifier.STATIC) != 0 && !staticDelegate);
		method.setType(runtimeMethod.getReturnType().getName());
		inferParameters(method, runtimeMethod);
		return method;
	}

	private Method extendMethodDescriptor(final Delegate delegateAnnotation, final Method method, final java.lang.reflect.Method runtimeMethod) {
		if (method.getModifiers() == null) {
			method.setModifiers(Modifier.toString(runtimeMethod.getModifiers() & ~Modifier.STATIC));
		}
		if (method.getType() == null) {
			method.setType(runtimeMethod.getReturnType().getName());
		}
		if (method.getParam().isEmpty()) {
			inferParameters(method, runtimeMethod);
		} else {
			int paramIndex = 0;
			for (final MethodParameterType param : method.getParam()) {
				if (param.getType() == null) {
					param.setType(runtimeMethod.getParameterTypes()[paramIndex++].getName());
				}
			}
		}
		return method;
	}

	private void inferParameters(final Method method, final java.lang.reflect.Method runtimeMethod) {
		int i = 0;
		for (final Class<?> paramType : runtimeMethod.getParameterTypes()) {
			final MethodParameterType param = new MethodParameterType();
			param.setName("p" + i++);
			param.setType(paramType.getName());
			method.getParam().add(param);
		}
	}

	private Class<?> findRuntimeClass(final JClass jClass) throws ClassNotFoundException {
		try {
			final ClassLoader contextClassLoader;
			// try the context class loader first
			if (System.getSecurityManager() == null) {
				contextClassLoader = Thread.currentThread().getContextClassLoader();
			} else {
				contextClassLoader = (ClassLoader)java.security.AccessController.doPrivileged(
						(PrivilegedAction)currentThread()::getContextClassLoader);
			}
			return contextClassLoader.loadClass(jClass.binaryName());
		} catch (final ClassNotFoundException e) {
			// then the default mechanism.
			return Class.forName(jClass.binaryName());
		}
	}

	private JType parseType(final JCodeModel model, final String typeSpec) {
		if (typeSpec == null) return JType.parse(model, "void");
		try {
			return JType.parse(model, typeSpec);
		} catch (final IllegalArgumentException e) {
			final ParameterizedType p = ParameterizedType.parse(typeSpec);
			return p.createModelClass(model);
		}
	}

	private int parseModifiers(final String modifiers) {
		int mod = JMod.NONE;
		for (final String token : modifiers.split("\\s+")) {
			switch (token.toLowerCase()) {
				case "public":
					mod |= JMod.PUBLIC;
					break;
				case "protected":
					mod |= JMod.PROTECTED;
					break;
				case "private":
					mod |= JMod.PRIVATE;
					break;
				case "final":
					mod |= JMod.FINAL;
					break;
				case "static":
					mod |= JMod.STATIC;
					break;
				case "abstract":
					mod |= JMod.ABSTRACT;
					break;
				case "native":
					mod |= JMod.NATIVE;
					break;
				case "synchronized":
					mod |= JMod.SYNCHRONIZED;
					break;
				case "transient":
					mod |= JMod.TRANSIENT;
					break;
				case "volatile":
					mod |= JMod.VOLATILE;
					break;
			}
		}
		return mod;
	}

	private CPluginCustomization getCustomizationElement(final ClassOutline classOutline, final String elementName) {
		return classOutline.target.getCustomizations().find(DelegatePlugin.CUSTOMIZATION_NS, elementName);
	}

	private <T> T coalesce(final T... vals) {
		for (final T val : vals) {
			if (val != null)
				return val;
		}
		return null;
	}
}
