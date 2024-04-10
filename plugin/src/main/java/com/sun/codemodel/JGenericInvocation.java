/*
 * Copyright (c) 1997, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.codemodel;

import java.util.ArrayList;
import java.util.List;


/**
 * JMethod invocation
 */
public final class JGenericInvocation extends JExpressionImpl implements JStatement {

    /**
     * Object expression upon which this method will be invoked, or null if
     * this is a constructor invocation
     */
    private JGenerable object;

    /**
     * Name of the method to be invoked.
     * Either this field is set, or {@link #method}, or {@link #type} (in which case it's a
     * constructor invocation.)
     * This allows {@link JMethod#name(String) the name of the method to be changed later}.
     */
    private String name;

    private JMethod method;

    private boolean isConstructor = false;

    /**
     * List of argument expressions for this method invocation
     */
    private List<JExpression> args = new ArrayList<>();

    /**
     * If isConstructor==true, this field keeps the type to be created.
     */
    private JType type = null;

    /**
     * Invokes a method on an object.
     *
     * @param object
     *        JExpression for the object upon which
     *        the named method will be invoked,
     *        or null if none
     *
     * @param name
     *        Name of method to invoke
     */
    JGenericInvocation(final JExpression object, final String name) {
        this( (JGenerable)object, name );
    }

	JGenericInvocation(final JExpression object, final JMethod method) {
        this( (JGenerable)object, method );
    }

    /**
     * Invokes a static method on a class.
     */
    JGenericInvocation(final JClass type, final String name) {
        this( (JGenerable)type, name );
    }

	JGenericInvocation(final JClass type, final JMethod method) {
        this( (JGenerable)type, method );
    }

    private JGenericInvocation(final JGenerable object, final String name) {
        this.object = object;
        if (name.indexOf('.') >= 0)
            throw new IllegalArgumentException("method name contains '.': " + name);
        this.name = name;
    }

    private JGenericInvocation(final JGenerable object, final JMethod method) {
        this.object = object;
        this.method =method;
    }

    /**
     * Invokes a constructor of an object (i.e., creates
     * a new object.)
     *
     * @param c
     *      Type of the object to be created. If this type is
     *      an array type, added arguments are treated as array
     *      initializer. Thus you can create an expression like
     *      <code>new int[]{1,2,3,4,5}</code>.
     */
    public JGenericInvocation(final JType c) {
        this.isConstructor = true;
        this.type = c;
    }

    /**
     *  Add an expression to this invocation's argument list
     *
     * @param arg
     *        Argument to add to argument list
     */
    public JGenericInvocation arg(final JExpression arg) {
        if(arg==null)   throw new IllegalArgumentException();
	    this.args.add(arg);
        return this;
    }

    /**
     * Adds a literal argument.
     *
     * Short for {@code arg(JExpr.lit(v))}
     */
    public JGenericInvocation arg(final String v) {
        return arg(JExpr.lit(v));
    }

	/**
	 * Returns all arguments of the invocation.
	 * @return
	 *      If there's no arguments, an empty array will be returned.
	 */
	public JExpression[] listArgs() {
		return this.args.toArray(new JExpression[0]);
	}

    @Override
    public void generate(final JFormatter f) {
        if (this.isConstructor && this.type.isArray()) {
            // [RESULT] new T[]{arg1,arg2,arg3,...};
            f.p("new").g(this.type).p('{');
        } else {
            if (this.isConstructor) {
                f.p("new").g(this.type).p('(');
            } else {
                String name = this.name;
                if(name==null)  name=this.method.name();

                if (this.object != null)
                    f.g(this.object).p('.').p(name).p('(');
                else
                    f.id(name).p('(');
            }
        }

        f.g(this.args);

        if (this.isConstructor && this.type.isArray())
            f.p('}');
        else
            f.p(')');

        if(this.type instanceof JDefinedClass && ((JDefinedClass)this.type).isAnonymous() ) {
            ((JAnonymousClass)this.type).declareBody(f);
        }
    }

    @Override
    public void state(final JFormatter f) {
        f.g(this).p(';').nl();
    }

}
