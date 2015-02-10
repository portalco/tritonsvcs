package ru.emdev.portlet;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import ru.emdev.util.DateUtil;
import ru.emdev.util.web.WebKeys;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.proxy.MessagingProxy;
import com.liferay.portal.kernel.messaging.proxy.ProxyMode;
import com.liferay.portal.kernel.messaging.proxy.ProxyModeThreadLocal;
import com.liferay.portal.kernel.portlet.PortletBag;
import com.liferay.portal.kernel.portlet.PortletBagPool;
import com.liferay.portal.kernel.servlet.BrowserSnifferUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.EmDevMethodCache;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.UnicodeProperties;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.PortletPreferencesIds;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.service.ServiceContextThreadLocal;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portlet.expando.ValueDataException;
import com.liferay.portlet.expando.model.ExpandoBridge;
import com.liferay.portlet.expando.model.ExpandoColumnConstants;
import com.liferay.portlet.expando.util.ExpandoBridgeFactoryUtil;
import com.liferay.util.bridges.mvc.ActionCommandCache;
import com.liferay.util.bridges.mvc.MVCPortlet;

/**
 * Base class for all portlets
 * 
 */
public class BasePortlet extends MVCPortlet {

    private static Log log = LogFactoryUtil.getLog(BasePortlet.class);

    public static final String ATTR_SKIP_REDIRECT = "__skip_redirect__";

    /**
     * Pverride render to fix "Back to full page" link for maximized portlets
     * default behavior is not friendly - it is much better to act as "cancel" -
     * redirect back to link specified in "redirect" paramter (if exists)
     */
    @Override
    public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        // is maximized window state?
        WindowState windowState = request.getWindowState();
        if (windowState.equals(WindowState.MAXIMIZED)) {
            // try to get redirect param
            String redirect = ParamUtil.getString(request, WebKeys.REDIRECT);
            if (StringUtils.isNotEmpty(redirect)) {
                ThemeDisplay themeDisplay = getThemeDisplay(request);
                themeDisplay.getPortletDisplay().setURLBack(redirect);
            }
        }

        super.render(request, response);
    }

    /**
     * Send redirect to url, stored in redirect attribute
     * 
     * @param actionRequest
     * @param actionResponse
     * @throws IOException
     */
    protected void sendRedirect(
            ActionRequest actionRequest, ActionResponse actionResponse)
        throws IOException {

        sendRedirect(actionRequest, actionResponse, null);
    }

    /**
     * Send redirect to specified url If url is not set - use value stored in
     * redirect param
     * 
     * @param actionRequest
     * @param actionResponse
     * @param redirect
     * @throws IOException
     */
    public static void sendRedirect(ActionRequest actionRequest, ActionResponse actionResponse, String redirect)
        throws IOException {
        log.debug("Redirect send to " + redirect);

        if (SessionErrors.isEmpty(actionRequest)) {
            String successMessage = ParamUtil.getString(actionRequest, "successMessage");
            // check attribute as well - workaround in cause of upload request
            // (it can be retrieved only once)
            if (StringUtils.isBlank(successMessage)) {
                Object attMsg = actionRequest.getAttribute("successMessage");
                successMessage = attMsg instanceof String ? (String) attMsg : StringPool.BLANK;
            }

            SessionMessages.add(
                    actionRequest, "request_processed", successMessage);
        }

        if (redirect == null) {
            redirect = ParamUtil.getString(actionRequest, "redirect");
        }

        if (Validator.isNotNull(redirect)) {

            // LPS-1928

            HttpServletRequest request = PortalUtil.getHttpServletRequest(
                    actionRequest);

            if (BrowserSnifferUtil.isIe(request) &&
                    (BrowserSnifferUtil.getMajorVersion(request) == 6.0)) {

                redirect = StringUtil.replace(redirect, StringPool.POUND, "&#");
            }

            actionResponse.sendRedirect(redirect);
        }
    }

    /**
     * Override basic processAction to do not send redirect on errors and to
     * keep redirect attribute in attributes
     */
    @Override
    public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws IOException,
        PortletException {
        if (!isProcessActionRequest(actionRequest)) {
            return;
        }

        if (!callActionMethod(actionRequest, actionResponse)) {
            return;
        }

        if (SessionErrors.isEmpty(actionRequest) && !isSkipRedirect(actionRequest)) {
            // AKA - I currently commented it - it is better to add this message
            // in each action we need it to be added
            // SessionMessages.add(actionRequest, "request_processed");

            String redirect = ParamUtil.getString(actionRequest, "redirect");

            if (Validator.isNotNull(redirect)) {
                actionResponse.sendRedirect(redirect);
            }
        } else {
            // stay on same form (do not send redirect)
            // also keep redirect in attributes - to restore late
            String redirect = ParamUtil.getString(actionRequest, "redirect");

            if (Validator.isNotNull(redirect)) {
                actionRequest.setAttribute("redirect", redirect);
            }
        }
    }

    @Override
    protected boolean callActionMethod(
            ActionRequest request, ActionResponse response)
        throws PortletException {

        String packagePrefix = getInitParameter(ActionCommandCache.ACTION_PACKAGE_NAME);

        if (Validator.isNotNull(packagePrefix)) {
            return super.callActionMethod(request, response);
        }

        String actionName = ParamUtil.getString(request, ActionRequest.ACTION_NAME);

        try {
            Method method = EmDevMethodCache.get(getClass(), actionName,
                    new Class[] {ActionRequest.class, ActionResponse.class});

            MessagingProxy messagingProxy = method.getAnnotation(MessagingProxy.class);

            if (messagingProxy == null) {
                messagingProxy = method.getDeclaringClass().getAnnotation(MessagingProxy.class);
            }

            if ((messagingProxy != null) && messagingProxy.mode().equals(ProxyMode.SYNC)) {

                boolean isForceSync = ProxyModeThreadLocal.isForceSync();
                try {
                    /* Set forceSync to true for synchronous messaging */
                    if (!isForceSync) {
                        ProxyModeThreadLocal.setForceSync(true);
                    }
                    return super.callActionMethod(request, response);
                } finally {
                    /* Restore value */
                    if (!isForceSync) {
                        ProxyModeThreadLocal.setForceSync(isForceSync);
                    }
                }
            } else {
                return super.callActionMethod(request, response);
            }
        } catch (NoSuchMethodException nsme) {
            return super.callActionMethod(request, response);
        }
    }

    /**
     * returns themeDisplay
     * 
     * @param request
     * @return
     */
    public static ThemeDisplay getThemeDisplay(PortletRequest request) {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);

        return themeDisplay;
    }

    /**
     * Sometimes we need to say to do not go by redirect parameter after
     * processing action
     * 
     * @param request
     */
    protected void skipRedirect(PortletRequest request) {
        request.setAttribute(ATTR_SKIP_REDIRECT, true);
    }

    protected boolean isSkipRedirect(PortletRequest request) {
        Boolean skipRedirect = (Boolean) request.getAttribute(ATTR_SKIP_REDIRECT);

        return skipRedirect != null ? skipRedirect : false;
    }

    protected Portlet getPortlet(PortletRequest request) {
        return ((Portlet) request.getAttribute(WebKeys.RENDER_PORTLET));
    }

    public ServletContext getServletContext(PortletRequest request) {
        String portletId = getPortlet(request).getPortletId();
        PortletBag portletBag = PortletBagPool.get(portletId);
        ServletContext servletContext = portletBag.getServletContext();
        return servletContext;
    }

    /**
     * Copy request parameters into response request
     * 
     * Used to pass values from original request sent into action into render -
     * to restore entered values
     * 
     * @param request
     * @param response
     */
    public void copyRequestParameters(HttpServletRequest request, ActionResponse response) {
        // copy parameters from action request to response
        for (Object key : request.getParameterMap().keySet()) {
            String val = request.getParameter((String) key);
            if (val != null && !"image".equals(key) && !((String) key).startsWith("fileName")) {
                response.setRenderParameter((String) key, val);
            }
        }
    }

    /**
     * Copy request parameters into response request
     * 
     * Used to pass values from original request sent into action into render -
     * to restore entered values
     * 
     * @param request
     * @param response
     */
    public void copyRequestParameters(ActionRequest request, ActionResponse response) {
        // copy parameters from action request to response
        for (Object key : request.getParameterMap().keySet()) {
            String val = request.getParameter((String) key);
            if (val != null && !"image".equals(key) && !((String) key).startsWith("fileName")) {
                response.setRenderParameter((String) key, val);
            }
        }
    }

    /**
     * Put attribute into request in case request hasn't neiver attribute nor
     * param with this name
     * 
     * @param request
     * @param name
     * @param value
     */
    public static void putAttribute(PortletRequest request, String name, Object value) {
        if (request.getAttribute(name) == null && request.getParameter(name) == null) {
            request.setAttribute(name, value);
        }
    }

    /**
     * Try to get value from attribute -if it is not exists - try to get it from
     * parameter
     * 
     * @param request
     * @param name
     * @return
     */
    public static Object getParamAttribute(HttpServletRequest request, String name) {
        if (request.getAttribute(name) != null) {
            return request.getAttribute(name);
        } else {
            return request.getParameter(name);
        }
    }

    /**
     * Get date from request
     * 
     * @param request
     * @param prefix
     * @return
     */
    protected Date getDateFromRequest(PortletRequest request, String prefix) {
        return DateUtil.getDateFromRequest(request, prefix);
    }

    /**
     * Get date from upload request
     * 
     * @param request
     * @param prefix
     * @return
     */
    protected Date getDateFromRequest(UploadPortletRequest request, String prefix) {
        return DateUtil.getDateFromRequest(request, prefix);
    }

    @Override
    protected String getTitle(RenderRequest request) {
        String key = "javax.portlet.title";
        Object portletId = request.getAttribute(WebKeys.PORTLET_ID);
        String resource = LanguageUtil.get(request.getLocale(), key + "." + portletId, StringPool.BLANK);
        if (StringUtils.isNotBlank(resource)) {
            return resource;
        } else {
            // default implementation:
            // getPortletConfig().getResourceBundle(request.getLocale()).getString(key);
            return super.getTitle(request);
        }
    }

    protected ServiceContext getServiceContext(String className, UploadPortletRequest request) throws PortalException,
        SystemException {
        ServiceContext serviceContext = ServiceContextFactory.getInstance(request);

        // Expando

        Map<String, Serializable> expandoBridgeAttributes =
                getExpandoBridgeAttributes(
                        ExpandoBridgeFactoryUtil.getExpandoBridge(
                                serviceContext.getCompanyId(), className),
                        request);

        serviceContext.setExpandoBridgeAttributes(expandoBridgeAttributes);

        return serviceContext;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Serializable> getExpandoBridgeAttributes(
            ExpandoBridge expandoBridge, UploadPortletRequest request)
        throws PortalException, SystemException {

        Map<String, Serializable> attributes =
                new HashMap<String, Serializable>();

        List<String> names = new ArrayList<String>();

        Enumeration<String> enu = request.getParameterNames();

        while (enu.hasMoreElements()) {
            String param = enu.nextElement();

            if (param.indexOf("ExpandoAttributeName--") != -1) {
                String name = ParamUtil.getString(request, param);

                names.add(name);
            }
        }

        for (String name : names) {
            int type = expandoBridge.getAttributeType(name);

            UnicodeProperties properties = expandoBridge.getAttributeProperties(
                    name);

            String displayType = GetterUtil.getString(
                    properties.getProperty(
                            ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE),
                    ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX);

            Serializable value = getExpandoValue(
                    request, "ExpandoAttribute--" + name + "--", type,
                    displayType);

            attributes.put(name, value);
        }

        return attributes;
    }

    protected Serializable getExpandoValue(
            UploadPortletRequest request, String name, int type,
            String displayType)
        throws PortalException, SystemException {

        Serializable value = null;

        if (type == ExpandoColumnConstants.BOOLEAN) {
            value = ParamUtil.getBoolean(request, name);
        }
        else if (type == ExpandoColumnConstants.BOOLEAN_ARRAY) {}
        else if (type == ExpandoColumnConstants.DATE) {
            int valueDateMonth = ParamUtil.getInteger(
                    request, name + "Month");
            int valueDateDay = ParamUtil.getInteger(
                    request, name + "Day");
            int valueDateYear = ParamUtil.getInteger(
                    request, name + "Year");
            int valueDateHour = ParamUtil.getInteger(
                    request, name + "Hour");
            int valueDateMinute = ParamUtil.getInteger(
                    request, name + "Minute");
            int valueDateAmPm = ParamUtil.getInteger(
                    request, name + "AmPm");

            if (valueDateAmPm == Calendar.PM) {
                valueDateHour += 12;
            }

            TimeZone timeZone = null;

            User user = PortalUtil.getUser(request);

            if (user != null) {
                timeZone = user.getTimeZone();
            }

            value = PortalUtil.getDate(
                    valueDateMonth, valueDateDay, valueDateYear, valueDateHour,
                    valueDateMinute, timeZone, ValueDataException.class);
        }
        else if (type == ExpandoColumnConstants.DATE_ARRAY) {}
        else if (type == ExpandoColumnConstants.DOUBLE) {
            value = ParamUtil.getDouble(request, name);
        }
        else if (type == ExpandoColumnConstants.DOUBLE_ARRAY) {
            String[] values = request.getParameterValues(name);

            if (displayType.equals(
                    ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX)) {

                values = StringUtil.splitLines(values[0]);
            }

            value = GetterUtil.getDoubleValues(values);
        }
        else if (type == ExpandoColumnConstants.FLOAT) {
            value = ParamUtil.getFloat(request, name);
        }
        else if (type == ExpandoColumnConstants.FLOAT_ARRAY) {
            String[] values = request.getParameterValues(name);

            if (displayType.equals(
                    ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX)) {

                values = StringUtil.splitLines(values[0]);
            }

            value = GetterUtil.getFloatValues(values);
        }
        else if (type == ExpandoColumnConstants.INTEGER) {
            value = ParamUtil.getInteger(request, name);
        }
        else if (type == ExpandoColumnConstants.INTEGER_ARRAY) {
            String[] values = request.getParameterValues(name);

            if (displayType.equals(
                    ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX)) {

                values = StringUtil.splitLines(values[0]);
            }

            value = GetterUtil.getIntegerValues(values);
        }
        else if (type == ExpandoColumnConstants.LONG) {
            value = ParamUtil.getLong(request, name);
        }
        else if (type == ExpandoColumnConstants.LONG_ARRAY) {
            String[] values = request.getParameterValues(name);

            if (displayType.equals(
                    ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX)) {

                values = StringUtil.splitLines(values[0]);
            }

            value = GetterUtil.getLongValues(values);
        }
        else if (type == ExpandoColumnConstants.SHORT) {
            value = ParamUtil.getShort(request, name);
        }
        else if (type == ExpandoColumnConstants.SHORT_ARRAY) {
            String[] values = request.getParameterValues(name);

            if (displayType.equals(
                    ExpandoColumnConstants.PROPERTY_DISPLAY_TYPE_TEXT_BOX)) {

                values = StringUtil.splitLines(values[0]);
            }

            value = GetterUtil.getShortValues(values);
        }
        else if (type == ExpandoColumnConstants.STRING_ARRAY) {
            value = request.getParameterValues(name);
        }
        else {
            value = ParamUtil.getString(request, name);
        }

        return value;
    }

    protected Serializable getExpandoValue(UploadPortletRequest request, String name, int type)
        throws PortalException, SystemException {

        Serializable value = null;

        if (type == ExpandoColumnConstants.BOOLEAN) {
            value = ParamUtil.getBoolean(request, name);
        }
        else if (type == ExpandoColumnConstants.BOOLEAN_ARRAY) {}
        else if (type == ExpandoColumnConstants.DATE) {
            User user = PortalUtil.getUser(request);

            int valueDateMonth = ParamUtil.getInteger(
                    request, name + "Month");
            int valueDateDay = ParamUtil.getInteger(
                    request, name + "Day");
            int valueDateYear = ParamUtil.getInteger(
                    request, name + "Year");
            int valueDateHour = ParamUtil.getInteger(
                    request, name + "Hour");
            int valueDateMinute = ParamUtil.getInteger(
                    request, name + "Minute");
            int valueDateAmPm = ParamUtil.getInteger(
                    request, name + "AmPm");

            if (valueDateAmPm == Calendar.PM) {
                valueDateHour += 12;
            }

            value = PortalUtil.getDate(
                    valueDateMonth, valueDateDay, valueDateYear, valueDateHour,
                    valueDateMinute, user.getTimeZone(), ValueDataException.class);
        }
        else if (type == ExpandoColumnConstants.DATE_ARRAY) {}
        else if (type == ExpandoColumnConstants.DOUBLE) {
            value = ParamUtil.getDouble(request, name);
        }
        else if (type == ExpandoColumnConstants.DOUBLE_ARRAY) {
            String[] values = StringUtil.split(
                    ParamUtil.getString(request, name), StringPool.NEW_LINE);

            value = GetterUtil.getDoubleValues(values);
        }
        else if (type == ExpandoColumnConstants.FLOAT) {
            value = ParamUtil.getFloat(request, name);
        }
        else if (type == ExpandoColumnConstants.FLOAT_ARRAY) {
            String[] values = StringUtil.split(
                    ParamUtil.getString(request, name), StringPool.NEW_LINE);

            value = GetterUtil.getFloatValues(values);
        }
        else if (type == ExpandoColumnConstants.INTEGER) {
            value = ParamUtil.getInteger(request, name);
        }
        else if (type == ExpandoColumnConstants.INTEGER_ARRAY) {
            String[] values = StringUtil.split(
                    ParamUtil.getString(request, name), StringPool.NEW_LINE);

            value = GetterUtil.getIntegerValues(values);
        }
        else if (type == ExpandoColumnConstants.LONG) {
            value = ParamUtil.getLong(request, name);
        }
        else if (type == ExpandoColumnConstants.LONG_ARRAY) {
            String[] values = StringUtil.split(
                    ParamUtil.getString(request, name), StringPool.NEW_LINE);

            value = GetterUtil.getLongValues(values);
        }
        else if (type == ExpandoColumnConstants.SHORT) {
            value = ParamUtil.getShort(request, name);
        }
        else if (type == ExpandoColumnConstants.SHORT_ARRAY) {
            String[] values = StringUtil.split(
                    ParamUtil.getString(request, name), StringPool.NEW_LINE);

            value = GetterUtil.getShortValues(values);
        }
        else if (type == ExpandoColumnConstants.STRING_ARRAY) {
            value = StringUtil.split(
                    ParamUtil.getString(request, name), StringPool.NEW_LINE);
        }
        else {
            value = ParamUtil.getString(request, name);
        }

        return value;
    }

    protected static ServiceContext getServiceContext(UploadPortletRequest request) throws PortalException,
        SystemException {

        // Theme display

        ServiceContext serviceContext =
                ServiceContextThreadLocal.getServiceContext();

        ThemeDisplay themeDisplay =
                (ThemeDisplay) request.getAttribute(
                        WebKeys.THEME_DISPLAY);

        if (serviceContext != null) {
            serviceContext = (ServiceContext) serviceContext.clone();
        }
        else {
            serviceContext = new ServiceContext();

            serviceContext.setCompanyId(themeDisplay.getCompanyId());
            serviceContext.setLanguageId(themeDisplay.getLanguageId());
            serviceContext.setLayoutFullURL(
                    PortalUtil.getLayoutFullURL(themeDisplay));
            serviceContext.setLayoutURL(PortalUtil.getLayoutURL(themeDisplay));
            serviceContext.setPathMain(PortalUtil.getPathMain());
            serviceContext.setPlid(themeDisplay.getPlid());
            serviceContext.setPortalURL(
                    PortalUtil.getPortalURL(request));
            serviceContext.setUserDisplayURL(
                    themeDisplay.getUser().getDisplayURL(themeDisplay));
            serviceContext.setUserId(themeDisplay.getUserId());
        }

        serviceContext.setScopeGroupId(themeDisplay.getScopeGroupId());

        // Attributes

        Map<String, Serializable> attributes =
                new HashMap<String, Serializable>();

        Enumeration<String> enu = request.getParameterNames();

        while (enu.hasMoreElements()) {
            String param = enu.nextElement();

            String[] values = request.getParameterValues(param);

            if ((values != null) && (values.length > 0)) {
                if (values.length == 1) {
                    attributes.put(param, values[0]);
                }
                else {
                    attributes.put(param, values);
                }
            }
        }

        serviceContext.setAttributes(attributes);

        // Command

        String cmd = ParamUtil.getString(request, Constants.CMD);

        serviceContext.setCommand(cmd);

        // Permissions

        boolean addCommunityPermissions = ParamUtil.getBoolean(
                request, "addCommunityPermissions");
        boolean addGuestPermissions = ParamUtil.getBoolean(
                request, "addGuestPermissions");
        String[] communityPermissions = PortalUtil.getGroupPermissions(
                request);
        String[] guestPermissions = PortalUtil.getGuestPermissions(
                request);

        serviceContext.setAddCommunityPermissions(addCommunityPermissions);
        serviceContext.setAddGuestPermissions(addGuestPermissions);
        serviceContext.setCommunityPermissions(communityPermissions);
        serviceContext.setGuestPermissions(guestPermissions);

        // Portlet preferences ids

        String portletId = PortalUtil.getPortletId(request);

        PortletPreferencesIds portletPreferencesIds =
                PortletPreferencesFactoryUtil.getPortletPreferencesIds(
                        request, portletId);

        serviceContext.setPortletPreferencesIds(portletPreferencesIds);

        // Asset

        long[] assetCategoryIds = StringUtil.split(
                ParamUtil.getString(request, "assetCategoryIds"), 0L);
        String[] assetTagNames = StringUtil.split(
                ParamUtil.getString(request, "assetTagNames"));

        serviceContext.setAssetCategoryIds(assetCategoryIds);
        serviceContext.setAssetTagNames(assetTagNames);

        // Workflow

        int workflowAction = ParamUtil.getInteger(
                request, "workflowAction", WorkflowConstants.ACTION_PUBLISH);

        serviceContext.setWorkflowAction(workflowAction);

        return serviceContext;
    }

}
