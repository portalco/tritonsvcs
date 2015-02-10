<%@page import="ru.emdev.util.RuDateFormatSymbols"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/portlet" prefix="portlet" %>

<%@ taglib uri="http://liferay.com/tld/aui" prefix="aui" %>
<%@ taglib uri="http://liferay.com/tld/portlet" prefix="liferay-portlet" %>
<%@ taglib uri="http://liferay.com/tld/theme" prefix="liferay-theme" %>
<%@ taglib uri="http://liferay.com/tld/ui" prefix="liferay-ui" %>
<%@ taglib uri="http://liferay.com/tld/security" prefix="liferay-security" %>
<%@ taglib uri="http://liferay.com/tld/util" prefix="liferay-util" %>

<%@ page import="com.liferay.portal.kernel.bean.BeanParamUtil" %>
<%@ page import="com.liferay.portal.kernel.dao.search.ResultRow" %>
<%@ page import="com.liferay.portal.kernel.dao.search.SearchContainer" %>
<%@ page import="com.liferay.portal.kernel.dao.search.SearchEntry" %>
<%@ page import="com.liferay.portal.kernel.dao.search.TextSearchEntry" %>
<%@ page import="com.liferay.portal.kernel.language.LanguageUtil" %>
<%@ page import="com.liferay.portal.kernel.util.LocalizationUtil" %>
<%@ page import="com.liferay.portal.kernel.log.Log" %>
<%@ page import="com.liferay.portal.kernel.log.LogFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.messaging.DestinationNames" %>
<%@ page import="com.liferay.portal.kernel.messaging.MessageBusUtil" %>
<%--<%@ page import="com.liferay.portal.kernel.util.BreadcrumbsUtil" %>--%>
<%@ page import="com.liferay.portal.kernel.util.CalendarFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.Constants"%>
<%@ page import="com.liferay.portal.kernel.util.ContentTypes" %>
<%@ page import="com.liferay.portal.kernel.util.DateFormatFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.GetterUtil" %>
<%@ page import="com.liferay.portal.kernel.util.HtmlUtil" %>
<%@ page import="com.liferay.portal.kernel.util.JavaConstants" %>
<%@ page import="com.liferay.portal.kernel.util.ListUtil" %>
<%@ page import="com.liferay.portal.kernel.util.ParamUtil" %>
<%@ page import="com.liferay.portal.kernel.util.StringBundler" %>
<%@ page import="com.liferay.portal.kernel.util.StringPool" %>
<%@ page import="com.liferay.portal.kernel.util.StringUtil" %>
<%@ page import="com.liferay.portal.kernel.util.Validator" %>
<%@ page import="com.liferay.portal.kernel.util.UnicodeFormatter"%>
<%@ page import="com.liferay.portal.model.Contact" %>
<%@ page import="com.liferay.portal.model.Group" %>
<%@ page import="com.liferay.portal.model.Layout" %>
<%@ page import="com.liferay.portal.model.LayoutSet" %>
<%@ page import="com.liferay.portal.model.Organization" %>
<%@ page import="com.liferay.portal.model.Portlet" %>
<%@ page import="com.liferay.portal.model.User" %>
<%@ page import="com.liferay.portal.model.Company"%>
<%@ page import="com.liferay.portlet.asset.model.AssetCategory" %>
<%@ page import="com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil" %>
<%@ page import="com.liferay.portal.security.permission.ActionKeys" %>
<%@ page import="com.liferay.portal.service.GroupLocalServiceUtil" %>
<%@ page import="com.liferay.portal.service.OrganizationLocalServiceUtil" %>
<%@ page import="com.liferay.portal.service.LayoutLocalServiceUtil" %>
<%@ page import="com.liferay.portal.service.PortletLocalServiceUtil" %>
<%@ page import="com.liferay.portal.service.UserLocalServiceUtil" %>
<%@ page import="com.liferay.portal.service.UserGroupRoleLocalServiceUtil" %>
<%@ page import="com.liferay.portal.service.permission.GroupPermissionUtil" %>
<%@ page import="com.liferay.portal.service.permission.UserPermissionUtil" %>
<%@ page import="com.liferay.portal.util.PortalUtil" %>
<%@ page import="com.liferay.portal.util.comparator.UserLoginDateComparator" %>
<%@ page import="com.liferay.portlet.blogs.service.BlogsEntryLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.blogs.service.BlogsStatsUserLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.expando.service.ExpandoValueLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.messageboards.service.MBStatsUserLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.messageboards.service.MBMessageLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.social.model.SocialActivity" %>
<%@ page import="com.liferay.portlet.social.model.SocialRelationConstants" %>
<%@ page import="com.liferay.portlet.social.model.SocialRequestConstants" %>
<%@ page import="com.liferay.portlet.social.service.SocialActivityLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.social.service.SocialRelationLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.social.service.SocialRequestLocalServiceUtil" %>
<%@ page import="com.liferay.util.RSSUtil" %>
<%@ page import="com.liferay.util.portlet.PortletProps" %>
<%@ page import="com.liferay.portal.kernel.util.PropsKeys" %>
<%@ page import="com.liferay.portal.kernel.util.PrefsPropsUtil" %>
<%@ page import="com.liferay.portal.kernel.util.PropsUtil"%>
<%@ page import="com.liferay.portal.service.CompanyLocalServiceUtil" %>
<%@ page import="com.liferay.portal.service.ImageLocalServiceUtil" %>
<%@ page import="com.liferay.portal.model.Image" %>
<%@ page import="java.net.URLDecoder"%>

<%@page import="com.liferay.portlet.shopping.model.ShoppingItem" %>
<%@ page import="com.liferay.portlet.shopping.model.ShoppingCategory" %>
<%@ page import="com.liferay.portlet.shopping.service.ShoppingCategoryLocalServiceUtil" %>
<%@ page import="com.liferay.portlet.shopping.model.ShoppingOrder" %>
<%@ page import="com.liferay.portlet.shopping.service.ShoppingOrderItemLocalServiceUtil"%>
<%@ page import="com.liferay.portlet.shopping.model.ShoppingOrderItem" %>
<%@ page import="com.liferay.portlet.shopping.DuplicateItemSKUException" %>
<%@ page import="com.liferay.portlet.shopping.ItemLargeImageNameException" %>
<%@ page import="com.liferay.portlet.shopping.ItemLargeImageSizeException" %>
<%@ page import="com.liferay.portlet.shopping.ItemNameException" %>
<%@ page import="com.liferay.portlet.shopping.ItemSKUException" %>

<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.NumberFormat" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.LinkedHashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.Map.Entry" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="java.io.Serializable" %>

<%@ page import="org.apache.commons.lang.StringUtils" %>

<%@ page import="javax.portlet.ActionRequest" %>
<%@ page import="javax.portlet.PortletURL" %>
<%@ page import="javax.portlet.ResourceURL" %>
<%@ page import="javax.portlet.PortletPreferences" %>
<%@ page import="javax.portlet.WindowState" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="javax.portlet.PortletResponse" %>

<%@ page import="ru.emdev.util.web.WebKeys" %>

<%@page contentType="text/html"%>
<%@page pageEncoding="UTF-8"%>


<portlet:defineObjects />

<liferay-theme:defineObjects />

<%
WindowState windowState = renderRequest.getWindowState();
String currentURL = PortalUtil.getCurrentURL(request);
String imagePath = themeDisplay.getPathThemeImages() + "/custom";


DateFormat dateFormatDate = DateFormat.getDateInstance(DateFormat.LONG, locale);
dateFormatDate.setTimeZone(timeZone);
DateFormat dateFormatDateTime = DateFormatFactoryUtil.getDateTime(locale, timeZone);

// apply russian date format symbols if required
if (locale.getLanguage().equals("ru")) {
	if (dateFormatDate instanceof SimpleDateFormat) {
		SimpleDateFormat sdf = (SimpleDateFormat)dateFormatDate;
		sdf.setDateFormatSymbols(new RuDateFormatSymbols());
	}

	if (dateFormatDateTime instanceof SimpleDateFormat) {
		SimpleDateFormat sdf = (SimpleDateFormat)dateFormatDateTime;
		sdf.setDateFormatSymbols(new RuDateFormatSymbols());
	}
}

%>
