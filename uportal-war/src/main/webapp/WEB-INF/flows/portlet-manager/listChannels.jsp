<%--

    Licensed to Jasig under one or more contributor license
    agreements. See the NOTICE file distributed with this work
    for additional information regarding copyright ownership.
    Jasig licenses this file to you under the Apache License,
    Version 2.0 (the "License"); you may not use this file
    except in compliance with the License. You may obtain a
    copy of the License at:

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on
    an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied. See the License for the
    specific language governing permissions and limitations
    under the License.

--%>

<%@ include file="/WEB-INF/jsp/include.jsp" %>
    
<!-- START: VALUES BEING PASSED FROM BACKEND -->
<portlet:actionURL var="queryUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
</portlet:actionURL>

<c:set var="n"><portlet:namespace/></c:set>

<portlet:actionURL var="newPortletUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
  <portlet:param name="_eventId" value="createPortlet"/>
</portlet:actionURL>
<portlet:actionURL var="editPortletUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
  <portlet:param name="_eventId" value="editPortlet"/>
  <portlet:param name="chanId" value="PORTLETID"/>
</portlet:actionURL>
<portlet:actionURL var="removePortletUrl">
  <portlet:param name="execution" value="${flowExecutionKey}" />
  <portlet:param name="_eventId" value="removePortlet"/>
  <portlet:param name="chanId" value="PORTLETID"/>
</portlet:actionURL>
<!-- END: VALUES BEING PASSED FROM BACKEND -->

<!--
PORTLET DEVELOPMENT STANDARDS AND GUIDELINES
| For the standards and guidelines that govern
| the user interface of this portlet
| including HTML, CSS, JavaScript, accessibilty,
| naming conventions, 3rd Party libraries
| (like jQuery and the Fluid Skinning System)
| and more, refer to:
| http://www.ja-sig.org/wiki/x/cQ
-->
    
<!-- Portlet -->
<div id="${n}portletBrowser" class="fl-widget portlet ptl-mgr view-home" role="section">
  
  <!-- Portlet Titlebar -->
  <div class="fl-widget-titlebar titlebar portlet-titlebar" role="sectionhead">
  	<h2 class="title" role="heading"><spring:message code="listChannels.title"/></h2>
    <div class="fl-col-flex2 toolbar" role="toolbar">
      <div class="fl-col">
        <ul>
          <li><a class="button" href="${ newPortletUrl }" title="<spring:message code="listChannels.newPortletButton"/>"><span><spring:message code="listChannels.newPortletButton"/></span></a></li>
        </ul>
      </div>
      <div class="fl-col fl-text-align-right portlet-search-view">
        <form class="portlet-search-form" style="display:inline">
            <input class="portlet-search-input"/>
            <input type="submit" value="<spring:message code="listChannels.searchSubmitButton"/>"/>
        </form>
      </div>
    </div>
  </div>
        
	<!-- Portlet Content -->
  <div class="fl-widget-content content portlet-content" role="main">
  
  	<!-- Portlet Message -->
  	<div class="portlet-msg-info portlet-msg info" role="status" id="${n}loadingMessage">
    	<div class="titlebar">
        <h3 class="title">Loading Information</h3>
      </div>
      <div class="content">
    	  <p>Please wait while the system finishes loading the portlet registry.</p>
      </div>
    </div>
    
    <!-- Portlet Section -->
    <div id="${n}channelAddingTabs" class="fl-pager">   
        <div class="fl-col-flex2">
          <div class="fl-col view-filter">
            <label for="${n}categorySelectMenu"><spring:message code="listChannels.categoryFilterLabel"/></label>
            <select id="${n}categorySelectMenu">
              <option value=""><spring:message code="listChannels.categoryFilterAllCategories"/></option>
            </select>
          </div>
          <div class="fl-col flc-pager-top view-pager">
            <ul id="pager-top" class="fl-pager-ui">
              <li class="flc-pager-previous"><a href="#">&lt; <spring:message code="listChannels.pagerPrevious"/></a></li>
              <li>
                <ul class="fl-pager-links flc-pager-links" style="margin:0; display:inline">
                  <li class="flc-pager-pageLink"><a href="javascript:;">1</a></li>
                  <li class="flc-pager-pageLink-disabled">2</li>
                  <li class="flc-pager-pageLink"><a href="javascript:;">3</a></li>
                </ul>
              </li>
              <li class="flc-pager-next"><a href="#"><spring:message code="listChannels.pagerNext"/> &gt;</a></li>
              <li>
                <span class="flc-pager-summary"><spring:message code="listChannels.pagerPerPagePrefix"/></span>
                <span> <select class="pager-page-size flc-pager-page-size">
                <option value="5">5</option>
                <option value="10">10</option>
                <option value="20">20</option>
                <option value="50">50</option>
                </select></span> <spring:message code="listChannels.pagerPerPageSuffix"/>
              </li>
            </ul>
          </div>
        </div>

        <table id="${n}categoriesTable1" summary="" xmlns:rsf="http://ponder.org.uk" class="portlet-table" style="width:100%;">
          <thead>
            <tr rsf:id="header:">
              <th id="${n}portletName" class="flc-pager-sort-header"><a rsf:id="name" title="Click to sort" href="javascript:;"><spring:message code="listChannels.portletTableNameHeading"/></a></th>
              <th id="${n}portletType" class="flc-pager-sort-header"><a rsf:id="type" title="Click to sort" href="javascript:;"><spring:message code="listChannels.portletTableTypeHeading"/></a></th>
              <th id="${n}portletState" class="flc-pager-sort-header"><a rsf:id="state" title="Click to sort" href="javascript:;"><spring:message code="listChannels.portletTableStateHeading"/></a></th>
              <th id="${n}portletEditLink" rsf:id="editLink"><spring:message code="listChannels.portletTableEditHeading"/></th>
              <th id="${n}portletDeleteLink" rsf:id="deleteLink"><spring:message code="listChannels.portletTableDeleteHeading"/></th>
            </tr>
          </thead>
          <tbody id="${n}categoriesBody">
            <tr rsf:id="row:">
              <td headers="${n}portletName"><span rsf:id="name"></span></td>
              <td headers="${n}portletType" rsf:id="type"></td>
              <td headers="${n}portletState" rsf:id="state" style="text-transform:capitalize"></td>
              <td headers="${n}portletEditLink"><a href="" rsf:id="editLink"></a></td>
              <td headers="${n}portletDeleteLink"><a href="" rsf:id="deleteLink"></a></td>
            </tr>
          </tbody>
        </table>
      
      </div>
    
  </div> <!-- end: portlet-body -->
  </form>

</div> <!-- end: portlet -->
    	
	<script type="text/javascript">
	 up.jQuery(function() {
        var $ = up.jQuery;
        var fluid = up.fluid;
        var editUrl = "${ editPortletUrl }";
        var removeUrl = "${ removePortletUrl }";

        var channelTypes = { };
        <c:forEach items="${channelTypes}" var="type">channelTypes[${type.id}] = '${type.name}';</c:forEach>
        
        up.PortletAdministrationCategoryListView = function(container, overallThat, options) {

            // construct the new component
            var that = fluid.initView("up.PortletAdministrationCategoryListView", container, options);

            // initialize a state map for this component
            that.state = {};

            // Build an array of all categories containing at least
            // one deep member, sorted by name
            var categories = [];
            categories.push({
                id: "",
                name: "All",
                description: "All Categories",
                categories: [],
                deepCategories: [],
                portlets: [],
                deepPortlets: []
            });
            $(overallThat.registry.getAllCategories()).each(function(idx, category){
                if (category.deepPortlets.length > 0 && category.id != "local.1") {
                    categories.push(category);
                }
            });
            categories.sort(up.getStringPropertySortFunction("name", "<spring:message code="listChannels.categoryFilterAllCategories"/>"));

            var tree = { children: [] };
            
            var s = overallThat.state.currentCategory || "";
            var selection = { 
                ID: "categorySelect", 
                selection: s, 
                optionlist: [], 
                optionnames: [],
                decorators: [
                    { type: "jQuery", func: "change",
                        args: function(){
                            var category;
                            if ($(this).val() == "") {
                                category = {
                                    id: "",
                                    name: "<spring:message code="listChannels.categoryFilterAllCategories"/>",
                                    description: "All Categories",
                                    categories: [],
                                    deepCategories: [],
                                    portlets: [],
                                    deepPortlets: []
                                };
                            } else {
                                category = overallThat.registry.getCategory($(this).val());
                            }
                            overallThat.events.onCategorySelect.fire(overallThat, category);
                        }
                    }
                ]
            };
            
            $(categories).each(function(idx, category){
                selection.optionlist.push(category.id);
                selection.optionnames.push(category.name);
            });
            
            tree.children.push(selection);

            var cutpoints = [ { id: "categorySelect", selector: "#${n}categorySelectMenu" } ];
            // render the component 
            that.state.templates = fluid.selfRender($(container).find(".view-filter"), tree, 
                { cutpoints: cutpoints });

            that.refresh = function() {
            };

            return that;
        };

        up.PortletAdministrationPortletListView = function(container, overallThat, options) {

            // construct the new component
            var that = fluid.initView("up.PortletAdministrationPortletListView", container, options);

            // initialize a state map for this component
            that.state = {};

            // Build a list of all portlets that are a deep member of the
            // currently-selected category, sorted by title
            var portlets = [];
            var members = (overallThat.state.currentCategory && overallThat.state.currentCategory != "" ) ? overallThat.registry.getMemberPortlets(overallThat.state.currentCategory, true) : overallThat.registry.getAllPortlets();
            $(members).each(function(idx, portlet){
                if (!overallThat.state.portletRegex || overallThat.state.portletRegex.test(portlet.title) || overallThat.state.portletRegex.test(portlet.description)) {
                    portlets.push(portlet);
                }
            });
            portlets.sort(up.getStringPropertySortFunction("title"));

            var options = {
                dataModel: portlets,
                columnDefs: [
                    { key: "name", valuebinding: "*.title", sortable: true },
                    { key: "type", valuebinding: "*.type", sortable: true,
                        components: function(row) {
                                return { value: channelTypes[row.type] };
                            }
                        },
                    { key: "state", valuebinding: "*.state", sortable: true,
                        components: function(row) {
                                return { value: row.state.toLowerCase() }
                            }
                        },
                    { key: "editLink", valuebinding: "*.id",
                        components: {
                            target: editUrl.replace("PORTLETID", '${"${*.id}"}'),
                            linktext: "Edit"
                            }
                        },
                    { key: "deleteLink", valuebinding: "*.id",
                        components: {
                            target: removeUrl.replace("PORTLETID", '${"${*.id}"}'),
                            linktext: "Delete"
                            }
                        }
                ],
                bodyRenderer: {
                  type: "fluid.pager.selfRender",
                  options: {
                      selectors: {
                         root: "#${n}categoriesTable1"
                      },
                      row: "row:"
                    }
                    
                },
                pagerBar: {type: "fluid.pager.pagerBar", options: {
                  pageList: {type: "fluid.pager.renderedPageList",
                    options: { 
                      linkBody: "a"
                    }
                  }
                }}
            };
            that.state.pager = fluid.pager("#${n}channelAddingTabs", options);

            that.refresh = function() {
                portlets = [];
                var members = overallThat.state.currentCategory ? overallThat.registry.getMemberPortlets(overallThat.state.currentCategory, true) : overallThat.registry.getAllPortlets();
                $(members).each(function(idx, portlet){
                    if (!overallThat.state.portletRegex || overallThat.state.portletRegex.test(portlet.title) || overallThat.state.portletRegex.test(portlet.description)) {
                        portlets.push(portlet);
                    }
                });
                portlets.sort(up.getStringPropertySortFunction("title"));
                up.refreshPager(that.state.pager, portlets);
            };

            return that;
        };
    
        $(document).ready(function() {
            var browser = up.PortletBrowser("#${n}portletBrowser", null,
                { 
                    portletRegistry: { 
                        type: "up.PortletRegistry",
                        options: { portletListUrl: "<c:url value="/mvc/channelList"/>" } 
                    },
                    categoryListView: {
                        type: "up.PortletAdministrationCategoryListView"
                    },
                    portletListView: {
                        type: "up.PortletAdministrationPortletListView"
                    },
                    listeners: {
                        onLoad: function(that) {
                            $("#${n}loadingMessage").hide();
                        }
                    }
                }
            );
        });

   	  });
    </script>
