<%@include file="/jsp/init.jsp" %>

<%
String searchHint = (String) request.getAttribute(WebKeys.SEARCH_HINT);
String searchFieldHint = (String) request.getAttribute(WebKeys.SEARCH_FIELD_HINT);
String keywords = ParamUtil.getString(request, "keywords");
searchHint = LanguageUtil.get(pageContext, searchHint);
String value = StringUtils.isNotBlank(keywords) ? keywords : searchHint;
%>

<portlet:actionURL name="search" var="searchURL"/>

<div class="portlet-search">
<aui:form method="post" name="fm" action="<%= searchURL %>" cssClass="portlet-search">
	<aui:fieldset>
		<aui:input name="keywords" id="keywords" inlineField="<%= true %>" size="30" label="" title="<%= searchFieldHint %>" value="<%= value %>" onfocus='<%= renderResponse.getNamespace() + "onfocusscript(this.value);" %>' onblur='<%= renderResponse.getNamespace() + "onblurscript(this.value);" %>'/>
        <button name="search" title='<%= Validator.isNotNull(searchFieldHint) ? searchFieldHint : "search" %>' onClick='<%= renderResponse.getNamespace() + "searchClick(this);" %>'>&#9658;</button>
	</aui:fieldset>
</aui:form>
</div>

<aui:script>
	function <portlet:namespace />onfocusscript(bbb) {
		if (bbb == '<%= searchHint %>') {
			document.getElementById('<portlet:namespace/>keywords').value = '';
		}
	}
	function <portlet:namespace />onblurscript(bbb) {
		if (document.getElementById('<portlet:namespace/>keywords').value == '') {
			document.getElementById('<portlet:namespace/>keywords').value = '<%= searchHint %>';
		}
	}
	function <portlet:namespace />searchClick(button) {
		if (document.<portlet:namespace />fm) {
			document.<portlet:namespace />fm.submit();
		}
	}
</aui:script>