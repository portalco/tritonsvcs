<%@ include file="/jsp/init.jsp" %>
<%@ page import="com.liferay.portlet.PortletPreferencesFactoryUtil"%>

<%
PortletPreferences preferences = renderRequest.getPreferences();

String portletResource = ParamUtil.getString(request, "portletResource");

if (Validator.isNotNull(portletResource)) {
	preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
}

String resultsUrl = preferences.getValue("results-url", StringPool.BLANK);
String searchHint = preferences.getValue(WebKeys.SEARCH_HINT, StringPool.BLANK);
String searchFieldHint = preferences.getValue(WebKeys.SEARCH_FIELD_HINT, StringPool.BLANK);
String redirectName = preferences.getValue("redirect-name", StringPool.BLANK);
%>

<liferay-portlet:actionURL  portletConfiguration="true" var="actionURL"/>

<aui:form action="<%= actionURL %>" method="post" name="<portlet:namespace/>fm">
	<aui:input type="hidden" name="<%= Constants.CMD %>" value="<%= Constants.UPDATE %>" />
	
	<aui:fieldset>
		<aui:input label="results-url" name="resultsUrl" value="<%= resultsUrl %>" size="100"/>
		<aui:input label="search-hint" name="<%= WebKeys.SEARCH_HINT %>" value="<%= searchHint %>" size="100"/>
		<aui:input label="search-field-hint" name="<%= WebKeys.SEARCH_FIELD_HINT %>" value="<%= searchFieldHint %>" size="100"/>
		<aui:input label="redirect-name" name="redirectName" value="<%= redirectName %>" size="100"/>
	</aui:fieldset>
	
	<aui:button-row>
		<aui:button type="submit" value="save" />
	</aui:button-row>
</aui:form>

<c:if test="<%= renderRequest.getWindowState().equals(WindowState.MAXIMIZED) %>">
	<script type="text/javascript">
		Liferay.Util.focusFormField(document.<portlet:namespace />fm.<portlet:namespace />pageUrl);
	</script>
</c:if>