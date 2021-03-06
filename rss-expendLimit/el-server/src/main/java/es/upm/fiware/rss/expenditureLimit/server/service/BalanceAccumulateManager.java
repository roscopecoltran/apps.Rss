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

package es.upm.fiware.rss.expenditureLimit.server.service;

import es.upm.fiware.rss.dao.DbeAppProviderDao;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.upm.fiware.rss.exception.RSSException;
import es.upm.fiware.rss.exception.UNICAExceptionType;
import es.upm.fiware.rss.expenditureControl.api.AccumExpend;
import es.upm.fiware.rss.expenditureControl.api.AccumsExpend;
import es.upm.fiware.rss.expenditureControl.api.ExpendControl;
import es.upm.fiware.rss.expenditureLimit.dao.DbeExpendControlDao;
import es.upm.fiware.rss.expenditureLimit.model.DbeExpendControl;
import es.upm.fiware.rss.expenditureLimit.processing.ProcessingLimitService;
import es.upm.fiware.rss.model.DbeTransaction;

/**
 * 
 */
@Service
@Transactional
public class BalanceAccumulateManager {
    /**
     * Variable to print the trace.
     */
    private static Logger logger = LoggerFactory.getLogger(ExpenditureLimitManager.class);
    private final String tx_charge_type = "C";
    private final String tx_refund_type = "R";

    @Autowired
    private ExpenditureLimitDataChecker checker;

    @Autowired
    private ProcessingLimitService processingLimitService;

    @Autowired
    private DbeExpendControlDao expendControlDao;

    @Autowired
    private DbeAppProviderDao appProviderDao;
    /**
     * Get limits for a customer.
     * 
     * @param urlEndUserId
     * @param service
     * @param aggregator
     * @param appPorviderId
     * @param currency
     * @param elType
     * @return
     * @throws RSSException
     */
    public AccumsExpend getUserAccumulated(
            String urlEndUserId, String service, 
            String aggregator, String appPorviderId,
            String currency, String elType)
        throws RSSException {

        BalanceAccumulateManager.logger.debug("Into getUserAccumulated for user:{}", urlEndUserId);
        // check mandatory information
        checker.checkRequiredParameters(urlEndUserId, service, aggregator, appPorviderId, currency);
        // check service Existence
        //checker.checkService(service);
        // check valid appPorviderId
        checker.checkDbeAppProvider(aggregator, appPorviderId);
        // check valid elType
        checker.checkElType(elType);

        List<AccumExpend> accums = getUserRegisterAccumulated(
                urlEndUserId, service, aggregator, appPorviderId, currency, elType);

        AccumsExpend accumsExpend = new AccumsExpend();
        accumsExpend.setAccums(accums);
        accumsExpend.setAppProvider(appPorviderId);
        accumsExpend.setService(service);
        accumsExpend.setAggregator(aggregator);

        return accumsExpend;
    }

    /**
     * Check if an user has enough balance to purchase an application.
     * 
     * @param urlEndUserId
     * @param expendControl
     * @return
     * @throws RSSException
     */
    public AccumsExpend checkUserBalance(String urlEndUserId, ExpendControl expendControl)
        throws RSSException {
        BalanceAccumulateManager.logger.debug("Into checkUserBalance for user:{}", urlEndUserId);
        // check mandatory information
        checker.checkChargeRequiredParameters(
                urlEndUserId, expendControl.getService(),
                expendControl.getAggregator(),
                expendControl.getAppProvider(),
                expendControl.getCurrency(),
                expendControl.getChargeType(),
                expendControl.getAmount());

        // check valid appPorviderId
        checker.checkDbeAppProvider(
                expendControl.getAggregator(),
                expendControl.getAppProvider());

        // check valid elType
        checker.checkElType(expendControl.getType());

        // check valid chargeType
        checker.checkChargeType(expendControl.getChargeType());

        // Generate Transaction
        DbeTransaction transaction = generateTransaction(urlEndUserId, expendControl);
        processingLimitService.proccesLimit(transaction);

        // After everything ok --> Return current accumulated
        BalanceAccumulateManager.logger.debug("Checking compplete.Returning status for user: {}", urlEndUserId);

        return getUserAccumulated(
                urlEndUserId, expendControl.getService(),
                expendControl.getAggregator(),
                expendControl.getAppProvider(),
                expendControl.getCurrency(), null);
    }

    /**
     * Update User Accumulated.
     * 
     * @param expendControl
     * @param urlEndUserId
     * @return
     */
    public AccumsExpend updateUserAccumulated(String urlEndUserId, ExpendControl expendControl)
        throws RSSException {

        BalanceAccumulateManager.logger.debug("Into updateUserAccumulated for user:{}", urlEndUserId);

        // Check that the user has enought balance to perform the update
        this.checkUserBalance(urlEndUserId, expendControl);

        // Generate Transaction
        DbeTransaction transaction = generateTransaction(urlEndUserId, expendControl);
        processingLimitService.updateLimit(transaction);

        BalanceAccumulateManager.logger.debug("Checking compplete.Returning status for user: {}", urlEndUserId);

        return getUserAccumulated(
                urlEndUserId, expendControl.getService(),
                expendControl.getAggregator(),
                expendControl.getAppProvider(),
                expendControl.getCurrency(), null);
    }

    /**
     * Delete the accumulate/s values of an user (set to 0)
     * 
     * @param urlEndUserId
     * @param expendControl
     * @return
     */
    public void deleteUserAccumulated(String urlEndUserId, ExpendControl expendControl)
        throws RSSException {

        BalanceAccumulateManager.logger.debug("Into deleteUserAccumulated for user:{}", urlEndUserId);

        // check mandatory information
        checker.checkRequiredParameters(
                urlEndUserId,
                expendControl.getService(),
                expendControl.getAggregator(),
                expendControl.getAppProvider(),
                expendControl.getCurrency());

        // check service Existence
        //checker.checkService(expendControl.getService());

        // check valid appPorviderId
        checker.checkDbeAppProvider(
                expendControl.getAggregator(),
                expendControl.getAppProvider());

        // check valid elType
        checker.checkElType(expendControl.getType());
        List<DbeExpendControl> controls = getControls(
                urlEndUserId,
                expendControl.getService(),
                expendControl.getAggregator(),
                expendControl.getAppProvider(),
                expendControl.getCurrency(),
                expendControl.getType());

        if (null != controls && controls.size() > 0) {
            for (DbeExpendControl control : controls) {
                BalanceAccumulateManager.logger.debug("Deleting limit:{}", control.getId().getTxElType());
                control.setFtExpensedAmount(new BigDecimal(0));
                expendControlDao.update(control);
            }
        }
    }

    /**
     * Generate the transaction required for expenditure limit control.
     * 
     * @param urlEndUserId
     * @param expendControl
     * @return
     */
    private DbeTransaction generateTransaction(String urlEndUserId, ExpendControl expendControl)
        throws RSSException {
        BalanceAccumulateManager.logger.debug("Into generateTransaction method.");

        DbeTransaction tx = new DbeTransaction();
        tx.setTxEndUserId(urlEndUserId);

        if (this.tx_charge_type.equalsIgnoreCase(expendControl.getChargeType())) {
            tx.setTcTransactionType(this.tx_charge_type);
        } else if (this.tx_refund_type.equalsIgnoreCase(expendControl.getChargeType())) {
            tx.setTcTransactionType(this.tx_refund_type);
        } else {
            BalanceAccumulateManager.logger.debug("Charge type not allowed.");
            String[] args = { "Currency Not found." };
            throw new RSSException(UNICAExceptionType.NON_EXISTENT_RESOURCE_ID, args);
        }

        tx.setFtChargedAmount(expendControl.getAmount());
        tx.setAppProvider(appProviderDao.
                getProvider(expendControl.getAggregator(), expendControl.getAppProvider()));

        // get currency
        tx.setBmCurrency(checker.checkCurrency(expendControl.getCurrency()));
        // return tx
        return tx;
    }

    /**
     * Get data
     * 
     * @param urlEndUserId
     * @param appProviderId
     * @param currency
     * @param elType
     * @return
     * @throws RSSException
     */
    private List<AccumExpend> getUserRegisterAccumulated(
            String urlEndUserId, String service, String aggregator,
            String appProviderId, String currency, String elType)
        throws RSSException {

        BalanceAccumulateManager.logger.debug("Into getControls method.");
        List<DbeExpendControl> controls = getControls(
                urlEndUserId, service, aggregator, appProviderId, currency, elType);

        // change format to output format
        List<AccumExpend> result = new ArrayList<>();
        if (null != controls && controls.size() > 0) {
            for (DbeExpendControl control : controls) {
                result.add(fillAccumExpendFromControl(control));
            }
        }
        return result;
    }

    /**
     * Get Controls.
     * 
     * @param urlEndUserId
     * @param appProviderId
     * @param currency
     * @param elType
     * @return
     * @throws RSSException
     */
    private List<DbeExpendControl> getControls(
            String urlEndUserId, String service, String aggregator,
            String appProviderId, String currency, String elType)
        throws RSSException {

        BalanceAccumulateManager.logger.debug("Into getControls method.");

        List<DbeExpendControl> controls =
            expendControlDao.getExpendDataForUserAppProvCurrency(
                    urlEndUserId, aggregator, appProviderId, checker.checkCurrency(currency));

        List<DbeExpendControl> controlsFinal = new ArrayList<>();
        if (null != elType && elType.length() > 0) {
            if (null != controls && controls.size() > 0) {
                for (DbeExpendControl control : controls) {
                    if (elType.equalsIgnoreCase(control.getId().getTxElType())) {
                        controlsFinal.add(control);
                    }
                }
            }
            return controlsFinal;
        }
        return controls;
    }

    /**
     * Fill accumdExpend with ControlData
     * 
     * @param control
     * @return
     */
    private AccumExpend fillAccumExpendFromControl(DbeExpendControl control) {
        AccumExpend accumExpend = new AccumExpend();
        accumExpend.setCurrency(control.getBmCurrency().getTxIso4217Code());
        accumExpend.setExpensedAmount(control.getFtExpensedAmount());
        accumExpend.setNextPeriodStartDate(control.getDtNextPeriodStart());
        accumExpend.setType(control.getId().getTxElType());
        return accumExpend;
    }
}
