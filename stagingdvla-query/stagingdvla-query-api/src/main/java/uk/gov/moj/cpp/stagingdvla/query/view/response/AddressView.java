package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.util.Objects;

public class AddressView implements Serializable {

    private static final long serialVersionUID = 1L;

    private String address1;
    private String address2;
    private String address3;
    private String address4;
    private String postCode;

    public AddressView(String address1,
                       String address2,
                       String address3,
                       String address4,
                       String postCode) {
        this.address1 = address1;
        this.address2 = address2;
        this.address3 = address3;
        this.address4 = address4;
        this.postCode = postCode;
    }

    public String getAddress1() {
        return address1;
    }

    public String getAddress2() {
        return address2;
    }

    public String getAddress3() {
        return address3;
    }

    public String getAddress4() {
        return address4;
    }

    public String getPostCode() {
        return postCode;
    }

    @Override
    public String toString() {
        return "AddressView{" +
                "address1='" + address1 + '\'' +
                ", address2='" + address2 + '\'' +
                ", address3='" + address3 + '\'' +
                ", address4='" + address4 + '\'' +
                ", postCode='" + postCode + '\'' +
                '}';
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AddressView that = (AddressView) o;
        return Objects.equals(address1, that.address1) &&
                Objects.equals(address2, that.address2) &&
                Objects.equals(address3, that.address3) &&
                Objects.equals(address4, that.address4) &&
                Objects.equals(postCode, that.postCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address1, address2, address3, address4, postCode);
    }
}
