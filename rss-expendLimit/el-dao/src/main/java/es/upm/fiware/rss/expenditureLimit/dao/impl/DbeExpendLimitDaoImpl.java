/**
 * Revenue Settlement and Sharing System GE
 * Copyright (C) 2011-2014, Javier Lucio - lucio@tid.es
 * Telefonica Investigacion y Desarrollo, S.A.
 *
 * Copyright (C) 2015, CoNWeT Lab., Universidad Politécnica de Madrid
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package es.upm.fiware.rss.expenditureLimit.dao.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import es.upm.fiware.rss.dao.impl.GenericDaoImpl;
import es.upm.fiware.rss.expenditureLimit.dao.DbeExpendLimitDao;
import es.upm.fiware.rss.expenditureLimit.model.DbeExpendLimit;
import es.upm.fiware.rss.expenditureLimit.model.DbeExpendLimitPK;
import es.upm.fiware.rss.model.BmCurrency;
import es.upm.fiware.rss.model.BmObCountry;
import es.upm.fiware.rss.model.BmService;

/**
 *
 */
@Repository
public class DbeExpendLimitDaoImpl extends GenericDaoImpl<DbeExpendLimit, DbeExpendLimitPK> implements
    DbeExpendLimitDao {

    private static Logger logger = LoggerFactory.getLogger(DbeExpendLimitDaoImpl.class);

    /*
     * (non-Javadoc)
     * 
     * @see es.upm.greta.dbe.dao.impl.GenericDaoImpl#getDomainClass()
     */
    @Override
    protected Class<DbeExpendLimit> getDomainClass() {
        return DbeExpendLimit.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see es.upm.greta.dbe.expenditureLimit.dao.DbeExpendLimitDao#getExpendLimitsByUser(java.lang.String,
     * java.lang.Long)
     */
    @Override
    public List<DbeExpendLimit> getExpendLimitsForUserAppProvCurrency(String urlEndUserId,
            String aggregator, String appProviderId, BmCurrency bmCurrency) {
        DbeExpendLimitDaoImpl.logger.debug("Entering getExpendLimitsForUserCurrencyObCountry...");

        String hql = " from DbeExpendLimit el where (el.id.txEndUserId = :txUsrId1 or el.id.txEndUserId = :txUsrId2 )" +
            " and ((el.id.txAppProviderId = :txAppPID1 and el.id.txAggregatorId = :txAgg)" + 
            " or el.id.txAppProviderId = :txAppPID2)";

        @SuppressWarnings("unchecked")
        List<DbeExpendLimit> list;
        if (bmCurrency != null) {
            hql += " and el.id.nuCurrencyId = :bmCurrID";
            list = (List<DbeExpendLimit>) this.getSession().createQuery(hql).
            		setParameter("txUsrId1", DbeExpendLimitDao.NO_USER_ID).
            		setParameter("txUsrId2", urlEndUserId).
                        setParameter("txAgg", aggregator).
            		setParameter("txAppPID1", appProviderId).
            		setParameter("txAppPID2", DbeExpendLimitDao.NO_APP_PROVIDER_ID).
            		setParameter("bmCurrID", bmCurrency.getNuCurrencyId()).
            		list();
        } else {
        	list = (List<DbeExpendLimit>) this.getSession().createQuery(hql).
            		setParameter("txUsrId1", DbeExpendLimitDao.NO_USER_ID).
            		setParameter("txUsrId2", urlEndUserId).
                        setParameter("txAgg", aggregator).
            		setParameter("txAppPID1", appProviderId).
            		setParameter("txAppPID2", DbeExpendLimitDao.NO_APP_PROVIDER_ID).
            		list();
        }

        return list;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * es.upm.greta.dbe.expenditureLimit.dao.DbeExpendLimitDao#getOrdExpLimitsForUserAppProvCurrencyObCountry(java.lang
     * .String, es.upm.greta.dbe.model.BmService, java.lang.String, es.upm.greta.dbe.model.BmCurrency,
     * es.upm.greta.dbe.model.BmObCountry)
     */
    @Override
    public HashMap<String, List<DbeExpendLimit>> getOrdExpLimitsForUserAppProvCurrency(String urlEndUserId,
            String aggregator, String appProviderId, BmCurrency bmCurrency) {

        HashMap<String, List<DbeExpendLimit>> hLimits = new HashMap<>();
        List<DbeExpendLimit> allLimits = getExpendLimitsForUserAppProvCurrency(urlEndUserId,
            aggregator, appProviderId, bmCurrency);

        // Split the limits
        List<DbeExpendLimit> userAppLimits = new ArrayList<>();
        List<DbeExpendLimit> userLimits = new ArrayList<>();
        List<DbeExpendLimit> appLimits = new ArrayList<>();
        List<DbeExpendLimit> genericLimits = new ArrayList<>();

        Iterator<DbeExpendLimit> it = allLimits.iterator();
        while (it.hasNext()) {
            DbeExpendLimit el = it.next();
            if (el.getId().getTxEndUserId().equalsIgnoreCase(DbeExpendLimitDao.NO_USER_ID)) {
                // Generic user
                if (el.getId().getTxAppProviderId().equalsIgnoreCase(DbeExpendLimitDao.NO_APP_PROVIDER_ID)) {
                    // Generic app provider
                    genericLimits.add(el);
                } else {
                    // Specific app provider
                    appLimits.add(el);
                }

            } else if (el.getId().getTxAppProviderId().equalsIgnoreCase(DbeExpendLimitDao.NO_APP_PROVIDER_ID)) {
                // Specific user and generic app provider
                userLimits.add(el);
            } else {
                // Specific user and specific app provider
                userAppLimits.add(el);
            }
        }

        hLimits.put(DbeExpendLimitDao.USER_APP_PROV_KEY, userAppLimits);
        hLimits.put(DbeExpendLimitDao.USER_KEY, userLimits);
        hLimits.put(DbeExpendLimitDao.APP_PROV_KEY, appLimits);
        hLimits.put(DbeExpendLimitDao.ALL_GENERIC_KEY, genericLimits);

        return hLimits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * es.upm.greta.dbe.expenditureLimit.dao.DbeExpendLimitDao#getExpendLimitsByProviderUserService(es.upm.greta.dbe
     * .model.BmService, java.lang.String, java.lang.String,es.upm.greta.dbe.model.bmObCountry, es.upm.greta.dbe
     * .model.BmCurrency)
     */
    @Override
    public List<DbeExpendLimit> getExpendLimitsByProviderUserService(
            String aggregator, String provider, 
            String userId, BmCurrency bmCurrency) {

        DbeExpendLimitDaoImpl.logger.debug("Entering getExpendLimitsByProviderUserService...");

        String hql = "from DbeExpendLimit el where el.id.txEndUserId = :txUsrID and el.id.txAppProviderId = :txAppPID and el.id.txAggregatorId = :txAgg ";

        List<DbeExpendLimit> list;
        if (bmCurrency != null) {
            hql += " and el.id.nuCurrencyId = :nuCurrID";
            list = (List<DbeExpendLimit>) this.getSession().createQuery(hql).
            		setParameter("txUsrID", userId).
                        setParameter("txAgg", aggregator).
            		setParameter("txAppPID", provider).
            		setParameter("nuCurrID", bmCurrency.getNuCurrencyId()).
            		list();
        } else {
        	list = (List<DbeExpendLimit>) this.getSession().createQuery(hql).
                        setParameter("txAgg", aggregator).
            		setParameter("txUsrID", userId).
            		setParameter("txAppPID", provider).
            		list();
        }

        return list;
    }
}
