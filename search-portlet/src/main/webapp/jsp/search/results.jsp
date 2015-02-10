<%@include file="/jsp/init.jsp" %>

<%
String keywords = ParamUtil.getString(request, "keywords");
%>

<div class="portlet-search">
<c:if test="<%= StringUtils.isNotBlank(keywords) %>">
	<liferay-util:include page="/html/portlet/search/search.jsp"/>
</c:if>


<c:if test="<%= StringUtils.isBlank(keywords) %>">
	<liferay-ui:search />
</c:if>
</div>
