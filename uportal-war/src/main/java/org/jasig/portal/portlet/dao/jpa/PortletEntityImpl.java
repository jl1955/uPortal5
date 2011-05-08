/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.portlet.dao.jpa;

import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.portlet.WindowState;

import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Type;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletDefinitionId;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletEntityId;
import org.jasig.portal.portlet.om.IPortletPreferences;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
@Entity
@Table(
        name = "UP_PORTLET_ENT", 
        uniqueConstraints = @UniqueConstraint(name = "IDX_PORT_END__USR_LAY_NODE",  columnNames = { "LAYOUT_NODE_ID", "USER_ID" })
    )
@SequenceGenerator(
        name="UP_PORTLET_ENT_GEN",
        sequenceName="UP_PORTLET_ENT_SEQ",
        allocationSize=10
    )
@TableGenerator(
        name="UP_PORTLET_ENT_GEN",
        pkColumnValue="UP_PORTLET_ENT",
        allocationSize=10
    )
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
class PortletEntityImpl implements IPortletEntity {
    //Properties are final to stop changes in code, hibernate overrides the final via reflection to set their values
    @Id
    @GeneratedValue(generator = "UP_PORTLET_ENT_GEN")
    @Column(name = "PORTLET_ENT_ID")
    private final long internalPortletEntityId;
    
    @Version
    @Column(name = "ENTITY_VERSION")
    private final long entityVersion;
    
    @Column(name = "LAYOUT_NODE_ID", nullable = false, updatable = false)
    private final String layoutNodeId;

    @Column(name = "USER_ID", nullable = false, updatable = false)
    private final int userId;

    //Hidden reference to the parent portlet definition, used by hibernate for referential integrety
    @ManyToOne(targetEntity = PortletDefinitionImpl.class, cascade = {  CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH })
    @JoinColumn(name = "PORTLET_DEF_ID", nullable = false)
    private final IPortletDefinition portletDefinition;
    
    @Column(name = "WINDOW_STATE")
    @Type(type = "windowState")
    private WindowState windowState;

    @OneToOne(targetEntity = PortletPreferencesImpl.class, cascade = { CascadeType.ALL }, orphanRemoval=true)
    @JoinColumn(name = "PORTLET_PREFS_ID", nullable = false)
    @Fetch(FetchMode.JOIN)
    private IPortletPreferences portletPreferences = null;

    @Transient
    private IPortletEntityId portletEntityId = null;
    
    /**
     * Used to initialize fields after persistence actions.
     */
    @SuppressWarnings("unused")
    @PostLoad
    @PostPersist
    @PostUpdate
    @PostRemove
    private void init() {
        this.portletEntityId = new PortletEntityIdImpl(this.internalPortletEntityId);
    }
    
    /**
     * Used by the ORM layer to create instances of the object.
     */
    @SuppressWarnings("unused")
    private PortletEntityImpl() {
        this.internalPortletEntityId = -1;
        this.entityVersion = -1;
        this.portletDefinition = null;
        this.layoutNodeId = null;
        this.userId = -1;
        this.portletPreferences = null;
    }
    
    public PortletEntityImpl(IPortletDefinition portletDefinition, String channelSubscribeId, int userId) {
        Validate.notNull(portletDefinition, "portletDefinition can not be null");
        Validate.notNull(channelSubscribeId, "layoutNodeId can not be null");
        
        this.internalPortletEntityId = -1;
        this.entityVersion = -1;
        this.portletDefinition = portletDefinition;
        this.layoutNodeId = channelSubscribeId;
        this.userId = userId;
        this.portletPreferences = new PortletPreferencesImpl();
    }

    @Override
    public IPortletDefinitionId getPortletDefinitionId() {
        return this.portletDefinition.getPortletDefinitionId();
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.om.portlet.IPortletEntity#getPortletEntityId()
     */
    @Override
    public IPortletEntityId getPortletEntityId() {
        return this.portletEntityId;
    }
    
    @Override
    public IPortletDefinition getPortletDefinition() {
        return this.portletDefinition;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.om.portlet.IPortletEntity#getChannelSubscribeId()
     */
    @Override
    public String getLayoutNodeId() {
        return this.layoutNodeId;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.om.portlet.IPortletEntity#getUserId()
     */
    @Override
    public int getUserId() {
        return this.userId;
    }
    
    @Override
    public WindowState getWindowState() {
        return this.windowState;
    }

    @Override
    public void setWindowState(WindowState state) {
        this.windowState = state;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.om.portlet.IPortletEntity#getPortletPreferences()
     */
    @Override
    public IPortletPreferences getPortletPreferences() {
        return this.portletPreferences;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.om.portlet.IPortletEntity#setPortletPreferences(org.jasig.portal.om.portlet.prefs.IPortletPreferences)
     */
    @Override
    public void setPortletPreferences(IPortletPreferences portletPreferences) {
        Validate.notNull(portletPreferences, "portletPreferences can not be null");
        this.portletPreferences = portletPreferences;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.layoutNodeId == null) ? 0 : this.layoutNodeId.hashCode());
        result = prime * result + ((this.portletDefinition == null) ? 0 : this.portletDefinition.hashCode());
        result = prime * result + this.userId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PortletEntityImpl other = (PortletEntityImpl) obj;
        if (this.layoutNodeId == null) {
            if (other.layoutNodeId != null)
                return false;
        }
        else if (!this.layoutNodeId.equals(other.layoutNodeId))
            return false;
        if (this.portletDefinition == null) {
            if (other.portletDefinition != null)
                return false;
        }
        else if (!this.portletDefinition.equals(other.portletDefinition))
            return false;
        if (this.userId != other.userId)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PortletEntityImpl [internalPortletEntityId=" + this.internalPortletEntityId + ", entityVersion="
                + this.entityVersion + ", layoutNodeId=" + this.layoutNodeId + ", userId=" + this.userId
                + ", portletDefinition=" + this.portletDefinition + ", windowState=" + this.windowState
                + ", portletEntityId=" + this.portletEntityId + "]";
    }
}
