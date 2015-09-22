/**
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
package es.tid.fiware.rss.ws;

import es.tid.fiware.rss.model.RSUser;
import es.tid.fiware.rss.service.SettlementManager;
import es.tid.fiware.rss.service.UserManager;
import java.util.MissingFormatArgumentException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author jortiz
 */
public class SettlementServiceTest {

    @Mock SettlementManager settlementManager;
    @Mock UserManager userManager;
    @InjectMocks SettlementService toTest;

    public SettlementServiceTest() {
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void launchSettlementTest() throws Exception {
        String aggregatorId = "aggregator@mail.com";
        String providerId = "provider@mail.com";
        String productClass = "productClass";

        RSUser user = new RSUser();
        user.setDisplayName("username");
        user.setEmail("user@mail.com");

        when(userManager.getCurrentUser()).thenReturn(user);
        when(userManager.isAdmin()).thenReturn(true);

        toTest.launchSettlement(aggregatorId, providerId, productClass);
    }

    @Test
    public void launchSettlementNotAdminTest() throws Exception {
        String aggregatorId = "aggregator@mail.com";
        String providerId = "provider@mail.com";
        String productClass = "productClass";

        RSUser user = new RSUser();
        user.setDisplayName("username");
        user.setEmail(aggregatorId);

        when(userManager.getCurrentUser()).thenReturn(user);
        when(userManager.isAdmin()).thenReturn(false);

        toTest.launchSettlement(aggregatorId, providerId, productClass);
    }

    @Test
    (expected = MissingFormatArgumentException.class)
    public void launchSettlementNotAllowedTest() throws Exception {
        String aggregatorId = "aggregator@mail.com";
        String providerId = "provider@mail.com";
        String productClass = "productClass";

        RSUser user = new RSUser();
        user.setDisplayName("username");
        user.setEmail("user@mail.com");

        when(userManager.getCurrentUser()).thenReturn(user);
        when(userManager.isAdmin()).thenReturn(false);

        toTest.launchSettlement(aggregatorId, providerId, productClass);
    }


}