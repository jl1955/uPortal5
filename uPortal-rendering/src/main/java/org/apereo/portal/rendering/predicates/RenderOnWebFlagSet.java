/**
 * Licensed to Apereo under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. Apereo
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at the
 * following location:
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apereo.portal.rendering.predicates;

import com.google.common.base.Predicate;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import org.apereo.portal.portlet.om.IPortletDefinition;
import org.apereo.portal.portlet.om.IPortletPreference;
import org.apereo.portal.rendering.RequestRenderingPipelineUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RenderOnWebFlagSet implements Predicate<HttpServletRequest> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private RequestRenderingPipelineUtils utils;

    @Autowired
    public void setUtils(RequestRenderingPipelineUtils u) {
        this.utils = u;
    }

    @Override
    public boolean apply(final HttpServletRequest request) {
        try {
            final IPortletDefinition portletDefinition =
                    utils.getPortletDefinitionFromServletRequest(request);
            Iterator<IPortletPreference> iterator =
                    portletDefinition.getPortletPreferences().iterator();
            while (iterator.hasNext()) {
                IPortletPreference cur = iterator.next();
                if ("renderOnWeb".equalsIgnoreCase(cur.getName())) {
                    return cur.getValues() != null
                            && cur.getValues().length == 1
                            && Boolean.parseBoolean(cur.getValues()[0]);
                }
            }
        } catch (Exception e) {
            logger.error(
                    "Failed to process renderOnWeb check for redirect during pipeline. Failing gracefully by returning false.",
                    e);
        }
        return false;
    }

    public String toString() {
        return this.getClass().getSimpleName();
    }
}
