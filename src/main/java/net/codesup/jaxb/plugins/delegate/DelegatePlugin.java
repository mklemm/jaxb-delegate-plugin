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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CPluginCustomization;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * XJC plugin to generate a "toString"-like method by generating an invocation of a delegate object formatter class. Delegate class, method names, method return types and modifiers can be customized
 * on the XJC command line or as binding customizations.
 *
 * @author Mirko Klemm 2015-01-22
 */
public class DelegatePlugin extends AbstractPlugin {
	private static final JAXBContext JAXB_CONTEXT;

	static {
		try {
			JAXB_CONTEXT = JAXBContext.newInstance(Delegates.class, Delegate.class);
		} catch (JAXBException e) {
			throw new RuntimeException(e);
		}
	}

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(DelegatePlugin.class.getName());
	private static final String OPTION_NAME = "-Xdelegate";
	private static final String CUSTOMIZATION_NS = "http://www.codesup.net/jaxb/plugins/delegate";
	private static final String DELEGATES_CUSTOMIZATION_NAME = "delegates";
	private static final String DELEGATE_CUSTOMIZATION_NAME = "delegate";
	private static final String METHOD_CUSTOMIZATION_NAME = "method";
	private static final List<String> CUSTOM_ELEMENTS = Arrays.asList(
			DelegatePlugin.DELEGATES_CUSTOMIZATION_NAME,
			DelegatePlugin.DELEGATE_CUSTOMIZATION_NAME,
			DelegatePlugin.METHOD_CUSTOMIZATION_NAME);
	private static final String DEFAULT_DELEGATE_FIELD_PATTERN = "__delegate%s";

	@Opt
	private boolean inferMethods = false;

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
			for (final ClassOutline classOutline : outline.getClasses()) {
				final CPluginCustomization delegatesCustomization = getCustomizationElement(classOutline, DelegatePlugin.DELEGATES_CUSTOMIZATION_NAME);
				if (delegatesCustomization != null) {
					delegatesCustomization.markAsAcknowledged();
					final JAXBElement<Delegates> delegatesElement = unmarshaller.unmarshal(delegatesCustomization.element, Delegates.class);
					for (final Delegate delegate : delegatesElement.getValue().getDelegate()) {
						generateDelegateReference(outline, errorHandler, classOutline, delegate);
					}
				} else {
					final CPluginCustomization delegateCustomization = getCustomizationElement(classOutline, DelegatePlugin.DELEGATE_CUSTOMIZATION_NAME);
					if (delegateCustomization != null) {
						delegateCustomization.markAsAcknowledged();
						final JAXBElement<Delegate> delegateElement = unmarshaller.unmarshal(delegateCustomization.element, Delegate.class);
						generateDelegateReference(outline, errorHandler, classOutline, delegateElement.getValue());
					}
				}
			}
			return true;
		} catch (final Exception e) {
			throw new SAXException(e);
		}
	}

	private void generateDelegateReference(final Outline outline, final ErrorHandler errorHandler, final ClassOutline classOutline, final Delegate delegate) {
		final JCodeModel model = outline.getCodeModel();
		final JClass delegateClass = model.ref(delegate.getClazz());
		final String delegateFieldName = String.format(DelegatePlugin.DEFAULT_DELEGATE_FIELD_PATTERN, delegateClass.name());
		final boolean staticDelegate = coalesce(delegate.isStatic(), Boolean.FALSE);
		final JDefinedClass definedClass = classOutline.implClass;
		final JFieldVar delegateField = delegate.isStatic() ? null : definedClass.field(JMod.PRIVATE | JMod.TRANSIENT, delegateClass, delegateFieldName, JExpr._null());
		for (final Method method : delegate.getMethod()) {
			final boolean staticMethod = coalesce(method.isStatic(), Boolean.FALSE);
			final int modifiers = parseModifiers(coalesce(method.getModifiers(), "public"));
			final JType returnType = parseType(model, method.getType());
			final JMethod implMethod = definedClass.method(staticMethod ? JMod.STATIC | modifiers : modifiers, returnType, method.getName());
			for (final MethodParameterType param : method.getParam()) {
				final JType paramType = parseType(model, param.getType());
				implMethod.param(paramType, param.getName());
			}
			final JInvocation invoke;
			if (staticDelegate) {
				invoke = delegateClass.staticInvoke(method.getName());
				if(!staticMethod) {
					invoke.arg(JExpr._this());
				}
			} else {
				if(staticMethod) {
					invoke = delegateClass.staticInvoke(method.getName());
				} else {
					final JConditional ifStatement = implMethod.body()._if(delegateField.eq(JExpr._null()));
					ifStatement._then().assign(delegateField, JExpr._new(delegateClass).arg(JExpr._this()));
					invoke = delegateField.invoke(method.getName());
				}
			}
			for (final MethodParameterType param : method.getParam()) {
				invoke.arg(param.getName());
			}
			implMethod.body()._return(invoke);
		}
	}

	private JType parseType(final JCodeModel model, final String typeSpec) {
		try {
			return JType.parse(model, typeSpec);
		} catch (final IllegalArgumentException e) {
			return model.ref(typeSpec);
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
