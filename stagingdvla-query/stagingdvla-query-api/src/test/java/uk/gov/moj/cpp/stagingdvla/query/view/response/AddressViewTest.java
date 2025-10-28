package uk.gov.moj.cpp.stagingdvla.query.view.response;

import com.google.common.testing.EqualsTester;
import org.junit.jupiter.api.Test;

public class AddressViewTest {

    private static final String ADDRESS_1 = "14 Tottenham Court Road";
    private static final String ADDRESS_2 = "London";
    private static final String ADDRESS_3 = "England";
    private static final String ADDRESS_4 = "UK";
    private static final String POST_CODE = "W1T 1JY";

    @SuppressWarnings({"squid:MethodCyclomaticComplexity", "squid:S1067", "squid:S00122"})
    @Test
    public void equalsAndHashCode() {
        AddressView item1 = createAddressView(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, POST_CODE);
        AddressView item2 = createAddressView(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, POST_CODE);
        AddressView item3 = createAddressView("Other Address 1", ADDRESS_2, ADDRESS_3, ADDRESS_4, POST_CODE);
        AddressView item4 = createAddressView(ADDRESS_1, "Other Address 2", ADDRESS_3, ADDRESS_4, POST_CODE);
        AddressView item5 = createAddressView(ADDRESS_1, ADDRESS_2, "Other Address 3", ADDRESS_4, POST_CODE);
        AddressView item6 = createAddressView(ADDRESS_1, ADDRESS_2, ADDRESS_3, "Other Address 4", POST_CODE);
        AddressView item7 = createAddressView(ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, "Other Postcode");

        new EqualsTester()
                .addEqualityGroup(item1, item2)
                .addEqualityGroup(item3)
                .addEqualityGroup(item4)
                .addEqualityGroup(item5)
                .addEqualityGroup(item6)
                .addEqualityGroup(item7)
                .testEquals();
    }

    private AddressView createAddressView(final String address1, final String address2, final String address3, final String address4, final String postCode) {
        return new AddressView(address1, address2, address3, address4, postCode);
    }
}