/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 */
/*
 * $Id: ApacheTransform.java 1211847 2011-12-08 11:56:30Z coheigea $
 */
package org.apache.xml.dsig.internal.dom;

import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Set;

import org.apache.xml.crypto.Data;
import org.apache.xml.crypto.MarshalException;
import org.apache.xml.crypto.NodeSetData;
import org.apache.xml.crypto.OctetStreamData;
import org.apache.xml.crypto.XMLCryptoContext;
import org.apache.xml.crypto.XMLStructure;
import org.apache.xml.crypto.dom.DOMCryptoContext;
import org.apache.xml.crypto.dsig.TransformException;
import org.apache.xml.crypto.dsig.TransformService;
import org.apache.xml.crypto.dsig.spec.TransformParameterSpec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.xml.security.Init;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.Transforms;

/**
 * This is a wrapper/glue class which invokes the Apache XML-Security
 * Transform.
 *
 * @author Sean Mullan
 * @author Erwin van der Koogh
 */
public abstract class ApacheTransform extends TransformService {

    static {
       Init.init();
    }

    private static org.apache.commons.logging.Log log =
        org.apache.commons.logging.LogFactory.getLog(ApacheTransform.class);
    private Transform apacheTransform;
    protected Document ownerDoc;
    protected Element transformElem;
    protected TransformParameterSpec params;
    
    public final AlgorithmParameterSpec getParameterSpec() {
        return params;
    }

    public void init(XMLStructure parent, XMLCryptoContext context)
        throws InvalidAlgorithmParameterException
    {
        if (context != null && !(context instanceof DOMCryptoContext)) {
            throw new ClassCastException
                ("context must be of type DOMCryptoContext");
        }
        transformElem = (Element) 
            ((org.apache.xml.crypto.dom.DOMStructure) parent).getNode();
        ownerDoc = DOMUtils.getOwnerDocument(transformElem);
    }

    public void marshalParams(XMLStructure parent, XMLCryptoContext context)
        throws MarshalException
    {
        if (context != null && !(context instanceof DOMCryptoContext)) {
            throw new ClassCastException
                ("context must be of type DOMCryptoContext");
        }
        transformElem = (Element) 
            ((org.apache.xml.crypto.dom.DOMStructure) parent).getNode();
        ownerDoc = DOMUtils.getOwnerDocument(transformElem);
    }

    public Data transform(Data data, XMLCryptoContext xc)
        throws TransformException
    {
        if (data == null) {
            throw new NullPointerException("data must not be null");
        }
        return transformIt(data, xc, (OutputStream)null);
    }

    public Data transform(Data data, XMLCryptoContext xc, OutputStream os)
        throws TransformException
    {
        if (data == null) {
            throw new NullPointerException("data must not be null");
        }
        if (os == null) {
            throw new NullPointerException("output stream must not be null");
        }
        return transformIt(data, xc, os);
    }

    private Data transformIt(Data data, XMLCryptoContext xc, OutputStream os)
        throws TransformException
    {
        if (ownerDoc == null) {
            throw new TransformException("transform must be marshalled");
        }

        if (apacheTransform == null) {
            try {
                apacheTransform = 
                    new Transform(ownerDoc, getAlgorithm(), transformElem.getChildNodes());
                apacheTransform.setElement(transformElem, xc.getBaseURI());
                if (log.isDebugEnabled()) {
                    log.debug("Created transform for algorithm: " +
                            getAlgorithm());
                }
            } catch (Exception ex) {
                throw new TransformException("Couldn't find Transform for: " +
                                             getAlgorithm(), ex);
            } 
        }
        
        Boolean secureValidation = (Boolean)
            xc.getProperty("org.apache.jcp.xml.dsig.secureValidation");
        if (secureValidation != null && secureValidation.booleanValue()) {
            String algorithm = getAlgorithm();
            if (Transforms.TRANSFORM_XSLT.equals(algorithm)) {
                throw new TransformException(
                    "Transform " + algorithm + " is forbidden when secure validation is enabled"
                );
            }
        }

        XMLSignatureInput in;
        if (data instanceof ApacheData) {
            if (log.isDebugEnabled()) {
                log.debug("ApacheData = true");
            }
            in = ((ApacheData)data).getXMLSignatureInput();
        } else if (data instanceof NodeSetData) {
            if (log.isDebugEnabled()) {
                log.debug("isNodeSet() = true");
            }
            if (data instanceof DOMSubTreeData) {
                if (log.isDebugEnabled()) {
                    log.debug("DOMSubTreeData = true");
                }
                DOMSubTreeData subTree = (DOMSubTreeData)data;
                in = new XMLSignatureInput(subTree.getRoot());
                in.setExcludeComments(subTree.excludeComments());
            } else {
                @SuppressWarnings("unchecked")
                Set<Node> nodeSet =
                    Utils.toNodeSet(((NodeSetData)data).iterator());
                in = new XMLSignatureInput(nodeSet);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("isNodeSet() = false");
            }
            try {
                in = new XMLSignatureInput
                    (((OctetStreamData)data).getOctetStream());
            } catch (Exception ex) {
                throw new TransformException(ex);
            }
        }

        try {
            if (os != null) {
                in = apacheTransform.performTransform(in, os);
                if (!in.isNodeSet() && !in.isElement()) {
                    return null;
                }
            } else {
                in = apacheTransform.performTransform(in);
            }
            if (in.isOctetStream()) {
                return new ApacheOctetStreamData(in);
            } else {
                return new ApacheNodeSetData(in);
            }
        } catch (Exception ex) {
            throw new TransformException(ex);
        }
    }

    public final boolean isFeatureSupported(String feature) {
        if (feature == null) {
            throw new NullPointerException();
        } else {
            return false;
        }
    }
}
