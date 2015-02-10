package ru.emdev.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.captcha.CaptchaMaxChallengesException;
import com.liferay.portal.kernel.captcha.CaptchaTextException;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.FileUtil;
import com.liferay.portal.kernel.util.Http;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.util.PropsUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Company;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.GroupConstants;
import com.liferay.portal.model.LayoutSet;
import com.liferay.portal.model.User;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.service.CompanyLocalServiceUtil;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.service.UserLocalServiceUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.AssetCategoryException;
import com.liferay.portlet.asset.AssetTagException;
import com.liferay.portlet.documentlibrary.FileExtensionException;
import com.liferay.portlet.documentlibrary.FileNameException;
import com.liferay.portlet.documentlibrary.FileSizeException;
import com.liferay.portlet.messageboards.LockedThreadException;
import com.liferay.portlet.messageboards.MessageBodyException;
import com.liferay.portlet.messageboards.MessageSubjectException;
import com.liferay.portlet.messageboards.NoSuchMessageException;
import com.liferay.portlet.messageboards.RequiredMessageException;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.service.MBMessageServiceUtil;

public class GeneralUtil {

    private static Log log = LogFactoryUtil.getLog(GeneralUtil.class);

    // getting messages from bundles
    private static Map<Locale, ResourceBundle> messagesMap = new HashMap<Locale, ResourceBundle>();

    static {
        for (Locale locale : LanguageUtil.getAvailableLocales()) {
            try {
                ResourceBundle resources = ResourceBundle.getBundle("content/messages", locale);
                if (resources != null) {
                    messagesMap.put(locale, resources);
                } else {
                    log.warn("No message bundle exist for available locale [" + locale.getDisplayName() + "]");
                }
            } catch (Exception ex) {
                // just ignore error. In case we do not have messages bundle in
                // the portlet it should not stop us
            }
        }
    }

    public static String getMessageFromBundle(Locale locale, String key) {

        ResourceBundle rb = messagesMap.get(locale);
        if (rb == null) {
            return key;
        }

        return rb.getString(key);
    }

    /**
     * Method used by all portlets supported forums
     * 
     * @param request
     * @param response
     * @throws Exception
     */
    public static MBMessage addDiscussion(ActionRequest request, ActionResponse response) throws Exception {
        String cmd = ParamUtil.getString(request, Constants.CMD);

        try {
            if (cmd.equals(Constants.ADD) || cmd.equals(Constants.UPDATE)) {
                return updateMessage(request);
            }
            else if (cmd.equals(Constants.DELETE)) {
                deleteMessage(request);
                return null;
            }
        } catch (Exception e) {
            if (e instanceof NoSuchMessageException ||
                    e instanceof PrincipalException ||
                    e instanceof RequiredMessageException) {

                SessionErrors.add(request, e.getClass().getName());

            }
            else if (e instanceof CaptchaMaxChallengesException ||
                    e instanceof CaptchaTextException ||
                    e instanceof FileExtensionException ||
                    e instanceof FileNameException ||
                    e instanceof FileSizeException ||
                    e instanceof LockedThreadException ||
                    e instanceof MessageBodyException ||
                    e instanceof MessageSubjectException) {

                SessionErrors.add(request, e.getClass().getName());
            }
            else if (e instanceof AssetCategoryException ||
                    e instanceof AssetTagException) {

                SessionErrors.add(request, e.getClass().getName(), e);
            }
            else {
                throw e;
            }
        }

        return null;
    }

    protected static void deleteMessage(ActionRequest actionRequest) throws Exception {
        long groupId = PortalUtil.getScopeGroupId(actionRequest);

        String className = ParamUtil.getString(actionRequest, "className");
        long classPK = ParamUtil.getLong(actionRequest, "classPK");
        String permissionClassName = ParamUtil.getString(
                actionRequest, "permissionClassName");
        long permissionClassPK = ParamUtil.getLong(
                actionRequest, "permissionClassPK");
        long permissionOwnerId = ParamUtil.getLong(
                actionRequest, "permissionOwnerId");

        long messageId = ParamUtil.getLong(actionRequest, "messageId");

        MBMessageServiceUtil.deleteDiscussionMessage(
                groupId, className, classPK, permissionClassName, permissionClassPK, permissionOwnerId,
                messageId);
    }

    protected static MBMessage updateMessage(ActionRequest actionRequest) throws Exception {

        String className = ParamUtil.getString(actionRequest, "className");
        long classPK = ParamUtil.getLong(actionRequest, "classPK");
        String permissionClassName = ParamUtil.getString(actionRequest, "permissionClassName");
        long permissionClassPK = ParamUtil.getLong(actionRequest, "permissionClassPK");
        long permissionOwnerId = ParamUtil.getLong(actionRequest, "permissionOwnerId");

        long messageId = ParamUtil.getLong(actionRequest, "messageId");

        long threadId = ParamUtil.getLong(actionRequest, "threadId");
        long parentMessageId = ParamUtil.getLong(actionRequest, "parentMessageId");
        String subject = ParamUtil.getString(actionRequest, "subject");
        String body = ParamUtil.getString(actionRequest, "body");

        ServiceContext serviceContext = ServiceContextFactory.getInstance(MBMessage.class.getName(), actionRequest);

        MBMessage message = null;

        if (messageId <= 0) {

            // Add message

            message = MBMessageServiceUtil.addDiscussionMessage(serviceContext.getScopeGroupId(), className, classPK,
                    permissionClassName, permissionClassPK, permissionOwnerId,
                    threadId, parentMessageId, subject, body, serviceContext);
        } else {

            // Update message

            message = MBMessageServiceUtil.updateDiscussionMessage(className, classPK,
                    permissionClassName, permissionClassPK, permissionOwnerId,
                    messageId, subject, body,
                    serviceContext);
        }

        return message;
    }

    public static String getLayoutSetFriendlyURL(LayoutSet layoutSet,
            ThemeDisplay themeDisplay, boolean alwaysUseDomainName)
        throws PortalException, SystemException {
        return getLayoutSetFriendlyURL(layoutSet, themeDisplay, null,
                alwaysUseDomainName);
    }

    /**
     * It is almost copy of PortalImpl.getLayoutSetFriendlyURL but add one
     * option - always use domain name
     * 
     * @param layoutSet
     * @param themeDisplay
     * @return
     * @throws PortalException
     * @throws SystemException
     */
    public static String getLayoutSetFriendlyURL(LayoutSet layoutSet,
            ThemeDisplay themeDisplay, Locale userLocale,
            boolean alwaysUseDomainName) throws PortalException,
        SystemException {

        String virtualHost = layoutSet.getVirtualHostname();

        if (Validator.isNull(virtualHost)
                && Validator.isNotNull(PropsUtil
                        .get(PropsKeys.VIRTUAL_HOSTS_DEFAULT_SITE_NAME))
                && !layoutSet.isPrivateLayout()) {

            try {
                Group group = GroupLocalServiceUtil.getGroup(layoutSet
                        .getCompanyId(), PropsUtil
                        .get(PropsKeys.VIRTUAL_HOSTS_DEFAULT_SITE_NAME));

                if (layoutSet.getGroupId() == group.getGroupId()) {
                    Company company = CompanyLocalServiceUtil
                            .getCompany(layoutSet.getCompanyId());

                    virtualHost = company.getVirtualHostname();
                }
            } catch (Exception e) {
                log.error(e, e);
            }
        }

        if (Validator.isNotNull(virtualHost)) {
            String portalURL = null;

            if (themeDisplay != null) {
                portalURL = PortalUtil.getPortalURL(virtualHost,
                        themeDisplay.getServerPort(), themeDisplay.isSecure());
            } else {
                // ignore secure and port
                portalURL = PortalUtil.getPortalURL(virtualHost,
                        Http.HTTP_PORT, false);
            }

            // Use the layout set's virtual host setting only if the layout set
            // is already used for the current request

            if (themeDisplay != null) { // use it only if themeDisplay specified
                long curLayoutSetId = themeDisplay.getLayout().getLayoutSet()
                        .getLayoutSetId();

                if ((layoutSet.getLayoutSetId() != curLayoutSetId)
                        || (portalURL.startsWith(themeDisplay.getURLPortal()))) {

                    String layoutSetFriendlyURL = StringPool.BLANK;

                    if (themeDisplay.isI18n()) {
                        layoutSetFriendlyURL = themeDisplay.getI18nPath();
                    }

                    return portalURL + PortalUtil.getPathContext()
                            + layoutSetFriendlyURL;
                }
            }
        }

        Group group = GroupLocalServiceUtil.getGroup(layoutSet.getGroupId());

        String friendlyURL = null;

        if (layoutSet.isPrivateLayout()) {
            if (group.isUser()) {
                friendlyURL = PropsUtil
                        .get(PropsKeys.LAYOUT_FRIENDLY_URL_PRIVATE_USER_SERVLET_MAPPING);
            } else {
                friendlyURL = PropsUtil
                        .get(PropsKeys.LAYOUT_FRIENDLY_URL_PRIVATE_GROUP_SERVLET_MAPPING);
            }
        } else {
            friendlyURL = PropsUtil
                    .get(PropsKeys.LAYOUT_FRIENDLY_URL_PUBLIC_SERVLET_MAPPING);
        }

        if (userLocale != null) {
            // this code copied from PortalImpl
            String tempI18nLanguageId = userLocale.toString();

            String tempI18nPath = StringPool.SLASH + tempI18nLanguageId;

            if (!LanguageUtil.isDuplicateLanguageCode(userLocale.getLanguage())) {

                tempI18nPath = StringPool.SLASH + userLocale.getLanguage();
            } else {
                Locale priorityLocale = LanguageUtil.getLocale(userLocale
                        .getLanguage());

                if (userLocale.equals(priorityLocale)) {
                    tempI18nPath = StringPool.SLASH + userLocale.getLanguage();
                }
            }

            if (StringUtils.isNotEmpty(tempI18nPath)) {
                friendlyURL = tempI18nPath + friendlyURL;
            }
        } else {
            if (themeDisplay != null && themeDisplay.isI18n()) {
                friendlyURL = themeDisplay.getI18nPath() + friendlyURL;
            }
        }

        if (alwaysUseDomainName) {
            String portalUrl = null;

            if (themeDisplay != null) {
                portalUrl = PortalUtil.getPortalURL(themeDisplay);
            } else {
                Company company = CompanyLocalServiceUtil.getCompany(layoutSet
                        .getCompanyId());
                portalUrl = PortalUtil.getPortalURL(company.getVirtualHostname(),
                        Http.HTTP_PORT, false);
            }

            return portalUrl + PortalUtil.getPathContext() + friendlyURL
                    + group.getFriendlyURL();
        } else {
            return PortalUtil.getPathContext() + friendlyURL
                    + group.getFriendlyURL();
        }
    }

    /**
     * Translates a double into a BigDecimal to show it without exponent.
     * Returns String to display a number in a correct way in portlets.
     * 
     * @param numberToFormat
     * @param locale
     *            TODO
     * @return
     */
    public static String formatDoubleNumber(double numberToFormat, Locale locale) {
        NumberFormat numberFormat = null;
        if (locale != null) {
            numberFormat = NumberFormat.getNumberInstance(locale);
        } else {
            numberFormat = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
        }
        numberFormat.setMaximumFractionDigits(2);
        numberFormat.setGroupingUsed(false);
        String formattedNumber = numberFormat.format(numberToFormat);
        return formattedNumber;
    }

    public static SimpleDateFormat getShortDateFormat(Locale locale) {
        SimpleDateFormat df;
        if (locale.getLanguage().equals("ru")) {
            df = new SimpleDateFormat("dd MMMM", new RuDateFormatSymbols());

        } else {
            df = new SimpleDateFormat("dd MMMM", locale);
        }
        return df;
    }

    public static SimpleDateFormat getFullDateFormat(Locale locale) {
        SimpleDateFormat df;
        if (locale.getLanguage().equals("ru")) {
            df = new SimpleDateFormat("dd MMMM yyyy", new RuDateFormatSymbols());

        } else {
            df = new SimpleDateFormat("dd MMMM yyyy", locale);
        }
        return df;
    }

    public static SimpleDateFormat getDayOfPublication(Locale locale) {
        SimpleDateFormat df;
        if (locale.getLanguage().equals("ru")) {
            df = new SimpleDateFormat("dd", new RuDateFormatSymbols());

        } else {
            df = new SimpleDateFormat("dd", locale);
        }
        return df;
    }

    public static SimpleDateFormat getMonthOfPublication(Locale locale) {
        SimpleDateFormat df;
        if (locale.getLanguage().equals("ru")) {
            df = new SimpleDateFormat("MMMM", new RuDateFormatSymbols());

        } else {
            df = new SimpleDateFormat("MMMM", locale);
        }
        return df;
    }

    public static SimpleDateFormat getYearOfPublication(Locale locale) {
        SimpleDateFormat df;
        if (locale.getLanguage().equals("ru")) {
            df = new SimpleDateFormat("yyyy", new RuDateFormatSymbols());

        } else {
            df = new SimpleDateFormat("yyyy", locale);
        }
        return df;
    }

    public static String formatDoubleNumber(double numberToFormat) {
        return formatDoubleNumber(numberToFormat, null);
    }

    public static double parseDoubleNumber(HttpServletRequest request, String doubleString, Locale locale) {
        double parsedNumber = -1;
        try {
            NumberFormat numberFormat = null;
            if (locale != null) {
                numberFormat = NumberFormat.getNumberInstance(locale);
            } else {
                log.warn("Use locale from request [" + request.getLocale() + "]");
                numberFormat = NumberFormat.getNumberInstance(request.getLocale());
            }
            ParsePosition parsePosition = new ParsePosition(0);
            numberFormat.setMaximumFractionDigits(2);
            Number number = numberFormat.parse(doubleString, parsePosition);
            if (doubleString.length() != parsePosition.getIndex() || number == null) {
                log.error("Error while parsing price [" + doubleString + "] using format for [" + request.getLocale()
                        + "] locale. Parse position index is not equal to parsed string length.");
                return parsedNumber;
            }

            if (number instanceof Double) {
                parsedNumber = number.doubleValue();
                log.debug("Parse as double [" + parsedNumber + "]");
            } else {
                parsedNumber = number.longValue();
                log.debug("Parse as long [" + parsedNumber + "]");
            }
        } catch (Exception e) {
            log.error("Unable to parse shopping item price [" + doubleString + "] using format for ["
                    + request.getLocale() + "] locale. Error:" + e.getMessage());
        }
        return parsedNumber;
    }

    public static HttpSession getHttpSession(PortletRequest request) {
        HttpServletRequest httpServletRequest = PortalUtil.getHttpServletRequest(request);
        HttpSession httpSession = httpServletRequest.getSession();
        return httpSession;
    }

    /**
     * Sometimes it is required to use service context for objects that it is
     * not intended for initially. In this case we have to clean permissions
     * cause it is possible to get no such resource action exception.
     * 
     * @param serviceContext
     * @param groupPermissions
     * @param guestPermissions
     * @return
     */
    public static ServiceContext cleanServiceContextPermissions(ServiceContext serviceContext,
            String[] groupPermissions, String[] guestPermissions) {
        // clone initial object to keep it untouched
        ServiceContext sContext = (ServiceContext) serviceContext.clone();
        // clean permissions cause they intended to serve other object
        if (groupPermissions == null) {
            groupPermissions = new String[0];
        }
        sContext.setGroupPermissions(groupPermissions);

        if (guestPermissions == null) {
            guestPermissions = new String[0];
        }
        sContext.setGuestPermissions(guestPermissions);

        return sContext;
    }

    /**
     * Get list of users for specified group
     * 
     * @param themeDisplay
     * @return
     * @throws SystemException
     */
    public static List<User> getUsers(Group group) throws SystemException {
        List<User> users = null;
        if (GroupConstants.GUEST.equals(group.getName())) {
            // guest community - get all users
            users = UserLocalServiceUtil.getCompanyUsers(group.getCompanyId(), QueryUtil.ALL_POS, QueryUtil.ALL_POS);
        } else if (group.isOrganization()) {
            // get users in specific organization
            users = UserLocalServiceUtil.getOrganizationUsers(group.getClassPK());
        } else {
            users = UserLocalServiceUtil.getGroupUsers(group.getGroupId());
        }

        return users;
    }

    /**
     * save uploaded file with name "fileName" to temp dir
     * 
     * TODO this method was added in CRM by Vladimir and need to be reviewed
     * 
     * @param request
     * @param fileName
     * @return
     * @throws IOException
     */
    public static File saveUploadFileToTempDir(UploadPortletRequest uploadRequest, String fileName) throws IOException {
        InputStream inputStream = uploadRequest.getFileAsStream(fileName);
        File uploadFile = File.createTempFile("upload_",
                "." + FileUtil.getExtension(uploadRequest.getFileName(fileName)));
        OutputStream out = new FileOutputStream(uploadFile);

        IOUtils.copy(inputStream, out);

        out.close();
        return uploadFile;
    }

}
