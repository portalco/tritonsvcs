package ru.emdev.portlet.search;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.liferay.portal.service.PortletPreferencesLocalServiceUtil;
import org.apache.commons.lang.StringUtils;

import ru.emdev.portlet.BasePortlet;
import ru.emdev.util.GeneralUtil;
import ru.emdev.util.web.WebKeys;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HttpUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portlet.PortletPreferencesFactoryUtil;

public class SearchPortlet extends BasePortlet {
	private static Log log = LogFactoryUtil.getLog(SearchPortlet.class);
	
	@Override
	public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
		PortletPreferences preferences = request.getPreferences();

		try {
			
			String portletResource = ParamUtil.getString(request, "portletResource");
	
			if (StringUtils.isNotBlank(portletResource)) {
				preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
			}
	
			String searchHint = preferences.getValue("search-hint", StringPool.BLANK);
			String searchFieldHint = preferences.getValue("search-field-hint", StringPool.BLANK);
			request.setAttribute(WebKeys.SEARCH_HINT, searchHint);
			request.setAttribute(WebKeys.SEARCH_FIELD_HINT, searchFieldHint);
			
			
		} catch (Exception ex) {
			log.warn("Cannot get portlet properties", ex);
		}
		
		super.render(request, response);
	}

	
	public void search(ActionRequest request, ActionResponse response) throws SystemException, PortalException, IOException  {
		String keywords = ParamUtil.getString(request, "keywords");
		log.info("Search for:" + keywords);
		
		PortletPreferences preferences = request.getPreferences();

		String portletResource = ParamUtil.getString(request, "portletResource");

		if (StringUtils.isNotBlank(portletResource)) {
			preferences = PortletPreferencesFactoryUtil.getPortletSetup(request, portletResource);
		}

		String resultsUrl = preferences.getValue("results-url", StringPool.BLANK);
        String redirect = preferences.getValue("redirect-name", "search");
        //If portlet preference stored blank preferences.getValue returns it as is
        //but not default value. Use the check below..
        if (StringUtils.isEmpty(redirect)) {
        	redirect = "search";
        }
        ThemeDisplay themeDisplay = (ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);

        if(StringUtils.isBlank(resultsUrl)){
            PortletPreferences portletPreferences = PortletPreferencesLocalServiceUtil.getDefaultPreferences(0,"search_WAR_searchportlet");
            resultsUrl = portletPreferences.getValue("results-url", StringPool.BLANK);
        }
		if (StringUtils.isNotBlank(resultsUrl)) {
			if (!resultsUrl.startsWith(StringPool.SLASH)) {
				// this is page in current web-site - lets generate url
				String siteUrl = GeneralUtil.getLayoutSetFriendlyURL(themeDisplay.getLayoutSet(), themeDisplay, false);
				resultsUrl = siteUrl + StringPool.SLASH + resultsUrl;
			}
			
			String newUrl =themeDisplay.getPathFriendlyURLPublic()+themeDisplay.getLayout().getGroup().getFriendlyURL()+resultsUrl;
			// do redirect
			if (StringUtils.isNotBlank(redirect))
				newUrl +="/-/" + redirect + "/" + HttpUtil.encodeURL(keywords);

			sendRedirect(request, response, newUrl);
		} else {
			response.setRenderParameter("keywords", keywords);
		}
	}
}
