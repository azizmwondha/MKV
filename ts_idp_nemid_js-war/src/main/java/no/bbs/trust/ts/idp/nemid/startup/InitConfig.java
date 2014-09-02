/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package no.bbs.trust.ts.idp.nemid.startup;

import java.util.ArrayList;

import no.bbs.trust.common.config.ConfigProperty;
import no.bbs.trust.common.config.type.ConfigTypes;
import no.bbs.trust.ts.idp.nemid.contants.ConfigKeys;


public class InitConfig extends no.bbs.trust.common.webapp.servlets.InitConfig {
	@Override
	public ArrayList<ConfigProperty> getConfigPropertySettings() {
		ArrayList<ConfigProperty> params = new ArrayList<ConfigProperty>();

		ConfigProperty property = new ConfigProperty();
		property.setName(ConfigKeys.CPRREG_REQUESTISSUER);
		property.setMandatory(true);
		property.setType(ConfigTypes.STRING);
		params.add(property);

//		property = new ConfigProperty();
//		property.setName(ConfigKeys.CPRREG_PASSWORD);
//		property.setMandatory(true);
//		property.setType(ConfigTypes.STRING);
//		params.add(property);

		property = new ConfigProperty();
		property.setName(ConfigKeys.CPRREG_LOOKUP_URL);
		property.setMandatory(true);
		property.setType(ConfigTypes.URL);
		params.add(property);

		property = new ConfigProperty();
		property.setName(ConfigKeys.APPLET_URL_PREFIX);
		property.setMandatory(true);
		property.setType(ConfigTypes.URL);
		params.add(property);

		property = new ConfigProperty();
		property.setName(ConfigKeys.RIDREG_LOOKUP_URL);
		property.setMandatory(false);
		property.setType(ConfigTypes.URL);
		params.add(property);

		return params;
	}
}
