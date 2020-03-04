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
 * $Id: KeySelectorResult.java 1092655 2011-04-15 10:24:18Z coheigea $
 */
package org.apache.xml.crypto;

import java.security.Key;

/**
 * The result returned by the {@link KeySelector#select KeySelector.select} 
 * method.
 * <p>
 * At a minimum, a <code>KeySelectorResult</code> contains the <code>Key</code>
 * selected by the <code>KeySelector</code>. Implementations of this interface
 * may add methods to return implementation or algorithm specific information,
 * such as a chain of certificates or debugging information.
 *
 * @author Sean Mullan
 * @author JSR 105 Expert Group
 * @see KeySelector
 */
public interface KeySelectorResult {

    /**
     * Returns the selected key.
     *
     * @return the selected key, or <code>null</code> if none can be found
     */
    Key getKey();
}
