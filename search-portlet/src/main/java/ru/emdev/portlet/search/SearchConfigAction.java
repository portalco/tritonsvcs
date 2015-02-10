package ru.emdev.portlet.search;

import com.liferay.portal.kernel.portlet.DefaultConfigurationAction;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portlet.PortletPreferencesFactoryUtil;

import javax.portlet.*;

import ru.emdev.util.web.WebKeys;

/** Configuration for search portlet
 * 
 * @author akakunin
 *
 */
public class SearchConfigAction extends DefaultConfigurationAction {

	@Override
	public void processAction(PortletConfig config, ActionRequest req, ActionResponse res) throws Exception {

		String cmd = ParamUtil.getString(req, Constants.CMD);

		if (!cmd.equals(Constants.UPDATE)) {
			return;
		}

		String resultsUrl = ParamUtil.getString(req, "resultsUrl");
		String searchHint = ParamUtil.getString(req, WebKeys.SEARCH_HINT);
		String searchFieldHint = ParamUtil.getString(req, WebKeys.SEARCH_FIELD_HINT);
		String redirectName = ParamUtil.getString(req, "redirectName", "search");

		String portletResource = ParamUtil.getString(req, "portletResource");

		PortletPreferences prefs = PortletPreferencesFactoryUtil.getPortletSetup(req, portletResource);
		prefs.setValue("results-url", resultsUrl);
		prefs.setValue("search-hint", searchHint);
		prefs.setValue("search-field-hint", searchFieldHint);
		prefs.setValue("redirect-name", redirectName);

		prefs.store();

		SessionMessages.add(req, config.getPortletName() + ".doConfigure");
	}

	@Override
	public String render(PortletConfig config, RenderRequest req, RenderResponse res) throws Exception {

		return "/jsp/search" + super.render(config, req, res); 
	}
}
